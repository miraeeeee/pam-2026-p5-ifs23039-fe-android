package org.delcom.pam_p5_ifs23039.ui.screens.todos

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.delcom.pam_p5_ifs23039.helper.*
import org.delcom.pam_p5_ifs23039.helper.SuspendHelper.SnackBarType
import org.delcom.pam_p5_ifs23039.network.todos.data.TodoUrgency
import org.delcom.pam_p5_ifs23039.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23039.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23039.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23039.ui.viewmodels.*

@Composable
fun TodosAddScreen(
    navController: NavHostController,
    snackbarHost: SnackbarHostState,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    val authToken = remember { mutableStateOf<String?>(null) }
    var pendingCoverUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCoverPart by remember { mutableStateOf<MultipartBody.Part?>(null) }
    var savedTodoId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            return@LaunchedEffect
        }
        authToken.value = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        uiStateTodo.todoAdd = TodoActionUIState.Loading
        uiStateTodo.todoChangeCover = TodoActionUIState.Loading
    }

    fun onSave(title: String, description: String, urgency: String, coverPart: MultipartBody.Part?) {
        if (authToken.value == null) return
        isLoading = true
        pendingCoverPart = coverPart
        todoViewModel.postTodo(
            authToken = authToken.value!!,
            title = title,
            description = description,
            urgency = urgency
        )
    }

    // Step 1: Setelah todo berhasil dibuat, upload foto jika ada
    LaunchedEffect(uiStateTodo.todoAdd) {
        when (val state = uiStateTodo.todoAdd) {
            is TodoActionUIState.Success -> {
                val todoId = state.todoId
                if (pendingCoverPart != null && todoId != null) {
                    // Upload foto dulu, baru navigasi
                    savedTodoId = todoId
                    todoViewModel.putTodoCover(authToken.value!!, todoId, pendingCoverPart!!)
                } else {
                    SuspendHelper.showSnackBar(snackbarHost, SnackBarType.SUCCESS, state.message)
                    RouteHelper.to(navController, ConstHelper.RouteNames.Todos.path, true)
                    isLoading = false
                }
            }
            is TodoActionUIState.Error -> {
                SuspendHelper.showSnackBar(snackbarHost, SnackBarType.ERROR, state.message)
                isLoading = false
            }
            else -> {}
        }
    }

    // Step 2: Setelah foto berhasil diupload, navigasi
    LaunchedEffect(uiStateTodo.todoChangeCover) {
        when (val state = uiStateTodo.todoChangeCover) {
            is TodoActionUIState.Success -> {
                SuspendHelper.showSnackBar(snackbarHost, SnackBarType.SUCCESS, "Todo berhasil ditambahkan!")
                RouteHelper.to(navController, ConstHelper.RouteNames.Todos.path, true)
                isLoading = false
            }
            is TodoActionUIState.Error -> {
                SuspendHelper.showSnackBar(snackbarHost, SnackBarType.ERROR, "Todo ditambahkan, tapi foto gagal diupload")
                RouteHelper.to(navController, ConstHelper.RouteNames.Todos.path, true)
                isLoading = false
            }
            else -> {}
        }
    }

    if (isLoading) {
        LoadingUI()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBarComponent(navController = navController, title = "Tambah Todo", showBackButton = true)
        Box(modifier = Modifier.weight(1f)) {
            TodosAddUI(onSave = ::onSave)
        }
        BottomNavComponent(navController = navController)
    }
}

@Composable
fun TodosAddUI(
    onSave: (String, String, String, MultipartBody.Part?) -> Unit
) {
    val alertState = remember { mutableStateOf(AlertState()) }
    val context = LocalContext.current

    var dataTitle by remember { mutableStateOf("") }
    var dataDescription by remember { mutableStateOf("") }
    var dataUrgency by remember { mutableStateOf(TodoUrgency.LOW) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var coverPart by remember { mutableStateOf<MultipartBody.Part?>(null) }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Buat MultipartBody.Part dari URI
            val inputStream = context.contentResolver.openInputStream(it)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                val requestBody = bytes.toRequestBody("image/*".toMediaTypeOrNull())
                coverPart = MultipartBody.Part.createFormData("cover", "cover.jpg", requestBody)
            }
        }
    }

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

        // Description
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
            label = { Text("Description", color = MaterialTheme.colorScheme.onPrimaryContainer) },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
            maxLines = 5,
            minLines = 3
        )

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
                            .clickable { dataUrgency = urgency }
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

        // Cover Photo Picker
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Foto Cover (opsional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (selectedImageUri != null) {
                // Preview foto yang dipilih
                Box(modifier = Modifier.fillMaxWidth()) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Cover Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    // Tombol ganti foto
                    TextButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                    ) {
                        Text("Ganti Foto", color = Color.White)
                    }
                }
            } else {
                // Tombol pilih foto
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Pilih Foto Cover",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

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
                onSave(dataTitle, dataDescription, dataUrgency.value, coverPart)
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