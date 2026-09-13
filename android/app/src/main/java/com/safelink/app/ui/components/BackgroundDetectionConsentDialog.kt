package com.safelink.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 백그라운드 감지를 켜기 전에 무엇이 필요한지 알리고, 같은 자리에서 AI 보조분석 동의까지 받는다.
 *
 * ── 왜 한 화면에서 같이 묻는가 ────────────────────────────────────────
 * 예전에는 이 안내에서 접근성 권한만 알리고, AI 보조분석은 설정 목록 안의 별도 토글로만 켤 수 있었다.
 * 그래서 홈에서 "실시간 보호 OFF"를 눌러 보호를 켠 사용자는 AI 보조분석이 있다는 것도,
 * 그게 꺼져 있다는 것도 모른 채 쓰게 됐다 — 물어본 적이 없으니 켜질 리도 없다.
 *
 * 지금은 보호를 켜는 그 자리에서 한 번 묻는다. "허용"을 누르면 AI 보조분석까지 동의한 것으로 보고
 * [onAllow] 를 호출해 접근성 설정 화면으로 이동한다. "거부"는 뒤로가기·바깥 탭과 마찬가지로
 * [onDismiss] 를 호출해 그냥 창만 닫는다 — 아무 상태도 바뀌지 않고 설정 화면으로도 가지 않는다.
 *
 * 그래서 "실시간 보호는 켜되 AI는 쓰지 않기" 조합은 이 다이얼로그에서는 고를 수 없다.
 * 필요하면 설정 탭에서 실시간 보호를 먼저 켠 뒤 AI 토글만 따로 끌 수 있다.
 *
 * 설정 화면과 홈 화면(실시간 보호 OFF 카드) 두 곳에서 같은 안내를 쓰므로 컴포넌트로 뺐다 —
 * 문구가 갈리면 "어디서 봤느냐에 따라 설명이 다른" 상황이 된다.
 */
@Composable
fun BackgroundDetectionConsentDialog(
    onDismiss: () -> Unit,
    onAllow: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            EnableBlurBehindDialog()
            Text("실시간 보호를 켤까요?")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("켜면 카카오톡·문자 같은 대화 앱의 화면 내용을 기기 안에서 분석해, 위험한 표현이 보이면 알려드립니다.")
                Text("이 기능에는 접근성 권한이 필요합니다. 이어지는 시스템 설정에서 SafeLink를 켜 주세요. 언제든 같은 자리에서 다시 끌 수 있습니다.")

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "AI 보조분석도 함께 쓸까요?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "켜면 좋은 점 — 기기 안의 규칙만으로 판단이 애매한 경우에, 대화의 앞뒤 맥락을 보고 " +
                        "규칙에 없던 새로운 수법까지 잡아낼 수 있습니다."
                )
                Text(
                    "대신 이런 점이 있습니다 — 그 경우에 한해 대화 내용이 AI 제공사(Anthropic)로 전송됩니다. " +
                        "여기에는 상대방이 보낸 메시지도 포함됩니다. 전화번호·계좌번호·주민등록번호·카드번호·이메일은 " +
                        "가리고 링크는 도메인만 남겨 보내지만, 금액과 이름은 판단에 필요해 그대로 전송됩니다. " +
                        "인터넷 연결이 필요하고 결과가 나오기까지 몇 초 걸립니다."
                )
                Text(
                    "· SafeLink는 전송된 대화 내용을 따로 수집하거나 보관하지 않습니다\n" +
                        "· 분석 외의 목적으로는 일절 사용하지 않습니다\n" +
                        "· AI 제공사는 이 내용을 모델 학습에 사용하지 않으며, 오·남용 확인 목적으로 일정 기간 보관될 수 있습니다"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onAllow) { Text("허용") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("거부") }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    )
}

/**
 * 이미 켜져 있는 실시간 보호를 끌 때의 확인 (홈 카드 재탭).
 *
 * 접근성 서비스는 앱이 스스로 끌 수 없다 — 시스템 설정에서만 꺼진다.
 * 그래서 "끄기"는 곧 "끄러 갈 수 있게 보내 주기"이고, 그 사실을 먼저 알려 준다.
 */
@Composable
fun BackgroundDetectionDisableDialog(
    onDismiss: () -> Unit,
    onGoToSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("실시간 보호를 끌까요?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("끄면 대화 앱을 지켜보지 않습니다. 위험한 표현이 와도 알림이 오지 않고, 직접 넣은 내용만 검사합니다.")
                Text("끄는 것은 시스템 설정에서만 할 수 있어 접근성 화면으로 이동합니다. SafeLink를 꺼 주세요.")
            }
        },
        confirmButton = {
            TextButton(onClick = onGoToSettings) { Text("끄러 가기") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}
