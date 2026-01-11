package com.example.pantry.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pantry.data.dao.ProductDao
import com.example.pantry.data.model.Product

@Database(entities = [Product::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "larder_database"
                )
                    .fallbackToDestructiveMigration() // Opcjonalnie: czyści bazę przy zmianie wersji
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}