package com.example.homestorage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homestorage.ui.cabinet.CabinetDetailScreen
import com.example.homestorage.ui.create.CreateCabinetScreen
import com.example.homestorage.ui.home.HomeScreen
import com.example.homestorage.ui.spot.SpotDetailScreen
import com.example.homestorage.ui.theme.HomeStorageTheme

/** 应用入口：注册导航路由 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeStorageTheme {
                AppNavHost()
            }
        }
    }
}

/** 导航宿主：home / create_cabinet / cabinet / spot */
@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenCabinet = { cabinetId -> navController.navigate("cabinet/$cabinetId") },
                onOpenItem = { cabinetId, spotId ->
                    navController.navigate("cabinet/$cabinetId?highlightSpot=$spotId")
                },
                onOpenCreateCabinet = { navController.navigate("create_cabinet") }
            )
        }
        composable("create_cabinet") {
            CreateCabinetScreen(
                onBack = { navController.popBackStack() },
                onSaved = { cabinetId ->
                    navController.popBackStack()
                    navController.navigate("cabinet/$cabinetId")
                }
            )
        }
        composable(
            route = "cabinet/{cabinetId}?highlightSpot={spotId}",
            arguments = listOf(
                navArgument("cabinetId") { type = NavType.LongType },
                navArgument("spotId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val cabinetId = backStackEntry.arguments?.getLong("cabinetId") ?: return@composable
            val highlightSpotId = backStackEntry.arguments?.getLong("spotId")?.takeIf { it > 0 }
            CabinetDetailScreen(
                cabinetId = cabinetId,
                highlightSpotId = highlightSpotId,
                onBack = { navController.popBackStack() },
                onOpenSpot = { spotId -> navController.navigate("spot/$spotId") }
            )
        }
        composable(
            route = "spot/{spotId}",
            arguments = listOf(navArgument("spotId") { type = NavType.LongType })
        ) { backStackEntry ->
            val spotId = backStackEntry.arguments?.getLong("spotId") ?: return@composable
            SpotDetailScreen(
                spotId = spotId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
