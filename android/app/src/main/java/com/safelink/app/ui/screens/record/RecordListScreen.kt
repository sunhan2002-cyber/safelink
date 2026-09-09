package com.safelink.app.ui.screens.record

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.R
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
                // UI/UX 2순위(카드 남용 줄이기) - 기록마다 따로 흰 카드로 감싸 카드가
                // 끝없이 반복되던 걸, 설정 화면과 같은 패턴(하나의 카드 안에 구분선으로
                // 행 나누기)으로 바꿔 "전부 카드" 단조로움을 줄임
                SafeLinkCard {
                    records.forEachIndexed { index, record ->
                        RecordRow(record = record, navController = navController)
                        if (index != records.lastIndex) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }
                // 기록이 적을 때 화면 아래쪽이 텅 빈 회색으로 남아 휑해 보이던 문제 대응
                // (UI/UX 리뷰 중 발견) - 정기 검사를 유도하는 짧은 팁으로 채움
                if (records.size < 3) {
                    RegularCheckTip()
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
            // UI/UX 2순위 - 텍스트만 있던 빈 상태에 아이콘 추가 (휑해 보이던 문제 대응).
            // UI/UX 1순위(AI스러운 인상 대응) - 진짜 "아직 기록 자체가 없는" 첫 상태는
            // 스톡 아이콘 대신 브랜드 캐릭터로, 필터 결과가 없는 경우는 검색류 빈 상태라
            // 캐릭터를 쓰면 어색해서 기존 아이콘 유지 (상황에 안 맞는 자산 남용 방지)
            if (isFiltered) {
                Icon(
                    imageVector = Icons.Filled.Inbox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Image(
                    painter = painterResource(R.drawable.safelink_handshake_mid),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
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

/** 기록이 적을 때(3개 미만) 리스트 아래 빈 공간을 정기 검사 팁으로 채운다. */
@Composable
private fun RegularCheckTip() {
    SafeLinkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = "의심스러운 대화를 받으면 바로 검사해보세요. 기록이 쌓일수록 반복되는 위험 패턴을 알아차리기 쉬워져요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** 기록 한 건 — 이제 개별 카드가 아니라 [SafeLinkCard] 안의 한 행(구분선으로 다음 행과 분리). */
@Composable
private fun RecordRow(record: RecordItem, navController: NavHostController) {
    Column {
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
