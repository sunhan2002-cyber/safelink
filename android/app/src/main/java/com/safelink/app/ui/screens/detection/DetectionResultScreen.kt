package com.safelink.app.ui.screens.detection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.DetectionResultDummyData
import com.safelink.app.data.model.RecommendedInstitutionUi
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
import com.safelink.app.ui.theme.SafeLinkTheme

/**
 * 분석 결과 (Figma 20:117)
 *
 * 데이터 구조: com.safelink.app.data.model.DetectionResult (필드 정의 근거는 해당 파일 KDoc 참고)
 * 더미 데이터: DetectionResultDummyData (data/위험 문장 테스트.json 검증 케이스 기반, 점수 실제 계산 로직과 일치)
 *
 * TODO(Task 6.10): 지금은 DetectionResultDummyData.vpCritical 고정 사용 중. ViewModel 연동 시
 * DetectionViewModel의 StateFlow<DetectionResult>를 collectAsState()로 구독하도록 교체.
 * TODO: 원문 내 위험 표현 밑줄/배경 강조는 MatchedKeyword.startIndex/endIndex를 이용해
 * AnnotatedString으로 구현 필요 (지금은 매칭된 구간을 별도 텍스트로만 나열).
 *
 * 네비게이션에 의존하지 않는 렌더링 로직은 DetectionResultContent로 분리했음
 * (Preview에서 NavHostController 없이 4가지 위험도 상태를 바로 확인 가능 — 하단 Preview 함수 참고).
 */
