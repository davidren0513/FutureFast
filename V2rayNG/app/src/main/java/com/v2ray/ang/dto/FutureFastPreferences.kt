package com.v2ray.ang.dto

/** One atomic MMKV record; IDs remain independent of list ordering and server addresses. */
data class FutureFastPreferences(
    val autoSelect: Boolean = false,
    val testIntervalMinutes: Long = 30,
    val aiEnabled: Boolean = false,
    val aiNodes: Map<String, FutureFastNodeReference> = emptyMap(),
    val lastTestAt: Long = 0,
    val lastTestOutcome: String = "",
    val connectionRevision: Long = 0,
    val connectionRequestedAt: Long = 0,
)

data class FutureFastNodeReference(
    val guid: String = "",
    val subscriptionId: String = "",
    val remarks: String = "",
)

enum class FutureFastAiService(val domains: List<String>) {
    CHATGPT(listOf("chatgpt.com", "openai.com", "oaistatic.com", "oaiusercontent.com")),
    CLAUDE(listOf("claude.ai", "anthropic.com")),
    GEMINI(listOf("gemini.google.com", "generativelanguage.googleapis.com", "aistudio.google.com"));

    val tag: String get() = "future-fast-ai-${name.lowercase()}"
}

/** Pure decisions shared by configuration, UI and background work. */
object FutureFastPolicy {
    fun unsupportedSubscription(content: String): Boolean {
        val trimmed = content.trimStart()
        return trimmed.startsWith("<") || Regex("(?m)^\\s*(proxies|proxy-groups|mixed-port|port):").containsMatchIn(trimmed)
    }

    fun intervalMinutes(value: Long): Long = value.coerceIn(15, 10080)

    fun resolveNode(reference: FutureFastNodeReference, nodes: List<FutureFastNodeReference>): String? {
        nodes.firstOrNull { it.guid == reference.guid }?.let { return it.guid }
        if (reference.subscriptionId.isBlank() || reference.remarks.isBlank()) return null
        return nodes.filter {
            it.subscriptionId == reference.subscriptionId && it.remarks == reference.remarks
        }.singleOrNull()?.guid
    }

    fun fastest(results: Map<String, Long>, validGuids: Set<String>): String? =
        results.filter { (guid, delay) -> guid in validGuids && delay >= 0 }
            .minWithOrNull(compareBy<Map.Entry<String, Long>> { it.value }.thenBy { it.key })?.key

    fun mayOptimize(running: Boolean?, requestedAt: Long, now: Long): Boolean =
        running == false && (requestedAt == 0L || now - requestedAt > 120_000L)
}
