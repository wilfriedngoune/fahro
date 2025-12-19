package com.example.fahr.ui.main.search

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fahr.R
import com.example.fahr.core.LocationUtils
import com.example.fahr.core.UserSession
import com.example.fahr.databinding.FragmentTripDetailsBinding
import com.example.fahr.ui.main.profile.model.UserProfile
import com.example.fahr.ui.main.search.model.TripDocument
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class TripDetailsFragment : Fragment() {

    private lateinit var binding: FragmentTripDetailsBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private var itineraryExpanded = false
    private var loadedTrip: TripDocument? = null
    private var tripId: String? = null

    companion object {
        private const val ARG_TRIP_ID = "tripId"

        fun newInstance(tripId: String): TripDetailsFragment {
            val f = TripDetailsFragment()
            val args = Bundle()
            args.putString(ARG_TRIP_ID, tripId)
            f.arguments = args
            return f
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tripId = arguments?.getString(ARG_TRIP_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTripDetailsBinding.inflate(inflater, container, false)

        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.itineraryContent.visibility = View.GONE
        binding.itineraryToggle.rotation = 0f
        binding.itineraryHeader.setOnClickListener {
            itineraryExpanded = !itineraryExpanded
            binding.itineraryContent.visibility =
                if (itineraryExpanded) View.VISIBLE else View.GONE
            binding.itineraryToggle.rotation = if (itineraryExpanded) 180f else 0f
        }

        binding.buttonBook.setOnClickListener {
            bookThisTrip()
        }

        loadTripDetails()

        return binding.root
    }


    private fun loadTripDetails() {
        val id = tripId ?: return

        firestore.collection("trips")
            .document(id)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener

                val trip = doc.toObject(TripDocument::class.java)
                if (trip == null) {
                    Toast.makeText(requireContext(), "Trip not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                loadedTrip = trip
                bindTrip(trip)          // infos trajet + itinéraire
                loadDriverProfile(trip) // infos driver
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Error loading trip", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadDriverProfile(trip: TripDocument) {
        val driverId = trip.driverId
        if (driverId.isBlank()) return

        firestore.collection("users")
            .document(driverId)
            .get()
            .addOnSuccessListener { doc ->
                if (!isAdded) return@addOnSuccessListener

                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) {
                    bindDriver(profile)
                } else {
                    // fallback si pas de profil
                    binding.driverName.text = "Driver $driverId"
                    binding.driverAvatar.setImageResource(R.drawable.ic_profile)
                    binding.ratingText.text = "-"
                    binding.carType.text = ""
                }
            }
            .addOnFailureListener {
            }
    }

    private fun bindDriver(p: UserProfile) {
        val avatarResId = avatarFromResName(p.avatarResName)
        binding.driverAvatar.setImageResource(avatarResId)

        binding.driverName.text = p.name.ifEmpty { "Driver ${p.id}" }
        binding.ratingText.text =
            if (p.rating > 0) String.format("%.1f", p.rating) else "-"

        binding.carType.text =
            if (p.car.isNotBlank()) "Car: ${p.car}" else ""
    }

    private fun avatarFromResName(name: String?): Int {
        if (name.isNullOrEmpty()) return R.drawable.ic_profile
        val resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
        return if (resId != 0) resId else R.drawable.ic_profile
    }



    private fun bindTrip(trip: TripDocument) {
        if (!isAdded) return

        binding.driverName.text = "Driver ${trip.driverId.ifBlank { "unknown" }}"
        binding.ratingText.text = String.format("%.1f", 4.8)
        binding.carType.text = "Car: VW Golf • Blue"
        binding.driverAvatar.setImageResource(R.drawable.ic_profile)

        binding.departure.text = "Departure: ${trip.departureAddress}"
        binding.arrival.text = "Arrival: ${trip.arrivalAddress}"
        binding.price.text = "Price: €${String.format("%.2f", trip.price)}"

        val ctx = requireContext()
        val depTime = trip.departureTime

        val allAddresses = mutableListOf<String>()
        allAddresses.add(trip.departureAddress)
        allAddresses.addAll(trip.stops)
        allAddresses.add(trip.arrivalAddress)

        binding.itineraryContent.removeAllViews()


        val segmentMinutes = mutableListOf<Int>()
        var totalMinutes = 0

        for (i in 0 until allAddresses.size - 1) {
            val startAddr = allAddresses[i]
            val endAddr = allAddresses[i + 1]

            val distanceKm = LocationUtils.distanceBetweenAddresses(ctx, startAddr, endAddr)
                ?: 1.0

            val minutes = LocationUtils.estimateTravelMinutes(distanceKm)
            segmentMinutes.add(minutes)
            totalMinutes += minutes
        }

        var currentTime = depTime

        for (i in allAddresses.indices) {
            val labelTime = currentTime

            val tv = TextView(requireContext()).apply {
                text = "• ${allAddresses[i]} — $labelTime"
                textSize = 14f
                setTextColor(Color.parseColor("#444444"))
                setPadding(0, 4, 0, 4)
            }
            binding.itineraryContent.addView(tv)

            if (i < segmentMinutes.size) {
                currentTime = LocationUtils.addMinutesToTime(currentTime, segmentMinutes[i])
            }
        }

        // currentTime = heure d’arrivée finale
        binding.time.text = "Departure: $depTime • Arrival: $currentTime"
    }



    private fun bookThisTrip() {
        val trip = loadedTrip ?: run {
            Toast.makeText(requireContext(), "Trip not loaded yet", Toast.LENGTH_SHORT).show()
            return
        }
        val id = tripId ?: return
        val currentUserId = UserSession.getCurrentUserId(requireContext())
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "No current user id", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUserId == trip.driverId) {
            Toast.makeText(requireContext(), "You cannot book your own trip", Toast.LENGTH_SHORT).show()
            return
        }

        val booking = hashMapOf(
            "tripId" to id,
            "driverId" to trip.driverId,
            "passengerId" to currentUserId,
            "status" to "pending",
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("bookings")
            .add(booking)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, BookingSuccessFragment())
                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Error booking trip", Toast.LENGTH_SHORT).show()
            }
    }
}
