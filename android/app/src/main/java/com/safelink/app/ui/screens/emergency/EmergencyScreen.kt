package com.safelink.app.ui.screens.emergency

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.settings.EmergencyContactStore
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueDark
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.SurfaceWhite
import com.safelink.app.util.IntentActions

/** 긴급 도움 요청 (Figma 20:987) — 스트레스 상황용 초대형 버튼 (Tasks 5.6~5.9) */
@Composable
fun EmergencyScreen(navController: NavHostController) {
    val context = LocalContext.current
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
            Text(
                text = "지금 바로 도움을 받을 수 있습니다",
                style = MaterialTheme.typography.titleLarge
            )

            EmergencyCallButton(
                title = "112 전화",
                caption = "경찰 신고 · 범죄 신고·긴급 출동 요청",
                color = RiskCritical,
                onClick = { IntentActions.dial(context, "112") }
            )
            EmergencyCallButton(
                title = "1366 전화",
                caption = "여성긴급전화 · 가정폭력·데이트폭력 24시간 상담",
                color = BrandBlueDark,
                onClick = { IntentActions.dial(context, "1366") }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "  지인에게 알리기  ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            val contact = EmergencyContactStore.getContact(context)
            SafeLinkOutlinedButton(
                text = if (contact != null) "${contact.name}에게 긴급 문자 보내기" else "등록된 지인에게 긴급 문자 보내기",
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
            .height(88.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Call,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleLarge, color = SurfaceWhite)
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = SurfaceWhite.copy(alpha = 0.85f)
            )
        }
    }
}
