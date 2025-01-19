package com.example.kiparys.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kiparys.Constants.COMPLETED_TASK
import com.example.kiparys.Constants.DELETE_TASK
import com.example.kiparys.Constants.EDIT_TASK
import com.example.kiparys.Constants.UNCOMPLETED_TASK
import com.example.kiparys.R
import com.example.kiparys.data.model.BottomSheetOption
import com.example.kiparys.data.model.ProjectTask
import com.example.kiparys.databinding.ItemOptionBinding


class ProjectTaskOptionsAdapter(
    private val context: Context,
    private val currentUserId: String,
    private val projectTask: ProjectTask,
    private val onOptionClick: (BottomSheetOption) -> Unit
) : RecyclerView.Adapter<ProjectTaskOptionsAdapter.OptionViewHolder>() {

    private val options = mutableListOf<BottomSheetOption>()

    init {
        options.apply {
            if (projectTask.completed == true) {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_task_not_completed),
                        iconRes = R.drawable.outline_check_box_outline_blank_24,
                        tag = UNCOMPLETED_TASK
                    )
                )
            } else {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_task_completed),
                        iconRes = R.drawable.outline_check_box_24,
                        tag = COMPLETED_TASK
                    )
                )
            }
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_edit),
                    iconRes = R.drawable.outline_edit_24,
                    tag = EDIT_TASK
                )
            )
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_delete),
                    iconRes = R.drawable.outline_delete_24,
                    tag = DELETE_TASK
                )
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptionViewHolder {
        val binding = ItemOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OptionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OptionViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount(): Int = options.size

    inner class OptionViewHolder(private val binding: ItemOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(option: BottomSheetOption) = with(binding) {
            sivOptionIcon.setImageResource(option.iconRes)
            mtvOptionText.text = option.title

            root.setOnClickListener {
                onOptionClick(option)
            }
        }
    }

}
