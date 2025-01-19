package com.example.kiparys.ui.authregister

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.kiparys.R
import com.example.kiparys.databinding.FragmentAuthRegisterBinding
import com.google.android.material.tabs.TabLayoutMediator

class AuthRegisterFragment : Fragment() {

    private var _binding: FragmentAuthRegisterBinding? = null
    private val fragmentAuthRegisterBinding get() = _binding!!

    private val args: AuthRegisterFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthRegisterBinding.inflate(inflater, container, false)
        return fragmentAuthRegisterBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appLink = args.appLink

        val pagerAdapter =
            AuthPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle, appLink)
        fragmentAuthRegisterBinding.viewPager.adapter = pagerAdapter

        fragmentAuthRegisterBinding.viewPager.isUserInputEnabled = false
        fragmentAuthRegisterBinding.viewPager.offscreenPageLimit = 1

        TabLayoutMediator(
            fragmentAuthRegisterBinding.tabLayout,
            fragmentAuthRegisterBinding.viewPager
        ) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.sign_in_tab)
                1 -> getString(R.string.sign_up_tab)
                else -> null
            }

            tab.contentDescription = tab.text
        }.attach()
    }

    override fun onDestroyView() {
        fragmentAuthRegisterBinding.viewPager.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
