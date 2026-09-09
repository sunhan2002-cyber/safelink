package com.safelink.app.ui.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.WifiCalling3
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.R
import com.safelink.app.settings.OnboardingManager
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueLight

@Composable
fun OnboardingScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // 첫인상 화면에서까지 스톡 Material 아이콘(방패)만 쓰던 걸, 스플래시에서만 등장하고
        // 나머지 화면엔 안 쓰이던 브랜드 캐릭터로 교체 - "어디서나 볼 수 있는 기본 아이콘"
        // 인상 대신 이 앱만의 그림체가 첫 화면부터 보이게 함 (UI/UX 1순위)
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(BrandBlueLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.safelink_handshake_mid),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .scale(1.7f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "위험한 대화로부터\n나를 보호하세요",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            // "AI가"가 아니라 온디바이스 규칙 엔진이 핵심 감지를 담당 (CLAUDE.md 원칙,
            // AnalyzingScreen과 동일한 문제 대응 - UI/UX 2순위)
            text = "보이스피싱, 스미싱, 로맨스 스캠을\n실시간으로 감지하고 대응 방법을 안내합니다.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FeatureItem(icon = Icons.Filled.WifiCalling3, label = "전화 감지")
            FeatureItem(icon = Icons.AutoMirrored.Filled.Message, label = "문자 탐지")
            // "AI 분석"이 아니라 온디바이스 위험 신호 분석이 핵심 (위 문구 수정과 동일 사유)
            FeatureItem(icon = Icons.Filled.Psychology, label = "위험 신호 분석")
        }

        Spacer(modifier = Modifier.weight(1f))
        val context = LocalContext.current
        SafeLinkPrimaryButton(text = "시작하기 →", onClick = {
            // 다음 실행부터는 온보딩을 건너뛰도록 완료 표시 (사용자 요청 - 최초 1회만 노출)
            OnboardingManager.markCompleted(context)
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        })
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "시작하면 SafeLink 이용약관에 동의하게 됩니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FeatureItem(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}
