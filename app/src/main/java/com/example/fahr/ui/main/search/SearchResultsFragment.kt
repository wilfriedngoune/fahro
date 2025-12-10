package com.example.fahr.ui.main.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fahr.R
import com.example.fahr.databinding.FragmentSearchResultsBinding
import com.example.fahr.ui.main.search.adapter.TripAdapter
import com.example.fahr.ui.main.search.model.Trip

class SearchResultsFragment : Fragment() {

    private lateinit var binding: FragmentSearchResultsBinding

    companion object {
        fun newInstance(dep: String, arr: String, time: String): SearchResultsFragment {
            val f = SearchResultsFragment()
            val args = Bundle()
            args.putString("dep", dep)
            args.putString("arr", arr)
            args.putString("time", time)

            f.arguments = args
            return f
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchResultsBinding.inflate(inflater, container, false)

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // For now, dummy list of trips (you can later calculate based on dep/arr/time)
        val exampleTrips = listOf(
            Trip("1", "Nelson", R.drawable.nelson, "10:30 - 12:32", "Am Roseneck 1 -> Ostfalia", 4.8f),
            Trip("2", "Millena", R.drawable.millena, "11:15 - 15:08", "Berliner str. 12 -> TU Braunschweig", 4.5f),
            Trip("3", "Wilfried", R.drawable.wilfried, "12:05 - 17:20", "Exer 12 -> TU Clausthal", 5.0f)
        )

        binding.tripResultsList.layoutManager = LinearLayoutManager(requireContext())
        binding.tripResultsList.adapter = TripAdapter(exampleTrips) { trip ->
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    TripDetailsFragment.newInstance(trip.tripId)
                )
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }
}
