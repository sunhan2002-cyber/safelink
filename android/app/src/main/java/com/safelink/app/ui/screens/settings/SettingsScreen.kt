package com.safelink.app.ui.screens.settings

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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.theme.RiskCritical

/** 설정 (Figma 20:1061) — 토글은 로컬 상태. 실제 저장은 EncryptedSharedPreferences (Task 5.15) */
@Composable
fun SettingsScreen(navController: NavHostController) {
    var appLock by remember { mutableStateOf(false) }
    var biometric by remember { mutableStateOf(false) }
    var screenshotAnalysis by remember { mutableStateOf(true) }
    var backgroundDetection by remember { mutableStateOf(false) }

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
                    caption = "갤러리에서 선택한 대화 스크린샷을 분석할 수 있어요",
                    checked = screenshotAnalysis,
                    onChange = { screenshotAnalysis = it }
                )
                ToggleRow(
                    label = "백그라운드 감지 준비",
                    caption = "위험 신호 감지 기능의 사용 여부를 추후 이곳에서 관리해요",
                    checked = backgroundDetection,
                    onChange = { backgroundDetection = it }
                )
                LinkRow(
                    label = "감지 기능 안내",
                    caption = "스크린샷 분석과 백그라운드 감지 흐름을 확인할 수 있어요"
                ) {
                    // TODO: 기능확장 안내 화면 또는 도움말 연결
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
