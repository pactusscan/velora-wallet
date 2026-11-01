package com.andrutstudio.velora.data.crypto

import com.andrutstudio.velora.domain.model.Amount
import com.andrutstudio.velora.domain.model.AnchorRequest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

// The raw vectors were returned by a Pactus 1.17.0 node (get_raw_anchor_transaction
// on testnet), the same ones the browser extension tests with.
class AnchorTxTest {

    private val sender = "tpc1rw03cn7xkd7jjl5ajjkwj67wwhsf7e252fu50nz"
    private val lock = 0x5a89ceL
    private val root = "ab".repeat(32)
    private val from = "0373e389f8d66fa52fd3b2959d2d79cebc13ecaa8a"

    // Lock time, fee 0.01 PAC, memo "hi", replacing a root with no new deposit
    private val setRaw = "0201ce895a0080ade20402686907${from}0020${root}000000"
    private val createRaw = "0201ce895a00000007${from}0020${root}00008094ebdc03"
    private val deleteRaw = "0201ce895a00000007${from}01"
    // What the node built when given the root as hex: it read it as base64
    private val misreadRootRaw = "0201ce895a0080ade20402686907${from}0030${"69b69b".repeat(16)}000000"

    private val expected = AnchorRequest(
        sender = sender,
        isDelete = false,
        rootHash = root,
        fee = Amount.fromNanoPac(10_000_000),
        memo = "hi",
    )

    private fun assertMismatch(raw: String, request: AnchorRequest, field: String) {
        val error = assertThrows(IllegalStateException::class.java) {
            TxDecoder.assertAnchorMatches(raw, request, lock)
        }
        assertTrue(error.message, error.message!!.contains(field))
    }

    @Test
    fun `decodes a set`() {
        val tx = TxDecoder.decodeAnchor(createRaw)
        assertEquals(0, tx.action)
        assertEquals(root, tx.rootHash)
        assertEquals(1_000_000_000, tx.deposit)
        assertEquals(lock, tx.lockTime)
    }

    @Test
    fun `decodes a delete, which ends after the action`() {
        val tx = TxDecoder.decodeAnchor(deleteRaw)
        assertEquals(1, tx.action)
        assertEquals("", tx.rootHash)
        assertEquals(0, tx.deposit)
    }

    @Test
    fun `refuses another payload type, unknown actions and trailing bytes`() {
        val cases = listOf(
            deleteRaw.replace("07$from", "01$from"),
            deleteRaw.dropLast(2) + "02",
            deleteRaw + "00",
        )
        cases.forEach { raw ->
            assertThrows(IllegalStateException::class.java) { TxDecoder.decodeAnchor(raw) }
        }
    }

    @Test
    fun `passes a set and a delete that match`() {
        TxDecoder.assertAnchorMatches(setRaw, expected, lock)
        TxDecoder.assertAnchorMatches(deleteRaw, AnchorRequest(sender = sender, isDelete = true), lock)
    }

    @Test
    fun `refuses a root the node misread`() {
        assertMismatch(misreadRootRaw, expected, "root hash")
    }

    @Test
    fun `refuses each changed field`() {
        assertMismatch(setRaw, expected.copy(sender = "tpc1rwpkwrkz09spu2s7lant2mwttlfjnletq9avmww"), "sender")
        assertMismatch(setRaw, expected.copy(isDelete = true), "action")
        assertMismatch(setRaw, expected.copy(manifestUri = "x"), "manifest URI")
        assertMismatch(setRaw, expected.copy(anchorType = 1), "anchor type")
        assertMismatch(setRaw, expected.copy(deposit = Amount.fromPac(1.0)), "deposit")
        assertMismatch(setRaw, expected.copy(fee = Amount.ZERO), "fee")
        assertMismatch(setRaw, expected.copy(memo = ""), "memo")
    }

    @Test
    fun `refuses another lock time`() {
        val error = assertThrows(IllegalStateException::class.java) {
            TxDecoder.assertAnchorMatches(setRaw, expected, lock + 1)
        }
        assertTrue(error.message!!.contains("lock time"))
    }

    @Test
    fun `root hex converts for the node`() {
        assertArrayEquals(ByteArray(32) { 0xab.toByte() }, TxDecoder.hexToBytes(root))
    }
}
