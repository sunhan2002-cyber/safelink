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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.repository.RecordItem
import com.safelink.app.ui.components.RiskBadge
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkTopBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 메모 작성 (Figma 미제작 — ScreenUI.md 18번 예정 구성) — 기록 DB에 저장 (Task 7.2) */
@Composable
fun MemoEditScreen(
    navController: NavHostController,
    recordId: String,
    viewModel: RecordListViewModel
) {
    var memo by remember { mutableStateOf("") }
    var record by remember { mutableStateOf<RecordItem?>(null) }

    // 기존 메모가 있으면 불러와 이어서 수정할 수 있게 한다.
    LaunchedEffect(recordId) {
        val loaded = viewModel.findById(recordId)
        record = loaded
        memo = loaded?.memo.orEmpty()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(
            title = "메모 작성",
            onBack = { navController.popBackStack() },
            actions = {
                TextButton(onClick = {
                    viewModel.updateMemo(recordId, memo)
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
                            text = record?.let { memoDateFormat.format(Date(it.timestamp)) } ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = record?.title ?: "기록을 불러오는 중",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    record?.let { RiskBadge(level = it.riskLevel) }
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
                    text = "메모는 서버로 전송되지 않고 이 기기에만 저장됩니다",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val memoDateFormat = SimpleDateFormat("yyyy년 M월 d일 · a h:mm", Locale.KOREA)
