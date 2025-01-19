package com.example.kiparys.ui.aboutapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.kiparys.BuildConfig
import com.example.kiparys.R
import com.example.kiparys.databinding.FragmentAboutAppBinding
import java.util.Calendar


class AboutAppFragment : Fragment() {

    private var _binding: FragmentAboutAppBinding? = null
    private val fragmentAboutAppBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAboutAppBinding.inflate(inflater, container, false)
        return fragmentAboutAppBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentAboutAppBinding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE.toString()

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        fragmentAboutAppBinding.mtvAppVersion.text =
            getString(R.string.label_app_version, versionName)
        fragmentAboutAppBinding.mtvBuildVersion.text =
            getString(R.string.label_build_version, versionCode)
        fragmentAboutAppBinding.mtvDeveloperInfo.text =
            getString(R.string.label_developer_info, currentYear.toString())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
