package com.safelink.app.ui.screens.guide

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.background.BackgroundDetectionState
import com.safelink.app.background.BackgroundDetectionSnapshot
import com.safelink.app.ui.screens.detection.DetectionViewModel
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.RiskBadge
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.components.color
import com.safelink.app.ui.components.containerColor
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueDark
import com.safelink.app.ui.theme.BrandBlueLight
import com.safelink.app.ui.theme.RiskCritical

@Composable
fun ResponseGuideScreen(
    navController: NavHostController,
    riskLevel: RiskLevel,
    detectionViewModel: DetectionViewModel
) {
    val backgroundSnapshot by BackgroundDetectionState.latestSnapshot.collectAsState()
    val matchedBackgroundSnapshot = backgroundSnapshot?.takeIf { it.riskLevel == riskLevel }
    val matchedRiskTypes = remember(detectionViewModel.result) {
        detectionViewModel.result?.recommendedInstitutions?.map { it.matchedRiskType }?.distinct().orEmpty()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "대응 가이드", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                Spacer(modifier = Modifier.size(6.dp))
                RiskBadge(level = riskLevel)
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
                            navController.navigate(Screen.DetectionResult.createRoute())
                        }
                    }
                )
            }

            Text(text = "대응 순서", style = MaterialTheme.typography.titleMedium)
            GuideStepList(
                steps = listOf(
                    "지금 해야 할 행동" to riskLevelAction(riskLevel),
                    "추가 확인 사항" to riskLevelExtraCheck(riskLevel)
                )
            )

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
            if (riskLevel == RiskLevel.CRITICAL) {
                // 긴급 단계 버튼 위계 정리 — 이 화면에서 112/1332를 각각 누르게 하는 대신
                // "긴급 도움 요청" 하나로 모아 전용 화면(EmergencyScreen, 88dp 대형 전화 버튼)으로
                // 보낸다. 화면당 주 버튼 1개 원칙 + 그 화면이 이미 112 연결을 제공하므로 중복 아님.
                SafeLinkPrimaryButton(
                    text = "긴급 도움 요청",
                    containerColor = RiskCritical,
                    onClick = { navController.navigate(Screen.Emergency.route) }
                )
                GuideSecondaryLink(
                    text = "추천 기관 목록 보기",
                    onClick = { navController.navigate(Screen.SupportMatch.createRoute(matchedRiskTypes)) }
                )
            } else {
                SafeLinkPrimaryButton(
                    text = "추천 기관 목록 보기",
                    onClick = { navController.navigate(Screen.SupportMatch.createRoute(matchedRiskTypes)) }
                )
            }
            GuideSecondaryLink(
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

@Composable
private fun GuideStepList(steps: List<Pair<String, String>>) {
    SafeLinkCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            steps.forEachIndexed { index, (title, description) ->
                if (index > 0) HorizontalDivider()
                Row {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(BrandBlueLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlueDark
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = title, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = description, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideSecondaryLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text)
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

/**
 * 감지된 앱 이름. 목록에 없으면 패키지명 대신 "다른 앱"으로 둔다 —
 * 예전에는 카카오톡 외에는 "com.instagram.android" 같은 패키지명이 사용자 화면에 그대로 보였다.
 * 감지 대상 목록은 MessageDetectionService.MONITORED_PACKAGES 와 같다.
 */
private fun sourceAppLabel(snapshot: BackgroundDetectionSnapshot): String = when (snapshot.sourceApp) {
    "com.kakao.talk" -> "카카오톡"
    "com.samsung.android.messaging" -> "삼성 메시지"
    "com.google.android.apps.messaging" -> "메시지"
    "com.instagram.android" -> "인스타그램"
    "com.discord" -> "디스코드"
    "org.telegram.messenger" -> "텔레그램"
    "com.nhn.android.band" -> "네이버 밴드"
    "jp.naver.line.android" -> "라인"
    "com.facebook.orca" -> "페이스북 메신저"
    "com.tencent.mm" -> "위챗"
    else -> "다른 앱"
}
