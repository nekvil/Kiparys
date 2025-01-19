package com.example.kiparys.ui.authregister

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class AuthPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle,
    private val appLink: String?
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> AuthFragment().apply {
                arguments = Bundle().apply {
                    putString("appLink", appLink)
                }
            }

            1 -> RegisterFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}
