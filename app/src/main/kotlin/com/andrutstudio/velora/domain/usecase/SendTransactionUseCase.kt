package com.andrutstudio.velora.domain.usecase

import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import com.andrutstudio.velora.domain.repository.WalletRepository
import javax.inject.Inject

class SendTransactionUseCase @Inject constructor(
    private val blockchainRepository: BlockchainRepository,
    private val walletRepository: WalletRepository,
) {
    suspend fun estimateFee(amount: Amount): Amount =
        blockchainRepository.calculateFee(amount)

    suspend operator fun invoke(
        from: String,
        to: String,
        amount: Amount,
        fee: Amount,
        memo: String?,
    ): String {
        val raw = blockchainRepository.buildTransferTransaction(
            from = from,
            to = to,
            amount = amount,
            fee = fee,
            memo = memo?.takeIf { it.isNotBlank() },
        )
        val signedHex = walletRepository.signRawTransaction(
            rawHex = raw.hex,
            signingMessageHex = raw.signingMessageHex,
            fromAddress = from,
        )
        return blockchainRepository.broadcastTransaction(signedHex)
    }
}
