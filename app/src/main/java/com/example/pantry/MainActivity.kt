package com.example.pantry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pantry.data.ProductRepository
import com.example.pantry.data.local.AppDatabase
import com.example.pantry.ui.screens.ProductAddScreen
import com.example.pantry.ui.screens.ProductListScreen
import com.example.pantry.ui.screens.ScannerScreen
import com.example.pantry.ui.theme.AppTopBarBrown // <--- Ważny import
import com.example.pantry.ui.viewmodel.ProductViewModel
import com.example.pantry.ui.viewmodel.ProductViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ProductRepository(database.productDao())
        val viewModelFactory = ProductViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[ProductViewModel::class.java]

        setContent {
            // USTAWIONO DOMYŚLNY KOLOR NA BRĄZOWY
            var appPrimaryColor by remember { mutableStateOf(AppTopBarBrown) }

            val customColorScheme = lightColorScheme(
                primary = appPrimaryColor,
                onPrimary = Color.White,
                secondary = appPrimaryColor.copy(alpha = 0.8f),
                onSecondary = Color.White,
                tertiary = appPrimaryColor.copy(alpha = 0.6f)
            )

            MaterialTheme(colorScheme = customColorScheme) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "product_list") {

                    composable("product_list") {
                        ProductListScreen(
                            viewModel = viewModel,
                            onNavigateToAdd = { category ->
                                val route = if (category != null) "product_form?category=$category" else "product_form"
                                navController.navigate(route)
                            },
                            onNavigateToEdit = { product ->
                                navController.navigate("product_form?productId=${product.id}")
                            },
                            currentGlobalColor = appPrimaryColor,
                            onGlobalColorChange = { newColor -> appPrimaryColor = newColor }
                        )
                    }

                    composable(
                        route = "product_form?productId={productId}&category={category}",
                        arguments = listOf(
                            navArgument("productId") {
                                type = NavType.IntType
                                defaultValue = -1
                            },
                            navArgument("category") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val scannedBarcode = backStackEntry.savedStateHandle
                            .getLiveData<String>("barcode")
                            .observeAsState()

                        val productIdArg = backStackEntry.arguments?.getInt("productId") ?: -1
                        val productId = if (productIdArg == -1) null else productIdArg
                        val initialCategory = backStackEntry.arguments?.getString("category")

                        ProductAddScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToScanner = { navController.navigate("scanner") },
                            scannedBarcode = scannedBarcode.value,
                            productIdToEdit = productId,
                            initialCategory = initialCategory
                        )
                    }

                    composable("scanner") {
                        ScannerScreen(
                            onBarcodeScanned = { barcode ->
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("barcode", barcode)
                                navController.popBackStack()
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}