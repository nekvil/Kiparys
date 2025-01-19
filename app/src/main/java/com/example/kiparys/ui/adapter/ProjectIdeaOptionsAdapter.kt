package com.example.kiparys.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kiparys.Constants.DELETE_IDEA
import com.example.kiparys.Constants.EDIT_IDEA
import com.example.kiparys.Constants.LIKE_IDEA
import com.example.kiparys.Constants.UNLIKE_IDEA
import com.example.kiparys.R
import com.example.kiparys.data.model.BottomSheetOption
import com.example.kiparys.data.model.ProjectIdea
import com.example.kiparys.databinding.ItemOptionBinding


class ProjectIdeaOptionsAdapter(
    private val context: Context,
    private val currentUserId: String,
    private val projectIdea: ProjectIdea,
    private val onOptionClick: (BottomSheetOption) -> Unit
) : RecyclerView.Adapter<ProjectIdeaOptionsAdapter.OptionViewHolder>() {

    private val options = mutableListOf<BottomSheetOption>()

    init {
        options.apply {
            if (projectIdea.votes?.contains(currentUserId) == true) {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_idea_vote_thumb_down),
                        iconRes = R.drawable.outline_sentiment_dissatisfied_24,
                        tag = UNLIKE_IDEA
                    )
                )
            } else {
                add(
                    BottomSheetOption(
                        title = context.getString(R.string.option_idea_vote_thumb_up),
                        iconRes = R.drawable.outline_sentiment_very_satisfied_24,
                        tag = LIKE_IDEA
                    )
                )
            }
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_edit),
                    iconRes = R.drawable.outline_edit_24,
                    tag = EDIT_IDEA
                )
            )
            add(
                BottomSheetOption(
                    title = context.getString(R.string.option_delete),
                    iconRes = R.drawable.outline_delete_24,
                    tag = DELETE_IDEA
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
