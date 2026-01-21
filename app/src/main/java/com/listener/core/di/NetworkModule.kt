package com.listener.core.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.listener.BuildConfig
import com.listener.data.remote.api.ITunesApi
import com.listener.data.remote.api.OpenAiApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Dns
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .create()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)  // 3분
            .writeTimeout(180, TimeUnit.SECONDS) // 3분
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        // HEADERS only - BODY causes OOM with large audio files
                        level = HttpLoggingInterceptor.Level.HEADERS
                    })
                }
            }
            .build()
    }

    @Provides
    @Singleton
    @Named("iTunes")
    fun provideITunesRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://itunes.apple.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("OpenAI")
    fun provideOpenAiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideITunesApi(@Named("iTunes") retrofit: Retrofit): ITunesApi {
        return retrofit.create(ITunesApi::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenAiApi(@Named("OpenAI") retrofit: Retrofit): OpenAiApi {
        return retrofit.create(OpenAiApi::class.java)
    }
}
