package com.sober.core.domain.usecase

import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MatchNotificationKeywordUseCaseTest {

    private val useCase = MatchNotificationKeywordUseCase()

    private fun keywordRule(
        value: String,
        enabled: Boolean = true,
        id: String = value
    ) = CustomRule(
        id = id,
        type = CustomRuleType.NOTIFICATION_KEYWORD,
        value = value,
        enabled = enabled
    )

    @Test
    fun `a keyword in the body matches regardless of case`() {
        val match = useCase(
            listOf("Flash SALE today"),
            listOf(keywordRule("sale"))
        )

        assertEquals("sale", match?.value)
    }

    @Test
    fun `a keyword in the title matches`() {
        val match = useCase(
            listOf("Your delivery", "Arriving at 6"),
            listOf(keywordRule("delivery"))
        )

        assertEquals("delivery", match?.value)
    }

    @Test
    fun `no keyword in any field means no match`() {
        assertNull(useCase(listOf("Hey", "Lunch tomorrow?"), listOf(keywordRule("sale"))))
    }

    @Test
    fun `a disabled rule does not match`() {
        assertNull(
            useCase(listOf("Flash SALE"), listOf(keywordRule("sale", enabled = false)))
        )
    }

    @Test
    fun `app rules are ignored by the keyword matcher`() {
        val appRule = CustomRule(
            id = "instagram",
            type = CustomRuleType.BLOCKED_APP,
            value = "com.instagram.android"
        )

        assertNull(
            useCase(listOf("com.instagram.android"), listOf(appRule))
        )
    }

    @Test
    fun `a blank keyword matches nothing`() {
        assertNull(useCase(listOf("anything at all"), listOf(keywordRule("   "))))
    }

    @Test
    fun `null and blank texts are skipped`() {
        assertNull(useCase(listOf(null, "", "   "), listOf(keywordRule("sale"))))
    }

    @Test
    fun `whitespace around a keyword is trimmed before matching`() {
        val match = useCase(listOf("Big SALE"), listOf(keywordRule("  sale  ")))

        assertEquals("  sale  ", match?.value)
    }

    @Test
    fun `the first matching rule in list order wins`() {
        val match = useCase(
            listOf("SALE ends tonight"),
            listOf(keywordRule("night"), keywordRule("sale"))
        )

        assertEquals("night", match?.value)
    }
}
