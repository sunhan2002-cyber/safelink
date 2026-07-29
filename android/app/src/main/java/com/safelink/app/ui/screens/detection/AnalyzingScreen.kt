package com.safelink.app.ui.screens.detection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.RiskSafe

/** 분석 진행 화면(AnalyzingScreen, Figma 20:2) — 최소 표시 시간 동안 진행률 애니메이션.
 *
 * 정리 내용(이전: 블라인드 delay(3000) + 하드코딩 65%/고정 단계):
 *  - 실제 분석 viewModel.analyze() 를 이 화면에서 수행 (입력 화면에서 옮겨옴).
 *    → "분석 중" 화면이 실제로 분석을 수행하는 의미를 갖는다.
 *  - 진행률 0→100%를 애니메이션하고, 임계값에 따라 단계 상태를 실제로 전환.
 *  - 고정 딜레이 대신 최소 표시 시간(ANIM_MS)만 두어 진행 상황을 사용자가 인지하게 함.
 *  - TODO: 서버 연동(비동기) 시 진행률을 실제 응답 상태에 연동 (Task 6.11).
 */
private const val ANIM_MS = 1800

@Composable
fun AnalyzingScreen(
    navController: NavHostController,
    viewModel: DetectionViewModel
) {
    var started by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(
        targetValue = started,
        animationSpec = tween(durationMillis = ANIM_MS),
        label = "analyze-progress"
    )

    LaunchedEffect(Unit) {
        viewModel.analyze()   // 실제 키워드 매칭·점수·위험도·추천기관 계산 (결과는 viewModel.result)
        started = 1f          // 진행률 애니메이션 시작
    }

    // 진행률이 목표치에 도달하면 결과 화면으로 이동
    LaunchedEffect(progress) {
        if (progress >= 1f) {
            navController.navigate(Screen.DetectionResult.route) {
                popUpTo(Screen.Analyzing.route) { inclusive = true }
            }
        }
    }

    val percent = (progress * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(140.dp),
                strokeWidth = 10.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$percent%",
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
            StepRow(label = "텍스트 추출", done = progress >= 0.33f)
            Spacer(modifier = Modifier.height(12.dp))
            StepRow(label = "위험 요소 탐지", done = progress >= 0.66f)
            Spacer(modifier = Modifier.height(12.dp))
            StepRow(label = "분석 리포트 생성", done = progress >= 1f)
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
