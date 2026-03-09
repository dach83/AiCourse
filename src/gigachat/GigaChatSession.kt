package gigachat

import kotlinx.serialization.json.JsonElement

class GigaChatSession {
    var model: GigaChatModel = GigaChatModel.LITE
    var temperature: Double = 0.87
    var systemPrompt: String? = null

    private val history = mutableListOf<Message>()

    fun historySize(): Int = history.size

    fun revertHistory(toSize: Int) {
        while (history.size > toSize) history.removeAt(history.lastIndex)
    }

    fun addUserMessage(content: String) = history.add(Message("user", content))

    fun addAssistantMessage(content: String) = history.add(Message("assistant", content))

    /** Добавляет сообщение ассистента с вызовом функции (finish_reason=function_call). */
    fun addAssistantFunctionCall(content: String?, name: String, arguments: JsonElement) =
        history.add(Message("assistant", content, functionCall = FunctionCall(name, arguments)))

    /** Добавляет результат выполнения функции (role=function). */
    fun addFunctionResult(name: String, result: String) =
        history.add(Message("function", result, name = name))

    /** Returns the full message list for the API: optional system prompt + conversation history. */
    fun buildMessages(): List<Message> = buildList {
        systemPrompt?.let { add(Message("system", it)) }
        addAll(history)
    }

    fun header(): String = "Model: ${model.displayName}, Temp:${temperature}"
}

data class Message(
    val role: String,
    val content: String?,
    val name: String? = null,
    val functionCall: FunctionCall? = null,
)

data class FunctionCall(val name: String, val arguments: JsonElement)

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