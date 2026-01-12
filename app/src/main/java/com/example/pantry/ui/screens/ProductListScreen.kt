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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import com.example.pantry.data.model.Product
import com.example.pantry.ui.theme.*
import com.example.pantry.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

val DEFAULT_START_CATEGORIES = listOf(
    "Warzywa i Owoce", "Nabiał", "Mięso", "Pieczywo",
    "Napoje", "Mrożonki", "Inne"
)

val AVAILABLE_ICONS = listOf(Icons.Rounded.Eco, Icons.Rounded.Egg, Icons.Rounded.RestaurantMenu, Icons.Rounded.BreakfastDining, Icons.Rounded.LocalDrink, Icons.Rounded.AcUnit, Icons.Rounded.Cookie, Icons.Rounded.CleaningServices, Icons.Rounded.Pets, Icons.Rounded.Spa, Icons.Rounded.Fastfood, Icons.Rounded.LocalCafe, Icons.Rounded.Kitchen, Icons.Rounded.Cake, Icons.Rounded.LocalPizza, Icons.Rounded.Icecream, Icons.Rounded.SetMeal, Icons.Rounded.Liquor)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    onNavigateToAdd: (String?, List<String>, Int) -> Unit,
    onNavigateToEdit: (Product, Int) -> Unit,
    currentGlobalColor: Color,
    onGlobalColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val products by viewModel.currentProducts.observeAsState(initial = emptyList())
    val mainAppColor = AppTopBarBrown
    val softBackground = mainAppColor.copy(alpha = 0.15f).compositeOver(Color.White)

    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isCategoryListView by remember { mutableStateOf(false) }

    val currentCategories = viewModel.categories
    val categoryColorsMap = viewModel.categoryColors
    val customIconsMap = viewModel.customIcons

    val pagerState = rememberPagerState(pageCount = { 2 })

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showCategoryOptionsDialog by remember { mutableStateOf(false) }
    var showDeleteWithProductsDialog by remember { mutableStateOf<String?>(null) }
    var categoryToEditColor by remember { mutableStateOf<String?>(null) }

    fun getCategoryColor(catName: String): Color {
        return categoryColorsMap[catName]?.let { Color(it) } ?: Color.Gray
    }

    val targetTopBarColor = if (selectedCategory != null) getCategoryColor(selectedCategory!!) else mainAppColor
    val animatedTopBarColor by animateColorAsState(targetValue = targetTopBarColor, animationSpec = tween(500), label = "TopBarColorAnimation")

    val fabContainerColor = if (selectedCategory == null) mainAppColor else getCategoryColor(selectedCategory!!)

    BackHandler(enabled = selectedCategory != null) { selectedCategory = null }

    Scaffold(
        containerColor = softBackground,
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when {
                        selectedCategory != null -> selectedCategory!!
                        pagerState.currentPage == 0 -> "Twoja Spiżarnia"
                        else -> "Terminy Ważności"
                    }
                    Text(text = titleText, fontWeight = FontWeight.Bold, color = Color.White)
                },
                navigationIcon = {
                    if (selectedCategory != null) {
                        IconButton(onClick = { selectedCategory = null }) { Icon(Icons.Default.ArrowBack, contentDescription = "Wróć", tint = Color.White) }
                    } else {
                        Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.padding(start = 12.dp))
                    }
                },
                actions = {
                    if (pagerState.currentPage == 0) {
                        if (selectedCategory == null) {
                            IconButton(onClick = { isCategoryListView = !isCategoryListView }) { Icon(imageVector = if (isCategoryListView) Icons.Default.GridView else Icons.Default.ViewList, contentDescription = "Widok", tint = Color.White) }
                        } else {
                            IconButton(onClick = { showCategoryOptionsDialog = true }) { Icon(Icons.Default.Settings, contentDescription = "Opcje", tint = Color.White) }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = animatedTopBarColor, titleContentColor = Color.White, actionIconContentColor = Color.White)
            )
        },
        floatingActionButton = {
            if (pagerState.currentPage == 0) {
                FloatingActionButton(
                    onClick = {
                        val colorToSend = if (selectedCategory != null) getCategoryColor(selectedCategory!!).toArgb() else mainAppColor.toArgb()
                        onNavigateToAdd(selectedCategory, currentCategories, colorToSend)
                    },
                    containerColor = fabContainerColor,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Dodaj produkt")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            if (selectedCategory == null) {
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = animatedTopBarColor,
                    contentColor = Color.White,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                            color = Color.White
                        )
                    }
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = { Text("KATEGORIE", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Category, null) }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = { Text("TERMINY", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.DateRange, null) }
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = selectedCategory == null,
                modifier = Modifier.weight(1f)
            ) { page ->
                if (page == 0) {
                    Crossfade(targetState = selectedCategory, label = "ViewTransition") { category ->
                        if (category == null) {
                            val onReorderCategories: (Int, Int) -> Unit = { from, to -> viewModel.moveCategory(from, to) }

                            if (isCategoryListView) {
                                DraggableCategoryList(
                                    categories = currentCategories,
                                    products = products,
                                    categoryColors = categoryColorsMap.toMap(),
                                    customIcons = customIconsMap.toMap(),
                                    onCategoryClick = { selectedCategory = it },
                                    onAddCategoryClick = { showAddCategoryDialog = true },
                                    onReorder = onReorderCategories,
                                    addButtonColor = mainAppColor
                                )
                            } else {
                                DraggableCategoryGrid(
                                    categories = currentCategories,
                                    products = products,
                                    categoryColors = categoryColorsMap.toMap(),
                                    customIcons = customIconsMap.toMap(),
                                    onCategoryClick = { selectedCategory = it },
                                    onAddCategoryClick = { showAddCategoryDialog = true },
                                    onReorder = onReorderCategories
                                )
                            }
                        } else {
                            val filteredProducts = products.filter { it.category == category }
                            val bgColor = getCategoryColor(category).copy(alpha = 0.2f).compositeOver(Color.White)

                            Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
                                if (filteredProducts.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Pusto tutaj...", color = Color.DarkGray) }
                                } else {
                                    ProductListView(
                                        products = filteredProducts,
                                        onProductClick = { product ->
                                            if (product.count > 1) viewModel.updateProduct(product.copy(count = product.count - 1))
                                            else { viewModel.deleteProduct(product); Toast.makeText(context, "Zużyto: ${product.name}", Toast.LENGTH_SHORT).show() }
                                        },
                                        onProductLongClick = { product ->
                                            val color = getCategoryColor(product.category).toArgb()
                                            onNavigateToEdit(product, color)
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val timelineProducts = products.filter { it.expirationDate != null }

                    ExpirationTimelineView(
                        products = timelineProducts,
                        getCategoryColor = { getCategoryColor(it) },
                        onProductClick = { product ->
                            if (product.count > 1) viewModel.updateProduct(product.copy(count = product.count - 1))
                            else { viewModel.deleteProduct(product); Toast.makeText(context, "Zużyto: ${product.name}", Toast.LENGTH_SHORT).show() }
                        },
                        onProductLongClick = { product ->
                            val color = getCategoryColor(product.category).toArgb()
                            onNavigateToEdit(product, color)
                        }
                    )
                }
            }
        }

        if (pagerState.currentPage == 0) {
            if (showAddCategoryDialog) {
                AdvancedAddCategoryDialog(
                    onDismiss = { showAddCategoryDialog = false },
                    onConfirm = { name, color, icon ->
                        if (name.isNotBlank()) viewModel.addSessionCategory(name, color.toArgb(), icon)
                        showAddCategoryDialog = false
                    }
                )
            }
            if (showCategoryOptionsDialog && selectedCategory != null) {
                CategoryOptionsDialog(categoryName = selectedCategory!!, onDismiss = { showCategoryOptionsDialog = false }, onDelete = { val cat = selectedCategory!!; if (products.any { it.category == cat }) { showCategoryOptionsDialog = false; showDeleteWithProductsDialog = cat } else { viewModel.removeSessionCategory(cat); selectedCategory = null; showCategoryOptionsDialog = false } }, onChangeColor = { showCategoryOptionsDialog = false; categoryToEditColor = selectedCategory })
            }
            if (showDeleteWithProductsDialog != null) {
                AlertDialog(onDismissRequest = { showDeleteWithProductsDialog = null }, title = { Text("Kategoria niepusta") }, text = { Text("Usunąć z produktami?") }, confirmButton = { Button(onClick = { viewModel.deleteProductsByCategory(showDeleteWithProductsDialog!!); selectedCategory = null; showDeleteWithProductsDialog = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Usuń") } }, dismissButton = { TextButton(onClick = { showDeleteWithProductsDialog = null }) { Text("Anuluj") } })
            }
            if (categoryToEditColor != null) {
                ColorPickerDialog(title = "Kolor kategorii", onDismiss = { categoryToEditColor = null }, onColorSelected = { viewModel.updateCategoryColor(categoryToEditColor!!, it.toArgb()); categoryToEditColor = null })
            }
        }
    }
}

@Composable
fun ExpirationTimelineView(
    products: List<Product>,
    getCategoryColor: (String) -> Color,
    onProductClick: (Product) -> Unit,
    onProductLongClick: (Product) -> Unit
) {
    val sortedProducts = remember(products) {
        products.sortedBy { it.expirationDate }
    }

    if (sortedProducts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.DateRange, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Brak produktów z terminem ważności", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    "Oś czasu produktów",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(sortedProducts, key = { it.id }) { product ->
                ProductRowWithCategoryTag(
                    product = product,
                    categoryColor = getCategoryColor(product.category),
                    onClick = { onProductClick(product) },
                    onLongClick = { onProductLongClick(product) }
                )
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun ProductRowWithCategoryTag(
    product: Product,
    categoryColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val daysRemaining = calculateDaysRemaining(product.expirationDate)

    val statusColor = when {
        daysRemaining == null -> Color.Gray
        daysRemaining < 0 -> StatusExpired
        daysRemaining < 2 -> StatusWarning
        else -> StatusFresh
    }

    val statusText = when {
        daysRemaining == null -> "Bezterminowo"
        daysRemaining < 0 -> "Po terminie!"
        daysRemaining == 0L -> "Dzisiaj"
        daysRemaining == 1L -> "Jutro"
        daysRemaining <= 7 -> "$daysRemaining dni"
        else -> SimpleDateFormat("dd.MM", Locale.getDefault()).format(Date(product.expirationDate!!))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(product) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = categoryColor,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                            Text(
                                text = product.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Timer, null, Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (daysRemaining != null && daysRemaining <= 3) statusColor else Color.Gray,
                        fontWeight = if (daysRemaining != null && daysRemaining <= 3) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${product.count}x",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- FUNKCJE POMOCNICZE (Draggable Grid/List) ---

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
                        val hitItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                            val x = item.offset.x; val y = item.offset.y
                            offset.x >= x && offset.x <= x + item.size.width && offset.y >= y && offset.y <= y + item.size.height
                        }
                        hitItem?.let { if (it.index < categories.size) { draggingItemIndex = it.index; draggingItemOffset = Offset.Zero } }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume(); draggingItemOffset += dragAmount
                        val currentIndex = draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                        val currentInfo = gridState.layoutInfo.visibleItemsInfo.find { it.index == currentIndex }
                        if (currentInfo != null) {
                            val centerX = currentInfo.offset.x + currentInfo.size.width / 2f + draggingItemOffset.x
                            val centerY = currentInfo.offset.y + currentInfo.size.height / 2f + draggingItemOffset.y
                            val targetItem = gridState.layoutInfo.visibleItemsInfo.find { item -> centerX > item.offset.x && centerX < item.offset.x + item.size.width && centerY > item.offset.y && centerY < item.offset.y + item.size.height }
                            if (targetItem != null && targetItem.index != currentIndex && targetItem.index < categories.size) {
                                onReorder(currentIndex, targetItem.index); draggingItemIndex = targetItem.index; draggingItemOffset = Offset.Zero
                            }
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
            val zIndex = if (isDragging) 10f else 0f
            val shadowElevation = if (isDragging) 12.dp else 2.dp
            val translationX = if (isDragging) draggingItemOffset.x else 0f
            val translationY = if (isDragging) draggingItemOffset.y else 0f
            val count = products.count { it.category == category }
            val tileColor = categoryColors[category]?.let { Color(it) } ?: Color.White
            val icon = customIcons[category] ?: getThematicIcon(category)
            Box(
                modifier = Modifier
                    .zIndex(zIndex)
                    .graphicsLayer { this.scaleX = scale; this.scaleY = scale; this.translationX = translationX; this.translationY = translationY }
                    .shadow(shadowElevation, RoundedCornerShape(24.dp))
            ) {
                CategoryTile(name = category, count = count, color = tileColor, icon = icon, onClick = { if (!isDragging) onCategoryClick(category) })
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
    addButtonColor: Color,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var draggingItemOffset by remember { mutableStateOf(0f) }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        val hitItem = listState.layoutInfo.visibleItemsInfo.firstOrNull { item -> offset.y >= item.offset && offset.y <= item.offset + item.size }
                        hitItem?.let { if (it.index < categories.size) { draggingItemIndex = it.index; draggingItemOffset = 0f } }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume(); draggingItemOffset += dragAmount.y
                        val currentIndex = draggingItemIndex ?: return@detectDragGesturesAfterLongPress
                        val currentInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == currentIndex }
                        if (currentInfo != null) {
                            val centerY = currentInfo.offset + currentInfo.size / 2f + draggingItemOffset
                            val targetItem = listState.layoutInfo.visibleItemsInfo.find { item -> centerY > item.offset && centerY < item.offset + item.size }
                            if (targetItem != null && targetItem.index != currentIndex && targetItem.index < categories.size) {
                                onReorder(currentIndex, targetItem.index); draggingItemIndex = targetItem.index; draggingItemOffset = 0f
                            }
                        }
                    },
                    onDragEnd = { draggingItemIndex = null; draggingItemOffset = 0f },
                    onDragCancel = { draggingItemIndex = null; draggingItemOffset = 0f }
                )
            }
    ) {
        items(categories, key = { it }) { category ->
            val index = categories.indexOf(category)
            val isDragging = index == draggingItemIndex
            val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
            val zIndex = if (isDragging) 10f else 0f
            val shadowElevation = if (isDragging) 8.dp else 0.dp
            val translationY = if (isDragging) draggingItemOffset else 0f
            val count = products.count { it.category == category }
            val tileColor = categoryColors[category]?.let { Color(it) } ?: Color.White
            val icon = customIcons[category] ?: getThematicIcon(category)
            Box(
                modifier = Modifier
                    .zIndex(zIndex)
                    .graphicsLayer { this.scaleX = scale; this.scaleY = scale; this.translationY = translationY }
                    .shadow(shadowElevation, RoundedCornerShape(20.dp))
            ) {
                CategoryListRow(name = category, count = count, color = tileColor, icon = icon, onClick = { if (!isDragging) onCategoryClick(category) })
            }
        }
        item {
            Button(onClick = onAddCategoryClick, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = addButtonColor)) { Text("Dodaj kategorię", color = Color.White) }
        }
    }
}

@Composable
fun CategoryTile(name: String, count: Int, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.height(130.dp), elevation = CardDefaults.cardElevation(0.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(32.dp)) }
            Spacer(modifier = Modifier.height(12.dp)); Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White); Text(text = if(count == 0) "Pusto" else "$count szt.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun CategoryListRow(name: String, count: Int, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = color)) {
        Row(modifier = Modifier.padding(horizontal = 20.dp).fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.size(24.dp)) }
            Spacer(modifier = Modifier.width(20.dp)); Column(modifier = Modifier.weight(1f)) { Text(text = name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White); if (count > 0) Text(text = "$count szt.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f)) }
        }
    }
}

@Composable
fun AddCategoryTile(onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.6f)), modifier = Modifier.height(130.dp), shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.Add, null, tint = Color.Black, modifier = Modifier.size(40.dp)); Text("Dodaj", fontWeight = FontWeight.Bold, color = Color.Black) }
    }
}

