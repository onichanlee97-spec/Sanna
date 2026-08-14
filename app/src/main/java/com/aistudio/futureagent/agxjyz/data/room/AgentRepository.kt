package com.aistudio.futureagent.agxjyz.data.room

import kotlinx.coroutines.flow.Flow

class AgentRepository(
    private val chatDao: ChatDao,
    private val taskDao: TaskDao,
    private val memoryDao: MemoryDao,
    private val vectorDao: VectorDao,
    private val approvalDao: ApprovalDao,
    private val offlineQueueDao: OfflineQueueDao
) {
    val allMessages: Flow<List<ChatMessageEntity>> = chatDao.getAllMessages()
    val allTasks: Flow<List<AgentTaskEntity>> = taskDao.getAllTasks()
    val allMemories: Flow<List<UserMemoryEntity>> = memoryDao.getAllMemories()
    val allApprovals: Flow<List<ApprovalEntity>> = approvalDao.getAllApprovals()
    val allOfflineRequests: Flow<List<OfflineRequestEntity>> = offlineQueueDao.getAllOfflineRequests()

    suspend fun insertOfflineRequest(request: OfflineRequestEntity) {
        offlineQueueDao.insertRequest(request)
    }

    suspend fun deleteOfflineRequest(request: OfflineRequestEntity) {
        offlineQueueDao.deleteRequest(request)
    }

    suspend fun clearOfflineQueue() {
        offlineQueueDao.clearQueue()
    }

    suspend fun getApproval(id: String): ApprovalEntity? = approvalDao.getApproval(id)

    suspend fun insertApproval(approval: ApprovalEntity) {
        approvalDao.insertApproval(approval)
    }

    suspend fun updateApprovalStatus(id: String, status: String, signature: String) {
        approvalDao.updateApprovalStatus(id, status, signature)
    }

    suspend fun deleteApproval(id: String) {
        approvalDao.deleteApproval(id)
    }

    suspend fun getAllVectors(): List<VectorEntity> = vectorDao.getAllVectors()

    suspend fun insertVector(vector: VectorEntity) {
        vectorDao.insertVector(vector)
    }

    suspend fun clearVectors() {
        vectorDao.clearVectors()
    }

    suspend fun pruneExpired(now: Long) {
        vectorDao.pruneExpired(now)
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
