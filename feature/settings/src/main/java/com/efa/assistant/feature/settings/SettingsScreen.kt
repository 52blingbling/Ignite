package com.efa.assistant.feature.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efa.assistant.core.designsystem.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onThemeChanged: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    val apiKey by viewModel.apiKey.collectAsState()
    val aiProvider by viewModel.aiProvider.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val eventMsg by viewModel.eventFlow.collectAsState()

    var showRestoreField by remember { mutableStateOf(false) }
    var restoreJson by remember { mutableStateOf("") }
    var obscureKey by remember { mutableStateOf(true) }

    LaunchedEffect(eventMsg) {
        if (eventMsg != null) {
            Toast.makeText(context, eventMsg, Toast.LENGTH_LONG).show()
            viewModel.clearEvent()
            restoreJson = ""
            showRestoreField = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "系统设置",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        // 1. AI 引擎配置
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AI 智能教练引擎",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "选择大语言模型提供商，切换时无需修改本地业务逻辑。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val providers = listOf("DeepSeek", "OpenAI", "Claude", "Gemini", "Qwen", "Ollama", "Local LLM")
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = aiProvider,
                        onValueChange = {},
                        label = { Text("AI Provider") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        providers.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider) },
                                onClick = {
                                    viewModel.saveAiProvider(provider)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { viewModel.saveApiKey(it) },
                    label = { Text("API Key (Encrypted)") },
                    placeholder = { Text("sk-...") },
                    visualTransformation = if (obscureKey) PasswordVisualTransformation() else VisualTransformation.None,
                    trailingIcon = {
                        Text(
                            text = if (obscureKey) "显示" else "隐藏",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clickable { obscureKey = !obscureKey }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // 2. 个性化主题设置：支持高对比度 AMOLED
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "主题风格 (支持 AMOLED 纯黑)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val modes = listOf("LIGHT", "DARK", "AMOLED")
                modes.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.saveThemeMode(mode)
                                when (mode) {
                                    "LIGHT" -> onThemeChanged(ThemeMode.LIGHT)
                                    "DARK" -> onThemeChanged(ThemeMode.DARK)
                                    "AMOLED" -> onThemeChanged(ThemeMode.AMOLED)
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = themeMode == mode,
                            onClick = {
                                viewModel.saveThemeMode(mode)
                                when (mode) {
                                    "LIGHT" -> onThemeChanged(ThemeMode.LIGHT)
                                    "DARK" -> onThemeChanged(ThemeMode.DARK)
                                    "AMOLED" -> onThemeChanged(ThemeMode.AMOLED)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (mode) {
                                "LIGHT" -> "明亮模式 (温暖砂底色，护眼防刺眼)"
                                "DARK" -> "暗黑模式 (标准 Slate 暗色，舒适护眼)"
                                "AMOLED" -> "AMOLED模式 (纯黑背景，极地省电高对比度)"
                                else -> mode
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // 3. 数据备份与恢复
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "本地数据导出与恢复",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "所有数据均保存在本地。您可以将数据导出并复制至剪贴板，用于跨设备同步或数据备份。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 导出按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val formats = listOf("JSON", "CSV", "Markdown")
                    formats.forEach { fmt ->
                        OutlinedButton(
                            onClick = {
                                viewModel.exportData(fmt) { content ->
                                    val clip = ClipData.newPlainText("EFA_Backup_${fmt}", content)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "${fmt} 数据已复制到剪贴板，您可以粘贴保存！", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = fmt, fontSize = 12.sp)
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                // 恢复入口
                if (!showRestoreField) {
                    OutlinedButton(
                        onClick = { showRestoreField = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "从剪贴板文本恢复备份")
                    }
                }

                AnimatedVisibility(visible = showRestoreField) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = restoreJson,
                            onValueChange = { restoreJson = it },
                            label = { Text("粘贴导出的 JSON 备份数据") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (restoreJson.isNotBlank()) {
                                        viewModel.importBackup(restoreJson)
                                    }
                                },
                                enabled = restoreJson.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("立即导入")
                            }
                            OutlinedButton(
                                onClick = {
                                    showRestoreField = false
                                    restoreJson = ""
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("取消")
                            }
                        }
                    }
                }
            }
        }
    }
}
