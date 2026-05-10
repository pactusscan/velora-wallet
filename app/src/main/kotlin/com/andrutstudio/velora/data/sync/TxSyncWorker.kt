package com.andrutstudio.velora.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
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
        private const val MAX_BLOCKS_PER_SYNC = 500L
    }

    override suspend fun doWork(): Result {
        android.util.Log.d("TxSyncWorker", "Sync worker started")
        return runCatching {
            // Ensure wallet is loaded if it's not already (e.g. after app kill)
            if (walletRepository.hasWallet() && walletRepository.observeWallet().first() == null) {
                android.util.Log.d("TxSyncWorker", "Wallet not loaded, calling loadLockedWallet")
                walletRepository.loadLockedWallet()
            }

            val wallet = walletRepository.observeWallet().filterNotNull().first()
            val addresses = wallet.accounts.map { it.address }
            if (addresses.isEmpty()) {
                android.util.Log.d("TxSyncWorker", "No addresses found, stopping")
                return Result.success()
            }

            // 1. Check for incoming transactions
            val currentHeight = rpcCaller.getBlockHeight()
            android.util.Log.d("TxSyncWorker", "Current height: $currentHeight")
            
            val lastSynced = transactionDao.getLastSyncedHeight(addresses)
                ?: (currentHeight - 20) // For new wallets, only check last 20 blocks for notifications

            val startHeight = maxOf(lastSynced + 1, currentHeight - MAX_BLOCKS_PER_SYNC)
            android.util.Log.d("TxSyncWorker", "Syncing from $startHeight to $currentHeight")
            
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
                        
                        // Notify for incoming transfers from external addresses
                        if (inserted != -1L && tx.receiver != null && addressSet.contains(tx.receiver)
                            && (tx.sender == null || !addressSet.contains(tx.sender))) {
                            
                            android.util.Log.d("TxSyncWorker", "New incoming TX found: ${tx.id}")
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

            android.util.Log.d("TxSyncWorker", "Sync worker completed successfully")
            Result.success()
        }.getOrElse { e -> 
            android.util.Log.e("TxSyncWorker", "Sync worker failed", e)
            Result.retry() 
        }
    }

    private suspend fun checkBalanceAlerts(accounts: List<com.andrutstudio.velora.domain.model.Account>) {
        val alerts = balanceAlertDao.getAllAlerts().first()
        if (alerts.isEmpty()) return

        for (alert in alerts) {
            if (!alert.isEnabled) continue
            
            val account = accounts.find { it.address == alert.address } ?: continue
            
            val balanceInfo = runCatching { rpcCaller.getAccount(account.address) }.getOrNull() ?: continue
            val currentBalance = balanceInfo.balance.nanoPac

            val isConditionMet = when (alert.type) {
                com.andrutstudio.velora.data.local.db.AlertType.LOWER_THAN -> currentBalance < alert.thresholdNanoPac
                com.andrutstudio.velora.data.local.db.AlertType.HIGHER_THAN -> currentBalance > alert.thresholdNanoPac
            }

            if (isConditionMet) {
                // If we haven't triggered for this condition yet, show notification
                if (alert.lastTriggeredValue == null) {
                    android.util.Log.d("TxSyncWorker", "Balance alert triggered for ${account.address}")
                    val balance = Amount.fromNanoPac(currentBalance)
                    val threshold = Amount.fromNanoPac(alert.thresholdNanoPac)
                    
                    notificationHelper.showLowBalanceAlert(
                        walletName = account.label.ifBlank { account.address.take(8) + "..." },
                        balance = formatPac(balance),
                        threshold = formatPac(threshold),
                        isLowerThan = alert.type == com.andrutstudio.velora.data.local.db.AlertType.LOWER_THAN
                    )

                    // Mark as triggered so we don't spam
                    balanceAlertDao.insertAlert(alert.copy(lastTriggeredValue = currentBalance))
                }
            } else {
                // Condition no longer met, reset triggered state so it can trigger again next time it crosses
                if (alert.lastTriggeredValue != null) {
                    android.util.Log.d("TxSyncWorker", "Balance alert reset for ${account.address}")
                    balanceAlertDao.insertAlert(alert.copy(lastTriggeredValue = null))
                }
            }
        }
    }

    private fun String.toTransactionType(): String = when {
        contains("UNBOND", ignoreCase = true) -> "UNBOND"
        contains("BOND", ignoreCase = true) -> "BOND"
        contains("SORTITION", ignoreCase = true) -> "SORTITION"
        else -> "TRANSFER"
    }
}
