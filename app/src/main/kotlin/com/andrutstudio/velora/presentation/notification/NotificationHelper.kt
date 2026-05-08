package com.andrutstudio.velora.presentation.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.andrutstudio.velora.MainActivity
import com.andrutstudio.velora.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_TRANSACTIONS = "transactions"
        const val CHANNEL_ALERTS = "alerts"
        const val NOTIFICATION_ID_TRANSACTION = 1001
        const val NOTIFICATION_ID_ALERT = 1002
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val txChannel = NotificationChannel(
                CHANNEL_TRANSACTIONS,
                "Transactions",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for incoming transfers"
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                "Wallet Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for low balance or other wallet events"
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(txChannel)
            manager.createNotificationChannel(alertChannel)
        }
    }

    fun showIncomingTransferNotification(address: String, amount: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_TRANSACTIONS)
            .setSmallIcon(R.drawable.ic_velora_logo)
            .setContentTitle("Incoming Transfer")
            .setContentText("Received $amount from $address")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_TRANSACTION, builder.build())
    }

    fun showLowBalanceAlert(walletName: String, balance: String, threshold: String, isLowerThan: Boolean = true) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isLowerThan) "Low Balance Alert" else "High Balance Alert"
        val condition = if (isLowerThan) "below" else "above"

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_velora_logo)
            .setContentTitle(title)
            .setContentText("Wallet '$walletName' balance is $balance, which is $condition your threshold of $threshold")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ALERT, builder.build())
    }
}
