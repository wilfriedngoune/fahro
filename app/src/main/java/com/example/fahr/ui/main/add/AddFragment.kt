package com.example.fahr.ui.main.add

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.fahr.R
import com.example.fahr.core.UserSession
import com.example.fahr.databinding.FragmentAddBinding
import com.example.fahr.ui.main.add.model.TripPayload
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AddFragment : Fragment() {

    private lateinit var binding: FragmentAddBinding
    private val stops = mutableListOf<String>()

    // 🔹 Firestore instance (using KTX)
    private val firestore by lazy { Firebase.firestore }

    // ---------- REGEX / VALIDATION ----------

    // Adresse : lettres (accentuées ok), chiffres, espace, ., , ' / -
    // longueur 5 à 120 caractères
    private val ADDRESS_REGEX = Regex(
        pattern = """^[\p{L}\p{N}\s,.'’/\-#()°]+$"""
    )

    // Heure : HH:mm (24h)
    private val TIME_REGEX =
        Regex("""^([01]\d|2[0-3]):[0-5]\d$""")

    // Date : jj/MM/aaaa
    private val DATE_REGEX =
        Regex("""^([0-2]\d|3[01])/(0\d|1[0-2])/\d{4}$""")

    private fun isValidAddress(addr: String): Boolean =
        ADDRESS_REGEX.matches(addr)

    private fun isValidTime(time: String): Boolean =
        TIME_REGEX.matches(time)

    private fun isValidDate(date: String): Boolean =
        DATE_REGEX.matches(date)

    // ---------- ANDROID LIFECYCLE ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBinding.inflate(inflater, container, false)

        // "Add a stop" -> affiche le petit formulaire
        binding.textAddStop.setOnClickListener {
            binding.addStopForm.visibility = View.VISIBLE
        }

        // Bouton "Confirm stop"
        binding.buttonConfirmStop.setOnClickListener {
            val stopAddress = binding.inputStopAddress.text.toString().trim()

            if (stopAddress.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a stop address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidAddress(stopAddress)) {
                Toast.makeText(
                    requireContext(),
                    "Please enter a valid stop address (street + number + city)",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Ajoute dans la liste + dans l'UI
            stops.add(stopAddress)
            addStopRow(stopAddress)

            // Reset input + cache le formulaire
            binding.inputStopAddress.text?.clear()
            binding.addStopForm.visibility = View.GONE
        }

        // Bouton principal "Add trip"
        binding.buttonAddTrip.setOnClickListener {
            createTrip()
        }

        return binding.root
    }

    // ---------- UI pour les stops ----------

    /**
     * Ajoute une ligne visuelle pour un stop avec bouton delete.
     */
    private fun addStopRow(stopAddress: String) {
        val inflater = LayoutInflater.from(requireContext())
        val stopView = inflater.inflate(R.layout.item_stop, binding.stopsContainer, false)

        val textStop = stopView.findViewById<TextView>(R.id.textStopAddress)
        val buttonDelete = stopView.findViewById<ImageView>(R.id.buttonDeleteStop)

        textStop.text = "• $stopAddress"

        buttonDelete.setOnClickListener {
            val index = binding.stopsContainer.indexOfChild(stopView)
            if (index != -1) {
                if (index < stops.size) {
                    stops.removeAt(index)
                }
                binding.stopsContainer.removeViewAt(index)
                updateStopsTitleVisibility()
            }
        }

        binding.stopsContainer.addView(stopView)
        updateStopsTitleVisibility()
    }

    /**
     * Affiche ou cache "Stops added" selon s'il y a des stops ou pas.
     */
    private fun updateStopsTitleVisibility() {
        binding.stopsTitle.visibility =
            if (stops.isEmpty()) View.GONE else View.VISIBLE
    }

    // ---------- CRÉATION DU TRIP ----------

    /**
     * Valide le formulaire, construit le payload et envoie à Firestore.
     */
    private fun createTrip() {
        val time = binding.inputTime.text.toString().trim()
        val dep = binding.inputDeparture.text.toString().trim()
        val arr = binding.inputArrival.text.toString().trim()
        val priceText = binding.inputPrice.text.toString().trim()
        val currentUserId = UserSession.getCurrentUserId(requireContext())



        // 🔎 Validation heure
        if (!isValidTime(time)) {
            Toast.makeText(
                requireContext(),
                "Please enter a valid time in format HH:mm (e.g. 08:30)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // 🔎 Validation adresses
        if (!isValidAddress(dep)) {
            Toast.makeText(
                requireContext(),
                "Please enter a valid departure address (street + number + city)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (!isValidAddress(arr)) {
            Toast.makeText(
                requireContext(),
                "Please enter a valid arrival address (street + number + city)",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // 🔎 Validation stops (au cas où tu veux empêcher des vieux stops invalides)
        for (s in stops) {
            if (!isValidAddress(s)) {
                Toast.makeText(
                    requireContext(),
                    "One of your stops has an invalid address. Please remove or edit it.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        // 🔎 Validation price
        val price = priceText.toDoubleOrNull()
        if (price == null || price <= 0.0) {
            Toast.makeText(requireContext(), "Please enter a valid price", Toast.LENGTH_SHORT).show()
            return
        }

        // Construire le payload pour l'écran de succès
        val payload = TripPayload(
            departureTime = time,
            departureAddress = dep,
            arrivalAddress = arr,
            stops = stops.toList(),
            price = price
        )

        // Désactive le bouton pour éviter plusieurs clics
        binding.buttonAddTrip.isEnabled = false

        // Document Firestore
        val tripMap = hashMapOf(
            "departureTime" to time,
            "departureAddress" to dep,
            "arrivalAddress" to arr,
            "stops" to stops.toList(),
            "price" to price,
            "driverId" to currentUserId,
            "createdAt" to FieldValue.serverTimestamp()
        )

        firestore.collection("trips")
            .add(tripMap)
            .addOnSuccessListener { _ ->
                Toast.makeText(requireContext(), "Trip saved!", Toast.LENGTH_SHORT).show()

                val successFragment = AddTripSuccessFragment.newInstance(payload)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, successFragment)
                    .addToBackStack(null)
                    .commit()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                Toast.makeText(
                    requireContext(),
                    "Error saving trip: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                binding.buttonAddTrip.isEnabled = true
            }
    }
}
