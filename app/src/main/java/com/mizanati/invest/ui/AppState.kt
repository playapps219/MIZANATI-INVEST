package com.mizanati.invest.ui

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.mizanati.invest.core.Engine
import com.mizanati.invest.core.Store
import com.mizanati.invest.core.syncEmergencyPortfolio
import org.json.JSONObject
import java.io.File

class AppState(context: Context) {
    val store = Store(File(context.filesDir, "pf4.json"))

    var version by mutableIntStateOf(0)
        private set

    var curP by mutableStateOf("all")
    var openIns by mutableStateOf<String?>(null)
    var toast by mutableStateOf<String?>(null)
    var ffEditSpend by mutableStateOf(false)

    val S: JSONObject get() = store.S

    init {
        sync()
    }

    private fun sync() {
        syncEmergencyPortfolio(S)
        Engine(S).syncAutoPfTargets()
    }

    fun U(): Boolean {
        sync()
        val ok = store.save()
        if (!ok) toast = "تعذر حفظ البيانات على الجهاز"
        version++
        return ok
    }

    fun replace(data: JSONObject): Boolean {
        if (!store.import(data)) return false
        sync()
        curP = "all"
        openIns = null
        version++
        return true
    }
}
