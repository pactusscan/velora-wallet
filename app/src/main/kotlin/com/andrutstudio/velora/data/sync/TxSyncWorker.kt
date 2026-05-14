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
    private val monitoredNodeDao: com.andrutstudio.velora.data.local.db.MonitoredNodeDao,
    private val pactusScan: com.andrutstudio.velora.data.rpc.PactusScanApiService,
    private val notificationHelper: NotificationHelper,
    private val widgetUpdater: com.andrutstudio.velora.presentation.widget.WidgetUpdater,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "tx_sync_periodic"
        const val NOTIFICATION_CHANNEL_ID = "transactions"
    }

    override suspend fun doWork(): Result {
        android.util.Log.d("TxSyncWorker", "Sync worker started")
        return runCatching {
            // Get ALL addresses from ALL wallets (TrustWallet style)
            val allWallets = walletRepository.observeWallets().first()
            val allAddresses = allWallets.flatMap { w -> w.accounts.map { it.address } }.toSet()
            
            if (allAddresses.isEmpty()) {
                android.util.Log.d("TxSyncWorker", "No addresses found, stopping")
                return Result.success()
            }

            android.util.Log.d("TxSyncWorker", "Monitoring ${allAddresses.size} addresses")

            // 1. Sync Transactions and trigger notifications
            for (address in allAddresses) {
                syncAddressTransactions(address, allAddresses)
            }

            // 2. Check for balance alerts (active wallet only)
            val activeWallet = walletRepository.observeWallet().filterNotNull().first()
            checkBalanceAlerts(activeWallet.accounts)
            
            // 3. Check for monitored nodes
            checkMonitoredNodes()
            
            // 4. Update widgets (active wallet only)
            val totalBalance = activeWallet.accounts.sumOf { acc -> 
                runCatching { rpcCaller.getAccount(acc.address).balance.nanoPac }.getOrDefault(0L)
            }
            widgetUpdater.update(
                totalBalanceNanoPac = totalBalance,
                walletName = activeWallet.name,
                networkName = activeWallet.network.displayName,
            )

            android.util.Log.d("TxSyncWorker", "Sync worker completed successfully")
            Result.success()
        }.getOrElse { e -> 
            android.util.Log.e("TxSyncWorker", "Sync worker failed", e)
            Result.retry() 
        }
    }

    private suspend fun syncAddressTransactions(address: String, myAddresses: Set<String>) {
        runCatching {
            // Fetch last 10 transactions from Explorer
            val response = pactusScan.getTransactions(address, page = 1, limit = 10)
            
            for (tx in response.txs) {
                // Skip if already in DB (meaning we've seen it and potentially notified)
                if (transactionDao.getById(tx.id) != null) continue

                val txType = tx.payloadType.toTransactionType()
                val entity = TransactionEntity(
                    id = tx.id,
                    type = txType,
                    fromAddress = tx.sender ?: "",
                    toAddress = tx.receiver,
                    amountNanoPac = tx.amountNanoPac,
                    feeNanoPac = tx.feeNanoPac,
                    memo = tx.memo,
                    blockHeight = tx.blockHeight,
                    blockTime = tx.blockTime,
                    status = TransactionStatus.CONFIRMED.name,
                    involvedAddress = address,
                    direction = tx.direction,
                    isNotified = true
                )

                // Insert into DB
                transactionDao.insertIfNew(entity)

                // Trigger Notification
                // direction: 0=self, 1=in, 2=out
                val isIncoming = tx.direction == 1
                val isOutgoing = tx.direction == 2
                
                // Skip if it's an internal transfer (between our own addresses)
                val isInternal = tx.sender != null && myAddresses.contains(tx.sender) && 
                                tx.receiver != null && myAddresses.contains(tx.receiver)
                
                // Skip if it's a block reward (from genesis address)
                val isBlockReward = tx.sender == "000000000000000000000000000000000000000000"

                if (isInternal || isBlockReward) {
                    android.util.Log.d("TxSyncWorker", "Skipping notification for TX ${tx.id} (Internal: $isInternal, BlockReward: $isBlockReward)")
                    continue
                }

                val amount = Amount.fromNanoPac(tx.amountNanoPac)
                val formattedAmount = formatPac(amount)

                when {
                    isIncoming -> {
                        android.util.Log.d("TxSyncWorker", "Notifying incoming TX: ${tx.id}")
                        notificationHelper.showIncomingTransferNotification(
                            txId = tx.id,
                            address = tx.sender ?: "Unknown",
                            amount = formattedAmount
                        )
                    }
                    isOutgoing -> {
                        android.util.Log.d("TxSyncWorker", "Notifying outgoing TX: ${tx.id}")
                        if (txType == "BOND") {
                            notificationHelper.showBondNotification(
                                txId = tx.id,
                                validatorAddress = tx.receiver ?: "Unknown",
                                amount = formattedAmount
                            )
                        } else {
                            notificationHelper.showOutgoingTransferNotification(
                                txId = tx.id,
                                address = tx.receiver ?: "Unknown",
                                amount = formattedAmount
                            )
                        }
                    }
                }
            }
        }.onFailure { e ->
            android.util.Log.e("TxSyncWorker", "Failed to sync address $address", e)
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

                    balanceAlertDao.insertAlert(alert.copy(lastTriggeredValue = currentBalance))
                }
            } else {
                if (alert.lastTriggeredValue != null) {
                    android.util.Log.d("TxSyncWorker", "Balance alert reset for ${account.address}")
                    balanceAlertDao.insertAlert(alert.copy(lastTriggeredValue = null))
                }
            }
        }
    }

    private suspend fun checkMonitoredNodes() {
        val nodes = monitoredNodeDao.getAll().first()
        for (node in nodes) {
            val isValidator = node.validatorAddress.startsWith("pc1p")
            val result = runCatching {
                if (isValidator) {
                    pactusScan.getValidatorPeer(node.validatorAddress)
                } else {
                    pactusScan.getPeerById(node.validatorAddress)
                }
            }

            val response = result.getOrNull()
            // A node is considered online only if the API call succeeds AND peerOnline is true
            val isOnline = result.isSuccess && response?.peerOnline == true

            if (!isOnline) {
                // If it was online before, and now it's offline (or API failed), notify
                if (node.lastKnownOnline) {
                    val nodeName = response?.peer?.moniker ?: (node.validatorAddress.take(8) + "...")
                    android.util.Log.d("TxSyncWorker", "Node offline detected: $nodeName")
                    notificationHelper.showNodeOfflineNotification(nodeName)
                    monitoredNodeDao.update(node.copy(lastKnownOnline = false))
                }
            } else {
                // If it was offline and is now back online, update status quietly
                if (!node.lastKnownOnline) {
                    android.util.Log.d("TxSyncWorker", "Node back online: ${node.validatorAddress}")
                    monitoredNodeDao.update(node.copy(lastKnownOnline = true))
                }
            }
        }
    }

    private fun Int.toTransactionType(): String = when (this) {
        1 -> "TRANSFER"
        2 -> "BOND"
        4 -> "UNBOND"
        3 -> "SORTITION"
        5 -> "WITHDRAW"
        6 -> "BATCH_TRANSFER"
        else -> "TRANSFER"
    }
}
