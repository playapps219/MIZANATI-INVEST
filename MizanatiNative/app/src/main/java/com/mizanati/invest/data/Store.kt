package com.mizanati.invest.data

import android.content.Context
import org.json.JSONObject

// Local storage: same "pf4" key and JSON shape as the original app's localStorage.

class Store(private val context: Context) {

    private val prefs = context.getSharedPreferences("mizanati", Context.MODE_PRIVATE)

    fun load(): AppState {
        val raw = try { prefs.getString("pf4", null) } catch (e: Exception) { null }
        val s = try {
            raw?.let { StateJson.fromJson(JSONObject(it)) }
        } catch (e: Exception) { null }
        return s ?: Seed.state()
    }

    fun save(s: AppState) {
        prefs.edit().putString("pf4", StateJson.toJson(s).toString()).apply()
    }

    fun exportJson(s: AppState): String = StateJson.toJson(s).toString(2)

    fun importJson(text: String): AppState {
        val s = StateJson.fromJson(JSONObject(text))
        save(s)
        return s
    }

    companion object {
        const val BACKUP_PREFIX = "Mizanati-Backup-"
    }
}
