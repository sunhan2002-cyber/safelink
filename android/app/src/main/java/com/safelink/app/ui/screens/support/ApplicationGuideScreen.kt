package com.safelink.app.ui.screens.support

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.safelink.app.util.IntentActions
import com.safelink.app.data.repository.InstitutionCatalog
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.theme.BrandBlue
import com.safelink.app.ui.theme.RiskSafe
import com.safelink.app.ui.theme.SurfaceWhite
import com.safelink.app.ui.theme.TextSecondary

private val applicationSteps = listOf(
    "상담 예약하기 — 대표번호에 전화하여 상담을 예약합니다",
    "필요 서류 준비하기 — 아래 서류 목록을 확인하세요",
    "방문 또는 전화 상담 진행",
    "지원 결정 및 후속 절차 안내 받기"
)

private val requiredDocuments = listOf("신분증", "피해 관련 증거 자료 (문자 캡처 등)", "관련 계좌 거래 내역")

/** 신청 절차 안내 (Figma 20:1332) — 단계 타임라인 + 완료 체크 (Task 5.5) */
@Composable
fun ApplicationGuideScreen(navController: NavHostController, institutionId: String) {
    val context = LocalContext.current
    val institutions = remember { InstitutionCatalog.load(context) }
    // 목록에 없는 id 면 엉뚱한 기관의 신청 절차를 보여주지 않는다(예전에는 첫 기관으로 대체됐다).
    val institution = institutions.find { it.id == institutionId } ?: run {
        InstitutionNotFound(title = "신청 절차 안내", onBack = { navController.popBackStack() })
        return
    }
    val completed = remember { mutableStateListOf<Int>() } // 세션 내 유지 (Task 5.5)

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "신청 절차 안내", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SafeLinkCard {
                Text(text = institution.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${completed.size}단계 / ${applicationSteps.size}단계 완료",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 수직 타임라인 — 단계마다 따로 카드로 감싸던 것을 하나의 카드 안에 구분선으로
            // 나누는 패턴(기록 화면과 동일)으로 정리해 "카드 남용"을 줄임
            SafeLinkCard {
                applicationSteps.forEachIndexed { index, step ->
                    val isDone = index in completed
                    val isCurrent = !isDone && index == (completed.maxOrNull()?.plus(1) ?: 0)
                    if (index != 0) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = when {
                                        isDone -> RiskSafe
                                        isCurrent -> BrandBlue
                                        else -> TextSecondary
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "완료",
                                    tint = SurfaceWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Text(text = "${index + 1}", color = SurfaceWhite)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${index + 1}단계: $step",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Checkbox(
                            checked = isDone,
                            onCheckedChange = {
                                if (isDone) completed.remove(index) else completed.add(index)
                            }
                        )
                    }
                }
            }

            // 필요 서류
            SafeLinkCard {
                Text(text = "필요한 서류", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.size(8.dp))
                requiredDocuments.forEach { doc ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = doc, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Text(
                text = "천천히 따라 하시면 됩니다. 막히는 단계가 있다면 기관에 직접 문의하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // contact가 전화번호 형식이 아닌 기관(웹주소/안내문구)도 있어 그럴 때만 버튼 숨김
        institution.phoneOrNull()?.let { phone ->
            Column(modifier = Modifier.padding(20.dp)) {
                SafeLinkPrimaryButton(text = "기관에 전화하기", onClick = {
                    IntentActions.dial(context, phone)
                })
            }
        }
    }
}
