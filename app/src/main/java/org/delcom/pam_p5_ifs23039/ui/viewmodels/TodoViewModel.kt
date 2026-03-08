package org.delcom.pam_p5_ifs23039.ui.viewmodels

import androidx.annotation.Keep
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.delcom.pam_p5_ifs23039.network.todos.data.RequestTodo
import org.delcom.pam_p5_ifs23039.network.todos.data.RequestUserChange
import org.delcom.pam_p5_ifs23039.network.todos.data.RequestUserChangePassword
import org.delcom.pam_p5_ifs23039.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23039.network.todos.data.ResponseUserData
import org.delcom.pam_p5_ifs23039.network.todos.service.ITodoRepository
import javax.inject.Inject

sealed interface ProfileUIState {
    data class Success(val data: ResponseUserData) : ProfileUIState
    data class Error(val message: String) : ProfileUIState
    object Loading : ProfileUIState
}

sealed interface TodosUIState {
    data class Success(
        val data: List<ResponseTodoData>,
        val currentPage: Int = 1,
        val totalPages: Int = 1,
        val total: Int = 0,
        val isLoadingMore: Boolean = false
    ) : TodosUIState
    data class Error(val message: String) : TodosUIState
    object Loading : TodosUIState
}

sealed interface TodoUIState {
    data class Success(val data: ResponseTodoData) : TodoUIState
    data class Error(val message: String) : TodoUIState
    object Loading : TodoUIState
}

sealed interface TodoActionUIState {
    data class Success(val message: String, val todoId: String? = null) : TodoActionUIState
    data class Error(val message: String) : TodoActionUIState
    object Loading : TodoActionUIState
}

data class UIStateTodo(
    val profile: ProfileUIState = ProfileUIState.Loading,
    val todos: TodosUIState = TodosUIState.Loading,
    var todo: TodoUIState = TodoUIState.Loading,
    var todoAdd: TodoActionUIState = TodoActionUIState.Loading,
    var todoChange: TodoActionUIState = TodoActionUIState.Loading,
    var todoDelete: TodoActionUIState = TodoActionUIState.Loading,
    var todoChangeCover: TodoActionUIState = TodoActionUIState.Loading,
    var profileChange: TodoActionUIState = TodoActionUIState.Loading,
    var profileChangePassword: TodoActionUIState = TodoActionUIState.Loading,
    var profileChangePhoto: TodoActionUIState = TodoActionUIState.Loading,
)

