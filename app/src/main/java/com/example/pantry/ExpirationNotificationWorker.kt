package com.example.pantry.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pantry.data.local.AppDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ExpirationNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val products = database.productDao().getAllProductsSync()

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val expiringToday = mutableListOf<String>()
        val expiringSoon = mutableListOf<String>()

        for (product in products) {
            // ZMIANA: Jeśli produkt nie ma daty, pomijamy go
            if (product.expirationDate == null) continue

            val productDate = Calendar.getInstance().apply {
                timeInMillis = product.expirationDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMillis = productDate.timeInMillis - today.timeInMillis
            val daysDiff = TimeUnit.MILLISECONDS.toDays(diffMillis)

            when (daysDiff) {
                0L -> expiringToday.add(product.name)
                1L, 2L -> expiringSoon.add("${product.name} (za $daysDiff dni)")
                else -> {
                    if (daysDiff < 0) {
                        expiringToday.add("${product.name} (PO TERMINIE!)")
                    }
                }
            }
        }

        if (expiringToday.isNotEmpty()) {
            sendNotification(
                1,
                "Zjedz to dzisiaj!",
                expiringToday.joinToString(", ")
            )
        }

        if (expiringSoon.isNotEmpty()) {
            sendNotification(
                2,
                "Wkrótce minie termin",
                "Sprawdź: ${expiringSoon.joinToString(", ")}"
            )
        }

        return Result.success()
    }

    private fun sendNotification(id: Int, title: String, content: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val channelId = "pantry_expiration_channel"
        val safeIcon = android.R.drawable.ic_dialog_info

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(safeIcon)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        notificationManager.notify(id, builder.build())
    }
}