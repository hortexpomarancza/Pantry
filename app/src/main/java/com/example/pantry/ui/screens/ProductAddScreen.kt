package com.example.pantry.ui.screens

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.pantry.R
import com.example.pantry.ui.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductAddScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    scannedBarcode: String? = null
) {
    val context = LocalContext.current

    var name by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var dateMillis by rememberSaveable {
        mutableStateOf(System.currentTimeMillis() + 1000 * 60 * 60 * 24)
    }
    val dateLabel = rememberSaveable(dateMillis) {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(dateMillis))
    }

    var showErrorBlankName by rememberSaveable { mutableStateOf(false) }
    var showErrorDateMismatch by rememberSaveable { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val pickedDate = Calendar.getInstance()
            pickedDate.set(year, month, dayOfMonth)
            dateMillis = pickedDate.timeInMillis
            },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    LaunchedEffect(scannedBarcode) {
        if (scannedBarcode != null) barcode = scannedBarcode
    }

    LaunchedEffect(barcode) {
        if (barcode.isNotBlank()) {
            try {
                val nameByBarcode = viewModel.getProductNameByBarcode(barcode)
                if (!nameByBarcode.isNullOrBlank()) {
                    name = nameByBarcode
                }
            } catch (e: Exception) {
                Log.e("ProductAddScreen", "Fetching error", e)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar( title = { Text(text = stringResource(R.string.add_product)) })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement
                .spacedBy(16.dp)
        ) {
            TextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(text = stringResource(R.string.product_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            TextField(
                value = barcode,
                onValueChange = { barcode = it },
                label = { Text(text = stringResource(R.string.barcode)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = onNavigateToScanner) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.scan_barcode))
                    }
                }
            )

            TextField(
                value = dateLabel,
                onValueChange = { },
                label = { Text(text = stringResource(R.string.expiration_date)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    TextButton(onClick = { datePickerDialog.show() }) {
                        Text(text = stringResource(R.string.select_date))
                    }
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            if (showErrorBlankName) {
                Text(
                    text = stringResource(R.string.error_cannot_be_empty),
                    color = MaterialTheme.colorScheme.error
                )
            } else if (showErrorDateMismatch) {
                Text(
                    text = stringResource(R.string.error_date_mismatch),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Button(
                onClick = {
                    val isNameBlank = name.isBlank()
                    val isDateMismatch = dateMillis < System.currentTimeMillis()

                    showErrorBlankName = isNameBlank
                    showErrorDateMismatch = isDateMismatch

                    if (!isNameBlank && !isDateMismatch) {
                        viewModel.addProduct(
                            name,
                            dateMillis,
                            barcode)
                        onNavigateBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.add))
            }
        }
    }
}