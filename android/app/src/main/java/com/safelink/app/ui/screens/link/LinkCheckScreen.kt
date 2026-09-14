package com.safelink.app.ui.screens.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.link.LinkExtractor
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.LinkRiskSection
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.screens.detection.DetectionViewModel
import com.safelink.app.ui.theme.BrandBlue
import com.safelink.app.ui.theme.TextSecondary
import com.safelink.app.ui.theme.TipBlue
import com.safelink.app.ui.theme.TipBlueContainer
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val MAX_CHARS = 5000

/**
 * 링크 검사 전용 화면 — 대화 전체를 분석하는 [com.safelink.app.ui.screens.detection.DetectionInputScreen]과
 * 달리, "이 링크가 위험한지"만 빠르게 확인하는 용도다(사용자 요청, Task 5.3 이후 추가).
 *
 * 문자 전체를 붙여넣게 한다(링크만 따로 오려낼 필요 없음) — [LinkExtractor]가 문장에 섞인
 * 링크·훼손된 링크까지 찾아낸다. 분석 경로는 [DetectionViewModel.runLinkCheckAnalysis]로
 * 기존 온디바이스 분석·링크 검사·기록 저장을 그대로 재사용한다.
 *
 * 결과가 위험(WARNING 이상)이면 기존 [Screen.DetectionResult] 전체 결과 화면으로 보낸다 —
 * 대응 가이드·지원기관 등 필요한 안내가 이미 그 화면에 다 있어서다. 그 외(안전/검사 불가)는
 * 이 화면 안에서 [LinkRiskSection]만 간단히 보여준다 — 링크 하나 확인하려고 "위험 점수 0점"
 * 같은 대화 분석용 화면까지 볼 필요는 없다는 판단.
 */
@Composable
fun LinkCheckScreen(navController: NavHostController, viewModel: DetectionViewModel) {
    var text by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var showInlineResult by remember { mutableStateOf(false) }
    var checkedLinkCount by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "링크 분석", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "받은 링크가 위험한지 확인해요. 링크만 오려낼 필요 없이 문자 내용을 그대로 붙여넣으면 그 안의 링크를 찾아 검사합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "받은 문자 내용", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        if (it.length <= MAX_CHARS) text = it
                        showInlineResult = false
                    },
                    placeholder = {
                        Text(
                            text = "의심스러운 링크가 포함된 문자를 붙여넣으세요.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
                Text(
                    text = "${"%,d".format(text.length)} / ${"%,d".format(MAX_CHARS)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SafeLinkCard(containerColor = TipBlueContainer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = TipBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "개인정보 보호 안내", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "링크 주소는 기기에 저장된 위험 주소 목록과 대조할 뿐, 외부로 전송되지 않습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isChecking) {
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
            } else if (showInlineResult) {
                if (checkedLinkCount == 0) {
                    SafeLinkCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = null,
                                tint = BrandBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "붙여넣은 내용에서 링크를 찾지 못했어요.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else if (viewModel.linkResults.isEmpty() && !viewModel.isCheckingLinks) {
                    // 링크는 찾았지만 검사 결과가 없다 — 이 빌드에 링크 검사 키가 없어 검사를 건너뛴 경우
                    // (DetectionViewModel.showLinks). LinkRiskSection은 결과가 없으면 아무것도 안 그리므로,
                    // 이 화면에서는 대신 이유를 알려준다. 검사가 아직 진행 중이면 이 안내를 띄우지 않는다.
                    SafeLinkCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "지금은 링크 검사를 사용할 수 없어요. 모르는 사람이 보낸 링크는 열지 마세요.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    LinkRiskSection(results = viewModel.linkResults, isChecking = viewModel.isCheckingLinks)
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            SafeLinkPrimaryButton(
                text = "링크 검사하기",
                enabled = text.isNotBlank() && !isChecking,
                onClick = {
                    val submitted = text
                    checkedLinkCount = LinkExtractor.extract(submitted).size
                    showInlineResult = false
                    isChecking = true
                    scope.launch {
                        viewModel.reset()
                        viewModel.originalText = submitted
                        val ok = viewModel.runLinkCheckAnalysis()
                        // 분석은 링크 검사를 최대 1.5초만 기다리고 먼저 끝난다(대화 분석 화면이 오래 멈추지 않게).
                        // 이 화면은 링크 판정이 곧 결과라, 검사가 끝날 때까지 기다린 뒤에 판단한다.
                        // 실제 폰에서는 첫 검사 때 Play 서비스 연결이 1.5초를 넘기는 일이 흔해서, 기다리지 않으면
                        // 검사 중인데도 "사용할 수 없어요"가 뜨고 위험 링크여도 결과 화면으로 넘어가지 않았다.
                        // 검사기는 링크마다 최대 2초에서 끊으므로 그만큼만 기다린다.
                        if (viewModel.isCheckingLinks) {
                            val waitMs = (checkedLinkCount.coerceAtLeast(1) * 2_000L + 1_000L).coerceAtMost(12_000L)
                            withTimeoutOrNull(waitMs) { snapshotFlow { viewModel.isCheckingLinks }.first { !it } }
                        }
                        isChecking = false
                        val level = viewModel.result?.riskLevel
                        if (ok && level != null && level.ordinal >= RiskLevel.WARNING.ordinal) {
                            navController.navigate(Screen.DetectionResult.createRoute())
                        } else {
                            showInlineResult = true
                        }
                    }
                }
            )
        }
    }
}
