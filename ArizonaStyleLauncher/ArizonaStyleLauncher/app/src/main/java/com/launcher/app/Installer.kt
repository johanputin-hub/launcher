package com.launcher.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object Installer {
    /** Downloads the APK from [url] with progress (0-100), then opens the system installer. */
    fun downloadAndInstall(
        ctx: Context, url: String,
        onProgress: (Int) -> Unit, onError: (String) -> Unit, onDone: () -> Unit
    ) = thread {
        try {
            val out = File(ctx.cacheDir, "client.apk")
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 10000; c.readTimeout = 15000
            val total = c.contentLengthLong
            c.inputStream.use { inp ->
                out.outputStream().use { o ->
                    val buf = ByteArray(64 * 1024); var done = 0L; var n: Int
                    while (inp.read(buf).also { n = it } > 0) {
                        o.write(buf, 0, n); done += n
                        if (total > 0) onProgress((done * 100 / total).toInt())
                    }
                }
            }
            val uri: Uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", out)
            ctx.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            onDone()
        } catch (e: Exception) { onError(e.message ?: "Download failed") }
    }
}
