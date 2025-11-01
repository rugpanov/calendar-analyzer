package dev.grigri.cli

import com.google.api.services.calendar.model.Event
import dev.grigri.analysis.*
import dev.grigri.llm.OptimizationSuggestion
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object OutputFormatter {
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE MMM dd")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm")

    fun formatStats(stats: MeetingStats, timeRangeName: String): String {
        val output = StringBuilder()

        output.appendLine("=" * 70)
        output.appendLine("CALENDAR ANALYSIS: $timeRangeName".uppercase())
        output.appendLine("=" * 70)
        output.appendLine()

        // Overview
        output.appendLine("OVERVIEW")
        output.appendLine("-" * 70)
        output.appendLine("Total meetings:        ${stats.totalMeetings}")
        output.appendLine("Average duration:      ${stats.averageDurationMinutes.toInt()} minutes")
        output.appendLine("Median duration:       ${stats.medianDurationMinutes} minutes")
        output.appendLine("Fragmentation score:   ${String.format("%.1f%%", stats.fragmentationScore * 100)}")
        output.appendLine()

        // Meetings per day
        if (stats.meetingsPerDay.isNotEmpty()) {
            output.appendLine("MEETINGS PER DAY")
            output.appendLine("-" * 70)
            stats.meetingsPerDay.toSortedMap().forEach { (date, count) ->
                output.appendLine("${date.format(dateFormatter)}: $count meeting${if (count != 1) "s" else ""}")
            }
            output.appendLine()
        }

        // Longest free blocks
        if (stats.longestFreeBlocks.isNotEmpty()) {
            output.appendLine("LONGEST FREE BLOCKS")
            output.appendLine("-" * 70)
            stats.longestFreeBlocks.take(5).forEach { block ->
                val hours = block.durationMinutes / 60
                val minutes = block.durationMinutes % 60
                val duration = if (hours > 0) {
                    "${hours}h ${minutes}m"
                } else {
                    "${minutes}m"
                }
                output.appendLine("${block.start.format(dateTimeFormatter)} - ${block.end.format(timeFormatter)} ($duration)")
            }
            output.appendLine()
        }

        // Reschedulable vs Fixed
        output.appendLine("MEETING CATEGORIZATION")
        output.appendLine("-" * 70)
        output.appendLine("Reschedulable (≤ 3 participants): ${stats.reschedulableMeetings.size}")
        output.appendLine("Fixed (> 3 participants):         ${stats.fixedMeetings.size}")
        output.appendLine()

        return output.toString()
    }

    fun formatOptimization(suggestion: OptimizationSuggestion): String {
        val output = StringBuilder()

        output.appendLine("FOCUS TIME RECOMMENDATIONS")
        output.appendLine("=" * 70)
        output.appendLine()

        if (suggestion.suggestions.isEmpty()) {
            output.appendLine("No optimization suggestions available.")
            output.appendLine("Your calendar may already be well-optimized for focus time.")
        } else {
            suggestion.suggestions.forEachIndexed { index, s ->
                output.appendLine("${index + 1}. $s")
            }
        }
        output.appendLine()

        return output.toString()
    }

    fun formatReschedulableMeetings(meetings: List<ReschedulableMeeting>): String {
        val output = StringBuilder()

        output.appendLine("RESCHEDULABLE MEETINGS")
        output.appendLine("=" * 70)
        output.appendLine()

        if (meetings.isEmpty()) {
            output.appendLine("No reschedulable meetings found.")
        } else {
            meetings.sortedBy { it.start }.forEach { meeting ->
                output.appendLine("• ${meeting.event.summary ?: "Untitled"}")
                output.appendLine("  ${meeting.start.format(dateTimeFormatter)} - ${meeting.end.format(timeFormatter)}")
                output.appendLine("  Duration: ${meeting.durationMinutes} min | Participants: ${meeting.participantCount}")
                output.appendLine()
            }
        }

        return output.toString()
    }

    fun formatEventList(events: List<Event>, timeRangeName: String): String {
        val output = StringBuilder()

        output.appendLine("=" * 70)
        output.appendLine("CALENDAR EVENTS: $timeRangeName".uppercase())
        output.appendLine("=" * 70)
        output.appendLine()

        if (events.isEmpty()) {
            output.appendLine("No events found for this time range.")
        } else {
            output.appendLine("Total events: ${events.size}")
            output.appendLine()

            events.forEach { event ->
                val summary = event.summary ?: "Untitled"
                val start = if (event.start.dateTime != null) {
                    event.start.dateTime.toZonedDateTime()
                } else {
                    // All-day event
                    null
                }
                val end = if (event.end.dateTime != null) {
                    event.end.dateTime.toZonedDateTime()
                } else {
                    null
                }

                output.appendLine("• $summary")

                if (start != null && end != null) {
                    output.appendLine("  ${start.format(dateTimeFormatter)} - ${end.format(timeFormatter)}")
                    val duration = java.time.Duration.between(start, end).toMinutes()
                    output.append("  Duration: $duration min")
                } else {
                    output.append("  All-day event")
                    if (event.start.date != null) {
                        output.append(" on ${event.start.date}")
                    }
                }

                val attendees = event.attendees?.size ?: 0
                if (attendees > 0) {
                    output.append(" | Attendees: $attendees")
                }
                output.appendLine()

                if (event.location != null) {
                    output.appendLine("  📍 ${event.location}")
                }

                if (event.description != null && event.description.isNotBlank()) {
                    val desc = event.description.take(100)
                    output.appendLine("  💬 ${desc}${if (event.description.length > 100) "..." else ""}")
                }

                output.appendLine()
            }
        }

        return output.toString()
    }

    private fun com.google.api.client.util.DateTime.toZonedDateTime(): java.time.ZonedDateTime {
        return java.time.Instant.ofEpochMilli(this.value)
            .atZone(ZoneId.systemDefault())
    }

    private operator fun String.times(count: Int): String = this.repeat(count)
}
