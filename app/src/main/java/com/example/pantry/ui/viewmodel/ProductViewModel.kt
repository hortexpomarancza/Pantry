package com.example.pantry.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pantry.data.ProductRepository
import com.example.pantry.data.model.Product
import kotlinx.coroutines.launch

class ProductViewModel(private val repository: ProductRepository) : ViewModel() {

    val allProducts: LiveData<List<Product>> = repository.allProducts

    suspend fun getProductNameByBarcode(barcode: String): String? {
        return repository.getProductNameByBarcode(barcode)
    }

    suspend fun getProductById(id: Int): Product? {
        return repository.getProductById(id)
    }

    fun addProduct(name: String, expirationDate: Long, barcode: String?, category: String, count: Int) {
        val newProduct = Product(
            name = name,
            expirationDate = expirationDate,
            barcode = barcode,
            category = category,
            count = count
        )
        viewModelScope.launch {
            repository.insert(newProduct)
        }
    }

    fun updateProduct(product: Product) {
        viewModelScope.launch {
            repository.update(product)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.delete(product)
        }
    }

    // NOWE: Usuwa produkty, żeby kategoria zniknęła z listy "displayCategories"
    fun deleteProductsByCategory(category: String) {
        viewModelScope.launch {
            repository.deleteProductsByCategory(category)
        }
    }
}