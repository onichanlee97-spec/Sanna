package com.aistudio.futureagent.agxjyz.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aistudio.futureagent.agxjyz.BuildConfig
import com.aistudio.futureagent.agxjyz.MainActivity
import com.aistudio.futureagent.agxjyz.R
import com.aistudio.futureagent.agxjyz.data.*
import com.aistudio.futureagent.agxjyz.data.room.AgentRepository
import com.aistudio.futureagent.agxjyz.data.room.AppDatabase
import com.aistudio.futureagent.agxjyz.data.room.ChatMessageEntity
import com.aistudio.futureagent.agxjyz.security.AuditLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

enum class FloatingState {
    STANDBY,
    LISTENING,
    PROCESSING,
    SPEAKING
}

class AgentListeningService : Service() {

    companion object {
        const val CHANNEL_ID = "SannaForegroundServiceChannel"
        const val NOTIFICATION_ID = 1338
        const val PREF_NAME = "SannaPreferences"
        const val KEY_SERVICE_ACTIVE = "service_active"

        var isRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, AgentListeningService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AgentListeningService::class.java)
            context.stopService(intent)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }
    private val repository by lazy {
        AgentRepository(
            database.chatDao(),
            database.taskDao(),
            database.memoryDao(),
            database.vectorDao(),
            database.approvalDao(),
            database.offlineQueueDao()
        )
    }
    private val voiceHelper by lazy { VoiceHelper(applicationContext) }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var floatingIcon: ImageView? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // AudioRecord Hardware Capture Engine
    private var audioRecord: AudioRecord? = null
    private var audioThread: Thread? = null
    @Volatile private var isRecordingAudio = false
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    // Voice Activity Detection (VAD) State
    private var speechFramesCount = 0
    private var silenceFramesCount = 0
    private var isSpeechActive = false
    private var lastSpeechDetectedTime = 0L

    // SpeechRecognizer Fallback/Interactive Engine
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechRecognizerIntent: Intent? = null
    private var isSpeechRecognizerBusy = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val watchdogRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                val hasMicPermission = ContextCompat.checkSelfPermission(
                    this@AgentListeningService,
                    android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (hasMicPermission) {
                    if (!isRecordingAudio || audioRecord == null || audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                        startPermanentAudioRecordStream()
                    }
                }
                mainHandler.postDelayed(this, 3000L)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SERVICE_ACTIVE, true).apply()

        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            startPermanentAudioRecordStream()
            initSpeechRecognizer()
        }
        startWatchdog()
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true

        getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SERVICE_ACTIVE, true).apply()

        // Acquire partial wake lock to keep background microphone alive
        val powerManager = getSystemService(POWER_SERVICE) as? PowerManager
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SannaAgent::ListeningWakeLock")
            wakeLock?.acquire(60 * 60 * 1000L)
        }

        createNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Sanna Assistant Active (Background Mic)")
            .setContentText("Background microphone active. Floating widget listening for queries.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val hasMicPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val serviceType = if (hasMicPermission) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                }
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val serviceType = if (hasMicPermission) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                }
                startForeground(NOTIFICATION_ID, notification, serviceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (se: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (fallbackEx: Exception) {
                try {
                    startForeground(NOTIFICATION_ID, notification)
                } catch (ignored: Exception) {}
            }
        }

        initFloatingOverlay()
        if (hasMicPermission) {
            startPermanentAudioRecordStream()
            initSpeechRecognizer()
        }
        startWatchdog()
        AuditLogger.logEvent(applicationContext, "AGENT_SERVICE", "AgentListeningService active in background.")
    }

    private fun startWatchdog() {
        mainHandler.removeCallbacks(watchdogRunnable)
        mainHandler.postDelayed(watchdogRunnable, 3000L)
    }

    /**
     * Starts the direct, low-level AudioRecord hardware stream that runs continuously
     * without timeouts, maintaining a permanently open microphone.
     */
    @SuppressLint("MissingPermission")
    @Synchronized
    private fun startPermanentAudioRecordStream() {
        if (isRecordingAudio && audioRecord != null && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return
        }

        stopAudioRecordStream()

        try {
            val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, sampleRate * 2)

            var record: AudioRecord? = null
            try {
                record = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            } catch (t: Throwable) {
                record = null
            }

            if (record == null || record.state != AudioRecord.STATE_INITIALIZED) {
                record?.release()
                record = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
            }

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return
            }

            audioRecord = record
            audioRecord?.startRecording()
            isRecordingAudio = true

            audioThread = Thread({
                val buffer = ShortArray(1024)
                while (isRecordingAudio && !Thread.currentThread().isInterrupted) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readCount > 0) {
                        var sum = 0.0
                        var maxAmp = 0
                        for (i in 0 until readCount) {
                            val sample = buffer[i].toInt()
                            val absVal = abs(sample)
                            if (absVal > maxAmp) maxAmp = absVal
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / readCount)
                        val normalizedLevel = (rms / 32768.0).toFloat().coerceIn(0.0f, 1.0f)

                        // Real-time Visual Waveform Feedback on floating HUD
                        mainHandler.post {
                            updateVisualFeedback(normalizedLevel, maxAmp)
                        }

                        // Voice Activity Detection (VAD)
                        detectVoiceActivity(normalizedLevel, maxAmp)
                    } else {
                        try {
                            Thread.sleep(20)
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                }
            }, "Sanna-PermanentMicThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            AuditLogger.logEvent(applicationContext, "AUDIO_STREAM", "Hardware microphone stream open.")
        } catch (e: Exception) {
            AuditLogger.logEvent(applicationContext, "AUDIO_STREAM_ERROR", e.message ?: "AudioRecord error")
        }
    }

    private fun stopAudioRecordStream() {
        isRecordingAudio = false
        try {
            audioThread?.interrupt()
            audioThread = null
        } catch (ignored: Exception) {}

        try {
            if (audioRecord != null) {
                if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord?.stop()
                }
                audioRecord?.release()
                audioRecord = null
            }
        } catch (ignored: Exception) {}
    }

    private fun updateVisualFeedback(normalizedLevel: Float, maxAmp: Int) {
        val icon = floatingIcon ?: return
        if (isSpeechRecognizerBusy) return
        if (maxAmp > 1200 || normalizedLevel > 0.05f) {
            val scale = 1.0f + (normalizedLevel * 0.45f).coerceIn(0.08f, 0.45f)
            icon.animate()
                .scaleX(scale)
                .scaleY(scale)
                .alpha(1.0f)
                .setDuration(80)
                .start()
        } else {
            icon.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(0.9f)
                .setDuration(200)
                .start()
        }
    }

    private fun updateFloatingState(state: FloatingState) {
        val icon = floatingIcon ?: return
        when (state) {
            FloatingState.STANDBY -> {
                icon.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(0.9f)
                    .rotation(0f)
                    .setDuration(150)
                    .start()
            }
            FloatingState.LISTENING -> {
                icon.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .alpha(1.0f)
                    .setDuration(120)
                    .start()
            }
            FloatingState.PROCESSING -> {
                icon.animate()
                    .scaleX(1.15f)
                    .scaleY(1.15f)
                    .alpha(0.7f)
                    .setDuration(180)
                    .start()
            }
            FloatingState.SPEAKING -> {
                icon.animate()
                    .scaleX(1.25f)
                    .scaleY(1.25f)
                    .alpha(1.0f)
                    .setDuration(120)
                    .start()
            }
        }
    }

    /**
     * Real-time Voice Activity Detection (VAD)
     */
    private fun detectVoiceActivity(normalizedLevel: Float, maxAmp: Int) {
        val now = System.currentTimeMillis()
        val isVoiceFrame = (maxAmp > 2800 || normalizedLevel > 0.12f)

        if (isVoiceFrame) {
            speechFramesCount++
            silenceFramesCount = 0
            lastSpeechDetectedTime = now

            if (speechFramesCount >= 3 && !isSpeechActive) {
                isSpeechActive = true
                AuditLogger.logEvent(applicationContext, "VAD_SPEECH_START", "Voice utterance detected.")
            }
        } else {
            if (isSpeechActive) {
                silenceFramesCount++
                if (silenceFramesCount > 18 && (now - lastSpeechDetectedTime) > 1200L) {
                    isSpeechActive = false
                    speechFramesCount = 0
                    silenceFramesCount = 0
                    AuditLogger.logEvent(applicationContext, "VAD_SPEECH_END", "Speech utterance concluded.")
                    mainHandler.post {
                        onUtteranceDetected()
                    }
                }
            }
        }
    }

    private fun onUtteranceDetected() {
        if (!isSpeechRecognizerBusy) {
            startBackgroundListening()
        }
    }

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    private fun initFloatingOverlay() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
                return
            }

            windowManager = getSystemService(WINDOW_SERVICE) as? WindowManager ?: return
            floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 300
            }

            floatingIcon = floatingView?.findViewById<ImageView>(R.id.floating_icon)

            floatingIcon?.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f
                private var isMoved = false

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    event ?: return false
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isMoved = false
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = (event.rawX - initialTouchX).toInt()
                            val deltaY = (event.rawY - initialTouchY).toInt()
                            if (abs(deltaX) > 10 || abs(deltaY) > 10) {
                                isMoved = true
                            }
                            params.x = initialX + deltaX
                            params.y = initialY + deltaY
                            floatingView?.let { windowManager?.updateViewLayout(it, params) }
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!isMoved) {
                                // Tap triggers background speech capture directly in the service
                                startBackgroundListening()
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            floatingView?.let { windowManager?.addView(it, params) }
        } catch (e: Exception) {
            e.printStackTrace()
            AuditLogger.logEvent(applicationContext, "FLOATING_OVERLAY_ERROR", e.message ?: "Failed to add floating view")
        }
    }

    private fun initSpeechRecognizer() {
        mainHandler.post {
            try {
                if (SpeechRecognizer.isRecognitionAvailable(this)) {
                    speechRecognizer?.destroy()
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                    speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    }

                    speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isSpeechRecognizerBusy = true
                            updateFloatingState(FloatingState.LISTENING)
                        }

                        override fun onBeginningOfSpeech() {
                            updateFloatingState(FloatingState.LISTENING)
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            if (isSpeechRecognizerBusy) {
                                val scale = 1.0f + (rmsdB.coerceIn(0f, 10f) / 10f) * 0.4f
                                floatingIcon?.animate()?.scaleX(scale)?.scaleY(scale)?.setDuration(50)?.start()
                            }
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            updateFloatingState(FloatingState.PROCESSING)
                        }

                        override fun onError(error: Int) {
                            isSpeechRecognizerBusy = false
                            updateFloatingState(FloatingState.STANDBY)
                        }

                        override fun onResults(results: Bundle?) {
                            isSpeechRecognizerBusy = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val spokenText = matches?.firstOrNull { it.isNotBlank() }
                            if (!spokenText.isNullOrBlank()) {
                                updateFloatingState(FloatingState.PROCESSING)
                                processBackgroundVoiceQuery(spokenText)
                            } else {
                                updateFloatingState(FloatingState.STANDBY)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            } catch (e: Exception) {
                AuditLogger.logEvent(applicationContext, "SPEECH_RECOGNIZER_INIT_ERROR", e.message ?: "Speech recognition error")
            }
        }
    }

    /**
     * Starts listening in the background service without launching or foregrounding MainActivity.
     */
    private fun startBackgroundListening() {
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    initSpeechRecognizer()
                }
                if (!isSpeechRecognizerBusy) {
                    isSpeechRecognizerBusy = true
                    updateFloatingState(FloatingState.LISTENING)
                    speechRecognizer?.startListening(speechRecognizerIntent)
                }
            } catch (e: Exception) {
                isSpeechRecognizerBusy = false
                updateFloatingState(FloatingState.STANDBY)
            }
        }
    }

    /**
     * Executes the user query in the background:
     * 1. Saves user input to Room DB
     * 2. Runs tool/AI processing
     * 3. Saves assistant response to Room DB
     * 4. Speaks the answer aloud via TextToSpeech
     * 5. Keeps MainActivity closed/minimized
     */
    private fun processBackgroundVoiceQuery(spokenText: String) {
        serviceScope.launch {
            try {
                AuditLogger.logEvent(applicationContext, "BACKGROUND_VOICE_COMMAND", "Recognized: $spokenText")

                // 1. Insert user message into Room DB
                val userMsgId = System.currentTimeMillis().toString()
                repository.insertMessage(
                    ChatMessageEntity(
                        id = userMsgId,
                        isUser = true,
                        text = spokenText,
                        timestamp = System.currentTimeMillis()
                    )
                )

                val secureKey = SecureStorage.getApiKey(this@AgentListeningService)
                val apiKey = if (secureKey.isNotBlank()) secureKey else BuildConfig.GEMINI_API_KEY
                val memories = try { repository.allMemories.firstOrNull() ?: emptyList() } catch (e: Exception) { emptyList() }

                val responseText = if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
                    try {
                        val memoryContext = if (memories.isNotEmpty()) {
                            "User Memory Context:\n" + memories.take(10).joinToString("\n") { "• ${it.key}: ${it.value}" } + "\n\n"
                        } else ""

                        val req = GeminiRequest(
                            contents = listOf(Content(role = "user", parts = listOf(Part(text = memoryContext + spokenText)))),
                            systemInstruction = Content(parts = listOf(Part(text = "You are Sanna, an autonomous Android AI assistant. Provide concise, clear, voice-friendly answers.")))
                        )
                        val resp = GeminiFallbackExecutor.generateWithFallback(this@AgentListeningService, apiKey, req)
                        val reply = resp.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        reply ?: OfflineRulesEngine.processOfflineQuery(
                            this@AgentListeningService,
                            spokenText,
                            memories.map { UserMemoryItem(it.key, it.value) }
                        )
                    } catch (e: Exception) {
                        OfflineRulesEngine.processOfflineQuery(
                            this@AgentListeningService,
                            spokenText,
                            memories.map { UserMemoryItem(it.key, it.value) }
                        )
                    }
                } else {
                    OfflineRulesEngine.processOfflineQuery(
                        this@AgentListeningService,
                        spokenText,
                        memories.map { UserMemoryItem(it.key, it.value) }
                    )
                }

                // 3. Insert assistant response into Room DB
                val assistantMsgId = (System.currentTimeMillis() + 1).toString()
                repository.insertMessage(
                    ChatMessageEntity(
                        id = assistantMsgId,
                        isUser = false,
                        text = responseText,
                        timestamp = System.currentTimeMillis()
                    )
                )

                // 4. Speak response via TTS
                val cleanSpeech = responseText
                    .replace(Regex("⚡|\\[.*?\\]|\\*|#|_|`"), "")
                    .replace(Regex("\\(.*?\\)"), "")
                    .trim()

                mainHandler.post {
                    updateFloatingState(FloatingState.SPEAKING)
                }

                voiceHelper.speak(cleanSpeech)
                AuditLogger.logEvent(applicationContext, "BACKGROUND_VOICE_RESPONSE", "Replied: ${cleanSpeech.take(50)}")

                delay(1500)
                mainHandler.post {
                    updateFloatingState(FloatingState.STANDBY)
                }
            } catch (e: Exception) {
                AuditLogger.logEvent(applicationContext, "BACKGROUND_VOICE_ERROR", e.message ?: "Processing error")
                voiceHelper.speak("Error processing voice directive.")
                mainHandler.post {
                    updateFloatingState(FloatingState.STANDBY)
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Sanna Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Sanna background microphone listening and floating overlay active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        mainHandler.removeCallbacksAndMessages(null)
        stopAudioRecordStream()
        serviceScope.cancel()

        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (ignored: Exception) {}

        try {
            if (floatingView != null && windowManager != null) {
                windowManager?.removeView(floatingView)
            }
        } catch (ignored: Exception) {}

        try {
            if (wakeLock != null && wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (ignored: Exception) {}

        AuditLogger.logEvent(applicationContext, "AGENT_SERVICE", "AgentListeningService destroyed.")
    }
}
