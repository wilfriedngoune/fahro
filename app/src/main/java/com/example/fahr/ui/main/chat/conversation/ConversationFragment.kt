package com.example.fahr.ui.main.chat.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fahr.databinding.FragmentConversationBinding
import com.example.fahr.ui.main.chat.model.Message

class ConversationFragment : Fragment() {

    private lateinit var binding: FragmentConversationBinding
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()

    companion object {
        fun newInstance(chatId: String, username: String): ConversationFragment {
            val fragment = ConversationFragment()
            val args = Bundle()
            args.putString("chatId", chatId)
            args.putString("username", username)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConversationBinding.inflate(inflater, container, false)

        binding.chatTitle.text = arguments?.getString("username")

        adapter = MessageAdapter(messages)
        binding.messagesList.layoutManager = LinearLayoutManager(requireContext())
        binding.messagesList.adapter = adapter

        // Dummy messages
        messages.add(Message("Hi, how are you?", false, "10:00", true))
        messages.add(Message("I'm good, thanks!", true, "10:01", true))
        messages.add(Message("Are we still meeting today?", false, "10:02", true))
        messages.add(Message("Have you done with the work please ?", true, "10:07", true))
        adapter.notifyDataSetChanged()

        // Send message
        binding.sendButton.setOnClickListener {
            val text = binding.messageInput.text.toString().trim()
            if (text.isNotBlank()) {
                messages.add(Message(text, true, "Now", true))
                adapter.notifyItemInserted(messages.size - 1)
                binding.messagesList.scrollToPosition(messages.size - 1)
                binding.messageInput.setText("")
            }
        }

        // back button
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }
}
