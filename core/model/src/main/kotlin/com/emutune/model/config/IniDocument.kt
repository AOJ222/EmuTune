package com.emutune.model.config

/**
 * A parsed INI document: ordered sections, each with ordered key/value entries.
 * Emulators (notably Dolphin) persist their configuration as INI files; this parser
 * turns that text into a structured, inspectable form so current settings can be read
 * and diffed without stringly-typed access.
 */
data class IniDocument(
    val sections: List<IniSection>,
) {
    fun section(name: String): IniSection? = sections.firstOrNull { it.name == name }

    /** Look up a single value by section + key, or null if absent. */
    fun value(section: String, key: String): String? =
        section(section)?.value(key)
}

data class IniSection(
    val name: String,
    val entries: List<IniEntry>,
) {
    fun value(key: String): String? = entries.firstOrNull { it.key == key }?.value
}

data class IniEntry(
    val key: String,
    val value: String,
)

/**
 * A deliberately small INI parser. It handles the subset emulator configs actually use:
 * `[Section]` headers, `Key = Value` (and `Key=Value`), full-line `;`/`#` comments, and
 * blank lines. It does not support line continuations or multi-line values — unknown
 * constructs fail open (the line is skipped) rather than corrupting neighbours.
 */
object IniParser {

    fun parse(text: String): IniDocument {
        val sections = mutableListOf<IniSection>()
        var currentName: String? = null
        var currentEntries = mutableListOf<IniEntry>()

        fun flush() {
            currentName?.let { sections += IniSection(it, currentEntries.toList()) }
        }

        for (rawLine in text.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue

            if (line.startsWith("[") && line.endsWith("]")) {
                flush()
                currentName = line.substring(1, line.length - 1).trim()
                currentEntries = mutableListOf()
                continue
            }

            val eq = line.indexOf('=')
            if (eq <= 0) continue // no key, or key before '=' is empty

            val key = line.substring(0, eq).trim()
            val value = line.substring(eq + 1).trim().trim('"').trim('\'')
            if (key.isEmpty()) continue

            currentEntries += IniEntry(key, value)
        }
        flush()

        return IniDocument(sections)
    }
}
