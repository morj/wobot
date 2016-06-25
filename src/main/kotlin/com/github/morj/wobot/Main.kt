package com.github.morj.wobot

import com.google.common.io.CharStreams
import com.kennycason.kumo.CollisionMode
import com.kennycason.kumo.WordCloud
import com.kennycason.kumo.font.scale.LinearFontScalar
import com.kennycason.kumo.nlp.FrequencyAnalyzer
import com.kennycason.kumo.palette.ColorPalette
import com.ullink.slack.simpleslackapi.SlackChannel
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
    session.addMessagePostedListener { posted, session ->
        if (posted.sender.userName == "morj") {
            // session.sendMessage(posted.channel, "Morj wrote: \n> ${posted.messageContent}", null)
            try {
                val os = PipedOutputStream()
                val wordCloud = wc(posted.messageContent)
                // Wobot::class.java.getResourceAsStream("bear.jpeg")
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
                LOGGER.error("Cannot render image", t)
            }
        }
    }
}

fun wc(input: String): WordCloud {
    val frequencyAnalyzer = FrequencyAnalyzer()
    frequencyAnalyzer.setWordFrequenciesToReturn(300)
    frequencyAnalyzer.setMinWordLength(4)
    frequencyAnalyzer.setStopWords(listOf("this"))

    val wordFrequencies = frequencyAnalyzer.load(input.split(' '))
    val dimension = Dimension(500, 312)
    return WordCloud(dimension, CollisionMode.PIXEL_PERFECT).apply {
        setPadding(2)
        // setBackground(PixelBoundryBackground(getInputStream("backgrounds/whale_small.png")))
        setColorPalette(ColorPalette(Color(0x4055F1), Color(0x408DF1), Color(0x40AAF1), Color(0x40C5F1), Color(0x40D3F1), Color(0xFFFFFF)))
        setFontScalar(LinearFontScalar(10, 40))
        build(wordFrequencies)
        // writeToFile("output/whale_wordcloud_small.png")
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
