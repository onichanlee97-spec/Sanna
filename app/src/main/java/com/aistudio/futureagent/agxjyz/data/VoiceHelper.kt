package com.aistudio.futureagent.agxjyz.data

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class VoiceHelper(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        try {
            val appContext = context.applicationContext ?: context
            tts = TextToSpeech(appContext) { status ->
                try {
                    if (status == TextToSpeech.SUCCESS) {
                        val result = tts?.setLanguage(Locale.US)
                        isInitialized = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
                    }
                } catch (t: Throwable) {
                    isInitialized = false
                }
            }
        } catch (t: Throwable) {
            isInitialized = false
            tts = null
        }
    }

    fun speak(text: String) {
        if (isInitialized && tts != null) {
            try {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            } catch (t: Throwable) {
                // Safely handle TTS invocation failure
            }
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (t: Throwable) {
            // Safely ignore shutdown errors
        }
    }
}
