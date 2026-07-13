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
import com.safelink.app.ui.components.color
import com.safelink.app.ui.navigation.Screen

/** 홈 대시보드 (Task 4.14) — 기능 진입점 + 최근 기록 요약 */
@Composable
fun HomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 상태 카드
        SafeLinkCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = RiskLevel.SAFE.color(),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.size(16.dp))
                Column {
                    Text(
                        text = "안전함",
                        style = MaterialTheme.typography.titleLarge,
                        color = RiskLevel.SAFE.color()
                    )
                    Text(
                        text = "실시간 보호 작동 중",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 활동 요약
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(label = "오늘의 알림", count = 0, modifier = Modifier.weight(1f))
            SummaryTile(label = "정밀 검사", count = 0, modifier = Modifier.weight(1f))
        }

        // 퀵 액션
        SafeLinkPrimaryButton(
            text = "스크린샷 분석",
            onClick = { navController.navigate(Screen.DetectionInput.route) }
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
                onClick = { navController.navigate(Screen.DetectionInput.route) },
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
