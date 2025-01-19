package com.example.kiparys.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kiparys.R
import com.example.kiparys.data.model.UserTask
import com.example.kiparys.databinding.ItemTaskBinding
import com.example.kiparys.util.StringUtil.formatTaskTimestamp
import com.example.kiparys.util.StringUtil.isSameDay
import com.example.kiparys.util.StringUtil.isTomorrow
import com.example.kiparys.util.SystemUtil.triggerSingleVibration


class UserTasksAdapter(
    private val onTaskClickListener: (UserTask) -> Unit,
    private val onCheckBoxClickListener: (UserTask, Boolean) -> Unit
) : ListAdapter<UserTask, UserTasksAdapter.TaskViewHolder>(differCallback) {

    companion object {
        private val differCallback = object : DiffUtil.ItemCallback<UserTask>() {
            override fun areItemsTheSame(oldItem: UserTask, newItem: UserTask): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: UserTask, newItem: UserTask): Boolean {
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

        fun bind(task: UserTask) = with(binding) {
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

            if (!task.projectName.isNullOrBlank() && task.completed != true) {
                mtvUserOrProjectName.text = task.projectName
                mtvUserOrProjectName.visibility = View.VISIBLE
            } else {
                mtvUserOrProjectName.visibility = View.GONE
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
                                    ContextCompat.getColor(
                                        root.context,
                                        R.color.md_theme_primary
                                    )
                                )
                            }

                            else -> {
                                mtvDueTime.setTextColor(
                                    ContextCompat.getColor(
                                        root.context,
                                        R.color.md_theme_onSurface
                                    )
                                )
                            }
                        }
                    }

                    else -> {
                        mtvDueTime.setTextColor(
                            ContextCompat.getColor(
                                root.context,
                                R.color.md_theme_error
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

            cbTaskState.setOnCheckedChangeListener(null)
            cbTaskState.isChecked = task.completed == true

            cbTaskState.setOnCheckedChangeListener { _, isChecked ->
                triggerSingleVibration(root.context)
                onCheckBoxClickListener(task, isChecked)
            }

            binding.root.setOnClickListener {
                onTaskClickListener(task)
            }
        }

    }

}
