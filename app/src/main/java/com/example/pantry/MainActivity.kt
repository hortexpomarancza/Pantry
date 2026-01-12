package com.example.pantry

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.pantry.data.ProductRepository
import com.example.pantry.data.local.AppDatabase
import com.example.pantry.ui.screens.ProductAddScreen
import com.example.pantry.ui.screens.ProductListScreen
import com.example.pantry.ui.screens.ScannerScreen
import com.example.pantry.ui.theme.AppTopBarBrown
import com.example.pantry.ui.theme.PantryTheme
import com.example.pantry.ui.viewmodel.ProductViewModel
import com.example.pantry.ui.viewmodel.ProductViewModelFactory
import com.example.pantry.worker.ExpirationNotificationWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicjalizacja bazy i ViewModel
        val database = AppDatabase.getDatabase(this)
        val repository = ProductRepository(database.productDao())

        // ZMIANA: Przekazujemy 'application' do fabryki
        val viewModelFactory = ProductViewModelFactory(application, repository)
        val productViewModel = ViewModelProvider(this, viewModelFactory)[ProductViewModel::class.java]

        // 2. Konfiguracja Powiadomień
        createNotificationChannel()
        scheduleExpirationCheck()

        setContent {
            PantryTheme {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    var currentGlobalColor by remember { mutableStateOf(AppTopBarBrown) }

                    NavHost(navController = navController, startDestination = "list") {

                        // --- EKRAN GŁÓWNY ---
                        composable("list") {
                            ProductListScreen(
                                viewModel = productViewModel,
                                currentGlobalColor = currentGlobalColor,
                                onGlobalColorChange = { currentGlobalColor = it },
                                onNavigateToAdd = { categoryName, currentCategoriesList, spaceColor ->
                                    navController.currentBackStackEntry?.savedStateHandle?.set("categories", currentCategoriesList)
                                    val route = if (categoryName != null) "add?cat=$categoryName&color=$spaceColor" else "add?color=$spaceColor"
                                    navController.navigate(route)
                                },
                                onNavigateToEdit = { product, spaceColor ->
                                    navController.navigate("edit/${product.id}?color=$spaceColor")
                                }
                            )
                        }

                        // --- EKRAN DODAWANIA ---
                        composable(
                            route = "add?cat={cat}&color={color}",
                            arguments = listOf(
                                navArgument("cat") { nullable = true },
                                navArgument("color") { type = NavType.IntType; defaultValue = 0 }
                            )
                        ) { backStackEntry ->
                            val categoryName = backStackEntry.arguments?.getString("cat")
                            val spaceColor = backStackEntry.arguments?.getInt("color")
                            val savedState = navController.previousBackStackEntry?.savedStateHandle
                            val categoriesList = savedState?.get<List<String>>("categories") ?: listOf("Inne")
                            val scannedBarcode = backStackEntry.savedStateHandle.get<String>("scanned_barcode")

                            ProductAddScreen(
                                viewModel = productViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToScanner = { navController.navigate("scanner") },
                                scannedBarcode = scannedBarcode,
                                initialCategory = categoryName,
                                availableCategories = categoriesList,
                                spaceColor = spaceColor
                            )
                        }

                        // --- EKRAN EDYCJI ---
                        composable(
                            route = "edit/{productId}?color={color}",
                            arguments = listOf(
                                navArgument("productId") { type = NavType.IntType },
                                navArgument("color") { type = NavType.IntType; defaultValue = 0 }
                            )
                        ) { backStackEntry ->
                            val productId = backStackEntry.arguments?.getInt("productId")
                            val spaceColor = backStackEntry.arguments?.getInt("color")
                            val categoriesList = listOf("Warzywa i Owoce", "Nabiał", "Mięso", "Pieczywo", "Napoje", "Mrożonki", "Inne")
                            val scannedBarcode = backStackEntry.savedStateHandle.get<String>("scanned_barcode")

                            ProductAddScreen(
                                viewModel = productViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToScanner = { navController.navigate("scanner") },
                                scannedBarcode = scannedBarcode,
                                productIdToEdit = productId,
                                availableCategories = categoriesList,
                                spaceColor = spaceColor
                            )
                        }

                        // --- EKRAN SKANERA ---
                        composable("scanner") {
                            ScannerScreen(
                                onBarcodeScanned = { code ->
                                    navController.previousBackStackEntry?.savedStateHandle?.set("scanned_barcode", code)
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Powiadomienia o ważności"
            val descriptionText = "Powiadamia o kończącym się terminie produktów"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("pantry_expiration_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun scheduleExpirationCheck() {
        // --- TESTOWANIE: Uruchom sprawdzenie NATYCHMIAST po włączeniu aplikacji ---
        val oneTimeRequest = androidx.work.OneTimeWorkRequest.Builder(com.example.pantry.worker.ExpirationNotificationWorker::class.java).build()
        WorkManager.getInstance(this).enqueue(oneTimeRequest)
        // --------------------------------------------------------------------------

        val expirationCheckRequest = PeriodicWorkRequestBuilder<com.example.pantry.worker.ExpirationNotificationWorker>(
            24, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ExpirationCheck",
            ExistingPeriodicWorkPolicy.KEEP,
            expirationCheckRequest
        )
    }
}