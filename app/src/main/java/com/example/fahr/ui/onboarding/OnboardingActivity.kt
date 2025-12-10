package com.example.fahr.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.fahr.R
import com.example.fahr.databinding.ActivityOnboardingBinding
import com.example.fahr.model.OnboardingPage
import com.example.fahr.ui.main.MainActivity

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var adapter: OnboardingPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup onboarding pages
        val pages = listOf(
            OnboardingPage(
                title = "Welcome !!",
                description = "We travel together, we gain in time and money",
                imageResId = R.drawable.onboarding_1
            ),
            OnboardingPage(
                title = "Safe. Flexible.",
                description = "You are a student and you have a car? Add your trips and travel with others.",
                imageResId = R.drawable.onboarding_2
            ),
            OnboardingPage(
                title = "Intuitive. Simple.",
                description = "You don't have a car? Search people travelling on the same route as you.",
                imageResId = R.drawable.onboarding_3
            )
        )

        adapter = OnboardingPagerAdapter(pages)
        binding.viewPager.adapter = adapter

        // Setup indicators
        setupIndicators(pages.size)
        setCurrentIndicator(0)

        // Change indicator + button text on page swipe
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setCurrentIndicator(position)
                binding.buttonNext.text = if (position == pages.size - 1) {
                    "Get started"
                } else {
                    "Next"
                }
            }
        })

        // Handle Next / Get started button
        binding.buttonNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current == pages.size - 1) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                binding.viewPager.currentItem = current + 1
            }
        }
    }

    /**
     * Creates small circular indicators (dots)
     */
    private fun setupIndicators(count: Int) {
        val indicators = Array(count) {
            val view = View(this)

            // FIX: enforce size so indicators cannot fill screen
            val params = LinearLayout.LayoutParams(20, 20)
            params.setMargins(8, 0, 8, 0)
            view.layoutParams = params

            view.setBackgroundResource(R.drawable.indicator_inactive)
            binding.indicatorsContainer.addView(view)
            view
        }
    }

    /**
     * Updates active indicator based on current ViewPager page
     */
    private fun setCurrentIndicator(position: Int) {
        val childCount = binding.indicatorsContainer.childCount
        for (i in 0 until childCount) {
            val indicator = binding.indicatorsContainer.getChildAt(i)
            if (i == position) {
                indicator.setBackgroundResource(R.drawable.indicator_active)
            } else {
                indicator.setBackgroundResource(R.drawable.indicator_inactive)
            }
        }
    }
}
