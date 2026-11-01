package com.andrutstudio.velora.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AnchorRulesTest {

    @Test
    fun `root must be 32 to 64 bytes of hex`() {
        assertEquals("ab".repeat(32), AnchorRules.normalizeRoot(" ${"AB".repeat(32)} "))
        assertEquals("ab".repeat(64), AnchorRules.normalizeRoot("ab".repeat(64)))
        assertNull(AnchorRules.normalizeRoot("ab".repeat(31)))
        assertNull(AnchorRules.normalizeRoot("ab".repeat(65)))
        assertNull(AnchorRules.normalizeRoot("zz".repeat(32)))
        assertNull(AnchorRules.normalizeRoot("a".repeat(65)))
    }

    @Test
    fun `reads a dApp request`() {
        val set = AnchorRules.parseDappRequest("set", "AB".repeat(32), "https://x", "3", "hi")
        assertEquals("ab".repeat(32), set.rootHash)
        assertEquals(3, set.anchorType)
        assertEquals("hi", set.memo)
        assertTrue(AnchorRules.parseDappRequest("delete", "", "", "", "").isDelete)
        assertEquals(0, AnchorRules.parseDappRequest("set", "ab".repeat(32), "", "", "").anchorType)
    }

    @Test
    fun `refuses a bad dApp request`() {
        val root = "ab".repeat(32)
        listOf<() -> Unit>(
            { AnchorRules.parseDappRequest("burn", root, "", "", "") },
            { AnchorRules.parseDappRequest("set", "abcd", "", "", "") },
            { AnchorRules.parseDappRequest("set", root, "a".repeat(129), "", "") },
            { AnchorRules.parseDappRequest("set", root, "", "256", "") },
            { AnchorRules.parseDappRequest("set", root, "", "", "m".repeat(65)) },
            { AnchorRules.requestedDeposit("-1") },
            { AnchorRules.requestedDeposit("lots") },
        ).forEach { assertThrows(IllegalArgumentException::class.java) { it() } }
        assertNull(AnchorRules.requestedDeposit(""))
    }

    @Test
    fun `limits count bytes, not characters`() {
        assertTrue(AnchorRules.isValidUri("a".repeat(128)))
        assertFalse(AnchorRules.isValidUri("a".repeat(129)))
        assertFalse(AnchorRules.isValidMemo("é".repeat(33)))
    }

    @Test
    fun `a new anchor locks the minimum and a replacement locks nothing more`() {
        assertEquals(1_000_000_000, AnchorRules.deposit(null, hasAnchor = false).nanoPac)
        assertEquals(0, AnchorRules.deposit(null, hasAnchor = true).nanoPac)
        assertEquals(5, AnchorRules.deposit(Amount.fromNanoPac(5), hasAnchor = true).nanoPac)
    }
}
