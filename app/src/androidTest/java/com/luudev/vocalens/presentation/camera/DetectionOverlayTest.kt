package com.luudev.vocalens.presentation.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.luudev.vocalens.domain.model.ObjectBounds
import com.luudev.vocalens.domain.model.RecognizedObject
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class DetectionOverlayTest {
    @get:Rule val compose = createComposeRule()

    @Test fun labelsStayInsideFittedPhotoWithLargeFontsAndEdgeDetectionsAndRemainClickable() {
        val detections = listOf(
            RecognizedObject(1, "Very long vocabulary prediction near the edge", .91f, ObjectBounds(.85f, .02f, 1f, .25f)),
            RecognizedObject(2, "Bus", .87f, ObjectBounds(0f, .8f, .12f, 1f))
        )
        var selected: RecognizedObject? = null
        var portrait by mutableStateOf(false)
        compose.setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, 1.6f)) {
                    DetectionOverlay(if (portrait) 320 else 640, if (portrait) 640 else 320,
                        detections, { selected = it }, Modifier.size(320.dp, 420.dp))
                }
            }
        }
        fun checkBounds(horizontalInset: Float, verticalInset: Float) {
            val overlay = compose.onNodeWithTag("detection-overlay").getUnclippedBoundsInRoot()
            detections.indices.forEach { index ->
                val label = compose.onNodeWithTag("detection-label-$index").assertIsDisplayed().getUnclippedBoundsInRoot()
                assertTrue(label.left.value >= overlay.left.value + horizontalInset - 1)
                assertTrue(label.right.value <= overlay.right.value - horizontalInset + 1)
                assertTrue(label.top.value >= overlay.top.value + verticalInset - 1)
                assertTrue(label.bottom.value <= overlay.bottom.value - verticalInset + 1)
            }
        }
        checkBounds(0f, 130f)
        compose.onNodeWithTag("detection-label-0").performClick()
        compose.runOnIdle { assertEquals(detections.first(), selected); portrait = true }
        checkBounds(55f, 0f)
        compose.onNodeWithTag("detection-label-1").performClick()
        compose.runOnIdle { assertEquals(detections.last(), selected) }
    }

    @Test fun rendersOverlayOnPhotoForVisualReview() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val image = instrumentation.context.assets.open("bus.jpg").use { BitmapFactory.decodeStream(it) }
        compose.setContent {
            VocaLensTheme(darkTheme = false, dynamicColor = false) {
                Box(Modifier.size(360.dp, 520.dp).testTag("detection-preview")) {
                    Image(image.asImageBitmap(), null, Modifier.matchParentSize(), contentScale = ContentScale.Fit)
                    DetectionOverlay(image.width, image.height, listOf(
                        RecognizedObject(0, "Bus", .94f, ObjectBounds(.02f, .2f, .98f, .68f)),
                        RecognizedObject(1, "Person", .89f, ObjectBounds(.05f, .38f, .25f, .87f))
                    ), {}, Modifier.matchParentSize())
                }
            }
        }
        compose.onNodeWithTag("detection-label-0").assertIsDisplayed()
        compose.onNodeWithText("94%").assertExists()
        val directory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let { File(it).apply { mkdirs() } } ?: instrumentation.targetContext.getExternalFilesDir(null)
        File(directory, "camera-detection-overlay.png").outputStream().use {
            compose.onNodeWithTag("detection-preview").captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
