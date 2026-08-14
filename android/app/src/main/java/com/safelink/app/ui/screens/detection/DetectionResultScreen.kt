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
import androidx.compose.material3.CircularProgressIndicator
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
import com.safelink.app.data.model.AnalysisEvidence
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
 * ViewModel 연동 완료 (Task 6.10) — viewModel.result(mutableStateOf)를 그대로 구독하므로
 * 온디바이스 분석이 끝난 뒤 AI 보조 분석(escalateToAI)이 비동기로 결과를 갱신해도 이 화면이
 * 자동으로 재구성된다(별도 StateFlow/collectAsState 불필요). 기록 재열람 등 분석 없이 직접
 * 진입한 경우에만 더미로 대체.
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
        sourceLabel = viewModel.lastAnalysisSource.label,
        isEscalatingToAI = viewModel.isEscalatingToAI,
        onBack = { navController.popBackStack() },
        onGuideClick = { navController.navigate(Screen.ResponseGuide.createRoute(result.riskLevel)) },
        onSupportClick = { navController.navigate(Screen.SupportMatch.route) },
        onEmergencyClick = { navController.navigate(Screen.Emergency.route) },
        onReanalyzeClick = {
            // 다시 분석: 이전 입력·이미지·결과를 비우고 입력 화면으로 (현재 결과 화면은 스택에서 제거)
            viewModel.reset()
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
    sourceLabel: String,
    isEscalatingToAI: Boolean = false,
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
            // 상태 헤더 카드 — 위험도별 고정 제목/설명 (category 결합 금지, 최종 가이드 v1.0)
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
                        Text(
                            text = riskLevelHeadline(result.riskLevel),
                            style = MaterialTheme.typography.titleMedium,
                            color = result.riskLevel.color()
                        )
                        Text(
                            text = riskLevelDescription(result.riskLevel),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "입력 경로: $sourceLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 확인된 위험 유형 — category 있을 때만 (최종 가이드 v1.0)
            if (result.category.isNotBlank()) {
                SafeLinkCard {
                    Text(text = "확인된 위험 유형", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = result.category,
                        style = MaterialTheme.typography.bodyLarge,
                        color = result.riskLevel.color()
                    )
                }
            }

            // 위험 점수 + 감지된 위험 요소 + 분석 근거 — matchedKeywords 있을 때만
            if (result.matchedKeywords.isNotEmpty()) {
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
                            // "위험도: 긴급" 형태 (배지 단독 중복 금지 — 최종 가이드 v1.0)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "위험도: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                RiskBadge(level = result.riskLevel)
                            }
                        }
                    }
                }

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
            }

            // 문장 규칙 근거 — sentenceRuleEvidences 있을 때만 (6주차 신설)
            if (result.sentenceRuleEvidences.isNotEmpty()) {
                Text(text = "문장 규칙 근거", style = MaterialTheme.typography.titleMedium)
                result.sentenceRuleEvidences.forEach { ev -> EvidenceCard(ev, result.riskLevel) }
            }

            // 상황 규칙 근거 — situationalRuleEvidences 있을 때만 (6주차 신설)
            if (result.situationalRuleEvidences.isNotEmpty()) {
                Text(text = "상황 규칙 근거", style = MaterialTheme.typography.titleMedium)
                result.situationalRuleEvidences.forEach { ev -> EvidenceCard(ev, result.riskLevel) }
            }

            // AI 보조 분석 — 2차 AI 분석(escalateToAI)이 실행됐거나 진행 중일 때만 (김재겸 8/14
            // 추가과제 item5 병합: 로딩 상태 표시 + 빈 문자열 안전 처리를 그대로 가져옴).
            // 순서: 문장 규칙 근거 → 상황 규칙 근거 → AI 보조 분석 (위 두 섹션 기준으로 확정).
            if (isEscalatingToAI || result.aiSummary != null || result.aiDetectedPattern != null) {
                Text(text = "AI 보조 분석", style = MaterialTheme.typography.titleMedium)
                SafeLinkCard {
                    if (result.aiSummary == null && result.aiDetectedPattern == null) {
                        // 온디바이스 결과는 이미 위에 표시됨 — AI 응답은 도착하는 대로 이 카드에 채워진다
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "정밀 분석을 진행하고 있어요. 잠시 후 결과가 더해집니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        result.aiDetectedPattern?.takeIf { it.isNotBlank() }?.let { pattern ->
                            Text(
                                text = "감지된 맥락: $pattern",
                                style = MaterialTheme.typography.bodyLarge,
                                color = result.riskLevel.color()
                            )
                        }
                        result.aiSummary?.takeIf { it.isNotBlank() }?.let { summary ->
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }

            // 추천 기관 — 즉시 대응기관/추가 지원기관 2단 (RecommendedInstitutionUi.group 기준)
            if (result.recommendedInstitutions.isNotEmpty()) {
                val (immediate, additional) = result.recommendedInstitutions
                    .sortedBy { it.rank }
                    .partition { it.group == "긴급대응" }

                Text(text = "추천 기관 목록", style = MaterialTheme.typography.titleMedium)
                if (immediate.isNotEmpty()) {
                    Text(text = "즉시 대응기관", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "현재 상황에서 먼저 도움을 받을 수 있는 기관입니다.",
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
            } else if (result.riskLevel != RiskLevel.SAFE) {
                // SAFE는 기관 영역 숨김, CAUTION 이상만 안내 (최종 가이드 v1.0)
                Text(
                    text = "현재 분석 결과에 맞는 추천 기관을 바로 표시하지 못했습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "분석 결과는 참고 정보이며, 최종 판단은 사용자에게 있습니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

/** 위험도별 고정 제목 — 최종 가이드 v1.0 (category 결합 금지) */
private fun riskLevelHeadline(level: RiskLevel): String = when (level) {
    RiskLevel.SAFE -> "위험한 표현이 감지되지 않았습니다."
    RiskLevel.CAUTION -> "주의가 필요한 표현이 감지되었습니다."
    RiskLevel.WARNING -> "위험 가능성이 높은 표현이 확인되었습니다."
    RiskLevel.CRITICAL -> "즉시 확인이 필요한 위험 신호가 감지되었습니다."
}

/** 위험도별 고정 설명 — 최종 가이드 v1.0 */
private fun riskLevelDescription(level: RiskLevel): String = when (level) {
    RiskLevel.SAFE -> "입력한 내용에서 즉시 확인이 필요한 위험 신호는 찾지 못했습니다."
    RiskLevel.CAUTION -> "일부 표현은 상황을 더 확인해 볼 필요가 있습니다."
    RiskLevel.WARNING -> "금전·개인정보 제공이나 외부 이동을 요구하는지 확인해 보세요."
    RiskLevel.CRITICAL -> "앱 설치, 인증정보 제공, 송금 요청은 진행하지 마세요."
}

/**
 * 문장 규칙/상황 규칙 근거 카드 (6주차 신설) — 기존 키워드 "분석 근거" 카드(좌측 컬러바 +
 * 텍스트)와 동일한 스타일로 맞춤. AnalysisEvidence.label이 설명, .detail이 매칭된 문구/판정
 * 조건이라 순서를 label 먼저, detail을 부가 설명으로 배치했다 (matchedKeyword 카드와 반대 —
 * 거긴 매칭 문구가 먼저였는데, 근거 콤보는 매칭된 "문구" 하나가 아니라 "조건 설명"이 핵심이라
 * label을 먼저 보여주는 게 더 자연스럽다고 판단).
 */
@Composable
private fun EvidenceCard(evidence: AnalysisEvidence, riskLevel: RiskLevel) {
    SafeLinkCard {
        Row {
            Spacer(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(riskLevel.color(), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = evidence.label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = evidence.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
            sourceLabel = "스크린샷 분석",
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
            sourceLabel = "텍스트 입력",
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
            sourceLabel = "텍스트 입력",
            onBack = {}, onGuideClick = {}, onSupportClick = {}
        )
    }
}

@Preview(showBackground = true, name = "문장·상황·AI 근거 미리보기 (6주차 신설 섹션 확인용)")
@Composable
private fun DetectionResultPreviewEvidenceShowcase() {
    SafeLinkTheme {
        DetectionResultContent(
            result = DetectionResultDummyData.evidenceShowcase,
            sourceLabel = "텍스트 입력",
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
            sourceLabel = "텍스트 입력",
            onBack = {}, onGuideClick = {}, onSupportClick = {}
        )
    }
}
