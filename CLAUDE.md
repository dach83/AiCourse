# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kotlin learning/sandbox project. 

- **Lesson 1** (`gigachat`) — CLI chat with GigaChat API, stores dialog history, supports runtime parameter changes, function calling (convert_currency).

## Build & Run

The project uses **Gradle** (Kotlin DSL). Open in IntelliJ IDEA and let it sync, or use a locally installed Gradle.

```sh
# Run gigachat (requires credentials.properties in project root)
./gradlew run

# Build fat jar
./gradlew jar
```

If no Gradle wrapper exists yet, generate it with:
```sh
gradle wrapper
```

## Tech Stack

- **Language:** Kotlin (JVM 17)
- **HTTP client:** Ktor CIO (`ktor-client-cio`)
- **JSON:** `kotlinx-serialization-json` (JSON DSL — no `@Serializable` codegen needed)
- **GigaChat API docs:** https://developers.sber.ru/docs/ru/gigachat/guides/using-sdks
- **Logging:** `slf4j-simple` (suppresses Ktor internal noise)

## Credentials

GigaChat auth key lives in `credentials.properties` at the project root (gitignored):

```
auth_key=<Base64-encoded client_id:client_secret>
```

`loadAuthKey()` in `Main.kt` reads it at startup.

## Architecture — Lesson 1

```
Main.kt            — entry point
GigaChatCLI.kt     — REPL loop, command parsing (/model, /temp, /system, /quit)
GigaChatSession.kt — holds conversation history + model/temperature/systemPrompt
                     buildMessages() injects system prompt as the first message each call
GigaChatClient.kt  — Ktor HTTP client; caches OAuth token, refreshes 60 s before expiry
                     chat() implements function-calling loop (finish_reason=function_call)
CurrencyTools.kt   — function definition JSON schema + execute() stub (1 USD = 78 RUB)
```

SSL verification is disabled in `GigaChatClient` because GigaChat uses a Russian national CA not trusted by the default JVM trust store.

GigaChat model name mapping:

| Command arg | API model name |
|-------------|---------------|
| `Lite`      | `GigaChat`    |
| `Pro`       | `GigaChat-Pro`|
| `Max`       | `GigaChat-Max`|

- Обновляй CLAUDE.md при изменении файлов проекта