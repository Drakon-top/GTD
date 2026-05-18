package com.gtd.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gtd.android.ui.auth.LoginScreen
import com.gtd.android.ui.context.ContextsScreen

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val CONTEXTS = "contexts"
    const val WORKSPACE = "workspace/{contextId}"

    fun workspace(contextId: String) = "workspace/$contextId"
}

@Composable
fun GtdNavHost() {
    val navController = rememberNavController()

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
            // Placeholder — will be implemented in TASK-039
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

        composable(Routes.WORKSPACE) {
            // Placeholder — will be implemented in TASK-040
        }
    }
}
