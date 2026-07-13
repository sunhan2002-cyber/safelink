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
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.components.color
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueLight
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.SurfaceWhite

/** 자가 진단 결과 (Figma 20:922) — 더미 데이터 UI 뼈대 (Task 4.8) */
@Composable
fun DiagnosisResultScreen(navController: NavHostController) {
    val level = RiskLevel.WARNING // TODO: DiagnosisViewModel에서 산출 결과 수신 (Task 4.9)
    val score = 55
    val reasons = listOf(
        "급하게 돈을 보내라는 요구가 있었어요",
        "수사기관을 사칭하는 연락이 있었어요",
        "다른 사람에게 말하지 말라는 요구가 있었어요"
    )

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
                        text = "주의가 필요한 상황으로 보입니다",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // 근거 섹션
            SafeLinkCard {
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
            SafeLinkOutlinedButton(text = "지원 기관 찾기", onClick = {
                navController.navigate(Screen.SupportMatch.route)
            })
            TextButton(
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("홈으로 돌아가기")
            }
        }
    }
}
