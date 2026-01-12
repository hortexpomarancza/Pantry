package com.example.pantry.data.local

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pantry.data.dao.ProductDao
import com.example.pantry.data.model.Product
import com.example.pantry.data.model.Space
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [Product::class, Space::class], version = 5, exportSchema = false)
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
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = database.productDao()
                                    // TYLKO JEDNA PRZESTRZEŃ NA START
                                    dao.insertSpace(Space("Twoja Spiżarnia", AndroidColor.parseColor("#5D4037")))
                                }
                            }
                        }
                        // Zabezpieczenie: jeśli baza jest pusta po migracji, dodaj spiżarnię
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = database.productDao()
                                    if (dao.getSpacesCount() == 0) {
                                        dao.insertSpace(Space("Twoja Spiżarnia", AndroidColor.parseColor("#5D4037")))
                                    }
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}