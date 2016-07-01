package com.github.morj.wobot

import com.google.common.io.CharStreams
import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.font.FontWeight
import com.kennycason.kumo.font.KumoFont
import com.kennycason.kumo.image.AngleGenerator
import com.kennycason.kumo.nlp.FrequencyAnalyzer
import com.kennycason.kumo.palette.ColorPalette
import com.ullink.slack.simpleslackapi.SlackChannel
import com.ullink.slack.simpleslackapi.impl.ChannelHistoryModuleFactory
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType.MULTIPART_FORM_DATA
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.HttpClientBuilder
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.io.InputStream
import java.io.InputStreamReader
import java.io.PipedInputStream
import java.io.PipedOutputStream

class Wobot() // handle

val TOKEN = System.getProperty("bot.token")

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
                val wordCloud = wc(text.toString())
                Thread {
                    try {
                        wordCloud.writeToStreamAsPNG(os)
                        os.close()
                    } catch (t: Throwable) {
                        LOGGER.error("Cannot write image", t)
                    }
                }.start()
                sendFile(PipedInputStream(os), posted.channel)
            } catch (t: Throwable) {
                LOGGER.error("Cannot prepare image", t)
            }
        }
    }
}


fun wc(input: String): WordCloud {
    val frequencyAnalyzer = FrequencyAnalyzer().apply {
        setWordFrequenciesToReturn(160)
        setMinWordLength(4)
        setStopWords(listOf("this"))
    }
    val wordFrequencies = frequencyAnalyzer.load(listOf(input))
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

fun sendFile(stream: InputStream, channel: SlackChannel) {
    try {
        val client = HttpClientBuilder.create().build()
        val post = HttpPost("https://slack.com/api/files.upload")
        val entity = MultipartEntityBuilder.create().apply {
            addPart("file", InputStreamBody(stream, "woooo"))
            addPart("channels", StringBody(channel.id, MULTIPART_FORM_DATA))
            addPart("token", StringBody(TOKEN, MULTIPART_FORM_DATA))
        }.build()
        LOGGER.info("Post file to channel ${channel.id}")
        post.entity = entity
        val result = client.execute(post)
        val json = CharStreams.toString(InputStreamReader(result.entity.content))
        LOGGER.info(json)
    } catch (t: Throwable) {
        LOGGER.error("Cannot attach file", t)
    }
}

fun wcc2(input: List<String>, path: String): WordCloud {
    val frequencyAnalyzer = FrequencyAnalyzer().apply {
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
        writeToFile("$path.png")
    }
}

/*fun wc(input: String): WordCloud {
    val frequencyAnalyzer = FrequencyAnalyzer().apply {
        setWordFrequenciesToReturn(160)
        setMinWordLength(4)
        setStopWords(listOf("this"))
    }
    val wordFrequencies = frequencyAnalyzer.load(input.split(' '))
    val height = 360
    val dimension = Dimension(height * 2, height * 2)
    return WordCloud(dimension, CollisionMode.PIXEL_PERFECT).apply {
        setPadding(2)
        setBackground(CircleBackground(height))
        setKumoFont(KumoFont("Impact", FontWeight.PLAIN))
        setBackgroundColor(Color.WHITE)
        setAngleGenerator(AngleGenerator(-60.0, 60.0, 3))
        setColorPalette(ColorPalette(listOf(
                0xFA6C07, 0xFF7614, 0xFF8936, 0x006400, 0x080706, 0x3B3029, 0x3B3029
        ).map { Color(it) }))
        setFontScalar(LinearFontScalar(6, 60))
        setWordPlacer(RTreeWordPlacer())
        build(wordFrequencies)
        // writeToFile("output/whale_wordcloud_small.png")
    }
}*/
