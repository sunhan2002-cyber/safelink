package com.safelink.app.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.safelink.app.background.BackgroundDetectionState
import com.safelink.app.ui.components.color
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
    // 백그라운드 감지 상태를 홈 대시보드에 반영 (감지가 있으면 상태 카드/알림 수가 살아난다)
    val snapshot by BackgroundDetectionState.latestSnapshot.collectAsState()
    val detectionCount by BackgroundDetectionState.detectionCount.collectAsState()
    val statusLevel = snapshot?.riskLevel ?: RiskLevel.SAFE
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 상태 카드 — 백그라운드 감지가 있으면 그 위험도로, 없으면 안전함. 감지 시 탭하면 대응 가이드로.
        SafeLinkCard(onClick = {
            snapshot?.let { navController.navigate(Screen.ResponseGuide.createRoute(statusLevel)) }
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = statusLevel.color(),
                    modifier = Modifier.size(48.dp)
                )
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

        // 활동 요약 — 오늘의 알림 = 백그라운드 감지 누적 횟수
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(label = "오늘의 알림", count = detectionCount, modifier = Modifier.weight(1f))
            SummaryTile(label = "정밀 검사", count = 0, modifier = Modifier.weight(1f))
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
            QuickActionCard(
                title = "대화 분석",
                icon = Icons.Filled.ImageSearch,
                onClick = startAnalysis,
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

        // TODO: 최근 기록 2~3개 요약 (Room DB 연동 후, Task 4.14)
        Spacer(modifier = Modifier.height(72.dp)) // SOS FAB 가림 방지
    }
}

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
