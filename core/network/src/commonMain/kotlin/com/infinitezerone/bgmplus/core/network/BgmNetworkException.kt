package com.infinitezerone.bgmplus.core.network

sealed class BgmNetworkException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class Unauthorized(
        message: String = "未授权或登录已过期",
    ) : BgmNetworkException(message)

    class Forbidden(
        message: String = "访问受限",
    ) : BgmNetworkException(message)

    class NotFound(
        message: String = "请求的资源不存在",
    ) : BgmNetworkException(message)

    class RateLimited(
        message: String = "请求过于频繁，已被限流",
    ) : BgmNetworkException(message)

    class ServerError(
        val statusCode: Int,
        message: String = "Bangumi 服务器异常 ($statusCode)",
    ) : BgmNetworkException(message)

    class Unknown(
        message: String,
        cause: Throwable? = null,
    ) : BgmNetworkException(message, cause)
}
