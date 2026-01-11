package com.example.pantry.data

import com.example.pantry.data.dao.ProductDao
import com.example.pantry.data.model.Product

class ProductRepository(private val productDao: ProductDao) {
    val allProducts = productDao.getAllProducts()

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

    // NOWE
    suspend fun deleteProductsByCategory(category: String) {
        productDao.deleteProductsByCategory(category)
    }
}