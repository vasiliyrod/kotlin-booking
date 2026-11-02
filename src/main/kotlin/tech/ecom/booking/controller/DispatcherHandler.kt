package controller

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import io.github.oshai.kotlinlogging.KotlinLogging

class DispatcherHandler(private val router: Router) : HttpHandler {
    private val logger = KotlinLogging.logger { }

    override fun handle(exchange: HttpExchange) {
        logger.info { "${exchange.requestMethod} ${exchange.requestURI}" }

        val routed = router.route(exchange)
        if (!routed) {
            val response = "404 Not Found"
            exchange.sendResponseHeaders(404, response.length.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
    }
}