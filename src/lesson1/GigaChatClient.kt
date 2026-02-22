package lesson1

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.put
import java.security.cert.X509Certificate
import java.util.UUID
import javax.net.ssl.X509TrustManager
import kotlin.collections.filter

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

    suspend fun chat(session: GigaChatSession): String {
        ensureToken()
        val requestBody = buildJsonObject {
            put("model", session.model.apiName)
            put("temperature", session.temperature)
            put("stream", false)
            put("messages", buildJsonArray {
                for (msg in session.buildMessages()) {
                    add(buildJsonObject {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            })
        }

        val response = http.post(CHAT_URL) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
            contentType(ContentType.Application.Json)
            setBody(requestBody.toString())
        }

        val responseBody = json.parseToJsonElement(response.bodyAsText()).jsonObject

        if (!response.status.isSuccess()) {
            throw RuntimeException("API error ${response.status.value}: ${response.bodyAsText()}")
        }


        return responseBody["choices"]!!
            .jsonArray[0].jsonObject["message"]!!
            .jsonObject["content"]!!.jsonPrimitive.content
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