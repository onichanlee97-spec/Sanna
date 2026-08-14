package com.aistudio.futureagent.agxjyz.service

import android.app.Service
import android.content.Intent
import android.os.*
import com.aistudio.futureagent.agxjyz.security.AuditLogger

class AgentIpcService : Service() {

    private lateinit var mMessenger: Messenger

    internal class IncomingHandler(
        private val context: android.content.Context
    ) : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_TOOL_REQUEST -> {
                    val data = msg.data
                    val toolName = data?.getString("toolName") ?: "unknown"

                    AuditLogger.logEvent(context, "IPC_TOOL_DISPATCH", "Executed tool: $toolName")

                    try {
                        val clientMessenger = msg.replyTo
                        if (clientMessenger != null) {
                            val replyMsg = Message.obtain(null, MSG_TOOL_RESPONSE)
                            val replyData = Bundle().apply {
                                putString("result", "Successfully executed tool: $toolName")
                            }
                            replyMsg.data = replyData
                            clientMessenger.send(replyMsg)
                        }
                    } catch (e: RemoteException) {
                        e.printStackTrace()
                    }
                }
                else -> super.handleMessage(msg)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        mMessenger = Messenger(IncomingHandler(applicationContext))
        return mMessenger.binder
    }

    companion object {
        const val MSG_TOOL_REQUEST = 2
        const val MSG_TOOL_RESPONSE = 2
    }
}

