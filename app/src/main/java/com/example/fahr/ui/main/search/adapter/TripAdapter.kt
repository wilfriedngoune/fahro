package com.example.fahr.ui.main.search.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fahr.databinding.ItemTripBinding
import com.example.fahr.ui.main.search.model.Trip

class TripAdapter(
    private val trips: List<Trip>,
    private val onClick: (Trip) -> Unit
) : RecyclerView.Adapter<TripAdapter.TripViewHolder>() {

    inner class TripViewHolder(val binding: ItemTripBinding)
        : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun getItemCount(): Int = trips.size

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        val t = trips[position]

        holder.binding.driverAvatar.setImageResource(t.driverAvatarResId)
        holder.binding.driverName.text = t.driverName
        holder.binding.time.text = t.departureTimeRange
        holder.binding.address.text = t.routeSummary
        holder.binding.ratingText.text = String.format("%.1f", t.rating)

        holder.itemView.setOnClickListener { onClick(t) }
    }
}
