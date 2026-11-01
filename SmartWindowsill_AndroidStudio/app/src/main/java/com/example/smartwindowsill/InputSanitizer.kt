package com.example.smartwindowsill

object InputSanitizer {

    private const val DEFAULT_MAX_LENGTH = 100

    // Невидимые и управляющие символы (\n, \r, \t, ASCII 0..31, 127..159, zero-width пробелы, bidi-маркеры)
    private val CONTROL_AND_INVISIBLE_CHARS = Regex(
        "[\\x00-\\x1F\\x7F-\\x9F\\u200B-\\u200D\\uFEFF\\u202A-\\u202E\\u2066-\\u2069]"
    )

    // Символы разметки XML/HTML/JSON и экранирования, предотвращающие побег из тегов в промпте и инъекции
    private val TAG_AND_ESCAPE_CHARS = Regex("[<>{}\"';`\\\\]")

    // Паттерны SQL-инъекций (комментарии, цепочки выражений, инъекции условий и деструктивные команды)
    private val SQL_INJECTION_PATTERNS = listOf(
        Regex("(?i)--+"),
        Regex("(?i)/\\*.*?\\*/"),
        Regex("(?i)\\b(or|and)\\s+['\"0-9a-z_]+\\s*=\\s*['\"0-9a-z_]+"),
        Regex("(?i)\\b(drop\\s+table|drop\\s+database|delete\\s+from|insert\\s+into|update\\s+\\w+\\s+set|union\\s+(all\\s+)?select|alter\\s+table|truncate\\s+table|exec(\\s+xp_)?)\\b")
    )

    // Паттерны попыток обхода/взлома промпта (Prompt Injection / Jailbreak)
    private val PROMPT_INJECTION_PATTERNS = listOf(
        // Английские варианты
        Regex("(?iU)\\bignore\\s+(all\\s+)?(previous|prior|above|system|your)?\\s*(instructions?|prompts?|rules?|commands?|directives?)\\b"),
        Regex("(?iU)\\bdisregard\\s+(all\\s+)?(previous|prior|above|your)?\\s*(instructions?|prompts?|rules?)\\b"),
        Regex("(?iU)\\bforget\\s+(all\\s+)?(everything|previous|prior|your)?\\s*(instructions?|prompts?|rules?)?\\b"),
        Regex("(?iU)\\b(you\\s+are\\s+now|act\\s+as(\\s+a)?|pretend\\s+to\\s+be|roleplay\\s+as|switch\\s+to)\\b"),
        Regex("(?iU)\\b(system\\s*prompt|system\\s*instructions?|jailbreak|developer\\s*mode|dan\\s*mode)\\b"),
        Regex("(?iU)\\b(override\\s+(all\\s+)?(rules?|instructions?)|new\\s+instructions?)\\b"),
        Regex("(?iU)\\b(bypass\\s+(safety|filters?|rules?)|do\\s+anything\\s+now)\\b"),
        // Русские варианты (флаг (?iU) обеспечивает поддержку Юникода для границы слова \b и кириллицы)
        Regex("(?iU)\\bигнорируй(те)?\\s+(все\\s+)?(предыдущие|прошлые|системные|свой|свои)?\\s*(инструкции|правила|промпт|указания|команды)?\\b"),
        Regex("(?iU)\\bзабудь(те)?\\s+(все\\s+)?(предыдущие|прошлые)?\\s*(инструкции|правила|промпт|обо\\s+всем)?\\b"),
        Regex("(?iU)\\b(действуй\\s+как|притворись|ты\\s+теперь|играй\\s+роль|переключись\\s+в)\\b"),
        Regex("(?iU)\\b(системный\\s*промпт|системные\\s*инструкции|режим\\s*разработчика)\\b"),
        Regex("(?iU)\\b(обойди\\s+(правила|защиту|фильтры)|новые\\s+инструкции)\\b")
    )

    private val MULTI_SPACE_REGEX = Regex("\\s+")

    /**
     * Комплексная очистка пользовательского ввода:
     * 1. Вырезает \n, \r, \t и любые скрытые управляющие символы.
     * 2. Удаляет спецсимволы тегов и кавычек, защищая от закрытия XML-тегов в Claude.
     * 3. Очищает от SQL-инъекций (DROP, UNION, комментарии и OR 1=1).
     * 4. Удаляет фразы попыток взлома промпта (Prompt Breaking / Jailbreak).
     * 5. Схлопывает пробелы и обрезает длину до maxLength.
     */
    fun sanitize(input: String, maxLength: Int = DEFAULT_MAX_LENGTH): String {
        if (input.isBlank()) return ""

        var cleaned = input

        // 1. Убираем управляющие символы (\n, \r, \t и др.)
        cleaned = CONTROL_AND_INVISIBLE_CHARS.replace(cleaned, " ")

        // 2. Убираем опасные символы разметки и кавычек
        cleaned = TAG_AND_ESCAPE_CHARS.replace(cleaned, " ")

        // 3. Удаляем SQL injection паттерны
        for (pattern in SQL_INJECTION_PATTERNS) {
            cleaned = pattern.replace(cleaned, " ")
        }

        // 4. Удаляем Prompt injection паттерны
        for (pattern in PROMPT_INJECTION_PATTERNS) {
            cleaned = pattern.replace(cleaned, " ")
        }

        // 5. Схлопываем лишние пробелы и триммим
        cleaned = MULTI_SPACE_REGEX.replace(cleaned, " ").trim()

        return if (cleaned.length > maxLength) cleaned.substring(0, maxLength).trim() else cleaned
    }

    /**
     * Очистка названия растения для отправки в Claude (до 60 символов).
     */
    fun sanitizePlantName(name: String): String = sanitize(name, maxLength = 60)

    /**
     * Очистка никнейма растения для локальной БД (до 40 символов).
     */
    fun sanitizeNickname(nickname: String): String = sanitize(nickname, maxLength = 40)
}
