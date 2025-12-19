package com.example.fahr.ui.main.chat

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fahr.R
import com.example.fahr.core.UserSession
import com.example.fahr.databinding.FragmentChatBinding
import com.example.fahr.ui.main.chat.conversation.ConversationFragment
import com.example.fahr.ui.main.chat.model.ChatPreview
import com.example.fahr.ui.main.profile.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ChatFragment : Fragment() {

    private lateinit var binding: FragmentChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val allChats = mutableListOf<ChatPreview>()
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)

        chatAdapter = ChatAdapter(mutableListOf()) { chat ->
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    ConversationFragment.newInstance(chat.chatId, chat.username)
                )
                .addToBackStack(null)
                .commit()
        }

        binding.chatList.layoutManager = LinearLayoutManager(requireContext())
        binding.chatList.adapter = chatAdapter

        binding.searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = Unit
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilter(s?.toString().orEmpty())
            }
        })

        loadDiscussions()

        return binding.root
    }

    private fun loadDiscussions() {
        val currentUserId = UserSession.getCurrentUserId(requireContext()) ?: return

        firestore.collection("discussions")
            .whereEqualTo("driverId", currentUserId)
            .get()
            .addOnSuccessListener { snapDriver ->
                firestore.collection("discussions")
                    .whereEqualTo("passengerId", currentUserId)
                    .get()
                    .addOnSuccessListener { snapPassenger ->
                        val docs = (snapDriver.documents + snapPassenger.documents)
                            .distinctBy { it.id }

                        if (docs.isEmpty()) {
                            showChats(emptyList())
                        } else {
                            buildChatPreviews(docs, currentUserId)
                        }
                    }
            }
    }

    private fun buildChatPreviews(docs: List<com.google.firebase.firestore.DocumentSnapshot>, currentUserId: String) {
        if (!isAdded) return

        val result = mutableListOf<ChatPreview>()
        var remaining = docs.size

        fun doneOne() {
            remaining--
            if (remaining <= 0 && isAdded) {
                showChats(result)
            }
        }

        for (doc in docs) {
            val driverId = doc.getString("driverId")
            val passengerId = doc.getString("passengerId")

            if (driverId.isNullOrEmpty() || passengerId.isNullOrEmpty()) {
                doneOne(); continue
            }

            val otherUserId = if (currentUserId == driverId) passengerId else driverId

            firestore.collection("users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener { userSnap ->
                    val user = userSnap.toObject(UserProfile::class.java)
                    val username = user?.name ?: "User $otherUserId"
                    val avatarResName = user?.avatarResName
                    val avatarResId = avatarFromResName(avatarResName)

                    val lastMessage = doc.getString("lastMessage").orEmpty()
                    val lastTs = doc.getTimestamp("lastMessageTime")
                        ?: doc.getTimestamp("createdAt")
                    val lastTimeStr = lastTs?.toDate()?.let { timeFormat.format(it) } ?: ""

                    result.add(
                        ChatPreview(
                            chatId = doc.id,
                            username = username,
                            avatarRes = avatarResId,
                            lastMessage = if (lastMessage.isNotEmpty()) lastMessage else "No messages yet",
                            lastMessageTime = lastTimeStr
                        )
                    )
                    doneOne()
                }
                .addOnFailureListener {
                    doneOne()
                }
        }
    }

    private fun showChats(list: List<ChatPreview>) {
        allChats.clear()
        allChats.addAll(list)
        applyFilter(binding.searchBar.text?.toString().orEmpty())
    }

    private fun applyFilter(query: String) {
        val q = query.trim().lowercase()
        val filtered =
            if (q.isEmpty()) allChats
            else allChats.filter {
                it.username.lowercase().contains(q) ||
                        it.lastMessage.lowercase().contains(q)
            }

        chatAdapter.updateData(filtered)

        binding.emptyText.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.chatList.visibility =
            if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun avatarFromResName(name: String?): Int {
        if (name.isNullOrEmpty()) return R.drawable.ic_profile
        val resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
        return if (resId != 0) resId else R.drawable.ic_profile
    }
}
