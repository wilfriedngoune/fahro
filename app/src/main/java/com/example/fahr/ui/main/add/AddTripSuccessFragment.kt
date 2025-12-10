package com.example.fahr.ui.main.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fahr.R
import com.example.fahr.databinding.FragmentAddTripSuccessBinding
import com.example.fahr.ui.main.add.model.TripPayload
import com.example.fahr.ui.main.search.SearchFragment

class AddTripSuccessFragment : Fragment() {

    private lateinit var binding: FragmentAddTripSuccessBinding

    companion object {
        private const val ARG_TRIP_PAYLOAD = "trip_payload"

        fun newInstance(payload: TripPayload): AddTripSuccessFragment {
            val f = AddTripSuccessFragment()
            val args = Bundle()
            args.putSerializable(ARG_TRIP_PAYLOAD, payload)
            f.arguments = args
            return f
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddTripSuccessBinding.inflate(inflater, container, false)

        // Récupérer le payload (optionnel, juste pour affichage)
        val payload = arguments?.getSerializable(ARG_TRIP_PAYLOAD) as? TripPayload

        payload?.let {
            val stopsText = if (it.stops.isEmpty()) {
                "No stops"
            } else {
                it.stops.joinToString(separator = "\n") { s -> "• $s" }
            }

            val summary = """
                Date: ${it.departureDate}
                Time: ${it.departureTime}
                From: ${it.departureAddress}
                To: ${it.arrivalAddress}
                
                Stops:
                $stopsText
            """.trimIndent()

            binding.summaryText.text = summary
        }

        binding.buttonBackHome.setOnClickListener {
            // Retour à la page d'accueil (SearchFragment)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, SearchFragment())
                .commit()
        }

        return binding.root
    }
}
