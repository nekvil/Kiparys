package com.example.kiparys.ui.adapter

import android.content.Context
import com.example.kiparys.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kiparys.Constants.CAMERA
import com.example.kiparys.Constants.FILES
import com.example.kiparys.Constants.PHOTO
import com.example.kiparys.databinding.ItemOptionBinding


data class AttachOption(
    val title: String,
    val iconRes: Int,
    val tag: String
)

class AttachToMessageOptionsAdapter(
    context: Context,
    private val onOptionClick: (AttachOption) -> Unit
) : RecyclerView.Adapter<AttachToMessageOptionsAdapter.OptionViewHolder>() {

    private val options = listOf(
        AttachOption(
            title = context.getString(R.string.option_photo),
            iconRes = R.drawable.outline_image_24,
            tag = PHOTO
        ),
        AttachOption(
            title = context.getString(R.string.option_camera),
            iconRes = R.drawable.outline_photo_camera_24,
            tag = CAMERA
        ),
        AttachOption(
            title = context.getString(R.string.option_files),
            iconRes = R.drawable.outline_upload_24,
            tag = FILES
        )
    )

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
        fun bind(option: AttachOption) = with(binding) {
            sivOptionIcon.setImageResource(option.iconRes)
            mtvOptionText.text = option.title

            root.setOnClickListener {
                onOptionClick(option)
            }
        }
    }

}
