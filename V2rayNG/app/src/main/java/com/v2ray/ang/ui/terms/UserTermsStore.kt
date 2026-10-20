package com.v2ray.ang.ui.terms

import com.v2ray.ang.AppConfig
import com.v2ray.ang.handler.MmkvManager
import com.v2ray.ang.util.LogUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val USER_TERMS_VERSION = 1
internal fun acceptedUserTerms(version: Int): Boolean = version >= USER_TERMS_VERSION

/** Consent has its own version and lifetime, independently of proxy profiles and their backups. */
internal interface UserTermsStore {
    suspend fun version(): Result<Int>
    suspend fun accept(): Result<Unit>
}

internal object MmkvUserTermsStore : UserTermsStore {
    private const val KEY = "future_fast_user_terms_version"

    override suspend fun version(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching { MmkvManager.decodeSettingsInt(KEY, 0) }
            .onFailure { LogUtil.e(AppConfig.TAG, "UserTerms: read acceptance version", it) }
    }

    override suspend fun accept(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            check(MmkvManager.encodeSettings(KEY, USER_TERMS_VERSION)) { "Terms acceptance write failed" }
        }.onFailure { LogUtil.e(AppConfig.TAG, "UserTerms: save acceptance version=$USER_TERMS_VERSION", it) }
    }
}
