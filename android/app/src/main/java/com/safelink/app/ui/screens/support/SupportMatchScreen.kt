package com.safelink.app.ui.screens.support

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.safelink.app.ui.theme.RiskCautionContainer
import com.safelink.app.ui.theme.RiskSafeContainer
import com.safelink.app.ui.theme.TipBlue
import com.safelink.app.ui.theme.TipBlueContainer

private data class SupportCategory(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val iconTint: Color,
    val containerColor: Color,
    val institutionIds: Set<String>
)

private val supportCategories = listOf(
    SupportCategory(
        title = "금융·사기",
        description = "보이스피싱, 송금 피해, 지급정지와 금융 범죄",
        icon = Icons.Filled.AttachMoney,
        iconTint = BrandBlue,
        containerColor = BrandBlueLight,
        institutionIds = setOf(
            "GOV-POLICE", "GOV-FSS", "GOV-KFCWF", "GOV-PROSECUTION", "PUB-LEGALAID"
        )
    ),
    SupportCategory(
        title = "디지털·개인정보",
        description = "악성 링크, 스미싱(문자 사기), 해킹과 개인정보 유출",
        icon = Icons.Filled.Computer,
        iconTint = TipBlue,
        containerColor = TipBlueContainer,
        institutionIds = setOf("GOV-KISA", "GOV-PIPC", "GOV-KCC", "GOV-POLICE")
    ),
    SupportCategory(
        title = "복지·생활",
        description = "긴급 생계, 고용, 건강보험과 외국인 생활 안내",
        icon = Icons.Filled.VolunteerActivism,
        iconTint = BrandBlue,
        containerColor = RiskSafeContainer,
        institutionIds = setOf(
            "PUB-BOKJIRO", "PUB-GOV24", "PUB-WORK24", "PUB-NHIS", "PUB-NPS", "PUB-KNCSW",
            "GOV-IMMIGRATION"
        )
    ),
    SupportCategory(
        title = "법률·피해보호",
        description = "범죄 신고, 법률 상담, 노인·장애인 학대와 피해자 보호",
        icon = Icons.Filled.Gavel,
        iconTint = RiskCritical,
        containerColor = RiskCautionContainer,
        institutionIds = setOf(
            "GOV-POLICE", "GOV-PROSECUTION", "PUB-LEGALAID", "PRIV-FAMILYLAW", "PRIV-NGO", "PUB-WOMEN1366",
            // 사회취약계층 전용 신고 창구 — 누가 피해를 입었는지에 따라 연락할 곳이 따로 있다
            "PUB-ELDER", "PUB-DISABILITY"
        )
    ),
    SupportCategory(
        title = "상담·회복",
        description = "심리 상담, 위기 개입, 쉼터, 청소년·다문화가족 상담",
        icon = Icons.Filled.Favorite,
        iconTint = BrandBlue,
        containerColor = BrandBlueLight,
        institutionIds = setOf(
            "PUB-WOMEN1366", "PUB-MENTALHEALTH", "PRIV-COUNSEL", "PRIV-SUICIDE",
            "PRIV-NGO", "PRIV-YOUTH", "PRIV-RELIGIOUS",
            "PUB-YOUTH", "PUB-DANURI"
        )
    )
)

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
 * 지원 서비스 찾기.
 * 하단 탭에서는 기관을 생활 언어로 묶은 대주제부터 보여주고, 분석 결과에서 들어오면
 * 현재 위험 유형에 맞는 추천 기관을 먼저 보여준다. 기관 상세와 연락 기능은 기존 흐름을 유지한다.
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
    var selectedCategory by remember { mutableStateOf<SupportCategory?>(null) }
    BackHandler(enabled = selectedCategory != null) { selectedCategory = null }

    val recommendedInstitutions = remember(institutions, recommendedRank) {
        institutions.filter { it.id in recommendedRank }.sortedBy { recommendedRank[it.id] }
    }
    val categoryInstitutions = remember(institutions, selectedCategory) {
        selectedCategory?.let { category ->
            institutions.filter { it.id in category.institutionIds }
        }.orEmpty()
    }
    val scrollState = rememberScrollState()
    val topBarCollapse by remember {
        derivedStateOf { (scrollState.value / 96f).coerceIn(0f, 1f) }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
        SafeLinkTopBar(
            title = selectedCategory?.title ?: "지원",
            onBack = selectedCategory?.let { { selectedCategory = null } },
            collapseFraction = topBarCollapse
        )

        Column(
            modifier = Modifier
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedCategory == null) {
                Text(
                    text = "어떤 도움이 필요한가요?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "상황에 가까운 분야를 선택하면 관련 기관을 모아 보여드려요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (recommendedInstitutions.isNotEmpty()) {
                    Text(text = "현재 상황에 추천", style = MaterialTheme.typography.titleMedium)
                    recommendedInstitutions.forEach {
                        InstitutionRow(it, navController, recommended = true)
                    }
                }

                Text(text = "지원 분야", style = MaterialTheme.typography.titleMedium)
                supportCategories.forEach { category ->
                    CategoryRow(
                        category = category,
                        institutionCount = institutions.count { it.id in category.institutionIds },
                        onClick = { selectedCategory = category }
                    )
                }
            } else {
                val category = selectedCategory!!
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "관련 기관 ${categoryInstitutions.size}곳",
                    style = MaterialTheme.typography.titleMedium
                )
                categoryInstitutions.forEach {
                    InstitutionRow(it, navController, recommended = it.id in recommendedRank)
                }

                SupportCallGuide()
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun CategoryRow(
    category: SupportCategory,
    institutionCount: Int,
    onClick: () -> Unit
) {
    SafeLinkCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(category.containerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = category.iconTint,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "관련 기관 ${institutionCount}곳",
                    style = MaterialTheme.typography.bodySmall,
                    color = category.iconTint
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

@Composable
private fun SupportCallGuide() {
    SafeLinkCard(containerColor = TipBlueContainer) {
        Text(
            text = "전화할 때 이렇게 말해보세요",
            style = MaterialTheme.typography.titleMedium,
            color = TipBlue
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "\"의심스러운 연락을 받았는데, 어떤 도움을 받을 수 있는지 알고 싶어요.\"",
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
            Text(text = "긴급한 상황인가요?", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "생명이나 신체에 즉각적인 위험이 있다면 기관을 찾기 전에 112로 신고하세요.",
            style = MaterialTheme.typography.bodyLarge
        )
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
