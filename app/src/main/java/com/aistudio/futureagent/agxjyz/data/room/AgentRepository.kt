package com.aistudio.futureagent.agxjyz.data.room

import kotlinx.coroutines.flow.Flow

class AgentRepository(
    private val chatDao: ChatDao,
    private val taskDao: TaskDao,
    private val memoryDao: MemoryDao,
    private val vectorDao: VectorDao
) {
    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    val allTasks: Flow<List<AgentTaskEntity>> = taskDao.getAllTasks()
    val allMemories: Flow<List<UserMemoryEntity>> = memoryDao.getAllMemories()

    suspend fun getAllVectors(): List<VectorEntity> = vectorDao.getAllVectors()

    suspend fun insertVector(vector: VectorEntity) {
        vectorDao.insertVector(vector)
    }

    suspend fun clearVectors() {
        vectorDao.clearVectors()
    }

    suspend fun insertMessage(msg: ChatMessageEntity) {
        chatDao.insertMessage(msg)
    }

    suspend fun insertTask(task: AgentTaskEntity) {
        taskDao.insertTask(task)
    }

    suspend fun updateTaskStatus(id: String, status: String) {
        taskDao.updateTaskStatus(id, status)
    }

    suspend fun insertMemory(memory: UserMemoryEntity) {
        memoryDao.insertMemory(memory)
    }

    suspend fun getMemoriesList(): List<UserMemoryEntity> {
        return memoryDao.getMemoriesList()
    }

    suspend fun deleteMemory(key: String) {
        memoryDao.deleteMemory(key)
    }
}
