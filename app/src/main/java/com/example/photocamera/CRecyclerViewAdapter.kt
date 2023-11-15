package com.example.photocamera

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File

data class ImageSelectionItem(val filePath: String, val name: String, var isChecked: Boolean)

class CRecyclerViewAdapter(private val itemList: MutableList<ImageSelectionItem>) :
    RecyclerView.Adapter<CRecyclerViewAdapter.ViewHolder>()
{
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageViewItem)
        val textViewName: TextView = itemView.findViewById(R.id.photoPathItem)
        val checkBox: CheckBox = itemView.findViewById(R.id.checkBoxItem)
    }

    private val checkedState = mutableMapOf<Int, Boolean>()

    // Get the checked state for a specific position
    private fun isChecked(position: Int): Boolean {
        return checkedState[position] ?: false
    }

    // Set the checked state for a specific position
    private fun setChecked(position: Int, isChecked: Boolean) {
        checkedState[position] = isChecked
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int)
    {
        val item = itemList[position]
        Glide.with(holder.itemView.context)
            .load(File(item.filePath))
            .into(holder.imageView)

        holder.textViewName.text = item.name
        holder.checkBox.isChecked = item.isChecked
        // Set a listener for the checkbox
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            item.isChecked = isChecked
            setChecked(position, isChecked)
            // Handle any additional actions when the checkbox is checked or unchecked
        }

    }

    fun getSelectedOnes(): List<String>
    {
        val selectedPaths = mutableListOf<String>()
        for (item in itemList) {
            if (item.isChecked) {
                selectedPaths.add(item.filePath)
            }
        }
        return selectedPaths
    }
    override fun getItemCount(): Int {
        return itemList.size
    }

    fun addAll(allItems: List<ImageSelectionItem>)
    {
        itemList.addAll(allItems)
        this.notifyDataSetChanged()
    }

    fun clear()
    {
        itemList.clear()
        notifyDataSetChanged()
    }
}