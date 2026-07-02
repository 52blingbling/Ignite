package com.efa.assistant.core.common

/**
 * 统一 UI 状态封装接口。
 * 供 Presentation 层订阅，以便在 Jetpack Compose 中实现 Loading、Success、Error 等状态的自动切换。
 */
sealed interface UiState<out T> {
    /**
     * 数据加载中状态。
     */
    data object Loading : UiState<Nothing>

    /**
     * 数据加载成功状态。
     * @param data 成功的业务数据。
     */
    data class Success<out T>(val data: T) : UiState<T>

    /**
     * 数据加载失败状态。
     * @param exception 捕获的异常信息。
     * @param message 可选的错误文案。
     */
    data class Error(
        val exception: Throwable,
        val message: String? = exception.localizedMessage
    ) : UiState<Nothing>
}
