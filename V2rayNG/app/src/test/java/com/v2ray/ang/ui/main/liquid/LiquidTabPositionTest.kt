package com.v2ray.ang.ui.main.liquid

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LiquidTabPositionTest {
    @Test fun `drag chooses the nearest tab and clamps outside edges`() {
        assertEquals(0, liquidTabIndex(-2f, 3))
        assertEquals(0, liquidTabIndex(0.49f, 3))
        assertEquals(1, liquidTabIndex(0.51f, 3))
        assertEquals(1, liquidTabIndex(1.49f, 3))
        assertEquals(2, liquidTabIndex(1.51f, 3))
        assertEquals(2, liquidTabIndex(8f, 3))
        assertEquals(0, liquidTabIndex(8f, 1))
    }
    @Test fun `invalid input returns a valid position and empty tabs are rejected`() {
        assertEquals(0, liquidTabIndex(Float.NaN, 3))
        assertEquals(0, liquidTabIndex(Float.POSITIVE_INFINITY, 3))
        assertThrows(IllegalArgumentException::class.java) { liquidTabIndex(0f, 0) }
        assertThrows(IllegalArgumentException::class.java) { liquidTabIndex(0f, -1) }
    }
}
