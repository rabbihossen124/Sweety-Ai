package com.sweety.ai.data

enum class AssistantState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    ACTION_EXECUTING,
    WAITING_CONFIRMATION
}

data class ActionPayload(
    val action: String,
    val params: Map<String, Any?> = emptyMap()
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val actions: List<ActionPayload> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)

data class AssistantResponse(
    val success: Boolean,
    val text: String,
    val actions: List<ActionPayload> = emptyList(),
    val sessionId: String = "session_default",
    val continueListening: Boolean = true,
    val error: String? = null
)

data class PendingConfirmation(
    val action: String,
    val description: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit
)

data class ChatSession(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val lastMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

