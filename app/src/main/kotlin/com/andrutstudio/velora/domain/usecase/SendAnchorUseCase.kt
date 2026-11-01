package com.andrutstudio.velora.domain.usecase

import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.AnchorRequest
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import com.andrutstudio.velora.domain.repository.WalletRepository
import javax.inject.Inject

class SendAnchorUseCase @Inject constructor(
    private val blockchainRepository: BlockchainRepository,
    private val walletRepository: WalletRepository,
) {
    suspend fun estimateFee(deposit: Amount): Amount =
        blockchainRepository.calculateAnchorFee(deposit)

    suspend operator fun invoke(request: AnchorRequest): String {
        val raw = blockchainRepository.buildAnchorTransaction(request)
        val signedHex = walletRepository.signRawTransaction(
            rawHex = raw.hex,
            signingMessageHex = raw.signingMessageHex,
            fromAddress = request.sender,
        )
        return blockchainRepository.broadcastTransaction(signedHex)
    }
}
