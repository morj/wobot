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
        val minFontSize = 11.toFloat()
        val maxWeight = (top.frequency - minFreq).toFloat() * top.word.length / (maxFreq - minFreq)
        val graphics = bufferedImage.graphics
        val fontMetrics = graphics.getFontMetrics(kumoFont.font)
        val areaPerLetter = fontMetrics.stringWidth("x").toFloat()
        val magic = kumoFont.font.size / (maxWeight * areaPerLetter / dimension.width)
        val maxFontSize = Math.max(minFontSize * 2.7.toFloat(), magic)
        val length = wordFrequencies.size
        val fraction = if (length > 100) {
            2.5.toFloat()
        } else if (length > 75) {
            2.0.toFloat()
        } else {
            1.5.toFloat()
        }
        setFontScalar(AdaptiveLinearFontScalar(minFreq, maxFreq, fraction, minFontSize, maxFontSize))
        for (word in buildWords(wordFrequencies, colorPalette)) {
            val placed = wordPlacer.place(word)
            if (placed) {
                graphics.drawImage(word.bufferedImage, word.position.x, word.position.y, null)
                if (LOGGER.isDebugEnabled) {
                    LOGGER.debug("placed: " + word.word + " (" + currentWord + "/" + length + ")")
                }
            } else {
                if (LOGGER.isDebugEnabled) {
                    LOGGER.debug("skipped: " + word.word + " (" + currentWord + "/" + length + ")")
                }
                skipped.add(word)
            }
            currentWord++
        }
        drawForegroundToBackground()
    }
}
