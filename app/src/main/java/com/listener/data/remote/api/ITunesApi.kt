package com.listener.data.remote.api

import com.listener.data.remote.dto.ITunesSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ITunesApi {
    @GET("search")
    suspend fun searchPodcasts(
        @Query("term") term: String,
        @Query("media") media: String = "podcast",
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): ITunesSearchResponse
}
