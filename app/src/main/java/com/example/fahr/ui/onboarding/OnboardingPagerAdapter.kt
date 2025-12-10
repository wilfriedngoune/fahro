package com.example.fahr.ui.onboarding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.fahr.databinding.ItemOnboardingPageBinding
import com.example.fahr.model.OnboardingPage


// Adapter for ViewPager2 onboarding screens
class OnboardingPagerAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingPagerAdapter.OnboardingViewHolder>() {

    inner class OnboardingViewHolder(val binding: ItemOnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = ItemOnboardingPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        val page = pages[position]
        holder.binding.title.text = page.title
        holder.binding.description.text = page.description
        holder.binding.image.setImageResource(page.imageResId)
    }

    override fun getItemCount(): Int = pages.size
}
