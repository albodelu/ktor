package io.ktor.client.engine.curl

class CurlHttpRequestException(cause: String) : IllegalStateException(cause)

class CurlIllegalStateException(cause: String) : IllegalStateException(cause)

class CurlEngineCreationException(cause: String) : IllegalStateException(cause)

class CurlUnsupportedProtocolException(protocolId: UInt) : IllegalArgumentException("Unsupported protocol $protocolId")
