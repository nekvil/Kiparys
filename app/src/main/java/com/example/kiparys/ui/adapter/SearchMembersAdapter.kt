package com.example.kiparys.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.transform.CircleCropTransformation
import com.example.kiparys.KiparysApplication
import com.example.kiparys.R
import com.example.kiparys.data.model.User
import com.example.kiparys.databinding.ItemUserBinding
import com.example.kiparys.util.SystemUtil.triggerSingleVibration


class SearchMembersAdapter(
    private val onUserClick: (User) -> Unit,
    private var selectedMember: User? = null
) : ListAdapter<User, SearchMembersAdapter.UserViewHolder>(differCallback) {

    companion object {
        private val differCallback = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }
        }
    }

    fun updateSelectedUsers(newSelectedMember: User?) {
        if (newSelectedMember == selectedMember) return

        val oldSelectedMember = selectedMember
        selectedMember = newSelectedMember

        listOfNotNull(oldSelectedMember, newSelectedMember).forEach { user ->
            val position = currentList.indexOf(user)
            if (position != -1) notifyItemChanged(position)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = getItem(position)
        holder.bind(user, user == selectedMember)
    }

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val imageLoader =
            (binding.root.context.applicationContext as KiparysApplication).imageLoader

        fun bind(user: User, isSelected: Boolean) {
            val request = ImageRequest.Builder(binding.root.context)
                .placeholder(R.drawable.baseline_circle_24)
                .error(R.drawable.baseline_circle_24)
                .data(user.profileImageUrl)
                .precision(Precision.EXACT)
                .transformations(CircleCropTransformation())
                .target(binding.sivUserIcon)
                .build()
            imageLoader.enqueue(request)

            binding.mtvUserName.text = binding.root.context.getString(
                R.string.user_full_name,
                user.firstName,
                if (user.lastName.isNullOrEmpty()) "" else " ${user.lastName}"
            )
            binding.mtvUserEmail.text = user.email
            binding.sivCheckMark.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.root.setOnClickListener {
                onUserClick(user)
                triggerSingleVibration(binding.root.context)
            }
        }
    }

}
