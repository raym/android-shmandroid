package com.lightship.safestring.ui.composable

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun KeystoreVaultNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                onItemClick = { stringName ->
                    navController.navigate("detail/$stringName")
                }
            )
        }

        composable(
            route = "detail/{stringName}",
            arguments = listOf(
                navArgument("stringName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val stringName = backStackEntry.arguments?.getString("stringName") ?: ""
            DetailScreen(
                stringName = stringName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
