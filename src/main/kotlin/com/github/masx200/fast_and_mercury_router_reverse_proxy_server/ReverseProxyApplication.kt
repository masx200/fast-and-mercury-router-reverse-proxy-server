package com.github.masx200.fast_and_mercury_router_reverse_proxy_server

import io.ktor.client.HttpClient
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.compression.compress
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.withCharset
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.compression.gzip
import io.ktor.server.plugins.origin
import io.ktor.server.request.httpMethod
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.util.filter
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.toByteArray
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.mozilla.universalchardet.UniversalDetector

/**
 * 该函数定义了一个Kotlin主程序，主要功能如下：
 *
 * 1. **创建命令行接口**：通过`HelloCommand`创建一个命令行接口，并传递参数`name`。
 * 2. **处理命令行参数**：使用`it`接收命令行参数，并打印出来。
 * 3. **启动嵌入式服务器**：使用`embeddedServer`创建一个CIO（Coroutine I/O）服务器，指定端口和模块。
 * 4. **启动并等待**：调用`server.start(wait = true)`启动服务器并等待其停止。
 * 5. **执行命令行**：最后调用`HelloCommand`的`main`方法，处理命令行参数。
 *
 *  The main entry point of the application. This application starts a webserver at port 8080 based on Netty.
 * It intercepts all the requests, reverse-proxying them to 上游服务器.
 *
 * In the case of HTML it is completely loaded in memory and preprocessed to change URLs to our own local domain.
 * In the case of other files, the file is streamed from the HTTP client to the HTTP server response.
 *
 */
fun main(args: Array<String>) {

    HelloCommand(name = "fast-and-mercury-router-reverse-proxy-server") {
        println(it)
        val server = embeddedServer(CIO, port = it.port.toInt(), module = createApp(it.upstream))
        // Starts the server and waits for the engine to stop and exits.
        server.start(wait = true)
    }.main(args)
    // Creates a Netty server

}

/**这段Kotlin代码定义了一个常量 scriptcontent，其值是一个JavaScript脚本字符串。该脚本尝试在浏览器的全局对象 window 上定义一个名为 pageRedirect 的属性，该属性的值是一个空函数。如果执行过程中出现错误，则会捕获异常并将其打印到控制台。*/
const val scriptcontent = """
try {
  Object.defineProperty(window, "pageRedirect", {
    value: function () {},
  });
} catch (e) {
  console.log(e);
}

"""
/**
该Kotlin函数`createApp`用于创建一个Ktor应用配置块，主要功能是代理请求到指定的上游服务器，并处理响应。具体功能如下：

1. **安装压缩插件**：为应用和HTTP客户端安装GZIP压缩支持。
2. **拦截请求**：在应用调用管道的`Call`阶段拦截所有请求。
3. **构建新URL**：从原始请求中提取上游服务器的URL，并构建新的目标URL。
4. **转发请求**：使用HTTP客户端向目标URL发送请求，并复制原始请求的所有头部信息。
5. **处理响应**：
   - **HTML响应**：如果响应状态码为200且内容类型为HTML，则解码响应体，插入脚本，并返回处理后的HTML。
   - **其他响应**：直接将响应内容传递给客户端，包括内容长度、内容类型和其他头部信息。

6. **日志输出**：在关键步骤中打印日志，便于调试。*/
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
            val originalUrl = upstream.toHttpUrl()


            // 移除路径部分
            val newUrl = HttpUrl.Builder()
                .scheme(originalUrl.scheme)
                .host(originalUrl.host)
                .port(originalUrl.port) // 如果原 URL 没有指定端口，则可以省略这一步
                .build();

            val targetUrl = newUrl.toString() + call.request.uri.slice(1..call.request.uri.length - 1)
            println(targetUrl)
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
            val messageheaders = headersBuilder.build()
            println(messageheaders)

            val toHttpUrltarget = (targetUrl).toHttpUrl()


            val response: HttpResponse = try {
                client.request(targetUrl) {
                method = call.request.httpMethod
//
//                headers.appendAll()
                headers.clear()
//                for (header in message){}
                messageheaders.forEach { key, values ->
                    values.forEach { value ->
//                        println("$key: $value")
                        headers.append(key, value)
                    }
                }
                toHttpUrltarget.let { it1 -> headers["host"] = it1.host }
                headers["Accept-Encoding"] = "gzip"
//                println( headers["host"])
                compress("gzip")
                setBody(originalRequestBody)
            }
            } catch (e: Exception) {
                println(e.toString())
                e.printStackTrace()
                // 捕获异常并返回HTTP 502错误
                call.respond(HttpStatusCode.BadGateway, "上游服务器返回错误: ${e.toString()}")
                return@intercept
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
                    val insertscriptintohtmlhead = { insertscriptintohtmlhead(decodedString, scriptcontent) }

                    val filteredText = if (decodedString.contains("</head>") && decodedString.contains("</html>")

                        &&
                        decodedString.contains("<head") && decodedString.contains("<html")

                    ) insertscriptintohtmlhead()
                    else decodedString
                    //.strip上游服务器Domain()

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

/**
 * 将脚本插入到HTML头部
 *
 * 此函数的目的是将给定的脚本文本插入到给定HTML文本的头部中，以便在网页加载时首先执行该脚本
 * 它使用Jsoup库来解析HTML并操作DOM，以确保脚本正确地插入到头部
 *
 * @param htmltext 包含HTML内容的字符串，这是原始的HTML文本
 * @param scripttext 需要插入到HTML头部的脚本内容
 * @return 返回修改后的HTML字符串，其中包含了插入头部的脚本
 */
fun insertscriptintohtmlhead(htmltext: String, scripttext: String): String {
    val doc: Document = Jsoup.parse(htmltext, "", Parser.xmlParser())
    val head = doc.head()
    val script = doc.createElement("script")
    script.attr("type", "text/javascript")
    script.text(scripttext)
    head.prependChild(script)

    return (doc.html())
}
/**
 * Detects the character encoding of a byte array.
 * Uses the UniversalDetector tool to detect the character encoding of a given byte array.
 *
 * @param bytes The byte array whose character encoding is to be detected.
 * @return Returns the detected character encoding, or null if detection fails.
 */
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
/**
 * Decodes a byte array into a string using the specified character set.
 *
 * This function aims to convert a given byte array into a string. If a character set is provided and is not empty,
 * it will attempt to decode using the provided character set; otherwise, it will default to using UTF-8 encoding for decoding.
 *
 * @param bytes The byte array to be decoded, representing the encoded string data.
 * @param charset The character set to use for decoding, which may be null or empty.
 * @return The decoded string, or the string obtained using the default UTF-8 encoding if no character set is provided.
 */
fun decodeString(bytes: ByteArray, charset: String?): String {
    return if (charset != null && charset.isNotEmpty()) {
        String(bytes, charset(charset))
    } else {
        // If no encoding is detected, fall back to UTF-8
        String(bytes, Charsets.UTF_8)
    }
}