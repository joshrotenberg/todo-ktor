package com.joshrotenberg.todo.api

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.joshrotenberg.todo.model.Todo
import com.joshrotenberg.todo.repository.TodoRepository
import io.ktor.application.Application
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

//inline fun <reified T> Gson.fromJson(json: String) = this.fromJson<T>(json, object: TypeToken<T>() {}.type)

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
    fun `test adding a todo`() = withTestApplication(Application::main) {
        val todo = Todo(title = "do stuff", order = 1, completed = false).toJson()

        with(runPost(todo)) {
            assertEquals(response.status(), HttpStatusCode.Created)
            val t = gson.fromJson<Todo>(response.content, Todo::class.java).withUrl(request)

            with(runGet(t.id!!)) {
                assertEquals(response.status(), HttpStatusCode.OK)
                assertEquals(t, gson.fromJson<Todo>(response.content, Todo::class.java).withUrl(request))
            }
        }
    }

    @Test
    fun `qwekjn`() = withTestApplication(Application::main) {
        val emptyTodo = ""
        with(runPost(emptyTodo)) {
            println(response.status())
        }
    }

    @Test
    fun `test fetching all todos`() = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Get, "/todo") {
            addHeader("Accept", "application/json")
        }.response.let {
            val empty = gson.fromJson(it.content, List::class.java)
            assertEquals(0, empty.count())
        }

        val todo0 = Todo(title = "get milk", order = 1, completed = false)
        val todo1 = Todo(title = "get eggs", order = 2, completed = false)

        runPost(todo0.toJson())
        runPost(todo1.toJson())

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

    private fun TestApplicationEngine.runGet(id: String): TestApplicationCall {
        return handleRequest(HttpMethod.Get, "/todo/$id") {
            addHeader("Accept", "application/json")
        }
    }

    private fun TestApplicationEngine.runPost(todoJson: String): TestApplicationCall {
        return handleRequest(HttpMethod.Post, "/todo") {
            setBody(todoJson)
            addHeader("Content-type", "application/json")
            addHeader("Accept", "application/json")
        }
    }
}