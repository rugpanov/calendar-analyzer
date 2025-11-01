package dev.grigri.llm

import dev.grigri.analysis.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class OptimizationContext(
    val reschedulableMeetings: List<MeetingInfo>,
    val fixedMeetings: List<MeetingInfo>,
    val freeBlocks: List<FreeBlockInfo>,
    val fragmentationScore: Double
)

@Serializable
data class MeetingInfo(
    val summary: String,
    val start: String,
    val end: String,
    val durationMinutes: Long,
    val participants: Int
)

@Serializable
data class FreeBlockInfo(
    val start: String,
    val end: String,
    val durationMinutes: Long
)

data class OptimizationSuggestion(
    val suggestions: List<String>,
    val rawResponse: String
)

class FocusTimeOptimizer(
    private val koogClient: KoogClient,
    private val minFocusBlockMinutes: Int = 180, // 3 hours
    private val maxFocusBlockMinutes: Int = 240  // 4 hours
) {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
    private val json = Json { prettyPrint = true }

    suspend fun generateSuggestions(stats: MeetingStats): OptimizationSuggestion {
        val context = buildContext(stats)
        val systemPrompt = buildSystemPrompt()
        val userMessage = buildUserMessage(context)

        val response = koogClient.chat(systemPrompt, userMessage)
        val suggestions = parseResponse(response)

        return OptimizationSuggestion(suggestions, response)
    }

    private fun buildContext(stats: MeetingStats): OptimizationContext {
        return OptimizationContext(
            reschedulableMeetings = stats.reschedulableMeetings.map { meeting ->
                MeetingInfo(
                    summary = meeting.event.summary ?: "Untitled",
                    start = meeting.start.format(dateFormatter),
                    end = meeting.end.format(dateFormatter),
                    durationMinutes = meeting.durationMinutes,
                    participants = meeting.participantCount
                )
            },
            fixedMeetings = stats.fixedMeetings.map { meeting ->
                MeetingInfo(
                    summary = meeting.event.summary ?: "Untitled",
                    start = meeting.start.format(dateFormatter),
                    end = meeting.end.format(dateFormatter),
                    durationMinutes = meeting.durationMinutes,
                    participants = meeting.participantCount
                )
            },
            freeBlocks = stats.longestFreeBlocks.map { block ->
                FreeBlockInfo(
                    start = block.start.format(dateFormatter),
                    end = block.end.format(dateFormatter),
                    durationMinutes = block.durationMinutes
                )
            },
            fragmentationScore = stats.fragmentationScore
        )
    }

    private fun buildSystemPrompt(): String {
        return """
            You are a calendar optimization assistant. Your goal is to help create focus time blocks of $minFocusBlockMinutes-$maxFocusBlockMinutes minutes.

            Rules:
            1. Only suggest moving reschedulable meetings (≤ 3 participants)
            2. Fixed meetings (> 3 participants or marked as "never move") CANNOT be moved
            3. Provide specific, actionable suggestions with exact times
            4. Format each suggestion on a new line starting with "• "
            5. Each suggestion should specify: meeting name, current time, proposed time, and the focus block it creates
            6. Consider the fragmentation score - higher scores mean more short gaps that reduce productivity
            7. Prioritize creating longer contiguous free blocks over reducing fragmentation
            8. Be concise and direct

            Example format:
            • Move "Team Sync" from Tuesday 10:00 to Tuesday 15:00 to create a 10:00-13:30 focus block
            • Combine "1:1 with Alice" (Monday 11:00) and "Project Review" (Monday 14:00) to Monday 16:00-17:00 to create a 09:00-12:00 focus block
        """.trimIndent()
    }

    private fun buildUserMessage(context: OptimizationContext): String {
        val jsonContext = json.encodeToString(context)
        return """
            Analyze this calendar and suggest specific meeting reschedules to create $minFocusBlockMinutes-$maxFocusBlockMinutes minute focus blocks.

            Calendar data:
            $jsonContext

            Provide 3-5 concrete, actionable suggestions. Each suggestion should specify exactly which meeting to move, from when, to when, and what focus block this creates.
        """.trimIndent()
    }

    private fun parseResponse(response: String): List<String> {
        return response.lines()
            .filter { it.trim().startsWith("•") || it.trim().startsWith("-") || it.trim().startsWith("*") }
            .map { it.trim().removePrefix("•").removePrefix("-").removePrefix("*").trim() }
            .filter { it.isNotEmpty() }
    }
}
