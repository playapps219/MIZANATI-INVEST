package com.mizanati.invest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.compose.runtime.remember
import com.mizanati.invest.ui.AppShell
import com.mizanati.invest.ui.AppState
import com.mizanati.invest.ui.FreedomScreen
import com.mizanati.invest.ui.HomeScreen
import com.mizanati.invest.ui.PortfolioScreen
import com.mizanati.invest.ui.ReportsScreen
import com.mizanati.invest.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val app = remember { AppState(applicationContext) }
            var dark by rememberSaveable { mutableStateOf(false) }
            var tab by rememberSaveable { mutableIntStateOf(0) }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            AppShell(
                dark = dark,
                tab = tab,
                investorName = app.S.optString("investorName"),
                onToggleTheme = { dark = !dark },
                onTab = { tab = it },
                onQuickAdd = {},
                pane = { t ->
                    if (t == 0) {
                        if (app.curP == "all" || app.S.optJSONArray("pfs")?.let { a -> (0 until a.length()).none { a.getJSONObject(it).optString("id") == app.curP } } != false) {
                            HomeScreen(app, onOpenPortfolio = { app.curP = it })
                        } else {
                            PortfolioScreen(app)
                        }
                    } else if (t == 1) {
                        ReportsScreen(app)
                    } else if (t == 2) {
                        FreedomScreen(app)
                    } else {
                        SettingsScreen(app)
                    }
                },
            )
        }
    }
}
