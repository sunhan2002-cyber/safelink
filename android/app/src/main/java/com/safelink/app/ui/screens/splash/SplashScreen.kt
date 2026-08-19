package com.safelink.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.security.AppLockManager
import com.safelink.app.ui.navigation.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    val context = LocalContext.current
    val reveal = remember { Animatable(0f) }
    val handOffset = remember { Animatable(0f) }
    val heartProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // 1) 흐릿한 SafeLink가 선명해진다.
        reveal.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
        )

        // 2) 사슬 없이 맞잡은 손만 위아래로 두 번 흔들린다.
        handOffset.animateTo(-8f, tween(120))
        handOffset.animateTo(7f, tween(170))
        handOffset.animateTo(-5f, tween(140))
        handOffset.animateTo(4f, tween(140))
        handOffset.animateTo(0f, tween(120))

        // 3) 손을 덮는 초록 하트가 오버슈트 후 제자리에 안착한다.
        heartProgress.animateTo(
            targetValue = 1f,
            animationSpec = keyframes {
                durationMillis = 430
                0f at 0
                1.15f at 210 using LinearOutSlowInEasing
                0.92f at 320
                1f at 430
            }
        )

        delay(350)
        val destination = if (AppLockManager.isEnabled(context)) Screen.Lock.route else Screen.Home.route
        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F7FF)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedSafeLinkLogo(
            reveal = reveal.value,
            handOffset = handOffset.value,
            heartProgress = heartProgress.value,
            modifier = Modifier.size(300.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "안전한 연결, 곁에서 함께",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF1E347A),
            fontWeight = FontWeight.Medium
        )
    }
}
