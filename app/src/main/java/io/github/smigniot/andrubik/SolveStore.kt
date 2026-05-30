package io.github.smigniot.andrubik

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * File-backed FIFO queue of solves still pending upload. Persists across app
 * restarts. All access MUST happen on a single background thread (see
 * [SyncViewModel]) so reads/writes never race.
 */
class SolveStore(context: Context) {
    private val file = File(context.applicationContext.filesDir, "pending_solves.json")

    fun load(): JSONArray =
        if (file.exists()) {
            runCatching { JSONArray(file.readText()) }.getOrDefault(JSONArray())
        } else {
            JSONArray()
        }

    fun save(array: JSONArray) {
        file.writeText(array.toString())
    }

    fun add(record: JSONObject) {
        val arr = load()
        arr.put(record)
        save(arr)
    }
}
