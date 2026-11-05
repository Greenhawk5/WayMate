package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.data.session.SessionStorage
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory and provider for the application's Retrofit HTTP networking stack.
 */
class NetworkClient(
    private val sessionStorage: SessionStorage,
    private val baseUrlProvider: () -> String = { ApiConfig.getBaseUrl() }
) {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        encodeDefaults = true
    }

    val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
            redactHeader(AuthInterceptor.HEADER_AUTHORIZATION)
        }

        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(AuthInterceptor(sessionStorage))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val retrofit: Retrofit by lazy {
        val contentType = "application/json".toMediaType()
        Retrofit.Builder()
            .baseUrl(baseUrlProvider())
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    fun createAuthApiService(): AuthApiService {
        return retrofit.create(AuthApiService::class.java)
    }

    fun createTeamApiService(): TeamApiService {
        return retrofit.create(TeamApiService::class.java)
    }

    fun createMessageApiService(): MessageApiService {
        return retrofit.create(MessageApiService::class.java)
    }

    fun createStatusApiService(): StatusApiService {
        return retrofit.create(StatusApiService::class.java)
    }

    fun createUserApiService(): UserApiService {
        return retrofit.create(UserApiService::class.java)
    }

    fun createAdminApiService(): AdminApiService {
        return retrofit.create(AdminApiService::class.java)
    }

    fun createPointApiService(): PointOfInterestApiService {
        return retrofit.create(PointOfInterestApiService::class.java)
    }


    companion object {
        fun createCustomClient(
            baseUrl: String,
            sessionStorage: SessionStorage,
            customOkHttpClient: OkHttpClient? = null
        ): Retrofit {
            val json = Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
                encodeDefaults = true
            }
            val client = customOkHttpClient ?: OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .addInterceptor(AuthInterceptor(sessionStorage))
                .build()

            val contentType = "application/json".toMediaType()
            return Retrofit.Builder()
                .baseUrl(if (!baseUrl.endsWith("/")) "$baseUrl/" else baseUrl)
                .client(client)
                .addConverterFactory(json.asConverterFactory(contentType))
                .build()
        }
    }
}
