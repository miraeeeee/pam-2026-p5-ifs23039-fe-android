package org.delcom.pam_p5_ifs23039.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import org.delcom.pam_p5_ifs23039.R
import org.delcom.pam_p5_ifs23039.helper.ConstHelper
import org.delcom.pam_p5_ifs23039.helper.RouteHelper
import org.delcom.pam_p5_ifs23039.helper.SuspendHelper
import org.delcom.pam_p5_ifs23039.helper.SuspendHelper.SnackBarType
import org.delcom.pam_p5_ifs23039.helper.ToolsHelper
import org.delcom.pam_p5_ifs23039.helper.ToolsHelper.uriToMultipart
import org.delcom.pam_p5_ifs23039.network.todos.data.ResponseUserData
import org.delcom.pam_p5_ifs23039.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23039.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23039.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23039.ui.components.TopAppBarMenuItem
import org.delcom.pam_p5_ifs23039.ui.viewmodels.*
import kotlin.time.Clock

@Composable
fun ProfileScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var isLoading by remember { mutableStateOf(false) }
    var profile by remember { mutableStateOf<ResponseUserData?>(null) }
    var authToken by remember { mutableStateOf<String?>(null) }
    var photoTimestamp by remember { mutableStateOf("0") }

    var showEditInfoDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showEditAboutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            return@LaunchedEffect
        }
        authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
        if (uiStateTodo.profile is ProfileUIState.Success) {
            profile = (uiStateTodo.profile as ProfileUIState.Success).data
            isLoading = false
            return@LaunchedEffect
        }
        todoViewModel.getProfile(authToken ?: "")
    }

    LaunchedEffect(uiStateTodo.profile) {
        if (uiStateTodo.profile !is ProfileUIState.Loading) {
            isLoading = false
            if (uiStateTodo.profile is ProfileUIState.Success) {
                profile = (uiStateTodo.profile as ProfileUIState.Success).data
            } else {
                RouteHelper.to(navController, ConstHelper.RouteNames.Home.path, true)
            }
        }
    }

    LaunchedEffect(uiStateTodo.profileChange) {
        when (val state = uiStateTodo.profileChange) {
            is TodoActionUIState.Success -> {
                SuspendHelper.showSnackBar(snackbarHostState, SnackBarType.SUCCESS, state.message)
                todoViewModel.getProfile(authToken ?: "")
                isLoading = false
                showEditInfoDialog = false
                showEditAboutDialog = false
            }
            is TodoActionUIState.Error -> {
                SuspendHelper.showSnackBar(snackbarHostState, SnackBarType.ERROR, state.message)
                isLoading = false
            }
            else -> {}
        }
    }

    LaunchedEffect(uiStateTodo.profileChangePassword) {
        when (val state = uiStateTodo.profileChangePassword) {
            is TodoActionUIState.Success -> {
                SuspendHelper.showSnackBar(snackbarHostState, SnackBarType.SUCCESS, state.message)
                isLoading = false
                showChangePasswordDialog = false
            }
            is TodoActionUIState.Error -> {
                SuspendHelper.showSnackBar(snackbarHostState, SnackBarType.ERROR, state.message)
                isLoading = false
            }
            else -> {}
        }
    }

    LaunchedEffect(uiStateTodo.profileChangePhoto) {
        when (val state = uiStateTodo.profileChangePhoto) {
            is TodoActionUIState.Success -> {
                photoTimestamp = Clock.System.now().toEpochMilliseconds().toString()
                SuspendHelper.showSnackBar(snackbarHostState, SnackBarType.SUCCESS, state.message)
                isLoading = false
            }
            is TodoActionUIState.Error -> {
                SuspendHelper.showSnackBar(snackbarHostState, SnackBarType.ERROR, state.message)
                isLoading = false
            }
            else -> {}
        }
    }

    fun onLogout(token: String) {
        isLoading = true
        authViewModel.logout(token)
    }

    LaunchedEffect(uiStateAuth.authLogout) {
        if (uiStateAuth.authLogout !is AuthLogoutUIState.Loading) {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
        }
    }

    fun onChangePhoto(ctx: Context, uri: Uri) {
        isLoading = true
        val filePart = uriToMultipart(ctx, uri, "file")
        todoViewModel.putUserMePhoto(authToken ?: "", filePart)
    }

    if (isLoading || profile == null) { LoadingUI(); return }

    val menuItems = listOf(
        TopAppBarMenuItem(
            text = "Profile",
            icon = Icons.Filled.Person,
            route = ConstHelper.RouteNames.Profile.path
        ),
        TopAppBarMenuItem(
            text = "Logout",
            icon = Icons.AutoMirrored.Filled.Logout,
            route = null,
            onClick = { onLogout(authToken ?: "") }
        )
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            TopAppBarComponent(
                navController = navController,
                title = "Profile",
                showBackButton = false,
                customMenuItems = menuItems
            )
            Box(modifier = Modifier.weight(1f)) {
                ProfileUI(
                    profile = profile!!,
                    photoTimestamp = photoTimestamp,
                    onChangePhoto = ::onChangePhoto,
                    onEditInfo = { showEditInfoDialog = true },
                    onChangePassword = { showChangePasswordDialog = true },
                    onEditAbout = { showEditAboutDialog = true }
                )
            }
            BottomNavComponent(navController = navController)
        }
    }

    if (showEditInfoDialog) {
        EditInfoDialog(
            profile = profile!!,
            onDismiss = { showEditInfoDialog = false },
            onSave = { name, username ->
                isLoading = true
                todoViewModel.putUserMe(authToken ?: "", name, username, profile!!.about)
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onSave = { currentPwd, newPwd ->
                isLoading = true
                todoViewModel.putUserMePassword(authToken ?: "", currentPwd, newPwd)
            }
        )
    }

    if (showEditAboutDialog) {
        EditAboutDialog(
            currentAbout = profile!!.about ?: "",
            onDismiss = { showEditAboutDialog = false },
            onSave = { about ->
                isLoading = true
                todoViewModel.putUserMe(authToken ?: "", profile!!.name, profile!!.username, about)
            }
        )
    }
}

