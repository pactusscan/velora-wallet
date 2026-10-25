package com.andrutstudio.velora.presentation.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguagesTest {

    @Test
    fun `region tags compare by language`() {
        assertEquals("pt", languageOf("pt-BR"))
        assertEquals("zh", languageOf("zh-CN"))
        assertEquals("en", languageOf("en"))
    }

    @Test
    fun `Indonesian is matched as in or id`() {
        assertEquals("in", languageOf("in"))
        assertEquals("in", languageOf("id"))
        assertEquals("in", languageOf("id-ID"))
    }

    @Test
    fun `every app language has a distinct language part`() {
        val languages = AppLanguages.map { languageOf(it.first) }
        assertEquals(languages.size, languages.toSet().size)
    }

    @Test
    fun `locales config lists every app language`() {
        val config = java.io.File("src/main/res/xml/locales_config.xml").readText()
        AppLanguages.forEach { (tag, _) ->
            assertTrue("missing $tag", config.contains("android:name=\"$tag\""))
        }
    }
}
