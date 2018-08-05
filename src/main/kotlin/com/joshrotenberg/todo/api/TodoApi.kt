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
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.request.ApplicationRequest
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.patch
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.routing.routing
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val DEFAULT_ENDPOINT = "/todo"

val LOG: Logger = LoggerFactory.getLogger("todo-ktor-app")

fun Todo.withUrl(request: ApplicationRequest): Todo {
    val local = request.local
    this.url = "${local.scheme}://${local.host}$DEFAULT_ENDPOINT/${this.id}"
    return this
}

fun Application.main() {

    val todos = TodoRepository()
    todos.init()

    install(AutoHeadResponse)
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Delete)
        method(HttpMethod.Patch)
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
        route(DEFAULT_ENDPOINT) {
            post("/") {
                try {
                    val todo = call.receive<Todo>()
                    LOG.info("Creating ${todo}")
                    val id = Todo.generateId()
                    todos.createTodo(id, title = todo.title, order = todo.order, completed = todo.completed)
                    call.respond(HttpStatusCode.Created, todo.copy(id = id))
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

            get("/{id}") {

                val id = call.parameters["id"] ?: throw IllegalArgumentException("No id in path")
                try {
                    LOG.info("Fetching todo $id")
                    val todo = todos.getTodo(id).withUrl(call.request)
                    call.respond(todo)
                } catch (e: java.util.NoSuchElementException) {
                    call.respond(HttpStatusCode.NotFound)
                } catch (e: Exception) {
                    LOG.error("Caught some other error: ${e}")
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            get("/") {
                LOG.info("Fetching all todos")
                call.respond(todos.getTodos().map { t -> t.withUrl(call.request) })
            }

            patch("/{id}") {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("No id in path")
                try {
                    LOG.info("Updating todo $id")
                    val todo = call.receive<Todo>()
                    todos.updateTodo(id, title = todo.title, order = todo.order, completed = todo.completed)
                    call.respond(HttpStatusCode.OK, todos.getTodo(id).withUrl(call.request))
                } catch (e: java.util.NoSuchElementException) {
                    call.respond(HttpStatusCode.NotFound)
                } catch (e: Exception) {
                    LOG.error("Caught some other error: ${e}")
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"] ?: throw IllegalArgumentException("No id in path")
                try {
                    LOG.info("Deleting todo $id")
                    when (todos.deleteTodo(id)) {
                        1 -> call.respond(HttpStatusCode.OK)
                        0 -> call.respond(HttpStatusCode.NotFound)
                        else -> throw IllegalArgumentException("Invalid id")
                    }
                } catch (e: Exception) {
                    LOG.error("Caught some other error: ${e}")
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }

            delete("/") {
                try {
                    LOG.info("Deleting all todos")
                    val numDeleted = todos.deleteTodos()
                    LOG.info("Deleted all $numDeleted todos")
                    call.respond(HttpStatusCode.OK)
                } catch (e: Exception) {
                    LOG.error("Caught some other error: ${e}")
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
        }
    }
}
