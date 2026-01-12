package com.example.pantry.worker


import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pantry.data.local.AppDatabase
import java.util.concurrent.TimeUnit
import com.example.pantry.notifications.ExpiryNotifier


class ExpirationCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {


        val thresholdMs = TimeUnit.DAYS.toMillis(3)
        val now = System.currentTimeMillis()
        val limit = now + thresholdMs

        val db = AppDatabase.getDatabase(applicationContext)
        val products = db.productDao().getAllProductsOnce()

        val expiring = products.filter { it.expirationDate <= limit }

        if (expiring.isNotEmpty()) {
            ExpiryNotifier.showSummaryNotification(
                applicationContext,
                expiring.size
            )
        }

        return Result.success()
    }
}
