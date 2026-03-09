package gigachat

import kotlinx.coroutines.runBlocking
import lesson2.CurrencyTools
import java.io.File
import java.util.*

class GigaChatCLI {

    fun run() = runBlocking {
        val client = GigaChatClient(loadAuthKey())
        val session = GigaChatSession()

        println("GigaChat CLI Chat")
        println(session.header())
        println()

        client.models()

        while (true) {
            print("You: ")
            val line = readlnOrNull()?.trim() ?: break

            when {
                line == "/quit" -> break
                line == "/help" -> showHelp()
                line.startsWith("/model ") -> changeModel(session, line)
                line.startsWith("/temp ") -> changeTemperature(session, line)
                line.startsWith("/system ") -> changeSystemPrompt(session, line)
                line.isNotEmpty() -> sendMessage(session, line, client)
            }
        }

        client.close()
        println("Goodbye!")
    }

    private suspend fun sendMessage(session: GigaChatSession, line: String, client: GigaChatClient) {
        val checkpoint = session.historySize()
        session.addUserMessage(line)
        runCatching {
            client.chat(session, listOf(CurrencyTools.functionDef), CurrencyTools::execute)
        }
            .onSuccess { reply ->
                session.addAssistantMessage(reply)
                println("GigaChat: $reply")
                println()
            }
            .onFailure { e ->
                session.revertHistory(checkpoint)
                println("Error: ${e.message}")
                println()
            }
    }

    private fun changeSystemPrompt(session: GigaChatSession, line: String) {
        session.systemPrompt = line.removePrefix("/system ").trim()
        println("System prompt updated")
    }

    private fun changeTemperature(session: GigaChatSession, line: String) {
        val value = line.removePrefix("/temp ").trim().toDoubleOrNull()
        if (value != null) {
            session.temperature = value
            println("Temperature set to $value")
        } else {
            println("Invalid temperature value")
        }
    }

    private fun changeModel(session: GigaChatSession, line: String) {
        session.model = GigaChatModel.parse(line.removePrefix("/model ").trim())
        println("Model switched to ${session.model.displayName}")
    }

    private fun showHelp() {
        println(
            """
             Commands:
                  /model <Lite|Pro|Max>  — switch model (history is preserved)
                  /temp <value>          — set temperature (history is preserved)
                  /system <text>         — set system prompt (history is preserved)
                  /quit                  — exit
        """.trimIndent()
        )
    }

    private fun loadAuthKey(): String {
        val file = File("credentials.properties")
        require(file.exists()) {
            "credentials.properties not found. Create it with:\n  auth_key=YOUR_GIGACHAT_KEY"
        }
        val props = Properties().apply { load(file.reader()) }
        return requireNotNull(props.getProperty("auth_key")) {
            "auth_key property is missing in credentials.properties"
        }
    }
}