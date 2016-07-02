package com.github.morj.wobot

import com.google.common.io.CharStreams
import com.kennycason.kumo.WordCloud
import com.ullink.slack.simpleslackapi.SlackChannel
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.entity.mime.content.InputStreamBody
import org.apache.http.entity.mime.content.StringBody
import org.apache.http.impl.client.HttpClientBuilder
import java.awt.Point
import java.io.InputStream
import java.io.InputStreamReader

fun sumAtLeast(tolerance: Int, vararg f: () -> Int): Int {
    var result = 0
    for (i in 0..f.size - 1) {
        result += f[i]()
        if (result > tolerance) {
            break
        }
    }
    return result
}

operator fun Point.div(other: Int): Point = Point(x / other, y / other)

fun sendFile(stream: InputStream, channel: SlackChannel, token: String) {
    try {
        val client = HttpClientBuilder.create().build()
        val post = HttpPost("https://slack.com/api/files.upload")
        val entity = MultipartEntityBuilder.create().apply {
            addPart("file", InputStreamBody(stream, "woooo"))
            addPart("channels", StringBody(channel.id, ContentType.MULTIPART_FORM_DATA))
            addPart("token", StringBody(token, ContentType.MULTIPART_FORM_DATA))
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

fun data(path: String) = InputStreamReader(Wobot::class.java.getResourceAsStream("$path.txt")).readLines()

fun test(input: List<String>, path: String): WordCloud {
    return wc(input).apply {
        writeToFile("$path.png")
    }
}
