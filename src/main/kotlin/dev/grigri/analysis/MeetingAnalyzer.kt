package dev.grigri.analysis

import com.google.api.services.calendar.model.Event
import java.time.*
import java.time.temporal.ChronoUnit

data class MeetingStats(
    val totalMeetings: Int,
    val averageDurationMinutes: Double,
    val medianDurationMinutes: Long,
    val meetingsPerDay: Map<LocalDate, Int>,
    val longestFreeBlocks: List<FreeBlock>,
    val fragmentationScore: Double,
    val reschedulableMeetings: List<ReschedulableMeeting>,
    val fixedMeetings: List<FixedMeeting>
)

data class FreeBlock(
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val durationMinutes: Long
)

data class ReschedulableMeeting(
    val event: Event,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val durationMinutes: Long,
    val participantCount: Int
)

data class FixedMeeting(
    val event: Event,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
    val durationMinutes: Long,
    val participantCount: Int
)

class MeetingAnalyzer(
    private val workDayStart: Int = 9,  // 9 AM
    private val workDayEnd: Int = 18,   // 6 PM
    private val reschedulableThreshold: Int = 3,
    private val neverMoveIds: Set<String> = emptySet()
) {
    private val zoneId = ZoneId.systemDefault()

    fun analyze(events: List<Event>): MeetingStats {
        val meetings = events.filter { it.start?.dateTime != null }

        if (meetings.isEmpty()) {
            return MeetingStats(
                totalMeetings = 0,
                averageDurationMinutes = 0.0,
                medianDurationMinutes = 0,
                meetingsPerDay = emptyMap(),
                longestFreeBlocks = emptyList(),
                fragmentationScore = 0.0,
                reschedulableMeetings = emptyList(),
                fixedMeetings = emptyList()
            )
        }

        val durations = meetings.map { calculateDuration(it) }
        val sortedDurations = durations.sorted()

        val (reschedulable, fixed) = categorizeMeetings(meetings)

        return MeetingStats(
            totalMeetings = meetings.size,
            averageDurationMinutes = durations.average(),
            medianDurationMinutes = calculateMedian(sortedDurations),
            meetingsPerDay = calculateMeetingsPerDay(meetings),
            longestFreeBlocks = findLongestFreeBlocks(meetings),
            fragmentationScore = calculateFragmentation(meetings),
            reschedulableMeetings = reschedulable,
            fixedMeetings = fixed
        )
    }

    private fun calculateDuration(event: Event): Long {
        val start = toZonedDateTime(event.start.dateTime)
        val end = toZonedDateTime(event.end.dateTime)
        return ChronoUnit.MINUTES.between(start, end)
    }

    private fun calculateMedian(sorted: List<Long>): Long {
        return if (sorted.isEmpty()) {
            0
        } else if (sorted.size % 2 == 0) {
            (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
        } else {
            sorted[sorted.size / 2]
        }
    }

    private fun calculateMeetingsPerDay(meetings: List<Event>): Map<LocalDate, Int> {
        return meetings
            .groupBy { toZonedDateTime(it.start.dateTime).toLocalDate() }
            .mapValues { it.value.size }
    }

    private fun findLongestFreeBlocks(meetings: List<Event>): List<FreeBlock> {
        val sortedMeetings = meetings.sortedBy { it.start.dateTime.value }
        val freeBlocks = mutableListOf<FreeBlock>()

        // Group meetings by day
        val meetingsByDay = sortedMeetings.groupBy {
            toZonedDateTime(it.start.dateTime).toLocalDate()
        }

        meetingsByDay.forEach { (date, dayMeetings) ->
            val workStart = date.atTime(workDayStart, 0).atZone(zoneId)
            val workEnd = date.atTime(workDayEnd, 0).atZone(zoneId)

            // Check start of day
            val firstMeeting = toZonedDateTime(dayMeetings.first().start.dateTime)
            if (firstMeeting.isAfter(workStart)) {
                val duration = ChronoUnit.MINUTES.between(workStart, firstMeeting)
                if (duration > 0) {
                    freeBlocks.add(FreeBlock(workStart, firstMeeting, duration))
                }
            }

            // Check between meetings
            for (i in 0 until dayMeetings.size - 1) {
                val currentEnd = toZonedDateTime(dayMeetings[i].end.dateTime)
                val nextStart = toZonedDateTime(dayMeetings[i + 1].start.dateTime)
                val duration = ChronoUnit.MINUTES.between(currentEnd, nextStart)
                if (duration > 0) {
                    freeBlocks.add(FreeBlock(currentEnd, nextStart, duration))
                }
            }

            // Check end of day
            val lastMeeting = toZonedDateTime(dayMeetings.last().end.dateTime)
            if (lastMeeting.isBefore(workEnd)) {
                val duration = ChronoUnit.MINUTES.between(lastMeeting, workEnd)
                if (duration > 0) {
                    freeBlocks.add(FreeBlock(lastMeeting, workEnd, duration))
                }
            }
        }

        return freeBlocks.sortedByDescending { it.durationMinutes }.take(10)
    }

    private fun calculateFragmentation(meetings: List<Event>): Double {
        val sortedMeetings = meetings.sortedBy { it.start.dateTime.value }
        val meetingsByDay = sortedMeetings.groupBy {
            toZonedDateTime(it.start.dateTime).toLocalDate()
        }

        val fragmentationScores = meetingsByDay.map { (_, dayMeetings) ->
            val gaps = mutableListOf<Long>()
            for (i in 0 until dayMeetings.size - 1) {
                val currentEnd = toZonedDateTime(dayMeetings[i].end.dateTime)
                val nextStart = toZonedDateTime(dayMeetings[i + 1].start.dateTime)
                val gap = ChronoUnit.MINUTES.between(currentEnd, nextStart)
                gaps.add(gap)
            }

            // Count short gaps (< 60 minutes) as fragmentation
            val shortGaps = gaps.count { it in 1..59 }
            if (gaps.isEmpty()) 0.0 else shortGaps.toDouble() / gaps.size
        }

        return if (fragmentationScores.isEmpty()) 0.0 else fragmentationScores.average()
    }

    private fun categorizeMeetings(meetings: List<Event>): Pair<List<ReschedulableMeeting>, List<FixedMeeting>> {
        val reschedulable = mutableListOf<ReschedulableMeeting>()
        val fixed = mutableListOf<FixedMeeting>()

        meetings.forEach { event ->
            val start = toZonedDateTime(event.start.dateTime)
            val end = toZonedDateTime(event.end.dateTime)
            val duration = ChronoUnit.MINUTES.between(start, end)
            val participantCount = (event.attendees?.size ?: 0) + 1 // +1 for organizer

            if (neverMoveIds.contains(event.id) || participantCount > reschedulableThreshold) {
                fixed.add(FixedMeeting(event, start, end, duration, participantCount))
            } else {
                reschedulable.add(ReschedulableMeeting(event, start, end, duration, participantCount))
            }
        }

        return Pair(reschedulable, fixed)
    }

    private fun toZonedDateTime(dateTime: com.google.api.client.util.DateTime): ZonedDateTime {
        return Instant.ofEpochMilli(dateTime.value).atZone(zoneId)
    }
}
