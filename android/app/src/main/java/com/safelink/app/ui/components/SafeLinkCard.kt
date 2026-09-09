package com.safelink.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 흰색 라운드 카드 — 모든 화면 공용. containerColor로 강조 카드(연파랑 등) 지원 */
@Composable
fun SafeLinkCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = CardDefaults.cardColors(
        containerColor = containerColor ?: MaterialTheme.colorScheme.surface
    )
    // 배경(BackgroundGray)과 카드가 거의 붙어 보이던 문제 대응 — 은은한 그림자로 뜨는 느낌만 추가
    // (너무 진하면 산만해지니 낮은 값 유지, 눌렀을 때는 살짝 더)
    val elevation = CardDefaults.cardElevation(
        defaultElevation = 2.dp,
        pressedElevation = 1.dp
    )
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}
