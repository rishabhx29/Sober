package com.sober.core.domain.usecase

import com.sober.core.domain.model.CustomRule
import com.sober.core.domain.model.CustomRuleType
import javax.inject.Inject

/**
 * Does a notification's text hit one of the user's keyword rules?
 *
 * Split from the listener so the matching rules are testable: the service only
 * has to gather strings and call this.
 *
 * Matching is case-insensitive and substring-based on purpose — the user typed
 * "sale" expecting to catch "Flash SALE" and "sales". A blank keyword matches
 * nothing: after trimming, an empty needle would otherwise hit every
 * notification, which is the worst possible failure mode for a filter.
 */
class MatchNotificationKeywordUseCase @Inject constructor() {

    /** The first enabled rule that matches, or null. */
    operator fun invoke(texts: List<String?>, rules: List<CustomRule>): CustomRule? {
        val keywords = rules.filter {
            it.enabled && it.type == CustomRuleType.NOTIFICATION_KEYWORD && it.value.isNotBlank()
        }
        if (keywords.isEmpty()) return null

        val haystack = texts.mapNotNull { it?.takeIf(String::isNotBlank) }
        if (haystack.isEmpty()) return null

        return keywords.firstOrNull { rule ->
            val needle = rule.value.trim()
            needle.isNotEmpty() && haystack.any { it.contains(needle, ignoreCase = true) }
        }
    }
}
