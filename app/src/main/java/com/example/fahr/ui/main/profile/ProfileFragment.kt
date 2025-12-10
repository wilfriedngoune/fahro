package com.example.fahr.ui.main.profile

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fahr.R
import com.example.fahr.databinding.FragmentProfileBinding
import com.example.fahr.ui.main.profile.model.BookedTripProfile
import com.example.fahr.ui.main.profile.model.TripRequest

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Header edit icon: go to EditProfileFragment
        binding.buttonEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        // Policy link: go to PolicyFragment
        binding.textPolicy.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PolicyFragment())
                .addToBackStack(null)
                .commit()
        }

        // Theme switch (just a placeholder for now)
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            // TODO: implement theme change (dark/light)
            val msg = if (isChecked) "Dark mode ON" else "Dark mode OFF"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // Dummy user data for top section
        binding.avatar.setImageResource(R.drawable.wilfried)
        binding.userName.text = "Wilfried"
        binding.userRatingText.text = "4.9"
        binding.userBalance.text = "23,54 €"

        // Dummy personal info
        binding.infoName.text = "Name: Wilfried"
        binding.infoEmail.text = "Email: wilfried@example.com"
        binding.infoPhone.text = "Phone: +49 123 456789"
        binding.infoCar.text = "Car: VW Golf, Blue"
        binding.infoAddress.text = "Address: Berliner Str. 23, 38106 Braunschweig"
        binding.infoDescription.text = "Description: I love sharing trips with other students."
        binding.infoVerified.text = "Verified profile ✓"

        // Populate trips lists
        populateTripRequests()
        populateBookedTrips()

        // Logout button
        binding.buttonLogout.setOnClickListener {
            // TODO: implement real logout (clear session, go to login/onboarding)
            Toast.makeText(requireContext(), "Logout clicked", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }

    private fun populateTripRequests() {
        val list = listOf(
            TripRequest(
                id = "1",
                name = "Nelson",
                avatarResId = R.drawable.nelson,
                departure = "Roseneck 8a",
                arrival = "TU Clausthal",
                departureTime = "12:12",
                arrivalTime = "12:30",
                price = "€3.50"
            ),
            TripRequest(
                id = "2",
                name = "Millena",
                avatarResId = R.drawable.millena,
                departure = "Am Exer 12",
                arrival = "TU Braunschweig",
                departureTime = "09:00",
                arrivalTime = "09:45",
                price = "€2.80"
            )
        )

        val container = binding.tripRequestsContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (req in list) {
            val view = inflater.inflate(R.layout.item_trip_request_profile, container, false)

            val avatar = view.findViewById<ImageView>(R.id.avatar)
            val name = view.findViewById<TextView>(R.id.name)
            val route = view.findViewById<TextView>(R.id.route)
            val time = view.findViewById<TextView>(R.id.time)
            val price = view.findViewById<TextView>(R.id.price)
            val buttonAccept = view.findViewById<Button>(R.id.buttonAccept)

            avatar.setImageResource(req.avatarResId)
            name.text = req.name
            route.text = "${req.departure} -> ${req.arrival}"
            time.text = "${req.departureTime} - ${req.arrivalTime}"
            price.text = "Price: ${req.price}"

            buttonAccept.setOnClickListener {
                // TODO: call backend to accept
                buttonAccept.text = "Accepted"
                buttonAccept.isEnabled = false
            }

            container.addView(view)
        }
    }

    private fun populateBookedTrips() {
        val list = listOf(
            BookedTripProfile(
                id = "10",
                departure = "Roseneck 8a",
                arrival = "Ostfalia",
                departureTime = "08:25",
                arrivalTime = "09:10",
                status = "Pending"
            ),
            BookedTripProfile(
                id = "11",
                departure = "Exer 12",
                arrival = "TU Braunschweig",
                departureTime = "14:00",
                arrivalTime = "14:45",
                status = "Accepted"
            ),
            BookedTripProfile(
                id = "12",
                departure = "Berliner Str. 23",
                arrival = "TU Clausthal",
                departureTime = "17:30",
                arrivalTime = "18:20",
                status = "Denied"
            )
        )

        val container = binding.tripsBookedContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (trip in list) {
            val view = inflater.inflate(R.layout.item_trip_booked_profile, container, false)

            val route = view.findViewById<TextView>(R.id.route)
            val time = view.findViewById<TextView>(R.id.time)
            val status = view.findViewById<TextView>(R.id.status)

            route.text = "${trip.departure} -> ${trip.arrival}"
            time.text = "${trip.departureTime} - ${trip.arrivalTime}"
            status.text = trip.status

            when (trip.status) {
                "Pending" -> status.setTextColor(Color.parseColor("#FFA000"))
                "Accepted" -> status.setTextColor(Color.parseColor("#2E7D32"))
                "Denied" -> status.setTextColor(Color.parseColor("#C62828"))
            }

            container.addView(view)
        }
    }
}
