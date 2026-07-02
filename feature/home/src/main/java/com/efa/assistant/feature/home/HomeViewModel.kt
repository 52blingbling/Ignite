package com.efa.assistant.feature.home

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efa.assistant.core.ai.AIProvider
import com.efa.assistant.core.ai.PromptManager
import com.efa.assistant.core.common.DispatcherProvider
import com.efa.assistant.core.common.UiState
import com.efa.assistant.core.common.stateInViewModel
import com.efa.assistant.core.model.Action
import com.efa.assistant.core.model.Mission
import com.efa.assistant.core.model.repository.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
    private val aiProvider: AIProvider,
    private val promptManager: PromptManager,
    @Named("encrypted_prefs") private val sharedPreferences: SharedPreferences,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    /**
     * 观察当前执行中的主任务 (Current Mission)。
     */
    val currentMissionState: StateFlow<UiState<Mission?>> = missionRepository.getCurrentMission()
        .map { UiState.Success(it) as UiState<Mission?> }
        .catch { emit(UiState.Error(it)) }
        .stateInViewModel(this, UiState.Loading)

    private val _eventFlow = MutableSharedFlow<HomeEvent>()
    val eventFlow: SharedFlow<HomeEvent> = _eventFlow

    /**
     * 创建并拆分新任务。
     * 检测本地 API Key 状态，有 Key 时自动启用 AI 智能拆分，无 Key 或网络故障时无感降级到本地规则引擎。
     */
    fun startNewMission(title: String, durationMinutes: Int) {
        viewModelScope.launch(dispatcherProvider.io()) {
            try {
                val apiKey = sharedPreferences.getString("api_key", "") ?: ""
                val provider = sharedPreferences.getString("ai_provider", "Gemini") ?: "Gemini"
                
                // 本地 Ollama 或 Local LLM 部署通常不需要外网 API Key 校验
                val isAiConfigured = apiKey.isNotBlank() || provider == "Ollama" || provider == "Local LLM"

                val actions = if (isAiConfigured) {
                    try {
                        aiProvider.splitTask(title, promptManager)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 网络故障、API 超时或 Key 失效时，温和降级至本地拆分引擎，保障用户体验不中断
                        splitTaskLocally(title, durationMinutes)
                    }
                } else {
                    splitTaskLocally(title, durationMinutes)
                }
                
                missionRepository.createMission(
                    title = title,
                    durationMinutes = durationMinutes,
                    actions = actions
                )
                _eventFlow.emit(HomeEvent.MissionCreated)
            } catch (e: Exception) {
                _eventFlow.emit(HomeEvent.Error(e.localizedMessage ?: "创建失败"))
            }
        }
    }

    /**
     * 推迟当前任务（增加拖延/延迟计数，支持“No Shame”原则）。
     */
    fun deferCurrentMission(missionId: String) {
        viewModelScope.launch(dispatcherProvider.io()) {
            missionRepository.incrementDeferredCount(missionId)
            _eventFlow.emit(HomeEvent.MissionDeferred)
        }
    }

    /**
     * 标记整个任务完成。
     */
    fun completeMission(missionId: String) {
        viewModelScope.launch(dispatcherProvider.io()) {
            missionRepository.updateMissionCompletion(missionId, true)
            _eventFlow.emit(HomeEvent.MissionCompleted)
        }
    }

    /**
     * 本地任务自动拆分规则引擎 (1~5分钟原则)
     */
    private fun splitTaskLocally(title: String, durationMinutes: Int): List<Action> {
        val actions = mutableListOf<Action>()
        if (durationMinutes <= 5) {
            actions.add(Action(UUID.randomUUID().toString(), "立即开始行动: $title", durationMinutes))
        } else {
            actions.add(Action(UUID.randomUUID().toString(), "准备工作：打开相关工具/软件", 2))
            
            var remainingTime = durationMinutes - 2
            var index = 1
            while (remainingTime > 0) {
                val stepTime = if (remainingTime >= 5) 5 else remainingTime
                actions.add(
                    Action(
                        UUID.randomUUID().toString(),
                        "开展第 $index 步行动：处理分项内容",
                        stepTime
                    )
                )
                remainingTime -= stepTime
                index++
            }
            
            actions.add(Action(UUID.randomUUID().toString(), "整理结果，存盘/保存并关闭", 2))
        }
        return actions
    }
}

sealed interface HomeEvent {
    data object MissionCreated : HomeEvent
    data object MissionDeferred : HomeEvent
    data object MissionCompleted : HomeEvent
    data class Error(val message: String) : HomeEvent
}
