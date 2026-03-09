package lesson3

import lesson2.CurrencyTools
import io.ktor.server.cio.CIO
import io.ktor.server.engine.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject

fun main(args: Array<String>): Unit = runBlocking {
    val port = args.firstOrNull()?.toIntOrNull() ?: 3000
    val mcpServer = Server(
        serverInfo = Implementation(
            name = "my-mcp-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = true)
            )
        )
    )

    mcpServer.addTool(
        name = CurrencyTools.NAME,
        description = CurrencyTools.DESCRIPTION,
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("input", CurrencyTools.INPUT_SHEMA)
            }
        )
    ) { request ->
        val args = request.arguments?.get("input")?.jsonObject
        CallToolResult(
            content = listOf(
                io.modelcontextprotocol.kotlin.sdk.types.TextContent(
                    text = CurrencyTools.execute(request.name, args)
                )
            )
        )
    }

    embeddedServer(CIO, host = "127.0.0.1", port = port, module = {
        mcp { mcpServer }
    }).start(wait = true)
}