package com.efa.assistant.feature.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.efa.assistant.core.common.UiState
import com.efa.assistant.core.model.Mission
import kotlin.math.roundToInt

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.metricsState.collectAsState()
    val aiInsightsState by viewModel.aiInsightsState.collectAsState()

    // 页面进入时触发一次完成率更新
    LaunchedEffect(Unit) {
        viewModel.refreshCompletionRate()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val metrics = state) {
            is UiState.Loading -> {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            is UiState.Error -> {
                Text(
                    text = "统计数据读取失败: ${metrics.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            is UiState.Success -> {
                val data = metrics.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "你的成长日志",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 1. 成就反馈卡片：只夸奖“开始”，杜绝挫败感
                    StreakAchievementCard(streak = data.currentStreak)

                    // 2. 本周专注时间趋势线（Canvas 手绘折线图）
                    TrendChartCard(dailyStats = data.recentDailyStats)

                    // 3. 行为指标仪表盘
                    MetricsDashboard(data = data)

                    // 4. Procrastinated Tasks 分析
                    ProcrastinationAnalysisList(missions = data.mostProcrastinated)

                    // 5. AI 深度复盘模块
                    AIInsightsCard(
                        aiInsightsState = aiInsightsState,
                        onGenerateClick = { viewModel.generateWeeklyInsights() }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

/**
 * 成就反馈模块：完全贯彻“No Shame, No Punishment”
 */
@Composable
fun StreakAchievementCard(streak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            ) {
                Text(
                    text = "🔥",
                    fontSize = 32.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = if (streak > 0) "已连续 $streak 天启动行动！" else "开启第一步吧！",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "不以任务完全完成为压力，哪怕只专注了1分钟，开始行动就是胜利。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * 纯 Jetpack Compose Canvas 绘制的趋势分析图
 */
@Composable
fun TrendChartCard(dailyStats: List<DailyStats>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "近 7 天专注时长趋势 (分钟)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            val maxMinutes = (dailyStats.maxOfOrNull { it.focusMinutes } ?: 15).coerceAtLeast(15)
            val primaryColor = MaterialTheme.colorScheme.primary
            val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val width = size.width
                val height = size.height
                val spacing = width / 6f

                val points = dailyStats.mapIndexed { index, stats ->
                    Offset(
                        x = index * spacing,
                        y = height - (stats.focusMinutes.toFloat() / maxMinutes * (height - 30f)) - 15f
                    )
                }

                // 绘制渐变填充背景
                val pathFill = Path().apply {
                    moveTo(0f, height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(width, height)
                    close()
                }
                drawPath(
                    path = pathFill,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.2f), Color.Transparent),
                        startY = 0f,
                        endY = height
                    )
                )

                // 绘制折线
                val pathLine = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = pathLine,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx())
                )

                // 绘制数据点与数值标签
                points.forEachIndexed { index, point ->
                    drawCircle(
                        color = primaryColor,
                        radius = 5.dp.toPx(),
                        center = point
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = point
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X 轴星期标签
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dailyStats.forEach {
                    Text(
                        text = it.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * 行为数据仪表盘
 */
@Composable
fun MetricsDashboard(data: BehaviorMetrics) {
    val items = listOf(
        Pair("🚀 总启动次数", "${data.totalStartCount} 次"),
        Pair("🎯 平均完成率", "${(data.completionRate * 100).roundToInt()}%"),
        Pair("⏳ 单次最长专注", "${data.longestFocusMinutes} 分钟"),
        Pair("⏰ 最佳状态时段", if (data.bestWorkingHour != null) "${data.bestWorkingHour}:00" else "暂无数据"),
        Pair("📅 最佳专注星期", if (data.bestWorkingDayOfWeek != null) "星期${mapDayOfWeek(data.bestWorkingDayOfWeek)}" else "暂无数据")
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "本地习惯透视",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            items.forEachIndexed { index, pair ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = pair.first,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pair.second,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (index < items.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                }
            }
        }
    }
}

/**
 * 拖延任务排行：标记拖延次数多的任务，未来第五阶段方便 AI 提供针对性调整。
 */
@Composable
fun ProcrastinationAnalysisList(missions: List<Mission>) {
    if (missions.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "拖延雷达（心智阻力分析）",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "这些是你拖延或修改次数较多的任务。这代表它们可能拆分得还不够细，或者给你带来了情绪压力。下一阶段，你可以让 AI 教练对这些任务执行更深度的自动拆分。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            missions.forEachIndexed { index, mission ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mission.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "推迟了 ${mission.deferredCount} 次",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                if (index < missions.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
                }
            }
        }
    }
}

private fun mapDayOfWeek(day: Int): String {
    return when (day) {
        1 -> "一"
        2 -> "二"
        3 -> "三"
        4 -> "四"
        5 -> "五"
        6 -> "六"
        7 -> "日"
        else -> ""
    }
}

@Composable
fun AIInsightsCard(
    aiInsightsState: UiState<String>,
    onGenerateClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onGenerateClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "✨ AI 深度复盘",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (aiInsightsState) {
                is UiState.Loading -> {
                    Text(
                        text = "点击上方按钮生成私人周报",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is UiState.Error -> {
                    Text(
                        text = "生成失败，请检查是否在设置中配置了有效的 API Key。\n错误信息：${aiInsightsState.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                is UiState.Success -> {
                    Text(
                        text = aiInsightsState.data,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
