package com.andrutstudio.velora.data.crypto

import com.andrutstudio.velora.domain.model.Delegation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PIP-49 bond and unbond, with the vectors of the browser extension (bond.test.ts):
 * raw transactions built by a mainnet node (Pactus 1.17) from public addresses
 * only; nothing was signed or broadcast.
 */
class BondTxTest {
    private val owner = "pc1ryhndnylqrtze0r8qne3tr37w3lled25f2j77cg"
    private val newValidator = "pc1p8fmz4l8prrth4zc35yqgxl8tm8e9uw0vg5a6ae"
    private val newValidatorKey =
        "public1psuz3y3vtakgh5zyanxfrdr56aaftr9dgyglff7q7mdsx96g0sngnfyke290z7arpy733l4nuq67h7rekzt5u5c9tyuy7wskyft6el2tsg02zxyemkdgr40zg5j6c4dstx6mrc96z2h843wcypqmgheu56v59pxy7"
    private val oldValidator = "pc1psptnvt29uml3zzrmlmq0k758cajxqs5ut3t0ja"
    private val oldOwner = "pc1rpfanncmnsm9t8x8gf85sexz9xlwh4pjdfsxp42"

    private val bondNew =
        "0601c885840080ade2040e5061637475735363616e20487562020325e6d993e01ac5978ce09e62b1c7ce8fff96aa89013a762afce118d77a8b11a100837cebd9f25e39ec60870512458bed917a089d9992368e9aef52b195a8223e94f81edb6062e90f84d13492d9515e2f746127a31fd67c06bd7f0f3612e9ca60ab2709e742c44af59fa97043d423133bb3503abc48a4b58ab60b36b63c174255cf58bb0408368be794d380a094a58d1d0325e6d993e01ac5978ce09e62b1c7ce8fff96aa898084af5fdda6b400"
    private val bondOld =
        "0601c885840080ade20400020325e6d993e01ac5978ce09e62b1c7ce8fff96aa89018057362d45e6ff11087bfec0fb7a87c76460429c0080a094a58d1d0325e6d993e01ac5978ce09e62b1c7ce8fff96aa8980cab5ee0140548900"
    private val bondPlain =
        "0201c885840080ade20400020325e6d993e01ac5978ce09e62b1c7ce8fff96aa89018057362d45e6ff11087bfec0fb7a87c76460429c0080e497d012"
    private val unbondDelegated =
        "0601c9858400000362796504018057362d45e6ff11087bfec0fb7a87c76460429c030a7b39e37386cab398e849e90c984537dd7a864d"

    private val fee = 10_000_000L
    private val stake = 1_000_000_000_000L
    private val hubMemo = "PactusScan Hub"
    private val approved = Delegation(owner, 200_000_000L, 11_839_197L)

    @Test
    fun `reads a delegated bond with a public key`() {
        val tx = TxDecoder.decodeBond(bondNew)
        assertEquals(8_685_000L, tx.lockTime)
        assertEquals(fee, tx.fee)
        assertEquals(hubMemo, tx.memo)
        assertEquals(192, tx.publicKey.length)
        assertEquals(stake, tx.stake)
        assertNotNull(tx.delegateOwner)
        assertEquals(200_000_000L, tx.delegateShare)
        assertEquals(11_839_197L, tx.delegateExpiry)
    }

    @Test
    fun `reads a delegated bond without a key and a plain bond`() {
        val old = TxDecoder.decodeBond(bondOld)
        assertEquals("", old.publicKey)
        assertEquals(500_000_000L, old.delegateShare)
        assertEquals(9_000_000L, old.delegateExpiry)
        val plain = TxDecoder.decodeBond(bondPlain)
        assertNull(plain.delegateOwner)
        assertEquals(5_000_000_000L, plain.stake)
    }

    @Test
    fun `reads a delegated unbond and refuses other payloads`() {
        val tx = TxDecoder.decodeUnbond(unbondDelegated)
        assertEquals("bye", tx.memo)
        assertNotNull(tx.delegateOwner)
        assertThrows(IllegalStateException::class.java) { TxDecoder.decodeBond(bondPlain + "00") }
        assertThrows(IllegalStateException::class.java) { TxDecoder.decodeBond(unbondDelegated) }
    }

    @Test
    fun `accepts the exact approved delegated bond`() {
        TxDecoder.assertBondMatches(bondNew, owner, newValidator, stake, newValidatorKey, fee, hubMemo, approved)
        TxDecoder.assertBondMatches(
            bondOld, owner, oldValidator, stake, null, fee, null, Delegation(owner, 500_000_000L, 9_000_000L),
        )
    }

    private fun failsOn(field: String, check: () -> Unit) {
        val e = assertThrows(IllegalStateException::class.java) { check() }
        assertTrue(field, e.message!!.contains(field))
    }

    @Test
    fun `fails closed on any changed field`() {
        val bond = { validator: String, key: String?, memo: String, d: Delegation? ->
            TxDecoder.assertBondMatches(bondNew, owner, validator, stake, key, fee, memo, d)
        }
        failsOn("delegate share") { bond(newValidator, newValidatorKey, hubMemo, approved.copy(shareNanoPac = 250_000_000L)) }
        failsOn("delegate expiry") { bond(newValidator, newValidatorKey, hubMemo, approved.copy(expiry = 11_839_198L)) }
        failsOn("delegate owner") { bond(newValidator, newValidatorKey, hubMemo, approved.copy(owner = oldOwner)) }
        failsOn("validator") { bond(oldValidator, newValidatorKey, hubMemo, approved) }
        failsOn("public key") { bond(newValidator, null, hubMemo, approved) }
        failsOn("memo") { bond(newValidator, newValidatorKey, "", approved) }
    }

    @Test
    fun `refuses a delegated bond where a plain one was approved, and the other way round`() {
        failsOn("delegation") { TxDecoder.assertBondMatches(bondOld, owner, oldValidator, stake, null, fee, null, null) }
        failsOn("delegation") {
            TxDecoder.assertBondMatches(bondPlain, owner, oldValidator, 5_000_000_000L, null, fee, null, Delegation(owner, 0, 1))
        }
    }

    @Test
    fun `checks the Stake Owner of a delegated unbond`() {
        TxDecoder.assertUnbondMatches(unbondDelegated, oldValidator, oldOwner, "bye")
        failsOn("delegate owner") { TxDecoder.assertUnbondMatches(unbondDelegated, oldValidator, owner, "bye") }
        failsOn("delegate owner") { TxDecoder.assertUnbondMatches(unbondDelegated, oldValidator, null, "bye") }
    }
}
