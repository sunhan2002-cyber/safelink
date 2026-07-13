package com.safelink.app.ui.screens.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.data.model.RiskLevel
import com.safelink.app.ui.components.RiskBadge
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen

/** 더미 기록 — 실제로는 Room DB 조회 (Task 7.1) */
private data class RecordItem(
    val id: String,
    val date: String,
    val level: RiskLevel,
    val title: String,
    val summary: String
)

private val dummyRecords = listOf(
    RecordItem(
        id = "r1",
        date = "2026년 7월 10일 · 오후 2:30",
        level = RiskLevel.CRITICAL,
        title = "모르는 번호의 카카오톡 메시지",
        summary = "지인을 사칭하여 금전을 요구하는 전형적인 피싱 문구의 패턴이 발견되었습니다."
    ),
    RecordItem(
        id = "r2",
        date = "2026년 7월 8일 · 오전 10:15",
        level = RiskLevel.SAFE,
        title = "택배 배송 알림 분석",
        summary = "공식 택배사의 안내 문구로 확인되었습니다. 포함된 링크는 공식 홈페이지와 일치합니다."
    ),
    RecordItem(
        id = "r3",
        date = "2026년 7월 5일 · 오후 6:45",
        level = RiskLevel.WARNING,
        title = "해외 결제 승인 문자",
        summary = "정체를 알 수 없는 해외 번호이며, 상담 유도를 위한 허위 결제 정보가 포함되어 있습니다."
    )
)

/** 활동 기록 (Figma 20:213) — 감지·진단 기록 최신순 (Task 7.1) */
@Composable
fun RecordListScreen(navController: NavHostController) {
    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "검사 기록", actions = {
            IconButton(onClick = { /* TODO: 위험도별 필터 */ }) {
                Icon(imageVector = Icons.Filled.FilterList, contentDescription = "필터")
            }
        })

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // TODO: 기록 없을 때 빈 상태 표시 (Room DB 연동 후)
            dummyRecords.forEach { record ->
                SafeLinkCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        RiskBadge(level = record.level)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = record.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = record.summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row {
                        TextButton(onClick = {
                            // 기록 상세 = 분석 결과 화면 재사용 (ScreenFlow.md Open Question #3)
                            navController.navigate(Screen.DetectionResult.route)
                        }) { Text("상세 보기 →") }
                        TextButton(onClick = {
                            navController.navigate(Screen.MemoEdit.createRoute(record.id))
                        }) { Text("메모 작성") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}
