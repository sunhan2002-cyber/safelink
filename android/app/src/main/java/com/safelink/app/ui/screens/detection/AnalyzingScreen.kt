package com.safelink.app.ui.screens.detection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.RiskSafe
import kotlinx.coroutines.delay

/** 분석 진행 중 (Figma 20:2) — 데모용 3초 후 결과 화면 이동 */
@Composable
fun AnalyzingScreen(navController: NavHostController) {
    // TODO: 실제 API 연동 시 DetectionViewModel 상태 기반 전환 (Task 6.11)
    LaunchedEffect(Unit) {
        delay(3000)
        navController.navigate(Screen.DetectionResult.route) {
            popUpTo(Screen.Analyzing.route) { inclusive = true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { 0.65f },
                modifier = Modifier.size(140.dp),
                strokeWidth = 10.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "65%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "분석 진행 중",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "AI가 대화 내용을 분석하고 있습니다…",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(24.dp))

        SafeLinkCard {
            StepRow(label = "텍스트 추출", done = true)
            Spacer(modifier = Modifier.height(12.dp))
            StepRow(label = "위험 요소 탐지", done = true)
            Spacer(modifier = Modifier.height(12.dp))
            StepRow(label = "분석 리포트 생성", done = false)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "안전한 결과를 위해 조금만 기다려 주세요.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepRow(label: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (done) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = if (done) "완료" else "진행 중",
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) RiskSafe else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
