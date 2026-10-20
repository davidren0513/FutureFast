package com.v2ray.ang.handler

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.multiprocess.RemoteWorkManager
import com.v2ray.ang.AppConfig
import com.v2ray.ang.core.CoreConfigManager
import com.v2ray.ang.core.CoreNativeManager
import com.v2ray.ang.dto.FutureFastPolicy
import com.v2ray.ang.helper.MessageHelper
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Background optimization has a different lifetime from the visible main-screen tests. */
object FutureFastOptimizer {
    private const val PERIODIC = "future_fast_optimizer"
    private const val MANUAL = "future_fast_optimizer_manual"
    private val batchMutex = Mutex()

    fun schedule(context: Context) {
        val manager = RemoteWorkManager.getInstance(context)
        val preferences = MmkvManager.futureFastPreferences()
        if (!preferences.autoSelect) {
            manager.cancelUniqueWork(PERIODIC)
            return
        }
        val minutes = FutureFastPolicy.intervalMinutes(preferences.testIntervalMinutes)
        manager.enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.UPDATE,
            PeriodicWorkRequestBuilder<OptimizeWorker>(minutes, TimeUnit.MINUTES)
                .setInitialDelay(minutes, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build())
    }

    fun runNow(context: Context) {
        RemoteWorkManager.getInstance(context).enqueueUniqueWork(MANUAL, ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<OptimizeWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build())
    }

    class OptimizeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
            if (!batchMutex.tryLock()) return@withContext Result.success()
            try {
                optimize()
                Result.success()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                LogUtil.e(AppConfig.TAG, "Future Fast: background optimization failed", error)
                MmkvManager.updateFutureFastPreferences { it.copy(lastTestOutcome = "error") }
                Result.retry()
            } finally {
                batchMutex.unlock()
            }
        }

        private suspend fun mayContinue(revision: Long): Boolean {
            val current = MmkvManager.futureFastPreferences()
            return current.connectionRevision == revision && FutureFastPolicy.mayOptimize(
                MessageHelper.queryFutureFastRunning(applicationContext), current.connectionRequestedAt,
                System.currentTimeMillis())
        }

        private suspend fun optimize() {
            val preferences = MmkvManager.futureFastPreferences()
            val revision = preferences.connectionRevision
            if (!mayContinue(revision)) {
                MmkvManager.updateFutureFastPreferences { it.copy(lastTestOutcome = "paused") }
                return
            }
            val selectedBefore = MmkvManager.getSelectServer()
            val profiles = SettingsManager.futureFastNodes().mapNotNull { node ->
                MmkvManager.decodeServerConfig(node.guid)?.copy()?.let { node.guid to it }
            }.toMap()
            val results = linkedMapOf<String, Long>()
            val started = System.currentTimeMillis()
            CoreNativeManager.initCoreEnv(applicationContext)
            for ((guid, snapshot) in profiles) {
                currentCoroutineContext().ensureActive()
                if (!mayContinue(revision) || System.currentTimeMillis() - started > 480_000L) return
                // These are isolated Xray outbound probes, never a global VPN or raw TCP ranking.
                val config = CoreConfigManager.getV2rayConfig4Speedtest(applicationContext, guid)
                if (!config.status) continue
                val delay = CoreNativeManager.measureOutboundDelay(config.content, SettingsManager.getDelayTestUrl())
                currentCoroutineContext().ensureActive()
                if (MmkvManager.decodeServerConfig(guid) == snapshot) {
                    results[guid] = delay
                    MmkvManager.encodeServerTestDelayMillis(guid, delay)
                }
            }
            if (!mayContinue(revision) || MmkvManager.getSelectServer() != selectedBefore) return
            val valid = profiles.filter { (guid, profile) -> MmkvManager.decodeServerConfig(guid) == profile }.keys
            val fastest = FutureFastPolicy.fastest(results, valid)
            // Recheck immediately before publishing. An active session is never restarted here.
            if (!mayContinue(revision)) return
            if (fastest != null && !MmkvManager.selectFutureFastServer(fastest, selectedBefore, revision)) return
            MmkvManager.updateFutureFastPreferences {
                it.copy(lastTestAt = System.currentTimeMillis(), lastTestOutcome = if (fastest == null) "none" else "selected")
            }
        }
    }
}
