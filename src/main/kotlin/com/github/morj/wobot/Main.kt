package com.github.morj.wobot

import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.font.FontWeight
import com.kennycason.kumo.font.KumoFont
import com.kennycason.kumo.image.AngleGenerator
import com.kennycason.kumo.nlp.FrequencyAnalyzer
import com.kennycason.kumo.palette.ColorPalette
import com.ullink.slack.simpleslackapi.impl.ChannelHistoryModuleFactory
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import org.languagetool.language.Russian
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.io.PipedInputStream
import java.io.PipedOutputStream

class Wobot() // handle

val TOKEN = System.getProperty("bot.token")

val RUSSIAN = Russian()

val LOGGER = LoggerFactory.getLogger(Wobot::class.java)

fun main(args: Array<String>) {
    val session = SlackSessionFactory.createWebSocketSlackSession(TOKEN)
    session.connect()
    val id = session.sessionPersona().id
    val magic = "<@$id>"
    val historyGetter = ChannelHistoryModuleFactory.createChannelHistoryModule(session)
    session.addMessagePostedListener { posted, session ->
        if (posted.messageContent.startsWith(magic)) {
            LOGGER.info("Start processing")
            // session.sendMessage(posted.channel, "Morj wrote: \n> ${posted.messageContent}", null)
            try {
                val messages = historyGetter.fetchHistoryOfChannel(posted.channel.id, 100)
                LOGGER.info("Finished fetching history")
                val text = messages.filter {
                    it.timestamp != posted.timestamp && !it.messageContent.startsWith(magic)
                }.fold(StringBuilder()) { sb, msg ->
                    sb.append(' ').append(msg.messageContent)
                }
                val os = PipedOutputStream()
                val wordCloud = wc(listOf(text.toString()))
                Thread {
                    try {
                        wordCloud.writeToStreamAsPNG(os)
                        os.close()
                    } catch (t: Throwable) {
                        LOGGER.error("Cannot write image", t)
                    }
                }.start()
                sendFile(PipedInputStream(os), posted.channel, TOKEN)
            } catch (t: Throwable) {
                LOGGER.error("Cannot prepare image", t)
            }
        }
    }
}


fun wc(input: List<String>): WordCloud {
    val frequencyAnalyzer = FrequencyAnalyzer().apply {
        setWordTokenizer {
            RUSSIAN.wordTokenizer.tokenize(it)
        }
        setWordFrequenciesToReturn(160)
        setMinWordLength(4)
        setStopWords(listOf("this"))
    }
    val wordFrequencies = frequencyAnalyzer.load(input)
    val dimension = Dimension(1247, 732)
    return SpiralWordCloud(dimension).apply {
        setPadding(2)
        setKumoFont(KumoFont(Font("Courier", FontWeight.PLAIN.weight, 100)))
        setBackgroundColor(Color.WHITE)
        setAngleGenerator(AngleGenerator(0))
        setColorPalette(ColorPalette(COLORS.map { Color(it) }))
        build(wordFrequencies)
    }
}