@Composable
fun ProductListView(products: List<Product>, onProductClick: (Product) -> Unit, onProductLongClick: (Product) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp)) { items(products, key = { it.id }) { product -> ProductRowItem(product, onClick = { onProductClick(product) }, onLongClick = { onProductLongClick(product) }) } }
}

@Composable
fun ProductRowItem(product: Product, onClick: () -> Unit, onLongClick: () -> Unit) {
    val daysRemaining = calculateDaysRemaining(product.expirationDate)
    val statusColor = when { daysRemaining == null -> Color.Gray; daysRemaining < 0 -> StatusExpired; daysRemaining < 2 -> StatusWarning; else -> StatusFresh }
    val statusText = when { daysRemaining == null -> "Bezterminowo"; daysRemaining < 0 -> "Po terminie!"; daysRemaining == 0L -> "Dzisiaj"; daysRemaining == 1L -> "Jutro"; daysRemaining <= 7 -> "$daysRemaining dni"; else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(product.expirationDate!!)) }

    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp).pointerInput(product) { detectTapGestures(onLongPress = { onLongClick() }, onTap = { onClick() }) }, elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(6.dp).height(40.dp).clip(RoundedCornerShape(4.dp)).background(statusColor)); Spacer(modifier = Modifier.width(14.dp));
            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Color.Black);
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Timer, null, Modifier.size(14.dp), tint = Color.Gray); Spacer(modifier = Modifier.width(4.dp)); Text(text = statusText, style = MaterialTheme.typography.bodySmall, color = if (daysRemaining != null && daysRemaining <= 3) statusColor else Color.Gray, fontWeight = if (daysRemaining != null && daysRemaining <= 3) FontWeight.Bold else FontWeight.Normal) }
            };
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(8.dp)) { Text(text = "${product.count}x", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}

@Composable
fun ColorPickerDialog(title: String, onDismiss: () -> Unit, onColorSelected: (Color) -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = { LazyVerticalGrid(columns = GridCells.Fixed(4), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { items(CategoryPalette) { color -> Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(color).clickable { onColorSelected(color) }.border(1.dp, Color.Gray, CircleShape)) } } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } })
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTopBarBrown,
                        unfocusedBorderColor = AppTopBarBrown,
                        focusedLabelColor = AppTopBarBrown,
                        cursorColor = AppTopBarBrown
                    )
                )
                Text("Kolor:", style = MaterialTheme.typography.labelLarge)
                LazyVerticalGrid(columns = GridCells.Adaptive(40.dp), modifier = Modifier.height(80.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(CategoryPalette) { color -> Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(color).clickable { selectedColor = color }.border(if(selectedColor == color) 2.dp else 0.dp, Color.Black, CircleShape)) } }
                Text("Ikona:", style = MaterialTheme.typography.labelLarge)
                LazyVerticalGrid(columns = GridCells.Adaptive(40.dp), modifier = Modifier.height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(AVAILABLE_ICONS) { icon -> Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(if(selectedIcon == icon) Color.LightGray else Color.Transparent).clickable { selectedIcon = icon }, contentAlignment = Alignment.Center) { Icon(icon, null) } } }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedColor, selectedIcon) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = AppTopBarBrown, contentColor = Color.White)
            ) { Text("Zapisz") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
            ) { Text("Anuluj") }
        }
    )
}

@Composable
fun CategoryOptionsDialog(categoryName: String, onDismiss: () -> Unit, onDelete: () -> Unit, onChangeColor: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(categoryName, fontWeight = FontWeight.Bold) }, text = { Text("Zarządzaj kategorią") }, confirmButton = { Button(onClick = onChangeColor, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) { Icon(Icons.Outlined.Palette, null); Spacer(Modifier.width(8.dp)); Text("Zmień Kolor") } }, dismissButton = { Button(onClick = onDelete, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Outlined.Delete, null); Spacer(Modifier.width(8.dp)); Text("Usuń") } })
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

fun calculateDaysRemaining(expirationDate: Long?): Long? {
    if (expirationDate == null) return null
    val diff = expirationDate - System.currentTimeMillis()
    return TimeUnit.MILLISECONDS.toDays(diff)
}