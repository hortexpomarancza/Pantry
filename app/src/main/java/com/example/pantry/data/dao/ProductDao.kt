package com.example.pantry.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update // <--- Ważne: import
import com.example.pantry.data.model.Product

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY expirationDate ASC")
    fun getAllProducts(): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: Int): Product?

    @Query("SELECT name FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductNameByBarcode(barcode: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Update
    suspend fun updateProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("SELECT * FROM products")
    suspend fun getAllProductsOnce(): List<Product>

}
