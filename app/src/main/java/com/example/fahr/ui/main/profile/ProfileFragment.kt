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
import com.example.fahr.core.UserSession
import com.example.fahr.databinding.FragmentProfileBinding
import com.example.fahr.ui.main.profile.model.BookedTripProfile
import com.example.fahr.ui.main.profile.model.TripRequest
import com.example.fahr.ui.main.profile.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)

        // Edit profile
        binding.buttonEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        // Policy
        binding.textPolicy.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, PolicyFragment())
                .addToBackStack(null)
                .commit()
        }

        // Theme switch placeholder
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            val msg = if (isChecked) "Dark mode ON (dummy)" else "Dark mode OFF (dummy)"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        // Logout
        binding.buttonLogout.setOnClickListener {
            // TODO: clear session (UserSession.setCurrentUserId(context, null) ou autre)
            Toast.makeText(requireContext(), "Logout clicked (dummy)", Toast.LENGTH_SHORT).show()
        }

        // Charger données depuis Firestore
        loadUserProfile()
        loadTripRequests()
        loadBookedTrips()

        return binding.root
    }

    // ---------- USER PROFILE ----------

    private fun loadUserProfile() {
        val userId = UserSession.getCurrentUserId(requireContext()) ?: "1"

        firestore.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) {
                    // Si pas de doc -> valeurs par défaut
                    binding.avatar.setImageResource(R.drawable.wilfried)
                    binding.userName.text = "Unknown user"
                    binding.userRatingText.text = "-"
                    binding.userBalance.text = "0.00 €"
                    binding.infoVerified.text = "Not verified"
                    return@addOnSuccessListener
                }

                val profile = doc.toObject(UserProfile::class.java)
                if (profile != null) {
                    showProfile(profile)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showProfile(p: UserProfile) {
        // avatar par nom de res
        val resId = resources.getIdentifier(p.avatarResName, "drawable", requireContext().packageName)
        if (resId != 0) {
            binding.avatar.setImageResource(resId)
        } else {
            binding.avatar.setImageResource(R.drawable.ic_profile)
        }

        binding.userName.text = p.name.ifEmpty { "No name" }
        binding.userRatingText.text = if (p.rating > 0) String.format("%.1f", p.rating) else "-"
        binding.userBalance.text = String.format("%.2f €", p.balance)

        binding.infoName.text = "Name: ${p.name}"
        binding.infoEmail.text = "Email: ${p.email}"
        binding.infoPhone.text = "Phone: ${p.phone}"
        binding.infoCar.text = "Car: ${p.car}"
        binding.infoAddress.text = "Address: ${p.address}"
        binding.infoDescription.text = "Description: ${p.description}"
        binding.infoVerified.text =
            if (p.verified) "Verified profile ✓" else "Profile not verified"
    }

    // ---------- TRIP REQUESTS (Trip booked – waiting for YOUR validation) ----------

    private fun loadTripRequests() {
        val userId = UserSession.getCurrentUserId(requireContext()) ?: "1"

        firestore.collection("trip_requests_for_me")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    // "No trip yet"
                    binding.tripRequestsContainer.removeAllViews()
                    val tv = TextView(requireContext()).apply {
                        text = "No trip requests yet"
                        setTextColor(Color.GRAY)
                    }
                    binding.tripRequestsContainer.addView(tv)
                    return@addOnSuccessListener
                }

                val requests = snapshot.documents.mapNotNull { doc ->
                    TripRequest(
                        id = doc.id,
                        name = doc.getString("fromUserName") ?: "",
                        avatarResId = avatarFromResName(doc.getString("fromUserAvatarResName")),
                        departure = doc.getString("departure") ?: "",
                        arrival = doc.getString("arrival") ?: "",
                        departureTime = doc.getString("departureTime") ?: "",
                        arrivalTime = doc.getString("arrivalTime") ?: "",
                        price = String.format("€%.2f", doc.getDouble("price") ?: 0.0)
                    )
                }

                showTripRequests(requests)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load trip requests", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showTripRequests(list: List<TripRequest>) {
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
            val buttonRefuse = view.findViewById<Button>(R.id.buttonRefuse)

            avatar.setImageResource(req.avatarResId)
            name.text = req.name
            route.text = "${req.departure} -> ${req.arrival}"
            time.text = "${req.departureTime} - ${req.arrivalTime}"
            price.text = "Price: ${req.price}"

            buttonAccept.setOnClickListener {
                updateTripRequestStatus(req.id, "Accepted") {
                    buttonAccept.text = "Accepted"
                    buttonAccept.isEnabled = false
                    buttonRefuse.isEnabled = false
                }
            }

            buttonRefuse.setOnClickListener {
                updateTripRequestStatus(req.id, "Denied") {
                    buttonRefuse.text = "Refused"
                    buttonAccept.isEnabled = false
                    buttonRefuse.isEnabled = false
                }
            }

            container.addView(view)
        }
    }

    private fun updateTripRequestStatus(requestId: String, newStatus: String, onSuccessUI: () -> Unit) {
        firestore.collection("trip_requests_for_me")
            .document(requestId)
            .update("status", newStatus)
            .addOnSuccessListener {
                onSuccessUI()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------- TRIPS THAT YOU BOOKED ----------

    private fun loadBookedTrips() {
        val userId = UserSession.getCurrentUserId(requireContext()) ?: "1"

        firestore.collection("booked_trips")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    binding.tripsBookedContainer.removeAllViews()
                    val tv = TextView(requireContext()).apply {
                        text = "No booked trips yet"
                        setTextColor(Color.GRAY)
                    }
                    binding.tripsBookedContainer.addView(tv)
                    return@addOnSuccessListener
                }

                val trips = snapshot.documents.mapNotNull { doc ->
                    BookedTripProfile(
                        id = doc.id,
                        departure = doc.getString("departure") ?: "",
                        arrival = doc.getString("arrival") ?: "",
                        departureTime = doc.getString("departureTime") ?: "",
                        arrivalTime = doc.getString("arrivalTime") ?: "",
                        status = doc.getString("status") ?: "Pending"
                    )
                }

                showBookedTrips(trips, snapshot.documents)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load booked trips", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showBookedTrips(trips: List<BookedTripProfile>, docs: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val container = binding.tripsBookedContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for ((index, trip) in trips.withIndex()) {
            val doc = docs[index]
            val driverName = doc.getString("driverName") ?: ""
            val avatarName = doc.getString("driverAvatarResName") ?: "ic_profile"
            val avatarId = avatarFromResName(avatarName)

            val view = inflater.inflate(R.layout.item_trip_booked_profile, container, false)

            val avatar = view.findViewById<ImageView>(R.id.driverAvatar)
            val route = view.findViewById<TextView>(R.id.route)
            val time = view.findViewById<TextView>(R.id.time)
            val status = view.findViewById<TextView>(R.id.status)

            avatar.setImageResource(avatarId)
            route.text = "$driverName: ${trip.departure} -> ${trip.arrival}"
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

    // ---------- Utils ----------

    private fun avatarFromResName(name: String?): Int {
        if (name.isNullOrEmpty()) return R.drawable.ic_profile
        val resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
        return if (resId != 0) resId else R.drawable.ic_profile
    }
}
