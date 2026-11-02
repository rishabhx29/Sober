package com.sober.core.domain.usecase

import com.sober.core.domain.model.WebsiteAddress
import javax.inject.Inject

/**
 * Returns the blocked domain the browser is showing, or null.
 *
 * The most specific rule wins so the overlay names the rule the user actually
 * wrote when both `example.com` and `news.example.com` are blocked.
 *
 * This only sees what the address bar says. A page that redirects after load,
 * or a browser that hides its toolbar while scrolling, can briefly show a site
 * before the cover lands; it is a speed bump, not a network filter.
 */
class DetectBlockedWebsiteUseCase @Inject constructor() {

    operator fun invoke(addressBarText: String?, blockedDomains: Set<String>): String? {
        val host = WebsiteAddress.hostFromAddressBar(addressBarText) ?: return null
        return blockedDomains
            .asSequence()
            .mapNotNull(WebsiteAddress::normalizeDomain)
            .filter { WebsiteAddress.covers(host, it) }
            .maxByOrNull { it.length }
    }
}
