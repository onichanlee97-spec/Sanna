package com.aistudio.futureagent.agxjyz.data.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages(): Int
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM agent_tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<AgentTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AgentTaskEntity): Long

    @Query("UPDATE agent_tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String): Int

    @Query("DELETE FROM agent_tasks")
    suspend fun clearTasks(): Int
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<UserMemoryEntity>>

    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC")
    suspend fun getMemoriesList(): List<UserMemoryEntity>

    @Query("SELECT * FROM user_memories WHERE `key` = :key LIMIT 1")
    suspend fun getMemory(key: String): UserMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemoryEntity): Long

    @Query("DELETE FROM user_memories WHERE `key` = :key")
    suspend fun deleteMemory(key: String): Int

    @Query("DELETE FROM user_memories")
    suspend fun clearMemories(): Int
}

@Dao
interface VectorDao {
    @Query("SELECT * FROM vector_store")
    suspend fun getAllVectors(): List<VectorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVector(vector: VectorEntity): Long

    @Query("DELETE FROM vector_store")
    suspend fun clearVectors(): Int

    @Query("DELETE FROM vector_store WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun pruneExpired(now: Long): Int
}

@Dao
interface OfflineQueueDao {
    @Query("SELECT * FROM offline_queue ORDER BY timestamp ASC")
    fun getAllOfflineRequests(): Flow<List<OfflineRequestEntity>>

    @Query("SELECT * FROM offline_queue ORDER BY timestamp ASC")
    suspend fun getAllRequestsList(): List<OfflineRequestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: OfflineRequestEntity): Long

    @Delete
    suspend fun deleteRequest(request: OfflineRequestEntity): Int

    @Query("DELETE FROM offline_queue")
    suspend fun clearQueue(): Int
}

@Dao
interface ApprovalDao {
    @Query("SELECT * FROM pending_approvals ORDER BY timestamp DESC")
    fun getAllApprovals(): Flow<List<ApprovalEntity>>

    @Query("SELECT * FROM pending_approvals WHERE id = :id LIMIT 1")
    suspend fun getApproval(id: String): ApprovalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApproval(approval: ApprovalEntity): Long

    @Query("UPDATE pending_approvals SET status = :status, operatorSignature = :signature WHERE id = :id")
    suspend fun updateApprovalStatus(id: String, status: String, signature: String): Int

    @Query("DELETE FROM pending_approvals WHERE id = :id")
    suspend fun deleteApproval(id: String): Int
}
