package com.example.pantry.data

import androidx.lifecycle.LiveData
import com.example.pantry.data.dao.ProductDao
import com.example.pantry.data.model.Product
import com.example.pantry.data.model.Space

class ProductRepository(private val productDao: ProductDao) {

    val allSpaces: LiveData<List<Space>> = productDao.getAllSpaces()

    fun getProductsByLocation(location: String): LiveData<List<Product>> {
        return productDao.getProductsByLocation(location)
    }

    suspend fun getProductNameByBarcode(barcode: String): String? {
        return productDao.getProductNameByBarcode(barcode)
    }

    suspend fun getProductById(id: Int): Product? {
        return productDao.getProductById(id)
    }

    suspend fun insert(product: Product) {
        productDao.insertProduct(product)
    }

    suspend fun update(product: Product) {
        productDao.updateProduct(product)
    }

    suspend fun delete(product: Product) {
        productDao.deleteProduct(product)
    }

    suspend fun deleteProductsByCategoryAndLocation(category: String, location: String) {
        productDao.deleteProductsByCategoryAndLocation(category, location)
    }

    // --- PRZESTRZENIE ---
    suspend fun insertSpace(name: String, color: Int) {
        productDao.insertSpace(Space(name, color))
    }

    suspend fun deleteSpace(space: Space) {
        productDao.deleteProductsByLocation(space.name)
        productDao.deleteSpace(space)
    }

    // NOWE
    suspend fun updateSpaceColor(name: String, color: Int) {
        productDao.updateSpaceColor(name, color)
    }
}