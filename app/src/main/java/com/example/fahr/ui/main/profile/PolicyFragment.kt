package com.example.fahr.ui.main.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fahr.databinding.FragmentPolicyBinding

class PolicyFragment : Fragment() {

    private lateinit var binding: FragmentPolicyBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPolicyBinding.inflate(inflater, container, false)

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return binding.root
    }
}
