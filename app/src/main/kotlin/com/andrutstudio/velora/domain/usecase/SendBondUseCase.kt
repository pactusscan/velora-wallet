package com.andrutstudio.velora.domain.usecase

import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.DappBond
import com.andrutstudio.velora.domain.model.Delegation
import com.andrutstudio.velora.domain.model.DelegationRules
import com.andrutstudio.velora.domain.repository.BlockchainRepository
import com.andrutstudio.velora.domain.repository.WalletRepository
import javax.inject.Inject

/** A PIP-49 delegated bond asked for by a dApp (window.pactus.bond). */
class SendBondUseCase @Inject constructor(
    private val blockchainRepository: BlockchainRepository,
    private val walletRepository: WalletRepository,
) {
    private val stake = Amount.fromNanoPac(DelegationRules.MAX_STAKE_NANO_PAC)

    data class Checked(val publicKey: String, val height: Long)

    /**
     * The node-dependent checks, before the approval sheet opens: the expiry is in
     * the future and the validator was not unbonded. The node refuses a key for a
     * validator it knows and needs one for a new validator.
     */
    suspend fun check(bond: DappBond): Checked {
        val height = blockchainRepository.getBlockHeight()
        require(bond.expiry > height) { "expiry in the past" }
        val validator = blockchainRepository.getValidator(bond.validator)
        require(validator?.isUnbonded != true) { "validator unbonded" }
        val key = if (validator != null) "" else bond.publicKey
        require(validator != null || key.isNotEmpty()) { "public key needed" }
        return Checked(key, height)
    }

    suspend fun estimateFee(): Amount = blockchainRepository.calculateFee(stake)

    suspend operator fun invoke(bond: DappBond, publicKey: String, owner: String, fee: Amount): String {
        val raw = blockchainRepository.buildBondTransaction(
            from = owner,
            to = bond.validator,
            stake = stake,
            publicKey = publicKey.ifEmpty { null },
            fee = fee,
            memo = bond.memo.ifEmpty { null },
            delegation = Delegation(owner = owner, shareNanoPac = bond.shareNanoPac, expiry = bond.expiry),
        )
        val signedHex = walletRepository.signRawTransaction(raw.hex, raw.signingMessageHex, owner)
        return blockchainRepository.broadcastTransaction(signedHex)
    }
}
