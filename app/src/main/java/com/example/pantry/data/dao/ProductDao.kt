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
    @Query("SELECT * FROM products WHERE storageLocation = :location ORDER BY expirationDate ASC")
    fun getProductsByLocation(location: String): LiveData<List<Product>>

    // NOWE: Metoda dla Workera powiadomień (pobiera wszystko synchronicznie)
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

    @Query("DELETE FROM products WHERE category = :category AND storageLocation = :location")
    suspend fun deleteProductsByCategoryAndLocation(category: String, location: String)

    @Query("DELETE FROM products WHERE storageLocation = :location")
    suspend fun deleteProductsByLocation(location: String)

    // --- PRZESTRZENIE ---
    @Query("SELECT * FROM spaces")
    fun getAllSpaces(): LiveData<List<Space>>

    @Query("SELECT COUNT(*) FROM spaces")
    suspend fun getSpacesCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSpace(space: Space)

    @Delete
    suspend fun deleteSpace(space: Space)

    @Query("UPDATE spaces SET color = :color WHERE name = :name")
    suspend fun updateSpaceColor(name: String, color: Int)
}