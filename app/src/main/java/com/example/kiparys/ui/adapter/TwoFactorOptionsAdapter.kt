package com.example.kiparys.ui.adapter

import com.example.kiparys.R
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kiparys.Constants.PHONE_FACTOR
import com.example.kiparys.Constants.TOTP_FACTOR
import com.example.kiparys.databinding.ItemOptionBinding
import com.google.firebase.auth.MultiFactorInfo


class TwoFactorOptionsAdapter(
    private val factors: List<MultiFactorInfo>,
    private val onFactorSelected: (Int) -> Unit
) : RecyclerView.Adapter<TwoFactorOptionsAdapter.FactorViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FactorViewHolder {
        val binding = ItemOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FactorViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FactorViewHolder, position: Int) {
        holder.bind(factors[position], position)
    }

    override fun getItemCount(): Int = factors.size

    inner class FactorViewHolder(private val binding: ItemOptionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(factor: MultiFactorInfo, position: Int) {
            binding.mtvOptionText.text = factor.displayName ?: "Unknown Factor"

            val iconRes = when (factor.factorId) {
                TOTP_FACTOR -> R.drawable.outline_av_timer_24
                PHONE_FACTOR -> R.drawable.outline_numbers_24
                else -> R.drawable.outline_security_key_24
            }
            binding.sivOptionIcon.setImageResource(iconRes)

            binding.root.setOnClickListener { onFactorSelected(position) }
        }
    }

}
