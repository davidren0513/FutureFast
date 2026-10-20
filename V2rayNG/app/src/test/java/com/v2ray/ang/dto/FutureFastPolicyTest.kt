package com.v2ray.ang.dto

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class FutureFastPolicyTest {
    @Test
    fun `only fresh successful valid-node results participate in selection`() {
        assertEquals("fast", FutureFastPolicy.fastest(mapOf("failed" to -1L, "stale" to 1L, "slow" to 80L, "fast" to 20L), setOf("failed", "slow", "fast")))
        assertNull(FutureFastPolicy.fastest(emptyMap(), emptySet()))
        assertNull(FutureFastPolicy.fastest(mapOf("a" to -1L), setOf("a")))
        assertEquals("a", FutureFastPolicy.fastest(mapOf("b" to 20L, "a" to 20L), setOf("a", "b")))
    }

    @Test
    fun `node identity survives reorder and unambiguous subscription replacement`() {
        val saved = FutureFastNodeReference("old", "subscription", "s2")
        val changed = saved.copy(guid = "new")
        assertEquals("old", FutureFastPolicy.resolveNode(saved, listOf(changed, saved)))
        assertEquals("new", FutureFastPolicy.resolveNode(saved, listOf(changed)))
        assertNull(FutureFastPolicy.resolveNode(saved, listOf(changed, changed.copy(guid = "duplicate"))))
        assertNull(FutureFastPolicy.resolveNode(saved, listOf(changed.copy(subscriptionId = "other"))))
        assertNull(FutureFastPolicy.resolveNode(saved, emptyList()))
        assertNull(FutureFastPolicy.resolveNode(saved.copy(subscriptionId = ""), listOf(changed)))
    }

    @Test
    fun `unknown or running daemon and recent connection requests prevent optimization`() {
        assertFalse(FutureFastPolicy.mayOptimize(null, 0, 200_000))
        assertFalse(FutureFastPolicy.mayOptimize(true, 0, 200_000))
        assertFalse(FutureFastPolicy.mayOptimize(false, 190_000, 200_000))
        assertFalse(FutureFastPolicy.mayOptimize(false, 300_000, 200_000))
        assertTrue(FutureFastPolicy.mayOptimize(false, 0, 200_000))
        assertTrue(FutureFastPolicy.mayOptimize(false, 1, 200_000))
    }

    @Test
    fun `interval honors background scheduling limits`() {
        assertEquals(15L, FutureFastPolicy.intervalMinutes(-1))
        assertEquals(30L, FutureFastPolicy.intervalMinutes(30))
        assertEquals(10080L, FutureFastPolicy.intervalMinutes(Long.MAX_VALUE))
    }

    @Test
    fun `HTML and incompatible Clash YAML are rejected before replacement`() {
        assertTrue(FutureFastPolicy.unsupportedSubscription("  <html>login</html>"))
        assertTrue(FutureFastPolicy.unsupportedSubscription("# Clash\nproxies:\n  - name: sample"))
        assertTrue(FutureFastPolicy.unsupportedSubscription("mixed-port: 7890\nproxy-groups: []"))
        assertFalse(FutureFastPolicy.unsupportedSubscription("vless://example"))
        assertFalse(FutureFastPolicy.unsupportedSubscription("dmxlc3M6Ly9leGFtcGxl"))
    }
    @Test
    fun `AI assignments stay independent of the default node and each other`() {
        val chatgpt = FutureFastNodeReference("us", "subscription", "US")
        val claude = FutureFastNodeReference("jp", "subscription", "JP")
        val gemini = FutureFastNodeReference("sg", "subscription", "SG")
        val saved = FutureFastPreferences(aiEnabled = true, aiNodes = mapOf(
            FutureFastAiService.CHATGPT.name to chatgpt,
            FutureFastAiService.CLAUDE.name to claude,
            FutureFastAiService.GEMINI.name to gemini,
        ))
        val restored = com.google.gson.Gson().fromJson(
            com.google.gson.Gson().toJson(saved), FutureFastPreferences::class.java)
        assertEquals(saved, restored)
        val nodes = listOf(gemini, claude, chatgpt)
        assertEquals(listOf("us", "jp", "sg"), FutureFastAiService.entries.map {
            FutureFastPolicy.resolveNode(restored.aiNodes.getValue(it.name), nodes)
        })
        // A missing reference stays unresolved; runtime routing decides the default fallback without changing saved assignments.
        assertNull(FutureFastPolicy.resolveNode(chatgpt, listOf(claude, gemini)))
        assertEquals("jp", FutureFastPolicy.resolveNode(claude, listOf(claude, gemini)))
        val followDefault = restored.copy(aiNodes = restored.aiNodes - FutureFastAiService.GEMINI.name)
        assertFalse(followDefault.aiNodes.containsKey(FutureFastAiService.GEMINI.name))
        assertEquals(chatgpt, followDefault.aiNodes[FutureFastAiService.CHATGPT.name])
        assertEquals(claude, followDefault.aiNodes[FutureFastAiService.CLAUDE.name])
    }

    @Test
    fun `AI outbound tags are unique and cover web API and studio endpoints`() {
        assertEquals(3, FutureFastAiService.entries.map { it.tag }.toSet().size)
        assertTrue(FutureFastAiService.CHATGPT.domains.containsAll(listOf("chatgpt.com", "openai.com")))
        assertTrue(FutureFastAiService.CLAUDE.domains.containsAll(listOf("claude.ai", "anthropic.com")))
        assertTrue(FutureFastAiService.GEMINI.domains.containsAll(listOf(
            "gemini.google.com", "generativelanguage.googleapis.com", "aistudio.google.com")))
    }
}
