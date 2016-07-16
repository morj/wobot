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
    val special = Regex("(<@([A-Z0-9])+(\\|([a-z0-9]){0,21})?>)|(@channel)|(@group)|(@here)|(@everyone)|(&lt;)|(&gt;)|(&amp;)")
    val mention = Regex("<@([A-Z0-9])+(\\|([a-z0-9]){0,21})?>")

    init {
        session.connect()
        id = session.sessionPersona().id
        me = "<@$id>"
    }

    fun parseCommand(posted: SlackMessagePosted): BotCommand {
        val msg = posted.messageContent.substring(startIndex = me.length)
        logger.debug(msg)
        // session.sendMessage(posted.channel, "Morj wrote: \n> ${posted.messageContent}", null)
        val tokens = language.wordTokenizer.tokenize(msg).filter { it.length > 2 }.toSet()
        if (logger.isDebugEnabled) {
            logger.debug(tokens.fold(StringBuilder("Normalized command:")) { sb, s ->
                sb.append(' ').append(s)
            }.toString())
        }
        val minWordSize = if (tokens.contains("longer") || tokens.contains("пространно")) {
            4
        } else {
            3
        }
        val set = hashSetOf<String>()
        mention.find(msg)?.groups?.forEach {
            val value = it?.value // TODO: use pre/post match in regex instead
            if (value != null && value.startsWith("<@") && value.endsWith(">")) {
                set.add(value.substring(2, value.length - 1))
            }
        }
        val users = session.users.filter { set.contains(it.id) }.toSet()
        val filter: (SlackMessagePosted) -> Boolean = if (users.isNotEmpty()) {
            { users.contains(it.sender) }
        } else {
            { true }
        }
        if (tokens.contains("today") || tokens.contains("сегодня")) {
            return HistoryToImageCommand(posted, minWordSize) {
                historyGetter.fetchHistoryOfChannel(posted.channel.id, LocalDate.now()).query(filter)
            }
        }
        // default command
        return HistoryToImageCommand(posted, minWordSize) {
            historyGetter.fetchHistoryOfChannel(posted.channel.id, 1000).query(filter)
        }
    }

    fun List<SlackMessagePosted>.query(filter: (SlackMessagePosted) -> Boolean): StringBuilder {
        logger.info("Finished fetching history")
        val text = fold(StringBuilder()) { sb, msg ->
            if (filter(msg)) {
                sb.append(' ').append(removeMention(msg))
            } else {
                sb
            }
        }
        return text
    }

    fun removeMention(msg: SlackMessagePosted): String {
        return msg.messageContent.replace(special, "")
    }
}

interface BotCommand {
    enum class Type {
        HISTORY_TO_IMAGE,
        MISC
    }

    val type: Type
    operator fun invoke(wobot: Wobot)
}

class HistoryToImageCommand(val source: SlackMessagePosted, val minWordSize: Int, val action: () -> Any) : BotCommand {
    override val type: BotCommand.Type
        get() = BotCommand.Type.HISTORY_TO_IMAGE

    override fun invoke(wobot: Wobot) {
        try {
            val text = action()
            val os = PipedOutputStream()
            val wordCloud = wc(wobot.language, minWordSize, listOf(text.toString()))
            Thread {
                try {
                    wordCloud.writeToStreamAsPNG(os)
                    os.close()
                } catch (t: Throwable) {
                    Wobot.logger.error("Cannot write image", t)
                }
            }.start()
            sendFile(PipedInputStream(os), source.channel, wobot.token)
        } catch (t: Throwable) {
            Wobot.logger.error("Cannot prepare image", t)
        }
    }
}

/*
class TypingEventComand(val source: SlackMessagePosted) : BotCommand {
    override val type: BotCommand.Type
        get() = BotCommand.Type.MISC

    override fun invoke(wobot: Wobot) {
    }
}
*/

fun main(args: Array<String>) {
    Wobot(System.getProperty("bot.token"), lang(System.getProperty("bot.cloud.lang", "en"))).apply {
        session.addMessagePostedListener { posted, session ->
            if (posted.messageContent?.startsWith(me) ?: false) {
                Wobot.logger.info("Start processing message: ${posted.timestamp}")
                parseCommand(posted)(this)
            }
        }
    }
}

fun wc(language: Language, minWordSize: Int, input: List<String>): WordCloud {
    val frequencyAnalyzer = FrequencyAnalyzer().apply {
        setWordTokenizer {
            language.wordTokenizer.tokenize(it)
        }
        setWordFrequenciesToReturn(160)
        setMinWordLength(minWordSize)
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
