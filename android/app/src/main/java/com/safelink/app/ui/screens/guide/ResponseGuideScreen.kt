package com.safelink.app.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.compose.ui.platform.LocalContext
import com.safelink.app.background.BackgroundDetectionState
import com.safelink.app.background.BackgroundDetectionSnapshot
import com.safelink.app.ui.screens.detection.DetectionViewModel
import com.safelink.app.util.IntentActions
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

@Composable
fun ResponseGuideScreen(
    navController: NavHostController,
    riskLevel: RiskLevel,
    detectionViewModel: DetectionViewModel
) {
    val context = LocalContext.current
    val backgroundSnapshot by BackgroundDetectionState.latestSnapshot.collectAsState()
    val matchedBackgroundSnapshot = backgroundSnapshot?.takeIf { it.riskLevel == riskLevel }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "대응 가이드", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (riskLevel == RiskLevel.CRITICAL) {
                SafeLinkPrimaryButton(
                    text = "긴급 도움 요청",
                    containerColor = RiskCritical,
                    onClick = { navController.navigate(Screen.Emergency.route) }
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .align(Alignment.CenterHorizontally)
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

            Text(
                text = "당황하지 말고 아래 순서대로 대응해 보세요.",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "분석 결과는 참고 정보입니다. 상황이 급박하거나 피해가 발생했다면 즉시 도움을 요청하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (matchedBackgroundSnapshot != null && matchedBackgroundSnapshot.detectedPhrases.isNotEmpty()) {
                Text(text = "감지된 표현", style = MaterialTheme.typography.titleMedium)
                SafeLinkCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "백그라운드 감지에서 아래 표현이 확인되었습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        matchedBackgroundSnapshot.detectedPhrases.forEach { phrase ->
                            Text(
                                text = "\"$phrase\"",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        riskLevel.containerColor(),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                        Text(
                            text = "감지 앱: ${sourceAppLabel(matchedBackgroundSnapshot)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // 백그라운드 감지 → 전체 분석 결과(점수·유형·근거·기관)로 이어보기 (결과 화면 데이터 흐름)
                SafeLinkOutlinedButton(
                    text = "분석 결과 자세히 보기",
                    onClick = {
                        if (detectionViewModel.loadBackgroundResult()) {
                            navController.navigate(Screen.DetectionResult.route)
                        }
                    }
                )
            }

            Text(text = "지금 해야 할 행동", style = MaterialTheme.typography.titleMedium)
            SafeLinkCard {
                Text(
                    text = riskLevelAction(riskLevel),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Text(text = "추가 확인 사항", style = MaterialTheme.typography.titleMedium)
            SafeLinkCard {
                Text(
                    text = riskLevelExtraCheck(riskLevel),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (riskLevel == RiskLevel.CRITICAL) {
                SafeLinkPrimaryButton(
                    text = "112 전화",
                    containerColor = RiskCritical,
                    onClick = { IntentActions.dial(context, "112") }
                )
                SafeLinkPrimaryButton(
                    text = "1332 전화",
                    containerColor = BrandBlueDark,
                    onClick = { IntentActions.dial(context, "1332") }
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
            SafeLinkPrimaryButton(
                text = "추천 기관 목록 보기",
                onClick = { navController.navigate(Screen.SupportMatch.route) }
            )
            SafeLinkOutlinedButton(
                text = "메인 화면으로 돌아가기",
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}

private fun riskLevelAction(level: RiskLevel): String = when (level) {
    RiskLevel.SAFE ->
        "전달받은 요청이 맞는지 다시 확인하고, 출처가 분명하지 않은 링크나 파일은 열지 마세요."
    RiskLevel.CAUTION ->
        "상대방의 요청을 바로 따르지 말고, 링크·파일·계좌번호의 출처를 먼저 확인하세요."
    RiskLevel.WARNING ->
        "송금·개인정보 제공을 멈추고 대화, 링크, 계좌 정보를 보관하세요."
    RiskLevel.CRITICAL ->
        "송금과 정보 제공을 즉시 중단하고 링크 접속이나 앱 설치를 더 진행하지 마세요."
}

private fun riskLevelExtraCheck(level: RiskLevel): String = when (level) {
    RiskLevel.SAFE -> "필요하면 같은 내용을 다시 분석해 보세요."
    RiskLevel.CAUTION -> "상대가 준 연락처 대신 공식 대표번호로 사실을 확인하세요."
    RiskLevel.WARNING -> "상대방 연락처 대신 공식 대표번호로 사실을 확인하세요."
    RiskLevel.CRITICAL -> "피해가 발생했거나 급박하면 즉시 112 또는 관련 기관에 도움을 요청하세요."
}

private fun sourceAppLabel(snapshot: BackgroundDetectionSnapshot): String = when (snapshot.sourceApp) {
    "com.kakao.talk" -> "카카오톡"
    else -> snapshot.sourceApp
}
