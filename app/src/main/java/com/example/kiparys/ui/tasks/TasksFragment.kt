package com.example.kiparys.ui.tasks

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.databinding.FragmentTasksBinding
import com.example.kiparys.ui.MainActivity
import com.example.kiparys.ui.SearchBarUpdatable
import com.example.kiparys.ui.adapter.UserTasksAdapter
import com.example.kiparys.ui.main.MainViewModel
import com.example.kiparys.util.ErrorUtil
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import kotlin.getValue


class TasksFragment : Fragment(), SearchBarUpdatable {

    private var _binding: FragmentTasksBinding? = null
    private val fragmentTasksBinding get() = _binding!!
    private val mainActivity get() = activity as? MainActivity
    private lateinit var userTasksAdapter: UserTasksAdapter
    private val mainViewModel: MainViewModel by activityViewModels()
    private val tasksViewModel: TasksViewModel by viewModels {
        TasksViewModelFactory(
            mainViewModel.userId,
            AuthRepository(),
            UserRepository(),
            ProjectRepository()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return fragmentTasksBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                mainActivity?.activityMainBinding?.includeSearchViewContent?.searchView?.setupWithSearchBar(
                    fragmentTasksBinding.searchBar
                )
            }

            override fun onPause(owner: LifecycleOwner) {
                mainActivity?.activityMainBinding?.includeSearchViewContent?.searchView?.setupWithSearchBar(
                    null
                )
            }

            override fun onDestroy(owner: LifecycleOwner) {
                fragmentTasksBinding.rvTasks.adapter = null
                super.onDestroy(owner)
            }
        })

        fragmentTasksBinding.searchBar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_profile) {
                mainActivity?.openProfileBottomSheet()
                true
            } else {
                false
            }
        }

        userTasksAdapter = UserTasksAdapter(
            onTaskClickListener = { task -> },
            onCheckBoxClickListener = { task, isChecked ->
                tasksViewModel.toggleTaskState(task, isChecked)
            }
        ).apply {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    val layoutManager =
                        fragmentTasksBinding.rvTasks.layoutManager as? LinearLayoutManager
                    layoutManager?.let {
                        val isAtTop = positionStart == 0
                        if (isAtTop) {
                            fragmentTasksBinding.rvTasks.scrollToPosition(0)
                        }
                    }
                }
                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                    if (fromPosition == 0) {
                        fragmentTasksBinding.rvTasks.scrollToPosition(0)
                    }
                }
            })
        }
        fragmentTasksBinding.rvTasks.apply {
            adapter = userTasksAdapter
            layoutManager = LinearLayoutManager(context).apply {
                reverseLayout = false
                stackFromEnd = false
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

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
                                        fragmentTasksBinding.searchBar.menu?.findItem(R.id.action_profile)?.icon =
                                            drawable
                                    }
                                    .build()
                                imageLoader.enqueue(request)
                            }
                        }?.onFailure { error ->
                            Log.e(TAG, "Failed to load profile image: ${error.message}")
                        }
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {

                launch {
                    tasksViewModel.userTasksUiState.collect { uiState ->
                        fragmentTasksBinding.lpiTasks.visibility =
                            if (uiState.isLoading) View.VISIBLE else View.GONE
                        userTasksAdapter.submitList(uiState.userTasks)
                        if (!uiState.isLoading) {
                            fragmentTasksBinding.nsvTasksPreview.visibility =
                                if (uiState.userTasks.isNotEmpty()) View.GONE else View.VISIBLE
                            fragmentTasksBinding.rvTasks.visibility =
                                if (uiState.userTasks.isNotEmpty()) View.VISIBLE else View.GONE

                            fragmentTasksBinding.appBarLayout.liftOnScrollTargetViewId =
                                if (uiState.userTasks.isNotEmpty()) fragmentTasksBinding.rvTasks.id
                                else fragmentTasksBinding.nsvTasksPreview.id
                        }
                    }
                }

                launch {
                    tasksViewModel.tasksUiState.collect { uiState ->
                        uiState.error?.let { error ->
                            val snackBar = Snackbar.make(
                                requireView(),
                                ErrorUtil.getErrorMessage(
                                    requireContext(),
                                    error
                                ),
                                Snackbar.LENGTH_LONG
                            )
                            mainActivity?.activityMainBinding?.bottomNavigationView?.let { fab ->
                                snackBar.setAnchorView(fab)
                            }
                            snackBar.show()
                            tasksViewModel.errorMessageShown()
                        }
                    }
                }

            }
        }

    }

    override fun updateSearchBarText(text: String) {
        fragmentTasksBinding.searchBar.setText(text)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "TasksFragment"
    }

}
