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
        private const val TAG = "Conversation"   // pour Logcat

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

        android.util.Log.d(TAG, "onCreate chatId=$chatId, otherUsername=$otherUsername")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConversationBinding.inflate(inflater, container, false)

        binding.chatTitle.text = otherUsername ?: "Chat"

        adapter = MessageAdapter(messages)
        binding.messagesList.layoutManager = LinearLayoutManager(requireContext())
        binding.messagesList.adapter = adapter

        currentUserId = UserSession.getCurrentUserId(requireContext())
        android.util.Log.d(TAG, "currentUserId from UserSession = $currentUserId")

        // Back
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Send
        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        subscribeMessages()
        debugAllMessagesOnce()   // 🔍 Debug pour voir TOUTE la collection messages

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        messagesListener?.remove()
        android.util.Log.d(TAG, "Listener removed")
    }

    // ============================================================
    // 1) Abonnement temps réel aux messages de la discussion
    // ============================================================
    private fun subscribeMessages() {
        val discussionId = chatId ?: run {
            android.util.Log.e(TAG, "subscribeMessages: chatId is null")
            return
        }

        android.util.Log.d(TAG, "subscribeMessages for discussionId=$discussionId")

        messagesListener = firestore.collection("messages")
            .whereEqualTo("discussionId", discussionId)
            .orderBy("sentAt")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e(TAG, "Listen error", e)
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    android.util.Log.e(TAG, "Snapshot is null")
                    return@addSnapshotListener
                }
                if (!isAdded) {
                    android.util.Log.w(TAG, "Fragment not added, ignore snapshot")
                    return@addSnapshotListener
                }

                android.util.Log.d(
                    TAG,
                    "snapshot.size=${snapshot.size()} | fromCache=${snapshot.metadata.isFromCache}"
                )

                messages.clear()

                for (doc in snapshot.documents) {
                    val text = doc.getString("text").orEmpty()
                    val senderId = doc.getString("senderId").orEmpty()
                    val discId = doc.getString("discussionId").orEmpty()
                    val ts = doc.getTimestamp("sentAt")
                    val timeStr = ts?.toDate()?.let { timeFormat.format(it) } ?: ""

                    val isMe = senderId == currentUserId

                    android.util.Log.d(
                        TAG,
                        "DOC id=${doc.id}, discussionIdField=$discId, text='$text', senderId=$senderId, currentUserId=$currentUserId, isMe=$isMe"
                    )

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

    // ============================================================
    // 2) Debug : lire TOUTE la collection messages une fois
    // ============================================================
    private fun debugAllMessagesOnce() {
        firestore.collection("messages")
            .get()
            .addOnSuccessListener { snapshot ->
                android.util.Log.d(TAG, "ALL MESSAGES size=${snapshot.size()}")

                for (doc in snapshot.documents) {
                    val text = doc.getString("text").orEmpty()
                    val senderId = doc.getString("senderId").orEmpty()
                    val discId = doc.getString("discussionId").orEmpty()
                    val ts = doc.getTimestamp("sentAt")
                    val timeStr = ts?.toDate()?.let { timeFormat.format(it) } ?: ""

                    android.util.Log.d(
                        TAG,
                        "ALL docId=${doc.id}, discussionId=$discId, senderId=$senderId, time=$timeStr, text='$text'"
                    )
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e(TAG, "Error loading ALL messages", e)
            }
    }

    // ============================================================
    // 3) Envoi d’un message
    // ============================================================
    private fun sendMessage() {
        val discussionId = chatId ?: run {
            android.util.Log.e(TAG, "sendMessage: chatId is null")
            return
        }
        val senderId = currentUserId ?: run {
            android.util.Log.e(TAG, "sendMessage: currentUserId is null")
            return
        }

        val text = binding.messageInput.text.toString().trim()
        if (text.isEmpty()) return

        val messageData = hashMapOf(
            "discussionId" to discussionId,
            "senderId" to senderId,
            "text" to text,
            "sentAt" to FieldValue.serverTimestamp(),
            "isRead" to false
        )

        android.util.Log.d(TAG, "sendMessage: $messageData")

        firestore.collection("messages")
            .add(messageData)
            .addOnSuccessListener { docRef ->
                android.util.Log.d(TAG, "Message added with id=${docRef.id}")

                // Update résumé discussion
                firestore.collection("discussions")
                    .document(discussionId)
                    .update(
                        mapOf(
                            "lastMessage" to text,
                            "lastMessageTime" to FieldValue.serverTimestamp()
                        )
                    )
                    .addOnSuccessListener {
                        android.util.Log.d(TAG, "Discussion updated with lastMessage")
                    }
                    .addOnFailureListener { e ->
                        android.util.Log.e(TAG, "Error updating discussion", e)
                    }

                binding.messageInput.setText("")
            }
            .addOnFailureListener { e ->
                android.util.Log.e(TAG, "Error sending message", e)
            }
    }
}
