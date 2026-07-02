package com.efa.assistant.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efa.assistant.core.common.DispatcherProvider
import com.efa.assistant.core.model.Action
import com.efa.assistant.core.model.Mission
import com.efa.assistant.core.model.repository.AnalyticsRepository
import com.efa.assistant.core.model.repository.MissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FocusViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _mission = MutableStateFlow<Mission?>(null)
    val mission: StateFlow<Mission?> = _mission.asStateFlow()

    private val _action = MutableStateFlow<Action?>(null)
    val action: StateFlow<Action?> = _action.asStateFlow()

    private val _timeLeftSeconds = MutableStateFlow(0L)
    val timeLeftSeconds: StateFlow<Long> = _timeLeftSeconds.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _showWaterReminder = MutableStateFlow(false)
    val showWaterReminder: StateFlow<Boolean> = _showWaterReminder.asStateFlow()

    private var timerJob: Job? = null
    
    // 跟踪单次打开此页面的总专注时长，用于 Hyperfocus 检测
    private var totalFocusedSecondsThisSession = 0L
    private var originalDurationSeconds = 0L

    fun initFocus(missionId: String, actionId: String) {
        viewModelScope.launch(dispatcherProvider.io()) {
            val missionDetail = missionRepository.getMissionById(missionId)
            _mission.value = missionDetail
            val actionDetail = missionDetail?.actions?.firstOrNull { it.id == actionId }
            _action.value = actionDetail

            if (actionDetail != null) {
                originalDurationSeconds = actionDetail.durationMinutes * 60L
                _timeLeftSeconds.value = originalDurationSeconds
                startTimer()
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        _isRunning.value = true
        timerJob = viewModelScope.launch(dispatcherProvider.main()) {
            while (_timeLeftSeconds.value > 0 && _isRunning.value) {
                delay(1000)
                _timeLeftSeconds.value -= 1
                totalFocusedSecondsThisSession += 1

                // 检测连续专注达 90 分钟 (5400 秒)
                if (totalFocusedSecondsThisSession >= 5400 && !_showWaterReminder.value) {
                    _showWaterReminder.value = true
                }
            }
            if (_timeLeftSeconds.value == 0L) {
                _isRunning.value = false
            }
        }
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
    }

    fun resumeTimer() {
        startTimer()
    }

    fun dismissWaterReminder() {
        _showWaterReminder.value = false
        // 重置会话累计，以便在下个 90 分钟再次提醒
        totalFocusedSecondsThisSession = 0L
    }

    fun completeAction(onCompleted: () -> Unit) {
        pauseTimer()
        val currentMission = _mission.value ?: return
        val currentAction = _action.value ?: return

        viewModelScope.launch(dispatcherProvider.io()) {
            // 标记行动完成
            missionRepository.updateActionCompletion(currentAction.id, true)
            // 写入专注分析日志
            val actualSeconds = originalDurationSeconds - _timeLeftSeconds.value
            analyticsRepository.recordFocusSession(
                missionId = currentMission.id,
                actionId = currentAction.id,
                durationSeconds = if (actualSeconds > 0) actualSeconds else 1,
                isCompleted = true
            )
            // 判断大任务是否也已全部完成
            val updatedMission = missionRepository.getMissionById(currentMission.id)
            val isAllCompleted = updatedMission?.actions?.all { it.isCompleted } ?: false
            if (isAllCompleted) {
                missionRepository.updateMissionCompletion(currentMission.id, true)
            }
            
            launch(dispatcherProvider.main()) {
                onCompleted()
            }
        }
    }

    fun quitFocus(onQuit: () -> Unit) {
        pauseTimer()
        val currentMission = _mission.value
        val currentAction = _action.value
        viewModelScope.launch(dispatcherProvider.io()) {
            if (currentMission != null && currentAction != null) {
                val actualSeconds = originalDurationSeconds - _timeLeftSeconds.value
                if (actualSeconds > 0) {
                    // 即使未计满打断，也记录实际专注时间，保护用户的每一滴努力（No Shame，有记录即是收获）
                    analyticsRepository.recordFocusSession(
                        missionId = currentMission.id,
                        actionId = currentAction.id,
                        durationSeconds = actualSeconds,
                        isCompleted = false
                    )
                }
            }
            launch(dispatcherProvider.main()) {
                onQuit()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
