package com.launcher.app

/** Edit this file for your server. */
object Config {
    const val APP_TITLE = "INTERNATIONAL ROLEPLAY"
    /** Installed SA-MP Mobile client (taken from the APK you uploaded: SA-MP Launcher 2.0). */
    const val CLIENT_PACKAGE = "ru.unisamp_mobile.game"
    const val CLIENT_ACTIVITY = "ru.unisamp_mobile.game.GTASA"
    /** Direct URL to the client APK YOU host and have rights to distribute. Leave "" to send players to CLIENT_STORE_URL instead. */
    const val CLIENT_APK_URL = ""
    const val CLIENT_STORE_URL = "https://play.google.com/store/apps/details?id=com.rockstargames.gtasa"
    /** Optional: URL returning a news line (plain text). Leave empty to disable. */
    const val NEWS_URL = ""
    val SERVERS = listOf(
        ServerInfo("International RolePlay | Season 1.0", "92.119.165.177", 7968)
    )
}

data class ServerInfo(val name: String, val ip: String, val port: Int)
