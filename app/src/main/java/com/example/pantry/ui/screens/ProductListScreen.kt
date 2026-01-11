package com.example.pantry.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.pantry.data.model.Product
import com.example.pantry.ui.theme.*
import com.example.pantry.ui.viewmodel.ProductViewModel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// --- STAŁE ---
val INITIAL_CATEGORIES = listOf(
    "Warzywa i Owoce", "Nabiał", "Mięso", "Pieczywo",
    "Napoje", "Mrożonki", "Inne"
)

val AVAILABLE_ICONS = listOf(
    Icons.Rounded.Eco, Icons.Rounded.Egg, Icons.Rounded.RestaurantMenu,
    Icons.Rounded.BreakfastDining, Icons.Rounded.LocalDrink, Icons.Rounded.AcUnit,
    Icons.Rounded.Cookie, Icons.Rounded.CleaningServices, Icons.Rounded.Pets,
    Icons.Rounded.Spa, Icons.Rounded.Fastfood, Icons.Rounded.LocalCafe,
    Icons.Rounded.Kitchen, Icons.Rounded.Cake, Icons.Rounded.LocalPizza,
    Icons.Rounded.Icecream, Icons.Rounded.SetMeal, Icons.Rounded.Liquor
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    onNavigateToAdd: (String?) -> Unit,
    onNavigateToEdit: (Product) -> Unit,
    currentGlobalColor: Color,
    onGlobalColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    val products by viewModel.allProducts.observeAsState(initial = emptyList())

    // --- MAPA KOLORÓW ---
    val initialColorMap = remember {
        mapOf(
            "Warzywa i Owoce" to TileColorVeg.toArgb(),
            "Nabiał" to TileColorDairy.toArgb(),
            "Mięso" to TileColorMeat.toArgb(),
            "Mrożonki" to TileColorFrozen.toArgb(),
            "Napoje" to TileColorDrinks.toArgb(),
            "Pieczywo" to TileColorBread.toArgb(),
            "Inne" to TileColorOther.toArgb()
        )
    }

    // --- STANY ---
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isCategoryListView by remember { mutableStateOf(false) }
    var currentCategories by remember { mutableStateOf(INITIAL_CATEGORIES) }

    var categoryColorsHex by remember { mutableStateOf(initialColorMap) }
    var customCategoryIcons by remember { mutableStateOf(mapOf<String, ImageVector>()) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showCategoryOptionsDialog by remember { mutableStateOf(false) }
    var showGlobalColorPicker by remember { mutableStateOf(false) }

    var showDeleteWithProductsDialog by remember { mutableStateOf<String?>(null) }
    var categoryToEditColor by remember { mutableStateOf<String?>(null) }

    // --- FUNKCJE POMOCNICZE ---
    fun getCategoryColor(catName: String): Color {
        return categoryColorsHex[catName]?.let { Color(it) } ?: Color.White
    }

    val targetTopBarColor = if (selectedCategory != null) {
        getCategoryColor(selectedCategory!!)
    } else {
        currentGlobalColor
    }

    val animatedTopBarColor by animateColorAsState(
        targetValue = targetTopBarColor,
        animationSpec = tween(500),
        label = "TopBarColorAnimation"
    )

    val contentColor = if (selectedCategory != null) Color.Black else Color.White
    val fabContainerColor = if (selectedCategory == null) currentGlobalColor else getCategoryColor(selectedCategory!!)
    val fabContentColor = if (selectedCategory == null) Color.White else Color.Black

    BackHandler(enabled = selectedCategory != null) {
        selectedCategory = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = selectedCategory ?: "Twoja Spiżarnia",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (selectedCategory != null) {
                        IconButton(onClick = { selectedCategory = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Wróć")
                        }
                    }
                },
                actions = {
                    if (selectedCategory == null) {
                        IconButton(onClick = { isCategoryListView = !isCategoryListView }) {
                            Icon(
                                imageVector = if (isCategoryListView) Icons.Default.GridView else Icons.Default.ViewList,
                                contentDescription = "Widok"
                            )
                        }
                        IconButton(onClick = { showGlobalColorPicker = true }) {
                            Icon(Icons.Outlined.Palette, contentDescription = "Kolor aplikacji")
                        }
                    } else {
                        IconButton(onClick = { showCategoryOptionsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Opcje")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = animatedTopBarColor,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor,
                    actionIconContentColor = contentColor
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAdd(selectedCategory) },
                containerColor = fabContainerColor,
                contentColor = fabContentColor
            ) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj produkt")
            }
        }
    ) { padding ->

        Crossfade(targetState = selectedCategory, label = "ViewTransition") { category ->
            if (category == null) {
                // --- EKRAN GŁÓWNY ---
                val usedInProducts = products.map { it.category }
                val allCats = (currentCategories + usedInProducts).distinct()

                if (allCats.size > currentCategories.size) {
                    currentCategories = allCats
                }

                val onReorderCategories: (Int, Int) -> Unit = { fromIndex, toIndex ->
                    val list = currentCategories.toMutableList()
                    val item = list.removeAt(fromIndex)
                    list.add(toIndex, item)
                    currentCategories = list
                }

                if (isCategoryListView) {
                    // --- DRAGGABLE LIST ---
                    DraggableCategoryList(
                        categories = currentCategories,
                        products = products,
                        categoryColors = categoryColorsHex,
                        customIcons = customCategoryIcons,
                        onCategoryClick = { selectedCategory = it },
                        onAddCategoryClick = { showAddCategoryDialog = true },
                        onReorder = onReorderCategories,
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    // --- DRAGGABLE GRID ---
                    DraggableCategoryGrid(
                        categories = currentCategories,
                        products = products,
                        categoryColors = categoryColorsHex,
                        customIcons = customCategoryIcons,
                        onCategoryClick = { selectedCategory = it },
                        onAddCategoryClick = { showAddCategoryDialog = true },
                        onReorder = onReorderCategories,
                        modifier = Modifier.padding(padding)
                    )
                }
            } else {
                // --- EKRAN KATEGORII (PRODUKTY) ---
                val filteredProducts = products.filter { it.category == category }
                val bgColor = getCategoryColor(category).copy(alpha = 0.2f)

                Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
                    if (filteredProducts.isEmpty()) {
                        Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Pusto tutaj...", color = Color.Gray)
                        }
                    } else {
                        ProductListView(
                            products = filteredProducts,
                            onProductClick = { product ->
                                if (product.count > 1) {
                                    viewModel.updateProduct(product.copy(count = product.count - 1))
                                } else {
                                    viewModel.deleteProduct(product)
                                    Toast.makeText(context, "Zużyto: ${product.name}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onProductLongClick = { onNavigateToEdit(it) },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }

        // --- DIALOGI ---
        if (showAddCategoryDialog) {
            AdvancedAddCategoryDialog(
                onDismiss = { showAddCategoryDialog = false },
                onConfirm = { name, color, icon ->
                    if (name.isNotBlank() && !currentCategories.contains(name)) {
                        currentCategories = currentCategories + name
                        val newColors = categoryColorsHex.toMutableMap()
                        newColors[name] = color.toArgb()
                        categoryColorsHex = newColors

                        val newIcons = customCategoryIcons.toMutableMap()
                        newIcons[name] = icon
                        customCategoryIcons = newIcons
                    }
                    showAddCategoryDialog = false
                }
            )
        }

        if (showCategoryOptionsDialog && selectedCategory != null) {
            CategoryOptionsDialog(
                categoryName = selectedCategory!!,
                onDismiss = { showCategoryOptionsDialog = false },
                onDelete = {
                    val catToDelete = selectedCategory!!
                    val hasProducts = products.any { it.category == catToDelete }

                    if (hasProducts) {
                        showCategoryOptionsDialog = false
                        showDeleteWithProductsDialog = catToDelete
                    } else {
                        currentCategories = currentCategories - catToDelete
                        selectedCategory = null
                        showCategoryOptionsDialog = false
                        Toast.makeText(context, "Usunięto kategorię", Toast.LENGTH_SHORT).show()
                    }
                },
                onChangeColor = {
                    showCategoryOptionsDialog = false
                    categoryToEditColor = selectedCategory
                }
            )
        }

        if (showDeleteWithProductsDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteWithProductsDialog = null },
                title = { Text("Kategoria nie jest pusta") },
                text = { Text("W tej kategorii znajdują się produkty. Czy chcesz usunąć kategorię wraz ze wszystkimi produktami w środku?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val catToDelete = showDeleteWithProductsDialog!!
                            viewModel.deleteProductsByCategory(catToDelete)
                            currentCategories = currentCategories - catToDelete
                            selectedCategory = null
                            showDeleteWithProductsDialog = null
                            Toast.makeText(context, "Usunięto kategorię i produkty", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Tak, usuń wszystko")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteWithProductsDialog = null }) { Text("Anuluj") }
                }
            )
        }

        if (categoryToEditColor != null) {
            ColorPickerDialog(
                title = "Kolor kategorii: $categoryToEditColor",
                onDismiss = { categoryToEditColor = null },
                onColorSelected = { newColor ->
                    val newColors = categoryColorsHex.toMutableMap()
                    newColors[categoryToEditColor!!] = newColor.toArgb()
                    categoryColorsHex = newColors
                    categoryToEditColor = null
                }
            )
        }

        if (showGlobalColorPicker) {
            ColorPickerDialog(
                title = "Kolor paska aplikacji",
                onDismiss = { showGlobalColorPicker = false },
                onColorSelected = { newColor ->
                    onGlobalColorChange(newColor)
                    showGlobalColorPicker = false
                }
            )
        }
    }
}

// --- DRAGGABLE IMPLEMENTATIONS ---

@Composable
fun DraggableCategoryGrid(
    categories: List<String>,
    products: List<Product>,
    categoryColors: Map<String, Int>,
    customIcons: Map<String, ImageVector>,
    onCategoryClick: (String) -> Unit,
    onAddCategoryClick: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingItemOffset by remember { mutableStateOf(Offset.Zero) }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        gridState.layoutInfo.visibleItemsInfo
                            .firstOrNull { item ->
                                offset.y >= item.offset.y && offset.y <= (item.offset.y + item.size.height) &&
                                        offset.x >= item.offset.x && offset.x <= (item.offset.x + item.size.width)
                            }
                            ?.let { item ->
                                if (item.index < categories.size) {
                                    draggingItemIndex = item.index
                                    draggingItemOffset = Offset.Zero
                                }
                            }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggingItemOffset += dragAmount

                        val currentDraggingIndex = draggingItemIndex ?: return@detectDragGesturesAfterLongPress

                        val currentItemInfo = gridState.layoutInfo.visibleItemsInfo.find { it.index == currentDraggingIndex }
                        val itemCenter = currentItemInfo?.let {
                            Offset(it.offset.x + it.size.width / 2f, it.offset.y + it.size.height / 2f)
                        } ?: return@detectDragGesturesAfterLongPress

                        val dragPosition = itemCenter + draggingItemOffset

                        val targetItem = gridState.layoutInfo.visibleItemsInfo.find { item ->
                            dragPosition.y >= item.offset.y && dragPosition.y <= (item.offset.y + item.size.height) &&
                                    dragPosition.x >= item.offset.x && dragPosition.x <= (item.offset.x + item.size.width)
                        }

                        if (targetItem != null && targetItem.index != currentDraggingIndex && targetItem.index < categories.size) {
                            onReorder(currentDraggingIndex, targetItem.index)
                            draggingItemIndex = targetItem.index
                            draggingItemOffset = Offset.Zero
                        }
                    },
                    onDragEnd = { draggingItemIndex = null; draggingItemOffset = Offset.Zero },
                    onDragCancel = { draggingItemIndex = null; draggingItemOffset = Offset.Zero }
                )
            }
    ) {
        items(categories, key = { it }) { category ->
            val index = categories.indexOf(category)
            val isDragging = index == draggingItemIndex

            val scale by animateFloatAsState(if (isDragging) 1.1f else 1f, label = "scale")
            val zIndex = if (isDragging) 1f else 0f
            val shadowElevation = if (isDragging) 8.dp else 2.dp

            val count = products.count { it.category == category }
            val tileColor = categoryColors[category]?.let { Color(it) } ?: Color.White
            val icon = customIcons[category] ?: getThematicIcon(category)

            Box(
                modifier = Modifier
                    .zIndex(zIndex)
                    .scale(scale)
                    .shadow(shadowElevation, RoundedCornerShape(24.dp))
            ) {
                CategoryTile(
                    name = category,
                    count = count,
                    color = tileColor,
                    icon = icon,
                    onClick = { if (!isDragging) onCategoryClick(category) }
                )
            }
        }
        item { AddCategoryTile(onAddCategoryClick) }
    }
}

@Composable
fun DraggableCategoryList(
    categories: List<String>,
    products: List<Product>,
    categoryColors: Map<String, Int>,
    customIcons: Map<String, ImageVector>,
    onCategoryClick: (String) -> Unit,
    onAddCategoryClick: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingItemOffset by remember { mutableStateOf(Offset.Zero) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        // POPRAWKA: W LazyColumn offset i size to Int, nie Float/Offset
                        listState.layoutInfo.visibleItemsInfo
                            .firstOrNull { item ->
                                // Sprawdzamy czy dotyk jest w pionowych granicach elementu
                                // item.offset to pozycja Y (góra elementu)
                                // item.size to wysokość elementu
                                offset.y.toInt() >= item.offset && offset.y.toInt() <= (item.offset + item.size)
                            }
                            ?.let { item ->
                                if (item.index < categories.size) {
                                    draggingItemIndex = item.index
                                    draggingItemOffset = Offset.Zero
                                }
                            }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        draggingItemOffset += dragAmount

                        val currentDraggingIndex = draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                        val currentItemInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == currentDraggingIndex }

                        // Obliczamy środek elementu (offset to Int, size to Int)
                        val itemCenterY = currentItemInfo?.let {
                            it.offset + (it.size / 2f)
                        } ?: return@detectDragGesturesAfterLongPress

                        val dragPositionY = itemCenterY + draggingItemOffset.y

                        // Znajdujemy cel
                        val targetItem = listState.layoutInfo.visibleItemsInfo.find { item ->
                            dragPositionY.toInt() >= item.offset && dragPositionY.toInt() <= (item.offset + item.size)
                        }

                        if (targetItem != null && targetItem.index != currentDraggingIndex && targetItem.index < categories.size) {
                            onReorder(currentDraggingIndex, targetItem.index)
                            draggingItemIndex = targetItem.index
                            draggingItemOffset = Offset.Zero
                        }
                    },
                    onDragEnd = { draggingItemIndex = null; draggingItemOffset = Offset.Zero },
                    onDragCancel = { draggingItemIndex = null; draggingItemOffset = Offset.Zero }
                )
            }
    ) {
        items(categories, key = { it }) { category ->
            val index = categories.indexOf(category)
            val isDragging = index == draggingItemIndex

            val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
            val zIndex = if (isDragging) 1f else 0f
            val shadowElevation = if (isDragging) 8.dp else 0.dp

            val count = products.count { it.category == category }
            val tileColor = categoryColors[category]?.let { Color(it) } ?: Color.White
            val icon = customIcons[category] ?: getThematicIcon(category)

            Box(
                modifier = Modifier
                    .zIndex(zIndex)
                    .scale(scale)
                    .shadow(shadowElevation, RoundedCornerShape(20.dp))
            ) {
                CategoryListRow(
                    name = category,
                    count = count,
                    color = tileColor,
                    icon = icon,
                    onClick = { if (!isDragging) onCategoryClick(category) }
                )
            }
        }
        item {
            Button(onClick = onAddCategoryClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Dodaj kategorię")
            }
        }
    }
}

