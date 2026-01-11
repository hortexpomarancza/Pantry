package com.example.pantry.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val expirationDate: Long,
    val barcode: String? = null,
    val category: String,
    val count: Int = 1 // <--- DODAJ TO POLE (domyślnie 1)
)