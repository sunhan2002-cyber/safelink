package com.safelink.app.ui.screens.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.safelink.app.R
import kotlin.math.min

private val SafeGreen = Color(0xFF22A866)

/** 몸통은 고정하고 팔의 굽힘만 다른 이미지 프레임으로 악수를 표현한다. */
@Composable
fun AnimatedSafeLinkLogo(
    reveal: Float,
    handOffset: Float,
    heartProgress: Float,
    modifier: Modifier = Modifier
) {
    val characterFrame = when {
        handOffset < -2f -> R.drawable.safelink_handshake_up
        handOffset > 2f -> R.drawable.safelink_handshake_down
        else -> R.drawable.safelink_handshake_mid
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(characterFrame),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        Image(
            painter = painterResource(R.drawable.safelink_splash_text),
            contentDescription = "SafeLink",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = reveal.coerceIn(0f, 1f)
                    scaleX = 0.96f + reveal.coerceIn(0f, 1f) * 0.04f
                    scaleY = scaleX
                }
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val side = min(size.width, size.height)
            val unit = side / 320f
            val handCenter = Offset(
                x = size.width / 2f,
                y = size.height * 0.665f
            )

            // 캐릭터 움직임이 끝난 뒤 가운데에 초록 하트가 나타난다.
            val heart = heartProgress.coerceIn(0f, 1.15f)
            if (heart > 0f) {
                scale(scale = heart, pivot = handCenter) {
                    val heartPath = Path().apply {
                        moveTo(handCenter.x, handCenter.y + 18f * unit)
                        cubicTo(
                            handCenter.x - 28f * unit,
                            handCenter.y + 2f * unit,
                            handCenter.x - 21f * unit,
                            handCenter.y - 19f * unit,
                            handCenter.x,
                            handCenter.y - 7f * unit
                        )
                        cubicTo(
                            handCenter.x + 21f * unit,
                            handCenter.y - 19f * unit,
                            handCenter.x + 28f * unit,
                            handCenter.y + 2f * unit,
                            handCenter.x,
                            handCenter.y + 18f * unit
                        )
                        close()
                    }
                    drawPath(heartPath, SafeGreen, alpha = heart.coerceAtMost(1f))
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = 2.5f * unit,
                        center = handCenter + Offset(-8f * unit, -7f * unit)
                    )
                }
            }
        }
    }
}