// ─────────────────────────────────────────────────────────
// UI Utama Profile
// ─────────────────────────────────────────────────────────

@Composable
fun ProfileUI(
    profile: ResponseUserData,
    photoTimestamp: String = "0",
    onChangePhoto: (Context, Uri) -> Unit = { _, _ -> },
    onEditInfo: () -> Unit = {},
    onChangePassword: () -> Unit = {},
    onEditAbout: () -> Unit = {}
) {
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onChangePhoto(context, it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header dengan gradient ───────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                )
                .padding(top = 36.dp, bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Foto profil — klik untuk ganti
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White,
                                    Color.White.copy(alpha = 0.5f)
                                )
                            ),
                            shape = CircleShape
                        )
                        .clickable {
                            imagePicker.launch(
                                PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                    contentAlignment = Alignment.BottomEnd
                ) {
                    AsyncImage(
                        model = ToolsHelper.getUserImage(profile.id, photoTimestamp),
                        contentDescription = "Photo Profil",
                        placeholder = painterResource(R.drawable.img_placeholder),
                        error = painterResource(R.drawable.img_placeholder),
                        modifier = Modifier.fillMaxSize()
                    )
                    // Ikon kamera di pojok bawah kanan foto
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Ganti foto",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Sentuh untuk ganti foto",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = profile.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "@${profile.username}",
                        fontSize = 13.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Card: Tentang ────────────────────────────────────────
        ProfileSectionCard(
            title = "Tentang",
            icon = Icons.Default.Info,
            onEdit = onEditAbout
        ) {
            Text(
                text = if (profile.about.isNullOrEmpty())
                    "Belum ada deskripsi. Ketuk ✏️ untuk menambahkan."
                else
                    profile.about,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 22.sp,
                color = if (profile.about.isNullOrEmpty())
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Card: Informasi Akun ─────────────────────────────────
        ProfileSectionCard(
            title = "Informasi Akun",
            icon = Icons.Default.Person,
            onEdit = onEditInfo
        ) {
            ProfileInfoRow(label = "Nama", value = profile.name)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            ProfileInfoRow(label = "Username", value = "@${profile.username}")
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── Card: Keamanan ───────────────────────────────────────
        ProfileSectionCard(
            title = "Keamanan",
            icon = Icons.Default.Lock,
            onEdit = onChangePassword,
            editLabel = "Ubah"
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Password,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Kata Sandi",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "••••••••",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

// ─────────────────────────────────────────────────────────
// Komponen Pendukung
// ─────────────────────────────────────────────────────────

@Composable
fun ProfileSectionCard(
    title: String,
    icon: ImageVector,
    editLabel: String = "Edit",
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                FilledTonalButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(editLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            content()
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─────────────────────────────────────────────────────────
// Dialog: Ubah Informasi Akun
// ─────────────────────────────────────────────────────────

@Composable
fun EditInfoDialog(
    profile: ResponseUserData,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var username by remember { mutableStateOf(profile.username) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Person, contentDescription = null) },
        title = { Text("Ubah Informasi Akun") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nama") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotEmpty() && username.isNotEmpty()) onSave(name, username)
                },
                shape = RoundedCornerShape(10.dp)
            ) { Text("Simpan") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) { Text("Batal") }
        }
    )
}

// ─────────────────────────────────────────────────────────
// Dialog: Ubah Kata Sandi
// ─────────────────────────────────────────────────────────

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var currentPwd by remember { mutableStateOf("") }
    var newPwd by remember { mutableStateOf("") }
    var confirmPwd by remember { mutableStateOf("") }
    var currentPwdVisible by remember { mutableStateOf(false) }
    var newPwdVisible by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null) },
        title = { Text("Ubah Kata Sandi") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = currentPwd,
                    onValueChange = { currentPwd = it; errorMsg = "" },
                    label = { Text("Kata Sandi Saat Ini") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (currentPwdVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { currentPwdVisible = !currentPwdVisible }) {
                            Icon(
                                if (currentPwdVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = newPwd,
                    onValueChange = { newPwd = it; errorMsg = "" },
                    label = { Text("Kata Sandi Baru") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (newPwdVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { newPwdVisible = !newPwdVisible }) {
                            Icon(
                                if (newPwdVisible) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = confirmPwd,
                    onValueChange = { confirmPwd = it; errorMsg = "" },
                    label = { Text("Konfirmasi Kata Sandi Baru") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    isError = errorMsg.isNotEmpty()
                )
                if (errorMsg.isNotEmpty()) {
                    Text(
                        errorMsg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        currentPwd.isEmpty() -> errorMsg = "Kata sandi saat ini tidak boleh kosong"
                        newPwd.isEmpty()     -> errorMsg = "Kata sandi baru tidak boleh kosong"
                        newPwd != confirmPwd -> errorMsg = "Konfirmasi kata sandi tidak cocok"
                        newPwd.length < 6    -> errorMsg = "Kata sandi minimal 6 karakter"
                        else                 -> onSave(currentPwd, newPwd)
                    }
                },
                shape = RoundedCornerShape(10.dp)
            ) { Text("Simpan") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) { Text("Batal") }
        }
    )
}

// ─────────────────────────────────────────────────────────
// Dialog: Ubah Tentang
// ─────────────────────────────────────────────────────────

@Composable
fun EditAboutDialog(
    currentAbout: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var about by remember { mutableStateOf(currentAbout) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("Ubah Tentang") },
        text = {
            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text("Ceritakan tentang dirimu...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                maxLines = 5,
                minLines = 3,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(about) },
                shape = RoundedCornerShape(10.dp)
            ) { Text("Simpan") }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) { Text("Batal") }
        }
    )
}