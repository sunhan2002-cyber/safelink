package com.safelink.app.ui.screens.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Shield
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.safelink.app.background.MessageDetectionService
import com.safelink.app.settings.AiConsentStore
import com.safelink.app.settings.NotificationTextStore
import com.safelink.app.security.AppLockManager
import com.safelink.app.security.BiometricAuth
import com.safelink.app.settings.EmergencyContactStore
import com.safelink.app.settings.FeatureToggleState
import com.safelink.app.data.repository.RecordRepository
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkTopBar
import kotlinx.coroutines.launch
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BackgroundGray
import com.safelink.app.ui.theme.BrandBlue
import com.safelink.app.ui.theme.BrandBlueLight
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.SurfaceWhite
import com.safelink.app.ui.theme.TextPrimary

/** 설정 (Figma 20:1061) — 토글은 로컬 상태. 실제 저장은 EncryptedSharedPreferences (Task 5.15) */
@Composable
fun SettingsScreen(navController: NavHostController) {
    val deleteScope = rememberCoroutineScope()
    val deleteContext = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deletedMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var appLock by remember { mutableStateOf(AppLockManager.isEnabled(context)) }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var biometric by remember { mutableStateOf(AppLockManager.isBiometricEnabled(context)) }
    var showNotifTextDialog by remember { mutableStateOf(false) }
    var notifCustomEnabled by remember { mutableStateOf(NotificationTextStore.isCustomEnabled(context)) }
    var notifTitleInput by remember { mutableStateOf(NotificationTextStore.title(context)) }
    var notifBodyInput by remember { mutableStateOf(NotificationTextStore.body(context)) }
    // 긴급 연락처 + 긴급 문자 본문 (긴급 화면 SMS 에 실제 사용)
    var contact by remember { mutableStateOf(EmergencyContactStore.getContact(context)) }
    var emergencyMessage by remember { mutableStateOf(EmergencyContactStore.getMessage(context)) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showMessageDialog by remember { mutableStateOf(false) }
    var contactNameInput by remember { mutableStateOf("") }
    var contactPhoneInput by remember { mutableStateOf("") }
    var messageInput by remember { mutableStateOf("") }
    // 스크린샷 분석 사용 — 앱 레벨 토글(FeatureToggleState)에 연결해 실제 기능(스크린샷 탭)을 제어
    val screenshotAnalysis by FeatureToggleState.screenshotAnalysisEnabled.collectAsState()
    var backgroundDetection by remember { mutableStateOf(isBackgroundDetectionEnabled(context)) }
    // 백그라운드 감지 켜기 전 동의·권한 안내 다이얼로그 (최종 가이드 v1.0)
    var showBackgroundConsent by remember { mutableStateOf(false) }

    // 백그라운드 AI 정밀 분석 동의 — 기본 꺼짐, 켤 때 무엇이 전송되는지 보여주고 동의를 받는다
    var aiConsent by remember { mutableStateOf(AiConsentStore.isEnabled(context)) }
    var showAiConsentDialog by remember { mutableStateOf(false) }

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

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; pinInput = "" },
            title = { Text(if (appLock) "PIN 변경" else "앱 잠금 PIN 설정") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("앱을 열 때 입력할 4자리 PIN을 설정하세요.")
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = { if (it.length <= 4 && it.all(Char::isDigit)) pinInput = it },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pinInput.length == 4,
                    onClick = {
                        AppLockManager.setPin(context, pinInput)
                        appLock = true
                        pinInput = ""
                        showPinDialog = false
                    }
                ) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; pinInput = "" }) { Text("취소") }
            }
        )
    }

    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("긴급 연락처 등록") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("긴급 상황에서 문자를 보낼 지인의 이름과 전화번호를 입력하세요.")
                    OutlinedTextField(
                        value = contactNameInput,
                        onValueChange = { contactNameInput = it },
                        singleLine = true,
                        label = { Text("이름") }
                    )
                    OutlinedTextField(
                        value = contactPhoneInput,
                        onValueChange = { if (it.all { c -> c.isDigit() || c == '+' || c == '-' }) contactPhoneInput = it },
                        singleLine = true,
                        label = { Text("전화번호") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = contactNameInput.isNotBlank() && contactPhoneInput.isNotBlank(),
                    onClick = {
                        EmergencyContactStore.setContact(context, contactNameInput, contactPhoneInput)
                        contact = EmergencyContactStore.getContact(context)
                        showContactDialog = false
                    }
                ) { Text("저장") }
            },
            dismissButton = {
                Row {
                    if (contact != null) {
                        TextButton(onClick = {
                            EmergencyContactStore.clearContact(context)
                            contact = null
                            showContactDialog = false
                        }) { Text("삭제", color = RiskCritical) }
                    }
                    TextButton(onClick = { showContactDialog = false }) { Text("취소", color = TextPrimary) }
                }
            }
        )
    }

    if (showMessageDialog) {
        AlertDialog(
            onDismissRequest = { showMessageDialog = false },
            title = { Text("긴급 문자 내용 설정") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("긴급 상황에서 지인에게 보낼 문구를 설정하세요.")
                    OutlinedTextField(
                        value = messageInput,
                        onValueChange = { messageInput = it },
                        label = { Text("문자 내용") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = messageInput.isNotBlank(),
                    onClick = {
                        EmergencyContactStore.setMessage(context, messageInput)
                        emergencyMessage = EmergencyContactStore.getMessage(context)
                        showMessageDialog = false
                    }
                ) { Text("저장") }
            },
            dismissButton = {
                TextButton(onClick = { showMessageDialog = false }) { Text("취소", color = TextPrimary) }
            }
        )
    }

    if (showAiConsentDialog) {
        AlertDialog(
            onDismissRequest = { showAiConsentDialog = false },
            title = { Text(AiConsentStore.CONSENT_TITLE) },
            text = { Text(AiConsentStore.CONSENT_BODY) },
            confirmButton = {
                TextButton(onClick = {
                    AiConsentStore.agree(context)
                    aiConsent = true
                    showAiConsentDialog = false
                }) { Text("동의하고 사용") }
            },
            dismissButton = {
                // 동의하지 않고 닫으면 꺼진 상태 그대로 둔다 (기본값이 꺼짐)
                TextButton(onClick = { showAiConsentDialog = false }) { Text("사용 안 함") }
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
            // 보호 상태 요약 카드 (Figma B10) — 여러 독립 토글 중 앱의 핵심 가치(메신저 실시간
            // 감지)와 가장 직결되는 백그라운드 감지 여부를 기준으로 표시
            SafeLinkCard(containerColor = if (backgroundDetection) BrandBlueLight else BackgroundGray) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (backgroundDetection) BrandBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = SurfaceWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (backgroundDetection) "실시간 보호가 켜져 있어요" else "실시간 보호가 꺼져 있어요",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (backgroundDetection) {
                                "메신저 대화를 백그라운드에서 감지하고 있어요"
                            } else {
                                "수동으로 대화를 분석할 수 있어요. 아래에서 백그라운드 감지를 켜면 자동으로 감지돼요."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            SectionLabel("보안")
            SafeLinkCard {
                ToggleRow(
                    label = "앱 잠금",
                    caption = "앱을 열 때 4자리 PIN을 입력해야 합니다.",
                    checked = appLock,
                    onChange = { on ->
                        if (on) {
                            showPinDialog = true // PIN 설정을 마쳐야 실제로 켜진다
                        } else {
                            AppLockManager.disable(context)
                            appLock = false
                        }
                    }
                )
                ToggleRow(
                    label = "생체인증 사용",
                    caption = when {
                        !appLock -> "앱 잠금을 먼저 켜 주세요"
                        !BiometricAuth.isAvailable(context) -> "이 기기에 등록된 지문·얼굴이 없습니다"
                        else -> "지문 또는 얼굴 인식으로 잠금 해제"
                    },
                    checked = biometric,
                    onChange = { enabled ->
                        // 앱 잠금이 꺼져 있거나 등록된 생체정보가 없으면 켤 수 없다
                        if (enabled && (!appLock || !BiometricAuth.isAvailable(context))) return@ToggleRow
                        AppLockManager.setBiometricEnabled(context, enabled)
                        biometric = AppLockManager.isBiometricEnabled(context)
                    }
                )
                LinkRow(
                    label = "PIN 변경",
                    caption = if (appLock) "저장된 PIN을 새로 설정합니다" else "앱 잠금을 먼저 켜 주세요"
                ) {
                    if (appLock) showPinDialog = true
                }
            }

            SectionLabel("긴급 연락처")
            SafeLinkCard {
                LinkRow(
                    label = "긴급 연락처 등록",
                    caption = contact?.let { "${it.name} · ${it.phone}" } ?: "등록된 연락처가 없습니다"
                ) {
                    contactNameInput = contact?.name.orEmpty()
                    contactPhoneInput = contact?.phone.orEmpty()
                    showContactDialog = true
                }
                LinkRow(label = "긴급 문자 내용 설정", caption = "\"$emergencyMessage\"") {
                    messageInput = emergencyMessage
                    showMessageDialog = true
                }
            }

            SectionLabel("알림")
            SafeLinkCard {
                LinkRow(
                    label = "알림 문구 설정",
                    caption = if (notifCustomEnabled) {
                        "현재: \"${NotificationTextStore.title(context)}\" — 위험 내용을 감춘 문구로 표시됩니다"
                    } else {
                        "알림에 표시되는 문구를 바꿀 수 있어요"
                    }
                ) {
                    notifCustomEnabled = NotificationTextStore.isCustomEnabled(context)
                    notifTitleInput = NotificationTextStore.title(context)
                    notifBodyInput = NotificationTextStore.body(context)
                    showNotifTextDialog = true
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
                ToggleRow(
                    label = "백그라운드 AI 정밀 분석",
                    caption = "켜면 판단이 애매한 경우 대화 내용이 AI 제공사(Anthropic)로 전송됩니다. 끄면 기기 안에서만 판단합니다.",
                    checked = aiConsent,
                    onChange = { on ->
                        // 켤 때만 동의 화면을 띄운다. 끄는 건 즉시 반영(동의 철회에 확인을 요구하지 않는다).
                        if (on) {
                            showAiConsentDialog = true
                        } else {
                            AiConsentStore.revoke(context)
                            aiConsent = false
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
                        .clickable { showDeleteDialog = true }
                        .padding(vertical = 12.dp)
                )
                LinkRow(label = "개인정보 처리방침") { /* TODO */ }
            }

            if (showNotifTextDialog) {
                AlertDialog(
                    onDismissRequest = { showNotifTextDialog = false },
                    title = { Text("알림 문구 설정") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "다른 사람이 화면을 함께 볼 수 있는 상황이라면, 위험 내용을 드러내지 않는 문구로 바꿀 수 있습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ToggleRow(
                                label = "중립 문구 사용",
                                caption = "끄면 감지된 표현이 알림에 그대로 표시됩니다",
                                checked = notifCustomEnabled,
                                onChange = { notifCustomEnabled = it }
                            )
                            OutlinedTextField(
                                value = notifTitleInput,
                                onValueChange = { notifTitleInput = it },
                                label = { Text("알림 제목") },
                                singleLine = true,
                                enabled = notifCustomEnabled,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = notifBodyInput,
                                onValueChange = { notifBodyInput = it },
                                label = { Text("알림 내용") },
                                singleLine = true,
                                enabled = notifCustomEnabled,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            NotificationTextStore.save(
                                context,
                                enabled = notifCustomEnabled,
                                title = notifTitleInput,
                                body = notifBodyInput
                            )
                            notifCustomEnabled = NotificationTextStore.isCustomEnabled(context)
                            showNotifTextDialog = false
                        }) { Text("저장") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNotifTextDialog = false }) { Text("취소") }
                    }
                )
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("데이터를 모두 삭제할까요?") },
                    text = {
                        Text("이 기기에 저장된 검사 기록과 메모가 모두 지워집니다. 삭제한 기록은 되돌릴 수 없습니다.")
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            deleteScope.launch {
                                runCatching { RecordRepository(deleteContext).deleteAll() }
                                deletedMessage = "검사 기록을 모두 삭제했습니다."
                            }
                        }) { Text("삭제", color = RiskCritical) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
                    }
                )
            }

            deletedMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
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
