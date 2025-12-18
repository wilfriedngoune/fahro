package com.example.fahr.ui.main.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
            val dep = binding.inputDeparture.text.toString().trim()
            val arr = binding.inputArrival.text.toString().trim()
            val time = binding.inputTime.text.toString().trim()

            if (dep.isEmpty() || arr.isEmpty() || time.isEmpty()) {
                // tu peux mettre un Toast si tu veux
                openSearchResults(dep, arr, time) // ou return si tu veux forcer les champs
            } else {
                // Sauvegarder cette recherche comme "recent search"
                saveRecentSearch(dep, arr, time)
                // Ouvrir les résultats
                openSearchResults(dep, arr, time)
            }
        }

        // Charger et afficher les 3 dernières recherches
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

    // ========= Recent searches (SharedPreferences) =========

    private fun saveRecentSearch(dep: String, arr: String, time: String) {
        val prefs = requireContext().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)

        val encodedNew = encodeRecent(dep, arr, time)
        val existing = prefs.getString("recent_searches", "") ?: ""

        val list = existing
            .split(";;")
            .filter { it.isNotBlank() && it != encodedNew }
            .toMutableList()

        // Ajouter la nouvelle recherche au début
        list.add(0, encodedNew)

        // Garder seulement les 3 dernières
        if (list.size > 3) {
            list.subList(3, list.size).clear()
        }

        val joined = list.joinToString(";;")
        prefs.edit().putString("recent_searches", joined).apply()
    }

    private fun loadRecentSearches(): List<RecentSearch> {
        val prefs = requireContext().getSharedPreferences("search_prefs", Context.MODE_PRIVATE)
        val stored = prefs.getString("recent_searches", "") ?: ""
        if (stored.isBlank()) return emptyList()

        return stored.split(";;")
            .filter { it.isNotBlank() }
            .mapNotNull { decodeRecent(it) }
    }

    private fun encodeRecent(dep: String, arr: String, time: String): String {
        // On évite les "||" dans les strings (assez safe pour notre cas)
        return "$dep||$arr||$time"
    }

    private fun decodeRecent(s: String): RecentSearch? {
        val parts = s.split("||")
        if (parts.size != 3) return null
        return RecentSearch(
            departure = parts[0],
            arrival = parts[1],
            time = parts[2]
        )
    }

    /**
     * Dynamically add recent search items under "Recent searches".
     */
    private fun populateRecentSearches() {
        val recentList = loadRecentSearches()

        val container = binding.recentSearchesContainer
        container.removeAllViews()

        if (recentList.isEmpty()) {
            // Rien à afficher (tu peux rajouter un petit texte "No recent searches" si tu veux)
            return
        }

        val inflater = LayoutInflater.from(requireContext())

        for (item in recentList) {
            val view = inflater.inflate(R.layout.item_recent_search, container, false)

            val textRoute = view.findViewById<TextView>(R.id.textRoute)
            val textTime = view.findViewById<TextView>(R.id.textTime)

            textRoute.text = "${item.departure} -> ${item.arrival}"
            textTime.text = item.time

            view.setOnClickListener {
                openSearchResults(item.departure, item.arrival, item.time)
            }

            container.addView(view)
        }
    }
}
