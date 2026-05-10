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
                context.getString(R.string.notification_channel_transactions),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_transactions_desc)
            }

            val alertChannel = NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.notification_channel_alerts_desc)
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
            .setContentTitle(context.getString(R.string.notification_incoming_transfer_title))
            .setContentText(context.getString(R.string.notification_incoming_transfer_text, amount, address))
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

        val title = if (isLowerThan) context.getString(R.string.notification_low_balance_title) 
                    else context.getString(R.string.notification_high_balance_title)
        val condition = if (isLowerThan) context.getString(R.string.notification_condition_below) 
                        else context.getString(R.string.notification_condition_above)

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_velora_logo)
            .setContentTitle(title)
            .setContentText(context.getString(R.string.notification_balance_alert_text, walletName, balance, condition, threshold))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ALERT, builder.build())
    }
}
