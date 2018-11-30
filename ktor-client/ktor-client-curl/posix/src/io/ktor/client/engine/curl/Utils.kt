package io.ktor.client.engine.curl

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.utils.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.cinterop.*
import kotlinx.coroutines.*
import kotlinx.coroutines.io.*
import kotlinx.io.charsets.*
import kotlinx.io.core.*
import platform.posix.*
import kotlin.native.concurrent.*

internal fun ByteArray.copyToBuffer(buffer: CPointer<ByteVar>, size: ULong, position: Int = 0) {
    usePinned { pinned ->
        memcpy(buffer, pinned.addressOf(position), size)
    }
}

internal inline fun <T : Any> T.asStablePointer() = StableRef.create(this).asCPointer()

internal inline fun <reified T : Any> COpaquePointer.fromCPointer(): T = asStableRef<T>().get()

internal fun List<ByteArray>.parseResponseHeaders(charset: Charset = Charsets.UTF_8): Headers {
    val headers = mutableMapOf<String, MutableList<String>>()

    forEach { byteArray ->
        val header = String(byteArray, charset = charset)
        if (!header.contains(':')) {
            println("WARNING: $header")
            return@forEach
        }

        // TODO: We need a better way to parse headers
        // TODO: And also we need a support for multiline headers.
        val (key, value) = header.split(":", limit = 2)
        val valueList: MutableList<String> = headers.getOrPut(key) { mutableListOf() }
        valueList.add(value.trim(' ', '\n'))
    }
    return HeadersImpl(headers)
}

internal fun HttpRequest.toCurlRequest(): CurlRequest {
    val curlRequestData = CurlRequestData(
        url = url.toString(),
        method = method.value,
        headers = headersToCurl(this),      // TODO: curl_slist_free_all(headerList)
        content = content.toCurlByteArray()
    )

    return CurlRequest(listOf(curlRequestData), ListenerKey().freeze())
}

internal fun OutgoingContent.toCurlByteArray(): ByteArray? = runBlocking {
    when (this@toCurlByteArray) {
        is OutgoingContent.ByteArrayContent -> bytes()
        is OutgoingContent.WriteChannelContent -> writer(coroutineContext) {
            writeTo(channel)
        }.channel.readRemaining().readBytes()
        is OutgoingContent.ReadChannelContent -> readFrom().readRemaining().readBytes()
        is OutgoingContent.NoContent -> null
        else -> throw UnsupportedContentTypeException(this@toCurlByteArray)
    }
}
