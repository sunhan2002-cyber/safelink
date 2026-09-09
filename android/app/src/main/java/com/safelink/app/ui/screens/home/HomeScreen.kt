package com.safelink.app.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.RiskBadge
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import com.safelink.app.background.BackgroundDetectionState
import com.safelink.app.ui.components.color
import com.safelink.app.ui.components.containerColor
import com.safelink.app.data.repository.RecordRepository
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.screens.detection.DetectionViewModel

/** 홈 대시보드 (Task 4.14) — 기능 진입점 + 최근 기록 요약 */
@Composable
fun HomeScreen(
    navController: NavHostController,
    detectionViewModel: DetectionViewModel
) {
    // 홈에서 대화 분석에 진입할 때마다 이전 세션(입력·이미지·결과)을 비운다
    val startAnalysis = {
        detectionViewModel.reset()
        navController.navigate(Screen.DetectionInput.route)
    }
    // 백그라운드 감지 상태를 홈 대시보드에 반영 (감지가 있으면 상태 카드가 살아난다)
    val snapshot by BackgroundDetectionState.latestSnapshot.collectAsState()

    // 카운터는 기록 DB에서 오늘 0시 기준으로 센다 — 앱을 껐다 켜도 유지되고 날짜가 바뀌면 0부터 (Task 4.14)
    val context = LocalContext.current
    val recordRepository = remember { RecordRepository(context) }
    val todayAlertCount by recordRepository.observeTodayBackgroundCount().collectAsState(initial = 0)
    val todayScanCount by recordRepository.observeTodayManualCount().collectAsState(initial = 0)
    val recentRecords by recordRepository.observeRecords().collectAsState(initial = emptyList())
    val statusLevel = snapshot?.riskLevel ?: RiskLevel.SAFE
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 홈 화면 좌측 상단 브랜드 워드마크 (사용자 요청 - 아이콘 그래픽 시도 몇 차례 후
        // 이미지 없이 텍스트만 쓰는 걸로 정리)
        Text(
            text = "SafeLink",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )

        // 상태 카드 — 백그라운드 감지가 있으면 그 위험도로, 없으면 안전함. 감지 시 탭하면 대응 가이드로.
        // 디자인 개선: 흰 카드에 아이콘만 떠있던 것 -> 상태색으로 카드 배경을 옅게 물들이고,
        // 아이콘은 원형 배지 안에 넣어서 더 눈에 띄게 함. 감지된 게 없을 때(평상시 감시 중)만
        // 은은하게 숨쉬는 펄스 애니메이션을 줘서 "실시간으로 계속 지켜보고 있다"는 인상을 줌 —
        // 실제 위험이 감지된 상태에서까지 애니메이션이 돌면 오히려 산만하니 그때는 정지.
        val pulse = rememberInfiniteTransition(label = "status-pulse")
        val pulseScale by pulse.animateFloat(
            initialValue = 1f,
            targetValue = if (snapshot == null) 1.18f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse-scale"
        )

        SafeLinkCard(
            containerColor = statusLevel.containerColor(),
            onClick = {
                snapshot?.let { navController.navigate(Screen.ResponseGuide.createRoute(statusLevel)) }
            }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                    // 은은한 숨쉬는 배경 원 (평상시에만 펄스)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .scale(pulseScale)
                            .background(statusLevel.color().copy(alpha = 0.18f), CircleShape)
                    )
                    // 아이콘 원형 배지
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = statusLevel.color(),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
                Column {
                    Text(
                        text = homeStatusTitle(statusLevel),
                        style = MaterialTheme.typography.titleLarge,
                        color = statusLevel.color()
                    )
                    Text(
                        text = snapshot?.let { "최근 감지된 표현이 있어요 · ${it.category}" }
                            ?: "실시간 보호 작동 중",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 활동 요약 — 오늘의 알림 = 백그라운드 감지 건수, 정밀 검사 = 사용자가 직접 실행한 분석 건수
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(label = "오늘의 알림", count = todayAlertCount, modifier = Modifier.weight(1f))
            SummaryTile(label = "정밀 검사", count = todayScanCount, modifier = Modifier.weight(1f))
        }

        // 퀵 액션
        SafeLinkPrimaryButton(
            text = "대화 분석 시작",
            onClick = startAnalysis
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickActionCard(
                title = "자가 진단",
                icon = Icons.Filled.Checklist,
                onClick = { navController.navigate(Screen.Diagnosis.route) },
                modifier = Modifier.weight(1f)
            )
            // "대화 분석"이 위 "대화 분석 시작" 버튼과 완전히 같은 동작(startAnalysis)이라
            // 순수 중복이었음(사용자 지적) - 홈에서 접근 어려웠던 백그라운드 감지 설정
            // 바로가기로 교체
            QuickActionCard(
                title = "백그라운드 감지",
                icon = Icons.Filled.Visibility,
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier.weight(1f)
            )
        }

        // 추천 지원 서비스
        Text(text = "추천 지원 서비스", style = MaterialTheme.typography.titleMedium)
        SafeLinkCard(onClick = { navController.navigate(Screen.SupportMatch.route) }) {
            Text(text = "전문가 1:1 상담", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "24시간 상담 전문가 대기 중",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SafeLinkOutlinedButton(
            text = "지원 서비스 전체보기",
            onClick = { navController.navigate(Screen.SupportMatch.route) }
        )

        // 최근 검사 기록 요약 (Task 4.14) — 기록이 있을 때만 보여준다
        if (recentRecords.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "최근 검사 기록",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { navController.navigate(Screen.RecordList.route) }) {
                    Text("전체 보기")
                }
            }
            recentRecords.take(MAX_RECENT_RECORDS).forEach { record ->
                SafeLinkCard(onClick = { navController.navigate(Screen.RecordList.route) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        RiskBadge(level = record.riskLevel)
                    }
                    Text(
                        text = record.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp)) // SOS FAB 가림 방지
    }
}

/** 홈에 보여줄 최근 기록 개수 — 너무 많으면 홈이 기록 화면과 구분되지 않는다 */
private const val MAX_RECENT_RECORDS = 3

/** 홈 상태 카드 제목 — 백그라운드 감지 위험도별 (없으면 SAFE) */
private fun homeStatusTitle(level: RiskLevel): String = when (level) {
    RiskLevel.SAFE -> "안전함"
    RiskLevel.CAUTION -> "주의가 필요해요"
    RiskLevel.WARNING -> "확인이 필요해요"
    RiskLevel.CRITICAL -> "위험 신호가 있어요"
}

@Composable
private fun SummaryTile(label: String, count: Int, modifier: Modifier = Modifier) {
    SafeLinkCard(modifier = modifier) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SafeLinkCard(modifier = modifier, onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium)
    }
}
