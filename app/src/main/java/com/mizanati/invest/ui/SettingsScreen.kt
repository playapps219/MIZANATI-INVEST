package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.data.AppState
import kotlinx.coroutines.launch

// Settings (الإعدادات): FX book rate, auto refresh, theme, data export/import.

@Composable
fun SettingsScreen(
    s: AppState,
    fxMsg: String,
    onMutate: ((AppState) -> Unit) -> Unit,
    onRefreshFx: () -> Unit,
    onExport: () -> Unit,
    onImport: (String) -> Unit,
) {
    val t = LocalTheme
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var bookRate by remember(s.fx.book) { mutableStateOf(Finance.fmt(s.fx.book, 2)) }
    var importText by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle("الإعدادات")

        Card2 {
            Text("سعر الصرف", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Field(bookRate, { bookRate = it }, "سعر شراء الدولار (أساس)")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("حفظ", Modifier.weight(1f)) {
                    val r = bookRate.toDoubleOrNull()
                    if (r != null && r > 1) onMutate { it.fx.book = r; it.fx.bookDate = com.mizanati.invest.data.today() }
                }
                BigButton("جلب الآن", Modifier.weight(1f)) { onRefreshFx() }
            }
            Spacer(Modifier.height(6.dp))
            Muted(fxMsg.ifBlank { if (s.fx.upd.isNotBlank()) "آخر تحديث: ${s.fx.upd}" else "التحديث التلقائي كل 30 دقيقة عند توفر الإنترنت." }, size = 11)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Muted("التحديث التلقائي", size = 13)
                Text(
                    if (s.fx.auto) "مفعل" else "معطل",
                    color = if (s.fx.auto) t.green else t.muted,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = clickableNoRipple {
                        onMutate { st -> st.fx.auto = !st.fx.auto }
                    },
                )
            }
        }

        Card2 {
            Text("المظهر", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("فاتح", s.theme != "dark") { onMutate { it.theme = "light" } }
                Chip("داكن", s.theme == "dark") { onMutate { it.theme = "dark" } }
            }
        }

        Card2 {
            Text("البيانات", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            BigButton("تصدير نسخة احتياطية") { onExport() }
            Spacer(Modifier.height(8.dp))
            Field(importText, { importText = it }, "الصق محتوى النسخة الاحتياطية (JSON)")
            Spacer(Modifier.height(8.dp))
            BigButton("استيراد", danger = true) {
                if (importText.isNotBlank()) onImport(importText)
            }
        }

        Muted("MIZANATI INVEST · v1.2.5 · يعمل دون إنترنت مع تحديث اختياري للأسعار", size = 11)
    }
}

fun clickableNoRipple(on: () -> Unit): Modifier = Modifier.clickable(
    interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
    indication = null,
    onClick = on,
)