@Composable
fun DetectionResultScreen(
    navController: NavHostController,
    viewModel: DetectionViewModel
) {
    // 실제 데이터 흐름 (김선한_02 문서): 공유 ViewModel의 분석 결과를 사용.
    // 기록 화면 재열람 등 분석 없이 직접 진입한 경우에만 더미로 대체 (TODO: Room 연동 시 recordId 조회로 교체)
    val result: DetectionResult = viewModel.result ?: DetectionResultDummyData.vpCritical

    DetectionResultContent(
        result = result,
        onBack = { navController.popBackStack() },
        onGuideClick = { navController.navigate(Screen.ResponseGuide.createRoute(result.riskLevel)) },
        onSupportClick = { navController.navigate(Screen.SupportMatch.route) },
        onEmergencyClick = { navController.navigate(Screen.Emergency.route) },
        onReanalyzeClick = {
            // 다시 분석: 입력 화면으로 돌아가기 (현재 결과 화면은 스택에서 제거)
            navController.navigate(Screen.DetectionInput.route) {
                popUpTo(Screen.DetectionInput.route) { inclusive = true }
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetectionResultContent(
    result: DetectionResult,
    onBack: () -> Unit,
    onGuideClick: () -> Unit,
    onSupportClick: () -> Unit,
    onEmergencyClick: () -> Unit = {},
    onReanalyzeClick: () -> Unit = {}
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "분석 결과", onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (result.isSafeAndEmpty) {
                SafeEmptyCard()
            } else {
                // 상태 헤더 카드
                SafeLinkCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = result.riskLevel.color(),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            // 위험 유형과 위험도를 분리 (개발용어·단정 표현 배제 — 김우영 final_allfile.wy)
                            Text(
                                text = "${result.category} 관련 위험 신호가 확인되었습니다",
                                style = MaterialTheme.typography.titleMedium,
                                color = result.riskLevel.color()
                            )
                            Text(
                                text = riskLevelSummary(result.riskLevel),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // 위험 점수 + 감지 요소 (태그 = matchedKeywords의 subcategoryName, 중복 제거)
                SafeLinkCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${result.score}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = result.riskLevel.color()
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
                            // 태그 3개 이상 시 줄바꿈되도록 FlowRow 사용 (Row는 화면 폭 초과 시 세로로 찌그러짐)
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                result.matchedKeywords
                                    .map { it.subcategoryName }
                                    .distinct()
                                    .forEach { tag ->
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = result.riskLevel.color(),
                                            modifier = Modifier
                                                .background(result.riskLevel.containerColor(), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                            }
                            RiskBadge(level = result.riskLevel)
                        }
                    }
                }

                // 분석 근거 (matchedText 기준 나열 — 인덱스 기반 강조는 TODO)
                Text(text = "분석 근거", style = MaterialTheme.typography.titleMedium)
                result.matchedKeywords.forEach { kw ->
                    SafeLinkCard {
                        Row {
                            Spacer(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(48.dp)
                                    .background(result.riskLevel.color(), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "\"${kw.matchedText}\"", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = "${kw.subcategoryName} · ${kw.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 추천 기관 — 즉시 대응기관/추가 지원기관 2단 표시
                // (김우영 문구 가이드 v4.2 4장 + RecommendedInstitutionUi.group KDoc 기준)
                if (result.recommendedInstitutions.isNotEmpty()) {
                    val (immediate, additional) = result.recommendedInstitutions
                        .sortedBy { it.rank }
                        .partition { it.group == "긴급대응" }

                    if (immediate.isNotEmpty()) {
                        Text(text = "즉시 대응기관", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "현재 위험 유형에 가장 적합한 기관입니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        immediate.forEach { inst -> RecommendedInstitutionCard(inst) }
                    }
                    if (additional.isNotEmpty()) {
                        Text(text = "추가 지원기관", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "피해 회복, 상담, 법률 및 복지 지원을 받을 수 있는 기관입니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        additional.forEach { inst -> RecommendedInstitutionCard(inst) }
                    }
                } else {
                    // 기관 없음 문구를 SAFE와 그 외로 분리 (김우영 final_allfile.wy)
                    Text(
                        text = if (result.riskLevel == RiskLevel.SAFE)
                            "현재 분석 결과에서는 추천 기관을 안내하지 않습니다."
                        else
                            "현재 분석 결과에 맞는 추천 기관을 안내하지 못했습니다. 상황이 계속 불안하거나 피해가 우려되면 공식 기관에 직접 문의해 보세요.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "분석 결과는 참고 정보이며, 최종 판단은 사용자에게 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "* 실제 공공기관은 전화로 자금 송금을 요구하지 않습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 위험도별 CTA (김우영 final_allfile.wy CTA 우선순위)
        //  SAFE     : 다시 분석하기
        //  CAUTION  : 대응 가이드 보기 → 다시 분석하기
        //  WARNING  : 대응 가이드 보기 → 추천 기관 목록 보기
        //  CRITICAL : 긴급 도움 요청 → 대응 가이드 보기 → 추천 기관 목록 보기
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (result.riskLevel) {
                RiskLevel.SAFE -> {
                    SafeLinkPrimaryButton(text = "다시 분석하기", onClick = onReanalyzeClick)
                }
                RiskLevel.CAUTION -> {
                    SafeLinkPrimaryButton(
                        text = "대응 가이드 보기",
                        containerColor = result.riskLevel.color(),
                        onClick = onGuideClick
                    )
                    SafeLinkOutlinedButton(text = "다시 분석하기", onClick = onReanalyzeClick)
                }
                RiskLevel.WARNING -> {
                    SafeLinkPrimaryButton(
                        text = "대응 가이드 보기",
                        containerColor = result.riskLevel.color(),
                        onClick = onGuideClick
                    )
                    SafeLinkOutlinedButton(text = "추천 기관 목록 보기", onClick = onSupportClick)
                }
                RiskLevel.CRITICAL -> {
                    SafeLinkPrimaryButton(
                        text = "긴급 도움 요청",
                        containerColor = RiskCritical,
                        onClick = onEmergencyClick
                    )
                    SafeLinkPrimaryButton(
                        text = "대응 가이드 보기",
                        containerColor = result.riskLevel.color(),
                        onClick = onGuideClick
                    )
                    SafeLinkOutlinedButton(text = "추천 기관 목록 보기", onClick = onSupportClick)
                }
            }
        }
    }
}

/** 위험도별 요약 문구 — 김우영 final_allfile.wy 기준 */
private fun riskLevelSummary(level: RiskLevel): String = when (level) {
    RiskLevel.SAFE -> "현재 분석된 대화에서는 뚜렷한 위험 신호가 확인되지 않았습니다. 의심스러운 상황이 계속되면 내용을 다시 확인해 보세요."
    RiskLevel.CAUTION -> "주의가 필요한 표현이 감지되었습니다."
    RiskLevel.WARNING -> "위험 가능성이 높은 표현이 확인되었습니다."
    RiskLevel.CRITICAL -> "즉시 확인이 필요한 위험 신호가 감지되었습니다."
}

@Composable
private fun SafeEmptyCard() {
    SafeLinkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "위험한 표현이 감지되지 않았습니다",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
private fun RecommendedInstitutionCard(inst: RecommendedInstitutionUi) {
    SafeLinkCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "${inst.rank}. ${inst.name}", style = MaterialTheme.typography.titleSmall)
                Text(text = inst.contact, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                text = inst.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Previews — 위험도 4단계(긴급/경고/주의/안전) 상태를 NavHostController 없이 바로 확인
// ─────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "긴급 - 보이스피싱 (82점)")
@Composable
private fun DetectionResultPreviewCritical() {
    SafeLinkTheme {
        DetectionResultContent(
            result = DetectionResultDummyData.vpCritical,
            onBack = {}, onGuideClick = {}, onSupportClick = {}
        )
    }
}

@Preview(showBackground = true, name = "경고 - 로맨스스캠 (47점, 추천기관 병합 예시)")
@Composable
private fun DetectionResultPreviewWarning() {
    SafeLinkTheme {
        DetectionResultContent(
            result = DetectionResultDummyData.rsWarning,
            onBack = {}, onGuideClick = {}, onSupportClick = {}
        )
    }
}

@Preview(showBackground = true, name = "주의 - 가스라이팅 (24점, 추천기관 없음)")
@Composable
private fun DetectionResultPreviewCaution() {
    SafeLinkTheme {
        DetectionResultContent(
            result = DetectionResultDummyData.glCaution,
            onBack = {}, onGuideClick = {}, onSupportClick = {}
        )
    }
}

@Preview(showBackground = true, name = "안전 - 위험 없음")
@Composable
private fun DetectionResultPreviewSafe() {
    SafeLinkTheme {
        DetectionResultContent(
            result = DetectionResultDummyData.safeEmpty,
            onBack = {}, onGuideClick = {}, onSupportClick = {}
        )
    }
}
