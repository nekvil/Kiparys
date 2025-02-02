package com.example.kiparys.ui.projectdetails

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter


class ProjectDetailsPagerAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProjectChatFragment()
            1 -> ProjectTasksFragment()
            2 -> ProjectIdeasFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }

}
