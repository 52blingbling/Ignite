package com.efa.assistant.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun FocusScreen(
    missionId: String,
    actionId: String,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FocusViewModel = hiltViewModel()
) {
    val mission by viewModel.mission.collectAsState()
    val action by viewModel.action.collectAsState()
    val timeLeft by viewModel.timeLeftSeconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val showWaterReminder by viewModel.showWaterReminder.collectAsState()

    LaunchedEffect(missionId, actionId) {
        viewModel.initFocus(missionId, actionId)
    }

    // 物理返回键拦截，走统一的退出弹窗/日志记录逻辑
    BackHandler {
        viewModel.quitFocus(onBackToHome)
    }

    // 全屏无干扰背景色彩，暗色主题下呈现纯净深邃黑以过滤刺眼光线
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 顶部：所属的主任务大标题
                Text(
                    text = mission?.title ?: "执行任务中...",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )

                // 中部：Action 标题和圆形倒计时图表
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = action?.title ?: "行动准备中",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 36.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(240.dp)
                    ) {
                        val durationSeconds = (action?.durationMinutes ?: 15) * 60f
                        val progress = if (durationSeconds > 0) timeLeft.toFloat() / durationSeconds else 0f
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            label = "TimerProgress"
                        )

                        // 倒计时渐变圆环
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(220.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 12.dp,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )

                        // 格式化展示时间，采用等宽字体避免数字跳跃闪烁
                        val minutes = timeLeft / 60
                        val seconds = timeLeft % 60
                        val timeStr = String.format("%02d:%02d", minutes, seconds)
                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 48.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                // 底部：单一主要按钮原则，降低操作判断开销
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 主动作按钮：标记完成（提供最高的多巴胺反馈）
                    Button(
                        onClick = { viewModel.completeAction(onBackToHome) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "我已完成这一步",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 辅助控制：暂停/继续和退出
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (isRunning) viewModel.pauseTimer() else viewModel.resumeTimer()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = if (isRunning) "暂停时钟" else "继续时钟")
                        }

                        OutlinedButton(
                            onClick = { viewModel.quitFocus(onBackToHome) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text(text = "退出专注")
                        }
                    }
                }
            }
        }
    }

    // Hyperfocus 喝水休息提示框 (不能强制中断)
    if (showWaterReminder) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissWaterReminder() },
            title = {
                Text(
                    text = "专注过度提醒 (Hyperfocus)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "你已经专注超过了 90 分钟！Hyperfocus 会让人忘记疲劳，请喝杯水、站起来扭扭脖子活动一下。你的身体比任务更重要。"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissWaterReminder() }
                ) {
                    Text("好的，马上去喝水")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissWaterReminder() }
                ) {
                    Text("我还要继续")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
