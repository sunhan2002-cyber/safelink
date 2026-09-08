package com.safelink.app.ui.screens.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.data.repository.RecordItem
import com.safelink.app.data.repository.RecordType
import com.safelink.app.ui.components.RiskBadge
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 검사 기록 (Figma 20:213) — 감지·분석·자가진단 기록 최신순 (Task 7.1).
 *
 * 기록은 기기 내 DB에서 실시간으로 흘러오며, 새 분석/진단을 하면 자동으로 목록에 추가된다.
 */
@Composable
fun RecordListScreen(
    navController: NavHostController,
    viewModel: RecordListViewModel
) {
    val records by viewModel.records.collectAsState()
    val filter by viewModel.filter.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "검사 기록", actions = {
            IconButton(onClick = { menuOpen = true }) {
                Icon(imageVector = Icons.Filled.FilterList, contentDescription = "위험도별 필터")
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                FilterMenuItem("전체", filter == null) {
                    viewModel.setFilter(null); menuOpen = false
                }
                listOf(RiskLevel.CRITICAL, RiskLevel.WARNING, RiskLevel.CAUTION, RiskLevel.SAFE)
                    .forEach { level ->
                        FilterMenuItem(level.label, filter == level) {
                            viewModel.setFilter(level); menuOpen = false
                        }
                    }
            }
        })

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (records.isEmpty()) {
                EmptyRecords(isFiltered = filter != null)
            } else {
                records.forEach { record ->
                    RecordCard(record = record, navController = navController)
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun FilterMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = {
            Text(
                text = if (selected) "✓ $label" else label,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        onClick = onClick
    )
}

/** 기록이 없을 때 — 왜 비어 있는지, 무엇을 하면 쌓이는지 함께 안내한다. */
@Composable
private fun EmptyRecords(isFiltered: Boolean) {
    SafeLinkCard {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Text(
                text = if (isFiltered) "해당 위험도의 기록이 없습니다" else "아직 검사 기록이 없습니다",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isFiltered) {
                    "필터를 '전체'로 바꾸면 모든 기록을 볼 수 있어요."
                } else {
                    "대화 분석이나 자가 진단을 하면 결과가 이곳에 자동으로 저장됩니다."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RecordCard(record: RecordItem, navController: NavHostController) {
    SafeLinkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatTimestamp(record.timestamp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            RiskBadge(level = record.riskLevel)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = record.title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = record.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!record.memo.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "메모: ${record.memo}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row {
            // 자가진단 기록은 분석 결과 화면 구조와 달라 상세 보기를 제공하지 않는다.
            if (record.type == RecordType.DETECTION) {
                TextButton(onClick = {
                    navController.navigate(Screen.DetectionResult.createRoute(record.id))
                }) { Text("상세 보기 →") }
            }
            TextButton(onClick = {
                navController.navigate(Screen.MemoEdit.createRoute(record.id))
            }) { Text(if (record.memo.isNullOrBlank()) "메모 작성" else "메모 수정") }
        }
    }
}

private val recordDateFormat = SimpleDateFormat("yyyy년 M월 d일 · a h:mm", Locale.KOREA)

private fun formatTimestamp(timestamp: Long): String = recordDateFormat.format(Date(timestamp))
