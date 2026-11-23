package com.mizanati.invest

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.data.*
import com.mizanati.invest.network.FxClient
import com.mizanati.invest.ui.*
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : ComponentActivity() {

    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        val initial = store.load()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Root(initial)
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Root(initial: AppState) {
        var state by remember { mutableStateOf(initial) }
        var section by remember { mutableIntStateOf(0) }       // 0 home, 1 reports, 2 settings
        var openPf by remember { mutableStateOf<String?>(null) }
        var analysis by remember { mutableStateOf<String?>(null) }
        var emergencyOpen by remember { mutableStateOf(false) }
        var fxMsg by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        fun mutate(f: (AppState) -> Unit) {
            f(state); store.save(state); state = state.copy()
        }

        fun refreshFx(force: Boolean) {
            fxMsg = "جارٍ جلب سعر الدولار من الإنترنت..."
            scope.launch {
                val rate = if (force) FxClient.fetchUsdEgp() else FxClient.fetchUsdEgp()
                if (rate != null && rate > 1 && rate < 1000) {
                    state.fx.now = rate
                    state.fx.upd = FxClient.stamp() + " (يدوي)"
                    state.fx.auAt = System.currentTimeMillis()
                    store.save(state); state = state.copy()
                    fxMsg = "تم تحديث سعر الصرف بنجاح."
                } else {
                    fxMsg = "فشل الاتصال بمصادر سعر الصرف. تحقق من الإنترنت، أو أدخل السعر يدويًا من الإعدادات."
                }
            }
        }

        LaunchedEffect(Unit) {
            val upd = FxClient.autoUpdateIfNeeded(state.fx)
            if (upd != null) { store.save(state); state = state.copy() }
        }

        fun exportBackup() {
            try {
                val f = java.io.File(getExternalFilesDir(null), "Mizanati-Backup-${today()}.json")
                f.writeText(store.exportJson(state))
                fxMsg = "تم حفظ النسخة: ${f.absolutePath}"
            } catch (e: Exception) { fxMsg = "فشل الحفظ: ${e.message}" }
        }

        fun exportExcel(template: Boolean) {
            try {
                val bytes = Excel.build(state, template)
                val name = if (template) "Mizanati_Import_Template.xlsx" else "Mizanati-Export-${today()}.xlsx"
                val f = java.io.File(getExternalFilesDir(null), name)
                f.writeBytes(bytes)
                val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", f)
                val it2 = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(it2, "مشاركة Excel"))
                fxMsg = "تم إنشاء الملف: $name"
            } catch (e: Exception) { fxMsg = "فشل إنشاء Excel: ${e.message}" }
        }

        MizanatiTheme(darkMode = state.theme == "dark") {
            val t = LocalTheme
            Scaffold(
                containerColor = t.bg,
                topBar = {
                    Column(Modifier.fillMaxWidth().background(t.card).statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
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
                                        ) { section = i; openPf = null; analysis = null; emergencyOpen = false }
                                        .padding(vertical = 10.dp),
                                )
                            }
                        }
                    }
                },
            ) { pad ->
                Box(Modifier.padding(pad)) {
                    when {
                        emergencyOpen -> EmergencyScreen(
                            s = state, onMutate = { mutate(it) },
                            onBack = { emergencyOpen = false },
                        )
                        analysis != null -> AnalysisScreen(
                            s = state, mode = analysis!!,
                            onBack = { analysis = null },
                        )
                        openPf != null -> PortfolioScreen(
                            s = state, pfId = openPf!!,
                            onMutate = { mutate(it) }, onBack = { openPf = null },
                        )
                        section == 0 -> HomeScreen(
                            s = state, fxMsg = fxMsg,
                            onRefreshFx = { refreshFx(true) },
                            onOpenPortfolio = { openPf = it },
                            onOpenEmergency = { emergencyOpen = true },
                            onOpenAnalysis = { analysis = it },
                        )
                        section == 1 -> ReportsScreen(
                            s = state, onMutate = { mutate(it) },
                            onExportExcel = { exportExcel(false) },
                            onExportTemplate = { exportExcel(true) },
                        )
                        else -> SettingsScreen(
                            s = state, fxMsg = fxMsg,
                            onMutate = { mutate(it) },
                            onRefreshFx = { refreshFx(true) },
                            onExportBackup = { exportBackup() },
                            onImportBackup = { txt ->
                                try { state = store.importJson(txt); fxMsg = "تم الاستيراد بنجاح." }
                                catch (e: Exception) { fxMsg = "استيراد غير صالح: ${e.message}" }
                            },
                            onExportExcel = { exportExcel(false) },
                        )
                    }
                }
            }
        }
    }
}

object Excel {
    /** Minimal real xlsx (xlsx = zip of xml sheets) with instruments sheet or empty template. */
    fun build(s: AppState, template: Boolean): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { z ->
            fun put(name: String, xml: String) {
                z.putNextEntry(ZipEntry(name))
                z.write(xml.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            put("[Content_Types].xml", """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""")
            put("_rels/.rels", """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")
            put("xl/workbook.xml", """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Instruments" sheetId="1" r:id="rId1"/></sheets>
</workbook>""")
            put("xl/_rels/workbook.xml.rels", """
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""")
            val header = listOf("portfolio", "instrument", "kind", "date", "type", "amount", "units", "note", "currentValue")
            val esc = { v: Any -> v.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;") }
            val sb = StringBuilder()
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
            sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>")
            sb.append("<row>").append(header.joinToString("") { "<t><v>${esc(it)}</v></t>" }).append("</row>")
            if (!template) {
                s.pfs.forEach { p ->
                    s.ins.filter { it.pf == p.id }.forEach { i ->
                        i.tx.forEach { t ->
                            val cells = listOf(p.name, i.name, i.kind, t.d, t.typ, t.amt, t.units, t.note, i.cv)
                            sb.append("<row>").append(cells.joinToString("") { c -> "<t><v>${esc(c)}</v></t>" }).append("</row>")
                        }
                    }
                }
            }
            sb.append("</sheetData></worksheet>")
            put("xl/worksheets/sheet1.xml", sb.toString())
        }
        return bos.toByteArray()
    }
}
