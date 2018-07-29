package com.joshrotenberg.todo

import com.joshrotenberg.todo.api.main
import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main(args: Array<String>) {
    val port: String = System.getenv("PORT") ?: "8080"
    embeddedServer(Netty, port.toInt(), module = Application::main).start(wait = true)
}
