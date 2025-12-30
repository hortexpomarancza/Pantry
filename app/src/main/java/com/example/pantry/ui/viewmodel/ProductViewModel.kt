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

    fun addProduct(name: String, expirationDate: Long, barcode: String?) {
        val newProduct = Product(name = name, expirationDate = expirationDate, barcode = barcode)

        viewModelScope.launch {
            repository.insert(newProduct)
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            repository.delete(product)
        }
    }
}