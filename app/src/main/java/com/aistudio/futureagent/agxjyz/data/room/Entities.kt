package com.aistudio.futureagent.agxjyz.data.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val isUser: Boolean,
    val text: String,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "agent_tasks")
data class AgentTaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val status: String, // QUEUED, EXECUTING, DONE
    val type: String = "Reasoning",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_memories")
data class UserMemoryEntity(
    @PrimaryKey val key: String,
    val value: String,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "vector_store")
data class VectorEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val metadata: String,
    val embedding: List<Float>,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "pending_approvals")
data class ApprovalEntity(
    @PrimaryKey val id: String,
    val actionName: String,
    val payload: String,
    val riskLevel: String,
    val status: String, // PENDING, APPROVED, DENIED
    val operatorSignature: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
