package com.safelink.app.ui.screens.emergency

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.settings.EmergencyContactStore
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueDark
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.SurfaceWhite
import com.safelink.app.util.IntentActions

/** 긴급 도움 요청 (Figma B07) — 스트레스 상황용 초대형 버튼 (Tasks 5.6~5.9) */
@Composable
fun EmergencyScreen(navController: NavHostController) {
    val context = LocalContext.current
    // 긴급 배지가 계속 반짝이며 퍼지는 느낌을 주는 펄스 링 — 이 화면은 항상 긴급 상태를
    // 보여주는 화면이라(홈과 달리 조건부 아님) 계속 반복 재생(사용자 요청).
    val pulse = rememberInfiniteTransition(label = "emergency-pulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-scale"
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse-alpha"
    )
    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(
            title = "긴급 도움 요청",
            onBack = { navController.popBackStack() },
            useCloseIcon = true
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                // 라이트 테마는 유지하되 원·글씨를 한층 더 키움(사용자 요청, 2차 확대) +
                // 빨간 원 밖으로 계속 반짝이며 퍼지는 펄스 링을 더해 위급함을 강조(3차 요청)
                Box(
                    modifier = Modifier.size(176.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .scale(pulseScale)
                            .background(RiskCritical.copy(alpha = pulseAlpha), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(128.dp)
                            .background(RiskCritical, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PriorityHigh,
                            contentDescription = null,
                            tint = SurfaceWhite,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.size(16.dp))
                Text(
                    text = "이미 송금했거나 위협을 받고 있나요?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text = "혼자 해결하려 하지 말고 아래 기관에 바로 연락하세요.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            EmergencyCallButton(
                number = "112",
                title = "경찰 신고",
                caption = "신변 위협 · 보이스피싱 즉시 신고",
                color = RiskCritical,
                onClick = { IntentActions.dial(context, "112") }
            )
            EmergencyCallButton(
                number = "1366",
                title = "여성긴급전화",
                caption = "가정폭력·데이트폭력 24시간 상담",
                color = BrandBlueDark,
                onClick = { IntentActions.dial(context, "1366") }
            )

            val contact = EmergencyContactStore.getContact(context)
            SafeLinkPrimaryButton(
                text = if (contact != null) "${contact.name}에게 긴급 문자 보내기" else "믿을 수 있는 사람에게 알리기",
                onClick = {
                    if (contact != null) {
                        // 문자 앱에 수신번호+사전 문구를 채워 열어준다(자동 전송 아님)
                        IntentActions.sendSms(context, contact.phone, EmergencyContactStore.getMessage(context))
                    } else {
                        navController.navigate(Screen.Settings.route)
                    }
                }
            )
            Text(
                text = if (contact != null) "미리 작성된 문구가 문자 앱에 채워집니다"
                else "먼저 설정에서 긴급 연락처를 등록해 주세요",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = "위험하면 안전한 장소로 먼저 이동하세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }

        TextButton(
            onClick = { navController.navigate(Screen.Settings.route) },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 20.dp)
        ) {
            Text("긴급 연락처가 없다면 설정에서 등록하세요")
        }
    }
}

@Composable
private fun EmergencyCallButton(
    number: String,
    title: String,
    caption: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = SurfaceWhite,
            modifier = Modifier.width(84.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = SurfaceWhite
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyLarge,
                color = SurfaceWhite.copy(alpha = 0.85f)
            )
        }
    }
}
