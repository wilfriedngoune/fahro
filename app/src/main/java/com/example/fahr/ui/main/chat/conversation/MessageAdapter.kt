package com.example.fahr.ui.main.chat.conversation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fahr.databinding.ItemMessageMeBinding
import com.example.fahr.databinding.ItemMessageOtherBinding
import com.example.fahr.ui.main.chat.model.Message

class MessageAdapter(private val messages: List<Message>)
    : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_ME = 1
    private val TYPE_OTHER = 2

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isMe) TYPE_ME else TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ME) {
            val binding = ItemMessageMeBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            MeViewHolder(binding)
        } else {
            val binding = ItemMessageOtherBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            OtherViewHolder(binding)
        }
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder is MeViewHolder) holder.bind(msg)
        else if (holder is OtherViewHolder) holder.bind(msg)
    }

    class MeViewHolder(private val binding: ItemMessageMeBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: Message) {
            binding.messageText.text = msg.text
            binding.time.text = msg.time
        }
    }

    class OtherViewHolder(private val binding: ItemMessageOtherBinding)
        : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: Message) {
            binding.messageText.text = msg.text
            binding.time.text = msg.time
        }
    }
}
