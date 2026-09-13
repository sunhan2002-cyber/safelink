package com.safelink.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.safelink.app.data.link.LinkRiskResult
import com.safelink.app.data.link.LinkVerdict
import com.safelink.app.ui.theme.RiskCritical

/**
 * 링크 검사 결과 섹션 — [com.safelink.app.ui.screens.detection.DetectionResultScreen]과
 * 링크 전용 검사 화면([com.safelink.app.ui.screens.link.LinkCheckScreen])이 함께 쓴다.
 * "안전합니다"라고 단정하지 않는다 — 차단 목록에 없다는 건 아직 신고되지 않았다는 뜻이라,
 * 그렇게 쓰면 앱이 사용자를 안심시키게 된다.
 *
 * 마지막 줄에서 검사 방식을 밝히는 것도 같은 이유다. 사용자가 받은 링크가 외부로
 * 나가지 않는다는 점은 이 앱에서 약속으로 남아야 하는 정보라 화면에 드러낸다.
 */
@Composable
fun LinkRiskSection(results: List<LinkRiskResult>, isChecking: Boolean) {
    if (results.isEmpty() && !isChecking) return

    EvidenceSectionHeader(icon = Icons.Filled.Link, title = "링크 검사", accent = MaterialTheme.colorScheme.primary)

    if (results.isEmpty()) {
        SafeLinkCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "링크를 확인하고 있어요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    results.forEach { item -> LinkRiskCard(item) }

    Text(
        text = "링크 주소는 외부로 전송되지 않으며, 기기에 저장된 위험 주소 목록과 대조했습니다.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // 구글 Safe Browsing 데이터로 판정한 건이 하나라도 있으면 출처를 밝힌다.
    // 표기 문구와 안내 페이지 링크는 Safe Browsing 이용 조건상 **의무 사항**이라 임의로
    // 번역하거나 생략하지 않는다(검사 자체가 실패해 UNCHECKED 뿐이면 구글 데이터를 쓴 게
    // 아니므로 표기하지 않는다).
    if (results.any { it.verdict != LinkVerdict.UNCHECKED }) {
        val uriHandler = LocalUriHandler.current
        Text(
            text = "Advisory provided by Google",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { uriHandler.openUri(SAFE_BROWSING_ADVISORY_URL) }
        )
    }
}

/** Safe Browsing 안내 페이지 — 위 출처 표기에서 링크해야 하는 주소. */
private const val SAFE_BROWSING_ADVISORY_URL = "https://developers.google.com/safe-browsing/v4/advisory"

@Composable
private fun LinkRiskCard(item: LinkRiskResult) {
    val dangerous = item.verdict == LinkVerdict.DANGEROUS
    val accent = if (dangerous) RiskCritical else MaterialTheme.colorScheme.onSurfaceVariant
    val icon = when (item.verdict) {
        LinkVerdict.DANGEROUS -> Icons.Filled.Warning
        LinkVerdict.NO_MATCH -> Icons.Filled.CheckCircle
        LinkVerdict.UNCHECKED -> Icons.AutoMirrored.Filled.HelpOutline
    }
    val headline = when (item.verdict) {
        LinkVerdict.DANGEROUS -> item.threat?.label ?: "위험한 주소"
        LinkVerdict.NO_MATCH -> "알려진 위험 목록에는 없습니다"
        LinkVerdict.UNCHECKED -> item.uncheckedReason ?: "검사하지 못했습니다"
    }
    val detail = when (item.verdict) {
        LinkVerdict.DANGEROUS -> item.threat?.description ?: "위험한 주소로 신고된 링크입니다."
        LinkVerdict.NO_MATCH -> "아직 신고되지 않은 새 주소일 수 있으니, 모르는 사람이 보낸 링크는 열지 마세요."
        LinkVerdict.UNCHECKED -> "직접 확인이 필요합니다. 모르는 사람이 보낸 링크는 열지 마세요."
    }

    SafeLinkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.titleSmall,
                color = accent,
                fontWeight = if (dangerous) FontWeight.Bold else FontWeight.Normal
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        // 원문에 쓰여 있던 그대로 보여준다 — 정규화한 주소만 보이면 사용자가 자기가 받은
        // 링크와 같은 것인지 알아보지 못한다.
        Text(text = item.link.displayText, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (item.link.obfuscated) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "주소를 일부러 변형해 보낸 링크입니다. 차단을 피하려는 수법입니다.",
                style = MaterialTheme.typography.bodySmall,
                color = RiskCritical
            )
        }
    }
}

@Composable
private fun EvidenceSectionHeader(icon: ImageVector, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = accent)
    }
}
