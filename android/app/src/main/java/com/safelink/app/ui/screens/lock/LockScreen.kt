package com.safelink.app.ui.screens.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueLight

/** PIN 입력 잠금 화면 — UI 뼈대. 실제 PIN 검증·차단 로직은 Tasks 5.11~5.12 */
@Composable
fun LockScreen(navController: NavHostController) {
    var pin by remember { mutableStateOf("") }

    // TODO: SHA-256 해시 비교 (Task 5.12). 지금은 4자리 입력 시 통과.
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Lock.route) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = "PIN 번호를 입력하세요", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                Spacer(
                    modifier = Modifier
                        .size(18.dp)
                        .background(
                            color = if (index < pin.length) MaterialTheme.colorScheme.primary
                            else BrandBlueLight,
                            shape = CircleShape
                        )
                )
            }
        }
        // TODO: 오류 시 도트 흔들림 + "PIN이 일치하지 않습니다 (n/5회)", 5회 오류 30초 차단

        Spacer(modifier = Modifier.weight(1f))

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("bio", "0", "back")
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                row.forEach { key ->
                    when (key) {
                        "bio" -> IconButton(
                            onClick = { /* TODO: BiometricPrompt (Task 5.13) */ },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fingerprint,
                                contentDescription = "생체인증",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        "back" -> IconButton(
                            onClick = { pin = pin.dropLast(1) },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                contentDescription = "지우기"
                            )
                        }

                        else -> TextButton(
                            onClick = { if (pin.length < 4) pin += key },
                            modifier = Modifier.size(72.dp)
                        ) {
                            Text(text = key, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
