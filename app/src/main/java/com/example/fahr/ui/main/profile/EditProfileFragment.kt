package com.example.fahr.ui.main.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fahr.core.UserSession
import com.example.fahr.databinding.FragmentEditProfileBinding
import com.example.fahr.ui.main.profile.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)

        // Back arrow
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadProfile()

        // Bouton "Save"
        binding.buttonSave.setOnClickListener {
            saveProfile()
        }

        return binding.root
    }


    private fun loadProfile() {
        val userId = UserSession.getCurrentUserId(requireContext()) ?: "1"

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val profile = doc.toObject(UserProfile::class.java)
                    if (profile != null) {
                        binding.inputName.setText(profile.name)
                        binding.inputEmail.setText(profile.email)
                        binding.inputPhone.setText(profile.phone)
                        binding.inputCar.setText(profile.car)
                        binding.inputAddress.setText(profile.address)
                        binding.inputDescription.setText(profile.description)
                    }
                } else {
                    // Aucun profil encore : tu peux mettre des valeurs par défaut ici si tu veux
                    // (pour l'instant, on laisse tout vide)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading profile", Toast.LENGTH_SHORT).show()
            }
    }


    private fun saveProfile() {
        val userId = UserSession.getCurrentUserId(requireContext()) ?: "1"

        val name = binding.inputName.text.toString().trim()
        val email = binding.inputEmail.text.toString().trim()
        val phone = binding.inputPhone.text.toString().trim()
        val car = binding.inputCar.text.toString().trim()
        val address = binding.inputAddress.text.toString().trim()
        val description = binding.inputDescription.text.toString().trim()

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Name and email are required", Toast.LENGTH_SHORT).show()
            return
        }

        // On n'envoie QUE ces champs-là
        val updates = hashMapOf<String, Any>(
            "id" to userId,
            "name" to name,
            "email" to email,
            "phone" to phone,
            "car" to car,
            "address" to address,
            "description" to description
        )

        firestore.collection("users")
            .document(userId)
            // MERGE = Firestore garde rating, balance, avatarResName, verified comme avant
            .set(updates, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error saving profile", Toast.LENGTH_SHORT).show()
            }
    }
}
