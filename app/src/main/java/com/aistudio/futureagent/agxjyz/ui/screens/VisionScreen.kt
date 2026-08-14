package com.aistudio.futureagent.agxjyz.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import android.util.Base64
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aistudio.futureagent.agxjyz.ui.theme.NeonCyan
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VisionScreen(onOpenDrawer: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val coroutineScope = rememberCoroutineScope()
    
    var analysisResult by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().build() }
    
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val textToSpeech = TextToSpeech(context) { _ -> }
        tts = textToSpeech
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    LaunchedEffect(analysisResult) {
        analysisResult?.let { result ->
            tts?.speak(result, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multimodal Vision", color = NeonCyan) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF07131D))
            )
        },
        containerColor = Color(0xFF060D14)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cameraPermissionState.status.isGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx)
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (exc: Exception) {
                                    // Handle exceptions
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                    
                    // Overlay UI
                    if (isAnalyzing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                analysisResult?.let { result ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2B3C)),
                        border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Scene Analysis", style = MaterialTheme.typography.titleSmall, color = NeonCyan)
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = { 
                                        analysisResult = null
                                        tts?.stop()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close", tint = NeonCyan, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(result, color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = {
                        isAnalyzing = true
                        analysisResult = null
                        
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    coroutineScope.launch {
                                        val base64 = imageProxyToBase64(image)
                                        image.close()
                                        
                                        withContext(Dispatchers.IO) {
                                            var attempt = 0
                                            val maxAttempts = 3
                                            var success = false
                                            
                                            val preferredModel = com.aistudio.futureagent.agxjyz.data.SecureStorage.getSelectedModel(context)
                                            val fallbackModels = listOf(preferredModel, "gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro").distinct()
                                            var currentModelIndex = 0
                                            
                                            while (attempt < maxAttempts && !success) {
                                                try {
                                                    attempt++
                                                    val apiKey = com.aistudio.futureagent.agxjyz.utils.ApiKeyManager.getEffectiveApiKey(context)
                                                    if (apiKey.isBlank()) {
                                                        analysisResult = "No API key configured. Please enter your API key in Settings or AI Studio Secrets."
                                                        success = true
                                                        break
                                                    }
                                                    val model = fallbackModels[currentModelIndex]
                                                    
                                                    val request = com.aistudio.futureagent.agxjyz.api.GenerateContentRequest(
                                                        contents = listOf(
                                                            com.aistudio.futureagent.agxjyz.api.Content(
                                                                parts = listOf(
                                                                    com.aistudio.futureagent.agxjyz.api.Part(text = "Describe this scene in detail using natural language. Do not return bounding box coordinates or JSON. Provide a rich, descriptive caption of the objects, text, and overall environment."),
                                                                    com.aistudio.futureagent.agxjyz.api.Part(
                                                                        inlineData = com.aistudio.futureagent.agxjyz.api.InlineData(
                                                                            mimeType = "image/jpeg",
                                                                            data = base64
                                                                        )
                                                                    )
                                                                )
                                                            )
                                                        )
                                                    )
                                                    val response = com.aistudio.futureagent.agxjyz.api.RetrofitClient.service.generateContent(model, apiKey, request)
                                                    val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No objects or text detected."
                                                    analysisResult = if (currentModelIndex > 0) {
                                                        "(Model: $model)\n$text"
                                                    } else {
                                                        text
                                                    }
                                                    success = true
                                                } catch (e: retrofit2.HttpException) {
                                                    if ((e.code() == 429 || e.code() == 404 || e.code() == 400) && currentModelIndex < fallbackModels.size - 1) {
                                                        currentModelIndex++
                                                        attempt = 0
                                                        delay(300L)
                                                    } else if (e.code() == 503 && attempt < maxAttempts) {
                                                        delay(1000L * attempt)
                                                    } else {
                                                        val errorBody = e.response()?.errorBody()?.string() ?: "No error body"
                                                        if (e.code() == 503) {
                                                            analysisResult = "Analysis failed: The AI model is currently experiencing high demand. Please try again later."
                                                        } else if (e.code() == 429) {
                                                            analysisResult = "Analysis failed: API quota exceeded across fallback models. Please wait a minute or check your key."
                                                        } else if (e.code() == 400) {
                                                            analysisResult = "Analysis failed: Invalid request or API key. Please verify your API key in settings."
                                                        } else {
                                                            analysisResult = "Analysis failed: HTTP ${e.code()} - $errorBody"
                                                        }
                                                        success = true
                                                    }
                                                } catch (e: Exception) {
                                                    if (currentModelIndex < fallbackModels.size - 1) {
                                                        currentModelIndex++
                                                        attempt = 0
                                                        delay(300L)
                                                    } else {
                                                        analysisResult = "Analysis failed: ${e.message}"
                                                        success = true
                                                    }
                                                }
                                            }
                                        }
                                        isAnalyzing = false
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    isAnalyzing = false
                                    analysisResult = "Capture failed: \${exception.message}"
                                }
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Analyze Frame")
                    Spacer(Modifier.width(8.dp))
                    Text("Analyze Current Frame", style = MaterialTheme.typography.titleMedium)
                }

            } else {
                Text(
                    "Camera permission is required for Multimodal Vision.",
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = Color.Black)
                ) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

private fun imageProxyToBase64(image: ImageProxy): String {
    val buffer: ByteBuffer = image.planes[0].buffer
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    if (bitmap == null) return ""
    
    val maxDimension = 1024
    val scale = Math.min(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height)
    val scaledBitmap = if (scale < 1) {
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
    } else {
        bitmap
    }
    
    val outputStream = ByteArrayOutputStream()
    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
    val compressedBytes = outputStream.toByteArray()
    
    return Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
}
