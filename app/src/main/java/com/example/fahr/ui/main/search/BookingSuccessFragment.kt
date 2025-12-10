package com.example.fahr.ui.main.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fahr.databinding.FragmentBookingSuccessBinding

class BookingSuccessFragment : Fragment() {

    private lateinit var binding: FragmentBookingSuccessBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBookingSuccessBinding.inflate(inflater, container, false)

        binding.buttonBackToSearch.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    com.example.fahr.R.id.fragmentContainer,
                    SearchFragment()
                )
                .commit()
        }

        return binding.root
    }
}
