package com.example.pantry.ui.theme

import androidx.compose.ui.graphics.Color

// --- GŁÓWNY KOLOR APLIKACJI (Brązowy pasek jak na zdjęciu) ---
val AppTopBarBrown = Color(0xFF795548)

// --- KOLORY KAFELKÓW (Zgodne ze zdjęciem "Twoja Spiżarnia") ---
val TileColorMeat = Color(0xFFE57373)    // Mięso (Czerwony/Różowy)
val TileColorVeg = Color(0xFFC8E6C9)     // Warzywa (Zielony)
val TileColorDairy = Color(0xFFFFE0B2)   // Nabiał (Żółto-pomarańczowy)
val TileColorFrozen = Color(0xFFBBDEFB)  // Mrożonki (Niebieski)
val TileColorBread = Color(0xFFD7CCC8)   // Pieczywo (Beżowo-brązowy)
val TileColorDrinks = Color(0xFFB2DFDB)  // Napoje (Morski/Teal)
val TileColorOther = Color(0xFFE1BEE7)   // Inne (Fioletowy)
val TileColorAdd = Color(0xFFE6E6FA)     // Dodaj (Jasny fiolet)

// --- Paleta do wyboru (dla nowych kategorii) ---
val CategoryPalette = listOf(
    TileColorVeg, TileColorDairy, TileColorMeat, TileColorFrozen,
    TileColorBread, TileColorDrinks, TileColorOther,
    Color(0xFFFFF9C4), Color(0xFFF8BBD0), Color(0xFFF0F4C3)
)

// --- Kolory systemowe / Stanów ---
val GreenPrimary = Color(0xFF4CAF50)
val GreenSecondary = Color(0xFF81C784)
val GreenTertiary = Color(0xFFC8E6C9)

val WhiteSurface = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFF5F5F5)

val StatusFresh = Color(0xFF4CAF50)
val StatusWarning = Color(0xFFFF9800)
val StatusExpired = Color(0xFFF44336)
val StatusText = Color(0xFF757575)