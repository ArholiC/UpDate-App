package com.update.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.update.app.ui.screens.*
import com.update.app.ui.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
    object Chat : Screen("chat/{userId}/{userName}") {
        fun createRoute(userId: Int, userName: String) = "chat/$userId/$userName"
    }
    // isCallee: "1" = callee (gelen arama), "0" = caller (giden arama)
    object Call : Screen("call/{userId}/{callType}/{userName}/{isCallee}") {
        fun createRoute(userId: Int, callType: String, userName: String, isCallee: Boolean = false) =
            "call/$userId/$callType/$userName/${if (isCallee) 1 else 0}"
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
    val currentUserId by authViewModel.currentUserId.collectAsState()

    val startDestination = if (isLoggedIn) Screen.Main.route else Screen.Welcome.route

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(Screen.Login.route) },
                onRegisterClick = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                authViewModel = authViewModel,
                onSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                authViewModel = authViewModel,
                onSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                userId = currentUserId,
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.logout(context)
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                onOpenChat = { userId, userName ->
                    navController.navigate(Screen.Chat.createRoute(userId, userName))
                },
                // Gelen arama → isCallee = true
                onStartCall = { calleeId, callType, calleeName ->
                    navController.navigate(Screen.Call.createRoute(calleeId, callType, calleeName, isCallee = true))
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.IntType },
                navArgument("userName") { type = NavType.StringType }
            )
        ) { backStack ->
            val userId = backStack.arguments?.getInt("userId") ?: 0
            val userName = backStack.arguments?.getString("userName") ?: ""
            ChatScreen(
                myId = currentUserId,
                userId = userId,
                userName = userName,
                onBack = { navController.popBackStack() },
                // Chat'ten başlatılan arama → isCallee = false
                onStartCall = { calleeId, callType, name ->
                    navController.navigate(Screen.Call.createRoute(calleeId, callType, name, isCallee = false))
                }
            )
        }

        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.IntType },
                navArgument("callType") { type = NavType.StringType },
                navArgument("userName") { type = NavType.StringType },
                navArgument("isCallee") { type = NavType.IntType; defaultValue = 0 }
            )
        ) { backStack ->
            val userId = backStack.arguments?.getInt("userId") ?: 0
            val callType = backStack.arguments?.getString("callType") ?: "audio"
            val userName = backStack.arguments?.getString("userName") ?: ""
            val isCallee = (backStack.arguments?.getInt("isCallee") ?: 0) == 1
            CallScreen(
                myId = currentUserId,
                calleeId = userId,
                calleeName = userName,
                callType = callType,
                isCallee = isCallee,
                onEnd = { navController.popBackStack() }
            )
        }
    }
}