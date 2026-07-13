package com.safelink.app.ui.screens.record

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.RiskBadge
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkTopBar

/** 메모 작성 (Figma 미제작 — ScreenUI.md 18번 예정 구성) — 저장은 Room Memo 엔티티 (Task 7.2) */
@Composable
fun MemoEditScreen(navController: NavHostController, recordId: String) {
    var memo by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(
            title = "메모 작성",
            onBack = { navController.popBackStack() },
            actions = {
                TextButton(onClick = {
                    // TODO: Room 저장 (Task 7.2). 미저장 이탈 확인 다이얼로그도 추가.
                    navController.popBackStack()
                }) { Text("저장") }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 연결 기록 카드
            SafeLinkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "2026년 7월 10일",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "보이스피싱 의심 대화 분석",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    RiskBadge(level = RiskLevel.CRITICAL)
                }
            }

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                placeholder = { Text("상황에 대해 기록하고 싶은 내용을 자유롭게 적어 주세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "메모는 암호화되어 이 기기에만 저장됩니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
