package com.example.kiparys.ui.meetings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.databinding.BottomSheetCreateMeetingBinding
import com.example.kiparys.databinding.FragmentMeetingsBinding
import com.example.kiparys.databinding.NavigationHeaderBinding
import com.example.kiparys.ui.MainActivity
import com.example.kiparys.ui.SearchBarUpdatable
import com.example.kiparys.ui.main.MainViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import kotlin.getValue


class MeetingsFragment : Fragment(), SearchBarUpdatable {

    private var _binding: FragmentMeetingsBinding? = null
    private val mainViewModel: MainViewModel by activityViewModels()
    private val fragmentMeetingsBinding get() = _binding!!
    private val mainActivity get() = activity as? MainActivity
    private var newMeetingBottomSheetDialog: BottomSheetDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMeetingsBinding.inflate(inflater, container, false)

        return fragmentMeetingsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchBar = fragmentMeetingsBinding.searchBar
        val searchView = mainActivity?.activityMainBinding?.includeSearchViewContent?.searchView

        val menu = searchBar.menu
        val profileMenuItem = menu?.findItem(R.id.action_profile)

        val appBarLayout = fragmentMeetingsBinding.appBarLayout
        val recyclerView = fragmentMeetingsBinding.rvMeetings
        val nestedScrollView = fragmentMeetingsBinding.nsvMeetingsPreview

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (true) {
                    recyclerView.visibility = View.GONE
                    nestedScrollView.visibility = View.VISIBLE
                    appBarLayout.liftOnScrollTargetViewId = nestedScrollView.id

                } else {
                    recyclerView.visibility = View.VISIBLE
                    nestedScrollView.visibility = View.GONE
                    appBarLayout.liftOnScrollTargetViewId = recyclerView.id
//            (recyclerView.adapter as MeetingsAdapter).submitList(meetings)
                }
                launch {
                    mainViewModel.userData.collect { result ->
                        result?.onSuccess { user ->
                            val profileImageUrl = user.profileImageUrl
                            if (profileImageUrl != null) {
                                val imageLoader =
                                    (requireContext().applicationContext as KiparysApplication).imageLoader
                                val request = ImageRequest.Builder(requireContext())
                                    .placeholder(R.drawable.baseline_circle_24)
                                    .error(R.drawable.baseline_circle_24)
                                    .data(profileImageUrl)
                                    .precision(Precision.EXACT)
                                    .transformations(CircleCropTransformation())
                                    .target { drawable ->
                                        profileMenuItem?.icon = drawable
                                    }
                                    .build()
                                imageLoader.enqueue(request)
                            }

                            mainActivity?.activityMainBinding?.extendedFab?.setOnClickListener {
                                showNewMeetingDialog()
                            }

                            val headerView =
                                mainActivity?.activityMainBinding?.navigationRailView?.headerView
                            headerView?.let {
                                val navigationHeaderBinding = NavigationHeaderBinding.bind(it)
                                navigationHeaderBinding.fabHeaderLayout.setOnClickListener {
                                    showNewMeetingDialog()
                                }
                            }

                        }?.onFailure { error ->
                            Log.e(TAG, "Failed to load profile image: ${error.message}")
                        }
                    }
                }

            }
        }

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                searchView?.setupWithSearchBar(searchBar)
            }

            override fun onPause(owner: LifecycleOwner) {
                searchView?.setupWithSearchBar(null)
            }
        })
        searchBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_profile) {
                mainActivity?.openProfileBottomSheet()
                true
            } else {
                false
            }
        }
    }

    private fun showNewMeetingDialog() {
        if (newMeetingBottomSheetDialog?.isShowing == true) {
            return
        }

        newMeetingBottomSheetDialog = BottomSheetDialog(requireContext())
        newMeetingBottomSheetDialog?.behavior?.isFitToContents = true
        newMeetingBottomSheetDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        val bottomSheetCreateMeetingBinding =
            BottomSheetCreateMeetingBinding.inflate(layoutInflater)

        bottomSheetCreateMeetingBinding.clMeetingStartOption.setOnClickListener {
            // TODO: MeetingStart
        }
        bottomSheetCreateMeetingBinding.clMeetingGetLinkOption.setOnClickListener {
            // TODO: MeetingGetLink
        }
        bottomSheetCreateMeetingBinding.clMeetingEnterCodeOption.setOnClickListener {
            // TODO: MeetingEnterCode
        }

        newMeetingBottomSheetDialog?.setOnDismissListener {
            bottomSheetCreateMeetingBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetCreateMeetingBinding.root)
            }
            newMeetingBottomSheetDialog = null
        }

        newMeetingBottomSheetDialog?.setContentView(bottomSheetCreateMeetingBinding.root)
        newMeetingBottomSheetDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        newMeetingBottomSheetDialog?.show()
    }

    override fun updateSearchBarText(text: String) {
        fragmentMeetingsBinding.searchBar.setText(text)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "MeetingsFragment"
    }

}
