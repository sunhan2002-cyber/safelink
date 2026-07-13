package com.safelink.app.ui.screens.diagnosis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueLight
import com.safelink.app.ui.theme.RiskWarning

/** 자가진단 체크리스트 항목 — 확정 문항은 Task 3.6, 가중치는 Design.md 5.1 */
private data class ChecklistItem(val text: String, val highRisk: Boolean)

private val checklistItems = listOf(
    ChecklistItem("상대방이 급하게 돈을 보내라고 요구했다", true),
    ChecklistItem("가족이나 지인을 사칭하는 것 같은 연락을 받았다", true),
    ChecklistItem("협박이나 위협적인 말을 들었다", true),
    ChecklistItem("개인정보나 계좌번호를 요구받았다", true),
    ChecklistItem("의심스러운 링크 클릭을 유도받았다", false),
    ChecklistItem("수사기관·정부기관이라며 연락이 왔다", true),
    ChecklistItem("높은 수익을 보장한다며 투자를 권유받았다", false),
    ChecklistItem("이 일을 다른 사람에게 말하지 말라고 했다", true),
    ChecklistItem("반복적으로 연락하며 재촉당하고 있다", false),
    ChecklistItem("만남이나 연락을 통제당하는 느낌이 든다", true),
    ChecklistItem("앱 설치나 원격 제어를 요구받았다", false),
    ChecklistItem("확인하기 어려운 이야기로 불안하게 만들었다", false),
)

@Composable
fun DiagnosisScreen(navController: NavHostController) {
    val checked = remember { mutableStateListOf<Int>() }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "자가 진단", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${checklistItems.size}개 항목 중 ${checked.size}개 선택됨",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            SafeLinkCard {
                Text(
                    text = "최근 상황에 해당하는 항목을 모두 선택해 주세요. 결과는 이 기기에만 저장됩니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            checklistItems.forEachIndexed { index, item ->
                val isChecked = index in checked
                Card(
                    onClick = {
                        if (isChecked) checked.remove(index) else checked.add(index)
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChecked) BrandBlueLight
                        else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                if (isChecked) checked.remove(index) else checked.add(index)
                            }
                        )
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.highRisk) {
                            // 고위험 항목(가중치 2점) 표시
                            Spacer(
                                modifier = Modifier
                                    .padding(end = 12.dp)
                                    .size(8.dp)
                                    .background(RiskWarning, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            SafeLinkPrimaryButton(
                text = "결과 확인하기",
                enabled = checked.isNotEmpty(),
                onClick = {
                    // TODO: 가중치 합산 → RiskLevel 분류 → DiagnosisViewModel 공유 (Task 4.9)
                    navController.navigate(Screen.DiagnosisResult.route)
                }
            )
        }
    }
}
