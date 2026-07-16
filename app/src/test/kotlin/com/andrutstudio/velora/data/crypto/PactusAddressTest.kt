package com.andrutstudio.velora.data.crypto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PactusAddressTest {

    // Vectors below were verified against an independent bech32m implementation.

    @Test
    fun `accepts ed25519 mainnet addresses`() {
        assertTrue(PactusAddress.isValidAccountAddress("pc1rlw4vxhmcrn790jlm0a2xh22vk6jllqh82yvl29"))
        assertTrue(PactusAddress.isValidAccountAddress("pc1rn3h74rlujrs3p4u8ad2yggkxj367td3crhugle"))
    }

    @Test
    fun `accepts secp256k1 pc1y address`() {
        assertTrue(PactusAddress.isValidAccountAddress("pc1yqqqsyqcyq5rqwzqfpg9scrgwpugpzysnmpzxdr"))
    }

    @Test
    fun `accepts testnet secp256k1 address`() {
        assertTrue(PactusAddress.isValidAccountAddress("tpc1yqqqsyqcyq5rqwzqfpg9scrgwpugpzysnp8s03g"))
    }

    @Test
    fun `accepts uppercase input`() {
        assertTrue(PactusAddress.isValidAccountAddress("PC1RLW4VXHMCRN790JLM0A2XH22VK6JLLQH82YVL29"))
    }

    @Test
    fun `rejects type 0 even when bech32m-encoded`() {
        // Type 0 is reserved for the treasury. Core renders the treasury as a
        // literal zero string, so a pc1q address should never exist — reject
        // the synthetic encoding anyway.
        assertFalse(PactusAddress.isValidAccountAddress("pc1qqqqsyqcyq5rqwzqfpg9scrgwpugpzysn7zvrsx"))
    }

    @Test
    fun `rejects the literal treasury address`() {
        // pactus-core special-cases the treasury as 42 zeros, not bech32m.
        assertFalse(PactusAddress.isValidAccountAddress("000000000000000000000000000000000000000000"))
    }

    @Test
    fun `rejects corrupted checksum`() {
        // Last character flipped on an otherwise valid address.
        assertFalse(PactusAddress.isValidAccountAddress("pc1rlw4vxhmcrn790jlm0a2xh22vk6jllqh82yvl28"))
    }

    @Test
    fun `rejects invalid charset`() {
        // Vanity string: 'b' and 'o' are not in the bech32 charset.
        assertFalse(PactusAddress.isValidAccountAddress("pc1rmv39cmjl7hknrxn27l5jg5wv67am5hy2velora"))
    }

    @Test
    fun `rejects wrong hrp`() {
        assertFalse(PactusAddress.isValidAccountAddress("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"))
    }

    @Test
    fun `rejects blank and malformed input`() {
        assertFalse(PactusAddress.isValidAccountAddress(""))
        assertFalse(PactusAddress.isValidAccountAddress("pc1"))
        assertFalse(PactusAddress.isValidAccountAddress("not an address"))
    }
}
