package com.aistudio.futureagent.agxjyz.data.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM agent_tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<AgentTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AgentTaskEntity)

    @Query("UPDATE agent_tasks SET status = :status WHERE id = :id")
    suspend fun updateTaskStatus(id: String, status: String)

    @Query("DELETE FROM agent_tasks")
    suspend fun clearTasks()
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
    suspend fun insertMemory(memory: UserMemoryEntity)

    @Query("DELETE FROM user_memories WHERE `key` = :key")
    suspend fun deleteMemory(key: String)

    @Query("DELETE FROM user_memories")
    suspend fun clearMemories()
}
