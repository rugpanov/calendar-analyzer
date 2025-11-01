package dev.grigri.calendar

import com.google.api.client.util.DateTime
import java.time.*
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class TimeRange {
    TOMORROW,
    CURRENT_WEEK,
    NEXT_WEEK
}

object TimeRangeCalculator {
    private val zoneId = ZoneId.systemDefault()

    fun getRange(range: TimeRange): Pair<DateTime, DateTime> {
        return when (range) {
            TimeRange.TOMORROW -> getTomorrowRange()
            TimeRange.CURRENT_WEEK -> getCurrentWeekRange()
            TimeRange.NEXT_WEEK -> getNextWeekRange()
        }
    }

    private fun getTomorrowRange(): Pair<DateTime, DateTime> {
        val tomorrow = LocalDate.now(zoneId).plusDays(1)
        val start = tomorrow.atStartOfDay(zoneId).toInstant()
        val end = tomorrow.plusDays(1).atStartOfDay(zoneId).toInstant()
        return toDateTimePair(start, end)
    }

    private fun getCurrentWeekRange(): Pair<DateTime, DateTime> {
        val now = LocalDate.now(zoneId)
        val weekStart = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekEnd = weekStart.plusWeeks(1)
        val start = weekStart.atStartOfDay(zoneId).toInstant()
        val end = weekEnd.atStartOfDay(zoneId).toInstant()
        return toDateTimePair(start, end)
    }

    private fun getNextWeekRange(): Pair<DateTime, DateTime> {
        val now = LocalDate.now(zoneId)
        val nextWeekStart = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        val nextWeekEnd = nextWeekStart.plusWeeks(1)
        val start = nextWeekStart.atStartOfDay(zoneId).toInstant()
        val end = nextWeekEnd.atStartOfDay(zoneId).toInstant()
        return toDateTimePair(start, end)
    }

    private fun toDateTimePair(start: Instant, end: Instant): Pair<DateTime, DateTime> {
        return Pair(
            DateTime(start.toEpochMilli()),
            DateTime(end.toEpochMilli())
        )
    }
}
