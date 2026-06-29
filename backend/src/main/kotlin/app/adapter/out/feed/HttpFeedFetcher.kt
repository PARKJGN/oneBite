package app.adapter.out.feed

import app.application.port.out.FeedFetcher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class HttpFeedFetcher : FeedFetcher {
    private val log = LoggerFactory.getLogger(javaClass)
    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()

    override fun fetch(url: String): String? = try {
        val req = HttpRequest.newBuilder(URI(url))
            .timeout(Duration.ofSeconds(10))
            .header("User-Agent", "oneBite/1.0 (+newsletter)")
            .GET().build()
        val res = client.send(req, HttpResponse.BodyHandlers.ofString())
        if (res.statusCode() in 200..299) res.body() else { log.warn("RSS HTTP {} {}", res.statusCode(), url); null }
    } catch (ex: Exception) {
        log.warn("RSS HTTP 실패 {}: {}", url, ex.message); null
    }
}
