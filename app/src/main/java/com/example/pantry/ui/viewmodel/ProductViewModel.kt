package com.example.pantry.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.example.pantry.data.ProductRepository
import com.example.pantry.data.model.Product
import com.example.pantry.data.model.Space
import com.example.pantry.ui.screens.AVAILABLE_ICONS
import com.example.pantry.ui.screens.DEFAULT_START_CATEGORIES
import com.example.pantry.ui.theme.*
import kotlinx.coroutines.launch

class ProductViewModel(application: Application, private val repository: ProductRepository) : AndroidViewModel(application) {

    private val HARDCODED_LOCATION = "Twoja Spiżarnia"
    private val PREFS_NAME = "pantry_categories_prefs"
    private val ORDER_KEY = "categories_order_list"

    private val _currentLocation = MutableLiveData(HARDCODED_LOCATION)
    val currentLocation: LiveData<String> = _currentLocation

    val currentProducts: LiveData<List<Product>> = repository.getProductsByLocation(HARDCODED_LOCATION)
    val allSpaces: LiveData<List<Space>> = repository.allSpaces

    private val _categories = mutableStateListOf<String>()
    val categories: List<String> get() = _categories

    val categoryColors = mutableStateMapOf<String, Int>().apply {
        put("Warzywa i Owoce", VividGreen.toArgb())
        put("Nabiał", VividOrange.toArgb())
        put("Mięso", VividRed.toArgb())
        put("Mrożonki", VividBlue.toArgb())
        put("Napoje", VividTeal.toArgb())
        put("Pieczywo", 0xFF8D6E63.toInt())
        put("Inne", VividPurple.toArgb())
    }

    val customIcons = mutableStateMapOf<String, ImageVector>()

    private val productsObserver = Observer<List<Product>> { products ->
        syncCategoriesWithProducts(products)
    }

    init {
        loadData()
        currentProducts.observeForever(productsObserver)
    }

    override fun onCleared() {
        super.onCleared()
        currentProducts.removeObserver(productsObserver)
    }

    fun moveCategory(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex in _categories.indices && toIndex in _categories.indices) {
            val item = _categories.removeAt(fromIndex)
            _categories.add(toIndex, item)
            saveCategoriesOrder()
        }
    }

    private fun loadData() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedOrderString = prefs.getString(ORDER_KEY, null)
        if (savedOrderString != null) {
            val savedList = savedOrderString.split(",").filter { it.isNotBlank() }
            _categories.clear()
            _categories.addAll(savedList)
        } else {
            _categories.clear()
            _categories.addAll(DEFAULT_START_CATEGORIES)
        }

        val allPrefs = prefs.all
        allPrefs.keys.forEach { key ->
            if (key.startsWith("color_")) {
                val catName = key.removePrefix("color_")
                val color = prefs.getInt(key, VividGrey.toArgb())
                categoryColors[catName] = color
            }
            if (key.startsWith("icon_idx_")) {
                val catName = key.removePrefix("icon_idx_")
                val idx = prefs.getInt(key, -1)
                if (idx >= 0 && idx < AVAILABLE_ICONS.size) {
                    customIcons[catName] = AVAILABLE_ICONS[idx]
                }
            }
        }
    }

    private fun saveCategoriesOrder() {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val orderString = _categories.joinToString(",")
        editor.putString(ORDER_KEY, orderString)
        editor.apply()
    }

    private fun saveCategoryAttributes(name: String, color: Int, icon: ImageVector?) {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt("color_$name", color)
        if (icon != null) {
            val index = AVAILABLE_ICONS.indexOf(icon)
            editor.putInt("icon_idx_$name", index)
        }
        editor.apply()
    }

    private fun removeCategoryAttributes(name: String) {
        val prefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.remove("color_$name")
        editor.remove("icon_idx_$name")
        editor.apply()
    }

    private fun syncCategoriesWithProducts(products: List<Product>) {
        var changed = false
        val usedCategories = products.map { it.category }.distinct()
        usedCategories.forEach { cat ->
            if (!_categories.contains(cat)) {
                _categories.add(cat)
                changed = true
            }
        }
        if (changed) saveCategoriesOrder()
    }

    fun addSessionCategory(categoryName: String, color: Int, icon: ImageVector?) {
        if (!_categories.contains(categoryName)) {
            _categories.add(0, categoryName)
            saveCategoriesOrder()
        }
        categoryColors[categoryName] = color
        if (icon != null) customIcons[categoryName] = icon
        saveCategoryAttributes(categoryName, color, icon)
    }

    fun updateCategoryColor(categoryName: String, color: Int) {
        categoryColors[categoryName] = color
        val currentIcon = customIcons[categoryName]
        saveCategoryAttributes(categoryName, color, currentIcon)
    }

    fun removeSessionCategory(categoryName: String) {
        _categories.remove(categoryName)
        saveCategoriesOrder()
        removeCategoryAttributes(categoryName)
    }

    fun setLocation(location: String) { }
    fun updateCurrentSpaceColor(color: Int) { }
    fun addSpace(name: String, color: Int) { }
    fun deleteSpace(space: Space) { }

    suspend fun getProductNameByBarcode(barcode: String): String? {
        return repository.getProductNameByBarcode(barcode)
    }

    suspend fun getProductById(id: Int): Product? {
        return repository.getProductById(id)
    }

    // ZMIANA: expirationDate teraz nullable (Long?)
    fun addProduct(name: String, expirationDate: Long?, barcode: String?, category: String, count: Int) {
        val newProduct = Product(
            name = name,
            expirationDate = expirationDate,
            barcode = barcode,
            category = category,
            count = count,
            storageLocation = HARDCODED_LOCATION
        )
        viewModelScope.launch { repository.insert(newProduct) }
    }

    // ZMIANA: expirationDate teraz nullable (Long?) w modelu Product
    fun updateProduct(product: Product) {
        viewModelScope.launch { repository.update(product) }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch { repository.delete(product) }
    }

    fun deleteProductsByCategory(category: String) {
        viewModelScope.launch {
            repository.deleteProductsByCategoryAndLocation(category, HARDCODED_LOCATION)
            removeSessionCategory(category)
        }
    }
}