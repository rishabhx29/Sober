package com.sober.core.domain.model

/**
 * Turns what a user types and what a browser shows into comparable hosts.
 *
 * Both sides go through the same rules, so a rule saved as
 * `https://www.Example.com/feed` matches an address bar showing
 * `example.com/feed` without the stored value having to be re-entered.
 */
object WebsiteAddress {

    /**
     * The domain a rule should store, or null when [raw] is not a host.
     *
     * A bare word is rejected on purpose: without a dot there is no way to tell
     * a domain from a search query, and blocking every page whose address
     * happens to contain it would be far worse than asking for the full site.
     */
    fun normalizeDomain(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.any { it.isWhitespace() } || trimmed.contains('@')) return null

        val lower = trimmed.lowercase()
        // Those schemes are browser or device UI, not websites.
        if (lower.startsWith("chrome:") ||
            lower.startsWith("chrome://") ||
            lower.startsWith("about:") ||
            lower.startsWith("file:")
        ) {
            return null
        }

        val withoutScheme = lower.substringAfter("://", lower)
        val host = withoutScheme
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringBefore(':')
            .removePrefix("www.")
            .trimEnd('.')

        if (host.isEmpty() || !host.contains('.')) return null
        if (!host.all { it.isLetterOrDigit() || it == '.' || it == '-' }) return null
        return host
    }

    /**
     * The host currently in the address bar, or null while it shows a
     * placeholder or a typed search query. Chrome exposes the full URL only
     * when the field is focused; the trimmed host is what a page shows.
     */
    fun hostFromAddressBar(text: String?): String? {
        val trimmed = text?.trim().orEmpty()
        if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return null
        return normalizeDomain(trimmed)
    }

    /** True for the domain itself and every subdomain of it. */
    fun covers(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")
}
