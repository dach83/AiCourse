package lesson1

class GigaChatSession {
    var model: GigaChatModel = GigaChatModel.LITE
    var temperature: Double = 0.87
    var systemPrompt: String? = null

    private val history = mutableListOf<Message>()

    fun addUserMessage(content: String) = history.add(Message("user", content))

    fun addAssistantMessage(content: String) = history.add(Message("assistant", content))

    fun removeLastMessage() {
        if (history.isNotEmpty()) history.removeAt(history.lastIndex)
    }

    /** Returns the full message list for the API: optional system prompt + conversation history. */
    fun buildMessages(): List<Message> = buildList {
        systemPrompt?.let { add(Message("system", it)) }
        addAll(history)
    }

    fun header(): String = "Model: ${model.displayName}, Temp:${temperature}"
}

data class Message(val role: String, val content: String)

enum class GigaChatModel(val displayName: String, val apiName: String) {
    LITE("Lite", "GigaChat"),
    PRO("Pro", "GigaChat-Pro"),
    MAX("Max", "GigaChat-Max");

    companion object {
        fun parse(input: String): GigaChatModel =
            entries.find { it.displayName.equals(input, ignoreCase = true) }
                ?: entries.find { it.apiName.equals(input, ignoreCase = true) }
                ?: LITE
    }
}
