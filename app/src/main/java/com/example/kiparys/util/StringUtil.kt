package com.example.kiparys.util

import android.content.Context
import android.text.format.DateFormat
import com.example.kiparys.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object StringUtil {

    fun formatTaskTimestamp(
        timestamp: Long,
        context: Context,
        locale: Locale = Locale.getDefault()
    ): String {
        val currentTime = System.currentTimeMillis()
        val difference = currentTime - timestamp

        val calendar = Calendar.getInstance()
        val targetDate = calendar.apply { timeInMillis = timestamp }

        val today = Calendar.getInstance()
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val oneWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }

        val hoursAgo = abs(difference / (1000 * 60 * 60)).toInt()
        val daysAgo = abs(difference / (1000 * 60 * 60 * 24)).toInt()

        return when {
            timestamp > currentTime -> {
                when {
                    targetDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            targetDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) ->
                        context.getString(
                            R.string.label_today_at_time,
                            SimpleDateFormat("HH:mm", locale).format(Date(timestamp))
                        )

                    targetDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                            targetDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) + 1 ->
                        context.getString(
                            R.string.label_tomorrow_at_time,
                            SimpleDateFormat("HH:mm", locale).format(Date(timestamp))
                        )

                    else -> SimpleDateFormat("EEE, d MMM, HH:mm", locale).format(Date(timestamp))
                }
            }

            targetDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    targetDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> {
                if (locale.language == "ru")
                    "$hoursAgo ${getPluralForm(hoursAgo, "час", "часа", "часов")} назад"
                else
                    context.getString(R.string.label_hours_ago, hoursAgo)
            }

            targetDate.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                    targetDate.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) ->
                context.getString(R.string.label_yesterday)

            targetDate.after(oneWeekAgo) -> {
                if (locale.language == "ru")
                    "$daysAgo ${getPluralForm(daysAgo, "день", "дня", "дней")} назад"
                else
                    context.getString(R.string.label_days_ago, daysAgo)
            }

            daysAgo in 7..30 -> {
                val weeksAgo = daysAgo / 7
                if (locale.language == "ru")
                    "$weeksAgo ${getPluralForm(weeksAgo, "неделю", "недели", "недель")} назад"
                else
                    context.getString(R.string.label_weeks_ago, weeksAgo)
            }

            daysAgo in 31..365 -> {
                val monthsAgo = daysAgo / 30
                if (locale.language == "ru")
                    "$monthsAgo ${getPluralForm(monthsAgo, "месяц", "месяца", "месяцев")} назад"
                else
                    context.getString(R.string.label_months_ago, monthsAgo)
            }

            else -> {
                val yearsAgo = daysAgo / 365
                if (locale.language == "ru")
                    "$yearsAgo ${getPluralForm(yearsAgo, "год", "года", "лет")} назад"
                else
                    context.getString(R.string.label_years_ago, yearsAgo)
            }
        }
    }

    fun formatUserProjectTimestamp(timestamp: Long): String {
        val messageDate = Calendar.getInstance().apply { time = Date(timestamp) }
        val currentDate = Calendar.getInstance()

        return when {
            isSameDay(messageDate, currentDate) -> SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(messageDate.time)

            isSameWeek(messageDate, currentDate) -> SimpleDateFormat(
                "E",
                Locale.getDefault()
            ).format(messageDate.time)

            isSameYear(messageDate, currentDate) -> SimpleDateFormat(
                "d MMM",
                Locale.getDefault()
            ).format(messageDate.time)

            else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(messageDate.time)
        }
    }

    fun getUserLastOnlineState(context: Context, lastOnlineTimestamp: Long?): String {
        if (lastOnlineTimestamp == null) {
            return context.getString(R.string.label_long_time_ago)
        }

        val lastOnlineDate = Calendar.getInstance().apply { timeInMillis = lastOnlineTimestamp }
        val currentDate = Calendar.getInstance()

        return when {
            isSameDay(
                lastOnlineDate,
                currentDate
            ) -> context.getString(R.string.label_recently_online)

            isYesterday(lastOnlineDate) -> context.getString(R.string.label_yesterday_online)
            isSameWeek(
                lastOnlineDate,
                currentDate
            ) -> context.getString(R.string.label_this_week_online)

            isSameMonth(
                lastOnlineDate,
                currentDate
            ) -> context.getString(R.string.label_this_month_online)

            isSameYear(
                lastOnlineDate,
                currentDate
            ) -> context.getString(R.string.label_this_year_online)

            else -> context.getString(R.string.label_long_time_ago)
        }
    }

    fun formatMessageTimestamp(context: Context, timestamp: Long?): String {
        return timestamp?.let {
            DateFormat.format("HH:mm", Date(it)).toString()
        } ?: context.getString(R.string.label_unknown_time)
    }

    fun formatMediaUploadedTimestamp(context: Context, timestamp: Long): String {
        val date = Date(timestamp)

        val dateFormatter = SimpleDateFormat("d MMM", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateString = dateFormatter.format(date)
        val timeString = timeFormatter.format(date)

        return context.getString(R.string.label_media_uploaded, dateString, timeString)
    }

    fun formatUserName(fullName: String, maxNameLength: Int, maxFullNameLength: Int): String {
        val parts = fullName.split(" ")
        val name = parts.getOrNull(0) ?: ""
        val surname = parts.getOrNull(1) ?: ""

        val fullName = "$name $surname"
        if (fullName.length <= maxFullNameLength) {
            return fullName
        }

        val shortName = if (name.length > maxNameLength) name.take(maxNameLength) + ".." else name
        val shortSurname = if (surname.isNotEmpty()) " ${surname.take(1)}." else ""

        return shortName + shortSurname
    }

    fun maskEmail(email: String): String {
        val index = email.indexOf("@")

        if (index <= 2) return email

        val visiblePart = email.substring(0, index)
        val charsToMask = visiblePart.length / 2

        val visibleStart = visiblePart.substring(0, (visiblePart.length - charsToMask) / 2)
        val visibleEnd =
            visiblePart.substring(visiblePart.length - (visiblePart.length - charsToMask) / 2)

        val hiddenPart =
            visiblePart.substring(visibleStart.length, visiblePart.length - visibleEnd.length)
                .map { '*' }

        return visibleStart + hiddenPart.joinToString("") + visibleEnd + email.substring(index)
    }

    fun getMessageViewCountString(count: Int, locale: Locale = Locale.getDefault()): String {
        return if (locale.language == "ru") {
            "$count ${getPluralForm(count, "просмотр", "просмотра", "просмотров")}"
        } else {
            "$count views"
        }
    }

    fun getMemberCountString(count: Int, locale: Locale = Locale.getDefault()): String {
        return if (locale.language == "ru") {
            "$count ${getPluralForm(count, "участник", "участника", "участников")}"
        } else {
            "$count members"
        }
    }

    fun stringToTimestamp(context: Context, dateString: String): Long? {
        if (dateString.isEmpty()) return null
        return try {
            val sdf = SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault())
            val date: Date =
                sdf.parse(dateString) ?: throw IllegalArgumentException("Invalid date format")
            date.time
        } catch (e: Exception) {
            throw IllegalArgumentException("Error parsing date: ${e.message}")
        }
    }

    fun timestampToString(context: Context, timestamp: Long?): String? {
        return if (timestamp != null) {
            val sdf = SimpleDateFormat(context.getString(R.string.date_format), Locale.getDefault())
            val date = Date(timestamp)
            sdf.format(date)
        } else {
            null
        }
    }


    fun isSameDay(currentTime: Long, targetTime: Long): Boolean {
        val current = Calendar.getInstance().apply { timeInMillis = currentTime }
        val target = Calendar.getInstance().apply { timeInMillis = targetTime }
        return current.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                current.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    }

    fun isTomorrow(currentTime: Long, targetTime: Long): Boolean {
        val current = Calendar.getInstance().apply { timeInMillis = currentTime }
        val target = Calendar.getInstance().apply { timeInMillis = targetTime }
        return current.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
                current.get(Calendar.DAY_OF_YEAR) + 1 == target.get(Calendar.DAY_OF_YEAR)
    }

    private fun isSameDay(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.DAY_OF_YEAR) == date2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(date1: Calendar): Boolean {
        val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return isSameDay(date1, yesterday)
    }

    private fun isSameWeek(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.WEEK_OF_YEAR) == date2.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR) &&
                date1.get(Calendar.MONTH) == date2.get(Calendar.MONTH)
    }

    private fun isSameYear(date1: Calendar, date2: Calendar): Boolean {
        return date1.get(Calendar.YEAR) == date2.get(Calendar.YEAR)
    }

    private fun getPluralForm(number: Int, one: String, few: String, many: String): String {
        val rem100 = number % 100
        val rem10 = number % 10
        return when {
            rem100 in 11..19 -> many
            rem10 == 1 -> one
            rem10 in 2..4 -> few
            else -> many
        }
    }

}
