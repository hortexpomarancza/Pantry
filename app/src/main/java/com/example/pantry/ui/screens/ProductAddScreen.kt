package com.example.pantry.ui.screens

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pantry.R
import com.example.pantry.data.model.Product
import com.example.pantry.ui.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.*

// USUNIĘTO "Chemia" z tej listy:
val AVAILABLE_CATEGORIES = listOf("Warzywa", "Owoce", "Nabiał", "Mięso", "Pieczywo", "Napoje", "Mrożonki", "Inne")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductAddScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    scannedBarcode: String? = null,
    productIdToEdit: Int? = null,
    initialCategory: String? = null
) {
    val context = LocalContext.current
    val isEditMode = productIdToEdit != null && productIdToEdit != -1

    var selectedCategory by rememberSaveable { mutableStateOf(initialCategory ?: "Inne") }

    var name by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf(1) }
    var dateMillis by rememberSaveable {
        mutableStateOf(System.currentTimeMillis() + 1000 * 60 * 60 * 24)
    }

    var isLoading by remember { mutableStateOf(isEditMode) }
    var showErrorBlankName by rememberSaveable { mutableStateOf(false) }

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

    LaunchedEffect(productIdToEdit) {
        if (isEditMode) {
            val product = viewModel.getProductById(productIdToEdit!!)
            product?.let {
                name = it.name
                barcode = it.barcode ?: ""
                dateMillis = it.expirationDate
                selectedCategory = it.category
                count = it.count
            }
            isLoading = false
        }
    }

    LaunchedEffect(scannedBarcode) { if (scannedBarcode != null) barcode = scannedBarcode }

    LaunchedEffect(barcode) {
        if (barcode.isNotBlank() && name.isBlank() && !isLoading) {
            try {
                val nameByBarcode = viewModel.getProductNameByBarcode(barcode)
                if (!nameByBarcode.isNullOrBlank()) name = nameByBarcode
            } catch (e: Exception) { Log.e("AddScreen", "Error", e) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edytuj produkt" else stringResource(R.string.add_product)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Anuluj")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (!isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Nazwa i kod
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(1.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.product_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = showErrorBlankName,
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = barcode,
                                onValueChange = { barcode = it },
                                label = { Text(stringResource(R.string.barcode)) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(onClick = onNavigateToScanner, modifier = Modifier.size(56.dp), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan")
                            }
                        }
                    }
                }

                // Kategoria
                Column {
                    Text(text = "Kategoria", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))

                    if (initialCategory != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Dodajesz do: $initialCategory", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AVAILABLE_CATEGORIES.forEach { category ->
                                FilterChip(
                                    selected = category == selectedCategory,
                                    onClick = { selectedCategory = category },
                                    label = { Text(category) },
                                    leadingIcon = if (category == selectedCategory) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null
                                )
                            }
                        }
                    }
                }

                // Licznik
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Ilość sztuk:", style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(onClick = { if (count > 1) count-- }, modifier = Modifier.size(40.dp)) { Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                            Text(text = count.toString(), modifier = Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.headlineMedium)
                            FilledTonalIconButton(onClick = { count++ }, modifier = Modifier.size(40.dp)) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                // Data
                val dateLabel = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))
                OutlinedCard(onClick = { datePickerDialog.show() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(stringResource(R.string.expiration_date), style = MaterialTheme.typography.labelMedium); Text(dateLabel, style = MaterialTheme.typography.bodyLarge) }
                        Icon(Icons.Default.CalendarToday, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            showErrorBlankName = true
                        } else {
                            if (isEditMode) {
                                viewModel.updateProduct(Product(id = productIdToEdit!!, name = name, expirationDate = dateMillis, barcode = barcode.ifBlank { null }, category = selectedCategory, count = count))
                            } else {
                                viewModel.addProduct(name = name, expirationDate = dateMillis, barcode = barcode.ifBlank { null }, category = selectedCategory, count = count)
                            }
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(if (isEditMode) "Zapisz zmiany" else stringResource(R.string.add), fontSize = 18.sp)
                }
            }
        }
    }
}