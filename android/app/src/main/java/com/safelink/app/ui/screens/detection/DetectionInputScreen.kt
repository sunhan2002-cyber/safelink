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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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

/** 대화 분석 입력 — 스크린샷 업로드 / 텍스트 입력 (Figma 20:762, 20:691) */
@Composable
fun DetectionInputScreen(navController: NavHostController) {
    var mode by remember { mutableIntStateOf(0) }
    var text by remember { mutableStateOf("") }

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
                    selected = mode == 0,
                    onClick = { mode = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("스크린샷 업로드") }
                SegmentedButton(
                    selected = mode == 1,
                    onClick = { mode = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("텍스트 입력") }
            }

            if (mode == 0) {
                ScreenshotUploadArea()
            } else {
                TextInputArea(text = text, onTextChange = { if (it.length <= MAX_CHARS) text = it })
            }

            // 프라이버시 보호 안내
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

        // 하단 고정 버튼
        Column(modifier = Modifier.padding(20.dp)) {
            SafeLinkPrimaryButton(
                text = "분석 시작하기",
                enabled = mode == 0 || text.isNotBlank(),
                onClick = { navController.navigate(Screen.Analyzing.route) }
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
            .height(260.dp)
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
        // TODO: PhotoPicker 연동 + 썸네일 목록/삭제 (OCR 채택 시, Open Question #2)
    }
}

@Composable
private fun TextInputArea(text: String, onTextChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            placeholder = { Text("의심스러운 대화 내용을 붙여넣어 주세요") },
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        )
        Text(
            text = "${"%,d".format(text.length)} / ${"%,d".format(MAX_CHARS)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
        SafeLinkOutlinedButton(text = "📋 클립보드에서 붙여넣기", onClick = {
            // TODO: ClipboardManager 연동 (Task 6.9)
        })
    }
}
