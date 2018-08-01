package com.joshrotenberg.todo.api

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.joshrotenberg.todo.model.Todo
import com.joshrotenberg.todo.repository.TodoRepository
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class TodoApiTest {

    private val gson = GsonBuilder().create()

    private val r = TodoRepository()

    @Before
    fun before() {
        r.init()
    }

    @After
    fun after() {
        r.drop()
    }

    fun Todo.toJson(): String {
        return gson.toJson(this, Todo::class.java)
    }

    @Test
    fun `add a todo`() = withTestApplication(Application::main) {
        val todo = addTodo(Todo(title = "do stuff", order = 1, completed = false))
        val responseTodo = getTodo(todo.id!!)

        assertEquals(todo, responseTodo)
    }

    @Test
    fun `fetch all todos`() = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Get, "/todo") {
            addHeader("Accept", "application/json")
        }.response.let {
            val empty = gson.fromJson(it.content, List::class.java)
            assertEquals(0, empty.count())
        }

        val todo0 = addTodo(Todo(title = "get milk", order = 1, completed = false))
        val todo1 = addTodo(Todo(title = "get eggs", order = 2, completed = false))

        handleRequest(HttpMethod.Get, "/todo/") {
            addHeader("Accept", "application/json")
        }.response.let {
            val todosType = object : TypeToken<List<Todo>>() {}.type

            val todos = gson.fromJson<List<Todo>>(it.content, todosType)

            assertEquals(2, todos.count())

            assertEquals(todo0.title, todos[0].title)
            assertEquals(todo0.order, todos[0].order)
            assertEquals(todo0.completed, todos[0].completed)

            assertEquals(todo1.title, todos[1].title)
            assertEquals(todo1.order, todos[1].order)
            assertEquals(todo1.completed, todos[1].completed)
        }
    }

    @Test
    fun `delete a todo`() = withTestApplication(Application::main) {

        val todo0 = addTodo(Todo(title = "get milk", order = 1, completed = false))
        val todo1 = addTodo(Todo(title = "get eggs", order = 2, completed = false))

        assertEquals(2, getTodos().count())

        with(handleRequest(HttpMethod.Delete, "/todo/${todo0.id}")) {
            assertEquals(HttpStatusCode.NoContent, response.status())
        }
        assertEquals(1, getTodos().count())

        with(handleRequest(HttpMethod.Get, "/todo/${todo0.id}")) {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }

        with(handleRequest(HttpMethod.Delete, "/todo/${todo1.id}")) {
            assertEquals(HttpStatusCode.NoContent, response.status())
        }
        assertEquals(0, getTodos().count())

        with(handleRequest(HttpMethod.Get, "/todo/${todo1.id}")) {
            assertEquals(HttpStatusCode.NotFound, response.status())
        }
    }


    private fun TestApplicationEngine.getTodo(id: String): Todo {
        handleRequest(HttpMethod.Get, "/todo/$id") {
            addHeader("Accept", "application/json")
        }.response.let {
            assertEquals(it.status(), HttpStatusCode.OK)
            return gson.fromJson<Todo>(it.content, Todo::class.java).withUrl(it.call.request)
        }
    }

    private fun TestApplicationEngine.getTodos(): List<Todo> {
        handleRequest(HttpMethod.Get, "/todo/") {
            addHeader("Accept", "application/json")
        }.response.let {
            assertEquals(it.status(), HttpStatusCode.OK)
            val todosType = object : TypeToken<List<Todo>>() {}.type
            return gson.fromJson<List<Todo>>(it.content, todosType)
        }
    }

    private fun TestApplicationEngine.addTodo(todo: Todo): Todo {
        handleRequest(HttpMethod.Post, "/todo") {
            setBody(todo.toJson())
            addHeader("Content-type", "application/json")
            addHeader("Accept", "application/json")
        }.response.let {
            assertEquals(it.status(), HttpStatusCode.Created)
            return gson.fromJson<Todo>(it.content, Todo::class.java).withUrl(it.call.request)
        }
    }

    private fun TestApplicationEngine.runPost(todoJson: String): TestApplicationCall {
        return handleRequest(HttpMethod.Post, "/todo") {
            setBody(todoJson)
            addHeader("Content-type", "application/json")
            addHeader("Accept", "application/json")
        }
    }

    private fun TestApplicationEngine.runGet(id: String): TestApplicationCall {
        return handleRequest(HttpMethod.Get, "/todo/$id") {
            addHeader("Accept", "application/json")
        }
    }
}