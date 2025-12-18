package com.example.fahr.ui.main.chat.conversation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fahr.core.UserSession
import com.example.fahr.databinding.FragmentConversationBinding
import com.example.fahr.ui.main.chat.model.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class ConversationFragment : Fragment() {

    private lateinit var binding: FragmentConversationBinding
    private lateinit var adapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var chatId: String? = null
    private var otherUsername: String? = null
    private var messagesListener: ListenerRegistration? = null
    private var currentUserId: String? = null

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatId = arguments?.getString("chatId")
        otherUsername = arguments?.getString("username")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentConversationBinding.inflate(inflater, container, false)

        binding.chatTitle.text = otherUsername ?: "Chat"

        adapter = MessageAdapter(messages)
        binding.messagesList.layoutManager = LinearLayoutManager(requireContext())
        binding.messagesList.adapter = adapter

        currentUserId = UserSession.getCurrentUserId(requireContext())

        // back
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // send
        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        subscribeMessages()

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messagesListener?.remove()
    }

    private fun subscribeMessages() {
        val discussionId = chatId ?: return

        messagesListener = firestore.collection("messages")
            .whereEqualTo("discussionId", discussionId)
            .orderBy("sentAt")
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !isAdded) return@addSnapshotListener

                messages.clear()
                for (doc in snapshot.documents) {
                    val text = doc.getString("text").orEmpty()
                    val senderId = doc.getString("senderId").orEmpty()
                    val ts = doc.getTimestamp("sentAt")
                    val timeStr = ts?.toDate()?.let { timeFormat.format(it) } ?: ""

                    val isMe = senderId == currentUserId
                    messages.add(
                        Message(
                            text = text,
                            isMe = isMe,
                            time = timeStr,
                            isSent = true
                        )
                    )
                }

                if (messages.isEmpty()) {
                    binding.emptyMessagesText.visibility = View.VISIBLE
                    binding.messagesList.visibility = View.GONE
                } else {
                    binding.emptyMessagesText.visibility = View.GONE
                    binding.messagesList.visibility = View.VISIBLE
                    adapter.notifyDataSetChanged()
                    binding.messagesList.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun sendMessage() {
        val discussionId = chatId ?: return
        val senderId = currentUserId ?: return

        val text = binding.messageInput.text.toString().trim()
        if (text.isEmpty()) return

        val messageData = hashMapOf(
            "discussionId" to discussionId,
            "senderId" to senderId,
            "text" to text,
            "sentAt" to FieldValue.serverTimestamp(),
            "isRead" to false
        )

        // ajoute le message
        firestore.collection("messages")
            .add(messageData)
            .addOnSuccessListener {
                // met à jour le résumé de la discussion
                firestore.collection("discussions")
                    .document(discussionId)
                    .update(
                        mapOf(
                            "lastMessage" to text,
                            "lastMessageTime" to FieldValue.serverTimestamp()
                        )
                    )

                binding.messageInput.setText("")
            }
    }
}
