package com.joshrotenberg.todo.model

import java.util.*

data class Todo(val id: String? = null, val title: String, val order: Int, val completed: Boolean, var url: String? = null) {
    companion object {
        fun generateId(): String = UUID.randomUUID().toString()
    }
}