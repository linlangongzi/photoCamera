package com.example.photocamera

import android.content.Context
import android.text.InputFilter
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

class IpAddressEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatEditText(context, attrs, defStyleAttr) {

    init {
        initialize()
    }

    private fun initialize()
    {
        filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val input = dest.subSequence(0, dstart).toString() + source.toString() + dest.subSequence(dend, dest.length)
            if (!NetworkUtils.isValidIpAddress(input)) {
                return@InputFilter ""
            }
            null // Accept the input
        })
    }

}
