package dev.grigri.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class Config(
    val neverMoveEventIds: Set<String> = emptySet(),
    val workDayStart: Int = 9,
    val workDayEnd: Int = 18,
    val reschedulableThreshold: Int = 3,
    val minFocusBlockMinutes: Int = 180,
    val maxFocusBlockMinutes: Int = 240,
    val openAIApiKey: String? = null
)

object ConfigManager {
    private val configDir = File(System.getProperty("user.home"), ".calendar-analyzer")
    private val configFile = File(configDir, "config.json")
    private val json = Json { prettyPrint = true }

    init {
        if (!configDir.exists()) {
            configDir.mkdirs()
        }
    }

    fun load(): Config {
        return if (configFile.exists()) {
            try {
                json.decodeFromString<Config>(configFile.readText())
            } catch (e: Exception) {
                println("Warning: Failed to load config, using defaults: ${e.message}")
                Config()
            }
        } else {
            Config()
        }
    }

    fun save(config: Config) {
        configFile.writeText(json.encodeToString(config))
    }

    fun addNeverMoveEvent(eventId: String) {
        val config = load()
        save(config.copy(neverMoveEventIds = config.neverMoveEventIds + eventId))
    }

    fun removeNeverMoveEvent(eventId: String) {
        val config = load()
        save(config.copy(neverMoveEventIds = config.neverMoveEventIds - eventId))
    }

    fun getKoogApiKey(): String {
        val config = load()
        return config.openAIApiKey
            ?: System.getenv("KOOG_API_KEY")
            ?: throw IllegalStateException(
                "KOOG_API_KEY not found. Please either:\n" +
                "  1. Set KOOG_API_KEY environment variable, or\n" +
                "  2. Add it to config.json at: ${configFile.absolutePath}"
            )
    }
}
