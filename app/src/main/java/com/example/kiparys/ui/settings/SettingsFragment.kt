package com.example.kiparys.ui.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import com.example.kiparys.R
import com.example.kiparys.databinding.BottomSheetThemeSelectionBinding
import com.example.kiparys.databinding.FragmentSettingsBinding
import com.example.kiparys.util.SystemUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog


class SettingsFragment : Fragment(), NotificationDisableDialogFragment.NotificationDialogListener {

    private var _binding: FragmentSettingsBinding? = null
    private val fragmentSettingsBinding get() = _binding!!
    private var themeSelectionBottomSheetDialog: BottomSheetDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        updateThemeInfo()
        fragmentSettingsBinding.clAppThemeSetting.setOnClickListener {
            showThemeSelectionBottomSheet()
        }

        fragmentSettingsBinding.clPrivacyPolicySetting.setOnClickListener {
            findNavController().navigate(R.id.action_settings_fragment_to_privacy_policy_fragment)
        }

        fragmentSettingsBinding.clAboutAppSetting.setOnClickListener {
            findNavController().navigate(R.id.action_settings_fragment_to_about_app_fragment)
        }

        fragmentSettingsBinding.clNotificationSetting.setOnClickListener {
            SystemUtil.triggerSingleVibration(requireContext())

            val currentChecked = fragmentSettingsBinding.msNotification.isChecked
            fragmentSettingsBinding.msNotification.isChecked = !currentChecked

            if (!currentChecked) {
                requestNotificationPermission()
            } else {
                NotificationDisableDialogFragment().show(childFragmentManager, "notificationDialog")
            }
        }

        return fragmentSettingsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentSettingsBinding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                checkNotificationPermission()
            }
        })
    }

    private fun checkNotificationPermission() {
        val notificationManager = NotificationManagerCompat.from(requireContext())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = notificationManager.areNotificationsEnabled()
            fragmentSettingsBinding.msNotification.isChecked = permissionGranted
        } else {
            fragmentSettingsBinding.msNotification.isChecked =
                notificationManager.areNotificationsEnabled()
        }
    }

    private fun revokeNotificationPermissions() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        startActivity(intent)
    }

    private fun requestNotificationPermission() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
        startActivity(intent)
    }

    private fun updateThemeInfo() {
        val currentTheme = getCurrentTheme()
        val themeInfoText = when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> getString(R.string.info_app_theme_system)
            AppCompatDelegate.MODE_NIGHT_NO -> getString(R.string.info_app_theme_light)
            AppCompatDelegate.MODE_NIGHT_YES -> getString(R.string.info_app_theme_dark)
            else -> getString(R.string.info_app_theme_system)
        }
        fragmentSettingsBinding.mtvAppThemeInfo.text = themeInfoText
    }

    private fun showThemeSelectionBottomSheet() {
        if (themeSelectionBottomSheetDialog?.isShowing == true) {
            return
        }

        val bottomSheetBinding = BottomSheetThemeSelectionBinding.inflate(
            LayoutInflater.from(requireContext())
        )

        val currentTheme = getCurrentTheme()
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> bottomSheetBinding.systemThemeRadioButton.isChecked =
                true

            AppCompatDelegate.MODE_NIGHT_NO -> bottomSheetBinding.lightThemeRadioButton.isChecked =
                true

            AppCompatDelegate.MODE_NIGHT_YES -> bottomSheetBinding.darkThemeRadioButton.isChecked =
                true
        }

        bottomSheetBinding.appThemeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.systemThemeRadioButton -> setAppTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                R.id.lightThemeRadioButton -> setAppTheme(AppCompatDelegate.MODE_NIGHT_NO)
                R.id.darkThemeRadioButton -> setAppTheme(AppCompatDelegate.MODE_NIGHT_YES)
            }
        }

        themeSelectionBottomSheetDialog = BottomSheetDialog(requireContext())
        themeSelectionBottomSheetDialog?.setOnDismissListener {
            bottomSheetBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetBinding.root)
            }
            themeSelectionBottomSheetDialog = null
        }

        themeSelectionBottomSheetDialog?.setContentView(bottomSheetBinding.root)
        themeSelectionBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        themeSelectionBottomSheetDialog?.show()
    }

    private fun getCurrentTheme(): Int {
        return AppCompatDelegate.getDefaultNightMode()
    }

    private fun setAppTheme(theme: Int) {
        AppCompatDelegate.setDefaultNightMode(theme)
        updateThemeInfo()
    }

    override fun onConfirm() {
        fragmentSettingsBinding.msNotification.isChecked = false
        revokeNotificationPermissions()
    }

    override fun onCancel() {
        fragmentSettingsBinding.msNotification.isChecked = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