// --- ZWYKŁE WIDOKI KATEGORII ---

@Composable
fun CategoryTile(name: String, count: Int, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.height(130.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.8f))
            Text(text = if(count == 0) "Pusto" else "$count szt.", style = MaterialTheme.typography.bodySmall, color = Color.Black.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun CategoryListRow(name: String, count: Int, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(80.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.Black.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.Black.copy(alpha = 0.8f))
                if (count > 0) Text(text = "$count szt.", style = MaterialTheme.typography.bodyMedium, color = Color.Black.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun AddCategoryTile(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
        modifier = Modifier.height(130.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(40.dp))
            Text("Dodaj", fontWeight = FontWeight.Bold, color = Color.Gray)
        }
    }
}

@Composable
fun ColorPickerDialog(title: String, onDismiss: () -> Unit, onColorSelected: (Color) -> Unit) {
    val colors = CategoryPalette + listOf(AppTopBarBrown, Color(0xFF607D8B), Color(0xFF795548), Color.DarkGray)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(colors) { color ->
                    Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color).clickable { onColorSelected(color) }.border(1.dp, Color.Gray, CircleShape))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

@Composable
fun CategoryOptionsDialog(categoryName: String, onDismiss: () -> Unit, onDelete: () -> Unit, onChangeColor: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(categoryName, fontWeight = FontWeight.Bold) },
        text = { Text("Zarządzaj kategorią") },
        confirmButton = {
            Button(onClick = onChangeColor, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                Icon(Icons.Outlined.Palette, null); Spacer(Modifier.width(8.dp)); Text("Zmień Kolor")
            }
        },
        dismissButton = {
            Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(8.dp)); Text("Usuń")
            }
        }
    )
}

