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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlue
import com.safelink.app.ui.theme.BrandBlueDark
import com.safelink.app.ui.theme.BrandBlueLight
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.SurfaceWhite
import com.safelink.app.ui.theme.TipBlue
import com.safelink.app.ui.theme.TipBlueContainer

/** 더미 기관 데이터 — 실제로는 assets/institutions.json 로드 (Tasks 5.1~5.2) */
internal data class Institution(
    val id: String,
    val name: String,
    val description: String,
    val phone: String,
    val hours: String,
    val target: String,
    // 홈페이지 이동 대상 URL. 각 기관 공식 사이트(2026-08 확인).
    val website: String
)

internal val dummyInstitutions = listOf(
    Institution(
        id = "112",
        name = "보이스피싱 통합신고센터 (112)",
        description = "경찰청 관할 피해 신고 및 즉시 조치",
        phone = "112",
        hours = "연중무휴 24시간",
        target = "보이스피싱·금융사기 피해자",
        website = "https://ecrm.police.go.kr"
    ),
    Institution(
        id = "118",
        name = "한국인터넷진흥원 (118)",
        description = "스미싱 및 인터넷 침해사고 대응",
        phone = "118",
        hours = "연중무휴 24시간",
        target = "스미싱·해킹·개인정보 침해 피해자",
        website = "https://www.boho.or.kr"
    ),
    Institution(
        id = "132",
        name = "대한법률구조공단 (132)",
        description = "피해 회복을 위한 법률 상담 및 지원",
        phone = "132",
        hours = "평일 09:00~18:00",
        target = "법률 상담이 필요한 피해자",
        website = "https://www.klac.or.kr"
    )
)

private fun badgeColor(institutionId: String): Color = when (institutionId) {
    "112" -> RiskCritical
    "132" -> BrandBlueDark
    else -> BrandBlue
}

/** 지원 서비스 추천 (Figma B09) — 위험 유형별 필터링은 Task 5.3 */
@Composable
fun SupportMatchScreen(navController: NavHostController) {
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

            Text(text = "빠른 도움", style = MaterialTheme.typography.titleMedium)

            dummyInstitutions.forEach { institution ->
                SafeLinkCard(onClick = {
                    navController.navigate(Screen.SupportDetail.createRoute(institution.id))
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(badgeColor(institution.id), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = institution.phone,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = SurfaceWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = institution.name.substringBefore(" ("),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = institution.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text(text = "주의사항", style = MaterialTheme.typography.titleMedium)
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
