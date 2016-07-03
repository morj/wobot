package com.github.morj.wobot

import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.font.FontWeight
import com.kennycason.kumo.font.KumoFont
import com.kennycason.kumo.image.AngleGenerator
import com.kennycason.kumo.nlp.FrequencyAnalyzer
import com.kennycason.kumo.palette.ColorPalette
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted
import com.ullink.slack.simpleslackapi.impl.ChannelHistoryModuleFactory
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import org.languagetool.Language
import org.slf4j.LoggerFactory
import org.threeten.bp.LocalDate
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.io.PipedInputStream
import java.io.PipedOutputStream

class Wobot(val token: String, val language: Language) {
    companion object {
        val logger = LoggerFactory.getLogger(Wobot::class.java)
    }

    val session = SlackSessionFactory.createWebSocketSlackSession(token)
    val historyGetter = ChannelHistoryModuleFactory.createChannelHistoryModule(session)
    val id: String
    val me: String
    val mention = Regex("(<@([A-Z0-9])+(\\|([a-z0-9]){0,21})?>)|(@here)|(@channel)")

    init {
        session.connect()
        id = session.sessionPersona().id
        me = "<@$id>"
    }

    fun parseCommand(posted: SlackMessagePosted): () -> Any {
        val msg = posted.messageContent.substring(startIndex = me.length)
        logger.debug(msg)
        // session.sendMessage(posted.channel, "Morj wrote: \n> ${posted.messageContent}", null)
        val tokens = language.wordTokenizer.tokenize(msg).filter { it.length > 2 }.toSet()
        if (logger.isDebugEnabled) {
            logger.debug(tokens.fold(StringBuilder("Normalized command:")) { sb, s ->
                sb.append(' ').append(s)
            }.toString())
        }
        if (tokens.contains("today") || tokens.contains("сегодня")) {
            return {
                historyGetter.fetchHistoryOfChannel(posted.channel.id, LocalDate.now()).query()
            }
        }
        return {
            historyGetter.fetchHistoryOfChannel(posted.channel.id, 1000).query()
        }
    }

    fun List<SlackMessagePosted>.query(): StringBuilder {
        logger.info("Finished fetching history")
        val text = fold(StringBuilder()) { sb, msg ->
            sb.append(' ').append(removeMention(msg))
        }
        return text
    }

    fun removeMention(msg: SlackMessagePosted): String {
        return msg.messageContent.replaceFirst(mention, "")
    }
}

fun main(args: Array<String>) {
    val wobot = Wobot(System.getProperty("bot.token"), lang(System.getProperty("bot.cloud.lang", "en")))
    wobot.session.addMessagePostedListener { posted, session ->
        if (posted.messageContent.startsWith(wobot.me)) {
            Wobot.logger.info("Start processing")
            val command = wobot.parseCommand(posted)
            try {
                val text = command()
                val os = PipedOutputStream()
                val wordCloud = wc(wobot.language, listOf(text.toString()))
                Thread {
                    try {
                        wordCloud.writeToStreamAsPNG(os)
                        os.close()
                    } catch (t: Throwable) {
                        Wobot.logger.error("Cannot write image", t)
                    }
                }.start()
                sendFile(PipedInputStream(os), posted.channel, wobot.token)
            } catch (t: Throwable) {
                Wobot.logger.error("Cannot prepare image", t)
            }
        }
    }
}


fun wc(language: Language, input: List<String>): WordCloud {
    val frequencyAnalyzer = FrequencyAnalyzer().apply {
        setWordTokenizer {
            language.wordTokenizer.tokenize(it)
        }
        setWordFrequenciesToReturn(160)
        setMinWordLength(3)
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
