package com.safelink.app.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.safelink.app.background.MessageDetectionService
import com.safelink.app.settings.FeatureToggleState
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.RiskCritical

/** 설정 (Figma 20:1061) — 토글은 로컬 상태. 실제 저장은 EncryptedSharedPreferences (Task 5.15) */
@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var appLock by remember { mutableStateOf(false) }
    var biometric by remember { mutableStateOf(false) }
    // 스크린샷 분석 사용 — 앱 레벨 토글(FeatureToggleState)에 연결해 실제 기능(스크린샷 탭)을 제어
    val screenshotAnalysis by FeatureToggleState.screenshotAnalysisEnabled.collectAsState()
    var backgroundDetection by remember { mutableStateOf(isBackgroundDetectionEnabled(context)) }
    // 백그라운드 감지 켜기 전 동의·권한 안내 다이얼로그 (최종 가이드 v1.0)
    var showBackgroundConsent by remember { mutableStateOf(false) }

    DisposableEffect(context, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                backgroundDetection = isBackgroundDetectionEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showBackgroundConsent) {
        AlertDialog(
            onDismissRequest = { showBackgroundConsent = false },
            title = { Text("백그라운드 감지 사용 안내") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("사용자가 명시적으로 동의하고 허용한 범위의 텍스트를 기기에서 분석합니다.")
                    Text("백그라운드 감지를 사용하려면 접근성 권한이 필요합니다. 설정에서 Safe Link를 켜면 언제든지 해제할 수 있습니다.")
                    Text("감지 결과를 알려드리려면 알림 권한을 허용해 주세요.")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundConsent = false
                    // 접근성 설정 화면으로 이동 (사용자가 직접 Safe Link 켜기)
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                }) { Text("설정으로 이동") }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundConsent = false }) { Text("나중에") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "설정")

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SectionLabel("보안")
            SafeLinkCard {
                ToggleRow(label = "앱 잠금", checked = appLock, onChange = { appLock = it })
                ToggleRow(
                    label = "생체인증 사용",
                    caption = "지문 또는 얼굴 인식으로 잠금 해제",
                    checked = biometric,
                    onChange = { biometric = it }
                )
                LinkRow(label = "PIN 변경") { /* TODO: PIN 변경 흐름 (Task 5.12) */ }
            }

            SectionLabel("긴급 연락처")
            SafeLinkCard {
                LinkRow(label = "긴급 연락처 등록", caption = "등록된 연락처가 없습니다") {
                    // TODO: 이름·전화번호 입력 + 암호화 저장 (Task 5.15)
                }
                LinkRow(label = "긴급 문자 내용 설정", caption = "\"도움이 필요해요. 연락 부탁해요.\"") {
                    // TODO: 긴급 문자 본문 편집
                }
            }

            SectionLabel("알림")
            SafeLinkCard {
                LinkRow(label = "알림 문구 설정", caption = "알림에 표시되는 문구를 바꿀 수 있어요") {
                    // TODO: 중립적 알림 문구 수정 (Task 5.15, Design.md 7장)
                }
            }

            SectionLabel("기능 확장")
            SafeLinkCard {
                ToggleRow(
                    label = "스크린샷 분석 사용",
                    caption = "끄면 대화 분석에서 스크린샷 탭이 숨겨지고 텍스트 입력만 사용합니다.",
                    checked = screenshotAnalysis,
                    onChange = { FeatureToggleState.setScreenshotAnalysisEnabled(it) }
                )
                ToggleRow(
                    label = "백그라운드 감지 설정",
                    caption = "접근성 권한을 허용하면 현재 화면 텍스트를 감지하고 알림으로 안내합니다.",
                    checked = backgroundDetection,
                    onChange = { on ->
                        // 켤 때는 동의·권한 안내 먼저 (동의/권한 없이 활성화 표시 안 함)
                        if (on) {
                            if (!backgroundDetection) showBackgroundConsent = true
                        } else if (backgroundDetection) {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                        } else {
                            backgroundDetection = false
                        }
                    }
                )
                LinkRow(
                    label = "감지 기능 안내",
                    caption = "스크린샷 분석, 감지 후 이동 기준, 접근성 흐름을 확인할 수 있어요"
                ) {
                    navController.navigate(Screen.FeatureGuide.route)
                }
            }

            SectionLabel("개인정보")
            SafeLinkCard {
                Text(
                    text = "데이터 모두 삭제",
                    style = MaterialTheme.typography.bodyLarge,
                    color = RiskCritical,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* TODO: 삭제 확인 다이얼로그 */ }
                        .padding(vertical = 12.dp)
                )
                LinkRow(label = "개인정보 처리방침") { /* TODO */ }
            }

            Text(
                text = "SafeLink v0.1.0",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    caption: String? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkRow(label: String, caption: String? = null, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            if (caption != null) {
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun isBackgroundDetectionEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val serviceName = ComponentName(context, MessageDetectionService::class.java).flattenToString()
    return enabledServices
        .split(':')
        .any { it.equals(serviceName, ignoreCase = true) }
}
