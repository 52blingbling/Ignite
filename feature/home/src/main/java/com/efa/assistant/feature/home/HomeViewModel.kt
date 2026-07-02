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

    /**
     * 历史任务列表（过滤重复任务，取最近的 5 条）。
     */
    val recentMissions: StateFlow<List<Mission>> = missionRepository.getAllMissions()
        .map { allMissions ->
            allMissions.filter { it.isCompleted } // 只取完成过的任务，或者直接按时间排序
                .sortedByDescending { it.createdAt }
                .distinctBy { it.title } // 去除同名任务
                .take(5)
        }
        .stateInViewModel(this, emptyList())

    private val _eventFlow = MutableSharedFlow<HomeEvent>()
    val eventFlow: SharedFlow<HomeEvent> = _eventFlow

    private val _draftMission = MutableStateFlow<Mission?>(null)
    val draftMission: StateFlow<Mission?> = _draftMission.asStateFlow()

    private val _isDraftLoading = MutableStateFlow(false)
    val isDraftLoading: StateFlow<Boolean> = _isDraftLoading.asStateFlow()

    private val _draftError = MutableStateFlow<String?>(null)
    val draftError: StateFlow<String?> = _draftError.asStateFlow()

    /**
     * 创建并拆分新任务。
     * 检测本地 API Key 状态，有 Key 时自动启用 AI 智能拆分，无 Key 或网络故障时无感降级到本地规则引擎。
     */
    fun startNewMissionDraft(title: String, durationMinutes: Int, extraInstructions: String = "") {
        viewModelScope.launch(dispatcherProvider.io()) {
            _isDraftLoading.value = true
            _draftError.value = null
            try {
                val apiKey = sharedPreferences.getString("api_key", "") ?: ""
                val provider = sharedPreferences.getString("ai_provider", "Gemini") ?: "Gemini"
                
                // 本地 Ollama 或 Local LLM 部署通常不需要外网 API Key 校验
                val isAiConfigured = apiKey.isNotBlank() || provider == "Ollama" || provider == "Local LLM"

                val actions = if (isAiConfigured) {
                    try {
                        aiProvider.splitTask(title, extraInstructions, promptManager)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // 网络故障、API 超时或 Key 失效时，温和降级至本地拆分引擎
                        splitTaskLocally(title, durationMinutes)
                    }
                } else {
                    splitTaskLocally(title, durationMinutes)
                }
                
                _draftMission.value = Mission(
                    id = UUID.randomUUID().toString(),
                    title = title,
                    durationMinutes = durationMinutes,
                    actions = actions,
                    createdAt = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _draftError.value = e.localizedMessage ?: "生成草稿失败"
            } finally {
                _isDraftLoading.value = false
            }
        }
    }

    /**
     * 确认并保存草稿任务。
     */
    fun confirmDraft() {
        val draft = _draftMission.value ?: return
        viewModelScope.launch(dispatcherProvider.io()) {
            try {
                missionRepository.createMission(
                    title = draft.title,
                    durationMinutes = draft.durationMinutes,
                    actions = draft.actions
                )
                _draftMission.value = null
                _eventFlow.emit(HomeEvent.MissionCreated)
            } catch (e: Exception) {
                _eventFlow.emit(HomeEvent.Error(e.localizedMessage ?: "保存任务失败"))
            }
        }
    }

    /**
     * 取消草稿任务。
     */
    fun cancelDraft() {
        _draftMission.value = null
        _draftError.value = null
    }

    /**
     * 针对当前草稿附加需求并重新生成。
     */
    fun regenerateDraft(extraInstructions: String) {
        val draft = _draftMission.value ?: return
        startNewMissionDraft(draft.title, draft.durationMinutes, extraInstructions)
    }

    /**
     * 从历史任务中直接拷贝并创建草稿。
     */
    fun reuseHistoricalMission(mission: Mission) {
        val clonedActions = mission.actions.map { oldAction ->
            oldAction.copy(
                id = UUID.randomUUID().toString(),
                isCompleted = false
            )
        }
        _draftMission.value = Mission(
            id = UUID.randomUUID().toString(),
            title = mission.title,
            durationMinutes = mission.durationMinutes,
            actions = clonedActions,
            createdAt = System.currentTimeMillis()
        )
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
