package pl.mdinstal.chatbox.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class UpdateInfo(
    val version: String,
    val tag: String,
    val apkUrl: String,
    val notes: String,
    val sizeBytes: Long
)

private data class GhAsset(val name: String, val browser_download_url: String, val size: Long)
private data class GhRelease(val tag_name: String, val body: String?, val assets: List<GhAsset>?)

/**
 * Aktualizacje z GitHub Releases (jak w MD-Tools).
 * Repo musi być publiczne, żeby API działało bez tokenu.
 */
class Updater(private val context: Context) {

    companion object {
        const val REPO = "kolczan07/MD-Chatbox"   // <- zmień, jeśli repo nazywa się inaczej
    }

    private val client = OkHttpClient()
    private val gson = Gson()

    fun currentVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
    }.getOrDefault("0")

    suspend fun checkForUpdate(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://api.github.com/repos/$REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 404) return@runCatching null   // brak release'ów
                if (!resp.isSuccessful) error("GitHub HTTP ${resp.code}")
                val rel = gson.fromJson(resp.body!!.string(), GhRelease::class.java)
                val asset = rel.assets?.firstOrNull { it.name.endsWith(".apk") }
                    ?: return@runCatching null
                val remote = rel.tag_name.removePrefix("v")
                if (!isNewer(remote, currentVersion())) return@runCatching null
                UpdateInfo(remote, rel.tag_name, asset.browser_download_url, rel.body.orEmpty(), asset.size)
            }
        }
    }

    suspend fun download(info: UpdateInfo, onProgress: (Float) -> Unit): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val dir = File(context.cacheDir, "updates").apply { deleteRecursively(); mkdirs() }
                val file = File(dir, "MD-Chatbox-${info.tag}.apk")
                client.newCall(Request.Builder().url(info.apkUrl).build()).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val body = resp.body ?: error("Pusta odpowiedź")
                    val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
                    var done = 0L
                    var lastPct = -1
                    body.byteStream().use { input ->
                        file.outputStream().use { out ->
                            val buf = ByteArray(16 * 1024)
                            while (true) {
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                                done += n
                                if (total > 0) {
                                    val pct = (done * 100 / total).toInt()
                                    if (pct != lastPct) { lastPct = pct; onProgress(pct / 100f) }
                                }
                            }
                        }
                    }
                }
                file
            }
        }

    /** Zwraca false, jeśli trzeba najpierw zezwolić na instalację z tego źródła. */
    fun install(file: File): Boolean {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    private fun parts(v: String) =
        v.removePrefix("v").substringBefore("-").split(".").map { it.toIntOrNull() ?: 0 }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = parts(remote); val l = parts(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }; val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }
}
