package com.example.fahr.ui.main.search

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.fahr.R
import com.example.fahr.databinding.FragmentTripDetailsBinding

class TripDetailsFragment : Fragment() {

    private lateinit var binding: FragmentTripDetailsBinding
    private var itineraryExpanded = false

    companion object {
        fun newInstance(tripId: String): TripDetailsFragment {
            val f = TripDetailsFragment()
            val args = Bundle()
            args.putString("tripId", tripId)
            f.arguments = args
            return f
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTripDetailsBinding.inflate(inflater, container, false)

        // Back button
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Dummy data for now, you can fetch real trip details later
        val rating = 4.8f

        binding.driverName.text = "Wilfried"
        binding.ratingText.text = String.format("%.1f", rating)
        binding.carType.text = "Car: VW Golf • Blue"
        binding.departure.text = "Departure: Roseneck 8a"
        binding.arrival.text = "Arrival: TU Clausthal"
        binding.time.text = "Departure: 12:12 • Arrival: 12:30"
        binding.price.text = "Price: €1.23"
        binding.driverAvatar.setImageResource(R.drawable.wilfried)

        // Build dummy itinerary list
        val stops = listOf(
            "Roseneck 8a — 12:12",
            "Berliner Str. 23 — 12:21",
            "TU Clausthal — 12:30"
        )

        // Fill itinerary content dynamically
        binding.itineraryContent.removeAllViews()
        for (stop in stops) {
            val tv = TextView(requireContext()).apply {
                text = "• $stop"
                textSize = 14f
                setTextColor(Color.parseColor("#444444"))
                setPadding(0, 4, 0, 4)
            }
            binding.itineraryContent.addView(tv)
        }

        // Initially collapsed
        binding.itineraryContent.visibility = View.GONE
        binding.itineraryToggle.rotation = 0f

        // Toggle itinerary section on header click
        binding.itineraryHeader.setOnClickListener {
            itineraryExpanded = !itineraryExpanded
            binding.itineraryContent.visibility =
                if (itineraryExpanded) View.VISIBLE else View.GONE
            binding.itineraryToggle.rotation = if (itineraryExpanded) 180f else 0f
        }

        binding.buttonBook.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, BookingSuccessFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }
}
