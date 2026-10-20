package com.v2ray.ang.ui.main.liquid

import kotlin.math.roundToInt

/** Presentation-only position: page identity remains the saved main-screen page ID. */
internal fun liquidTabIndex(position: Float, count: Int): Int {
    require(count > 0)
    return if (position.isFinite()) position.roundToInt().coerceIn(0, count - 1) else 0
}
