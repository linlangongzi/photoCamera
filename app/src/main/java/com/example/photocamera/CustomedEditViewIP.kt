package com.example.photocamera

import android.content.Context
import android.text.InputFilter
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import java.util.regex.Pattern

class IpAddressEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatEditText(context, attrs, defStyleAttr) {

    init {
        initialize()
    }

    private fun initialize() {
        filters = arrayOf(InputFilter { source, start, end, dest, dstart, dend ->
            val input = dest.subSequence(0, dstart).toString() + source.toString() + dest.subSequence(dend, dest.length)
            if (!isValidIpAddress(input)) {
                return@InputFilter ""
            }
            null // Accept the input
        })
    }

    private fun isValidIpAddress(input: String): Boolean {
        val ipAddressPattern =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
                    "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"

        val pattern = Pattern.compile(ipAddressPattern)
        val matcher = pattern.matcher(input)
        return matcher.matches()
    }
}
