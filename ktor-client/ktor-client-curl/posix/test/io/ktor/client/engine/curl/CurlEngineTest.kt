import io.ktor.client.*
import io.ktor.client.engine.curl.*
import io.ktor.client.features.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlin.test.*

@Test
fun test() = runBlocking {
    val client = HttpClient(Curl) {
        install(feature = Logging, configure = {
            level = LogLevel.ALL
        })
    }

    client.request<String>(urlString = "https://httpbin.org/get") {
        method = HttpMethod.Get
    }

    client.request<String>(urlString = "https://httpbin.org/put") {
        method = HttpMethod.Put
        body = "mike check"
    }

    client.request<String>(urlString = "https://httpbin.org/post") {
        method = HttpMethod.Post
        body = "one two three"
    }

    client.close()
}
