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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import com.safelink.app.background.BackgroundDetectionState
import com.safelink.app.ui.components.color
import com.safelink.app.ui.components.containerColor
import com.safelink.app.data.repository.RecordRepository
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.screens.detection.DetectionViewModel
import com.safelink.app.ui.theme.SurfaceWhite
import com.safelink.app.ui.theme.TextPrimary

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
            color = TextPrimary
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

        // 9주차 - Figma "Concept B" B02(Protection Home) 구조로 재배치: 아이콘을 카드
        // 우상단 배지로, 상태 문구 아래에 "최근 위험 신호 N건" 통계 줄 추가. 펄스·색·클릭
        // 로직은 기존 그대로 유지 — 배치만 Figma 기준으로 바꿈.
        SafeLinkCard(
            containerColor = statusLevel.containerColor(),
            onClick = {
                snapshot?.let { navController.navigate(Screen.ResponseGuide.createRoute(statusLevel)) }
            }
        ) {
            // Figma 카드는 화면 세로 비율의 약 29%를 차지하는데 기존 구현은 12%밖에 안 돼
            // 훨씬 작아 보였음(사용자 지적) — 기본 카드 패딩(16dp) 위에 여유 패딩을 더하고
            // 헤드라인 글자 크기도 키워서 카드 전체 크기를 Figma 비율에 맞춤
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.78f)) {
                    Text(
                        text = if (snapshot == null) "실시간 보호 중" else "위험 신호 감지됨",
                        style = MaterialTheme.typography.bodyLarge,
                        color = statusLevel.color()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    // Figma B02: 짧은 상태 단어("안전함") 없이 이 문장 자체가 굵은 큰 헤드라인 —
                    // homeStatusTitle()의 짧은 단어와 이 설명 문장을 굳이 나눠 두 줄로 보여주던 걸
                    // Figma대로 한 줄(헤드라인)로 합침
                    Text(
                        text = snapshot?.let { "최근 감지된 표현이 있어요 · ${it.category}" }
                            ?: homeStatusHeadline(statusLevel),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "최근 위험 신호 ${todayAlertCount}건",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(76.dp)
                ) {
                    // 은은한 숨쉬는 배경 원 (평상시에만 펄스)
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .scale(pulseScale)
                            .background(statusLevel.color().copy(alpha = 0.18f), CircleShape)
                    )
                    // 아이콘 원형 배지 — Figma는 상태색 원 안에 흰색 아이콘(SAFE는 체크),
                    // 카드 대비 원 크기도 더 큼(사용자 확인 후 44dp -> 64dp로 확대)
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(statusLevel.color(), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (statusLevel == RiskLevel.SAFE) Icons.Filled.Check else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = SurfaceWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // 퀵 액션 — Figma는 대화분석/링크검사/자가진단 3개. "링크 검사"는 독립 입력화면이 따로
        // 없고 대화 분석 흐름 안에서 링크가 있으면 자동으로 같이 검사되는 구조라(LinkRiskChecker,
        // DetectionViewModel.checkLinks), 이 타일도 대화 분석과 같은 입력 화면으로 보낸다 —
        // 없는 화면을 새로 만드는 대신, 실제로 동작하는 기능으로 연결(사용자 확인 후 추가).
        // 기존 "대화 분석 시작" 큰 버튼은 제거하고 이 타일이 그 역할을 겸함
        // (Figma에 별도 대형 버튼이 없음 - 퀵액션 3개가 곧 메인 진입점).
        // "백그라운드 감지" 바로가기는 Figma에 없는 항목이라 홈에서는 빠지고, 설정 탭에서는
        // 그대로 이용 가능(기능 삭제 아님, 위치만 이동).
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickActionCard(
                title = "대화 분석",
                icon = Icons.Filled.Search,
                onClick = startAnalysis,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "링크 검사",
                icon = Icons.Filled.Link,
                onClick = startAnalysis,
                modifier = Modifier.weight(1f)
            )
            QuickActionCard(
                title = "자가 진단",
                icon = Icons.Filled.Checklist,
                onClick = { navController.navigate(Screen.Diagnosis.route) },
                modifier = Modifier.weight(1f)
            )
        }

        // 최근 확인 — Figma의 단일 인사이트 카드로 교체. 기존 "추천 지원 서비스"(전문가 상담)
        // 카드는 Figma 홈에 없는 항목이라 빠짐 — 지원 탭에서 동일 항목을 그대로 볼 수 있어
        // 기능이 사라지는 건 아님. 기존 "최근 검사 기록"(최대 3건 리스트)은 최신 1건 요약으로
        // 압축하고, todayScanCount(정밀 검사 수)는 여기 통계 줄로 옮겨서 정보는 그대로 유지.
        Text(text = "최근 확인", style = MaterialTheme.typography.titleMedium)
        SafeLinkCard(onClick = { navController.navigate(Screen.RecordList.route) }) {
            val latest = recentRecords.firstOrNull()
            if (latest == null) {
                Text(text = "위험한 대화가 발견되지 않았어요", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "문자나 링크가 의심되면 바로 확인해 보세요 ›",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = latest.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    RiskBadge(level = latest.riskLevel)
                }
                Text(
                    text = latest.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "오늘 정밀 검사 ${todayScanCount}건 · 전체 기록 보기 ›",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 오늘의 안전 팁 — Figma 신규 섹션. 기존 로직/데이터 없이 고정 문구 + 자가진단으로
        // 가는 지름 버튼만 추가(신규 기능 아님, 기존 자가진단 화면 재사용) — "신규기능은
        // 보류, 구조만" 범위 안에 들어가는 선에서 구성.
        SafeLinkCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lightbulb,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "오늘의 안전 팁", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "낯선 번호가 송금을 재촉하면 대화를 멈추고 직접 확인하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { navController.navigate(Screen.Diagnosis.route) }) {
                    Text("30초 안전 진단")
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp)) // SOS FAB 가림 방지
    }
}

/** 홈 상태 카드 제목 — 백그라운드 감지 위험도별 (없으면 SAFE) */
private fun homeStatusHeadline(level: RiskLevel): String = when (level) {
    RiskLevel.SAFE -> "오늘도 안전하게\n살펴보고 있어요"
    RiskLevel.CAUTION -> "주의가 필요해요"
    RiskLevel.WARNING -> "확인이 필요해요"
    RiskLevel.CRITICAL -> "위험 신호가 있어요"
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
