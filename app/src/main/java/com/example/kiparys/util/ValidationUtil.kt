package com.example.kiparys.util

import android.content.Context
import com.example.kiparys.R
import android.util.Patterns
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ValidationUtil {

    fun isValidEmail(input: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(input).matches()
    }

    fun validateBirthdate(context: Context, date: String): String? {
        if (date.isEmpty()) {
            return null
        }
        val sdf = SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault())
        sdf.isLenient = false
        return try {
            val parsedDate = sdf.parse(date)
            val calendar = Calendar.getInstance()
            val currentDate = calendar.time

            calendar.set(1900, Calendar.JANUARY, 1)
            val minDate = calendar.time

            when {
                parsedDate?.after(currentDate) == true -> {
                    context.getString(R.string.error_date_in_future)
                }

                parsedDate?.before(minDate) == true -> {
                    context.getString(R.string.error_date_in_past)
                }

                else -> null
            }
        } catch (_: ParseException) {
            context.getString(R.string.error_invalid_date_format)
        }
    }

    fun isValidPassword(password: String): Boolean {
        if (!isValidPasswordLength(password)) return false

        val passwordPattern = Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[\\W_]).{6,}$")
        return passwordPattern.matches(password)
    }

    fun isValidPasswordLength(password: String): Boolean {
        return password.length >= 6
    }

}
