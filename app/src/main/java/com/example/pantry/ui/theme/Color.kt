package com.example.pantry.ui.theme

import androidx.compose.ui.graphics.Color

// --- GŁÓWNY KOLOR APLIKACJI ---
// Ciemniejszy, głęboki brąz (Espresso / Dark Chocolate)
val AppTopBarBrown = Color(0xFF3E2723)

// --- ŻYWE KOLORY KAFELKÓW ---
val VividRed = Color(0xFFD32F2F)
val VividGreen = Color(0xFF388E3C)
val VividOrange = Color(0xFFF57C00)
val VividBlue = Color(0xFF1976D2)
val VividPurple = Color(0xFF7B1FA2)
val VividTeal = Color(0xFF00796B)
val VividPink = Color(0xFFC2185B)
val VividYellow = Color(0xFFFBC02D)
val VividCyan = Color(0xFF0097A7)
val VividBrown = Color(0xFF3E2723)
val VividGrey = Color(0xFF616161)
val VividIndigo = Color(0xFF303F9F)

// --- Paleta do wyboru (dla nowych kategorii) ---
val CategoryPalette = listOf(
    VividGreen,     // Warzywa
    VividOrange,    // Nabiał
    VividRed,       // Mięso
    VividBlue,      // Mrożonki
    Color(0xFF8D6E63), // Pieczywo (Jasny brąz)
    VividBrown,     // <--- DODANE: Twój nowy ciemny brąz
    VividTeal,      // Napoje
    VividPurple,    // Inne
    VividPink,      // Słodycze/Inne
    VividYellow,    // Owoce
    VividIndigo,    // Chemia
    VividCyan,      // Ryby
    VividGrey       // Inne
)

// Kolory systemowe (dostosowane do trybu ciemnego)
val GreenPrimary = Color(0xFF2E7D32)
val GreenSecondary = Color(0xFF4CAF50)
val WhiteSurface = Color(0xFFFFFFFF)
val DarkSurface = Color(0xFF1E1E1E)

val LightBackground = Color(0xFFF5F5F5)

val StatusFresh = Color(0xFF388E3C)
val StatusWarning = Color(0xFFFFA000)
val StatusExpired = Color(0xFFD32F2F)