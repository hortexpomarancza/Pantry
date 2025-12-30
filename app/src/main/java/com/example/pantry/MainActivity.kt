package com.example.pantry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pantry.data.ProductRepository
import com.example.pantry.data.local.AppDatabase
import com.example.pantry.ui.screens.ProductAddScreen
import com.example.pantry.ui.screens.ProductListScreen
import com.example.pantry.ui.screens.ScannerScreen
import com.example.pantry.ui.viewmodel.ProductViewModel
import com.example.pantry.ui.viewmodel.ProductViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState : Bundle?) {
        super.onCreate(savedInstanceState)

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = ProductRepository(database.productDao())

        val viewModelFactory = ProductViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, viewModelFactory)[ProductViewModel::class.java]

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "product_list") {
                composable("product_list") {
                    ProductListScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { navController.navigate("add_product" )}
                    )
                }
                composable("add_product") { backStackEntry ->
                    val scannedBarcode = backStackEntry.savedStateHandle
                        .getLiveData<String>("barcode")
                        .observeAsState()

                    ProductAddScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToScanner = { navController.navigate("scanner") },
                        scannedBarcode = scannedBarcode.value
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