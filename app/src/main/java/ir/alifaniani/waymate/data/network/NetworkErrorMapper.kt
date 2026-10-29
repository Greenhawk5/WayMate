package ir.alifaniani.waymate.data.network

import ir.alifaniani.waymate.data.dto.ApiErrorDto
import ir.alifaniani.waymate.data.dto.ApiResponseDto
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Utility for mapping network failures, HTTP error codes, and RFC-7807 error envelopes
 * into user-facing Persian error messages.
 */
object NetworkErrorMapper {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseApiError(errorBody: ResponseBody?): ApiErrorDto? {
        if (errorBody == null) return null
        return try {
            val errorString = errorBody.string()
            val envelope = json.decodeFromString<ApiResponseDto<Unit>>(errorString)
            envelope.error
        } catch (e: Exception) {
            null
        }
    }

    fun mapHttpError(httpCode: Int, apiError: ApiErrorDto?): String {
        if (apiError?.message?.isNotBlank() == true) {
            return apiError.message
        }

        return when (httpCode) {
            400 -> "اطلاعات ورودی نامعتبر است."
            401 -> "نام کاربری یا گذرواژه اشتباه است."
            403 -> "شما مجوز دسترسی به این عملیات را ندارید."
            404 -> "اطلاعات درخواستی در سرور یافت نشد."
            409 -> "تداخل در داده‌های ارسالی با وضعیت فعلی سرور."
            422 -> "داده‌های ارسالی قابل پردازش نیستند."
            429 -> "تعداد درخواست‌ها بیش از حد مجاز است. لطفاً کمی صبر کنید."
            in 500..599 -> "خطای موقت در سرور رخ داده است. لطفاً بعداً دوباره امتحان کنید."
            else -> "خطای غیرمنتظره در ارتباط با سرور ($httpCode)."
        }
    }

    fun mapThrowable(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException, is ConnectException ->
                "عدم برقراری ارتباط با سرور. لطفاً اتصال اینترنت یا آدرس سرویس را بررسی کنید."
            is SocketTimeoutException ->
                "مهلت برقراری ارتباط با سرور به پایان رسید. لطفاً مجدداً تلاش کنید."
            is IOException ->
                "خطای شبکه در تبادل داده با سرور."
            else ->
                throwable.message ?: "خطای ناشناخته رخ داد."
        }
    }
}
