package com.example.pantry.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    // ZMIANA: Znak zapytania oznacza, że data może być pusta (null)
    val expirationDate: Long?,
    val barcode: String? = null,
    val category: String,
    val count: Int = 1,
    val storageLocation: String = "Twoja Spiżarnia"
)

@Entity(tableName = "spaces")
data class Space(
    @PrimaryKey val name: String,
    val color: Int
)