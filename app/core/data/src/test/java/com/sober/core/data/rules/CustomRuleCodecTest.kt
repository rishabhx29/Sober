package com.sober.core.data.rules

import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CustomRuleCodecTest {

    private fun rule(
        value: String = "com.instagram.android",
        type: CustomRuleType = CustomRuleType.BLOCKED_APP,
        enabled: Boolean = true
    ) = CustomRule(id = "rule-1", type = type, value = value, enabled = enabled)

    @Test
    fun `a rule survives a round trip`() {
        val original = rule()

        assertEquals(original, CustomRuleCodec.decode(CustomRuleCodec.encode(original)))
    }

    @Test
    fun `the disabled flag survives a round trip`() {
        val original = rule(enabled = false)

        assertEquals(false, CustomRuleCodec.decode(CustomRuleCodec.encode(original))?.enabled)
    }

    @Test
    fun `a keyword containing the separator survives`() {
        val original = rule(value = "50|50 sale", type = CustomRuleType.NOTIFICATION_KEYWORD)

        assertEquals(original, CustomRuleCodec.decode(CustomRuleCodec.encode(original)))
    }

    @Test
    fun `a record with too few fields is dropped`() {
        assertNull(CustomRuleCodec.decode("rule-1|BLOCKED_APP|1"))
    }

    @Test
    fun `an unknown type is dropped`() {
        assertNull(CustomRuleCodec.decode("rule-1|NOT_A_TYPE|1|com.example"))
    }

    @Test
    fun `an unreadable enabled flag is dropped`() {
        assertNull(CustomRuleCodec.decode("rule-1|BLOCKED_APP|maybe|com.example"))
    }

    @Test
    fun `a blank value is dropped`() {
        assertNull(CustomRuleCodec.decode("rule-1|BLOCKED_APP|1|   "))
    }
}
