package com.safelink.app.ui.screens.diagnosis

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.RiskBadge
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.components.color
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueLight
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.SurfaceWhite

/**
 * 자가 진단 결과 (Figma 20:922) — 점수·위험도·근거는 [DiagnosisViewModel.submit]이 산출한
 * 실제 값을 사용한다(Task 4.9). 산출식은 Design.md 5.1 참고.
 */
@Composable
fun DiagnosisResultScreen(
    navController: NavHostController,
    viewModel: DiagnosisViewModel
) {
    // 체크리스트를 거치지 않고 직접 진입한 경우(기록 재열람 등)는 빈 결과로 처리
    val result = viewModel.result
    val level = result?.level ?: RiskLevel.SAFE
    val score = result?.score ?: 0
    val reasons = result?.reasons.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(
            title = "진단 결과",
            onBack = { navController.popBackStack() },
            useCloseIcon = true
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 긴급 위험도 시 상단 배너
            if (level == RiskLevel.CRITICAL) {
                SafeLinkPrimaryButton(
                    text = "긴급 도움 요청",
                    containerColor = RiskCritical,
                    onClick = { navController.navigate(Screen.Emergency.route) }
                )
            }

            // 점수 카드
            SafeLinkCard {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { score / 100f },
                            modifier = Modifier.size(120.dp),
                            strokeWidth = 10.dp,
                            color = level.color()
                        )
                        Text(
                            text = "${score}점",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = level.color()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    RiskBadge(level = level)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = headlineFor(level),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // 근거 섹션 — 체크한 항목이 있을 때만 표시
            if (reasons.isNotEmpty()) SafeLinkCard {
                Text(text = "왜 이런 결과가 나왔나요?", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Spacer(
                            modifier = Modifier
                                .size(8.dp)
                                .background(level.color(), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = reason, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            // 안심 카드
            SafeLinkCard(containerColor = BrandBlueLight) {
                Text(
                    text = "혼자 해결하지 않아도 됩니다. 아래에서 도움을 받을 수 있는 방법을 확인하세요.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SafeLinkPrimaryButton(text = "대응 가이드 보기", onClick = {
                navController.navigate(Screen.ResponseGuide.createRoute(level))
            })
            TextButton(
                onClick = { navController.navigate(Screen.SupportMatch.route) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("지원 기관 찾기")
            }
            TextButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("홈으로 돌아가기")
            }
        }
    }
}

/** 위험도별 결과 요약 문구 — 단정적 표현 대신 완곡한 어투 사용(Design.md 7장 톤 기준) */
private fun headlineFor(level: RiskLevel): String = when (level) {
    RiskLevel.CRITICAL -> "지금 바로 확인이 필요한 상황으로 보입니다"
    RiskLevel.WARNING -> "주의가 필요한 상황으로 보입니다"
    RiskLevel.CAUTION -> "일부 신호가 확인되어 살펴볼 필요가 있습니다"
    RiskLevel.SAFE -> "특별한 위험 신호는 확인되지 않았습니다"
}
