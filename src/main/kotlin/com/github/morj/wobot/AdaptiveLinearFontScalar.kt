package com.github.morj.wobot

import com.kennycason.kumo.font.scale.FontScalar
import org.slf4j.LoggerFactory

val MAX_MULTIPLIER = 300.toFloat()

class AdaptiveLinearFontScalar(val minFreq: Int,
                               val maxFreq: Int,
                               val params: FontParams,
                               maxFontSize: Float) : FontScalar {
    companion object {
        val LOGGER = LoggerFactory.getLogger(AdaptiveLinearFontScalar::class.java)
    }

    val fontMultiplier: Float

    init {
        val multiplier = (maxFontSize - params.minFontSize) / params.fraction
        fontMultiplier = if (multiplier > MAX_MULTIPLIER) {
            if (LOGGER.isWarnEnabled) {
                LOGGER.warn("Decreasing multiplier from $multiplier to $MAX_MULTIPLIER")
            }
            MAX_MULTIPLIER
        } else {
            multiplier
        }
    }

    override fun scale(freq: Int, minValue: Int, maxValue: Int): Float {
        return ((freq - minFreq) * fontMultiplier / (maxFreq - minFreq)) + params.minFontSize
    }
}
