package org.delcom.pam_p5_ifs23039.ui.screens.todos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import org.delcom.pam_p5_ifs23039.R
import org.delcom.pam_p5_ifs23039.helper.ConstHelper
import org.delcom.pam_p5_ifs23039.helper.RouteHelper
import org.delcom.pam_p5_ifs23039.helper.ToolsHelper
import org.delcom.pam_p5_ifs23039.network.todos.data.ResponseTodoData
import org.delcom.pam_p5_ifs23039.ui.components.BottomNavComponent
import org.delcom.pam_p5_ifs23039.ui.components.LoadingUI
import org.delcom.pam_p5_ifs23039.ui.components.TopAppBarComponent
import org.delcom.pam_p5_ifs23039.ui.components.TopAppBarMenuItem
import org.delcom.pam_p5_ifs23039.ui.viewmodels.AuthLogoutUIState
import org.delcom.pam_p5_ifs23039.ui.viewmodels.AuthUIState
import org.delcom.pam_p5_ifs23039.ui.viewmodels.AuthViewModel
import org.delcom.pam_p5_ifs23039.ui.viewmodels.TodoViewModel
import org.delcom.pam_p5_ifs23039.ui.viewmodels.TodosUIState

@Composable
fun TodosScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    todoViewModel: TodoViewModel
) {
    // Ambil data dari viewmodel
    val uiStateAuth by authViewModel.uiState.collectAsState()
    val uiStateTodo by todoViewModel.uiState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember {
        mutableStateOf(TextFieldValue(""))
    }

    // Muat data
    var todos by remember { mutableStateOf<List<ResponseTodoData>>(emptyList()) }
    var authToken by remember { mutableStateOf<String?>(null) }

    fun fetchTodosData() {
        isLoading = true

        authToken = (uiStateAuth.auth as AuthUIState.Success).data.authToken

        todoViewModel.getAllTodos(authToken ?: "", searchQuery.text)
    }

    // Picu pengambilan data todos
    LaunchedEffect(Unit) {
        isLoading = true

        if (uiStateAuth.auth !is AuthUIState.Success) {
            RouteHelper.to(
                navController,
                ConstHelper.RouteNames.Home.path,
                true
            )
            return@LaunchedEffect
        }

        fetchTodosData()
    }

    // Picu ketika terjadi perubahan data todos
    LaunchedEffect(uiStateTodo.todos) {
        if (uiStateTodo.todos !is TodosUIState.Loading) {
            isLoading = false

            todos = if (uiStateTodo.todos is TodosUIState.Success) {
                (uiStateTodo.todos as TodosUIState.Success).data
            } else {
                emptyList()
            }
        }
    }

    fun onLogout(token: String){
        isLoading = true
        authViewModel.logout(token)
    }

    LaunchedEffect(uiStateAuth.authLogout) {
        if (uiStateAuth.authLogout !is AuthLogoutUIState.Loading) {
            RouteHelper.to(
                navController,
                ConstHelper.RouteNames.AuthLogin.path,
                true
            )
        }
    }

    // Tampilkan halaman loading
    if (isLoading) {
        LoadingUI()
        return
    }

    // Menu Top App Bar
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
            onClick = {
                onLogout(authToken ?: "")
            }
        )
    )

    fun onOpen(todoId: String) {
        RouteHelper.to(
            navController = navController,
            destination = "todos/${todoId}"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBarComponent(
            navController = navController,
            title = "Todos",
            showBackButton = false,
            customMenuItems = menuItems,
            withSearch = true,
            searchQuery = searchQuery,
            onSearchQueryChange = { query ->
                searchQuery = query
            },
            onSearchAction = {
                fetchTodosData()
            }
        )
        // Content
        Box(
            modifier = Modifier
                .weight(1f)
        ) {
            TodosUI(
                todos = todos,
                onOpen = ::onOpen
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
            )
            {
                // Floating Action Button
                FloatingActionButton(
                    onClick = {
                        RouteHelper.to(
                            navController,
                            ConstHelper.RouteNames
                                .TodosAdd
                                .path
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Tumbuhan",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        // Bottom Nav
        BottomNavComponent(navController = navController)
    }
}

@Composable
fun TodosUI(
    todos: List<ResponseTodoData>,
    onOpen: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        items(todos) { todo ->
            TodoItemUI(
                todo,
                onOpen
            )
        }
    }

    if (todos.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Belum ada todo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tekan + untuk menambahkan todo baru",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TodoItemUI(
    todo: ResponseTodoData,
    onOpen: (String) -> Unit
) {
    val isDone = todo.isDone
    val statusColor = if (isDone) Color(0xFF2ECC71) else Color(0xFFE67E22)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onOpen(todo.id) },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = ToolsHelper.getTodoImage(todo.id, todo.updatedAt),
                    contentDescription = todo.title,
                    placeholder = painterResource(R.drawable.img_placeholder),
                    error = painterResource(R.drawable.img_placeholder),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Konten teks
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = todo.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status dengan ikon
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDone) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isDone) "Selesai" else "Belum Selesai",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Accent bar vertikal kanan
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = if (isDone)
                                listOf(Color(0xFF2ECC71), Color(0xFF1ABC9C))
                            else
                                listOf(Color(0xFFE67E22), Color(0xFFE74C3C))
                        )
                    )
            )
        }
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun PreviewTodosUI() {
//    DelcomTheme {
//        TodosUI(
//            todos = DummyData.getTodosData(),
//            onOpen = {}
//        )
//    }
}