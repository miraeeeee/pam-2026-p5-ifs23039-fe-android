package org.delcom.pam_p5_ifs23039.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import org.delcom.pam_p5_ifs23039.helper.ConstHelper
import org.delcom.pam_p5_ifs23039.helper.RouteHelper
import org.delcom.pam_p5_ifs23039.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23039.network.todos.data.TodoUrgency
import org.delcom.pam_p5_ifs23039.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23039.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23039.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23039.ui.components.TopAppBarMenuItem
import org.delcom.pam_p5_ifs23039.ui.viewmodels.*

@Composable
fun HomeScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel
) {
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var isFreshToken by remember { mutableStateOf(false) }
    var authToken by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (isLoading) return@LaunchedEffect
        isLoading = true
        isFreshToken = true
        uiStateAuth.authLogout = AuthLogoutUIState.Loading
        authViewModel.loadTokenFromPreferences()
    }

    fun onLogout(token: String) {
        isLoading = true
        authViewModel.logout(token)
    }

    LaunchedEffect(uiStateAuth.auth) {
        if (!isLoading) return@LaunchedEffect
        if (uiStateAuth.auth !is AuthUIState.Loading) {
            if (uiStateAuth.auth is AuthUIState.Success) {
                if (isFreshToken) {
                    val dataToken = (uiStateAuth.auth as AuthUIState.Success).data
                    authViewModel.refreshToken(dataToken.authToken, dataToken.refreshToken)
                    isFreshToken = false
                } else if (uiStateAuth.authRefreshToken is AuthActionUIState.Success) {
                    val newToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken
                    if (authToken != newToken) {
                        authToken = newToken
                        // Muat semua todo untuk statistik Home
                        todoViewModel.getAllTodos(authToken ?: "", page = 1, appendData = false, perPage = 100)
                    }
                    isLoading = false
                }
            } else {
                onLogout("")
            }
        }
    }

    LaunchedEffect(uiStateAuth.authLogout) {
        if (uiStateAuth.authLogout !is AuthLogoutUIState.Loading) {
            RouteHelper.to(navController, ConstHelper.RouteNames.AuthLogin.path, true)
        }
    }

    if (isLoading || authToken == null || isFreshToken) {
        LoadingUI()
        return
    }

    val menuItems = listOf(
        TopAppBarMenuItem(text = "Profile", icon = Icons.Filled.Person, route = ConstHelper.RouteNames.Profile.path),
        TopAppBarMenuItem(text = "Logout", icon = Icons.AutoMirrored.Filled.Logout, route = null, onClick = { onLogout(authToken ?: "") })
    )

    val todos = if (uiStateTodo.todos is TodosUIState.Success) {
        (uiStateTodo.todos as TodosUIState.Success).data
    } else emptyList()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBarComponent(navController = navController, title = "Home", showBackButton = false, customMenuItems = menuItems)
        Box(modifier = Modifier.weight(1f)) {
            HomeUI(todos = todos, isLoadingTodos = uiStateTodo.todos is TodosUIState.Loading)
        }
        BottomNavComponent(navController = navController)
    }
}

@Composable
fun HomeUI(
    todos: List<ResponseTodoData> = emptyList(),
    isLoadingTodos: Boolean = false
) {
    val totalTodos = todos.size
    val doneTodos = todos.count { it.isDone }
    val pendingTodos = todos.count { !it.isDone }
    val lowCount = todos.count { TodoUrgency.fromValue(it.urgency) == TodoUrgency.LOW }
    val mediumCount = todos.count { TodoUrgency.fromValue(it.urgency) == TodoUrgency.MEDIUM }
    val highCount = todos.count { TodoUrgency.fromValue(it.urgency) == TodoUrgency.HIGH }
    val doneProgress = if (totalTodos > 0) doneTodos.toFloat() / totalTodos.toFloat() else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = doneProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "📋", fontSize = 40.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "My Todos Dashboard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Kelola semua tugasmu di sini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }

        if (isLoadingTodos) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Stats Row - Total, Selesai, Belum
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(modifier = Modifier.weight(1f), title = "Total", value = totalTodos.toString(), icon = Icons.AutoMirrored.Filled.List, containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                StatCard(modifier = Modifier.weight(1f), title = "Selesai", value = doneTodos.toString(), icon = Icons.Default.CheckCircle, containerColor = Color(0xFFD4EDDA), contentColor = Color(0xFF155724))
                StatCard(modifier = Modifier.weight(1f), title = "Belum", value = pendingTodos.toString(), icon = Icons.Default.Schedule, containerColor = Color(0xFFFFF3CD), contentColor = Color(0xFF856404))
            }

            // Progress Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Progress Penyelesaian", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(text = "${(doneProgress * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)),
                        strokeCap = StrokeCap.Round,
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "$doneTodos dari $totalTodos tugas selesai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Urgency Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Ringkasan Urgensi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        UrgencyBadge(modifier = Modifier.weight(1f), label = "🟢 Low", count = lowCount, bgColor = Color(0xFFD4EDDA), textColor = Color(0xFF155724))
                        UrgencyBadge(modifier = Modifier.weight(1f), label = "🟡 Medium", count = mediumCount, bgColor = Color(0xFFFFF3CD), textColor = Color(0xFF856404))
                        UrgencyBadge(modifier = Modifier.weight(1f), label = "🔴 High", count = highCount, bgColor = Color(0xFFF8D7DA), textColor = Color(0xFF721C24))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, title: String, value: String, icon: ImageVector, containerColor: Color, contentColor: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = containerColor), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = contentColor)
            Text(text = title, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun UrgencyBadge(modifier: Modifier = Modifier, label: String, count: Int, bgColor: Color, textColor: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = bgColor), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = count.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.8f), textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}