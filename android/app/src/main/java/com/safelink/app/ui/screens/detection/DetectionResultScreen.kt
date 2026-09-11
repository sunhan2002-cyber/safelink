package com.safelink.app.ui.screens.detection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.link.LinkRiskResult
import com.safelink.app.data.link.LinkVerdict
import com.safelink.app.data.model.DetectionResult
import com.safelink.app.data.model.EvidenceText
import com.safelink.app.data.model.DetectionResultDummyData
import com.safelink.app.data.model.RecommendedInstitutionUi
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.RiskBadge
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.components.color
import com.safelink.app.ui.components.containerColor
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.RuleAiAccent
import com.safelink.app.ui.theme.RuleAiContainer
import com.safelink.app.ui.theme.RuleSentenceAccent
import com.safelink.app.ui.theme.RuleSentenceContainer
import com.safelink.app.ui.theme.RuleSituationalAccent
import com.safelink.app.ui.theme.RuleSituationalContainer
import com.safelink.app.ui.theme.SafeLinkTheme

/**
 * 분석 결과 (Figma 20:117)
 *
 * 데이터 구조: com.safelink.app.data.model.DetectionResult (필드 정의 근거는 해당 파일 KDoc 참고)
 * 더미 데이터: DetectionResultDummyData (data/위험 문장 테스트.json 검증 케이스 기반, 점수 실제 계산 로직과 일치)
 *
 * ViewModel 연동 완료 (Task 6.10) — viewModel.result(mutableStateOf)를 그대로 구독하므로
 * 온디바이스 분석이 끝난 뒤 AI 보조분석(escalateToAI)이 비동기로 결과를 갱신해도 이 화면이
 * 자동으로 재구성된다(별도 StateFlow/collectAsState 불필요). 기록 재열람 등 분석 없이 직접
 * 진입한 경우에만 더미로 대체.
 * "분석한 내용" 카드는 MatchedKeyword.startIndex/endIndex로 원문의 매칭 구간에 배경색·밑줄을
 * 입혀 보여준다([highlightMatches]) — 목록만으로는 어떤 문맥에서 걸렸는지 보이지 않기 때문.
 *
 * 네비게이션에 의존하지 않는 렌더링 로직은 DetectionResultContent로 분리했음
 * (Preview에서 NavHostController 없이 4가지 위험도 상태를 바로 확인 가능 — 하단 Preview 함수 참고).
 */
