package com.safelink.app.ui.screens.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.raw.InstitutionEntry
import com.safelink.app.data.repository.InstitutionCatalog
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlue
import com.safelink.app.ui.theme.BrandBlueLight
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.TipBlue
import com.safelink.app.ui.theme.TipBlueContainer

/**
 * 기관의 contact 필드는 전화번호("112", "1577-5500")뿐 아니라 웹 주소("복지로.kr"),
 * 안내 문구("지역별 센터", "24시간 상담전화") 등 형식이 섞여 있어(data/institutions.json 참고),
 * 버튼(전화 걸기/홈페이지 이동)을 보여줄지는 값 형태로 판별한다.
 */
internal fun InstitutionEntry.phoneOrNull(): String? {
    val core = contact.substringBefore("(").trim()
    return core.takeIf { it.isNotEmpty() && it.all { c -> c.isDigit() || c == '-' } }
}

internal fun InstitutionEntry.websiteOrNull(): String? {
    val trimmed = contact.trim()
    return trimmed.takeIf { Regex("\\.[a-zA-Z]{2,4}$").containsMatchIn(it) }
}

/**
 * 지원 서비스 추천 (Figma B09).
 *
 * [matchedRiskTypes]가 비어있으면(하단 탭으로 바로 진입) 예전처럼 전체 목록만 보여준다.
 * 분석 결과 화면에서 "추천 기관 전체 보기"로 들어와 값이 있으면(Task 5.3), institutions.json의
 * risk_type_priority를 찾아 매칭된 기관을 그룹 안에서 순위대로 앞으로 정렬하고 "추천" 배지를 단다
 * — group(긴급대응/기타) 2단 구조 자체는 그대로 유지, 그 안에서만 재정렬.
 */
@Composable
fun SupportMatchScreen(navController: NavHostController, matchedRiskTypes: List<String> = emptyList()) {
    val context = LocalContext.current
    val institutions = remember { InstitutionCatalog.load(context) }
    val recommendedRank = remember(matchedRiskTypes) {
        if (matchedRiskTypes.isEmpty()) emptyMap()
        else {
            val priority = InstitutionCatalog.riskTypePriority(context)
            val ranks = mutableMapOf<String, Int>()
            matchedRiskTypes.forEach { type ->
                priority[type].orEmpty().forEach { entry ->
                    val current = ranks[entry.institutionId]
                    if (current == null || entry.rank < current) ranks[entry.institutionId] = entry.rank
                }
            }
            ranks
        }
    }
    val (urgent, others) = remember(institutions, recommendedRank) {
        institutions.partition { it.group == "긴급대응" }
            .let { (u, o) ->
                u.sortedBy { recommendedRank[it.id] ?: Int.MAX_VALUE } to
                    o.sortedBy { recommendedRank[it.id] ?: Int.MAX_VALUE }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "도움받기")

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 화면 제목과 이 카드 제목이 똑같은 문구라 중복돼 보이던 문제 대응 (UI/UX 리뷰 중 발견)
            // - 카드는 "어떤 기준으로 추천했는지" 설명으로 역할 분리
            SafeLinkCard(containerColor = BrandBlueLight) {
                Text(
                    text = "어디에 연락해야 할지 함께 찾아드릴게요",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "분석 결과를 바탕으로 현재 상황에 도움이 될 수 있는 기관을 안내합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (urgent.isNotEmpty()) {
                Text(text = "즉시 대응기관", style = MaterialTheme.typography.titleMedium)
                urgent.forEach { InstitutionRow(it, navController, recommended = it.id in recommendedRank) }
            }

            if (others.isNotEmpty()) {
                Text(text = "추가 지원기관", style = MaterialTheme.typography.titleMedium)
                others.forEach { InstitutionRow(it, navController, recommended = it.id in recommendedRank) }
            }

            // 전화를 앞두고 무슨 말을 해야 할지 막막한 사용자를 위한 짧은 스크립트 (Figma B09) —
            // 노인/사회적 약자 접근성 개선 취지와도 맞음
            // 다른 안내성 카드들과 같은 파란 톤으로 통일(사용자 요청)
            SafeLinkCard(containerColor = TipBlueContainer) {
                Text(
                    text = "전화할 때 이렇게 말해보세요",
                    style = MaterialTheme.typography.titleMedium,
                    color = TipBlue
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"의심되는 연락을 받았고, 이미 송금했는지 확인이 필요해요.\"",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            SafeLinkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = RiskCritical,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "주의사항", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• 피해 발생 시 신속하게 계좌 지급 정지를 요청하세요.\n• 출처가 불분명한 앱은 즉시 삭제해 주세요.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun InstitutionRow(
    institution: InstitutionEntry,
    navController: NavHostController,
    recommended: Boolean = false
) {
    SafeLinkCard(onClick = {
        navController.navigate(Screen.SupportDetail.createRoute(institution.id))
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(BrandBlueLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalance,
                    contentDescription = null,
                    tint = BrandBlue
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = institution.name, style = MaterialTheme.typography.titleMedium)
                    if (recommended) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "추천",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = TipBlue,
                            modifier = Modifier
                                .background(TipBlueContainer, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = institution.role,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
