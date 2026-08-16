package com.safelink.app.ui.screens.detection

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.settings.FeatureToggleState
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlue
import com.safelink.app.ui.theme.BrandBlueLight
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.SurfaceWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_CHARS = 5000

/** 시연·테스트용 예시 대화 (DetectionResultDummyData.vpCritical 원문과 동일) */
private const val SAMPLE_CONVERSATION =
    "택배기사인데요, 배송 중 확인 차 연락드렸습니다. 그럼 명의 도용 우려가 있어서 " +
        "확인이 필요합니다. 지금 당장 확인 안 하시면 계좌가 압류될 수 있습니다."

/**
 * 대화 분석 입력 화면(DetectionInput).
 * - 텍스트 입력: 원문을 직접 입력/붙여넣기
 * - 스크린샷 업로드: 갤러리에서 스크린샷 선택 → OCR로 텍스트 추출 → 분석
 * 두 경우 모두 최종적으로 originalText 를 공유 ViewModel 에 담아 Analyzing 으로 넘긴다.
 */
@Composable
fun DetectionInputScreen(
    navController: NavHostController,
    viewModel: DetectionViewModel
) {
    val clipboard = LocalClipboardManager.current
    // 스크린샷 분석 사용 토글(설정) — off 면 스크린샷 탭을 숨기고 텍스트 입력만 사용
    val screenshotEnabled by FeatureToggleState.screenshotAnalysisEnabled.collectAsState()
    val isTextMode = viewModel.inputMethod == "텍스트 입력" || !screenshotEnabled

    // 설정에서 스크린샷 분석을 끈 상태로 스크린샷 모드가 남아 있으면 텍스트 입력으로 되돌린다
    LaunchedEffect(screenshotEnabled) {
        if (!screenshotEnabled && viewModel.inputMethod != "텍스트 입력") {
            viewModel.switchToTextInput()
        }
    }

    // 갤러리에서 스크린샷 여러 장 선택 (Android PhotoPicker — 별도 권한 불필요)
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(DetectionViewModel.MAX_IMAGES)
    ) { uris -> if (uris.isNotEmpty()) viewModel.addImages(uris) }

    val canAnalyze =
        if (isTextMode) viewModel.originalText.isNotBlank()
        else viewModel.selectedImages.isNotEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "대화 분석", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 스크린샷 분석 사용 시에만 입력 방식 선택 탭 노출 (off 면 텍스트 입력 전용)
            if (screenshotEnabled) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = !isTextMode,
                        onClick = { viewModel.switchMode("스크린샷 업로드") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text("스크린샷") }
                    SegmentedButton(
                        selected = isTextMode,
                        onClick = { viewModel.switchMode("텍스트 입력") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text("텍스트 입력") }
                }
            }

            if (isTextMode) {
                TextInputArea(
                    text = viewModel.originalText,
                    onTextChange = { if (it.length <= MAX_CHARS) viewModel.originalText = it },
                    onPaste = {
                        clipboard.getText()?.text
                            ?.takeIf { it.isNotBlank() }
                            ?.let { viewModel.originalText = it.take(MAX_CHARS) }
                    },
                    onSample = { viewModel.originalText = SAMPLE_CONVERSATION }
                )
            } else {
                if (viewModel.ocrNoText) {
                    OcrNoTextBanner(
                        message = viewModel.ocrFeedbackMessage
                            ?: "스크린샷에서 텍스트를 찾지 못했어요.",
                        onSwitchToText = { viewModel.switchToTextInput() }
                    )
                }
                ScreenshotAnalysisGuideCard()
                ScreenshotArea(
                    images = viewModel.selectedImages,
                    onPick = {
                        pickImages.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onRemove = { viewModel.removeImage(it) }
                )
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
                            text = "분석에 사용한 내용은 분석 목적 외에는 사용하지 않습니다.",
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
                    text = "스크린샷의 텍스트를 추출해 분석합니다. 텍스트가 잘 보이는 이미지를 선택해 주세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
            }
            SafeLinkPrimaryButton(
                text = "결과 확인하기",
                enabled = canAnalyze,
                onClick = {
                    // 스크린샷 모드면 Analyzing 화면에서 OCR 후 분석까지 수행한다
                    navController.navigate(Screen.Analyzing.route)
                }
            )
        }
    }
}

/** 스크린샷 분석 입력 기준 안내 — OCR 성공률을 높이기 위한 사용자 가이드 */
@Composable
private fun ScreenshotAnalysisGuideCard() {
    SafeLinkCard(containerColor = BrandBlueLight) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "스크린샷 분석 안내",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "선명한 대화 화면을 선택하면 OCR이 더 안정적으로 작동합니다.",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "• 대화 글자가 잘 보이는 이미지를 선택해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• 여러 장을 선택하면 추출된 텍스트를 합쳐서 한 번에 분석합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "• 텍스트를 찾지 못하면 텍스트 입력 방식으로 바로 다시 분석할 수 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TextInputArea(
    text: String,
    onTextChange: (String) -> Unit,
    onPaste: () -> Unit,
    onSample: () -> Unit
) {
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
        SafeLinkOutlinedButton(text = "📋 클립보드에서 붙여넣기", onClick = onPaste)
        TextButton(onClick = onSample, modifier = Modifier.fillMaxWidth()) {
            Text("예시 대화로 테스트하기")
        }
    }
}

/** 스크린샷에서 글자를 인식하지 못했을 때의 안내 배너 */
@Composable
private fun OcrNoTextBanner(
    message: String,
    onSwitchToText: () -> Unit
) {
    SafeLinkCard(containerColor = BrandBlueLight) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "OCR 안내",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SafeLinkOutlinedButton(
                text = "텍스트 입력으로 전환",
                onClick = onSwitchToText
            )
        }
    }
}

@Composable
private fun ScreenshotArea(
    images: List<Uri>,
    onPick: () -> Unit,
    onRemove: (Uri) -> Unit
) {
    val dashColor = BrandBlue
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 업로드 영역 (탭하면 갤러리 열림)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onPick)
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
                text = "최대 ${DetectionViewModel.MAX_IMAGES}장까지 선택 가능합니다",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 선택된 스크린샷 썸네일 목록
        if (images.isNotEmpty()) {
            Text(
                text = "선택된 스크린샷 ${images.size}장",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                images.forEach { uri ->
                    ScreenshotThumb(uri = uri, onRemove = { onRemove(uri) })
                }
            }
        }
    }
}

/** URI에서 다운스케일한 썸네일을 로드해 표시 (Coil 등 외부 라이브러리 없이) */
@Composable
private fun ScreenshotThumb(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeStream(input, null, opts)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Box(modifier = Modifier.size(84.dp)) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandBlueLight),
            contentAlignment = Alignment.Center
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = "선택한 스크린샷",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Image,
                    contentDescription = null,
                    tint = BrandBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        // 삭제 버튼
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(RiskCritical)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "삭제",
                tint = SurfaceWhite,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
