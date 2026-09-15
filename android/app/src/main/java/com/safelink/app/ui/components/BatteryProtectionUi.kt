package com.safelink.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.safelink.app.settings.BatteryProtection
import com.safelink.app.ui.theme.RiskCaution
import com.safelink.app.ui.theme.RiskCautionContainer
import com.safelink.app.ui.theme.TextPrimary

/**
 * 절전 때문에 실시간 보호가 멈출 수 있다고 알리고 해제 창으로 보내는 확인 창.
 * 실시간 보호를 켜고 앱으로 돌아왔을 때, 그리고 홈·설정의 경고 줄을 눌렀을 때 뜬다.
 */
@Composable
fun BatteryProtectionDialog(onDismiss: () -> Unit, onOpenSettings: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(BatteryProtection.DIALOG_TITLE) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(BatteryProtection.DIALOG_BODY)
                if (BatteryProtection.isSamsung) {
                    Text(
                        BatteryProtection.SAMSUNG_NOTE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onOpenSettings) { Text("설정하기", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("나중에", color = TextPrimary) } }
    )
}

/** 실시간 보호는 켜져 있지만 절전 제한이 걸려 있을 때 보이는 노란 경고 줄. 누르면 [BatteryProtectionDialog]. */
@Composable
fun BatteryProtectionWarning(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(RiskCautionContainer, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.BatteryAlert, contentDescription = null, tint = RiskCaution, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("보호가 멈출 수 있어요", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Text(
                "휴대폰 절전 때문에 SafeLink가 멈출 수 있어요",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text("설정하기", style = MaterialTheme.typography.labelLarge, color = RiskCaution, fontWeight = FontWeight.Bold)
    }
}
