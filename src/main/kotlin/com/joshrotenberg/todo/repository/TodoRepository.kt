package com.joshrotenberg.todo.repository

import com.joshrotenberg.todo.model.Todo
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


object Todos : Table() {
    val id = varchar("id", 36).primaryKey()
    val title = varchar("title", 256)
    val order = integer("order")
    val completed = bool("completed")
}

interface RepoInterface {
    fun init()
    fun drop()
    fun createTodo(id: String, title: String, order: Int, completed: Boolean)
    fun getTodo(id: String): Todo
    fun getTodos(): List<Todo>
    fun updateTodo(id: String, title: String? = null, order: Int? = null, completed: Boolean? = null)
    fun deleteTodo(id: String): Number
}

class TodoRepository(val db: Database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")) : RepoInterface {
    override fun init() {
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Todos)
        }
    }

    override fun drop() {
        transaction {
            SchemaUtils.drop(Todos)
        }
    }

    override fun createTodo(id: String, title: String, order: Int, completed: Boolean) {
        transaction {
            Todos.insert {
                it[Todos.id] = id
                it[Todos.title] = title
                it[Todos.order] = order
                it[Todos.completed] = completed
            }
        }
    }

    override fun getTodo(id: String): Todo {
        return transaction {
            val r = Todos.select { Todos.id.eq(id) }.single()
            Todo(id, r[Todos.title], r[Todos.order], r[Todos.completed])
        }
    }

    override fun getTodos(): List<Todo> {
        return transaction {
            Todos.selectAll().map {
                Todo(it[Todos.id], it[Todos.title], it[Todos.order], it[Todos.completed])
            }
        }
    }

    override fun updateTodo(id: String, title: String?, order: Int?, completed: Boolean?) {
        transaction {
            val current = getTodo(id)
            Todos.update({ Todos.id eq id }) {
                it[Todos.title] = title ?: current.title
                it[Todos.order] = order ?: current.order
                it[Todos.completed] = completed ?: current.completed
            }
        }
    }

    override fun deleteTodo(id: String): Number {
        return transaction {
            Todos.deleteWhere { Todos.id.eq(id) }
        }
    }


}