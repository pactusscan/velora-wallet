package com.andrutstudio.velora.domain.repository

interface MarketRepository {
    suspend fun getPacPriceUsd(): Double
}
