package com.andrutstudio.velora

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import dagger.hilt.android.HiltAndroidApp
import com.andrutstudio.velora.data.sync.TxSyncWorker
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class VeloraApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("TrustWalletCore")
        createNotificationChannel()
        schedulePeriodicSync()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TxSyncWorker.NOTIFICATION_CHANNEL_ID,
                "Transactions",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { 
                description = "Incoming transaction alerts"
                enableVibration(true)
                enableLights(true)
            }

            val alertChannel = NotificationChannel(
                "alerts",
                "Wallet Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts for low/high balance"
                enableVibration(true)
                enableLights(true)
            }
            
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    private fun schedulePeriodicSync() {
        val request = PeriodicWorkRequestBuilder<TxSyncWorker>(5, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            TxSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
