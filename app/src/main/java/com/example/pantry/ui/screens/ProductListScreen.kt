package com.example.pantry.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pantry.R
import com.example.pantry.data.model.Product
import com.example.pantry.ui.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Product) -> Unit
) {
    val products by viewModel.allProducts.observeAsState(initial = emptyList())

    var promptedProduct by rememberSaveable { mutableStateOf<Product?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(products) { product ->
                ProductItem(
                    product = product,
                    onLongPress = { promptedProduct = product },
                    onClick = { onNavigateToEdit(product) }
                )
            }
        }

        if (promptedProduct != null) {
            AlertDialog(
                onDismissRequest = { promptedProduct = null },
                title = { Text(text = stringResource(R.string.delete_product)) },
                text = { Text(text = stringResource(R.string.delete_product_prompt, promptedProduct!!.name)) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            promptedProduct?.let { viewModel.deleteProduct(it) }
                            promptedProduct = null
                        }
                    ) { Text(text = stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { promptedProduct = null }
                    ) { Text(text = stringResource(R.string.cancel)) }
                }
            )
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .pointerInput(product) {
                detectTapGestures(
                    onLongPress = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongPress()
                    },
                    onTap = { onClick() }
                )
            }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(product.expirationDate)
            Text(text = product.name)
            Text(
                text = stringResource(R.string.best_before, formattedDate),
                color = if (product.expirationDate > System.currentTimeMillis()) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}