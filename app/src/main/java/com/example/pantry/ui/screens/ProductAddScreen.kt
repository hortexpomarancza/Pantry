package com.example.pantry.ui.screens

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pantry.data.model.Product
import com.example.pantry.ui.theme.*
import com.example.pantry.ui.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductAddScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToScanner: () -> Unit,
    scannedBarcode: String? = null,
    productIdToEdit: Int? = null,
    initialCategory: String? = null,
    availableCategories: List<String>,
    spaceColor: Int? = null
) {
    val context = LocalContext.current
    val isEditMode = productIdToEdit != null && productIdToEdit != -1

    var selectedCategory by rememberSaveable { mutableStateOf(initialCategory ?: availableCategories.firstOrNull() ?: "Inne") }
    var hasUserSelectedCategory by remember { mutableStateOf(initialCategory != null) }

    // --- POPRAWKA: Pobieramy kolory z ViewModelu (tam są też Twoje własne) ---
    val categoryColors = viewModel.categoryColors
    val targetCategoryColor = categoryColors[selectedCategory]?.let { Color(it) }

    val fallbackSpaceColor = if (spaceColor != null && spaceColor != 0) Color(spaceColor) else MaterialTheme.colorScheme.primary

    // Logika wyboru głównego koloru ekranu
    val mainColor = if (!hasUserSelectedCategory && !isEditMode) {
        fallbackSpaceColor
    } else {
        targetCategoryColor ?: fallbackSpaceColor
    }

    val softBackground = mainColor.copy(alpha = 0.15f).compositeOver(Color.White)

    val customTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = mainColor,
        unfocusedBorderColor = mainColor.copy(alpha = 0.5f),
        focusedLabelColor = mainColor,
        unfocusedLabelColor = Color.DarkGray,
        cursorColor = mainColor,
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        focusedContainerColor = Color.White.copy(alpha = 0.6f),
        unfocusedContainerColor = Color.White.copy(alpha = 0.6f),
        selectionColors = TextSelectionColors(handleColor = mainColor, backgroundColor = mainColor.copy(alpha = 0.4f))
    )

    var name by rememberSaveable { mutableStateOf("") }
    var barcode by rememberSaveable { mutableStateOf("") }
    var count by rememberSaveable { mutableStateOf(1) }
    var dateMillis by rememberSaveable { mutableStateOf(System.currentTimeMillis() + 1000 * 60 * 60 * 24) }
    var originalLocation by remember { mutableStateOf("Twoja Spiżarnia") }

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
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
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
                originalLocation = it.storageLocation
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
        containerColor = softBackground,
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edytuj produkt" else "Dodaj produkt", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Anuluj", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = mainColor)
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
                // Karta: Nazwa i Kod
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nazwa produktu") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            isError = showErrorBlankName,
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, tint = mainColor) },
                            shape = RoundedCornerShape(12.dp),
                            colors = customTextFieldColors
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = barcode,
                                onValueChange = { barcode = it },
                                label = { Text("Kod kreskowy") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = customTextFieldColors
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledIconButton(
                                onClick = onNavigateToScanner,
                                modifier = Modifier.size(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = mainColor)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = "Skanuj", tint = Color.White)
                            }
                        }
                    }
                }

                // Kategoria
                Column {
                    Text(text = "Kategoria", style = MaterialTheme.typography.titleSmall, color = mainColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    if (initialCategory != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = mainColor.copy(alpha = 0.2f)), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, null, tint = Color.Black)
                                Spacer(Modifier.width(8.dp))
                                Text("Dodajesz do: $initialCategory", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    } else {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            availableCategories.forEach { category ->
                                val isSelected = category == selectedCategory

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategory = category
                                        hasUserSelectedCategory = true
                                    },
                                    label = { Text(category) },
                                    leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = mainColor,
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White,
                                        containerColor = Color.White,
                                        labelColor = Color.Black
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = if (isSelected) mainColor else Color.LightGray,
                                        borderWidth = if (isSelected) 0.dp else 1.dp
                                    )
                                )
                            }
                        }
                    }
                }

                // Licznik
                Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = "Ilość sztuk:", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalIconButton(
                                onClick = { if (count > 1) count-- },
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = mainColor.copy(alpha = 0.2f), contentColor = Color.Black)
                            ) { Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold) }

                            Text(text = count.toString(), modifier = Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.headlineMedium, color = Color.Black)

                            FilledTonalIconButton(
                                onClick = { count++ },
                                modifier = Modifier.size(40.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = mainColor.copy(alpha = 0.2f), contentColor = Color.Black)
                            ) { Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                // Data
                val dateLabel = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(dateMillis))
                OutlinedCard(
                    onClick = { datePickerDialog.show() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, mainColor),
                    colors = CardDefaults.outlinedCardColors(containerColor = Color.White.copy(alpha = 0.5f))
                ) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Data ważności", style = MaterialTheme.typography.labelMedium, color = mainColor)
                            Text(dateLabel, style = MaterialTheme.typography.bodyLarge, color = Color.Black)
                        }
                        Icon(Icons.Default.CalendarToday, null, tint = mainColor)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            showErrorBlankName = true
                        } else {
                            if (isEditMode) {
                                viewModel.updateProduct(Product(id = productIdToEdit!!, name = name, expirationDate = dateMillis, barcode = barcode.ifBlank { null }, category = selectedCategory, count = count, storageLocation = originalLocation))
                            } else {
                                viewModel.addProduct(name = name, expirationDate = dateMillis, barcode = barcode.ifBlank { null }, category = selectedCategory, count = count)
                            }
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = mainColor, contentColor = Color.White)
                ) {
                    Text(if (isEditMode) "Zapisz zmiany" else "Dodaj produkt", fontSize = 18.sp, color = Color.White)
                }
            }
        }
    }
}