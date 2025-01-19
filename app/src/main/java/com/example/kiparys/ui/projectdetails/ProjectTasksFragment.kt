package com.example.kiparys.ui.projectdetails

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
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
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.Constants.DATE_PICKER
import com.example.kiparys.Constants.TIME_PICKER
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.model.ProjectTask
import com.example.kiparys.databinding.BottomSheetCreateTaskBinding
import com.example.kiparys.databinding.BottomSheetOptionsBinding
import com.example.kiparys.databinding.FragmentProjectTasksBinding
import com.example.kiparys.ui.adapter.ProjectTaskOptionsAdapter
import com.example.kiparys.ui.adapter.ProjectTasksAdapter
import com.example.kiparys.ui.main.MainViewModel
import com.example.kiparys.util.ErrorUtil
import com.example.kiparys.util.SystemUtil.isKeyboardVisible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.getValue


class ProjectTasksFragment : Fragment() {

    private var _binding: FragmentProjectTasksBinding? = null
    private val fragmentProjectTasksBinding get() = _binding!!
    private var createTaskDialog: BottomSheetDialog? = null
    private var datePickerDialog: MaterialDatePicker<Long>? = null
    private var projectTaskOptionsDialog: BottomSheetDialog? = null
    private var deleteTaskDialog: AlertDialog? = null
    private lateinit var projectTasksAdapter: ProjectTasksAdapter
    private val projectDetailsViewModel: ProjectDetailsViewModel by viewModels({ requireParentFragment() })
    private val mainViewModel: MainViewModel by activityViewModels()
    private val imm by lazy {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProjectTasksBinding.inflate(inflater, container, false)
        return fragmentProjectTasksBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                fragmentProjectTasksBinding.rvTasks.adapter = null
                super.onDestroy(owner)
            }
        })

        projectTasksAdapter = ProjectTasksAdapter(
            onTaskLongClickListener = { task ->
                showProjectTaskOptionsDialog(task)
            },
            onCheckBoxClickListener = { task, isChecked ->
                projectDetailsViewModel.toggleTaskState(task, isChecked)
            }
        ).apply {
            registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    val layoutManager =
                        fragmentProjectTasksBinding.rvTasks.layoutManager as? LinearLayoutManager
                    layoutManager?.let {
                        val isAtTop = positionStart == 0
                        if (isAtTop) {
                            fragmentProjectTasksBinding.rvTasks.scrollToPosition(0)
                        }
                    }
                }
                override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                    super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                    if (fromPosition == 0) {
                        fragmentProjectTasksBinding.rvTasks.scrollToPosition(0)
                    }
                }
            })
            stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }
        fragmentProjectTasksBinding.rvTasks.apply {
            adapter = projectTasksAdapter
            layoutManager = LinearLayoutManager(context).apply {
                reverseLayout = false
                stackFromEnd = false
            }
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        fragmentProjectTasksBinding.fabAddTask.hide()
                    } else if (dy < 0) {
                        fragmentProjectTasksBinding.fabAddTask.show()
                    }
                }
            })
        }

        fragmentProjectTasksBinding.fabAddTask.setOnClickListener {
            showCreateTaskBottomSheetDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {

                launch {
                    projectDetailsViewModel.projectTasksUiState.collect { uiState ->
                        fragmentProjectTasksBinding.lpiProjectTasks.visibility =
                            if (uiState.isLoading) View.VISIBLE else View.GONE
                        projectTasksAdapter.submitList(uiState.projectTasks)
                        if (!uiState.isLoading) {
                            fragmentProjectTasksBinding.clProjectTasksPreview.visibility =
                                if (uiState.projectTasks.isNotEmpty()) View.GONE else View.VISIBLE
                        }
                    }
                }

                launch {
                    projectDetailsViewModel.projectMainScreenUiState.collect { uiState ->
                        uiState.error?.let { error ->
                            requireView().postDelayed({
                                Snackbar.make(
                                    requireView(),
                                    ErrorUtil.getErrorMessage(requireContext(), error),
                                    Snackbar.LENGTH_LONG
                                )
                                    .show()
                            }, 250)
                            projectDetailsViewModel.errorMessageShown()
                        }

                        if (uiState.saveTaskSuccess) {
                            createTaskDialog?.dismiss()
                            projectDetailsViewModel.saveTaskSuccessMessageShown()
                        }

                        if (uiState.showConfirmDeleteTaskDialog) {
                            deleteTaskDialog = MaterialAlertDialogBuilder(requireContext())
                                .setTitle(getString(R.string.dialog_title_delete_task))
                                .setMessage(getString(R.string.dialog_message_delete_task))
                                .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                                    dialog.dismiss()
                                }
                                .setPositiveButton(getString(R.string.action_delete_for_all)) { dialog, _ ->
                                    projectDetailsViewModel.confirmDeleteTask()
                                    dialog.dismiss()
                                }
                                .setOnDismissListener {
                                    deleteTaskDialog = null
                                }
                                .create()
                            deleteTaskDialog?.show()
                            projectDetailsViewModel.confirmDeleteTaskDialogShown()
                        }

                    }
                }
            }
        }

    }

    private fun showCreateTaskBottomSheetDialog() {
        if (createTaskDialog?.isShowing == true) {
            return
        }

        createTaskDialog = BottomSheetDialog(requireContext())
        createTaskDialog?.behavior?.isFitToContents = true
        createTaskDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val bottomSheetCreateTaskBinding = BottomSheetCreateTaskBinding.inflate(layoutInflater)

        bottomSheetCreateTaskBinding.etTaskName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                bottomSheetCreateTaskBinding.mbSaveTask.isEnabled = !s.isNullOrEmpty()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        bottomSheetCreateTaskBinding.mbAddAssignedUser.setOnClickListener {
            bottomSheetCreateTaskBinding.root.clearFocus()
            if (isKeyboardVisible(requireActivity())) {
                imm.hideSoftInputFromWindow(bottomSheetCreateTaskBinding.root.windowToken, 0)
                bottomSheetCreateTaskBinding.root.postDelayed({
                    MemberSearchDialogFragment.show(
                        childFragmentManager
                    )
                }, 300)
            } else {
                MemberSearchDialogFragment.show(childFragmentManager)
            }
        }

        bottomSheetCreateTaskBinding.mbAddDue.setOnClickListener {
            bottomSheetCreateTaskBinding.root.clearFocus()
            if (isKeyboardVisible(requireActivity())) {
                imm.hideSoftInputFromWindow(bottomSheetCreateTaskBinding.root.windowToken, 0)
                bottomSheetCreateTaskBinding.root.postDelayed({ showDateTimePickerDialog() }, 300)
            } else {
                showDateTimePickerDialog()
            }
        }

        bottomSheetCreateTaskBinding.mbSaveTask.setOnClickListener {
            val taskNameInput = bottomSheetCreateTaskBinding.etTaskName.text.toString().trim()
                .replace("\\s+".toRegex(), " ")
            val taskDescriptionInput =
                bottomSheetCreateTaskBinding.etTaskDescription.text.toString().trim()
                    .replace("\\s+".toRegex(), " ")
            val messageToMember =
                if (projectDetailsViewModel.projectMainScreenUiState.value.taskDueTimestamp != null)
                    getString(
                        R.string.notification_message_to_assigned_member_with_due,
                        projectDetailsViewModel.getCurrentUserDisplayName(),
                        taskNameInput,
                        SimpleDateFormat("EEE, d MMM, HH:mm", Locale.getDefault())
                            .format(projectDetailsViewModel.projectMainScreenUiState.value.taskDueTimestamp)
                    )
                else
                    getString(
                        R.string.notification_message_to_assigned_member,
                        projectDetailsViewModel.getCurrentUserDisplayName(),
                        taskNameInput
                    )
            projectDetailsViewModel.saveTask(
                taskName = taskNameInput,
                taskDescription = if (taskDescriptionInput.isEmpty()) null else taskDescriptionInput,
                messageToMember = messageToMember
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                projectDetailsViewModel.projectMainScreenUiState.collect { uiState ->
                    if (bottomSheetCreateTaskBinding.etTaskName.text?.isNotEmpty() == true) {
                        bottomSheetCreateTaskBinding.mbSaveTask.isEnabled = !uiState.isSaveTask
                    }
                    bottomSheetCreateTaskBinding.cpiTaskSave.visibility =
                        if (uiState.isSaveTask) View.VISIBLE else View.GONE
                    bottomSheetCreateTaskBinding.mbSaveTask.text =
                        if (uiState.isSaveTask) getString(R.string.prompt_empty_string) else getString(
                            R.string.action_save
                        )

                    val chipGroup = bottomSheetCreateTaskBinding.cgDueAssigned
                    chipGroup.removeAllViews()

                    bottomSheetCreateTaskBinding.hsvChipGroup.visibility =
                        if (uiState.taskDueTimestamp != null || uiState.taskAssignedUser != null)
                            View.VISIBLE else View.GONE

                    if (uiState.taskDueTimestamp != null || uiState.taskAssignedUser != null) {
                        val chipDue =
                            layoutInflater.inflate(R.layout.chip_due, chipGroup, false) as Chip
                        if (uiState.taskDueTimestamp != null) {
                            chipDue.text =
                                SimpleDateFormat("EEE, d MMM, HH:mm", Locale.getDefault())
                                    .format(uiState.taskDueTimestamp)
                            chipDue.setOnClickListener {
                                showDateTimePickerDialog()
                            }
                            chipDue.setOnCloseIconClickListener {
                                projectDetailsViewModel.setTaskDueTimestamp(null)
                            }
                            chipGroup.addView(chipDue)
                        }

                        val chipUser =
                            layoutInflater.inflate(R.layout.chip_user, chipGroup, false) as Chip
                        if (uiState.taskAssignedUser != null) {
                            chipUser.text = uiState.taskAssignedUser.email
                            chipUser.setOnCloseIconClickListener {
                                projectDetailsViewModel.setAssignedUser(
                                    null
                                )
                            }
                            chipUser.setOnClickListener {
                                MemberSearchDialogFragment.show(
                                    childFragmentManager
                                )
                            }
                            val imageLoader =
                                (requireContext().applicationContext as KiparysApplication).imageLoader
                            val request = ImageRequest.Builder(requireContext())
                                .data(uiState.taskAssignedUser.profileImageUrl)
                                .precision(Precision.EXACT)
                                .transformations(CircleCropTransformation())
                                .placeholder(R.drawable.baseline_circle_24)
                                .error(R.drawable.baseline_circle_24)
                                .target { drawable ->
                                    chipUser.chipIcon = drawable
                                }
                                .build()
                            imageLoader.enqueue(request)
                            chipGroup.addView(chipUser)
                        }

                    }

                }
            }
        }


        createTaskDialog?.setOnDismissListener {
            bottomSheetCreateTaskBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetCreateTaskBinding.root)
            }
            projectDetailsViewModel.setTaskDueTimestamp(null)
            projectDetailsViewModel.setAssignedUser(null)
            createTaskDialog = null
        }

        createTaskDialog?.setContentView(bottomSheetCreateTaskBinding.root)
        createTaskDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (isKeyboardVisible(requireActivity())) {
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            requireView().postDelayed({
                createTaskDialog?.show()
            }, 60)
        } else {
            createTaskDialog?.show()
        }
    }

    private fun showDateTimePickerDialog() {
        if (datePickerDialog != null && datePickerDialog?.isAdded == true) {
            return
        }

        val calendar = Calendar.getInstance()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.date_picker_title_task_due))
            .setSelection(calendar.timeInMillis)

        datePickerDialog = datePicker.build()
        datePickerDialog?.addOnPositiveButtonClickListener { dateSelection ->
            calendar.timeInMillis = dateSelection

            val timePickerDialog = MaterialTimePicker.Builder()
                .setTitleText(getString(R.string.time_picker_title_task_due))
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(calendar.get(Calendar.HOUR_OF_DAY))
                .setMinute(calendar.get(Calendar.MINUTE))
                .build()

            timePickerDialog.addOnPositiveButtonClickListener {
                calendar.set(Calendar.HOUR_OF_DAY, timePickerDialog.hour)
                calendar.set(Calendar.MINUTE, timePickerDialog.minute)
                projectDetailsViewModel.setTaskDueTimestamp(calendar.timeInMillis)
            }

            timePickerDialog.show(parentFragmentManager, TIME_PICKER)

        }

        datePickerDialog?.addOnDismissListener {
            datePickerDialog = null
        }

        datePickerDialog?.show(parentFragmentManager, DATE_PICKER)

    }


    private fun showProjectTaskOptionsDialog(task: ProjectTask) {
        if (projectTaskOptionsDialog?.isShowing == true) {
            return
        }

        projectTaskOptionsDialog = BottomSheetDialog(requireContext())
        projectTaskOptionsDialog?.behavior?.isFitToContents = true
        projectTaskOptionsDialog?.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }

        val bottomSheetSelectTwoFactorBinding = BottomSheetOptionsBinding.inflate(layoutInflater)

        val adapter = mainViewModel.userId.value?.let {
            ProjectTaskOptionsAdapter(requireContext(), it, task) { selectedOption ->
                projectDetailsViewModel.handleProjectTaskOptionSelection(selectedOption, task)
                projectTaskOptionsDialog?.dismiss()
            }
        }
        bottomSheetSelectTwoFactorBinding.rvOptions.adapter = adapter
        bottomSheetSelectTwoFactorBinding.rvOptions.layoutManager = LinearLayoutManager(context)

        projectTaskOptionsDialog?.setOnDismissListener {
            bottomSheetSelectTwoFactorBinding.root.parent?.let {
                (it as ViewGroup).removeView(bottomSheetSelectTwoFactorBinding.root)
            }
            projectTaskOptionsDialog = null
        }

        projectTaskOptionsDialog?.setContentView(bottomSheetSelectTwoFactorBinding.root)
        projectTaskOptionsDialog?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        if (isKeyboardVisible(requireActivity())) {
            imm.hideSoftInputFromWindow(requireView().windowToken, 0)
            requireView().post { projectTaskOptionsDialog?.show() }
        } else {
            projectTaskOptionsDialog?.show()
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
