package com.sober.core.data.rules

import com.sober.core.domain.model.DetectionType
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DetectionRuleCatalogParserTest {

    private fun parse(json: String) =
        DetectionRuleCatalogParser.parse(Buffer().writeUtf8(json))

    @Test
    fun `the bundled catalog parses and covers every supported app`() {
        val file = File("src/main/assets/detection_rules.json")
        val catalog = parse(file.readText())

        assertEquals(1, catalog.version)
        assertEquals(
            setOf(
                "com.instagram.android",
                "com.google.android.youtube",
                "com.zhiliaoapp.musically",
                "com.twitter.android",
                "com.reddit.frontpage",
                "com.snapchat.android"
            ),
            catalog.rules.map { it.packageName }.toSet()
        )
        // Instagram and YouTube are the two apps 3.3/3.4 name explicitly.
        assertEquals(
            setOf("reels", "explore"),
            catalog.rules.filter { it.packageName == "com.instagram.android" }
                .map { it.sectionId }.toSet()
        )
        assertEquals(
            setOf("shorts", "home"),
            catalog.rules.filter { it.packageName == "com.google.android.youtube" }
                .map { it.sectionId }.toSet()
        )
        assertTrue(catalog.rules.all { it.overlayTitle.isNotBlank() })
        assertTrue(catalog.rules.all { it.detectionType == DetectionType.SELECTED_TAB })
    }

    @Test
    fun `unknown fields are skipped rather than failing the document`() {
        val catalog = parse(
            """
            {
              "_comment": "not part of the schema",
              "version": 4,
              "apps": [{
                "packageName": "com.example.app",
                "appName": "Example",
                "extra": { "nested": [1, 2, 3] },
                "sections": [{
                  "id": "feed",
                  "name": "Feed",
                  "match": "SELECTED_TAB",
                  "value": "com.example.app:id/feed_tab",
                  "futureField": true
                }]
              }]
            }
            """.trimIndent()
        )

        assertEquals(4, catalog.version)
        assertEquals(1, catalog.rules.size)
        assertEquals("Example", catalog.rules.first().appName)
    }

    @Test
    fun `an unusable section is skipped without losing its siblings`() {
        val catalog = parse(
            """
            {
              "version": 1,
              "apps": [{
                "packageName": "com.example.app",
                "appName": "Example",
                "sections": [
                  { "id": "bad-type", "name": "Bad", "match": "WIDGET", "value": "x" },
                  { "id": "blank", "name": "Blank", "match": "TEXT", "value": "  " },
                  { "id": "good", "name": "Good", "match": "TEXT", "value": "Feed" }
                ]
              }]
            }
            """.trimIndent()
        )

        assertEquals(listOf("com.example.app:good"), catalog.rules.map { it.id })
    }

    @Test
    fun `duplicate ids keep the first rule`() {
        val catalog = parse(
            """
            {
              "version": 1,
              "apps": [{
                "packageName": "com.example.app",
                "appName": "Example",
                "sections": [
                  { "id": "feed", "name": "First", "match": "TEXT", "value": "A", "title": "First title" },
                  { "id": "feed", "name": "Second", "match": "TEXT", "value": "B", "title": "Second title" }
                ]
              }]
            }
            """.trimIndent()
        )

        assertEquals(1, catalog.rules.size)
        assertEquals("First title", catalog.rules.first().overlayTitle)
    }

    @Test
    fun `a document without a version is rejected`() {
        val thrown = runCatching {
            parse("""{"apps":[]}""")
        }.exceptionOrNull()

        assertTrue(thrown is IllegalArgumentException)
    }

    @Test
    fun `an app with a malformed package name is dropped`() {
        val catalog = parse(
            """
            {
              "version": 1,
              "apps": [
                { "packageName": "not a package", "sections": [
                    { "id": "x", "name": "X", "match": "TEXT", "value": "X" }] },
                { "packageName": "com.ok.app", "sections": [
                    { "id": "x", "name": "X", "match": "TEXT", "value": "X" }] }
              ]
            }
            """.trimIndent()
        )

        assertEquals(listOf("com.ok.app:x"), catalog.rules.map { it.id })
    }

    @Test
    fun `missing copy falls back to a readable default`() {
        val catalog = parse(
            """
            {
              "version": 1,
              "apps": [{
                "packageName": "com.example.app",
                "appName": "Example",
                "sections": [{ "id": "feed", "name": "Feed", "match": "TEXT", "value": "Feed" }]
              }]
            }
            """.trimIndent()
        )

        val rule = catalog.rules.single()
        assertEquals("Feed is blocked", rule.overlayTitle)
        assertEquals("You set this rule in Sober.", rule.overlaySubtitle)
    }
}
