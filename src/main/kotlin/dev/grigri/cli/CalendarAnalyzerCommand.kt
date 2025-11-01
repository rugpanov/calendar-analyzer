package dev.grigri.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.enum
import dev.grigri.analysis.MeetingAnalyzer
import dev.grigri.calendar.GoogleCalendarClient
import dev.grigri.calendar.TimeRange
import dev.grigri.calendar.TimeRangeCalculator
import dev.grigri.config.ConfigManager
import dev.grigri.llm.FocusTimeOptimizer
import dev.grigri.llm.KoogClient
import kotlinx.coroutines.runBlocking

class CalendarAnalyzerCommand : CliktCommand(
    name = "calendar-analyzer",
    help = "Analyze your Google Calendar and get focus time recommendations"
) {
    override fun run() = Unit
}

class AnalyzeCommand : CliktCommand(
    name = "analyze",
    help = "Analyze calendar for a specific time range"
) {
    private val timeRange by option("-r", "--range", help = "Time range to analyze")
        .enum<TimeRange>()
        .default(TimeRange.CURRENT_WEEK)

    private val withOptimization by option("-o", "--optimize", help = "Generate focus time optimization suggestions")
        .flag(default = false)

    private val showReschedulable by option("-s", "--show-reschedulable", help = "Show list of reschedulable meetings")
        .flag(default = false)

    override fun run() = runBlocking {
        try {
            val config = ConfigManager.load()
            val calendarClient = GoogleCalendarClient()
            val (timeMin, timeMax) = TimeRangeCalculator.getRange(timeRange)

            echo("Fetching events from Google Calendar...")
            val events = calendarClient.getEvents(timeMin, timeMax)

            echo("Analyzing ${events.size} events...\n")

            val analyzer = MeetingAnalyzer(
                workDayStart = config.workDayStart,
                workDayEnd = config.workDayEnd,
                reschedulableThreshold = config.reschedulableThreshold,
                neverMoveIds = config.neverMoveEventIds
            )

            val stats = analyzer.analyze(events)
            val timeRangeName = when (timeRange) {
                TimeRange.TOMORROW -> "Tomorrow"
                TimeRange.CURRENT_WEEK -> "Current Week"
                TimeRange.NEXT_WEEK -> "Next Week"
            }

            echo(OutputFormatter.formatStats(stats, timeRangeName))

            if (showReschedulable) {
                echo(OutputFormatter.formatReschedulableMeetings(stats.reschedulableMeetings))
            }

            if (withOptimization) {
                echo("Generating optimization suggestions with AI...\n")
                val koogClient = KoogClient()
                try {
                    val optimizer = FocusTimeOptimizer(
                        koogClient,
                        config.minFocusBlockMinutes,
                        config.maxFocusBlockMinutes
                    )
                    val suggestions = optimizer.generateSuggestions(stats)
                    echo(OutputFormatter.formatOptimization(suggestions))
                } finally {
                    koogClient.close()
                }
            }
        } catch (e: Exception) {
            echo("Error: ${e.message}", err = true)
            throw e
        }
    }
}

class ConfigCommand : CliktCommand(
    name = "config",
    help = "Manage configuration"
) {
    override fun run() = Unit
}

class NeverMoveCommand : CliktCommand(
    name = "never-move",
    help = "Mark a meeting as 'never move' by event ID"
) {
    private val eventId by option("-e", "--event-id", help = "Google Calendar event ID")
        .required()

    override fun run() {
        ConfigManager.addNeverMoveEvent(eventId)
        echo("Event $eventId marked as 'never move'")
    }
}

class AllowMoveCommand : CliktCommand(
    name = "allow-move",
    help = "Remove 'never move' restriction from a meeting"
) {
    private val eventId by option("-e", "--event-id", help = "Google Calendar event ID")
        .required()

    override fun run() {
        ConfigManager.removeNeverMoveEvent(eventId)
        echo("Event $eventId can now be moved")
    }
}

class ShowConfigCommand : CliktCommand(
    name = "show",
    help = "Show current configuration"
) {
    override fun run() {
        val config = ConfigManager.load()
        echo("Current Configuration:")
        echo("Work day: ${config.workDayStart}:00 - ${config.workDayEnd}:00")
        echo("Reschedulable threshold: ≤ ${config.reschedulableThreshold} participants")
        echo("Focus block target: ${config.minFocusBlockMinutes}-${config.maxFocusBlockMinutes} minutes")
        echo("Never move events: ${config.neverMoveEventIds.size}")
        if (config.neverMoveEventIds.isNotEmpty()) {
            config.neverMoveEventIds.forEach { id ->
                echo("  - $id")
            }
        }
    }
}

class ListEventsCommand : CliktCommand(
    name = "list",
    help = "List all events from Google Calendar"
) {
    private val timeRange by option("-r", "--range", help = "Time range to list events for")
        .enum<TimeRange>()
        .default(TimeRange.CURRENT_WEEK)

    override fun run() {
        try {
            val calendarClient = GoogleCalendarClient()
            val (timeMin, timeMax) = TimeRangeCalculator.getRange(timeRange)

            echo("Fetching events from Google Calendar...")
            val events = calendarClient.getEvents(timeMin, timeMax)

            val timeRangeName = when (timeRange) {
                TimeRange.TOMORROW -> "Tomorrow"
                TimeRange.CURRENT_WEEK -> "Current Week"
                TimeRange.NEXT_WEEK -> "Next Week"
            }

            echo(OutputFormatter.formatEventList(events, timeRangeName))
        } catch (e: Exception) {
            echo("Error: ${e.message}", err = true)
            throw e
        }
    }
}

fun buildCli() = CalendarAnalyzerCommand()
    .subcommands(
        AnalyzeCommand(),
        ListEventsCommand(),
        ConfigCommand().subcommands(
            ShowConfigCommand(),
            NeverMoveCommand(),
            AllowMoveCommand()
        )
    )
