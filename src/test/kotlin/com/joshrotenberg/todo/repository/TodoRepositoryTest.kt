package com.joshrotenberg.todo.repository

import com.joshrotenberg.todo.model.Todo
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TodoRepositoryTest {

    // create the repo once but call init/drop before/after each test
    private val r = TodoRepository()

    @Before
    fun before() {
        r.init()
    }

    @After
    fun after() {
        r.drop()
    }

    @Test
    fun `create and get a todo`() {
        val id = Todo.generateId()
        r.createTodo(id, "do stuff", 1, false)

        val todo = r.getTodo(id)
        assertEquals(id, todo.id)
        assertEquals("do stuff", todo.title)
        assertEquals(1, todo.order)
        assertEquals(false, todo.completed)
    }

    @Test
    fun `create and update a todo`() {
        val id = Todo.generateId()
        r.createTodo(id, "do stuff", 1, false)

        val todo = r.getTodo(id)
        assertEquals(id, todo.id)
        assertEquals("do stuff", todo.title)
        assertEquals(1, todo.order)
        assertEquals(false, todo.completed)

        r.updateTodo(todo.id!!, completed = true)

        val todoUpdate = r.getTodo(id)
        assertEquals(id, todoUpdate.id)
        assertEquals(true, todoUpdate.completed)

        r.updateTodo(todo.id!!, title = "do more stuff")

        val todoUpdate2 = r.getTodo(id)
        assertEquals(id, todoUpdate.id)
        assertEquals(true, todoUpdate2.completed)
        assertEquals("do more stuff", todoUpdate2.title)
    }

    @Test(expected = java.util.NoSuchElementException::class)
    fun `delete a todo`() {
        val id = Todo.generateId()
        r.createTodo(id, "do stuff", 1, false)

        val n = r.deleteTodo(id)
        assertEquals(1, n)

        r.getTodo(id)
    }

    @Test(expected = java.util.NoSuchElementException::class)
    fun `get doesn't exist`() {
        r.getTodo(Todo.generateId())
    }

    @Test(expected = java.util.NoSuchElementException::class)
    fun `update doesn't exist`() {
        r.updateTodo(Todo.generateId())
    }

    @Test
    fun `delete doesn't exist`() {
        val n = r.deleteTodo(Todo.generateId())
        assertEquals(0, n)
    }
}