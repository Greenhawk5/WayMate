package ir.alifaniani.waymate.core.network

/**
 * Domain-mapped exceptions for network and API responses.
 */
sealed class ApiException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class Unauthorized(message: String = "نشست کاربری نامعتبر است یا منقضی شده است") : ApiException(message)
    class Forbidden(message: String = "دسترسی به این منبع برای نقش شما مجاز نیست") : ApiException(message)
    class NotFound(message: String = "منبع درخواستی یافت نشد") : ApiException(message)
    class RateLimited(message: String = "تعداد درخواست‌ها بیش از حد مجاز است") : ApiException(message)
    class Validation(val errors: List<String>, message: String = "خطای اعتبارسنجی مقادیر ورودی") : ApiException(message)
    class NetworkUnavailable(message: String = "ارتباط با شبکه تاکتیکی برقرار نیست") : ApiException(message)
    class ServerError(val statusCode: Int, message: String = "خطای غیرمنتظره در سرور رخ داد") : ApiException(message)
}
