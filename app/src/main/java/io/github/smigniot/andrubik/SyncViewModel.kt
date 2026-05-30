package io.github.smigniot.andrubik

import android.app.Application
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Activity-scoped shared state: carries the current scramble from the Scrambler
 * tab to the Timer tab, and owns the result-sync pipeline.
 *
 * Sync policy:
 *  - On each solve (only when sync is enabled) the result is appended to a
 *    persistent local queue, then a flush is attempted.
 *  - Flush POSTs queued results in order; on the first failure it stops and keeps
 *    that result (and the rest) for next time. On full success the queue empties.
 *  - When sync is disabled, nothing is sent or stored.
 */
class SyncViewModel(app: Application) : AndroidViewModel(app) {

    /** Latest scramble shown in the Scrambler tab (pushed from JS via the bridge). */
    @Volatile
    var currentScramble: String? = null

    private val settings = Settings(app)
    private val store = SolveStore(app)
    private val io = Executors.newSingleThreadExecutor()

    /** Number of results still waiting to upload (observed by the Settings tab). */
    val pendingCount = MutableLiveData<Int>()

    init {
        io.execute { refreshCount() }
    }

    /** Called when a solve finishes; persists + attempts to sync when enabled. */
    fun recordSolve(timeMs: Long) {
        val scramble = currentScramble.orEmpty()
        io.execute {
            if (!settings.syncEnabled) return@execute
            val record = JSONObject()
                .put("scramble", scramble)
                .put("timeMs", timeMs)
                .put("time", formatTime(timeMs))
                .put("timestamp", System.currentTimeMillis())
            store.add(record)
            flush()
            refreshCount()
        }
    }

    /** Retry pending uploads (e.g. after the user saves settings). */
    fun syncNow() {
        io.execute {
            flush()
            refreshCount()
        }
    }

    private fun flush() {
        if (!settings.syncEnabled || settings.serverUrl.isBlank()) return
        val pending = store.load()
        val remaining = JSONArray()
        var failed = false
        for (i in 0 until pending.length()) {
            val record = pending.getJSONObject(i)
            if (failed || !post(record)) {
                // First failure (or anything after it) stays queued for next time.
                failed = true
                remaining.put(record)
            }
        }
        store.save(remaining)
    }

    /** POSTs one record; returns true only on a 2xx response. */
    private fun post(record: JSONObject): Boolean {
        val conn = try {
            URL(settings.serverUrl).openConnection() as HttpURLConnection
        } catch (e: Exception) {
            return false
        }
        return try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val user = settings.username
            if (user.isNotEmpty()) {
                val token = Base64.encodeToString(
                    "$user:${settings.password}".toByteArray(Charsets.UTF_8),
                    Base64.NO_WRAP,
                )
                conn.setRequestProperty("Authorization", "Basic $token")
            }
            // Kinto expects every POST to be a JSON object with a single "data"
            // property wrapping the record.
            val payload = JSONObject().put("data", record)
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            false
        } finally {
            conn.disconnect()
        }
    }

    private fun refreshCount() {
        pendingCount.postValue(store.load().length())
    }

    private fun formatTime(millis: Long): String {
        val totalCentis = millis / 10
        val centis = totalCentis % 100
        val totalSeconds = totalCentis / 100
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60
        return if (minutes > 0) {
            String.format(Locale.US, "%d:%02d.%02d", minutes, seconds, centis)
        } else {
            String.format(Locale.US, "%d.%02d", seconds, centis)
        }
    }

    override fun onCleared() {
        io.shutdown()
        super.onCleared()
    }
}
