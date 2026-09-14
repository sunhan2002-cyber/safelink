package com.safelink.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.safelink.app.ui.theme.RiskCritical
import com.safelink.app.ui.theme.SurfaceWhite

/** 전역 SOS 플로팅 버튼 — 긴급 도움 요청 화면으로 이동 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
fun SosFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var showHelp by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .size(56.dp)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onLongClickLabel = "SOS 기능 설명 보기",
                onLongClick = { showHelp = true },
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = RiskCritical,
        contentColor = SurfaceWhite,
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = Icons.Filled.Sos, contentDescription = "긴급 도움 요청")
        }
    }

    ActionHelpDialog(
        visible = showHelp,
        title = "SOS 긴급 도움",
        description = "위험을 느끼거나 이미 돈을 보냈을 때 필요한 연락처와 대응 방법을 " +
            "한곳에서 확인할 수 있어요. 보이스피싱·위협은 112에 신고하고, 송금 피해는 " +
            "은행과 경찰에 계좌 지급정지를 요청할 수 있도록 안내해요. 금융감독원 1332와 " +
            "여성긴급전화 1366에 상담을 요청하거나, 미리 등록한 믿을 수 있는 사람에게 " +
            "알릴 수도 있어요. 버튼을 누르는 것만으로 전화나 메시지가 바로 전송되지는 않으며, " +
            "긴급 도움 화면에서 원하는 항목을 직접 선택해 진행해요.",
        onDismiss = { showHelp = false }
    )
}
