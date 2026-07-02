package com.efa.assistant.feature.settings

import androidx.annotation.Keep
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.efa.assistant.core.common.DispatcherProvider
import com.efa.assistant.core.database.dao.AnalyticsDao
import com.efa.assistant.core.database.dao.MissionDao
import com.efa.assistant.core.database.model.ActionEntity
import com.efa.assistant.core.database.model.FocusRecordEntity
import com.efa.assistant.core.database.model.MissionEntity
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

@Keep
data class BackupPayload(
    val missions: List<MissionBackup>,
    val focusRecords: List<FocusRecordEntity>
)

@Keep
data class MissionBackup(
    val mission: MissionEntity,
    val actions: List<ActionEntity>
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val missionDao: MissionDao,
    private val analyticsDao: AnalyticsDao,
    @Named("encrypted_prefs") private val sharedPrefs: SharedPreferences,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _aiProvider = MutableStateFlow("Gemini")
    val aiProvider: StateFlow<String> = _aiProvider.asStateFlow()

    private val _themeMode = MutableStateFlow("DARK")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _eventFlow = MutableStateFlow<String?>(null)
    val eventFlow: StateFlow<String?> = _eventFlow.asStateFlow()

    init {
        _apiKey.value = sharedPrefs.getString("api_key", "") ?: ""
        _aiProvider.value = sharedPrefs.getString("ai_provider", "Gemini") ?: "Gemini"
        _themeMode.value = sharedPrefs.getString("theme_mode", "DARK") ?: "DARK"
    }

    fun saveApiKey(key: String) {
        _apiKey.value = key
        sharedPrefs.edit().putString("api_key", key).apply()
    }

    fun saveAiProvider(provider: String) {
        _aiProvider.value = provider
        sharedPrefs.edit().putString("ai_provider", provider).apply()
    }

    fun saveThemeMode(mode: String) {
        _themeMode.value = mode
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    fun clearEvent() {
        _eventFlow.value = null
    }

    /**
     * 导出本地数据
     */
    fun exportData(format: String, onComplete: (String) -> Unit) {
        viewModelScope.launch(dispatcherProvider.io()) {
            val missionsList = missionDao.getAllMissionsFlow().first()
            val focusList = analyticsDao.getAllFocusRecordsFlow().first()

            val output = when (format.uppercase()) {
                "JSON" -> {
                    val backupPayload = BackupPayload(
                        missions = missionsList.map { MissionBackup(it.mission, it.actions) },
                        focusRecords = focusList
                    )
                    Gson().toJson(backupPayload)
                }
                "CSV" -> {
                    val csv = StringBuilder()
                    csv.append("id,missionId,actionId,startTime,durationSeconds,isCompleted,dayOfWeek,hourOfDay\n")
                    focusList.forEach {
                        csv.append("${it.id},${it.missionId},${it.actionId},${it.startTime},${it.durationSeconds},${it.isCompleted},${it.dayOfWeek},${it.hourOfDay}\n")
                    }
                    csv.toString()
                }
                "MARKDOWN" -> {
                    val md = StringBuilder()
                    md.append("# 启程 专注成长日志\n\n")
                    md.append("导出的历史任务统计摘要如下：\n\n")
                    missionsList.forEach { item ->
                        val status = if (item.mission.isCompleted) "✅ 已完成" else "⏳ 进行中"
                        md.append("## 任务：${item.mission.title} ($status)\n")
                        md.append("- 预估时长：${item.mission.durationMinutes} 分钟\n")
                        md.append("- 延期/拖延次数：${item.mission.deferredCount} 次\n")
                        md.append("- 步骤清单：\n")
                        item.actions.sortedBy { it.sequenceOrder }.forEach { act ->
                            val actStatus = if (act.isCompleted) "[x]" else "[ ]"
                            md.append("  - $actStatus ${act.title} (${act.durationMinutes}分钟)\n")
                        }
                        md.append("\n")
                    }
                    md.toString()
                }
                else -> ""
            }

            withContext(dispatcherProvider.main()) {
                onComplete(output)
            }
        }
    }

    /**
     * 恢复 JSON 备份数据
     */
    fun importBackup(jsonStr: String) {
        viewModelScope.launch(dispatcherProvider.io()) {
            try {
                val payload = Gson().fromJson(jsonStr, BackupPayload::class.java)
                payload.missions.forEach { backup ->
                    missionDao.saveMissionWithActions(backup.mission, backup.actions)
                }
                payload.focusRecords.forEach { record ->
                    analyticsDao.insertFocusRecord(record)
                }
                _eventFlow.value = "数据恢复成功！已导入 ${payload.missions.size} 个任务和 ${payload.focusRecords.size} 条专注日志。"
            } catch (e: Exception) {
                _eventFlow.value = "导入失败：JSON 格式不合法"
            }
        }
    }
}
