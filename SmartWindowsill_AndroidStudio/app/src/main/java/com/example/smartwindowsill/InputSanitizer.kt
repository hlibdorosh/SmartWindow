package com.example.smartwindowsill

object InputSanitizer {

    const val MAX_LENGTH = 60

    // Невидимые и управляющие символы (\n, \r, \t, ASCII 0..31, 127..159, zero-width пробелы)
    private val CONTROL_CHARS = Regex("[\\x00-\\x1F\\x7F-\\x9F\\u200B-\\u200D\\uFEFF]")

    // Символы разметки XML/HTML и экранирования (<, >, ", `, \)
    private val FORBIDDEN_CHARS = Regex("[<>\"`\\\\]")

    private val MULTI_SPACE = Regex("\\s+")

    /**
     * Очищает пользовательский ввод:
     * - Заменяет \n, \r, \t и управляющие символы на пробелы
     * - Удаляет символы тегов < >, двойные кавычки ", обратные кавычки ` и слэши \
     * - Схлопывает повторяющиеся пробелы и делает trim()
     * - Ограничивает длину до [maxLength] (по умолчанию 60 символов)
     */
    fun sanitize(input: String, maxLength: Int = MAX_LENGTH): String {
        if (input.isBlank()) return ""
        val withoutControl = CONTROL_CHARS.replace(input, " ")
        val withoutForbidden = FORBIDDEN_CHARS.replace(withoutControl, "")
        val collapsed = MULTI_SPACE.replace(withoutForbidden, " ").trim()
        return collapsed.take(maxLength)
    }

    /**
     * Фильтр для ввода в реальном времени в полях ввода (OutlinedTextField):
     * не дает ввести запрещенные символы (\n, \t, <, >, ", `, \) и ограничивает длину до [maxLength].
     */
    fun filterInput(input: String, maxLength: Int = MAX_LENGTH): String {
        val filtered = input.filter { c ->
            c != '\n' && c != '\r' && c != '\t' &&
            c != '<' && c != '>' && c != '"' && c != '`' && c != '\\' &&
            !c.isISOControl()
        }
        return filtered.take(maxLength)
    }
}
