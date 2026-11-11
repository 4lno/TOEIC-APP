package com.luudev.vocalens.presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

internal enum class AppGlyph { Home, Book, Camera, Practice, Person, Bell, Flame, Check, Clock, Chevron, Headphones, Mic, Flashcard, Back, Search, Sort, Sound, Trash, Close, Play, Download, Sparkles, Edit, Target, Chart, Settings, Logout }

@Composable
internal fun AppIcon(glyph: AppGlyph, color: Color, modifier: Modifier = Modifier.size(22.dp), description: String? = null) {
    val icon = remember(glyph) {
        ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp,
            viewportWidth = 24f, viewportHeight = 24f).apply {
            val nodes = PathBuilder().apply {
                fun circle(x: Float, y: Float, r: Float) {
                    moveTo(x + r, y)
                    arcTo(r, r, 0f, true, true, x - r, y)
                    arcTo(r, r, 0f, true, true, x + r, y)
                    close()
                }
                when (glyph) {
                    AppGlyph.Sparkles -> {
                        moveTo(10f, 2f); lineTo(12f, 8f); lineTo(18f, 10f); lineTo(12f, 12f)
                        lineTo(10f, 18f); lineTo(8f, 12f); lineTo(2f, 10f); lineTo(8f, 8f); close()
                        moveTo(19f, 15f); lineTo(20f, 18f); lineTo(23f, 19f); lineTo(20f, 20f)
                        lineTo(19f, 23f); lineTo(18f, 20f); lineTo(15f, 19f); lineTo(18f, 18f); close()
                    }
                    AppGlyph.Edit -> {
                        moveTo(4f, 16f); lineTo(16f, 4f); lineTo(20f, 8f); lineTo(8f, 20f)
                        lineTo(3f, 21f); close(); moveTo(13f, 7f); lineTo(17f, 11f)
                    }
                    AppGlyph.Target -> { circle(12f, 12f, 10f); circle(12f, 12f, 6f); circle(12f, 12f, 2f) }
                    AppGlyph.Chart -> {
                        moveTo(3f, 21f); lineTo(22f, 21f); moveTo(6f, 21f); lineTo(6f, 11f)
                        moveTo(12f, 21f); lineTo(12f, 3f); moveTo(18f, 21f); lineTo(18f, 7f)
                    }
                    AppGlyph.Settings -> {
                        circle(12f, 12f, 3f)
                        moveTo(9f, 2f); lineTo(15f, 2f); lineTo(16f, 6f); lineTo(20f, 6f)
                        lineTo(22f, 11f); lineTo(19f, 14f); lineTo(20f, 18f); lineTo(15f, 21f)
                        lineTo(12f, 18f); lineTo(9f, 21f); lineTo(4f, 18f); lineTo(5f, 14f)
                        lineTo(2f, 11f); lineTo(4f, 6f); lineTo(8f, 6f); close()
                    }
                    AppGlyph.Logout -> {
                        moveTo(10f, 3f); lineTo(4f, 3f); lineTo(4f, 21f); lineTo(10f, 21f)
                        moveTo(10f, 12f); lineTo(22f, 12f); moveTo(17f, 7f); lineTo(22f, 12f); lineTo(17f, 17f)
                    }
                    AppGlyph.Play -> {
                        moveTo(7f, 3f); lineTo(20f, 12f); lineTo(7f, 21f); close()
                    }
                    AppGlyph.Download -> {
                        moveTo(12f, 2f); lineTo(12f, 16f)
                        moveTo(6f, 10f); lineTo(12f, 16f); lineTo(18f, 10f)
                        moveTo(4f, 21f); lineTo(20f, 21f)
                    }
                    AppGlyph.Back -> { moveTo(15f, 5f); lineTo(8f, 12f); lineTo(15f, 19f) }
                    AppGlyph.Search -> {
                        circle(10f, 10f, 7f); moveTo(15f, 15f); lineTo(22f, 22f)
                    }
                    AppGlyph.Sort -> {
                        moveTo(3f, 5f); lineTo(21f, 5f)
                        moveTo(7f, 12f); lineTo(17f, 12f)
                        moveTo(10f, 19f); lineTo(14f, 19f)
                    }
                    AppGlyph.Sound -> {
                        moveTo(3f, 9f); lineTo(7f, 9f); lineTo(12f, 5f)
                        lineTo(12f, 19f); lineTo(7f, 15f); lineTo(3f, 15f); close()
                        moveTo(16f, 8f); curveTo(19f, 10f, 19f, 14f, 16f, 16f)
                        moveTo(19f, 4f); curveTo(24f, 8f, 24f, 16f, 19f, 20f)
                    }
                    AppGlyph.Trash -> {
                        moveTo(4f, 6f); lineTo(20f, 6f)
                        moveTo(6f, 6f); lineTo(7f, 21f); lineTo(17f, 21f); lineTo(18f, 6f)
                        moveTo(9f, 6f); lineTo(9f, 3f); lineTo(15f, 3f); lineTo(15f, 6f)
                    }
                    AppGlyph.Close -> {
                        moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f)
                    }
                    AppGlyph.Home -> {
                        moveTo(2f, 11f); lineTo(12f, 3f); lineTo(22f, 11f)
                        moveTo(5f, 9f); lineTo(5f, 21f); lineTo(19f, 21f); lineTo(19f, 9f)
                        moveTo(10f, 21f); lineTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 21f)
                    }
                    AppGlyph.Book, AppGlyph.Flashcard -> {
                        moveTo(6f, 3f); lineTo(18f, 3f); lineTo(18f, 21f); lineTo(6f, 21f)
                        curveTo(3f, 21f, 3f, 18f, 3f, 17f); lineTo(3f, 6f); curveTo(3f, 4f, 4f, 3f, 6f, 3f); close()
                        if (glyph == AppGlyph.Book) {
                            moveTo(7f, 3f); lineTo(7f, 21f)
                        } else {
                            moveTo(8f, 8f); lineTo(14f, 8f); moveTo(8f, 12f); lineTo(12f, 12f)
                        }
                    }
                    AppGlyph.Camera -> {
                        moveTo(3f, 7f); lineTo(8f, 7f); lineTo(10f, 4f); lineTo(14f, 4f)
                        lineTo(16f, 7f); lineTo(21f, 7f); lineTo(21f, 21f); lineTo(3f, 21f); close()
                        circle(12f, 13.5f, 4f)
                    }
                    AppGlyph.Practice -> {
                        moveTo(8f, 4f); lineTo(4f, 4f); lineTo(4f, 21f); lineTo(20f, 21f)
                        lineTo(20f, 4f); lineTo(16f, 4f)
                        moveTo(8f, 3f); lineTo(16f, 3f); lineTo(16f, 6f); lineTo(8f, 6f); close()
                        moveTo(8f, 13f); lineTo(11f, 16f); lineTo(17f, 10f)
                    }
                    AppGlyph.Person -> {
                        circle(12f, 7f, 4f)
                        moveTo(4f, 21f); curveTo(4f, 10f, 20f, 10f, 20f, 21f)
                    }
                    AppGlyph.Bell -> {
                        moveTo(4f, 18f); lineTo(6f, 14f); lineTo(6f, 9f)
                        curveTo(6f, 1f, 18f, 1f, 18f, 9f)
                        lineTo(18f, 14f); lineTo(20f, 18f); close()
                        moveTo(10f, 22f); lineTo(14f, 22f)
                    }
                    AppGlyph.Flame -> {
                        moveTo(12f, 2f); curveTo(13f, 7f, 20f, 10f, 20f, 15f)
                        curveTo(20f, 25f, 4f, 25f, 4f, 15f)
                        curveTo(4f, 10f, 9f, 7f, 12f, 2f); close()
                    }
                    AppGlyph.Check -> { moveTo(6f, 12f); lineTo(10f, 16f); lineTo(18f, 7f) }
                    AppGlyph.Clock -> {
                        circle(12f, 12f, 9f); moveTo(12f, 6f); lineTo(12f, 12f); lineTo(16f, 14f)
                    }
                    AppGlyph.Chevron -> { moveTo(9f, 5f); lineTo(16f, 12f); lineTo(9f, 19f) }
                    AppGlyph.Headphones -> {
                        moveTo(3f, 16f); lineTo(3f, 10f); curveTo(3f, -1f, 21f, -1f, 21f, 10f); lineTo(21f, 16f)
                        moveTo(3f, 12f); lineTo(7f, 12f); lineTo(7f, 21f); lineTo(3f, 21f); close()
                        moveTo(17f, 12f); lineTo(21f, 12f); lineTo(21f, 21f); lineTo(17f, 21f); close()
                    }
                    AppGlyph.Mic -> {
                        moveTo(9f, 5f); curveTo(9f, 0f, 15f, 0f, 15f, 5f); lineTo(15f, 12f)
                        curveTo(15f, 17f, 9f, 17f, 9f, 12f); close()
                        moveTo(5f, 11f); curveTo(5f, 22f, 19f, 22f, 19f, 11f)
                        moveTo(12f, 19f); lineTo(12f, 23f); moveTo(8f, 23f); lineTo(16f, 23f)
                    }
                }
            }.nodes
            addPath(nodes, stroke = SolidColor(Color.White), strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }.build()
    }
    Icon(icon, contentDescription = description, modifier = modifier, tint = color)
}
