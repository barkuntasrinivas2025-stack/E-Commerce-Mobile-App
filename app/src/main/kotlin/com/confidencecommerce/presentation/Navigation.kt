package com.confidencecommerce.presentation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.confidencecommerce.presentation.cart.CartScreen
import com.confidencecommerce.presentation.home.HomeScreen
import com.confidencecommerce.presentation.product.ProductDetailScreen

sealed class Screen(val route: String) {
    object Home          : Screen("home")
    object ProductDetail : Screen("product/{productId}") {
        fun createRoute(productId: String) = "product/$productId"
    }
    object Cart          : Screen("cart")
}

@Composable
fun ConfidenceCommerceNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {

        composable(Screen.Home.route) {
            HomeScreen(
                onProductClick  = { id -> navController.navigate(Screen.ProductDetail.createRoute(id)) },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) }
            )
        }

        composable(
            route     = Screen.ProductDetail.route,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStack ->
            val productId = backStack.arguments?.getString("productId") ?: return@composable
            ProductDetailScreen(
                productId        = productId,
                onNavigateBack   = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate(Screen.Cart.route) }
            )
        }

        composable(Screen.Cart.route) {
            CartScreen(
                onNavigateBack = { navController.popBackStack() },
                onCheckout     = { /* TODO: Navigate to checkout */ }
            )
        }
    }
}
