package com.safelink.app.ui.screens.diagnosis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueLight
import com.safelink.app.ui.theme.RiskWarning
import com.safelink.app.ui.theme.RiskWarningContainer
import com.safelink.app.ui.theme.TipBlueContainer

/** 문항·가중치 정의와 산출 로직은 [DiagnosisViewModel]에 있다(Task 4.9). */
@Composable
fun DiagnosisScreen(
    navController: NavHostController,
    viewModel: DiagnosisViewModel
) {
    val checked = viewModel.checkedIndices

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "자가진단", onBack = { navController.popBackStack() })

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

            SafeLinkCard(containerColor = TipBlueContainer) {
                Text(
                    text = "최근 상황에 해당하는 항목을 모두 선택해 주세요. 결과는 이 기기에만 저장됩니다.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            checklistItems.forEachIndexed { index, item ->
                val isChecked = index in checked
                SafeLinkCard(
                    onClick = { viewModel.toggle(index) },
                    containerColor = if (isChecked) BrandBlueLight else null
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { viewModel.toggle(index) }
                        )
                        Text(
                            text = item.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (item.highRisk) {
                            // 고위험 항목(가중치 2점) — 색상만으로 구분하지 않도록 텍스트 배지로 표시
                            Text(
                                text = "고위험",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = RiskWarning,
                                modifier = Modifier
                                    .background(RiskWarningContainer, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
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
                    viewModel.submit()
                    navController.navigate(Screen.DiagnosisResult.route)
                }
            )
        }
    }
}
