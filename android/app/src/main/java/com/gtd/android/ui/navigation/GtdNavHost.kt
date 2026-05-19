package com.gtd.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.gtd.android.NotificationNavigation
import com.gtd.android.ui.auth.AuthViewModel
import com.gtd.android.ui.auth.LoginScreen
import com.gtd.android.ui.auth.RegisterScreen
import com.gtd.android.ui.context.ContextsScreen
import com.gtd.android.ui.task.TaskDetailScreen
import com.gtd.android.ui.workspace.WorkspaceScreen
import kotlinx.coroutines.flow.StateFlow

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CONTEXTS = "contexts"
    const val WORKSPACE = "workspace/{contextId}"
    const val TASK_DETAIL = "workspace/{contextId}/task/{taskId}"

    fun workspace(contextId: String) = "workspace/$contextId"
    fun taskDetail(contextId: String, taskId: String) = "workspace/$contextId/task/$taskId"
}

@Composable
fun GtdNavHost(pendingNavigation: StateFlow<NotificationNavigation?>? = null) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    var startChecked by rememberSaveable { mutableStateOf(false) }
    var startRoute by rememberSaveable { mutableStateOf(Routes.LOGIN) }

    LaunchedEffect(Unit) {
        if (!startChecked) {
            val hasSession = authViewModel.tryRefreshSession()
            startRoute = if (hasSession) Routes.CONTEXTS else Routes.LOGIN
            startChecked = true
            if (hasSession) {
                navController.navigate(Routes.CONTEXTS) {
                    popUpTo(Routes.LOGIN) { inclusive = true }
                }
            }
        }
    }

    val navigation = pendingNavigation?.collectAsState()?.value
    LaunchedEffect(navigation) {
        if (navigation != null && startChecked && startRoute == Routes.CONTEXTS) {
            navController.navigate(Routes.taskDetail(navigation.contextId, navigation.taskId)) {
                popUpTo(Routes.CONTEXTS)
            }
            val activity = navController.context as? com.gtd.android.MainActivity
            activity?.consumeNavigation()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    navController.navigate(Routes.CONTEXTS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegistrationSuccess = {
                    navController.popBackStack()
                },
            )
        }

        composable(Routes.CONTEXTS) {
            ContextsScreen(
                onContextClick = { contextId ->
                    navController.navigate(Routes.workspace(contextId))
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.WORKSPACE,
            arguments = listOf(navArgument("contextId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val contextId = backStackEntry.arguments?.getString("contextId") ?: ""
            WorkspaceScreen(
                contextId = contextId,
                onNavigateBack = { navController.popBackStack() },
                onTaskClick = { taskId ->
                    navController.navigate(Routes.taskDetail(contextId, taskId))
                },
            )
        }

        composable(
            route = Routes.TASK_DETAIL,
            arguments = listOf(
                navArgument("contextId") { type = NavType.StringType },
                navArgument("taskId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val contextId = backStackEntry.arguments?.getString("contextId") ?: ""
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskDetailScreen(
                taskId = taskId,
                contextId = contextId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSubtask = { subtaskId ->
                    navController.navigate(Routes.taskDetail(contextId, subtaskId))
                },
            )
        }
    }
}
