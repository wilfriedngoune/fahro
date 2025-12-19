package com.example.fahr.ui.main.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.fahr.R
import com.example.fahr.core.LocationUtils
import com.example.fahr.core.UserSession
import com.example.fahr.databinding.FragmentSearchResultsBinding
import com.example.fahr.ui.main.profile.model.UserProfile
import com.example.fahr.ui.main.search.adapter.TripAdapter
import com.example.fahr.ui.main.search.model.Trip
import com.example.fahr.ui.main.search.model.TripDocument
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs

class SearchResultsFragment : Fragment() {

    private lateinit var binding: FragmentSearchResultsBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var searchDeparture: String = ""
    private var searchArrival: String = ""
    private var searchTime: String = ""

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

        searchDeparture = arguments?.getString("dep").orEmpty()
        searchArrival = arguments?.getString("arr").orEmpty()
        searchTime = arguments?.getString("time").orEmpty()

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.resultsTitle.text = "Available trips for your search"

        binding.tripResultsList.layoutManager = LinearLayoutManager(requireContext())

        searchTrips()

        return binding.root
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            binding.tripResultsList.visibility = View.GONE
            binding.emptyText.visibility = View.GONE
        }
    }

    private fun showEmptyState() {
        binding.tripResultsList.visibility = View.GONE
        binding.emptyText.visibility = View.VISIBLE
    }

    private fun showResults(trips: List<Trip>) {
        binding.emptyText.visibility = View.GONE
        binding.tripResultsList.visibility = View.VISIBLE
        binding.tripResultsList.adapter = TripAdapter(trips) { trip ->
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    TripDetailsFragment.newInstance(trip.id)
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun searchTrips() {
        showLoading(true)

        // Validation simple de l’heure
        if (!isValidTime(searchTime)) {
            Toast.makeText(requireContext(), "Invalid time format (use HH:mm)", Toast.LENGTH_SHORT).show()
        }

        firestore.collection("trips")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                if (snapshot.isEmpty) {
                    showLoading(false)
                    showEmptyState()
                    return@addOnSuccessListener
                }

                val ctx = requireContext()
                val radiusMeters = 1000.0
                val requestedMinutes = parseTimeToMinutes(searchTime)
                val currentUserId = UserSession.getCurrentUserId(ctx)

                val matchingTrips = snapshot.documents.mapNotNull { doc ->
                    val trip = doc.toObject(TripDocument::class.java) ?: return@mapNotNull null
                    val tripId = doc.id

                    if (currentUserId != null && trip.driverId == currentUserId) {
                        return@mapNotNull null
                    }

                    val depDist = LocationUtils.distanceBetweenAddresses(
                        ctx,
                        searchDeparture,
                        trip.departureAddress
                    )
                    val arrDist = LocationUtils.distanceBetweenAddresses(
                        ctx,
                        searchArrival,
                        trip.arrivalAddress
                    )

                    var matches = false

                    if (depDist != null && arrDist != null &&
                        depDist <= radiusMeters && arrDist <= radiusMeters
                    ) {
                        matches = true
                    }

                    if (!matches && trip.stops.isNotEmpty()) {
                        for (stop in trip.stops) {
                            val depStopDist = LocationUtils.distanceBetweenAddresses(
                                ctx,
                                searchDeparture,
                                stop
                            )
                            val arrStopDist = LocationUtils.distanceBetweenAddresses(
                                ctx,
                                searchArrival,
                                stop
                            )
                            if ((depStopDist != null && depStopDist <= radiusMeters) ||
                                (arrStopDist != null && arrStopDist <= radiusMeters)
                            ) {
                                matches = true
                                break
                            }
                        }
                    }

                    if (!matches) return@mapNotNull null

                    Pair(tripId, trip)
                }

                if (matchingTrips.isEmpty()) {
                    showLoading(false)
                    showEmptyState()
                    return@addOnSuccessListener
                }

                loadDriversAndBuildTrips(matchingTrips, requestedMinutes)
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                showLoading(false)
                Toast.makeText(requireContext(), "Error loading trips", Toast.LENGTH_SHORT).show()
                showEmptyState()
            }
    }


    private fun loadDriversAndBuildTrips(
        matchingTrips: List<Pair<String, TripDocument>>,
        requestedMinutes: Int?
    ) {
        if (matchingTrips.isEmpty() || !isAdded) {
            showLoading(false)
            showEmptyState()
            return
        }

        val ctx = requireContext()
        val result = mutableListOf<Triple<Trip, Int, Int>>() // Trip, depMinutes, diffWithRequested
        val cacheUsers = mutableMapOf<String, UserProfile>()
        var remaining = matchingTrips.size

        fun doneOne() {
            remaining--
            if (remaining <= 0 && isAdded) {
                showLoading(false)
                if (result.isEmpty()) {
                    showEmptyState()
                } else {
                    // Trier par proximité de l’heure demandée
                    val sorted = result.sortedBy { it.third }
                    showResults(sorted.map { it.first })
                }
            }
        }

        for ((tripId, tripDoc) in matchingTrips) {
            val driverId = tripDoc.driverId
            if (driverId.isEmpty()) {
                doneOne()
                continue
            }

            val depMinutes = parseTimeToMinutes(tripDoc.departureTime)
            if (depMinutes == null) {
                doneOne()
                continue
            }


            val diff = if (requestedMinutes != null) {
                val d = abs(depMinutes - requestedMinutes)
                if (d > 30) {
                    doneOne()
                    continue
                }
                d
            } else {
                depMinutes
            }

            val distance = LocationUtils.distanceBetweenAddresses(
                ctx,
                tripDoc.departureAddress,
                tripDoc.arrivalAddress
            )
            val travelMinutes = if (distance != null) {
                LocationUtils.estimateTravelMinutes(distance)
            } else 30
            val arrivalTime = LocationUtils.addMinutesToTime(tripDoc.departureTime, travelMinutes)

            val cachedUser = cacheUsers[driverId]
            if (cachedUser != null) {
                val trip = buildTripViewModel(tripId, tripDoc, cachedUser, arrivalTime)
                result.add(Triple(trip, depMinutes, diff))
                doneOne()
                continue
            }

            firestore.collection("users")
                .document(driverId)
                .get()
                .addOnSuccessListener { userSnap ->
                    if (!isAdded) {
                        doneOne()
                        return@addOnSuccessListener
                    }
                    val user = userSnap.toObject(UserProfile::class.java)
                    if (user != null) {
                        cacheUsers[driverId] = user
                        val trip = buildTripViewModel(tripId, tripDoc, user, arrivalTime)
                        result.add(Triple(trip, depMinutes, diff))
                    }
                    doneOne()
                }
                .addOnFailureListener {
                    if (!isAdded) return@addOnFailureListener
                    doneOne()
                }
        }
    }

    private fun buildTripViewModel(
        tripId: String,
        tripDoc: TripDocument,
        driver: UserProfile,
        arrivalTime: String
    ): Trip {
        val avatarResId = avatarFromResName(driver.avatarResName)
        val rating = if (driver.rating > 0) driver.rating.toFloat() else 0f
        val timeDisplay = "${tripDoc.departureTime} - $arrivalTime"
        val addressDisplay = "${tripDoc.departureAddress} -> ${tripDoc.arrivalAddress}"

        return Trip(
            id = tripId,
            driverId = driver.id,
            driverName = if (driver.name.isNotBlank()) driver.name else "Driver ${driver.id}",
            driverAvatarResId = avatarResId,
            departureTimeRange = timeDisplay,
            routeSummary = addressDisplay,
            rating = rating
        )
    }


    private fun avatarFromResName(name: String?): Int {
        if (name.isNullOrEmpty()) return R.drawable.ic_profile
        val resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
        return if (resId != 0) resId else R.drawable.ic_profile
    }

    private fun isValidTime(time: String): Boolean {
        val regex = Regex("^([01]\\d|2[0-3]):[0-5]\\d\$")
        return regex.matches(time)
    }

    private fun parseTimeToMinutes(time: String): Int? {
        val parts = time.split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        return h * 60 + m
    }
}
