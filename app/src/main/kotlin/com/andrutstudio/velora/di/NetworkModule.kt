package com.andrutstudio.velora.di

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import com.andrutstudio.velora.BuildConfig
import com.andrutstudio.velora.data.market.MarketApiService
import com.andrutstudio.velora.data.rpc.PactusRpcService
import com.andrutstudio.velora.data.rpc.PactusScanApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .apply {
            addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "VeloraWallet/1.0 (Android)")
                    .build()
                chain.proceed(request)
            }
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }
                )
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRpcService(okHttpClient: OkHttpClient, gson: Gson): PactusRpcService =
        Retrofit.Builder()
            .baseUrl("https://bootstrap1.pactus.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(PactusRpcService::class.java)

    @Provides
    @Singleton
    fun provideMarketApiService(okHttpClient: OkHttpClient): MarketApiService =
        Retrofit.Builder()
            .baseUrl("https://api.pactusscan.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MarketApiService::class.java)

    @Provides
    @Singleton
    fun providePactusScanApiService(okHttpClient: OkHttpClient): PactusScanApiService =
        Retrofit.Builder()
            .baseUrl("https://api.pactusscan.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PactusScanApiService::class.java)
}
