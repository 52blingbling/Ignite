package com.efa.assistant.feature.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efa.assistant.core.common.DispatcherProvider
import com.efa.assistant.core.model.Action
import com.efa.assistant.core.model.Mission
import com.efa.assistant.core.model.repository.AnalyticsRepository
import com.efa.assistant.core.model.repository.MissionRepository
import com.efa.assistant.feature.focus.audio.NoiseSynthesizer
import com.efa.assistant.feature.focus.audio.NoiseType
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

    // 氛围白噪音状态
    private val _activeNoiseType = MutableStateFlow(NoiseType.NONE)
    val activeNoiseType: StateFlow<NoiseType> = _activeNoiseType.asStateFlow()

    // 严格模式开关与失败状态
    private val _isStrictMode = MutableStateFlow(false)
    val isStrictMode: StateFlow<Boolean> = _isStrictMode.asStateFlow()

    private val _isDistractedFailed = MutableStateFlow(false)
    val isDistractedFailed: StateFlow<Boolean> = _isDistractedFailed.asStateFlow()

    private val noiseSynthesizer = NoiseSynthesizer(dispatcherProvider.default())
    private var timerJob: Job? = null
    private var distractionJob: Job? = null
    
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
        startNoiseIfEnabled()
        timerJob = viewModelScope.launch(dispatcherProvider.main()) {
            while (_isRunning.value) {
                delay(1000)
                // 弹性计时 (Flow State): 允许倒计时减到负数（代表心流溢出），不自动结束
                _timeLeftSeconds.value -= 1
                totalFocusedSecondsThisSession += 1

                // 检测连续专注达 90 分钟 (5400 秒)
                if (totalFocusedSecondsThisSession >= 5400 && !_showWaterReminder.value) {
                    _showWaterReminder.value = true
                }
            }
        }
    }

    fun pauseTimer() {
        _isRunning.value = false
        timerJob?.cancel()
        stopNoise()
    }

    fun resumeTimer() {
        if (_isDistractedFailed.value) return // 已经开小差判定失败，不允许继续
        startTimer()
    }

    fun dismissWaterReminder() {
        _showWaterReminder.value = false
        // 重置会话累计，以便在下个 90 分钟再次提醒
        totalFocusedSecondsThisSession = 0L
    }

    // 设置白噪音类型
    fun setNoiseType(type: NoiseType) {
        _activeNoiseType.value = type
        if (_isRunning.value) {
            noiseSynthesizer.start(type)
        } else {
            noiseSynthesizer.stop()
        }
    }

    // 设置严格模式
    fun setStrictMode(enabled: Boolean) {
        _isStrictMode.value = enabled
    }

    // 监控应用退到后台 (严格模式)
    fun onAppBackgrounded() {
        if (!_isStrictMode.value || !_isRunning.value || _isDistractedFailed.value) return
        distractionJob?.cancel()
        distractionJob = viewModelScope.launch(dispatcherProvider.main()) {
            delay(10000) // 10秒宽限期
            failSessionDueToDistraction()
        }
    }

    // 监控应用回到前台 (清除开小差判定计时)
    fun onAppForegrounded() {
        distractionJob?.cancel()
        distractionJob = null
    }

    private fun failSessionDueToDistraction() {
        pauseTimer()
        stopNoise()
        _isDistractedFailed.value = true

        val currentMission = _mission.value ?: return
        val currentAction = _action.value ?: return
        viewModelScope.launch(dispatcherProvider.io()) {
            val actualSeconds = originalDurationSeconds - _timeLeftSeconds.value
            analyticsRepository.recordFocusSession(
                missionId = currentMission.id,
                actionId = currentAction.id,
                durationSeconds = if (actualSeconds > 0) actualSeconds else 1,
                isCompleted = false
            )
        }
    }

    private fun startNoiseIfEnabled() {
        if (_activeNoiseType.value != NoiseType.NONE) {
            noiseSynthesizer.start(_activeNoiseType.value)
        }
    }

    private fun stopNoise() {
        noiseSynthesizer.stop()
    }

    fun completeAction(onCompleted: () -> Unit) {
        pauseTimer()
        val currentMission = _mission.value ?: return
        val currentAction = _action.value ?: return

        viewModelScope.launch(dispatcherProvider.io()) {
            // 标记行动完成
            missionRepository.updateActionCompletion(currentAction.id, true)
            // 写入专注分析日志（弹性计时：实际专注秒数 = 预设时长 - 剩余时长。如果剩余时长为负数，代表溢出，相减即为增加）
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
            // 如果还未被判定为开小差失败，才记录本次退出的正常打断数据
            if (currentMission != null && currentAction != null && !_isDistractedFailed.value) {
                val actualSeconds = originalDurationSeconds - _timeLeftSeconds.value
                if (actualSeconds > 0) {
                    // 即使未计满打断，也记录实际专注时间，保护用户的每一滴努力（No Shame）
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
        distractionJob?.cancel()
        noiseSynthesizer.stop()
    }
}
