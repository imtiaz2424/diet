package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class UserProfileDto(
    @Json(name = "id") val id: Int = 1,
    @Json(name = "age") val age: Int,
    @Json(name = "gender") val gender: String,
    @Json(name = "weight") val weight: Double,
    @Json(name = "height") val height: Double,
    @Json(name = "goal") val goal: String,
    @Json(name = "dietary_preference") val dietaryPreference: String,
    @Json(name = "allergies") val allergies: String,
    @Json(name = "daily_calorie_target") val dailyCalorieTarget: Int,
    @Json(name = "daily_water_target_ml") val dailyWaterTargetMl: Int = 2500,
    @Json(name = "medical_conditions") val medicalConditions: List<String> = emptyList(),
    @Json(name = "cuisine_preferences") val cuisinePreferences: List<String> = emptyList(),
    @Json(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis()
)

interface UserProfileApiService {
    @GET("api/user/profile")
    suspend fun getUserProfile(): UserProfileDto
}

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class SchemaProperty(
    val type: String,
    val description: String? = null,
    val properties: Map<String, SchemaProperty>? = null,
    val required: List<String>? = null,
    val items: SchemaProperty? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormatText(
    val mimeType: String,
    val responseSchema: SchemaProperty? = null
)

@JsonClass(generateAdapter = true)
data class ResponseFormat(
    val text: ResponseFormatText? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val responseFormat: ResponseFormat? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class PartResponse(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class ContentResponse(
    val parts: List<PartResponse>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: ContentResponse? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class DietPlanJsonResponse(
    val totalCalories: Int,
    val breakfast: String,
    val breakfastCal: Int,
    val snack1: String,
    val snack1Cal: Int,
    val lunch: String,
    val lunchCal: Int,
    val snack2: String,
    val snack2Cal: Int,
    val dinner: String,
    val dinnerCal: Int,
    val dailyTip: String
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val moshi = com.squareup.moshi.Moshi.Builder()
        .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    val userProfileService: UserProfileApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://ais-dev-w6uvgg7wwi6pfad6psdksq-981939184447.asia-southeast1.run.app/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UserProfileApiService::class.java)
    }

    val moshiInstance: com.squareup.moshi.Moshi = moshi
}
