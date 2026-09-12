package com.safelink.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkTopBar

/**
 * 개인정보 처리방침 — 설정에서 열리는 화면.
 *
 * 예전에는 설정에 항목만 있고 눌러도 아무 일도 일어나지 않았다(TODO). 내용은 **실제 구현에서
 * 확인되는 것만** 적는다. 바뀌면 여기도 같이 고쳐야 한다.
 *  - 원문 저장 위치: [com.safelink.app.data.local.SafeLinkDatabase] (기기 내 앱 전용 DB, 백업 제외)
 *  - 전송 조건: [com.safelink.app.settings.AiConsentStore] 동의가 있을 때의 AI 보조분석
 *  - 링크 검사: [com.safelink.app.data.link.LinkRiskChecker] (주소를 보내지 않고 기기 안에서 대조)
 */
@Composable
fun PrivacyPolicyScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "개인정보 처리방침", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "SafeLink가 어떤 정보를 어디에 두는지 정리했습니다. 회원가입이 없고, 계정이나 연락처를 수집하지 않습니다.",
                style = MaterialTheme.typography.bodyLarge
            )

            PolicySection(
                title = "1. 기기 안에만 저장하는 것",
                body = "분석한 대화 원문, 위험도·점수·감지된 표현, 검사 시각, 메모, 자가진단 결과를 이 기기의 앱 전용 저장소에 저장합니다. " +
                    "기록 화면에서 판단 근거를 다시 볼 수 있게 하기 위해서입니다. 클라우드 백업에서도 제외됩니다."
            )
            PolicySection(
                title = "2. 기기 밖으로 나가는 경우",
                body = "AI 보조분석을 사용할 때만 대화 내용이 AI 제공사(Anthropic)로 전송됩니다. " +
                    "전송 전에 전화번호와 링크는 가립니다. 설정에서 켜지 않으면 자동으로 전송되지 않고, " +
                    "결과 화면에서 직접 요청할 때만 한 건씩 전송됩니다.\n\n" +
                    "SafeLink는 전송된 대화를 따로 수집하거나 보관하지 않습니다. AI 제공사는 이 내용을 모델 학습에 사용하지 않으며, " +
                    "오·남용 확인 목적으로 일정 기간 보관될 수 있습니다."
            )
            PolicySection(
                title = "3. 링크 검사",
                body = "받은 링크 주소는 외부로 보내지 않습니다. 기기에 내려받아 둔 위험 주소 목록과 기기 안에서 대조합니다. " +
                    "그래서 어떤 링크를 받았는지는 외부에 남지 않습니다."
            )
            PolicySection(
                title = "4. 백그라운드 감지",
                body = "접근성 권한을 직접 켠 경우에만 동작합니다. 지정된 메신저 앱의 화면 텍스트를 기기 안에서 분석하고, " +
                    "경고 이상일 때만 알림과 기록을 남깁니다. 권한을 끄면 즉시 중단됩니다."
            )
            PolicySection(
                title = "5. 보관과 삭제",
                body = "저장된 기록은 사용자가 지울 때까지 기기에 남습니다. 기록 화면에서 한 건씩, 설정 > 데이터 모두 삭제에서 전부 지울 수 있습니다. " +
                    "앱을 삭제하면 저장된 내용도 함께 사라집니다."
            )
            PolicySection(
                title = "6. 권한",
                body = "알림 권한은 위험 감지를 알리는 데, 접근성 권한은 백그라운드 감지에, 사진 선택은 스크린샷 분석에 사용합니다. " +
                    "각 권한은 해당 기능에만 쓰이며 언제든 끌 수 있습니다."
            )
            PolicySection(
                title = "7. 문의",
                body = "이 앱은 제22회 한성공학경진대회 출품작(SafeLink 팀)입니다. 문의는 팀에 직접 연락해 주세요."
            )

            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun PolicySection(title: String, body: String) {
    SafeLinkCard {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
