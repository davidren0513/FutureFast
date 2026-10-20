package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.SubscriptionItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SubscriptionImportTargetTest {
    @Test fun `pasting into Default preserves group identity metadata and previous value until success`() {
        val saved = SubscriptionItem(remarks = "Default", url = "https://old.test/sub", enabled = false,
            autoUpdate = true, updateInterval = 360)
        val plan = AngConfigManager.existingSubscriptionImport("https://new.test/sub", "default", saved)!!
        assertEquals("default", plan.guid)
        assertEquals("Default", plan.subscription.remarks)
        assertEquals("https://new.test/sub", plan.subscription.url)
        assertTrue(plan.subscription.enabled)
        assertTrue(plan.subscription.autoUpdate)
        assertEquals(360L, plan.subscription.updateInterval)
        assertEquals("https://old.test/sub", saved.url)
        assertFalse(saved.enabled)
    }
    @Test fun `unselected deleted or empty input does not attach a subscription`() {
        val saved = SubscriptionItem(remarks = "Default")
        assertNull(AngConfigManager.existingSubscriptionImport(null, "default", saved))
        assertNull(AngConfigManager.existingSubscriptionImport(" ", "default", saved))
        assertNull(AngConfigManager.existingSubscriptionImport("https://new.test", "", saved))
        assertNull(AngConfigManager.existingSubscriptionImport("https://new.test", "deleted", null))
    }
}
