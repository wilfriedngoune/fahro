package com.example.fahr.ui.main.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fahr.databinding.ItemChatPreviewBinding
import com.example.fahr.ui.main.chat.model.ChatPreview

class ChatAdapter(
    private val chats: List<ChatPreview>,
    private val onClick: (ChatPreview) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(val binding: ItemChatPreviewBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatPreviewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChatViewHolder(binding)
    }

    override fun getItemCount(): Int = chats.size

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]

        holder.binding.avatar.setImageResource(chat.avatarRes)
        holder.binding.username.text = chat.username
        holder.binding.lastMessage.text = chat.lastMessage
        holder.binding.time.text = chat.lastMessageTime

        holder.itemView.setOnClickListener {
            onClick(chat)
        }
    }
}
