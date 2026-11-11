package com.luudev.vocalens.presentation.welcome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luudev.vocalens.R
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import com.luudev.vocalens.presentation.components.BrandHeader

private val Blue = Color(0xFF2864FF)
private val Ink = Color(0xFF151D30)
private val Muted = Color(0xFF7183A0)

@Composable
fun WelcomeScreen(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFFEFF6FF), Color(0xFFF7F9FB)))
    )) {
        AmbientBackground(Modifier.fillMaxSize())
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandHeader()
            Spacer(Modifier.height(26.dp))
            PhoneIllustration(Modifier.widthIn(max = 350.dp).fillMaxWidth().height(264.dp))
            Spacer(Modifier.height(22.dp))
            Text(stringResource(R.string.welcome_eyebrow), color = Blue,
                fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.welcome_title), color = Ink, fontSize = 30.sp,
                lineHeight = 36.sp, letterSpacing = (-1.2).sp, textAlign = TextAlign.Center)
            Text(stringResource(R.string.welcome_title_accent), color = Blue, fontSize = 30.sp,
                lineHeight = 36.sp, letterSpacing = (-1.2).sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.welcome_description), color = Muted,
                fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 340.dp))
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onStartClick,
                modifier = Modifier.widthIn(max = 400.dp).fillMaxWidth()
                    .height(54.dp).shadow(14.dp, RoundedCornerShape(16.dp),
                        ambientColor = Blue.copy(alpha = 0.25f), spotColor = Blue.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2560EF))
            ) {
                Text(stringResource(R.string.welcome_start), fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.width(12.dp))
                LineIcon(Glyph.Chevron, Color.White, Modifier.size(18.dp))
            }
            Spacer(Modifier.height(13.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LineIcon(Glyph.Shield, Color(0xFF8FA2BD), Modifier.size(12.dp))
                Spacer(Modifier.width(5.dp))
                Text(stringResource(R.string.welcome_safety), color = Color(0xFF8FA2BD),
                    fontSize = 10.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun AmbientBackground(modifier: Modifier) {
    Canvas(modifier) {
        drawCircle(Brush.radialGradient(listOf(Color(0xFFC9E1FF).copy(alpha = 0.85f),
            Color(0xFFC9E1FF).copy(alpha = 0f)), center = Offset(size.width, size.height * 0.25f),
            radius = size.width * 0.34f), size.width * 0.34f,
            Offset(size.width, size.height * 0.25f))
        drawCircle(Brush.radialGradient(listOf(Color(0xFF9CF5E4).copy(alpha = 0.9f),
            Color(0xFF9CF5E4).copy(alpha = 0f)), center = Offset(-12.dp.toPx(), size.height * 0.43f),
            radius = 78.dp.toPx()), 78.dp.toPx(), Offset(-12.dp.toPx(), size.height * 0.43f))
    }
}

@Composable
private fun PhoneIllustration(modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.offset(y = 1.dp).rotate(-4.5f).size(180.dp, 246.dp)
                .shadow(25.dp, RoundedCornerShape(36.dp),
                    ambientColor = Color(0xFF719AC7).copy(alpha = 0.3f),
                    spotColor = Color(0xFF719AC7).copy(alpha = 0.3f))
                .background(Color(0xFF192855), RoundedCornerShape(36.dp)).padding(7.dp)
                .clip(RoundedCornerShape(29.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF2666EE), Color(0xFF10796F))))
        ) {
            Box(Modifier.align(Alignment.TopCenter).size(58.dp, 14.dp)
                .background(Color(0xFF192855), RoundedCornerShape(bottomStart = 13.dp, bottomEnd = 13.dp)))
            Box(Modifier.align(Alignment.Center).offset(y = 12.dp).size(124.dp, 162.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    drawRoundRect(Color(0xFF9FD8F4), topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                        size = Size(size.width - 2.dp.toPx(), size.height - 2.dp.toPx()),
                        cornerRadius = CornerRadius(23.dp.toPx()),
                        style = Stroke(1.7.dp.toPx(), pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(6.dp.toPx(), 4.dp.toPx()))))
                }
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    LineIcon(Glyph.Camera, Color.White, Modifier.size(36.dp))
                    Spacer(Modifier.height(18.dp))
                    Text(stringResource(R.string.welcome_camera_caption), color = Color.White,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp,
                        textAlign = TextAlign.Center)
                }
            }
        }
        Surface(modifier = Modifier.align(Alignment.TopStart).offset(x = 4.dp, y = 49.dp),
            shape = RoundedCornerShape(14.dp), color = Color.White,
            shadowElevation = 15.dp, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDEEBFF))) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                Text(stringResource(R.string.welcome_hello), color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.welcome_translation), color = Muted, fontSize = 10.sp)
            }
        }
        Surface(modifier = Modifier.align(Alignment.BottomEnd).offset(y = (-53).dp),
            shape = RoundedCornerShape(14.dp), color = Color.White,
            shadowElevation = 15.dp, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDEEBFF))) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                LineIcon(Glyph.Sound, Blue, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.welcome_pronunciation), color = Ink,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private enum class Glyph { Camera, Sound, Shield, Chevron }

// Decorative vector strokes keep the illustration crisp at every screen density.
@Composable
private fun LineIcon(glyph: Glyph, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val stroke = Stroke(width = 1.9f, cap = StrokeCap.Round)
            fun line(vararg points: Float) {
                val path = Path().apply {
                    moveTo(points[0], points[1])
                    for (i in 2 until points.size step 2) lineTo(points[i], points[i + 1])
                }
                drawPath(path, color, style = stroke)
            }
            when (glyph) {
                Glyph.Camera -> {
                    line(3f, 7f, 8f, 7f, 9.5f, 4f, 15f, 4f, 16.5f, 7f, 21f, 7f,
                        21f, 21f, 3f, 21f, 3f, 7f)
                    drawCircle(color, 4.4f, Offset(12f, 13.5f), style = stroke)
                }
                Glyph.Chevron -> line(9f, 5f, 16f, 12f, 9f, 19f)
                Glyph.Shield -> {
                    line(12f, 2f, 20f, 6f, 19f, 14f, 16f, 19f, 12f, 22f,
                        8f, 19f, 5f, 14f, 4f, 6f, 12f, 2f)
                    line(8f, 12f, 11f, 15f, 16f, 9f)
                }
                Glyph.Sound -> {
                    line(3f, 9f, 7f, 9f, 12f, 5f, 12f, 19f, 7f, 15f, 3f, 15f, 3f, 9f)
                    drawArc(color, -55f, 110f, false, Offset(10f, 6f), Size(10f, 12f), style = stroke)
                    drawArc(color, -55f, 110f, false, Offset(10f, 2f), Size(16f, 20f), style = stroke)
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Preview(showBackground = true, widthDp = 320, heightDp = 640)
@Composable
private fun WelcomeScreenPreview() {
    VocaLensTheme(darkTheme = false, dynamicColor = false) {
        WelcomeScreen(onStartClick = {})
    }
}
