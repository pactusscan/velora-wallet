package com.andrutstudio.velora.data.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TxDecoderTest {

    // Pactus node vector: fee 1000, memo "test", amount 20000, then signature and key
    private val transfer = "00" + "01" + "01020300" + "e807" + "0474657374" + "01" +
        "03706ce1d84f2c03c543dfecd6adb96bfa653fe560" +
        "03088868ebc9ec0528a46000bc7dae285eb65a0ef0" +
        "a09c01" +
        "109a480d296f61e793b64a9ffcf1ce4a5a535c882edfa9c52aa0af8003e39cee" +
        "da02d5c5ce69c135514bbd1f4c6c42798fe2b9615e56ebef93ad2923b33f170e" +
        "ade295295be1368479ecf0345c13ea0db1248d4dffe39eac627319954d39b264"

    private val sender = "pc1rwpkwrkz09spu2s7lant2mwttlfjnletqkqnlpq"
    private val receiver = "pc1rpzyx367faszj3frqqz78mt3gt6m95rhsvavwd2"

    private fun assertMismatch(
        field: String,
        from: String = sender,
        to: String = receiver,
        amount: Long = 20_000,
        fee: Long = 1_000,
        memo: String? = "test",
    ) {
        val error = assertThrows(IllegalStateException::class.java) {
            TxDecoder.assertTransferMatches(transfer, from, to, amount, fee, memo)
        }
        assertTrue(error.message, error.message!!.contains(field))
    }

    @Test
    fun `decodes header and transfer payload`() {
        val tx = TxDecoder.decodeTransfer(transfer)
        assertEquals(1_000, tx.fee)
        assertEquals("test", tx.memo)
        assertEquals(20_000, tx.amount)
        assertEquals(0x030201, tx.lockTime)
    }

    @Test
    fun `passes when the raw transaction matches`() {
        TxDecoder.assertTransferMatches(transfer, sender, receiver, 20_000, 1_000, "test")
    }

    @Test
    fun `refuses a swapped sender or receiver`() {
        assertMismatch("sender", from = receiver)
        assertMismatch("receiver", to = sender)
    }

    @Test
    fun `refuses a changed amount, fee or memo`() {
        assertMismatch("amount", amount = 999_999)
        assertMismatch("fee", fee = 5_000)
        assertMismatch("memo", memo = "evil")
        assertMismatch("memo", memo = null)
    }

    @Test
    fun `refuses another payload type`() {
        val bond = transfer.replaceFirst("0474657374" + "01", "0474657374" + "02")
        assertThrows(IllegalStateException::class.java) { TxDecoder.decodeTransfer(bond) }
    }

    @Test
    fun `refuses truncated bytes`() {
        assertThrows(IllegalStateException::class.java) { TxDecoder.decodeTransfer("0001") }
    }
}