@HiltViewModel
@Keep
class TodoViewModel @Inject constructor(
    private val repository: ITodoRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UIStateTodo())
    val uiState = _uiState.asStateFlow()

    // ---------------------------------------------------------------
    // Profile
    // ---------------------------------------------------------------

    fun getProfile(authToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(profile = ProfileUIState.Loading) }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.getUserMe(authToken)
                }.fold(
                    onSuccess = {
                        if (it.status == "success") ProfileUIState.Success(it.data!!.user)
                        else ProfileUIState.Error(it.message)
                    },
                    onFailure = { ProfileUIState.Error(it.message ?: "Unknown error") }
                )
                it.copy(profile = tmpState)
            }
        }
    }

    fun putUserMe(authToken: String, name: String, username: String, about: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileChange = TodoActionUIState.Loading) }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.putUserMe(
                        authToken = authToken,
                        request = RequestUserChange(name = name, username = username, about = about)
                    )
                }.fold(
                    onSuccess = {
                        if (it.status == "success") TodoActionUIState.Success(it.message)
                        else TodoActionUIState.Error(it.message)
                    },
                    onFailure = { TodoActionUIState.Error(it.message ?: "Unknown error") }
                )
                it.copy(profileChange = tmpState)
            }
        }
    }

    fun putUserMePassword(authToken: String, password: String, newPassword: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileChangePassword = TodoActionUIState.Loading) }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.putUserMePassword(
                        authToken = authToken,
                        request = RequestUserChangePassword(password = password, newPassword = newPassword)
                    )
                }.fold(
                    onSuccess = {
                        if (it.status == "success") TodoActionUIState.Success(it.message)
                        else TodoActionUIState.Error(it.message)
                    },
                    onFailure = { TodoActionUIState.Error(it.message ?: "Unknown error") }
                )
                it.copy(profileChangePassword = tmpState)
            }
        }
    }

    fun putUserMePhoto(authToken: String, file: MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.update { it.copy(profileChangePhoto = TodoActionUIState.Loading) }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.putUserMePhoto(authToken = authToken, file = file)
                }.fold(
                    onSuccess = {
                        if (it.status == "success") TodoActionUIState.Success(it.message)
                        else TodoActionUIState.Error(it.message)
                    },
                    onFailure = { TodoActionUIState.Error(it.message ?: "Unknown error") }
                )
                it.copy(profileChangePhoto = tmpState)
            }
        }
    }

    // ---------------------------------------------------------------
    // Todos (with pagination)
    // ---------------------------------------------------------------

    private val perPage = 10

    fun getAllTodos(
        authToken: String,
        search: String? = null,
        isDone: Boolean? = null,
        urgency: String? = null,
        page: Int = 1,
        appendData: Boolean = false,
        perPage: Int = 10   // ← tambah parameter ini
    ) {
        viewModelScope.launch {
            if (!appendData) {
                _uiState.update { it.copy(todos = TodosUIState.Loading) }
            } else {
                val currentState = _uiState.value.todos
                if (currentState is TodosUIState.Success) {
                    _uiState.update { it.copy(todos = currentState.copy(isLoadingMore = true)) }
                }
            }

            val result = runCatching {
                repository.getTodos(
                    authToken = authToken,
                    search = search,
                    isDone = isDone,
                    urgency = urgency,
                    page = page,
                    perPage = perPage   // ← pakai parameter, bukan private val
                )
            }.fold(
                onSuccess = { response ->
                    if (response.status == "success" && response.data != null) {
                        val newItems = response.data.todos
                        val meta = response.data.meta
                        val totalPages = meta?.totalPages ?: 1
                        val total = meta?.total ?: newItems.size

                        if (appendData) {
                            val current = _uiState.value.todos
                            val existing = if (current is TodosUIState.Success) current.data else emptyList()
                            TodosUIState.Success(
                                data = existing + newItems,
                                currentPage = page,
                                totalPages = totalPages,
                                total = total,
                                isLoadingMore = false
                            )
                        } else {
                            TodosUIState.Success(
                                data = newItems,
                                currentPage = page,
                                totalPages = totalPages,
                                total = total,
                                isLoadingMore = false
                            )
                        }
                    } else {
                        TodosUIState.Error(response.message)
                    }
                },
                onFailure = { TodosUIState.Error(it.message ?: "Unknown error") }
            )

            _uiState.update { it.copy(todos = result) }
        }
    }

    fun postTodo(authToken: String, title: String, description: String, urgency: String = "low") {
        viewModelScope.launch {
            _uiState.update { it.copy(todoAdd = TodoActionUIState.Loading) }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.postTodo(authToken = authToken, RequestTodo(title, description, urgency = urgency))
                }.fold(
                    onSuccess = {
                        if (it.status == "success")
                            TodoActionUIState.Success(it.message, it.data?.todoId)  // ← simpan todoId
                        else TodoActionUIState.Error(it.message)
                    },
                    onFailure = { TodoActionUIState.Error(it.message ?: "Unknown error") }
                )
                it.copy(todoAdd = tmpState)
            }
        }
    }

    fun getTodoById(authToken: String, todoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(todo = TodoUIState.Loading) }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.getTodoById(authToken, todoId)
                }.fold(
                    onSuccess = {
                        if (it.status == "success") TodoUIState.Success(it.data!!.todo)
                        else TodoUIState.Error(it.message)
                    },
                    onFailure = { TodoUIState.Error(it.message ?: "Unknown error") }
                )
                it.copy(todo = tmpState)
            }
        }
    }

    fun putTodo(authToken: String, todoId: String, title: String, description: String, isDone: Boolean, urgency: String = "low") {
        viewModelScope.launch {
            _uiState.update { it.copy(todoChange = TodoActionUIState.Loading) }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.putTodo(authToken, todoId, RequestTodo(title, description, isDone, urgency))
                }.fold(
                    onSuccess = {
                        if (it.status == "success") TodoActionUIState.Success(it.message)
                        else TodoActionUIState.Error(it.message)
                    },
                    onFailure = { TodoActionUIState.Error(it.message ?: "Unknown error") }
                )
                it.copy(todoChange = tmpState)
            }
        }
    }

    fun putTodoCover(authToken: String, todoId: String, file: MultipartBody.Part) {
        viewModelScope.launch {
            _uiState.update { it.copy(todoChangeCover = TodoActionUIState.Loading) }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.putTodoCover(authToken, todoId, file)
                }.fold(
                    onSuccess = {
                        if (it.status == "success") TodoActionUIState.Success(it.message)
                        else TodoActionUIState.Error(it.message)
                    },
                    onFailure = { TodoActionUIState.Error(it.message ?: "Unknown error") }
                )
                it.copy(todoChangeCover = tmpState)
            }
        }
    }

    fun deleteTodo(authToken: String, todoId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(todoDelete = TodoActionUIState.Loading) }
            _uiState.update { it ->
                val tmpState = runCatching {
                    repository.deleteTodo(authToken, todoId)
                }.fold(
                    onSuccess = {
                        if (it.status == "success") TodoActionUIState.Success(it.message)
                        else TodoActionUIState.Error(it.message)
                    },
                    onFailure = { TodoActionUIState.Error(it.message ?: "Unknown error") }
                )
                it.copy(todoDelete = tmpState)
            }
        }
    }
}