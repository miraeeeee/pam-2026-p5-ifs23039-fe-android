package org.delcom.pam_p5_ifs23039.ui.screens.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import org.delcom.pam_p5_ifs23039.helper.*
import org.delcom.pam_p5_ifs23039.helper.SuspendHelper.SnackBarType
import org.delcom.pam_p5_ifs23039.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23039.network.todos.data.TodoUrgency
import org.delcom.pam_p5_ifs23039.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23039.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23039.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23039.ui.viewmodels.*

@Composable
fun TodosEditScreen(
    navController: NavHostController,
    snackbarHost: SnackbarHostState,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel,
    todoId: String
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var todo by remember { mutableStateOf<ResponseTodoData?>(null) }
    val authToken = remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true

        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            return@LaunchedEffect
        }

        authToken.value = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        uiStateTodo.todo = TodoUIState.Loading
        uiStateTodo.todoChange = TodoActionUIState.Loading
        todoViewModel.getTodoById(authToken.value!!, todoId)
    }

    LaunchedEffect(uiStateTodo.todo) {
        if (uiStateTodo.todo !is TodoUIState.Loading) {
            if (uiStateTodo.todo is TodoUIState.Success) {
                todo = (uiStateTodo.todo as TodoUIState.Success).data
                isLoading = false
            } else {
                RouteHelper.back(navController)
                isLoading = false
            }
        }
    }

    fun onSave(title: String, description: String, isDone: Boolean, urgency: String) {
        isLoading = true
        todoViewModel.putTodo(
            authToken = authToken.value!!,
            todoId = todoId,
            title = title,
            description = description,
            isDone = isDone,
            urgency = urgency
        )
    }

    LaunchedEffect(uiStateTodo.todoChange) {
        when (val state = uiStateTodo.todoChange) {
            is TodoActionUIState.Success -> {
                SuspendHelper.showSnackBar(snackbarHost, SnackBarType.SUCCESS, state.message)
                RouteHelper.to(
                    navController = navController,
                    destination = ConstHelper.RouteNames.TodosDetail.path.replace("{todoId}", todoId),
                    popUpTo = ConstHelper.RouteNames.TodosDetail.path.replace("{todoId}", todoId),
                    removeBackStack = true
                )
                isLoading = false
            }
            is TodoActionUIState.Error -> {
                SuspendHelper.showSnackBar(snackbarHost, SnackBarType.ERROR, state.message)
                isLoading = false
            }
            else -> {}
        }
    }

    if (isLoading || todo == null) {
        LoadingUI()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBarComponent(navController = navController, title = "Ubah Data", showBackButton = true)
        Box(modifier = Modifier.weight(1f)) {
            TodosEditUI(todo = todo!!, onSave = ::onSave)
        }
        BottomNavComponent(navController = navController)
    }
}

@Composable
fun TodosEditUI(
    todo: ResponseTodoData,
    onSave: (String, String, Boolean, String) -> Unit
) {
    val alertState = remember { mutableStateOf(AlertState()) }

    var dataTitle by remember { mutableStateOf(todo.title) }
    var dataDescription by remember { mutableStateOf(todo.description) }
    var dataIsDone by remember { mutableStateOf(todo.isDone) }
    var dataUrgency by remember { mutableStateOf(TodoUrgency.fromValue(todo.urgency)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title
        OutlinedTextField(
            value = dataTitle,
            onValueChange = { dataTitle = it },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                cursorColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            label = { Text("Title", color = MaterialTheme.colorScheme.onPrimaryContainer) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        )

        // Is Done
        Column(modifier = Modifier.fillMaxWidth()) {
            Text("Is Done?", color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = dataIsDone, onClick = { dataIsDone = true })
                Text("Yes")
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(selected = !dataIsDone, onClick = { dataIsDone = false })
                Text("No")
            }
        }

        // Urgency Selector
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Urgency",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TodoUrgency.entries.forEach { urgency ->
                    val isSelected = dataUrgency == urgency
                    val (bgColor, borderColor, textColor) = when (urgency) {
                        TodoUrgency.LOW -> Triple(
                            if (isSelected) Color(0xFF4CAF50) else Color.Transparent,
                            Color(0xFF4CAF50),
                            if (isSelected) Color.White else Color(0xFF4CAF50)
                        )
                        TodoUrgency.MEDIUM -> Triple(
                            if (isSelected) Color(0xFFFF9800) else Color.Transparent,
                            Color(0xFFFF9800),
                            if (isSelected) Color.White else Color(0xFFFF9800)
                        )
                        TodoUrgency.HIGH -> Triple(
                            if (isSelected) Color(0xFFF44336) else Color.Transparent,
                            Color(0xFFF44336),
                            if (isSelected) Color.White else Color(0xFFF44336)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                            .clickable { dataUrgency = urgency }   // ← INI yang penting
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = urgency.label,
                            color = textColor,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        // Deskripsi
        OutlinedTextField(
            value = dataDescription,
            onValueChange = { dataDescription = it },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                unfocusedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                focusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                cursorColor = MaterialTheme.colorScheme.primaryContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            label = { Text("Deskripsi", color = MaterialTheme.colorScheme.onPrimaryContainer) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            maxLines = 5,
            minLines = 3
        )

        Spacer(modifier = Modifier.height(64.dp))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        FloatingActionButton(
            onClick = {
                if (dataTitle.isEmpty()) {
                    AlertHelper.show(alertState, AlertType.ERROR, "Judul tidak boleh kosong!")
                    return@FloatingActionButton
                }
                if (dataDescription.isEmpty()) {
                    AlertHelper.show(alertState, AlertType.ERROR, "Deskripsi tidak boleh kosong!")
                    return@FloatingActionButton
                }
                onSave(dataTitle, dataDescription, dataIsDone, dataUrgency.value)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(imageVector = Icons.Default.Save, contentDescription = "Simpan Data")
        }
    }

    if (alertState.value.isVisible) {
        AlertDialog(
            onDismissRequest = { AlertHelper.dismiss(alertState) },
            title = { Text(alertState.value.type.title) },
            text = { Text(alertState.value.message) },
            confirmButton = {
                TextButton(onClick = { AlertHelper.dismiss(alertState) }) { Text("OK") }
            }
        )
    }
}