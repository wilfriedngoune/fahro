package com.example.fahr.ui.main.chat.model

data class Message(
    val text: String,
    val isMe: Boolean,
    val time: String,
    val isSent: Boolean
)
