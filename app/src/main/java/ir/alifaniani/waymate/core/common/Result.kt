package ir.alifaniani.waymate.core.common

/**
 * A generic sealed hierarchy representing the outcome of an operation.
 */
sealed interface Result<out T> {
    data class Success<out T>(val data: T) : Result<T>
    data class Error(val throwable: Throwable, val message: String? = throwable.message) : Result<Nothing>
    data object Loading : Result<Nothing>
}
