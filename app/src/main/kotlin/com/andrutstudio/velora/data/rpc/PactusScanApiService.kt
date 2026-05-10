package com.andrutstudio.velora.data.rpc

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PactusScanApiService {
    @GET("api/v1/account/{address}/txs")
    suspend fun getTransactions(
        @Path("address") address: String,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): PactusScanTxListResponse

    @GET("api/v1/transaction/{hash}")
    suspend fun getTransaction(
        @Path("hash") hash: String
    ): PactusScanTxResponse

    @GET("api/v1/validators/{address}/peer")
    suspend fun getValidatorPeer(
        @Path("address") address: String
    ): PeerDetailResponse

    @GET("api/v1/peers/{peerId}")
    suspend fun getPeerById(
        @Path("peerId") peerId: String
    ): PeerDetailResponse
}

data class PactusScanTxListResponse(
    @SerializedName("total") val total: Int,
    @SerializedName("txs") val txs: List<PactusScanTxResponse>
)

// ── Node monitoring models ────────────────────────────────────────────────────

data class PeerDetailResponse(
    @SerializedName("peer") val peer: PeerInfo,
    @SerializedName("peer_online") val peerOnline: Boolean,
    @SerializedName("validators") val validators: List<ValidatorPeerInfo>
)

data class PeerInfo(
    @SerializedName("peer_id") val peerId: String,
    @SerializedName("moniker") val moniker: String,
    @SerializedName("agent") val agent: String,
    @SerializedName("agent_parsed") val agentParsed: AgentParsed,
    @SerializedName("status") val status: Int,
    @SerializedName("height") val height: Long,
    @SerializedName("last_sent") val lastSent: Long,
    @SerializedName("last_received") val lastReceived: Long,
    @SerializedName("address") val address: String,
    @SerializedName("direction") val direction: Int,
    @SerializedName("services") val services: Int
)

data class AgentParsed(
    @SerializedName("node_type") val nodeType: String,
    @SerializedName("node_version") val nodeVersion: String,
    @SerializedName("os") val os: String,
    @SerializedName("arch") val arch: String,
    @SerializedName("protocol_version") val protocolVersion: String
)

data class ValidatorPeerInfo(
    @SerializedName("address") val address: String,
    @SerializedName("stake") val stake: Long,
    @SerializedName("availability_score") val availabilityScore: Double,
    @SerializedName("last_sortition_height") val lastSortitionHeight: Long
)

// ─────────────────────────────────────────────────────────────────────────────

data class PactusScanTxResponse(
    @SerializedName("hash") val id: String,
    @SerializedName("payload_type") val payloadType: Int,
    @SerializedName("sender") val sender: String?,
    @SerializedName("receiver") val receiver: String?,
    @SerializedName("amount") val amountNanoPac: Long,
    @SerializedName("fee") val feeNanoPac: Long,
    @SerializedName("memo") val memo: String?,
    @SerializedName("block_time") val blockTime: Long,
    @SerializedName("block_height") val blockHeight: Long,
    @SerializedName("direction") val direction: Int // 0=self, 1=in, 2=out
)
