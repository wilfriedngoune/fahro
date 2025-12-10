package com.example.fahr.ui.main.chat.model

data class ChatPreview(
    val chatId: String,
    val username: String,
    val avatarRes: Int,
    val lastMessage: String,
    val lastMessageTime: String
)
