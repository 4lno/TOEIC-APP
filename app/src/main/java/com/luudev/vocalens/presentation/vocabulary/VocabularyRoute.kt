package com.luudev.vocalens.presentation.vocabulary

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.luudev.vocalens.R
import java.util.Locale

/** Owns device speech and lifecycle; form and list state remain in the ViewModel. */
@Composable
fun VocabularyRoute(viewModel: VocabularyViewModel, soundEnabled: Boolean = true, onBackClick: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    var engine by remember { mutableStateOf<TextToSpeech?>(null) }
    var initialized by remember { mutableStateOf(false) }
    var voiceAvailable by remember { mutableStateOf(false) }
    var speechMessage by remember { mutableStateOf<String?>(null) }
    val loadingMessage = stringResource(R.string.vocabulary_voice_loading)
    val unavailableMessage = stringResource(R.string.vocabulary_voice_unavailable)
    val soundDisabledMessage = stringResource(R.string.profile_sound_disabled)

    DisposableEffect(context, lifecycleOwner) {
        var disposed = false
        initialized = false
        voiceAvailable = false
        val tts = TextToSpeech(context) { status ->
            if (!disposed) {
                val languageStatus = if (status == TextToSpeech.SUCCESS) engine?.setLanguage(Locale.UK)
                    else null
                voiceAvailable = languageStatus != null && languageStatus >= TextToSpeech.LANG_AVAILABLE
                initialized = true
            }
        }
        engine = tts
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) tts.stop()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            disposed = true
            lifecycleOwner.lifecycle.removeObserver(observer)
            tts.stop()
            tts.shutdown()
            engine = null
        }
    }
    LaunchedEffect(speechMessage) {
        speechMessage?.let {
            snackbarHostState.showSnackbar(it)
            speechMessage = null
        }
    }

    Box(Modifier.fillMaxSize()) {
        VocabularyScreen(
            uiState = viewModel.uiState,
            onBackClick = onBackClick,
            onQueryChange = viewModel::onQueryChange,
            onCategorySelected = viewModel::onCategorySelected,
            onSortSelected = viewModel::onSortSelected,
            onSpeakWord = { word ->
                when {
                    !soundEnabled -> speechMessage = soundDisabledMessage
                    !initialized -> speechMessage = loadingMessage
                    !voiceAvailable -> speechMessage = unavailableMessage
                    else -> {
                        val result = engine?.speak(word.english, TextToSpeech.QUEUE_FLUSH, null, word.id)
                        if (result != TextToSpeech.SUCCESS) speechMessage = unavailableMessage
                    }
                }
            },
            onDeleteRequested = viewModel::onDeleteRequested,
            onDeleteConfirmed = viewModel::onDeleteConfirmed,
            onDeleteDismissed = viewModel::onDeleteDismissed,
            onStartReview = viewModel::onStartReview,
            onRevealMeaning = viewModel::onRevealMeaning,
            onNextReviewWord = viewModel::onNextReviewWord,
            onDismissReview = viewModel::onDismissReview
        )
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }
}
