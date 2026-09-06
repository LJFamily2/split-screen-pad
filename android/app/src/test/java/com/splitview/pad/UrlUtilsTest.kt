package com.splitview.pad

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Address-bar behaviour is the one piece of pane logic with no Android
 * dependencies, so it can be pinned down with plain JVM tests.
 */
class UrlUtilsTest {

    @Test
    fun `full urls are left alone`() {
        assertEquals("https://example.com/a?b=c", UrlUtils.normalize("https://example.com/a?b=c"))
        assertEquals("http://example.com", UrlUtils.normalize("http://example.com"))
    }

    @Test
    fun `bare domains get https`() {
        assertEquals("https://example.com", UrlUtils.normalize("example.com"))
        assertEquals("https://m.youtube.com/watch", UrlUtils.normalize("m.youtube.com/watch"))
        assertEquals("https://localhost:8080", UrlUtils.normalize("localhost:8080"))
    }

    @Test
    fun `plain words become a search`() {
        val result = UrlUtils.normalize("how to split screen")
        assertTrue(result, result.startsWith("https://www.google.com/search?q="))
        assertTrue(result, result.contains("split"))
    }

    @Test
    fun `whitespace is trimmed and empty input falls back`() {
        assertEquals("https://example.com", UrlUtils.normalize("   example.com  "))
        assertEquals(Prefs.DEFAULT_URL_2, UrlUtils.normalize("   "))
    }

    @Test
    fun `prettify strips the noise an address bar does not need`() {
        assertEquals("example.com", UrlUtils.prettify("https://www.example.com/"))
        assertEquals("example.com/a/b", UrlUtils.prettify("http://example.com/a/b"))
    }

    @Test
    fun `host extracts the domain for pair labels`() {
        assertEquals("youtube.com", UrlUtils.host("https://www.youtube.com/watch?v=1"))
        assertEquals("chat.zalo.me", UrlUtils.host("https://chat.zalo.me"))
    }
}
