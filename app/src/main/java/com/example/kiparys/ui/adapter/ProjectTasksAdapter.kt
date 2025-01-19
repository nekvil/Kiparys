package com.example.kiparys.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kiparys.data.model.ProjectTask
import com.example.kiparys.databinding.ItemTaskBinding
import com.example.kiparys.util.StringUtil.formatTaskTimestamp
import com.example.kiparys.util.StringUtil.isSameDay
import com.example.kiparys.util.StringUtil.isTomorrow
import com.example.kiparys.util.SystemUtil.triggerSingleVibration
import com.google.android.material.color.MaterialColors


class ProjectTasksAdapter(
    private val onTaskLongClickListener: (ProjectTask) -> Unit,
    private val onCheckBoxClickListener: (ProjectTask, Boolean) -> Unit
) : ListAdapter<ProjectTask, ProjectTasksAdapter.TaskViewHolder>(differCallback) {

    companion object {
        private val differCallback = object : DiffUtil.ItemCallback<ProjectTask>() {
            override fun areItemsTheSame(oldItem: ProjectTask, newItem: ProjectTask): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ProjectTask, newItem: ProjectTask): Boolean {
                return oldItem == newItem
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemTaskBinding.inflate(inflater, parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: ProjectTask) = with(binding) {
            mtvTaskTitle.text = task.name

            if (task.completed == true) {
                mtvTaskTitle.paintFlags = mtvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                mcvTaskComplete.visibility = View.VISIBLE
                mcvTaskInProgress.visibility = View.GONE

            } else {
                mtvTaskTitle.paintFlags =
                    mtvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                mcvTaskComplete.visibility = View.GONE
                mcvTaskInProgress.visibility = View.VISIBLE
            }

            if (!task.description.isNullOrBlank() && task.completed != true) {
                mtvTaskDescription.text = task.description
                mtvTaskDescription.visibility = View.VISIBLE
            } else {
                mtvTaskDescription.visibility = View.GONE
            }

            val currentTime = System.currentTimeMillis()
            val dueDate = task.dueDate?.let { timestamp ->
                when {
                    timestamp > currentTime -> {
                        when {
                            isSameDay(currentTime, timestamp) || isTomorrow(
                                currentTime,
                                timestamp
                            ) -> {
                                mtvDueTime.setTextColor(
                                    MaterialColors.getColor(
                                        root,
                                        com.google.android.material.R.attr.colorPrimary
                                    )
                                )
                            }

                            else -> {
                                mtvDueTime.setTextColor(
                                    MaterialColors.getColor(
                                        root,
                                        com.google.android.material.R.attr.colorOnSurface
                                    )
                                )
                            }
                        }
                    }

                    else -> {
                        mtvDueTime.setTextColor(
                            MaterialColors.getColor(
                                root,
                                com.google.android.material.R.attr.colorError
                            )
                        )
                    }
                }
                formatTaskTimestamp(timestamp, root.context)
            }

            if (!dueDate.isNullOrBlank() && task.completed != true) {
                mtvDueTime.text = dueDate
                mtvDueTime.visibility = View.VISIBLE
            } else {
                mtvDueTime.visibility = View.GONE
            }

            if (!task.assignedUser?.assignedName.isNullOrBlank() && task.completed != true) {
                mtvUserOrProjectName.text = task.assignedUser.assignedName
                mtvUserOrProjectName.visibility = View.VISIBLE
            } else {
                mtvUserOrProjectName.visibility = View.GONE
            }

            cbTaskState.setOnCheckedChangeListener(null)
            cbTaskState.isChecked = task.completed == true

            cbTaskState.setOnCheckedChangeListener { _, isChecked ->
                triggerSingleVibration(root.context)
                onCheckBoxClickListener(task, isChecked)
            }

            binding.root.setOnLongClickListener {
                triggerSingleVibration(root.context)
                onTaskLongClickListener(task)
                true
            }
        }
    }

}
