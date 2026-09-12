package com.safelink.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * 백그라운드 감지를 켜기 전에 무엇이 필요한지 알리는 안내 (최종 가이드 v1.0).
 *
 * 설정 화면과 홈 화면(실시간 보호 OFF 카드) 두 곳에서 같은 안내를 쓰므로 컴포넌트로 뺐다 —
 * 문구가 갈리면 "어디서 봤느냐에 따라 설명이 다른" 상황이 된다.
 */
@Composable
fun BackgroundDetectionConsentDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("백그라운드 감지를 켤까요?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("켜면 카카오톡·문자 같은 대화 앱의 화면 내용을 기기 안에서 분석해, 위험한 표현이 보이면 알려드립니다.")
                Text("이 기능에는 접근성 권한이 필요합니다. 시스템 설정에서 SafeLink를 켜 주세요. 언제든 같은 자리에서 다시 끌 수 있습니다.")
                Text("감지 결과를 알리려면 알림 권한도 허용되어 있어야 합니다.")
            }
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) { Text("설정으로 이동") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("나중에") }
        }
    )
}
