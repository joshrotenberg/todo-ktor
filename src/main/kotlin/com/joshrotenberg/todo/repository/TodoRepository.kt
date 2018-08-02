package com.joshrotenberg.todo.repository

import com.joshrotenberg.todo.model.Todo
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

const val ID_LENGTH = 36
const val TITLE_LENGTH = 256

object Todos : Table() {
    val id = varchar("id", ID_LENGTH).primaryKey()
    val title = varchar("title", TITLE_LENGTH)
    val order = integer("order")
    val completed = bool("completed")
}

interface RepoInterface {
    fun init()
    fun destroy()
    fun createTodo(id: String, title: String, order: Int, completed: Boolean)
    fun getTodo(id: String): Todo
    fun getTodos(): List<Todo>
    fun updateTodo(id: String, title: String? = null, order: Int? = null, completed: Boolean? = null): Number
    fun deleteTodo(id: String): Number
    fun deleteTodos(): Number
}

class TodoRepository(val db: Database = Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        driver = "org.h2.Driver")) : RepoInterface {
    override fun init() {
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Todos)
        }
    }

    override fun destroy() {
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

    override fun updateTodo(id: String, title: String?, order: Int?, completed: Boolean?): Number {
        return transaction {
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

    override fun deleteTodos(): Number {
        return transaction {
            Todos.deleteAll()
        }
    }
}
