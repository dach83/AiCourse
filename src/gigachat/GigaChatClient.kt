package gigachat

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.X509TrustManager

class GigaChatClient(private val authKey: String) {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = HttpClient(CIO) {
//        install(Logging) {
//            logger = Logger.DEFAULT
//            level = LogLevel.ALL
//        }
//        install(HttpTimeout) {
//            requestTimeoutMillis = 10000
//        }
        engine {
            https {
                trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit
                    override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) = Unit
                    override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
                }
            }
        }
    }

    private var accessToken: String? = null
    private var tokenExpiresAt: Long = 0L

    private suspend fun ensureToken() {
        val now = System.currentTimeMillis()
        if (accessToken != null && now < tokenExpiresAt - 60_000L) return

        val response = http.post(OAUTH_URL) {
            header(HttpHeaders.Authorization, "Basic $authKey")
            header("RqUID", UUID.randomUUID().toString())
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("scope=GIGACHAT_API_PERS")
        }

        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        accessToken = body["access_token"]!!.jsonPrimitive.content
        tokenExpiresAt = body["expires_at"]!!.jsonPrimitive.long
    }

    /**
     * Отправляет сообщение и возвращает текстовый ответ модели.
     *
     * Если модель возвращает function_call, функция выполняется через [functionExecutor],
     * результат добавляется в историю сессии, и запрос повторяется (цикл завершается
     * при получении обычного текстового ответа).
     *
     * @param functions  список JSON-схем функций, доступных модели
     * @param functionExecutor  (name, argsJson) -> result
     */
    suspend fun chat(
        session: GigaChatSession,
        functions: List<JsonObject> = emptyList(),
        functionExecutor: ((String, String) -> String)? = null,
    ): String {
        ensureToken()

        while (true) {
            val requestBody = buildJsonObject {
                put("model", session.model.apiName)
                put("temperature", session.temperature)
                put("stream", false)
                put("messages", buildJsonArray {
                    for (msg in session.buildMessages()) {
                        add(buildJsonObject {
                            put("role", msg.role)
                            if (msg.content != null) put("content", msg.content) else put("content", JsonNull)
                            msg.name?.let { put("name", it) }
                            msg.functionCall?.let { fc ->
                                put("function_call", buildJsonObject {
                                    put("name", fc.name)
                                    put("arguments", fc.arguments)
                                })
                            }
                        })
                    }
                })
                if (functions.isNotEmpty()) {
                    put("functions", buildJsonArray { functions.forEach { add(it) } })
                    put("function_call", "auto")
                }
            }

            val response = http.post(CHAT_URL) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(requestBody.toString())
            }

            if (!response.status.isSuccess()) {
                throw RuntimeException("API error ${response.status.value}: ${response.bodyAsText()}")
            }

            val responseBody = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val choice = responseBody["choices"]!!.jsonArray[0].jsonObject
            val message = choice["message"]!!.jsonObject
            val finishReason = choice["finish_reason"]?.jsonPrimitive?.content

            if (finishReason == "function_call") {
                val fc = message["function_call"]!!.jsonObject
                val name = fc["name"]!!.jsonPrimitive.content
                val arguments = fc["arguments"]!!
                val assistantContent = message["content"]
                    ?.takeIf { it != JsonNull }
                    ?.jsonPrimitive?.content

                session.addAssistantFunctionCall(assistantContent, name, arguments)

                val result = requireNotNull(functionExecutor) { "Function executor not provided" }
                    .invoke(name, arguments.toString())

                println("[Tool] $name → $result")
                session.addFunctionResult(name, result)
                continue
            }

            val contentElement = message["content"]
                ?: throw RuntimeException("No content in response")
            return if (contentElement == JsonNull) "" else contentElement.jsonPrimitive.content
        }
    }

    suspend fun models(): List<String> {
        ensureToken()
        val response = http.get(MODELS_URL) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            accept(ContentType.Application.Json)
        }

        val body = json.parseToJsonElement(response.bodyAsText()).jsonObject
        return body["data"]!!.jsonArray
            .map { it.jsonObject["id"]!!.jsonPrimitive.content }
    }

    fun close() = http.close()

    companion object {
        private const val OAUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth"
        private const val CHAT_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions"
        private const val MODELS_URL = "https://gigachat.devices.sberbank.ru/api/v1/models"
    }
}