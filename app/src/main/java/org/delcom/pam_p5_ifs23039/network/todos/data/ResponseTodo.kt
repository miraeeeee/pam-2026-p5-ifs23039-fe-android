package org.delcom.pam_p5_ifs23039.network.todos.data

import kotlinx.serialization.Serializable

@Serializable
data class ResponseTodos(
    val todos: List<ResponseTodoData>,
    val meta: ResponseTodoMeta? = null
)

@Serializable
data class ResponseTodoMeta(
    val page: Int = 1,
    val perPage: Int = 10,
    val total: Int = 0,
    val totalPages: Int = 1
)

@Serializable
data class ResponseTodo(
    val todo: ResponseTodoData
)

@Serializable
data class ResponseTodoData(
    val id: String = "",
    val userId: String = "",
    val title: String,
    val description: String,
    val isDone: Boolean = false,
    val urgency: String? = null,      // ← nullable, fix crash sebelumnya
    val cover: String? = null,
    val createdAt: String = "",
    var updatedAt: String = ""
)

@Serializable
data class ResponseTodoAdd(           // ← INI yang hilang!
    val todoId: String
)

enum class TodoUrgency(val label: String, val value: String) {
    LOW("Low", "low"),
    MEDIUM("Medium", "medium"),
    HIGH("High", "high");

    companion object {
        fun fromValue(value: String?): TodoUrgency {   // ← parameter String?
            return entries.find { it.value == value } ?: LOW
        }
    }
}