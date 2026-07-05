package com.efa.assistant.feature.focus

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.efa.assistant.feature.focus.audio.NoiseType

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
    val activeNoiseType by viewModel.activeNoiseType.collectAsState()
    val isStrictMode by viewModel.isStrictMode.collectAsState()
    val isDistractedFailed by viewModel.isDistractedFailed.collectAsState()

    LaunchedEffect(missionId, actionId) {
        viewModel.initFocus(missionId, actionId)
    }

    // 监控应用前后台切换 (用于严格专注模式)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onAppBackgrounded()
                Lifecycle.Event.ON_START -> viewModel.onAppForegrounded()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 物理返回键拦截，走统一的退出弹窗/日志记录逻辑
    BackHandler {
        viewModel.quitFocus(onBackToHome)
    }

    // 呼吸动画定义
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )
    val breathAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathAlpha"
    )

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
                        // 弹性心流状态样式映射
                        val isOverrun = timeLeft < 0
                        val timerColor = if (isOverrun) Color(0xFFFFB300) else MaterialTheme.colorScheme.primary

                        // 呼吸动画底圈
                        if (isRunning) {
                            Box(
                                modifier = Modifier
                                    .size(200.dp * breathScale)
                                    .clip(CircleShape)
                                    .background(timerColor.copy(alpha = breathAlpha))
                            )
                        }

                        val durationSeconds = (action?.durationMinutes ?: 15) * 60f
                        // 溢出后进度条拉满 (1.0f)
                        val progress = if (isOverrun) 1.0f else (if (durationSeconds > 0) timeLeft.toFloat() / durationSeconds else 0f)
                        val animatedProgress by animateFloatAsState(
                            targetValue = progress,
                            label = "TimerProgress"
                        )

                        // 倒计时渐变圆环
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.size(220.dp),
                            color = timerColor,
                            strokeWidth = 12.dp,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        )

                        // 格式化展示时间，采用等宽字体避免数字跳跃闪烁
                        val absTimeLeft = if (isOverrun) -timeLeft else timeLeft
                        val minutes = absTimeLeft / 60
                        val seconds = absTimeLeft % 60
                        val timeStr = String.format("%s%02d:%02d", if (isOverrun) "+" else "", minutes, seconds)
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeStr,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 44.sp
                                ),
                                color = if (isOverrun) timerColor else MaterialTheme.colorScheme.onBackground
                            )
                            if (isOverrun) {
                                Text(
                                    text = "✨ 心流无限叠加",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = timerColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 底部：单一主要按钮原则，降低操作判断开销
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 严格模式开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔒 严格专注模式",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = isStrictMode,
                            onCheckedChange = { viewModel.setStrictMode(it) },
                            modifier = Modifier.scale(0.8f) // 缩放小一点不夺目
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 心流氛围音控制面板
                    Text(
                        text = "🎵 心流氛围音",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair(NoiseType.NONE, "静音"),
                            Pair(NoiseType.RAIN, "雨声"),
                            Pair(NoiseType.BROWN, "褐噪"),
                            Pair(NoiseType.WHITE, "白噪")
                        ).forEach { (type, label) ->
                            val selected = activeNoiseType == type
                            OutlinedButton(
                                onClick = { viewModel.setNoiseType(type) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground
                                )
                            ) {
                                Text(text = label, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

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

    // 开小差失败弹窗 (严格模式下切入后台过久)
    if (isDistractedFailed) {
        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    text = "⚠️ 专注失败 (严格模式)",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "你刚才切出了应用或关闭了屏幕。在严格模式下，开小差超过 10 秒即判定当前专注失败。\n\n没关系，之前已付出的努力已经被保存 (No Shame)，下次继续加油！"
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.quitFocus(onBackToHome) }
                ) {
                    Text("返回首页")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
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
