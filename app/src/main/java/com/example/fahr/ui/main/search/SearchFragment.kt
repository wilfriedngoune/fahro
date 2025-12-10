package com.example.fahr.ui.main.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.fahr.R
import com.example.fahr.databinding.FragmentSearchBinding
import com.example.fahr.ui.main.search.model.RecentSearch

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Button Search: go to results with current form values
        binding.buttonSearch.setOnClickListener {
            val dep = binding.inputDeparture.text.toString()
            val arr = binding.inputArrival.text.toString()
            val time = binding.inputTime.text.toString()

            openSearchResults(dep, arr, time)
        }

        // Fill recent searches list (dummy data for now)
        populateRecentSearches()

        return binding.root
    }

    /**
     * Open results screen with given parameters.
     */
    private fun openSearchResults(dep: String, arr: String, time: String) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainer,
                SearchResultsFragment.newInstance(dep, arr, time)
            )
            .addToBackStack(null)
            .commit()
    }

    /**
     * Dynamically add recent search items under "Recent searches".
     */
    private fun populateRecentSearches() {
        // Example recent searches (later you can load from SharedPreferences / DB)
        val recentList = listOf(
            RecentSearch("Berliner Str. 23", "TU Clausthal", "08:25"),
            RecentSearch("Am Roseneck 1", "Ostfalia", "09:10"),
            RecentSearch("Exer 12", "TU Braunschweig", "14:45")
        )

        val container = binding.recentSearchesContainer
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        for (item in recentList) {
            // Inflate the layout for one recent search
            val view = inflater.inflate(R.layout.item_recent_search, container, false)

            val textRoute = view.findViewById<android.widget.TextView>(R.id.textRoute)
            val textTime = view.findViewById<android.widget.TextView>(R.id.textTime)

            textRoute.text = "${item.departure} -> ${item.arrival}"
            textTime.text = item.time

            // Click: open SearchResultsFragment with this search
            view.setOnClickListener {
                openSearchResults(item.departure, item.arrival, item.time)
            }

            container.addView(view)
        }
    }
}