@Composable
fun DetectionResultScreen(
    navController: NavHostController,
    viewModel: DetectionViewModel,
    recordId: String? = null
) {
    // 기록 탭에서 들어온 경우: 저장된 원문을 같은 엔진으로 재분석해 결과를 복원한다(Task 7.1).
    LaunchedEffect(recordId) {
        if (recordId != null) viewModel.loadRecord(recordId)
    }

    // 실제 데이터 흐름 (김선한_02 문서): 공유 ViewModel의 분석 결과를 사용.
    // 결과가 아직 없을 때만(복원 중이거나 직접 진입) 더미로 대체한다.
    val result: DetectionResult = viewModel.result ?: DetectionResultDummyData.vpCritical

    DetectionResultContent(
        result = result,
        sourceLabel = viewModel.lastAnalysisSource.label,
        isEscalatingToAI = viewModel.isEscalatingToAI,
        linkResults = viewModel.linkResults,
        isCheckingLinks = viewModel.isCheckingLinks,
        manualAiMessage = viewModel.manualAiMessage,
        onRequestAi = { viewModel.requestManualAi() },
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
    linkResults: List<LinkRiskResult> = emptyList(),
    isCheckingLinks: Boolean = false,
    manualAiMessage: String? = null,
    onRequestAi: () -> Unit = {},
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
            // 상태 헤더 카드 — 위험도별 고정 제목/설명 (category 결합 금지, 최종 가이드 v1.0).
            // 9주차 - Figma "Concept B" B04(Risk Result) 구조 반영: 정성적 문장을 제일 크게
            // 유지(4순위 작업 그대로)하고, 예전에 "확인된 위험 유형" 카드 + 점수 큰 숫자 카드로
            // 따로 있던 것을 헤더 카드 안 작은 보조 줄 하나로 합침 — 정보는 그대로, 비중만 낮춤.
            SafeLinkCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = result.riskLevel.color(),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = riskLevelHeadline(result.riskLevel),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = result.riskLevel.color()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = riskLevelDescription(result.riskLevel),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))
                        val scoreLine = buildString {
                            if (result.matchedKeywords.isNotEmpty()) append("위험 점수 ${result.score}")
                            if (result.category.isNotBlank()) {
                                if (isNotEmpty()) append(" · ")
                                append(result.category)
                            }
                            if (isNotEmpty()) append(" · ")
                            append("입력 경로: $sourceLabel")
                        }
                        Text(
                            text = scoreLine,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 감지된 표현 — 매칭 키워드는 짧은 태그 칩으로만(무엇이 걸렸는지 한눈에), 구체적인
            // "왜"는 아래 통합 리스트에서 설명한다. 위험도 배지 병행(색만으로 전달 금지, 최종
            // 가이드 v1.0).
            if (result.matchedKeywords.isNotEmpty()) {
                SafeLinkCard {
                    Text(text = "감지된 표현", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
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
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "위험도: ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        RiskBadge(level = result.riskLevel)
                    }
                }

                // 원문에서 어느 구간이 걸렸는지 그대로 보여준다 — 목록만으로는 문맥이 안 보이므로
                if (result.originalText.isNotBlank()) {
                    Text(text = "분석한 내용", style = MaterialTheme.typography.titleMedium)
                    SafeLinkCard {
                        Text(
                            text = highlightMatches(result),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // 링크 검사 — 키워드가 하나도 안 걸린 문장이라도 링크만 위험할 수 있으므로
            // matchedKeywords 조건 밖에 둔다.
            LinkRiskSection(results = linkResults, isChecking = isCheckingLinks)

            // 이런 표현을 확인했어요 — 9주차: 예전엔 "문장 규칙 근거"/"상황 규칙 근거"/
            // "AI 보조분석"이 각각 따로 박스로 나뉘어 있어 화면이 길고 조각나 보이던 문제
            // (Figma 리디자인 검토 중 발견) 대응. 색 라벨(문장=청록/상황=보라/AI=남색, 7주차
            // 색 그대로)로 구분은 유지한 채 하나의 리스트로 통합.
            UnifiedReasonList(result = result, isEscalatingToAI = isEscalatingToAI)

            // 수동 AI 보조분석 요청 (보고서 4장 "수동 신고") — AI 가 아직 반영되지 않은 결과에서만 보인다.
            // 온디바이스 판정이 AI 호출 조건에 걸리지 않았어도, 사용자가 애매하다고 느끼면 직접 요청할 수 있다.
            // (김재겸 병합) UnifiedReasonList가 문장/상황/AI 근거를 이미 하나로 합쳐 보여주므로,
            // 이 버튼은 그 아래에 이어 붙인다 — 자동 표시(위)와 수동 요청(아래)이 한 흐름으로 읽히게.
            if (!isEscalatingToAI && result.aiSummary == null && result.aiDetectedPattern == null) {
                ResultSecondaryLink(text = "AI 보조분석 요청", onClick = onRequestAi)
                Text(
                    text = "판단이 애매하다고 느껴질 때 눌러주세요. 누르면 대화 내용이 AI 제공사(Anthropic)로 전송됩니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                manualAiMessage?.let { message ->
                    Text(text = message, style = MaterialTheme.typography.bodySmall, color = RiskCritical)
                }
            }

            // 추천 기관 — 1순위 기관만 인라인으로 보여주고 나머지는 지원 탭에서(정보는 그대로,
            // 화면에 한 번에 몰아넣지 않음 - Figma 리디자인 구조 반영)
            if (result.recommendedInstitutions.isNotEmpty()) {
                val sorted = result.recommendedInstitutions.sortedBy { it.rank }
                Text(text = "추천 기관", style = MaterialTheme.typography.titleMedium)
                RecommendedInstitutionCard(sorted.first())
                if (sorted.size > 1) {
                    TextButton(onClick = onSupportClick, modifier = Modifier.fillMaxWidth()) {
                        Text("추천 기관 전체 보기 (${sorted.size}곳) ›")
                    }
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

        // 위험도별 CTA — 9주차: 화면마다 버튼이 3개씩 같은 무게로 나열되던 걸(Figma 리디자인
        // 검토 중 발견, KRDS "화면당 Primary 1개" 원칙과도 일치) 위험도별 제일 중요한 행동
        // 1개만 채워진 버튼으로 두고, 나머지는 텍스트 링크로. 핸들러(4개)는 전부 그대로 유지 —
        // 무게만 다르지 다 화면에 남아있고 다 누를 수 있음.
        //  SAFE     : 다시 분석하기(주)
        //  CAUTION  : 대응 가이드 보기(주) · 다시 분석하기(링크)
        //  WARNING  : 대응 가이드 보기(주) · 추천 기관 전체 보기(링크)
        //  CRITICAL : 긴급 도움 요청(주) · 대응 가이드 보기 · 추천 기관 전체 보기(링크)
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    ResultSecondaryLink(text = "다시 분석하기", onClick = onReanalyzeClick)
                }
                RiskLevel.WARNING -> {
                    SafeLinkPrimaryButton(
                        text = "대응 가이드 보기",
                        containerColor = result.riskLevel.color(),
                        onClick = onGuideClick
                    )
                    ResultSecondaryLink(text = "추천 기관 전체 보기", onClick = onSupportClick)
                }
                RiskLevel.CRITICAL -> {
                    SafeLinkPrimaryButton(
                        text = "긴급 도움 요청",
                        containerColor = RiskCritical,
                        onClick = onEmergencyClick
                    )
                    ResultSecondaryLink(text = "대응 가이드 보기", onClick = onGuideClick)
                    ResultSecondaryLink(text = "추천 기관 전체 보기", onClick = onSupportClick)
                }
            }
        }
    }
}

@Composable
private fun ResultSecondaryLink(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(text)
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
 * 근거 섹션 제목(아이콘+컬러 텍스트) — 문장 규칙/상황 규칙/AI 보조분석을 키워드 "분석 근거"
 * 섹션과 시각적으로 구분하기 위해 7주차에 추가. 아이콘 하나만으로도 스크롤하면서 "지금
 * 어느 층을 보고 있는지" 바로 알 수 있게 하는 게 목적 — 최종 아이콘/색상 선택은
 * 김우영/김재겸이 다듬을 수 있음(구조는 고정, 표현은 유동).
 */
/**
 * 대화에 섞여 온 링크의 안전성 검사 결과.
 *
 * 문구를 쓸 때 지킨 원칙이 하나 있다 — **위험하지 않다고 단정하지 않는다.**
 * 차단 목록에 없다는 건 "안전하다"가 아니라 "아직 신고되지 않았다"는 뜻이고,
 * 새로 만든 스미싱 도메인은 대개 등재 전이다. 여기서 "안전합니다"라고 써 버리면
 * 앱이 오히려 사용자를 안심시켜 링크를 누르게 만든다.
 *
 * 마지막 줄에서 검사 방식을 밝히는 것도 같은 이유다. 사용자가 받은 링크가 외부로
 * 나가지 않는다는 점은 이 앱에서 약속으로 남아야 하는 정보라 화면에 드러낸다.
 */
@Composable
private fun LinkRiskSection(results: List<LinkRiskResult>, isChecking: Boolean) {
    if (results.isEmpty() && !isChecking) return

    EvidenceSectionHeader(icon = Icons.Filled.Link, title = "링크 검사", accent = MaterialTheme.colorScheme.primary)

    if (results.isEmpty()) {
        SafeLinkCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "링크를 확인하고 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    results.forEach { item -> LinkRiskCard(item) }

    Text(
        text = "링크 주소는 외부로 전송되지 않으며, 기기에 저장된 위험 주소 목록과 대조했습니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // 구글 Safe Browsing 데이터로 판정한 건이 하나라도 있으면 출처를 밝힌다.
    // 표기 문구와 안내 페이지 링크는 Safe Browsing 이용 조건상 **의무 사항**이라 임의로
    // 번역하거나 생략하지 않는다(검사 자체가 실패해 UNCHECKED 뿐이면 구글 데이터를 쓴 게
    // 아니므로 표기하지 않는다).
    if (results.any { it.verdict != LinkVerdict.UNCHECKED }) {
        val uriHandler = LocalUriHandler.current
        Text(
            text = "Advisory provided by Google",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { uriHandler.openUri(SAFE_BROWSING_ADVISORY_URL) }
        )
    }
}

/** Safe Browsing 안내 페이지 — 위 출처 표기에서 링크해야 하는 주소. */
private const val SAFE_BROWSING_ADVISORY_URL = "https://developers.google.com/safe-browsing/v4/advisory"

@Composable
private fun LinkRiskCard(item: LinkRiskResult) {
    val dangerous = item.verdict == LinkVerdict.DANGEROUS
    val accent = if (dangerous) RiskCritical else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = when (item.verdict) {
        LinkVerdict.DANGEROUS -> Icons.Filled.Warning
        LinkVerdict.NO_MATCH -> Icons.Filled.CheckCircle
        LinkVerdict.UNCHECKED -> Icons.AutoMirrored.Filled.HelpOutline
    }
    val headline = when (item.verdict) {
        LinkVerdict.DANGEROUS -> item.threat?.label ?: "위험한 주소"
        LinkVerdict.NO_MATCH -> "알려진 위험 목록에는 없습니다"
        LinkVerdict.UNCHECKED -> item.uncheckedReason ?: "검사하지 못했습니다"
    }
    val detail = when (item.verdict) {
        LinkVerdict.DANGEROUS -> item.threat?.description ?: "위험한 주소로 신고된 링크입니다."
        LinkVerdict.NO_MATCH -> "아직 신고되지 않은 새 주소일 수 있으니, 모르는 사람이 보낸 링크는 열지 마세요."
        LinkVerdict.UNCHECKED -> "직접 확인이 필요합니다. 모르는 사람이 보낸 링크는 열지 마세요."
    }

    SafeLinkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = if (dangerous) FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // 원문에 쓰여 있던 그대로 보여준다 — 정규화한 주소만 보이면 사용자가 자기가 받은
        // 링크와 같은 것인지 알아보지 못한다.
        Text(text = item.link.displayText, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (item.link.obfuscated) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "주소를 일부러 변형해 보낸 링크입니다. 차단을 피하려는 수법입니다.",
                style = MaterialTheme.typography.bodySmall,
                color = RiskCritical
            )
        }
    }
}

@Composable
private fun EvidenceSectionHeader(icon: ImageVector, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = accent)
    }
}

/** 통합 리스트 한 줄 — 색 라벨(문장/상황/AI)로 어느 근거인지 구분한다. */
private data class UnifiedReason(
    val tagLabel: String,
    val tagColor: Color,
    val tagContainer: Color,
    val text: String
)

/**
 * "이런 표현을 확인했어요" — 문장 규칙/상황 규칙/AI 보조분석 근거를 하나의 리스트로 통합
 * (9주차, Figma 리디자인 반영). 예전엔 이 셋이 각각 별도 박스+섹션 제목으로 나뉘어 있어
 * 화면이 길고 조각나 보인다는 지적이 있었음 — 근거 종류 구분은 색 라벨로 유지한 채 하나의
 * 카드 안에서 구분선으로만 나눈다. AI가 아직 진행 중이면(로딩) 기존 항목 아래에 진행 표시를
 * 이어 붙인다(완전히 새로운 카드로 만들지 않음 — 완료되면 그 자리에 내용이 채워지도록).
 */
@Composable
private fun UnifiedReasonList(result: DetectionResult, isEscalatingToAI: Boolean) {
    // label/detail에 내부 개발 메모(기법 코드·규칙 id)가 섞여 있어 화면에 보일 때만 걸러낸다
    // (EvidenceText 참고, 김재겸 병합) — detail은 매칭된 원문도 담고 있어 개발 메모 자르기는
    // 건너뛰고 코드·규칙 id만 걷어낸다.
    val reasons = buildList {
        result.sentenceRuleEvidences.forEach {
            val label = EvidenceText.forUser(it.label).ifBlank { it.label }
            val detail = EvidenceText.forUser(it.detail, cutDeveloperNote = false)
            add(UnifiedReason("문장", RuleSentenceAccent, RuleSentenceContainer, "$label — $detail"))
        }
        result.situationalRuleEvidences.forEach {
            val label = EvidenceText.forUser(it.label).ifBlank { it.label }
            val detail = EvidenceText.forUser(it.detail, cutDeveloperNote = false)
            add(UnifiedReason("상황", RuleSituationalAccent, RuleSituationalContainer, "$label — $detail"))
        }
        val aiText = result.aiDetectedPattern?.takeIf { it.isNotBlank() }
            ?: result.aiSummary?.takeIf { it.isNotBlank() }
        if (aiText != null) {
            add(UnifiedReason("AI", RuleAiAccent, RuleAiContainer, aiText))
        }
    }
    val aiLoading = isEscalatingToAI && result.aiSummary == null && result.aiDetectedPattern == null

    if (reasons.isEmpty() && !aiLoading) return

    Text(text = "이런 표현을 확인했어요", style = MaterialTheme.typography.titleMedium)
    SafeLinkCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            reasons.forEachIndexed { index, reason ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = reason.tagLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = reason.tagColor,
                        modifier = Modifier
                            .background(reason.tagContainer, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = reason.text,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (index != reasons.lastIndex || aiLoading) {
                    HorizontalDivider()
                }
            }
            if (aiLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "정밀 분석을 진행하고 있어요. 잠시 후 결과가 더해집니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

/**
 * 원문에서 매칭된 구간에 배경색과 밑줄을 입힌 문자열을 만든다.
 *
 * [com.safelink.app.data.model.MatchedKeyword]의 startIndex/endIndex 를 그대로 쓰며,
 * 구간이 겹치거나 범위를 벗어난 값은 건너뛴다(엔진이 바뀌어도 화면이 깨지지 않도록).
 */
private fun highlightMatches(result: DetectionResult): AnnotatedString {
    val text = result.originalText
    val spans = result.matchedKeywords
        .filter { it.startIndex in 0..text.length && it.endIndex in it.startIndex..text.length }
        .sortedBy { it.startIndex }

    return buildAnnotatedString {
        var cursor = 0
        spans.forEach { kw ->
            // 앞선 구간과 겹치면 건너뛴다(같은 자리를 두 번 칠하지 않도록)
            if (kw.startIndex < cursor) return@forEach
            append(text.substring(cursor, kw.startIndex))
            withStyle(
                SpanStyle(
                    background = result.riskLevel.color().copy(alpha = 0.18f),
                    color = result.riskLevel.color(),
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(text.substring(kw.startIndex, kw.endIndex))
            }
            cursor = kw.endIndex
        }
        append(text.substring(cursor))
    }
}
