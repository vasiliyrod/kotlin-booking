package utils

import com.sun.net.httpserver.HttpExchange

typealias Handler = (HttpExchange) -> Unit