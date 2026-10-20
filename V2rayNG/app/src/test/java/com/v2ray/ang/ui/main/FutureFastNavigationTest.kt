package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FutureFastNavigationTest {
    @Test
    fun `a released lens selects the nearest destination`() {
        assertEquals("home", futureFastDragDestination(0f))
        assertEquals("home", futureFastDragDestination(0.49f))
        assertEquals("nodes", futureFastDragDestination(0.5f))
        assertEquals("nodes", futureFastDragDestination(1.49f))
        assertEquals("ai", futureFastDragDestination(1.5f))
        assertEquals("ai", futureFastDragDestination(2f))
    }

    @Test
    fun `overscroll clamps and invalid gesture coordinates return home`() {
        assertEquals("home", futureFastDragDestination(-5f))
        assertEquals("ai", futureFastDragDestination(8f))
        assertEquals("home", futureFastDragDestination(Float.NaN))
        assertEquals("home", futureFastDragDestination(Float.POSITIVE_INFINITY))
        assertEquals("home", futureFastDragDestination(Float.NEGATIVE_INFINITY))
    }
}
