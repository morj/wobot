package com.github.morj.wobot

import com.kennycason.kumo.font.scale.FontScalar

class AdaptiveLinearFontScalar(val minFreq: Int,
                               val maxFreq: Int,
                               val minFontSize: Float, maxFontSize: Float) : FontScalar {
    val fontMultiplier = Math.min(maxFontSize - minFontSize, 200.toFloat())

    override fun scale(freq: Int, minValue: Int, maxValue: Int): Float {
        return ((freq - minFreq) * fontMultiplier / (maxFreq - minFreq)) + minFontSize
    }
}
