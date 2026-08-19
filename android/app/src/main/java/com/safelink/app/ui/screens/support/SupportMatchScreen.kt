package com.safelink.app.ui.screens.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.util.IntentActions
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueLight

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

/** 지원 서비스 추천 (Figma 20:318) — 위험 유형별 필터링은 Task 5.3 */
@Composable
fun SupportMatchScreen(navController: NavHostController) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "추천 기관 목록")

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SafeLinkCard(containerColor = BrandBlueLight) {
                Text(
                    text = "추천 기관 목록",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "분석 결과를 바탕으로 현재 상황에 도움이 될 수 있는 기관을 안내합니다.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            dummyInstitutions.forEach { institution ->
                SafeLinkCard(onClick = {
                    navController.navigate(Screen.SupportDetail.createRoute(institution.id))
                }) {
                    Text(text = institution.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = "추천 이유", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = institution.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        Column(modifier = Modifier.weight(1f)) {
                            SafeLinkPrimaryButton(text = "전화하기", onClick = {
                                IntentActions.dial(context, institution.phone)
                            })
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SafeLinkOutlinedButton(text = "홈페이지 이동", onClick = {
                                IntentActions.openWeb(context, institution.website)
                            })
                        }
                    }
                }
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
