package com.sober.core.common.presets

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.telecom.TelecomManager

/** Focus presets from PRD 2.5. Meal Break is not a session and lives in its own use case. */
enum class PresetMode(
    val label: String,
    val durationMinutes: Int,
    val description: String
) {
    SLEEP(label = "Sleep", durationMinutes = 8 * 60, description = "8h · Only calls"),
    STUDY(label = "Study", durationMinutes = 120, description = "2h · Social blocked"),
    WORK(label = "Work", durationMinutes = 240, description = "4h · Strict"),
    GYM(label = "Gym", durationMinutes = 60, description = "1h · Music only")
}

data class PresetPlan(
    val mode: PresetMode,
    val durationMinutes: Int,
    val blockedPackages: List<String>
)

/**
 * Turns a preset into the concrete package list a session blocks.
 *
 * Resolution happens on the device instead of shipping a hardcoded list,
 * because a preset is a promise about *categories* of app ("social", "music")
 * and the installed set differs per user. Three sources feed it, in order of
 * reliability:
 *
 *  1. `ApplicationInfo.category`, which stores actually set (games, social and
 *     video are the best populated).
 *  2. Known package names for the big apps that ship with no category, and for
 *     the roles that have no category at all (messaging, music, clocks).
 *  3. Role resolution for the default dialer, SMS app and alarm app, so a
 *     custom ROM's dialer is still allowed during Sleep.
 *
 * Sober itself is never blocked: enforcement has to stay reachable.
 */
object PresetResolver {

    fun resolve(context: Context, mode: PresetMode): PresetPlan {
        val pm = context.packageManager
        val launchable = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName != context.packageName }
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }

        val blocked = when (mode) {
            PresetMode.SLEEP ->
                launchable.filterNot { isCommunication(it, context) || isClock(it, context) }

            PresetMode.STUDY, PresetMode.WORK ->
                launchable.filter { isEntertainment(it) }

            PresetMode.GYM ->
                launchable.filterNot { isAudio(it) || isDefaultDialer(context, it.packageName) }
        }

        return PresetPlan(
            mode = mode,
            durationMinutes = mode.durationMinutes,
            blockedPackages = blocked.map { it.packageName }.sorted()
        )
    }

    /**
     * Packages that count as social media: what the platform labels social,
     * plus the known apps that ship without a category.
     *
     * Achievement evaluation reuses this so "a day without social media" means
     * exactly what the Study preset blocks rather than a second guess.
     */
    fun socialPackages(context: Context): Set<String> {
        val installed = runCatching {
            context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        }.getOrDefault(emptyList())

        val categorized = installed
            .filter {
                it.packageName in SOCIAL_PACKAGES ||
                    it.category == ApplicationInfo.CATEGORY_SOCIAL
            }
            .map { it.packageName }

        return SOCIAL_PACKAGES + categorized
    }

    private fun isEntertainment(info: ApplicationInfo): Boolean {
        if (info.packageName in ENTERTAINMENT_PACKAGES) return true
        return info.category == ApplicationInfo.CATEGORY_GAME ||
            info.category == ApplicationInfo.CATEGORY_VIDEO ||
            info.category == ApplicationInfo.CATEGORY_SOCIAL
    }

    private fun isAudio(info: ApplicationInfo): Boolean =
        info.category == ApplicationInfo.CATEGORY_AUDIO || info.packageName in MUSIC_PACKAGES

    private fun isCommunication(info: ApplicationInfo, context: Context): Boolean =
        isDefaultDialer(context, info.packageName) ||
            isDefaultSmsApp(context, info.packageName) ||
            info.packageName in MESSAGING_PACKAGES

    private fun isClock(info: ApplicationInfo, context: Context): Boolean =
        info.packageName in CLOCK_PACKAGES ||
            resolvePackage(context, Intent(AlarmClock.ACTION_SET_ALARM)) == info.packageName

    private fun isDefaultDialer(context: Context, packageName: String): Boolean {
        if (packageName in DIALER_PACKAGES) return true
        val default = runCatching {
            context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage
        }.getOrNull()
        return default == packageName ||
            resolvePackage(context, Intent(Intent.ACTION_DIAL)) == packageName
    }

    private fun isDefaultSmsApp(context: Context, packageName: String): Boolean =
        resolvePackage(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:"))) == packageName

    private fun resolvePackage(context: Context, intent: Intent): String? =
        context.packageManager
            .resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName

    private val DIALER_PACKAGES = setOf(
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.dialer",
        "com.oneplus.dialer",
        "com.miui.dialer"
    )

    private val MESSAGING_PACKAGES = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.thoughtcrime.securesms",
        "com.facebook.orca",
        "com.viber.voip",
        "com.tencent.mm",
        "jp.naver.line.android",
        "com.discord",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms"
    )

    private val CLOCK_PACKAGES = setOf(
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.sec.android.app.clockpackage",
        "com.oneplus.deskclock",
        "com.coloros.alarmclock",
        "com.miui.clock"
    )

    private val MUSIC_PACKAGES = setOf(
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.google.android.apps.podcasts",
        "com.google.android.music",
        "com.soundcloud.android",
        "au.com.shiftyjelly.pocketcasts",
        "com.castbox",
        "tunein.player",
        "com.audible.application",
        "com.deezer.android",
        "com.aspiro.tidal",
        "com.amazon.mp3",
        "com.apple.android.music",
        "com.samsung.android.music",
        "org.videolan.vlc",
        "code.name.monkey.retromusic"
    )

    private val SOCIAL_PACKAGES = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.zhiliaoapp.musically.go",
        "com.twitter.android",
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.pinterest"
    )

    private val ENTERTAINMENT_PACKAGES = SOCIAL_PACKAGES + setOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "tv.twitch.android.app",
        "com.disney.disneyplus",
        "com.amazon.avod.thirdpartyclient"
    )
}
