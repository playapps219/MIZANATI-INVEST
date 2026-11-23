package com.mizanati.invest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.indication
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.data.*
import com.mizanati.invest.network.FxClient
import com.mizanati.invest.ui.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        var initial = store.load()
        setContent {
            var state by remember { mutableStateOf(initial) }
            var section by remember { mutableIntStateOf(0) }   // 0 wallets, 1 tools, 2 settings
            var tab by remember { mutableIntStateOf(0) }
            var openPf by remember { mutableStateOf<String?>(null) }
            var fxMsg by remember { mutableStateOf("") }
            val scope = androidx.compose.runtime.rememberCoroutineScope()

            fun mutate(f: (AppState) -> Unit) {
                f(state); store.save(state); state = state.copy()
            }

            fun refreshFx() {
                fxMsg = "جارٍ جلب سعر الدولار من الإنترنت..."
                scope.launch {
                    val rate = FxClient.fetchUsdEgp()
                    if (rate != null && rate > 1 && rate < 1000) {
                        state.fx.now = rate
                        state.fx.upd = FxClient.stamp() + " (يدوي)"
                        state.fx.auAt = System.currentTimeMillis()
                        store.save(state)
                        state = state.copy()
                        fxMsg = "تم تحديث سعر الصرف بنجاح."
                    } else {
                        fxMsg = "فشل الاتصال بمصادر سعر الصرف. تحقق من الإنترنت، أو أدخل السعر يدوياً."
                    }
                }
            }

            // auto FX refresh at launch (same 30-min rule as the original)
            LaunchedEffect(Unit) {
                val upd = FxClient.autoUpdateIfNeeded(state.fx)
                if (upd != null) { store.save(state); state = state.copy() }
            }

            MizanatiTheme(darkMode = state.theme == "dark") {
                val t = LocalTheme
                Scaffold(
                    containerColor = t.bg,
                    topBar = {
                        Column(Modifier.fillMaxWidth().background(t.card).statusBarsPadding().padding(14.dp)) {
                            Text("MIZANATI INVEST", color = t.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Muted("ميزانيتي · إدارة واستثمار", size = 11)
                        }
                    },
                    bottomBar = {
                        Column(Modifier.fillMaxWidth().background(t.card).navigationBarsPadding()) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                listOf("المحافظ", "التقارير والأدوات", "الإعدادات").forEachIndexed { i, label ->
                                    Text(
                                        label,
                                        color = if (section == i) t.primary else t.muted,
                                        fontSize = 12.sp,
                                        fontWeight = if (section == i) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                            ) { section = i; openPf = null }
                                            .padding(vertical = 10.dp),
                                    )
                                }
                            }
                        }
                    },
                ) { pad ->
                    Box(Modifier.padding(pad)) {
                        when {
                            openPf != null -> PortfolioScreen(s = state, pfId = openPf!!,
                                onMutate = { mutate(it) }, onBack = { openPf = null })
                            section == 0 -> HomeScreen(
                                s = state, fxMsg = fxMsg,
                                onRefreshFx = { refreshFx() },
                                onOpenPortfolio = { openPf = it },
                                onGoTools = { section = 1 },
                            )
                            section == 1 -> ToolsScreen(s = state)
                            else -> SettingsScreen(
                                s = state, fxMsg = fxMsg,
                                onMutate = { mutate(it) },
                                onRefreshFx = { refreshFx() },
                                onExport = {
                                    try {
                                        val f = java.io.File(
                                            getExternalFilesDir(null),
                                            "Mizanati-Backup-" + today() + ".json",
                                        )
                                        f.writeText(store.exportJson(state))
                                        fxMsg = "تم حفظ النسخة: ${f.absolutePath}"
                                    } catch (e: Exception) { fxMsg = "فشل الحفظ: ${e.message}" }
                                },
                                onImport = { txt ->
                                    try {
                                        state = store.importJson(txt); fxMsg = "تم الاستيراد بنجاح."
                                    } catch (e: Exception) { fxMsg = "استيراد غير صالح: ${e.message}" }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
