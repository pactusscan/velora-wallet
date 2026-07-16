package com.andrutstudio.velora.data.crypto

/**
 * Guard for dApp signMessage requests. Pactus signs a message as raw UTF-8 with
 * NO domain-separation prefix (see pactus-core wallet.SignMessage), so a message
 * signature is, byte for byte, the same primitive as a transaction-body
 * signature. A malicious dApp could try to get a signature over bytes that
 * actually form an unsigned transaction body, bypassing the transaction
 * approval flow. Legitimate sign-in messages are always human-readable text, so
 * we refuse messages containing control characters (which binary transaction
 * bytes are full of). This does not change how a legitimate text message is
 * signed — the byte-compat invariant with the node's verify_message holds.
 *
 * Mirrors signGuard.ts in the Velora browser extension.
 */
object SignGuard {

    // Allow tab (0x09), newline (0x0A), carriage return (0x0D); reject other C0
    // and C1 control characters.
    private fun isControlChar(code: Int): Boolean {
        if (code == 0x09 || code == 0x0A || code == 0x0D) return false
        return code <= 0x1F || code in 0x7F..0x9F
    }

    fun isSignableMessage(message: String): Boolean {
        var i = 0
        while (i < message.length) {
            val code = message.codePointAt(i)
            if (isControlChar(code)) return false
            i += Character.charCount(code)
        }
        return true
    }

    fun assertSignableMessage(message: String) {
        require(isSignableMessage(message)) {
            "Message contains non-text characters and cannot be signed"
        }
    }
}
