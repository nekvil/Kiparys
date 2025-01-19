package com.example.kiparys.ui.adapter

import android.content.Context
import com.example.kiparys.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kiparys.Constants.CLEAR_PROJECT_HISTORY
import com.example.kiparys.Constants.DISABLE_PROJECT_NOTIFICATIONS
import com.example.kiparys.Constants.ENABLE_PROJECT_NOTIFICATIONS
import com.example.kiparys.Constants.LEAVE_PROJECT
import com.example.kiparys.Constants.MARK_AS_READ
import com.example.kiparys.Constants.MARK_AS_UNREAD
import com.example.kiparys.Constants.PIN_PROJECT
import com.example.kiparys.Constants.UNPIN_PROJECT
import com.example.kiparys.data.model.BottomSheetOption
import com.example.kiparys.data.model.UserProject
import com.example.kiparys.databinding.ItemOptionBinding


class UserProjectOptionsAdapter(
    private val context: Context,
    private val project: UserProject,
    private val onOptionClick: (BottomSheetOption) -> Unit
) : RecyclerView.Adapter<UserProjectOptionsAdapter.OptionViewHolder>() {

    private val options = mutableListOf<BottomSheetOption>()

    init {
        options.apply {
            if (project.isUnread()) {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_mark_as_read),
                        iconRes = R.drawable.outline_mark_chat_read_24,
                        tag = MARK_AS_READ
                    )
                )
            } else {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_mark_as_unread),
                        iconRes = R.drawable.outline_mark_chat_unread_24,
                        tag = MARK_AS_UNREAD
                    )
                )
            }
            if (project.notifications == false) {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_enable_notifications),
                        iconRes = R.drawable.outline_notifications_24,
                        tag = ENABLE_PROJECT_NOTIFICATIONS
                    )
                )
            } else {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_disable_notifications),
                        iconRes = R.drawable.outline_notifications_off_24dp,
                        tag = DISABLE_PROJECT_NOTIFICATIONS
                    )
                )
            }
            if (project.pinned == true) {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_unpin),
                        iconRes = R.drawable.outline_keep_off_24,
                        tag = UNPIN_PROJECT
                    )
                )
            } else {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_pin),
                        iconRes = R.drawable.outline_keep_24,
                        tag = PIN_PROJECT
                    )
                )
            }
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_clear_project_history),
                    iconRes = R.drawable.outline_delete_history_24,
                    tag = CLEAR_PROJECT_HISTORY
                )
            )
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_delete_and_leave_project),
                    iconRes = R.drawable.outline_delete_24,
                    tag = LEAVE_PROJECT
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

fun UserProject.isUnread(): Boolean {
    return timestamp?.let { lastMessageTime ->
        lastSeenTimestamp?.let { lastSeenTime ->
            lastMessageTime > lastSeenTime
        }
    } == true
}
