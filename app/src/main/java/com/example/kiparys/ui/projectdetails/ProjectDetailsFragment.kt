package com.example.kiparys.ui.projectdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.Constants.PROJECT_IDEAS_CHANNEL_ID
import com.example.kiparys.Constants.PROJECT_MESSAGES_CHANNEL_ID
import com.example.kiparys.Constants.PROJECT_TASKS_CHANNEL_ID
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.repository.AuthRepository
import com.example.kiparys.data.repository.ProjectRepository
import com.example.kiparys.data.repository.UserRepository
import com.example.kiparys.databinding.FragmentProjectDetailsBinding
import com.example.kiparys.ui.main.MainViewModel
import com.example.kiparys.util.StringUtil.formatUserName
import com.example.kiparys.util.StringUtil.getMemberCountString
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.getValue


class ProjectDetailsFragment : Fragment() {

    private var _binding: FragmentProjectDetailsBinding? = null
    private var dotJob: Job? = null
    private val fragmentProjectDetailsBinding get() = _binding!!
    private val mainViewModel: MainViewModel by activityViewModels()
    private val projectDetailsViewModelFactory by lazy {
        ProjectDetailsViewModelFactory(
            mainViewModel.userId,
            mainViewModel.projectId,
            AuthRepository(),
            UserRepository(),
            ProjectRepository(),
            (requireActivity().application as KiparysApplication).dataManagementRepository,
            (requireActivity().application as KiparysApplication).messagingRepository
        )
    }
    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModels { projectDetailsViewModelFactory }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectDetailsBinding.inflate(inflater, container, false)
        return fragmentProjectDetailsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val projectId = arguments?.getString("projectId")
        if (!projectId.isNullOrEmpty()) {
            mainViewModel.setProjectId(projectId)
            arguments?.remove("projectId")
        }

        fragmentProjectDetailsBinding.topAppBar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        fragmentProjectDetailsBinding.clProjectProfile.setOnClickListener {
            // TODO: OpenProjectProfile
        }

        val pagerAdapter =
            ProjectDetailsPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        fragmentProjectDetailsBinding.viewPager.adapter = pagerAdapter

        fragmentProjectDetailsBinding.viewPager.isUserInputEnabled = false
        fragmentProjectDetailsBinding.viewPager.offscreenPageLimit = 2

        val channelTabMapping = mapOf(
            PROJECT_MESSAGES_CHANNEL_ID to 0,
            PROJECT_TASKS_CHANNEL_ID to 1,
            PROJECT_IDEAS_CHANNEL_ID to 2
        )

        val channelId = arguments?.getString("channelId")
        val selectedTabPosition = channelId?.let { channelTabMapping[it] } ?: 0
        fragmentProjectDetailsBinding.viewPager.setCurrentItem(selectedTabPosition, false)

        if (!channelId.isNullOrEmpty()) {
            arguments?.remove("channelId")
        }

        TabLayoutMediator(
            fragmentProjectDetailsBinding.tabLayout,
            fragmentProjectDetailsBinding.viewPager
        ) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_chat)
                1 -> getString(R.string.tab_tasks)
                2 -> getString(R.string.tab_ideas)
                else -> null
            }

            tab.contentDescription = tab.text
        }.attach()

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                stopAddingDots()
                fragmentProjectDetailsBinding.viewPager.adapter = null
                super.onDestroy(owner)
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    projectDetailsViewModel.projectDetailsUiState.collect { uiState ->
                        val imageLoader =
                            (requireContext().applicationContext as KiparysApplication).imageLoader
                        val request = ImageRequest.Builder(requireContext())
                            .placeholder(R.drawable.baseline_circle_24)
                            .error(R.drawable.baseline_circle_24)
                            .data(uiState.project?.projectImageUrl)
                            .precision(Precision.EXACT)
                            .transformations(CircleCropTransformation())
                            .target(fragmentProjectDetailsBinding.sivProject)
                            .build()
                        imageLoader.enqueue(request)

                        uiState.project?.let { project ->
                            fragmentProjectDetailsBinding.mtvProjectName.text = project.name

                            project.members?.size?.let {
                                fragmentProjectDetailsBinding.mtvMemberCount.text =
                                    getMemberCountString(it)
                            }

                            val typingUsers =
                                project.typing?.filterKeys { it != mainViewModel.userId.value }
                                    ?: emptyMap()

                            if (typingUsers.isNotEmpty()) {
                                val formattedNames =
                                    typingUsers.values.mapIndexed { index, fullName ->
                                        when (typingUsers.size) {
                                            1 -> formatUserName(
                                                fullName,
                                                maxNameLength = 16,
                                                maxFullNameLength = 16
                                            )

                                            else -> formatUserName(
                                                fullName,
                                                maxNameLength = 10,
                                                maxFullNameLength = 10
                                            )
                                        }
                                    }

                                val typingText = when (typingUsers.size) {
                                    1 -> "${formattedNames[0]} печатает".replace(
                                        "\\s+".toRegex(),
                                        " "
                                    )

                                    else -> "${formattedNames.random()} и ещё ${
                                        typingUsers.size.minus(
                                            1
                                        )
                                    } печатает".replace("\\s+".toRegex(), " ")
                                }

                                fragmentProjectDetailsBinding.mtvMemberInChatNowCount.text =
                                    typingText
                                addDotEverySecond(
                                    fragmentProjectDetailsBinding.mtvMemberInChatNowCount,
                                    typingText
                                )
                            } else {
                                stopAddingDots()
                                fragmentProjectDetailsBinding.mtvMemberInChatNowCount.text =
                                    if (project.nowInChat != null)
                                        getString(
                                            R.string.label_now_in_chat_members_count,
                                            project.nowInChat.size
                                        )
                                    else ""
                            }

                        }

                    }
                }

            }
        }
    }

    fun addDotEverySecond(textView: TextView, baseText: String) {
        stopAddingDots()
        dotJob = CoroutineScope(Dispatchers.Main).launch {
            var dotCount = 0
            while (isActive) {
                delay(400)
                dotCount = (dotCount + 1) % 4
                val dots = ".".repeat(dotCount)
                val formattedText =
                    textView.context.getString(R.string.label_typing, baseText, dots)
                textView.text = formattedText
            }
        }
    }

    fun stopAddingDots() {
        dotJob?.cancel()
        dotJob = null
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

}
