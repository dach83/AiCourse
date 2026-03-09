package lesson2

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.Locale

object CurrencyTools {

    private const val USD_TO_RUB = 78.0
    const val NAME = "convert_currency"
    const val DESCRIPTION = "Конвертирует денежную сумму из одной валюты в другую"
    val INPUT_SHEMA = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("amount", buildJsonObject {
                put("type", "number")
                put("description", "Сумма для конвертации")
            })
            put("from", buildJsonObject {
                put("type", "string")
                put("description", "Исходная валюта, например: USD, RUB")
            })
            put("to", buildJsonObject {
                put("type", "string")
                put("description", "Целевая валюта, например: USD, RUB")
            })
        })
        put("required", buildJsonArray {
            add("amount"); add("from"); add("to")
        })
    }

    /** JSON-схема функции для передачи в GigaChat API. */
    val functionDef: JsonObject = buildJsonObject {
        put("name", NAME)
        put("description", DESCRIPTION)
        put("parameters", INPUT_SHEMA)
    }

    /**
     * Выполняет функцию по имени и JSON-строке аргументов.
     * Возвращает результат в виде строки.
     */
    fun execute(name: String, argsJson: String): String {
        require(name == "convert_currency") { "Unknown function: $name" }
        val args = Json.Default.parseToJsonElement(argsJson).jsonObject
        return execute(name, args)
    }

    fun execute(name: String, args: JsonObject?): String {
        require(name == "convert_currency") { "Unknown function: $name" }
        if (args == null) return ""
        val amount = args["amount"]!!.jsonPrimitive.double
        val from = args["from"]!!.jsonPrimitive.content.uppercase()
        val to = args["to"]!!.jsonPrimitive.content.uppercase()
        return "%.2f".format(Locale.US, convertCurrency(amount, from, to))
    }

    private fun convertCurrency(amount: Double, from: String, to: String): Double = when {
        from == to -> amount
        from == "USD" && to == "RUB" -> amount * USD_TO_RUB
        from == "RUB" && to == "USD" -> amount / USD_TO_RUB
        else -> error("Unsupported pair: $from → $to (only USD↔RUB supported)")
    }
}