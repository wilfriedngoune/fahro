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
import com.example.fahr.databinding.FragmentAddBinding
import com.example.fahr.ui.main.add.model.TripPayload

class AddFragment : Fragment() {

    private lateinit var binding: FragmentAddBinding
    private val stops = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddBinding.inflate(inflater, container, false)

        // Show stop form when user clicks "Add a stop"
        binding.textAddStop.setOnClickListener {
            binding.addStopForm.visibility = View.VISIBLE
        }

        // Confirm a stop
        binding.buttonConfirmStop.setOnClickListener {
            val stopAddress = binding.inputStopAddress.text.toString().trim()
            if (stopAddress.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a stop address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add to list
            stops.add(stopAddress)
            addStopRow(stopAddress)

            // Clear input & hide form
            binding.inputStopAddress.text?.clear()
            binding.addStopForm.visibility = View.GONE
        }

        // Add trip button
        binding.buttonAddTrip.setOnClickListener {
            createTrip()
        }

        return binding.root
    }

    /**
     * Add one row in the UI for a stop, with delete button.
     */
    private fun addStopRow(stopAddress: String) {
        val inflater = LayoutInflater.from(requireContext())
        val stopView = inflater.inflate(R.layout.item_stop, binding.stopsContainer, false)

        val textStop = stopView.findViewById<TextView>(R.id.textStopAddress)
        val buttonDelete = stopView.findViewById<ImageView>(R.id.buttonDeleteStop)

        textStop.text = "• $stopAddress"

        buttonDelete.setOnClickListener {
            // Find index of this view in the container
            val index = binding.stopsContainer.indexOfChild(stopView)
            if (index != -1) {
                // Remove from list and from UI
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
     * Show or hide "Stops added" title depending on if stops exist.
     */
    private fun updateStopsTitleVisibility() {
        binding.stopsTitle.visibility =
            if (stops.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun createTrip() {
        val date = binding.inputDate.text.toString().trim()
        val time = binding.inputTime.text.toString().trim()
        val dep = binding.inputDeparture.text.toString().trim()
        val arr = binding.inputArrival.text.toString().trim()

        if (date.isEmpty() || time.isEmpty() || dep.isEmpty() || arr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Build payload ready for backend
        val payload = TripPayload(
            departureDate = date,
            departureTime = time,
            departureAddress = dep,
            arrivalAddress = arr,
            stops = stops.toList()
        )

        // TODO: send payload to backend when ready

        val successFragment = AddTripSuccessFragment.newInstance(payload)

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, successFragment)
            .addToBackStack(null)
            .commit()
    }
}
