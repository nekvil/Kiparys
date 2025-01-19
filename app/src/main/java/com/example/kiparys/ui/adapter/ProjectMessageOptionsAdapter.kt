package com.example.kiparys.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kiparys.Constants.ADD_IDEA
import com.example.kiparys.Constants.ADD_TASK
import com.example.kiparys.Constants.COPY_MESSAGE
import com.example.kiparys.Constants.DELETE_MESSAGE
import com.example.kiparys.Constants.EDIT_MESSAGE
import com.example.kiparys.Constants.PIN_MESSAGE
import com.example.kiparys.Constants.REPLY_MESSAGE
import com.example.kiparys.Constants.SHOW_VIEW_COUNT
import com.example.kiparys.Constants.UNPIN_MESSAGE
import com.example.kiparys.R
import com.example.kiparys.data.model.BottomSheetOption
import com.example.kiparys.data.model.ProjectMessage
import com.example.kiparys.databinding.ItemOptionBinding


class ProjectMessageOptionsAdapter(
    private val context: Context,
    private val currentUserId: String,
    private val message: ProjectMessage,
    private val onOptionClick: (BottomSheetOption) -> Unit
) : RecyclerView.Adapter<ProjectMessageOptionsAdapter.OptionViewHolder>() {

    private val options = mutableListOf<BottomSheetOption>()

    init {
        options.apply {
            if (message.senderId == currentUserId)
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_edit),
                        iconRes = R.drawable.outline_edit_24,
                        tag = EDIT_MESSAGE
                    )
                )
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_reply),
                    iconRes = R.drawable.outline_reply_24,
                    tag = REPLY_MESSAGE
                )
            )
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_copy),
                    iconRes = R.drawable.outline_content_copy_24,
                    tag = COPY_MESSAGE
                )
            )
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_create_task),
                    iconRes = R.drawable.outline_task_alt_24,
                    tag = ADD_TASK
                )
            )
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_add_idea),
                    iconRes = R.drawable.outline_emoji_objects_24,
                    tag = ADD_IDEA
                )
            )
            if (message.pinned == true) {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_unpin),
                        iconRes = R.drawable.outline_keep_off_24,
                        tag = UNPIN_MESSAGE
                    )
                )
            } else {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_pin),
                        iconRes = R.drawable.outline_keep_24,
                        tag = PIN_MESSAGE
                    )
                )
            }
            if (message.senderId == currentUserId)
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_show_view_count),
                        iconRes = R.drawable.outline_visibility_24dp,
                        tag = SHOW_VIEW_COUNT
                    )
                )
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_delete),
                    iconRes = R.drawable.outline_delete_24,
                    tag = DELETE_MESSAGE
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
