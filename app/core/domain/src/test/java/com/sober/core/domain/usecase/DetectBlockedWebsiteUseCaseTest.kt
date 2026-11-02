package com.sober.core.domain.usecase

import com.sober.core.domain.model.WebsiteAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetectBlockedWebsiteUseCaseTest {

    private val detect = DetectBlockedWebsiteUseCase()

    @Test
    fun `a blocked domain is matched in the address bar`() {
        assertEquals(
            "example.com",
            detect("example.com", setOf("example.com"))
        )
    }

    @Test
    fun `subdomains of a blocked domain are covered`() {
        assertEquals(
            "example.com",
            detect("news.example.com", setOf("example.com"))
        )
    }

    @Test
    fun `a path in the address bar does not stop the match`() {
        assertEquals(
            "example.com",
            detect("example.com/feed?tab=1", setOf("example.com"))
        )
    }

    @Test
    fun `www is ignored on both sides`() {
        assertEquals(
            "example.com",
            detect("www.example.com", setOf("https://www.example.com/"))
        )
    }

    @Test
    fun `a lookalike suffix is not a subdomain`() {
        assertNull(detect("notexample.com", setOf("example.com")))
    }

    @Test
    fun `an unblocked site passes`() {
        assertNull(detect("example.org", setOf("example.com")))
    }

    @Test
    fun `the placeholder shown on a new tab is not a host`() {
        assertNull(detect("Search or type URL", setOf("example.com")))
    }

    @Test
    fun `a typed search query is not a host`() {
        assertNull(detect("weather today", setOf("example.com")))
    }

    @Test
    fun `an empty address bar is not a host`() {
        assertNull(detect(null, setOf("example.com")))
        assertNull(detect("", setOf("example.com")))
    }

    @Test
    fun `the most specific blocked domain is reported`() {
        assertEquals(
            "news.example.com",
            detect("news.example.com", setOf("example.com", "news.example.com"))
        )
    }

    @Test
    fun `browser ui is never stored as a domain`() {
        assertNull(WebsiteAddress.normalizeDomain("chrome://settings"))
        assertNull(WebsiteAddress.normalizeDomain("about:blank"))
        assertNull(WebsiteAddress.normalizeDomain("localhost"))
    }

    @Test
    fun `a bare word is rejected because it could be a search`() {
        assertNull(WebsiteAddress.normalizeDomain("instagram"))
    }

    @Test
    fun `a full url is reduced to its host`() {
        assertEquals(
            "youtube.com",
            WebsiteAddress.normalizeDomain("https://www.youtube.com/watch?v=abc")
        )
    }

    @Test
    fun `a subdomain stays part of the rule the user wrote`() {
        assertEquals(
            "m.youtube.com",
            WebsiteAddress.normalizeDomain("https://m.youtube.com/watch?v=abc")
        )
    }
}
