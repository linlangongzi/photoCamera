package com.example.photocamera

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import java.io.File

class CustomAdapter(context: Context, resource: Int, textViewResourceId: Int, objects: MutableList<String>) :
    ArrayAdapter<String>(context, resource, textViewResourceId, objects) {

    private val checkedState = mutableMapOf<Int, Boolean>()

    // Get the checked state for a specific position
    private fun isChecked(position: Int): Boolean {
        return checkedState[position] ?: false
    }

    // Set the checked state for a specific position
    private fun setChecked(position: Int, isChecked: Boolean) {
        checkedState[position] = isChecked
    }

    fun addAll(directoryPaths: List<String>) {
        for (path in directoryPaths) {
            add(path)
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val rowView = inflater.inflate(R.layout.list_item_layout, parent, false)

        val imageView = rowView.findViewById<ImageView>(R.id.imageViewItem)
        val textView = rowView.findViewById<TextView>(R.id.textViewItem)
        val checkBox = rowView.findViewById<CheckBox>(R.id.checkBoxItem)

        val filePath = getItem(position)
        // Set the values for each item
        textView.text = File(filePath).name

        Glide.with(context)
            .load(File(filePath))  // Assuming the item is a file path
            .placeholder(com.google.android.material.R.drawable.ic_search_black_24)
            .error(com.google.android.material.R.drawable.ic_clear_black_24)
            .centerCrop()
            .into(imageView)

        // Set the checkbox state
        checkBox.isChecked = isChecked(position)

        // Handle checkbox state change
        checkBox.setOnCheckedChangeListener { _, isChecked ->
            setChecked(position, isChecked)
        }

        return rowView
    }
}
