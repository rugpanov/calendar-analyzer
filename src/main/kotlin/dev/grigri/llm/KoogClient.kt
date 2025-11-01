package dev.grigri.llm

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.openai.OpenAIClientSettings
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import dev.grigri.config.ConfigManager

class KoogClient(
    private val apiKey: String = ConfigManager.getKoogApiKey(),
    private val modelName: String = "gpt-5"
) {
    // Create OpenAI client configured for Koog's API endpoint
    private val llmClient = OpenAILLMClient(
        apiKey = apiKey
    )

    private val executor = SingleLLMPromptExecutor(llmClient = llmClient)

    // Create model instance for Koog
    private val model = LLModel(
        provider = LLMProvider.OpenAI,
        id = modelName,
        capabilities = listOf(
            LLMCapability.Completion,
            LLMCapability.Temperature
        )
    )

    suspend fun chat(systemPrompt: String, userMessage: String): String {
        // Build prompt from messages
        val prompt = Prompt(
            id = "chat-prompt",
            messages = listOf(
                Message.System(systemPrompt),
                Message.User(userMessage)
            )
        )

        // Execute with model
        val response = executor.execute(
            prompt = prompt,
            model = model
        )

        return response
    }

    fun close() {
        // Client cleanup if needed
    }
}
