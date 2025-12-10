package com.example.fahr.ui.main.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fahr.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Pre-fill with existing dummy data (optional)
        binding.inputName.setText("Wilfried")
        binding.inputEmail.setText("wilfried@example.com")
        binding.inputPhone.setText("+49 123 456789")
        binding.inputCar.setText("VW Golf, Blue")
        binding.inputAddress.setText("Berliner Str. 23, 38106 Braunschweig")
        binding.inputDescription.setText("I love sharing trips with other students.")

        binding.buttonSave.setOnClickListener {
            // TODO: save changes to backend / local storage
            Toast.makeText(requireContext(), "Profile updated (dummy)", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }
}
