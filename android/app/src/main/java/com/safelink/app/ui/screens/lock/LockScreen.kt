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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import com.safelink.app.security.AppLockManager
import com.safelink.app.security.BiometricAuth
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlueLight
import com.safelink.app.ui.theme.RiskCritical
import kotlinx.coroutines.delay

/** PIN 입력 잠금 화면 — 저장된 PIN(SHA-256)과 비교해 일치할 때만 홈으로 진입. */
@Composable
fun LockScreen(navController: NavHostController) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var biometricMessage by remember { mutableStateOf<String?>(null) }
    // 남은 입력 차단 시간(ms). 0보다 크면 키패드를 막고 남은 초를 보여준다.
    var lockoutRemaining by remember { mutableLongStateOf(AppLockManager.remainingLockoutMs(context)) }

    val unlock: () -> Unit = {
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Lock.route) { inclusive = true }
            // 잠금 해제 후 홈이 스택에 두 번 쌓이지 않게 한다(앱 복귀 시 잠금은 홈 위에 열린다)
            launchSingleTop = true
        }
    }

    // 차단 중이면 1초 간격으로 남은 시간을 갱신한다(Design.md 5.4 — 5회 실패 시 30초 차단)
    LaunchedEffect(lockoutRemaining > 0) {
        while (lockoutRemaining > 0) {
            delay(1000)
            lockoutRemaining = AppLockManager.remainingLockoutMs(context)
        }
    }

    // 생체인증이 켜져 있으면 화면 진입과 동시에 한 번 시도한다(사용자가 취소하면 PIN 입력으로)
    LaunchedEffect(Unit) {
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        if (AppLockManager.isBiometricEnabled(context) && AppLockManager.remainingLockoutMs(context) == 0L) {
            BiometricAuth.authenticate(
                activity = activity,
                onSuccess = unlock,
                onFailed = { message -> biometricMessage = message }
            )
        }
    }

    // 4자리가 모이면 저장된 PIN과 대조 — 일치 시 홈, 불일치 시 오류 표시 후 초기화
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            if (AppLockManager.verify(context, pin)) {
                unlock()
            } else {
                error = true
                pin = ""
                lockoutRemaining = AppLockManager.remainingLockoutMs(context)
            }
        } else if (pin.isNotEmpty()) {
            error = false
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
        if (lockoutRemaining > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PIN을 여러 번 잘못 입력해 잠시 제한되었습니다. ${(lockoutRemaining + 999) / 1000}초 후 다시 시도해 주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = RiskCritical
            )
        } else if (error) {
            val remainingTries = AppLockManager.MAX_FAIL_COUNT - AppLockManager.failCount(context)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "PIN이 일치하지 않습니다. ${remainingTries}번 더 틀리면 잠시 입력이 제한됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = RiskCritical
            )
        }
        biometricMessage?.let { message ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

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
                            onClick = {
                                val activity = context as? FragmentActivity
                                if (activity != null) {
                                    BiometricAuth.authenticate(
                                        activity = activity,
                                        onSuccess = unlock,
                                        onFailed = { message -> biometricMessage = message }
                                    )
                                }
                            },
                            enabled = lockoutRemaining == 0L && AppLockManager.isBiometricEnabled(context),
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
                            enabled = lockoutRemaining == 0L,
                            modifier = Modifier.size(72.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                contentDescription = "지우기"
                            )
                        }

                        else -> TextButton(
                            onClick = { if (pin.length < 4) pin += key },
                            enabled = lockoutRemaining == 0L,
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
