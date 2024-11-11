package com.github.masx200.fast_and_mercury_router_reverse_proxy_server

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import io.ktor.client.*
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.compression.compress
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.utils.io.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.mozilla.universalchardet.UniversalDetector

/**
 * The main entry point of the application. This application starts a webserver at port 8080 based on Netty.
 * It intercepts all the requests, reverse-proxying them to 上游服务器.
 *
 * In the case of HTML it is completely loaded in memory and preprocessed to change URLs to our own local domain.
 * In the case of other files, the file is streamed from the HTTP client to the HTTP server response.
 */
fun main(args: Array<String>) {

    HelloCommand {
        println(it)
        val server = embeddedServer(Netty, port = it.port.toInt(), module = createApp(it.upstream))
        // Starts the server and waits for the engine to stop and exits.
        server.start(wait = true)
    }.main(args)
    // Creates a Netty server

}


class HelloCommand(val callback: (options: HelloCommand) -> Unit) : CliktCommand() {
    override fun run() {
        callback(this)
    }

    override fun toString(): String {
        return "HelloCommand(upstream='$upstream', port='$port')" + super.toString()
    }


    val upstream: String by option("-u", "--upstream", help = "upstream").required()
    val port: String by option("-p", "--port", help = "port").required()


}

const val scriptcontent = """
    Object.defineProperty(window, "pageRedirect", {
    value: function() {},
})
"""
fun createApp(upstream: String): Application.() -> Unit {

    return {
        install(Compression) {
            gzip()
        }
// Creates a new HttpClient
        val client = HttpClient {
            install(ContentEncoding) {
                gzip()

            }
        }
//    val 上游服务器Lang = "en"

        // Let's intercept all the requests at the [ApplicationCallPipeline.Call] phase.
        intercept(ApplicationCallPipeline.Call) {
            val targetUrl = upstream + call.request.uri
            // We create a GET request to the 上游服务器 domain and return the call (with the request and the unprocessed response).
            val originalRequestBody = call.receiveChannel().toByteArray()

//            val request = HttpRequestBuilder().apply {
//                method = call.request.httpMethod
//                url(targetUrl)
//                headers.appendAll(call.request.headers)
//                headers["host"] = URL(targetUrl).host
//                setBody(originalRequestBody)
//            }
//            println(call.request.httpMethod)

//            println(call.request.host())
            println(call.request.origin)
//            println(call.request.uri)
            val headersBuilder = HeadersBuilder()
            headersBuilder.appendAll(call.request.headers)
            val message = headersBuilder.build()
            println(message)

            val response = client.request(targetUrl) {
                method = call.request.httpMethod
//
//                headers.appendAll()
                headers.clear()
//                for (header in message){}
                message.forEach { key, values ->
                    values.forEach { value ->
//                        println("$key: $value")
                        headers.append(key, value)
                    }
                }
                (targetUrl).toHttpUrl().let { it1 -> headers["host"] = it1.host }
                headers["Accept-Encoding"] = "gzip"
//                println( headers["host"])
                compress("gzip")
                setBody(originalRequestBody)
            }
//            val response = client.request(upstream + call.request.uri) {
//                method = call.request.httpMethod
//                headers.appendAll(call.request.headers)
////                body=call.request.body
//            }
//        val response = client.request("https://$上游服务器Lang.上游服务器.org${call.request.uri}")

            // Get the relevant headers of the client response.
            val proxiedHeaders = response.headers
//        val location = proxiedHeaders[HttpHeaders.Location]
            val contentType = proxiedHeaders[HttpHeaders.ContentType]
            val contentLength = proxiedHeaders[HttpHeaders.ContentLength]
            println(response.status)
            println(proxiedHeaders)
            // Extension method to process all the served HTML documents
//        fun String.strip上游服务器Domain() = this.replace(Regex("(https?:)?//\\w+\\.上游服务器\\.org"), "")

            // Propagates location header, removing the 上游服务器 domain from it
//        if (location != null) {
//            call.response.header(HttpHeaders.Location, location.strip上游服务器Domain())
//        }

            // Depending on the ContentType, we process the request one way or another.
            when {
                // In the case of HTML we download the whole content and process it as a string replacing
                // 上游服务器 links.
                response.status == HttpStatusCode.OK && contentType?.startsWith("text/html") == true -> {
                    val textarray = response.bodyAsBytes()
                    val charset = detectCharset(textarray)
                    println("Detected charset: $charset")

                    // Decode the byte array using the detected charset
                    val decodedString = decodeString(textarray, charset)
//                    println("Decoded string: $decodedString")
//                    println(textarray)
                    val insertscriptintohtmlhead = insertscriptintohtmlhead(decodedString, scriptcontent)

                    val filteredText = insertscriptintohtmlhead //.strip上游服务器Domain()

//                    println(filteredText)
                    call.respond(
                        TextContent(
                            filteredText,
                            ContentType.Text.Html.withCharset(Charsets.UTF_8),
                            response.status
                        )
                    )
                }

                else -> {
                    // In the case of other content, we simply pipe it. We return a [OutgoingContent.WriteChannelContent]
                    // propagating the contentLength, the contentType and other headers, and simply we copy
                    // the ByteReadChannel from the HTTP client response, to the HTTP server ByteWriteChannel response.
                    call.respond(object : OutgoingContent.WriteChannelContent() {
                        override val contentLength: Long? = contentLength?.toLong()
                        override val contentType: ContentType? = contentType?.let { ContentType.parse(it) }
                        override val headers: Headers = Headers.build {
                            appendAll(proxiedHeaders.filter { key, _ ->
                                !key.equals(
                                    HttpHeaders.ContentType,
                                    ignoreCase = true
                                ) && !key.equals(HttpHeaders.ContentLength, ignoreCase = true)
                            })
                        }
                        override val status: HttpStatusCode = response.status
                        override suspend fun writeTo(channel: ByteWriteChannel) {
                            response.bodyAsChannel().copyAndClose(channel)
                        }
                    })
                }
            }
        }
    }
}


fun insertscriptintohtmlhead(htmltext: String, scripttext: String): String {
    val doc: Document = Jsoup.parse(htmltext, "", Parser.xmlParser())
    val head = doc.head()
    val script = doc.createElement("script")
    script.attr("type", "text/javascript")
    script.text(scripttext)
    head.prependChild(script)

    return (doc.html())
}

fun detectCharset(bytes: ByteArray): String? {
    val detector = UniversalDetector(null)

    // Feed the bytes to the detector
    detector.handleData(bytes, 0, bytes.size)
    detector.dataEnd()

    // Get the detected encoding
    val encoding = detector.detectedCharset
    detector.reset()

    return encoding
}

fun decodeString(bytes: ByteArray, charset: String?): String {
    return if (charset != null && charset.isNotEmpty()) {
        String(bytes, charset(charset))
    } else {
        // If no encoding is detected, fall back to UTF-8
        String(bytes, Charsets.UTF_8)
    }
}