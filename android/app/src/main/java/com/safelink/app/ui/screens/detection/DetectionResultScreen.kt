package com.safelink.app.ui.screens.detection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
import com.safelink.app.ui.components.RiskBadge
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.components.color
import com.safelink.app.ui.components.containerColor
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.RiskCritical

/** 분석 결과 (Figma 20:117) — 더미 데이터 UI 뼈대. 실제 데이터는 DetectionViewModel 공유 (Task 6.10) */
@Composable
fun DetectionResultScreen(navController: NavHostController) {
    val level = RiskLevel.CRITICAL // TODO: ViewModel에서 수신
    val score = 85
    val tags = listOf("기관 사칭", "송금 요구")
    val highlights = listOf(
        "\"서울중앙지검 수사관입니다. 본인 명의 계좌가 범죄에 연루되어 확인이 필요합니다.\"",
        "\"비밀 유지를 위해 즉시 안전 계좌로 자금을 송금하신 후 조사를 받으셔야 합니다.\""
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "분석 결과", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 상태 헤더 카드
            SafeLinkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = level.color(),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "주의! 보이스피싱 위험이 감지되었습니다",
                            style = MaterialTheme.typography.titleMedium,
                            color = level.color()
                        )
                        Text(
                            text = "현재 진행 중인 대화에서 사기 패턴이 발견되었습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 위험 점수 + 감지 요소
            SafeLinkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = level.color()
                        )
                        Text(
                            text = "위험 점수",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "감지된 위험 요소", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            tags.forEach { tag ->
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = level.color(),
                                    modifier = Modifier
                                        .background(level.containerColor(), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                        RiskBadge(level = level)
                    }
                }
            }

            // 대화 하이라이트
            Text(text = "대화 하이라이트", style = MaterialTheme.typography.titleMedium)
            highlights.forEach { quote ->
                SafeLinkCard {
                    Row {
                        Spacer(
                            modifier = Modifier
                                .width(4.dp)
                                .height(48.dp)
                                .background(level.color(), RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = quote, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                // TODO: 위험 표현 밑줄 강조 — AnnotatedString + flagged_phrases 인덱스 (Task 6.10)
            }

            Text(
                text = "* 실제 공공기관은 전화로 자금 송금을 요구하지 않습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SafeLinkPrimaryButton(
                text = "대응 가이드 보기",
                containerColor = RiskCritical,
                onClick = { navController.navigate(Screen.ResponseGuide.createRoute(level)) }
            )
            SafeLinkOutlinedButton(
                text = "전문가 상담 연결",
                onClick = { navController.navigate(Screen.SupportMatch.route) }
            )
        }
    }
}
