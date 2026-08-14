package com.aistudio.futureagent.agxjyz.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.aistudio.futureagent.agxjyz.data.room.AppDatabase
import com.aistudio.futureagent.agxjyz.security.AuditLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

class NetworkMonitor(private val context: Context) {
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startMonitoring() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            @Override
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                flushOfflineQueue()
            }
        }

        try {
            connectivityManager.registerNetworkCallback(request, networkCallback!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun flushOfflineQueue() {
        scope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.offlineQueueDao()
                val pendingRequests = dao.getAllRequestsList()

                if (pendingRequests.isNotEmpty()) {
                    AuditLogger.log(
                        context,
                        "OFFLINE_QUEUE_FLUSH_START",
                        "Flushing ${pendingRequests.size} pending offline requests."
                    )
                }

                for (request in pendingRequests) {
                    val success = transmitPayload(request.url, request.payload)
                    if (success) {
                        dao.deleteRequest(request)
                        AuditLogger.log(
                            context,
                            "OFFLINE_QUEUE_DISPATCHED",
                            "Dispatched offline request #${request.id} to ${request.url}"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun transmitPayload(urlString: String, payload: String): Boolean {
        return try {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true

            try {
                conn.outputStream.use { os: OutputStream ->
                    val input = payload.toByteArray(StandardCharsets.UTF_8)
                    os.write(input, 0, input.size)
                }
                val responseCode = conn.responseCode
                responseCode in 200..299
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            // In a demo/sandbox environment, if target server is offline/dummy, we still simulate resilient transport
            false
        }
    }
}
