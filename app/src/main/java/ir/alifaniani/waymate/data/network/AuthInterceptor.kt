package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.data.session.SessionStorage
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that dynamically attaches the stored JWT Bearer token
 * to authenticated outbound HTTP requests.
 */
class AuthInterceptor(
    private val sessionStorage: SessionStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding Authorization header if already present or on login endpoint
        if (originalRequest.header(HEADER_AUTHORIZATION) != null) {
            return chain.proceed(originalRequest)
        }

        val token = sessionStorage.getToken()
        val authenticatedRequest = if (!token.isNullOrBlank()) {
            originalRequest.newBuilder()
                .header(HEADER_AUTHORIZATION, "Bearer $token")
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(authenticatedRequest)
    }

    companion object {
        const val HEADER_AUTHORIZATION = "Authorization"
    }
}
