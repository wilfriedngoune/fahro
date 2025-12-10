package com.example.fahr.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.fahr.R
import com.example.fahr.databinding.ActivityMainBinding
import com.example.fahr.ui.main.add.AddFragment
import com.example.fahr.ui.main.chat.ChatFragment
import com.example.fahr.ui.main.profile.ProfileFragment
import com.example.fahr.ui.main.search.SearchFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Default fragment
        openFragment(SearchFragment())

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_search -> openFragment(SearchFragment())
                R.id.nav_add -> openFragment(AddFragment())
                R.id.nav_chat -> openFragment(ChatFragment())
                R.id.nav_profile -> openFragment(ProfileFragment())
            }
            true
        }
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
