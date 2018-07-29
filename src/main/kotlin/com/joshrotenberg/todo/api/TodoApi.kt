package com.joshrotenberg.todo.api

import com.joshrotenberg.todo.model.Todo
import com.joshrotenberg.todo.repository.TodoRepository
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val LOG: Logger = LoggerFactory.getLogger("todo-ktor-app")

infix fun Todo.withUrl(request: ApplicationRequest): Todo {
    val local = request.local
    this.url = "${local.scheme}://${local.host}:${local.port}/${this.id}"
    return this
}

fun Application.main() {

    val todos = TodoRepository()
    todos.init()

    install(AutoHeadResponse)
    install(CORS) {
        method(HttpMethod.Options)
        header(HttpHeaders.Range)
        anyHost()
        allowCredentials = true
    }
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            //            Converters.registerDateTime(this)
            setPrettyPrinting()
        }
    }

    routing {
        post("/todo") {
            try {
                val todo = call.receive<Todo>()
                LOG.info("creating ${todo}")
//            if (person.name.isNullOrBlank() || person.email.isNullOrBlank() || person.date == null) {
//                call.respond(UnprocessableEntity)
//            } else {
                val id = Todo.generateId()
                todos.createTodo(id, todo.title, todo.order, todo.completed)
                call.respond(HttpStatusCode.Created, todo.copy(id = id))
//            }
            } catch (e: com.google.gson.JsonSyntaxException) {
                LOG.error("Syntax error in JSON")
                call.respond(HttpStatusCode.BadRequest)
            } catch (e: java.lang.IllegalArgumentException) {
                LOG.error(e.message)
                call.respond(HttpStatusCode.BadRequest)
            } catch (e: Exception) {
                LOG.error("Caught some other error: ${e}")
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("/todo/{id}") {

            val id = call.parameters["id"] ?: throw IllegalArgumentException("No id in path")
            try {
                val todo = todos.getTodo(id).withUrl(call.request)
                LOG.info("hi $todo ")
                call.respond(todo)
            } catch (e: java.util.NoSuchElementException) {
                call.respond(HttpStatusCode.NotFound)
            } catch (e: Exception) {
                LOG.error("Caught some other error: ${e}")
                call.respond(HttpStatusCode.InternalServerError)
            }
        }

        get("/todo") {
            LOG.info("Fetching all todos")
            call.respond(todos.getTodos())
        }


    }
}
