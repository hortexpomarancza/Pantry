package com.example.pantry.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pantry.data.model.Product
import com.example.pantry.data.model.Space

@Dao
interface ProductDao {
    // --- PRODUKTY ---
    // ZMIANA: Sortowanie - najpierw te co mają datę, na końcu te bez daty (NULL)
    @Query("SELECT * FROM products WHERE storageLocation = :location ORDER BY CASE WHEN expirationDate IS NULL THEN 1 ELSE 0 END, expirationDate ASC")
    fun getProductsByLocation(location: String): LiveData<List<Product>>

    @Query("SELECT * FROM products")
    suspend fun getAllProductsSync(): List<Product>

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
