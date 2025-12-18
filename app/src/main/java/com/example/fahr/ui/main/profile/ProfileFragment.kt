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
import com.example.fahr.core.LocationUtils
import com.example.fahr.core.UserSession
import com.example.fahr.databinding.FragmentProfileBinding
import com.example.fahr.ui.main.profile.model.BookedTripProfile
import com.example.fahr.ui.main.profile.model.MyTripProfile
import com.example.fahr.ui.main.profile.model.TripRequest
import com.example.fahr.ui.main.profile.model.UserProfile
import com.example.fahr.ui.main.search.model.TripDocument
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    // Pour afficher createdAt proprement
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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
            // TODO: clear session: UserSession.setCurrentUserId(context, null)
            Toast.makeText(requireContext(), "Logout clicked (dummy)", Toast.LENGTH_SHORT).show()
        }

        // Charger les données Firestore
        loadUserProfile()
        loadTripRequests()   // bookings où driverId == currentUserId & status == "pending"
        loadBookedTrips()    // bookings où passengerId == currentUserId
        loadMyTrips()        // trips créés par currentUser

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
                    binding.avatar.setImageResource(R.drawable.ic_profile)
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

    // ===================================================================
    //  TRIP REQUESTS (Trip booked – waiting for YOUR validation)
    //  → bookings où driverId == currentUserId et status == "pending"
    // ===================================================================

    private fun loadTripRequests() {
        val userId = UserSession.getCurrentUserId(requireContext()) ?: "1"

        firestore.collection("bookings")
            .whereEqualTo("driverId", userId)
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                if (snapshot.isEmpty) {
                    binding.tripRequestsContainer.removeAllViews()
                    val tv = TextView(requireContext()).apply {
                        text = "No trip requests yet"
                        setTextColor(Color.GRAY)
                    }
                    binding.tripRequestsContainer.addView(tv)
                    return@addOnSuccessListener
                }

                buildTripRequestsFromBookings(snapshot.documents)
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to load trip requests", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Pour chaque booking où je suis driver, on va chercher:
     *  - le trip (trips/{tripId})
     *  - le passager (users/{passengerId})
     * et on construit une liste de TripRequest pour l’UI.
     */
    private fun buildTripRequestsFromBookings(bookingDocs: List<DocumentSnapshot>) {
        if (bookingDocs.isEmpty() || !isAdded) return

        val result = mutableListOf<TripRequest>()
        var remaining = bookingDocs.size

        fun doneOne() {
            remaining--
            if (remaining <= 0 && isAdded) {
                if (result.isEmpty()) {
                    binding.tripRequestsContainer.removeAllViews()
                    val tv = TextView(requireContext()).apply {
                        text = "No trip requests yet"
                        setTextColor(Color.GRAY)
                    }
                    binding.tripRequestsContainer.addView(tv)
                } else {
                    showTripRequests(result)
                }
            }
        }

        val ctx = requireContext()

        for (bookingDoc in bookingDocs) {
            val bookingId = bookingDoc.id
            val tripId = bookingDoc.getString("tripId")
            val passengerId = bookingDoc.getString("passengerId")

            if (tripId.isNullOrEmpty() || passengerId.isNullOrEmpty()) {
                doneOne()
                continue
            }

            // 1) Charger le trip
            firestore.collection("trips")
                .document(tripId)
                .get()
                .addOnSuccessListener { tripSnap ->
                    val trip = tripSnap.toObject(TripDocument::class.java)
                    if (trip == null) {
                        doneOne()
                        return@addOnSuccessListener
                    }

                    // Estimation heure d’arrivée pour l’affichage
                    val distance = LocationUtils.distanceBetweenAddresses(
                        ctx,
                        trip.departureAddress,
                        trip.arrivalAddress
                    )
                    val travelMinutes = if (distance != null) {
                        LocationUtils.estimateTravelMinutes(distance)
                    } else 30
                    val arrivalTime = LocationUtils.addMinutesToTime(trip.departureTime, travelMinutes)

                    // 2) Charger le passager
                    firestore.collection("users")
                        .document(passengerId)
                        .get()
                        .addOnSuccessListener { userSnap ->
                            val user = userSnap.toObject(UserProfile::class.java)
                            val name = user?.name ?: "Passenger $passengerId"
                            val avatarResId = avatarFromResName(user?.avatarResName)

                            val req = TripRequest(
                                id = bookingId,
                                name = name,
                                avatarResId = avatarResId,
                                departure = trip.departureAddress,
                                arrival = trip.arrivalAddress,
                                departureTime = trip.departureTime,
                                arrivalTime = arrivalTime,
                                price = String.format("€%.2f", trip.price)
                            )
                            result.add(req)
                            doneOne()
                        }
                        .addOnFailureListener {
                            doneOne()
                        }
                }
                .addOnFailureListener {
                    doneOne()
                }
        }
    }

    private fun showTripRequests(list: List<TripRequest>) {
        if (!isAdded) return

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
                updateBookingStatus(req.id, "accepted") {
                    buttonAccept.text = "Accepted"
                    buttonAccept.isEnabled = false
                    buttonRefuse.isEnabled = false
                }
            }

            buttonRefuse.setOnClickListener {
                updateBookingStatus(req.id, "denied") {
                    buttonRefuse.text = "Refused"
                    buttonAccept.isEnabled = false
                    buttonRefuse.isEnabled = false
                }
            }

            container.addView(view)
        }
    }

    private fun updateBookingStatus(bookingId: String, newStatus: String, onSuccessUI: () -> Unit) {
        firestore.collection("bookings")
            .document(bookingId)
            .update("status", newStatus)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                onSuccessUI()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to update status", Toast.LENGTH_SHORT).show()
            }
    }

    // ===================================================================
    //  TRIPS THAT YOU BOOKED
    //  → bookings où passengerId == currentUserId
    // ===================================================================

    private fun loadBookedTrips() {
        val userId = UserSession.getCurrentUserId(requireContext()) ?: "1"

        firestore.collection("bookings")
            .whereEqualTo("passengerId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                if (snapshot.isEmpty) {
                    binding.tripsBookedContainer.removeAllViews()
                    val tv = TextView(requireContext()).apply {
                        text = "No booked trips yet"
                        setTextColor(Color.GRAY)
                    }
                    binding.tripsBookedContainer.addView(tv)
                    return@addOnSuccessListener
                }

                buildBookedTripsFromBookings(snapshot.documents)
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to load booked trips", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buildBookedTripsFromBookings(bookingDocs: List<DocumentSnapshot>) {
        if (bookingDocs.isEmpty() || !isAdded) return

        val result = mutableListOf<BookedTripProfile>()
        var remaining = bookingDocs.size

        fun doneOne() {
            remaining--
            if (remaining <= 0 && isAdded) {
                if (result.isEmpty()) {
                    binding.tripsBookedContainer.removeAllViews()
                    val tv = TextView(requireContext()).apply {
                        text = "No booked trips yet"
                        setTextColor(Color.GRAY)
                    }
                    binding.tripsBookedContainer.addView(tv)
                } else {
                    showBookedTrips(result)
                }
            }
        }

        val ctx = requireContext()

        for (bookingDoc in bookingDocs) {
            val bookingId = bookingDoc.id
            val tripId = bookingDoc.getString("tripId")
            val driverId = bookingDoc.getString("driverId")
            val status = bookingDoc.getString("status") ?: "pending"

            if (tripId.isNullOrEmpty() || driverId.isNullOrEmpty()) {
                doneOne()
                continue
            }

            // 1) Charger le trip
            firestore.collection("trips")
                .document(tripId)
                .get()
                .addOnSuccessListener { tripSnap ->
                    val trip = tripSnap.toObject(TripDocument::class.java)
                    if (trip == null) {
                        doneOne()
                        return@addOnSuccessListener
                    }

                    val distance = LocationUtils.distanceBetweenAddresses(
                        ctx,
                        trip.departureAddress,
                        trip.arrivalAddress
                    )
                    val travelMinutes = if (distance != null) {
                        LocationUtils.estimateTravelMinutes(distance)
                    } else 30
                    val arrivalTime = LocationUtils.addMinutesToTime(trip.departureTime, travelMinutes)

                    // 2) Charger le driver
                    firestore.collection("users")
                        .document(driverId)
                        .get()
                        .addOnSuccessListener { userSnap ->
                            val user = userSnap.toObject(UserProfile::class.java)
                            val driverName = user?.name ?: "Driver $driverId"
                            val avatarResId = avatarFromResName(user?.avatarResName)

                            val booked = BookedTripProfile(
                                id = bookingId,
                                departure = trip.departureAddress,
                                arrival = trip.arrivalAddress,
                                departureTime = trip.departureTime,
                                arrivalTime = arrivalTime,
                                status = status,
                                driverName = driverName,
                                driverAvatarResId = avatarResId
                            )
                            result.add(booked)
                            doneOne()
                        }
                        .addOnFailureListener {
                            doneOne()
                        }
                }
                .addOnFailureListener {
                    doneOne()
                }
        }
    }

    private fun showBookedTrips(trips: List<BookedTripProfile>) {
        if (!isAdded) return

        val container = binding.tripsBookedContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        for (trip in trips) {
            val view = inflater.inflate(R.layout.item_trip_booked_profile, container, false)

            val avatar = view.findViewById<ImageView>(R.id.driverAvatar)
            val route = view.findViewById<TextView>(R.id.route)
            val time = view.findViewById<TextView>(R.id.time)
            val statusView = view.findViewById<TextView>(R.id.status)

            avatar.setImageResource(trip.driverAvatarResId)
            route.text = "${trip.driverName}: ${trip.departure} -> ${trip.arrival}"
            time.text = "${trip.departureTime} - ${trip.arrivalTime}"

            val statusLower = trip.status.lowercase()
            statusView.text = statusLower.replaceFirstChar { it.uppercase() } // "pending" -> "Pending"

            when (statusLower) {
                "pending" -> statusView.setTextColor(Color.parseColor("#FFA000"))
                "accepted" -> statusView.setTextColor(Color.parseColor("#2E7D32"))
                "denied" -> statusView.setTextColor(Color.parseColor("#C62828"))
                else -> statusView.setTextColor(Color.DKGRAY)
            }

            container.addView(view)
        }
    }

    // ===================================================================
    //  MY TRIPS (trips créés par l'utilisateur connecté)
    // ===================================================================

    private fun loadMyTrips() {
        val userId = UserSession.getCurrentUserId(requireContext()) ?: "1"

        firestore.collection("trips")
            .whereEqualTo("driverId", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!isAdded) return@addOnSuccessListener

                if (snapshot.isEmpty) {
                    binding.myTripsContainer.removeAllViews()
                    val tv = TextView(requireContext()).apply {
                        text = "You have not created any trips yet"
                        setTextColor(Color.GRAY)
                    }
                    binding.myTripsContainer.addView(tv)
                    return@addOnSuccessListener
                }

                val ctx = requireContext()
                val list = snapshot.documents.mapNotNull { doc ->
                    val trip = doc.toObject(TripDocument::class.java) ?: return@mapNotNull null

                    // estimation durée
                    val distance = LocationUtils.distanceBetweenAddresses(
                        ctx,
                        trip.departureAddress,
                        trip.arrivalAddress
                    )
                    val travelMinutes = if (distance != null) {
                        LocationUtils.estimateTravelMinutes(distance)
                    } else 30
                    val arrivalTime = LocationUtils.addMinutesToTime(trip.departureTime, travelMinutes)

                    val ts = doc.getTimestamp("createdAt")
                    val createdAtText = ts?.toDate()?.let { dateTimeFormat.format(it) } ?: "-"

                    MyTripProfile(
                        id = doc.id,
                        departure = trip.departureAddress,
                        arrival = trip.arrivalAddress,
                        departureTime = trip.departureTime,
                        arrivalTime = arrivalTime,
                        createdAtText = createdAtText
                    )
                }

                showMyTrips(list)
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to load your trips", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showMyTrips(trips: List<MyTripProfile>) {
        if (!isAdded) return

        val container = binding.myTripsContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())

        if (trips.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "You have not created any trips yet"
                setTextColor(Color.GRAY)
            }
            container.addView(tv)
            return
        }

        for (trip in trips) {
            val view = inflater.inflate(R.layout.item_my_trip_profile, container, false)

            val textRoute = view.findViewById<TextView>(R.id.textRoute)
            val textTime = view.findViewById<TextView>(R.id.textTime)
            val textCreatedAt = view.findViewById<TextView>(R.id.textCreatedAt)
            val buttonDelete = view.findViewById<Button>(R.id.buttonDeleteTrip)

            textRoute.text = "${trip.departure} -> ${trip.arrival}"
            textTime.text = "${trip.departureTime} - ${trip.arrivalTime}"
            textCreatedAt.text = "Created at: ${trip.createdAtText}"

            buttonDelete.setOnClickListener {
                deleteTrip(trip.id) {
                    // Supprimer la vue de l’UI
                    container.removeView(view)
                    if (container.childCount == 0) {
                        val tv = TextView(requireContext()).apply {
                            text = "You have not created any trips yet"
                            setTextColor(Color.GRAY)
                        }
                        container.addView(tv)
                    }
                }
            }

            container.addView(view)
        }
    }

    private fun deleteTrip(tripId: String, onSuccessUI: () -> Unit) {
        firestore.collection("trips")
            .document(tripId)
            .delete()
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Trip deleted", Toast.LENGTH_SHORT).show()
                onSuccessUI()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to delete trip", Toast.LENGTH_SHORT).show()
            }
    }

    // ---------- Utils ----------

    private fun avatarFromResName(name: String?): Int {
        if (name.isNullOrEmpty()) return R.drawable.ic_profile
        val resId = resources.getIdentifier(name, "drawable", requireContext().packageName)
        return if (resId != 0) resId else R.drawable.ic_profile
    }
}
