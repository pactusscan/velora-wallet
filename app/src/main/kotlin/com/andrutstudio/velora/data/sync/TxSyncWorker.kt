package com.andrutstudio.velora.data.sync

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.core.app.NotificationCompat
import com.andrutstudio.velora.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import com.andrutstudio.velora.data.local.db.TransactionDao
import com.andrutstudio.velora.data.local.db.TransactionEntity
import com.andrutstudio.velora.data.rpc.RpcCaller
import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.TransactionStatus
import com.andrutstudio.velora.domain.repository.WalletRepository
import com.andrutstudio.velora.presentation.notification.NotificationHelper
import com.andrutstudio.velora.presentation.components.formatPac

@HiltWorker
class TxSyncWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val rpcCaller: RpcCaller,
    private val walletRepository: WalletRepository,
    private val transactionDao: TransactionDao,
    private val balanceAlertDao: com.andrutstudio.velora.data.local.db.BalanceAlertDao,
    private val notificationHelper: NotificationHelper,
    private val widgetUpdater: com.andrutstudio.velora.presentation.widget.WidgetUpdater,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "tx_sync_periodic"
        const val NOTIFICATION_CHANNEL_ID = "transactions"
        private const val MAX_BLOCKS_PER_SYNC = 50L
    }

    override suspend fun doWork(): Result {
        return runCatching {
            // Check for notifications permission if needed, but the helper handles the actual notification.
            // Foreground support to stay alive longer
            setForeground(createForegroundInfo())
            
            val wallet = walletRepository.observeWallet().filterNotNull().first()
            val addresses = wallet.accounts.map { it.address }
            if (addresses.isEmpty()) return Result.success()

            // 1. Check for incoming transactions
            val currentHeight = rpcCaller.getBlockHeight()
            val lastSynced = transactionDao.getLastSyncedHeight(addresses)
                ?: (currentHeight - MAX_BLOCKS_PER_SYNC)

            val startHeight = maxOf(lastSynced + 1, currentHeight - MAX_BLOCKS_PER_SYNC)
            
            val addressSet = addresses.toSet()
            
            if (startHeight <= currentHeight) {
                for (height in startHeight..currentHeight) {
                    val block = runCatching { rpcCaller.getBlock(height) }.getOrNull() ?: continue
                    for (tx in block.txs) {
                        val involvedAddress = when {
                            (tx.receiver != null && addressSet.contains(tx.receiver)) -> tx.receiver
                            (tx.sender != null && addressSet.contains(tx.sender)) -> tx.sender
                            else -> null
                        } ?: continue

                        val entity = TransactionEntity(
                            id = tx.id,
                            type = tx.typeStr.toTransactionType(),
                            fromAddress = tx.sender ?: "",
                            toAddress = tx.receiver,
                            amountNanoPac = tx.amountNanoPac,
                            feeNanoPac = tx.feeNanoPac,
                            memo = tx.memo,
                            blockHeight = tx.blockHeight,
                            blockTime = tx.blockTime,
                            status = TransactionStatus.CONFIRMED.name,
                            involvedAddress = involvedAddress,
                        )
                        val inserted = transactionDao.insertIfNew(entity)
                        if (inserted != -1L && tx.receiver != null && addressSet.contains(tx.receiver)
                            && (tx.sender == null || !addressSet.contains(tx.sender))) {
                            
                            val amount = Amount.fromNanoPac(tx.amountNanoPac)
                            notificationHelper.showIncomingTransferNotification(
                                address = tx.sender ?: "Unknown",
                                amount = formatPac(amount),
                            )
                        }
                    }
                }
            }

            // 2. Check for balance alerts
            checkBalanceAlerts(wallet.accounts)
            
            // 3. Update widgets
            val totalBalance = wallet.accounts.sumOf { acc -> 
                runCatching { rpcCaller.getAccount(acc.address).balance.nanoPac }.getOrDefault(0L)
            }
            widgetUpdater.update(
                totalBalanceNanoPac = totalBalance,
                walletName = wallet.name,
                networkName = wallet.network.displayName,
            )

            Result.success()
        }.getOrElse { Result.retry() }
    }

    private suspend fun checkBalanceAlerts(accounts: List<com.andrutstudio.velora.domain.model.Account>) {
        val alerts = balanceAlertDao.getAllAlerts().first()
        if (alerts.isEmpty()) return

        for (alert in alerts) {
            if (!alert.isEnabled) continue
            
            val account = accounts.find { it.address == alert.address } ?: continue
            
            // We need to fetch the LATEST balance from RPC because account balance in model might be stale
            val balanceInfo = runCatching { rpcCaller.getAccount(account.address) }.getOrNull() ?: continue
            val currentBalance = balanceInfo.balance.nanoPac

            val shouldAlert = when (alert.type) {
                com.andrutstudio.velora.data.local.db.AlertType.LOWER_THAN -> currentBalance < alert.thresholdNanoPac
                com.andrutstudio.velora.data.local.db.AlertType.HIGHER_THAN -> currentBalance > alert.thresholdNanoPac
            }

            if (shouldAlert) {
                // Prevent duplicate alerts for the same value crossing
                if (alert.lastTriggeredValue == currentBalance) continue

                val balance = Amount.fromNanoPac(currentBalance)
                val threshold = Amount.fromNanoPac(alert.thresholdNanoPac)
                
                notificationHelper.showLowBalanceAlert(
                    walletName = account.label.ifBlank { account.address.take(8) + "..." },
                    balance = formatPac(balance),
                    threshold = formatPac(threshold),
                    isLowerThan = alert.type == com.andrutstudio.velora.data.local.db.AlertType.LOWER_THAN
                )

                // Save last triggered to avoid spamming
                balanceAlertDao.insertAlert(alert.copy(lastTriggeredValue = currentBalance))
            } else if (alert.lastTriggeredValue != null) {
                // Reset triggered value if condition is no longer met
                balanceAlertDao.insertAlert(alert.copy(lastTriggeredValue = null))
            }
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_velora_logo)
            .setContentTitle("Velora Sync")
            .setContentText("Checking for new transactions...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(999, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(999, notification)
        }
    }

    private fun String.toTransactionType(): String = when {
        contains("UNBOND", ignoreCase = true) -> "UNBOND"
        contains("BOND", ignoreCase = true) -> "BOND"
        contains("SORTITION", ignoreCase = true) -> "SORTITION"
        else -> "TRANSFER"
    }
}
