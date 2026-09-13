package com.safelink.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
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
 * 지금은 보호를 켜는 그 자리에서 함께 묻는다. 버튼과 체크박스가 맡는 일이 다르다.
 * - 버튼은 **실시간 보호**에 대한 답이다. [허용] → 켜러 간다([onDecide]), [거부] → 아무것도 켜지 않는다([onDismiss]).
 * - 체크박스는 **AI 보조분석**에 대한 답이다. 체크하고 허용하면 AI 까지, 체크하지 않고 허용하면 기기 안에서만 판단한다.
 *
 * 예전에는 [거부]가 "AI 없이 보호만 켜기"였는데, 버튼 이름이 거부인데 보호는 켜지는 게 어색했다.
 * 동의는 체크박스로 따로 받고, 거부는 말 그대로 거부가 되게 나눴다.
 *
 * 체크박스는 기본적으로 비어 있다(동의를 미리 체크해 두지 않는다). 다만 이미 AI 보조분석에 동의해 둔
 * 사용자라면 [initialAiConsent] 로 체크된 채 연다 — 그렇지 않으면 보호를 다시 켜는 것만으로 모르는 사이에
 * 동의가 철회된다.
 *
 * 설정 화면과 홈 화면(실시간 보호 OFF 카드) 두 곳에서 같은 안내를 쓰므로 컴포넌트로 뺐다 —
 * 문구가 갈리면 "어디서 봤느냐에 따라 설명이 다른" 상황이 된다.
 */
@Composable
fun BackgroundDetectionConsentDialog(
    onDismiss: () -> Unit,
    onDecide: (aiEnabled: Boolean) -> Unit,
    initialAiConsent: Boolean = false
) {
    var aiAgreed by remember { mutableStateOf(initialAiConsent) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            EnableBlurBehindDialog()
            Text("실시간 보호를 켤까요?")
        },
        text = {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("켜면 카카오톡·문자 같은 대화 앱의 화면 내용을 기기 안에서 분석해, 위험한 표현이 보이면 알려드립니다.")
                    Text("이 기능은 휴대폰 설정의 '접근성' 메뉴에서 켭니다. 허용을 누르면 그 화면이 열리니, 목록에서 SafeLink를 찾아 켜 주세요. 끌 때도 같은 곳에서 끌 수 있습니다.")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "AI 보조분석 (선택)",
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
                            "가리고 링크는 사이트 주소 앞부분(예: naver.com)만 남겨 보내지만, 금액과 이름은 판단에 필요해 그대로 전송됩니다. " +
                            "인터넷 연결이 필요하고 결과가 나오기까지 몇 초 걸립니다."
                    )
                    Text(
                        "· SafeLink는 전송된 대화 내용을 따로 수집하거나 보관하지 않습니다\n" +
                            "· 분석 외의 목적으로는 일절 사용하지 않습니다\n" +
                            "· AI 제공사는 이 내용을 모델 학습에 사용하지 않으며, 오·남용 확인 목적으로 일정 기간 보관될 수 있습니다"
                    )
                }
                // 동의 체크박스는 스크롤 밖, 버튼 바로 위에 둔다. 설명이 길어 스크롤 안에 두면
                // 끝까지 내리지 않은 사용자는 체크박스가 있는 줄도 모르고 [허용]을 누르게 된다.
                Spacer(modifier = Modifier.height(12.dp))
                // 글자까지 눌러도 체크되도록 줄 전체를 토글로 만든다(작은 체크박스만 누르기는 어렵다)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = aiAgreed, role = Role.Checkbox, onValueChange = { aiAgreed = it })
                ) {
                    Checkbox(checked = aiAgreed, onCheckedChange = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "위 내용을 확인했고, AI 보조분석 사용에 동의합니다",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "동의하지 않아도 실시간 보호는 켤 수 있습니다. 그때는 기기 안에서만 판단하고, " +
                        "필요할 때 결과 화면에서 한 건씩 직접 요청할 수 있습니다.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onDecide(aiAgreed) }) { Text("허용") }
        },
        dismissButton = {
            // 거부는 실시간 보호도, AI 도 켜지 않는다 — 저장된 동의도 건드리지 않고 창만 닫는다
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
