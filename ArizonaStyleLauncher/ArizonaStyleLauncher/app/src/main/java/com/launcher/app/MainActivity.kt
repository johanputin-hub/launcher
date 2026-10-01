package com.launcher.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val prefs by lazy { getSharedPreferences("launcher", MODE_PRIVATE) }
    private val nickRegex = Regex("^[A-Za-z0-9_\\[\\]$=@.()]{3,20}$")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nick = findViewById<EditText>(R.id.nick)
        val spinner = findViewById<Spinner>(R.id.servers)
        val status = findViewById<TextView>(R.id.status)
        val play = findViewById<Button>(R.id.play)
        findViewById<TextView>(R.id.title).text = Config.APP_TITLE

        nick.setText(prefs.getString("nick", ""))
        spinner.adapter = ArrayAdapter(this, R.layout.spinner_item, Config.SERVERS.map { it.name })
        spinner.setSelection(prefs.getInt("server", 0).coerceIn(0, Config.SERVERS.lastIndex))

        // Ping selected server
        fun ping() {
            val s = Config.SERVERS[spinner.selectedItemPosition]
            status.text = "Checking ${s.ip}:${s.port}..."
            thread {
                val ok = try {
                    Socket().use { it.connect(InetSocketAddress(s.ip, s.port), 1500) }
                    true
                } catch (e: Exception) { false }
                runOnUiThread { status.text = if (ok) "Server reachable" else "Server status: unknown (UDP query not shown)" }
            }
        }
        spinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) { ping() }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        play.setOnClickListener {
            val name = nick.text.toString().trim()
            if (!nickRegex.matches(name)) {
                nick.error = "3-20 chars: letters, numbers, _ [ ] $ = @ . ( )"
                return@setOnClickListener
            }
            val idx = spinner.selectedItemPosition
            val s = Config.SERVERS[idx]
            prefs.edit().putString("nick", name).putInt("server", idx).apply()
            launchGame(name, s)
        }
    }

    private fun launchGame(nick: String, s: ServerInfo) {
        val pm = packageManager
        val installed = try { pm.getPackageInfo(Config.CLIENT_PACKAGE, 0); true } catch (e: Exception) { false }
        val intent = if (installed) pm.getLaunchIntentForPackage(Config.CLIENT_PACKAGE) else null
        if (intent == null) {
            if (Config.CLIENT_APK_URL.isBlank()) {
                findViewById<TextView>(R.id.status).text = "Game client not installed (${Config.CLIENT_PACKAGE}). Install it first, then press PLAY."
                Toast.makeText(this, "Game client not installed", Toast.LENGTH_LONG).show()
                return
            }
            val bar = findViewById<ProgressBar>(R.id.progress)
            val status = findViewById<TextView>(R.id.status)
            bar.visibility = android.view.View.VISIBLE
            Installer.downloadAndInstall(this, Config.CLIENT_APK_URL,
                onProgress = { p -> runOnUiThread { bar.progress = p; status.text = "Downloading client... $p%" } },
                onError = { m -> runOnUiThread { bar.visibility = android.view.View.GONE; status.text = "Error: $m" } },
                onDone = { runOnUiThread { bar.visibility = android.view.View.GONE; status.text = "Install the client, then press PLAY again" } })
            return
        }
        writeClientSettings(nick, s)
        intent.putExtra("nickname", nick)
        intent.putExtra("ip", s.ip)
        intent.putExtra("port", s.port)
        try {
            startActivity(intent)
        } catch (e: Exception) {
            findViewById<TextView>(R.id.status).text = "Could not start the game: ${e.message}"
        }
    }

    /** Best effort: write nickname/server into the client's settings.ini (only works if storage access allows it). */
    private fun writeClientSettings(nick: String, s: ServerInfo) {
        try {
            val dir = java.io.File(
                android.os.Environment.getExternalStorageDirectory(),
                "Android/data/${Config.CLIENT_PACKAGE}/files/SAMP"
            )
            if (!dir.exists() && !dir.mkdirs()) return
            java.io.File(dir, "settings.ini").writeText(
                "[client]\nname = $nick\nhost = ${s.ip}\nport = ${s.port}\n"
            )
        } catch (e: Exception) { /* blocked by scoped storage; extras still sent */ }
    }
}
