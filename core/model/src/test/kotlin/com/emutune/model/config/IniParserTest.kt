package com.emutune.model.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class IniParserTest {

    @Test
    fun `parses sections and key values`() {
        val doc = IniParser.parse(
            """
            [Core]
            CPUThread = True
            GPUDeterminismMode = auto

            [Video_Settings]
            InternalResolution = 3
            """.trimIndent(),
        )

        assertEquals(listOf("Core", "Video_Settings"), doc.sections.map { it.name })
        assertEquals("True", doc.value("Core", "CPUThread"))
        assertEquals("auto", doc.value("Core", "GPUDeterminismMode"))
        assertEquals("3", doc.value("Video_Settings", "InternalResolution"))
    }

    @Test
    fun `handles key without spaces around equals`() {
        val doc = IniParser.parse("[Core]\nCPUThread=True")
        assertEquals("True", doc.value("Core", "CPUThread"))
    }

    @Test
    fun `strips quotes from values`() {
        val doc = IniParser.parse("[A]\nkey = \"quoted value\"")
        assertEquals("quoted value", doc.value("A", "key"))
    }

    @Test
    fun `ignores full-line comments and blank lines`() {
        val doc = IniParser.parse(
            """
            # a comment
            ; another comment
            [Core]
            key = value
            """.trimIndent(),
        )
        assertEquals(1, doc.sections.size)
        assertEquals("value", doc.value("Core", "key"))
    }

    @Test
    fun `skips lines without an equals sign`() {
        val doc = IniParser.parse("[Core]\nmalformed-line\nkey = value")
        assertEquals("value", doc.value("Core", "key"))
    }

    @Test
    fun `missing section or key returns null`() {
        val doc = IniParser.parse("[Core]\nkey = value")
        assertNull(doc.value("Nope", "key"))
        assertNull(doc.value("Core", "missing"))
    }

    @Test
    fun `values with equals inside are preserved`() {
        val doc = IniParser.parse("[A]\npath = /a/b=c")
        assertEquals("/a/b=c", doc.value("A", "path"))
    }
}
