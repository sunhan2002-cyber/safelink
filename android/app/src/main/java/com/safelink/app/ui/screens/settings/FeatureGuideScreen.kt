package com.safelink.app.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen

/** 기능확장 안내 화면 — 스크린샷 분석/백그라운드 감지 흐름을 한 곳에서 설명한다. */
@Composable
fun FeatureGuideScreen(navController: NavHostController) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "감지 기능 안내", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SafeLinkCard {
                SectionTitle("스크린샷 분석 흐름")
                BodyText("갤러리에서 대화 스크린샷을 선택하면 기기에서 텍스트를 추출한 뒤 기존 분석 엔진으로 위험도를 계산합니다.")
                BodyText("텍스트를 찾지 못하면 입력 화면에서 다시 선택하거나 텍스트 입력 방식으로 전환할 수 있습니다.")
            }

            SafeLinkCard {
                SectionTitle("백그라운드 감지 흐름")
                BodyText("백그라운드 감지는 휴대폰 설정의 '접근성' 메뉴에서 SafeLink를 직접 켠 뒤에만 동작합니다.")
                BodyText("감지된 텍스트는 기기 안에서 점검되고, 경고 이상이면 알림을 보냅니다.")
            }

            SafeLinkCard {
                SectionTitle("알림을 누르면")
                BodyText("그 감지가 저장된 분석 결과 화면이 열립니다. 무엇이 왜 위험한지 먼저 확인한 뒤, 그 화면에서 긴급 도움 요청이나 대응 가이드로 넘어갈 수 있습니다.")
                BodyText("위험도가 긴급이면 결과 화면의 첫 버튼이 긴급 도움 요청으로 바뀝니다.")
            }

            SafeLinkCard {
                SectionTitle("분석 방식")
                BodyText("1차 분석은 기기 안에서 키워드·문장 규칙·상황 규칙을 함께 보고 위험도를 계산합니다.")
                BodyText("2차 AI 보조분석은 1차 분석만으로 애매한 경우에만 추가로 사용합니다.")
            }
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SafeLinkPrimaryButton(
                text = "스크린샷 분석하러 가기",
                onClick = { navController.navigate(Screen.DetectionInput.route) }
            )
            TextButton(
                onClick = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("접근성 설정 열기") }
            TextButton(
                onClick = {
                    navController.navigate(Screen.ResponseGuide.createRoute(RiskLevel.WARNING))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("대응 가이드 예시 보기") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
