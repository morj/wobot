package com.github.morj.wobot

import com.kennycason.kumo.CollisionMode
import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.WordFrequency
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.util.*

class SpiralWordCloud(dimension: Dimension) : WordCloud(dimension, CollisionMode.RECTANGLE) {
    companion object {
        val LOGGER = LoggerFactory.getLogger(SpiralWordCloud::class.java)
    }

    init {
        setWordPlacer(SpiralWordPlacer(dimension))
    }

    override fun build(wordFrequencies: MutableList<WordFrequency>) {
        wordPlacer.reset()
        skipped.clear()
        Collections.sort(wordFrequencies)
        var currentWord = 1
        val minFreq = wordFrequencies.minBy { it.frequency }?.frequency ?: 0
        val maxFreq = wordFrequencies.maxBy { it.frequency }?.frequency ?: 0
        val top = wordFrequencies.maxBy { (it.frequency - minFreq) * it.word.length } ?: return // empty input
        val graphics = bufferedImage.graphics
        val fontMetrics = graphics.getFontMetrics(kumoFont.font)
        val maxWeight = (top.frequency - minFreq).toFloat() * top.word.length / Math.max(maxFreq - minFreq, 1)
        val areaPerLetter = fontMetrics.stringWidth("x").toFloat()
        val magic = kumoFont.font.size / (maxWeight * areaPerLetter / dimension.width)
        val params = FontParams(wordFrequencies.size)
        val maxFontSize = Math.max(params.minFontSize * 2.7.toFloat(), magic)
        setFontScalar(AdaptiveLinearFontScalar(minFreq, maxFreq, params, maxFontSize))
        for (word in buildWords(wordFrequencies, colorPalette)) {
            val placed = wordPlacer.place(word)
            if (placed) {
                graphics.drawImage(word.bufferedImage, word.position.x, word.position.y, null)
                if (LOGGER.isDebugEnabled) {
                    LOGGER.debug("placed: " + word.word + " (" + currentWord + "/" + wordFrequencies.size + ")")
                }
            } else {
                if (LOGGER.isDebugEnabled) {
                    LOGGER.debug("skipped: " + word.word + " (" + currentWord + "/" + wordFrequencies.size + ")")
                }
                skipped.add(word)
            }
            currentWord++
        }
        drawForegroundToBackground()
    }
}

data class FontParams(val fraction: Float, val minFontSize: Int) {

    constructor(pair: Pair<Double, Int>) : this(pair.first.toFloat(), pair.second)

    constructor(length: Int) : this(if (length > 100) {
        2.5 to 11
    } else if (length > 75) {
        2.0 to 17
    } else {
        1.5 to 21
    })
}
