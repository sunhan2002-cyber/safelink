package com.safelink.app.ui.screens.detection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlue
import com.safelink.app.ui.theme.BrandBlueLight

private const val MAX_CHARS = 5000

/** 시연·테스트용 예시 대화 (DetectionResultDummyData.vpCritical 원문과 동일) */
private const val SAMPLE_CONVERSATION =
    "택배기사인데요, 배송 중 확인 차 연락드렸습니다. 그럼 명의 도용 우려가 있어서 " +
        "확인이 필요합니다. 지금 당장 확인 안 하시면 계좌가 압류될 수 있습니다."

/**
 * 대화 분석 입력 화면(DetectionInput) — 입력값 전달 구조 (김선한_02 문서 3-1)
 * 원문 텍스트(originalText)를 공유 DetectionViewModel에 담아 분석 단계로 전달한다.
 * 스크린샷 업로드(OCR)는 이번 주 범위 아님(김선한_01 문서 3장) — 안내만 표시.
 */
@Composable
fun DetectionInputScreen(
    navController: NavHostController,
    viewModel: DetectionViewModel
) {
    val clipboard = LocalClipboardManager.current
    val isTextMode = viewModel.inputMethod == "텍스트 입력"

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "대화 분석", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isTextMode,
                    onClick = { viewModel.inputMethod = "스크린샷 업로드" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("스크린샷 업로드") }
                SegmentedButton(
                    selected = isTextMode,
                    onClick = { viewModel.inputMethod = "텍스트 입력" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("텍스트 입력") }
            }

            if (isTextMode) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = viewModel.originalText,
                        onValueChange = {
                            if (it.length <= MAX_CHARS) viewModel.originalText = it
                        },
                        placeholder = { Text("의심스러운 대화 내용을 붙여넣어 주세요") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                    Text(
                        text = "${"%,d".format(viewModel.originalText.length)} / ${"%,d".format(MAX_CHARS)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SafeLinkOutlinedButton(text = "📋 클립보드에서 붙여넣기", onClick = {
                        clipboard.getText()?.text
                            ?.takeIf { it.isNotBlank() }
                            ?.let { viewModel.originalText = it.take(MAX_CHARS) }
                    })
                    TextButton(
                        onClick = { viewModel.originalText = SAMPLE_CONVERSATION },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text("예시 대화로 테스트하기") }
                }
            } else {
                ScreenshotUploadArea()
            }

            SafeLinkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(text = "개인정보 보호 안내", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "분석된 대화 내용은 익명으로 처리되며 분석 즉시 파기되어 서버에 저장되지 않습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            if (!isTextMode) {
                Text(
                    text = "스크린샷 분석(OCR)은 준비 중입니다. 텍스트 입력을 이용해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
            SafeLinkPrimaryButton(
                text = "분석 시작하기",
                enabled = isTextMode && viewModel.originalText.isNotBlank(),
                onClick = {
                    viewModel.analyze()
                    navController.navigate(Screen.Analyzing.route)
                }
            )
        }
    }
}

@Composable
private fun ScreenshotUploadArea() {
    val dashColor = BrandBlue
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .drawBehind {
                drawRoundRect(
                    color = dashColor,
                    style = Stroke(
                        width = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))
                    ),
                    cornerRadius = CornerRadius(16.dp.toPx())
                )
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .drawBehind { drawCircle(color = BrandBlueLight) },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AddPhotoAlternate,
                contentDescription = null,
                tint = BrandBlue,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "갤러리에서 스크린샷 선택", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "최대 10장까지 선택 가능합니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // TODO: PhotoPicker + OCR — 이번 주 범위 아님 (김선한_01 문서 3장)
    }
}
