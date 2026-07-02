package com.efa.assistant.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * 将普通冷流 (Flow) 转换为生命周期感知的热流 (StateFlow)。
 * 适用于 ViewModel 中将数据源映射为 UI State，并在界面不可见时（5秒后）自动停止上游数据流以节省资源。
 *
 * @param viewModel 所属的 ViewModel。
 * @param initialValue 初始 UI 状态值。
 * @return 订阅后的 StateFlow。
 */
fun <T> Flow<T>.stateInViewModel(
    viewModel: ViewModel,
    initialValue: T
): StateFlow<T> = this.stateIn(
    scope = viewModel.viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = initialValue
)
