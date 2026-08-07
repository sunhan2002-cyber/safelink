package com.safelink.app.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.components.color
import com.safelink.app.ui.components.containerColor
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueDark
import com.safelink.app.ui.theme.RiskCritical

/** 대응 가이드 (Figma 20:605) — 위험도별 행동 지침 (Task 6.16) */
@Composable
fun ResponseGuideScreen(navController: NavHostController, riskLevel: RiskLevel) {
    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "대응 가이드", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 긴급 위험도 시 긴급 버튼 상단 고정 (Task 6.16)
            if (riskLevel == RiskLevel.CRITICAL) {
                SafeLinkPrimaryButton(
                    text = "긴급 도움 요청",
                    containerColor = RiskCritical,
                    onClick = { navController.navigate(Screen.Emergency.route) }
                )
            }

            // 히어로 (위험도별 중립 타이틀 — 보이스피싱 단정 배제, 김우영 final_allfile.wy)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(riskLevel.containerColor(), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = riskLevel.color(),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(text = "대응 안내", style = MaterialTheme.typography.titleLarge)
            }

            // 상단 안내
            Text(
                text = "당황하지 말고 아래 순서대로 대응해 보세요.",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "분석 결과는 참고 정보입니다. 상황이 급박하거나 피해가 발생했다면 즉시 도움을 요청하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 지금 해야 할 행동 (위험도별)
            Text(text = "지금 해야 할 행동", style = MaterialTheme.typography.titleMedium)
            SafeLinkCard {
                Text(
                    text = riskLevelAction(riskLevel),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // 추가 확인 사항 (위험도별)
            Text(text = "추가 확인 사항", style = MaterialTheme.typography.titleMedium)
            SafeLinkCard {
                Text(
                    text = riskLevelExtraCheck(riskLevel),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // 긴급 전화 영역 — CRITICAL에서만 노출 (최종 가이드 v1.0)
            if (riskLevel == RiskLevel.CRITICAL) {
                SafeLinkPrimaryButton(
                    text = "112 전화",
                    containerColor = RiskCritical,
                    onClick = { /* TODO: ACTION_DIAL 112 (Task 5.7) */ }
                )
                SafeLinkPrimaryButton(
                    text = "1332 전화 (금융감독원)",
                    containerColor = BrandBlueDark,
                    onClick = { /* TODO: ACTION_DIAL 1332 */ }
                )
            }

            Text(
                text = "분석 결과는 참고 정보이며, 최종 판단은 사용자에게 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SafeLinkPrimaryButton(text = "추천 기관 목록 보기", onClick = {
                navController.navigate(Screen.SupportMatch.route)
            })
            SafeLinkOutlinedButton(text = "메인 화면으로 돌아가기", onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            })
        }
    }
}

/** 위험도별 "지금 해야 할 행동" — 최종 가이드 v1.0 */
private fun riskLevelAction(level: RiskLevel): String = when (level) {
    RiskLevel.SAFE ->
        "의심스러운 요청이 있었는지 다시 확인하고, 출처가 분명하지 않은 링크·파일은 열지 마세요."
    RiskLevel.CAUTION ->
        "상대방의 요청을 바로 따르지 말고, 링크·파일·계좌번호의 출처를 확인하세요."
    RiskLevel.WARNING ->
        "송금·개인정보 제공을 멈추고 대화·링크·계좌 정보를 보관하세요."
    RiskLevel.CRITICAL ->
        "상대방 요구를 중단하고 송금·앱 설치·인증정보 제공·링크 접속을 하지 마세요."
}

/** 위험도별 "추가 확인 사항" — 최종 가이드 v1.0 */
private fun riskLevelExtraCheck(level: RiskLevel): String = when (level) {
    RiskLevel.SAFE -> "필요하면 대화 내용을 다시 분석해 보세요."
    RiskLevel.CAUTION -> "의심이 계속되면 상대가 준 연락처가 아닌 공식 대표번호로 확인하세요."
    RiskLevel.WARNING -> "상대방 연락처 대신 공식 대표번호로 사실을 확인하세요."
    RiskLevel.CRITICAL -> "피해 또는 신변 위협이 급박하면 즉시 112에 신고하세요."
}

@Composable
private fun ReasonRow(text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = "•", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
