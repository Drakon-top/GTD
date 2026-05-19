package com.gtd.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gtd.android.ui.auth.AuthViewModel
import com.gtd.android.ui.auth.LoginScreen
import com.gtd.android.ui.auth.RegisterScreen
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

        composable(Routes.WORKSPACE) {
            // Placeholder — will be implemented in TASK-040
        }
    }
}
