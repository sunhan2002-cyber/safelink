package com.safelink.app.ui.screens.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneDisabled
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.components.color
import com.safelink.app.ui.components.containerColor
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueDark
import com.safelink.app.ui.theme.RiskCritical

/** 대응 가이드 (Figma 20:605) — 위험도별 행동 지침 (Task 6.16) */
@Composable
fun ResponseGuideScreen(navController: NavHostController, riskLevel: RiskLevel) {
    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "대응 가이드", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 긴급 위험도 시 긴급 버튼 상단 고정 (Task 6.16)
            if (riskLevel == RiskLevel.CRITICAL) {
                SafeLinkPrimaryButton(
                    text = "긴급 도움 요청",
                    containerColor = RiskCritical,
                    onClick = { navController.navigate(Screen.Emergency.route) }
                )
            }

            // 히어로
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 8.dp).align(Alignment.CenterHorizontally)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(riskLevel.containerColor(), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = riskLevel.color(),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "보이스피싱 감지 시 대응법",
                    style = MaterialTheme.typography.titleLarge
                )
                // TODO: 위험 유형·위험도별 타이틀/지침 분기 (Task 6.16)
            }

            // 단계별 체크리스트
            StepCard(step = "STEP 01", title = "즉시 전화 차단", icon = Icons.Filled.PhoneDisabled)
            StepCard(step = "STEP 02", title = "URL·링크 차단", icon = Icons.Filled.Block)
            StepCard(step = "STEP 03", title = "개인정보 보호", icon = Icons.Filled.Lock)

            // 왜 사기인가요?
            Text(text = "왜 사기인가요?", style = MaterialTheme.typography.titleMedium)
            SafeLinkCard {
                ReasonRow("공공기관은 카톡/문자로 공문을 보내지 않습니다.")
                ReasonRow("대환대출을 위한 상환 요구는 100% 사기입니다.")
                ReasonRow("출처 불명의 앱(.apk) 설치 유도는 위험합니다.")
            }

            // 긴급 전화 버튼
            SafeLinkPrimaryButton(
                text = "112 전화",
                containerColor = RiskCritical,
                onClick = { /* TODO: ACTION_DIAL 112 (Task 5.7) */ }
            )
            SafeLinkPrimaryButton(
                text = "1332 전화 (금융감독원)",
                containerColor = BrandBlueDark,
                onClick = { /* TODO: ACTION_DIAL 1332 */ }
            )
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SafeLinkPrimaryButton(text = "지원 기관 보기", onClick = {
                navController.navigate(Screen.SupportMatch.route)
            })
            SafeLinkOutlinedButton(text = "메인 화면으로 돌아가기", onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            })
        }
    }
}

@Composable
private fun StepCard(step: String, title: String, icon: ImageVector) {
    SafeLinkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = step,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(text = title, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ReasonRow(text: String) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(text = "•", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
