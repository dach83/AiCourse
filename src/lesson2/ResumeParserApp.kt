package lesson2

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import lesson1.GigaChatClient
import lesson1.GigaChatSession
import java.io.File
import java.util.Properties

data class ResumeData(
    val name: String,
    val skills: List<String>,
    val experienceYears: Int
)

class ResumeParserApp {

    fun run() = runBlocking {
        val client = GigaChatClient(loadAuthKey())
        val session = GigaChatSession().apply {
            systemPrompt = """
                Верни ТОЛЬКО JSON без пояснений. Формат: {"name": "...", "skills": ["skill1", "skill2"], "experience_years": N}
            """.trimIndent()
        }

        session.addUserMessage(content = RESUME)

        println("Отправляю резюме в GigaChat...")
        val rawResponse = client.chat(session)

        println("Ответ модели:\n$rawResponse\n")

        val resume = parseResume(rawResponse)
        printResume(resume)

        client.close()
    }

    private fun parseResume(response: String): ResumeData {
        val jsonText = response
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = Json { ignoreUnknownKeys = true }
        val obj = json.parseToJsonElement(jsonText).jsonObject

        val name = obj["name"]!!.jsonPrimitive.content
        val skills = obj["skills"]!!.jsonArray.map { element ->
            when {
                element is JsonPrimitive -> element.content
                element is JsonObject -> element.entries
                    .filter { it.key != "experience_level" }
                    .joinToString(", ") { it.value.jsonPrimitive.content }
                else -> element.toString()
            }
        }
        val experienceYears = obj["experience_years"]!!.jsonPrimitive.int

        return ResumeData(name, skills, experienceYears)
    }

    private fun printResume(resume: ResumeData) {
        println("=== Распарсенное резюме ===")
        println("Имя:              ${resume.name}")
        println("Лет опыта:        ${resume.experienceYears}")
        println("Навыки:")
        resume.skills.forEach { println("  - $it") }
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

    companion object {
        private val RESUME = """
            Иван Петров

            Senior Android Developer

            Телефон: +7 (999) 123-45-67
            Email: ivan.petrov@email.com
            Город: Москва (готов к переезду/удаленной работе)
            GitHub: github.com/ivanpetrov
            LinkedIn: linkedin.com/in/ivanpetrov
            Telegram: @ivan_dev

            О себе

            Senior Android-разработчик с 7+ годами опыта создания высоконагруженных приложений с миллионной аудиторией. Специализируюсь на чистой архитектуре, оптимизации производительности и наставничестве. Ищу интересные долгосрочные проекты, где могу применить свой опыт для создания качественного продукта и развития команды.

            Навыки

            Языки: Kotlin, Java, Coroutines & Flow.
            Архитектура: Clean Architecture, MVVM, MVI, Multi-module проекты.
            Android SDK: Jetpack Compose, XML, Navigation Component, WorkManager, Lifecycle, Paging 3, Room, DataStore.
            DI: Dagger 2, Hilt, Koin.
            Работа с сетью: Retrofit, OkHttp, GraphQL.
            Тестирование: JUnit, Mockito, Espresso, Robolectric.
            Инструменты: Git (GitFlow), Firebase (Crashlytics, Analytics, Remote Config), Gradle (KTS), CI/CD (GitHub Actions, Jenkins).
            Другое: RxJava, Kotlin Multiplatform (базовый опыт).

            Опыт работы

            Сентябрь 2021 — Настоящее время (Senior Android Developer)
            Компания: "BigTech Solutions", Москва
            Проект: Мобильный банкинг (MAU — 5 млн+)

            Переписал легаси-модули с Java на Kotlin и внедрил Clean Architecture, что ускорило разработку новых фич на 30%.
            Разработал кастомную систему кэширования на Room + DataStore для офлайн-режима.
            Оптимизировал скорость запуска приложения с 3 секунд до 1.2 секунд (оптимизация Dagger и ленивая инициализация).
            Проводил code review, внедрил статические анализаторы (detekt, ktlint) в CI/CD пайплайн.
            Менторил двух Junior-разработчиков, которые успешно вошли в команду и закрывают коммерческие задачи.

            Июнь 2018 — Август 2021 (Android Developer)
            Компания: "StartupTrip", Санкт-Петербург
            Проект: Сервис для путешествий (B2C)

            Разработал приложение "с нуля" до публикации в Google Play (текущий рейтинг 4.7).
            Интегрировал карты (Yandex Maps) и геолокацию.
            Настроил Firebase Push Notifications и Deeplinks для маркетинговых рассылок.
            Участвовал в проектировании API вместе с бэкенд-командой.
            Использовал Jetpack Compose для построения современных UI-компонентов.

            Сентябрь 2016 — Май 2018 (Junior Android Developer)
            Компания: "WebStudio", Москва

            Поддержка корпоративных приложений.
            Верстка экранов на XML, исправление багов.
            Работа с SQLite и Content Providers.

            Образование

            2012 — 2016 (Бакалавр)
            Московский государственный технический университет им. Н.Э. Баумана
            Факультет: Информатика и системы управления (Программная инженерия)

            Сертификаты и курсы

            Google Associate Android Developer (Сертификат выдан: 2022)
            Kotlin Coroutines и Flow (Stepik / OTUS, 2021)
            Английский язык: Upper-Intermediate (B2) (чтение технической документации, переписка, переговоры).

            Достижения (Pet-проекты)

            Open Source Contributor: Внес правки в документацию библиотеки Coil (библиотека для загрузки изображений).
            Pet-проект "WeatherApp": Приложение погоды на Ktor клиенте + Jetpack Compose. Исходники выложены на GitHub.
        """.trimIndent()
    }
}