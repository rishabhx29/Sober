package com.sober.core.data.repository

import android.content.Context
import com.sober.core.data.local.datastore.SoberPreferences
import com.sober.core.data.rules.DetectionRuleCatalogParser
import com.sober.core.domain.repository.DetectionRuleCatalog
import com.sober.core.domain.repository.DetectionRuleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okio.Buffer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the bundled rules document, with the last successfully parsed copy kept
 * in DataStore as a fallback.
 *
 * The fallback matters when the asset itself cannot be read (a damaged install,
 * a stripped split APK), not for a bad edit: an invalid document is rejected
 * and the previous good copy is used instead. There is no network fetch — the
 * app has no INTERNET permission — so "newer rules" arrive with an app update.
 */
@Singleton
class DetectionRuleRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: SoberPreferences
) : DetectionRuleRepository {

    @Volatile
    private var cached: DetectionRuleCatalog? = null

    override suspend fun load(): DetectionRuleCatalog = withContext(Dispatchers.IO) {
        cached?.let { return@withContext it }

        val fresh = runCatching {
            context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        }.getOrNull()?.let { raw ->
            parse(raw)?.also { catalog ->
                runCatching { preferences.setDetectionRulesJson(raw) }
                cached = catalog
            }
        }
        if (fresh != null) return@withContext fresh

        val fallback = runCatching { preferences.detectionRulesJson.first() }
            .getOrNull()
            ?.let(::parse)
            ?.also { cached = it }

        fallback ?: DetectionRuleCatalog.EMPTY
    }

    /**
     * An empty catalog is treated as a failure: it would silently switch off
     * every section toggle the user had enabled.
     */
    private fun parse(raw: String): DetectionRuleCatalog? = runCatching {
        Buffer().writeUtf8(raw).let(DetectionRuleCatalogParser::parse)
    }.getOrNull()?.takeIf { it.rules.isNotEmpty() }

    private companion object {
        const val ASSET_NAME = "detection_rules.json"
    }
}
