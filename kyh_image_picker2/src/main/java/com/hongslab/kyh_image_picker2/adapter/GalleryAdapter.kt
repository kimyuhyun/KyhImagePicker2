package com.hongslab.kyh_image_picker2.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.hongslab.kyh_image_picker2.R
import com.hongslab.kyh_image_picker2.databinding.ImageRowBinding
import com.hongslab.kyh_image_picker2.models.ImageVO

class GalleryAdapter(
    private val context: Context,
    private val cellWidth: Int,
    private val clickListener: AdapterClickListener,
) : ListAdapter<ImageVO, GalleryAdapter.ItemViewHolder>(diffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(ImageRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    inner class ItemViewHolder(private val binding: ImageRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ImageVO) {
            binding.rlRoot.updateLayoutParams<ViewGroup.LayoutParams> {
                height = cellWidth
            }

            Glide.with(context)
                .load(item.uri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(binding.ivThumb)

            if (item.isToggle) {
                binding.tvCount.setBackgroundResource(R.drawable.circle_accent)
                binding.tvCount.text = "${item.seq + 1}"
            } else {
                binding.tvCount.setBackgroundResource(R.drawable.circle_accent_border)
            }

            if (item.isChoose) {
                val drawable = ColorDrawable(Color.WHITE)
                drawable.alpha = 128 // 불투명도를 50%로 조절
                binding.ivThumb.foreground = drawable
            } else {
                binding.ivThumb.foreground = null
            }

            val pos = adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                binding.rlRoot.setOnClickListener {
                    clickListener.onClick(pos)
                }
            }
        }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<ImageVO>() {
            override fun areItemsTheSame(oldItem: ImageVO, newItem: ImageVO): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ImageVO, newItem: ImageVO): Boolean {
                return oldItem == newItem
            }
        }
    }

    interface AdapterClickListener {
        fun onClick(pos: Int)
    }
}