@Composable
fun ProductListView(products: List<Product>, onProductClick: (Product) -> Unit, onProductLongClick: (Product) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)) {
        items(products, key = { it.id }) { product ->
            ProductRowItem(product, onClick = { onProductClick(product) }, onLongClick = { onProductLongClick(product) })
        }
    }
}

@Composable
fun ProductRowItem(product: Product, onClick: () -> Unit, onLongClick: () -> Unit) {
    val daysRemaining = calculateDaysRemaining(product.expirationDate)
    val statusColor = when {
        daysRemaining < 0 -> StatusExpired
        daysRemaining < 2 -> StatusWarning
        else -> StatusFresh
    }
    val statusText = when {
        daysRemaining < 0 -> "Po terminie!"
        daysRemaining == 0L -> "Dzisiaj"
        daysRemaining == 1L -> "Jutro"
        daysRemaining <= 7 -> "$daysRemaining dni"
        else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(product.expirationDate))
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).pointerInput(product) {
            detectTapGestures(onLongPress = { onLongClick() }, onTap = { onClick() })
        },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(6.dp).height(40.dp).clip(RoundedCornerShape(4.dp)).background(statusColor))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, null, Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = if (daysRemaining <= 3) statusColor else Color.Gray, fontWeight = if (daysRemaining <= 3) FontWeight.Bold else FontWeight.Normal)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) {
                Text(text = "${product.count}x", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AdvancedAddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, Color, ImageVector) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(CategoryPalette[0]) }
    var selectedIcon by remember { mutableStateOf(AVAILABLE_ICONS[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowa Kategoria") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nazwa") }, singleLine = true)
                Text("Kolor:", style = MaterialTheme.typography.labelLarge)
                LazyVerticalGrid(columns = GridCells.Adaptive(40.dp), modifier = Modifier.height(80.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CategoryPalette) { color ->
                        Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color).clickable { selectedColor = color }.border(if(selectedColor == color) 2.dp else 0.dp, Color.Black, CircleShape))
                    }
                }
                Text("Ikona:", style = MaterialTheme.typography.labelLarge)
                LazyVerticalGrid(columns = GridCells.Adaptive(40.dp), modifier = Modifier.height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AVAILABLE_ICONS) { icon ->
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(if(selectedIcon == icon) Color.LightGray else Color.Transparent).clickable { selectedIcon = icon }, contentAlignment = Alignment.Center) {
                            Icon(icon, null)
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(name, selectedColor, selectedIcon) }, enabled = name.isNotBlank()) { Text("Zapisz") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } }
    )
}

fun getThematicIcon(category: String): ImageVector {
    return when {
        category.contains("Warzyw", true) -> Icons.Rounded.Eco
        category.contains("Nabiał", true) -> Icons.Rounded.Egg
        category.contains("Mięso", true) -> Icons.Rounded.RestaurantMenu
        category.contains("Pieczywo", true) -> Icons.Rounded.BreakfastDining
        category.contains("Napoje", true) -> Icons.Rounded.LocalDrink
        category.contains("Mrożonki", true) -> Icons.Rounded.AcUnit
        category.contains("Inne", true) -> Icons.Rounded.AllInclusive
        else -> Icons.Rounded.Category
    }
}

fun calculateDaysRemaining(expirationDate: Long): Long {
    val diff = expirationDate - System.currentTimeMillis()
    return TimeUnit.MILLISECONDS.toDays(diff)
}