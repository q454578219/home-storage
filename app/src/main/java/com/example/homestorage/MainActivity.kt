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
import com.example.homestorage.ui.floorplan.FloorPlanDetailScreen
import com.example.homestorage.ui.floorplan.FloorPlanListScreen
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
                onOpenCreateCabinet = { navController.navigate("create_cabinet") },
                onOpenFloorPlans = { navController.navigate("floorplans") }
            )
        }
        composable(
            route = "create_cabinet?floorPlanId={floorPlanId}&x={x}&y={y}",
            arguments = listOf(
                navArgument("floorPlanId") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("x") {
                    type = NavType.FloatType
                    defaultValue = 0.5f
                },
                navArgument("y") {
                    type = NavType.FloatType
                    defaultValue = 0.5f
                }
            )
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("floorPlanId")?.takeIf { it > 0 }
            val x = backStackEntry.arguments?.getFloat("x") ?: 0.5f
            val y = backStackEntry.arguments?.getFloat("y") ?: 0.5f
            CreateCabinetScreen(
                floorPlanId = planId,
                floorPlanX = x,
                floorPlanY = y,
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
        composable("floorplans") {
            FloorPlanListScreen(
                onBack = { navController.popBackStack() },
                onOpenFloorPlan = { planId -> navController.navigate("floorplan/$planId") }
            )
        }
        composable(
            route = "floorplan/{planId}",
            arguments = listOf(navArgument("planId") { type = NavType.LongType })
        ) { backStackEntry ->
            val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable
            FloorPlanDetailScreen(
                planId = planId,
                onBack = { navController.popBackStack() },
                onOpenCabinet = { cabinetId -> navController.navigate("cabinet/$cabinetId") },
                onCreateCabinet = { x, y ->
                    navController.navigate(
                        "create_cabinet?floorPlanId=$planId&x=$x&y=$y"
                    )
                }
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
