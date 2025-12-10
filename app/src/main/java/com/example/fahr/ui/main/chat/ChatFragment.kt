package com.example.fahr.ui.main.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fahr.R
import com.example.fahr.databinding.FragmentChatBinding
import com.example.fahr.ui.main.chat.model.ChatPreview
import com.example.fahr.ui.main.chat.conversation.ConversationFragment

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)

        val chats = listOf(
            ChatPreview("1", "Nelson", R.drawable.nelson, "See you tomorrow!", "10:25"),
            ChatPreview("2", "Wilfried", R.drawable.wilfried, "Thanks!", "09:12"),
            ChatPreview("3", "Millena", R.drawable.millena, "I won !", "07:01"),
            ChatPreview("4", "Handrea", R.drawable.handrea, "Where are you?", "Yesterday"),
            ChatPreview("5", "Ericka", R.drawable.ericka, "I'am there please !", "09:23")
        )

        chatAdapter = ChatAdapter(chats) { chat ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ConversationFragment.newInstance(chat.chatId, chat.username))
                .addToBackStack(null)
                .commit()
        }

        binding.chatList.layoutManager = LinearLayoutManager(requireContext())
        binding.chatList.adapter = chatAdapter

        return binding.root
    }
}
