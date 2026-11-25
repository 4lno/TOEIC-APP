package com.luudev.vocalens.presentation.camera

import android.Manifest
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.luudev.vocalens.data.repository.LocalObjectRecognitionRepository
import com.luudev.vocalens.domain.usecase.RecognizeObjectsUseCase
import com.luudev.vocalens.presentation.theme.VocaLensTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CameraScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test(timeout = 60_000)
    fun cameraCaptureAnalysisAndRetakeWorkOnDevice() {
        val tag = "CameraScreenTest"
        Log.d(tag, "Step 1: Granting camera runtime permission")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.grantRuntimePermission(
            instrumentation.targetContext.packageName,
            Manifest.permission.CAMERA
        )

        lateinit var vm: CameraViewModel
        compose.runOnUiThread {
            Log.d(tag, "Step 2: Initializing CameraViewModel")
            vm = CameraViewModel(RecognizeObjectsUseCase(LocalObjectRecognitionRepository(instrumentation.targetContext)))
        }

        Log.d(tag, "Step 3: Setting Compose content with CameraRoute")
        compose.setContent { VocaLensTheme { CameraRoute(vm, onBack = {}) } }

        repeat(2) { iteration ->
            Log.d(tag, "Iteration $iteration: Waiting for camera preview ready")
            compose.waitUntil(20_000) {
                !vm.uiState.isLoading && !vm.uiState.hasError && vm.uiState.stage == CameraStage.Preview
            }
            Log.d(tag, "Iteration $iteration: Camera ready, clicking capture button")
            compose.onNodeWithTag("camera_capture").assertIsEnabled().performClick()

            Log.d(tag, "Iteration $iteration: Waiting for CameraStage.Results")
            compose.waitUntil(30_000) {
                vm.uiState.stage == CameraStage.Results || vm.uiState.captureError != null
            }

            Log.d(tag, "Iteration $iteration: Reached Results stage or error. State: ${vm.uiState.stage}, error: ${vm.uiState.captureError}")
            compose.runOnIdle {
                assertNull("Capture or inference failed with error: ${vm.uiState.captureError}", vm.uiState.captureError)
                val photo = vm.uiState.photo
                assertNotNull("Photo byte array must not be null", photo)
                val bitmap = BitmapFactory.decodeByteArray(photo!!, 0, photo.size)
                assertNotNull("Decoded bitmap must not be null", bitmap)
                assertTrue("Bitmap max dimension should be <= 1280", maxOf(bitmap.width, bitmap.height) <= 1280)
                bitmap.recycle()
            }

            Log.d(tag, "Iteration $iteration: Clicking retake button")
            compose.onNodeWithText("Chụp lại").performClick()
        }
        Log.d(tag, "CameraScreenTest completed successfully")
    }
}
