package com.andrutstudio.velora.data.rpc

import com.andrutstudio.velora.data.rpc.model.RpcError
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RpcErrorTest {

    @Test
    fun `a node that needs a login says nothing about the request`() {
        assertTrue(RpcError(-32000, "Unauthenticated").isPrivateNode())
        assertTrue(RpcError(16, "rpc error: code = Unauthenticated desc = missing token").isPrivateNode())
    }

    @Test
    fun `other node errors still count`() {
        assertFalse(RpcError(-32000, "insufficient balance").isPrivateNode())
    }
}
