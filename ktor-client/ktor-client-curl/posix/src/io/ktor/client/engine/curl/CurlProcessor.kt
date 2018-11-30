package io.ktor.client.engine.curl

import kotlin.native.concurrent.*


// Only set in curl thread
@ThreadLocal
private var curlState: CurlState? = null

internal class CurlProcessor : WorkerProcessor<CurlRequest, CurlResponse>() {
    fun start() {
        worker.execute(TransferMode.SAFE, { "dummy" }) {
            check(curlState == null)
            curlState = CurlState()
        }
    }

    fun requestJob(request: CurlRequest) {
        pendingFutures += worker.execute(TransferMode.SAFE, { request.freeze() }, ::curlUpdate)
    }

    fun close() {
        worker.execute(TransferMode.SAFE, { "dummy" }) { curlState!!.close() }
    }
}

internal fun curlUpdate(request: CurlRequest): CurlResponse {
    request.newRequests.forEach {
        curlState!!.setupEasyHandle(it)
    }

    val readyResponses = curlState!!.singleIteration(100)
    return CurlResponse(readyResponses, request.listenerKey).freeze()
}
