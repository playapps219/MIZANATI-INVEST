package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.data.AppState
import com.mizanati.invest.data.today

// Settings (الإعدادات): FX book rate + auto refresh, target USD split, theme,
// inflation (CBE style), backup export/import, about.

@Composable
fun SettingsScreen(
    s: AppState,
    fxMsg: String,
    onMutate: ((AppState) -> Unit) -> Unit,
    onRefreshFx: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: (String) -> Unit,
    onExportExcel: () -> Unit,
) {
    val t = LocalTheme
    var bookRate by remember(s.fx.book) { mutableStateOf(Finance.fmt(s.fx.book, 2)) }
    var bookDate by remember(s.fx.bookDate) { mutableStateOf(s.fx.bookDate) }
    var split by remember(s.split) { mutableStateOf(s.split.toString()) }
    var importText by remember { mutableStateOf("") }
    var inflation by remember(s.inflation) { mutableStateOf(if (s.inflation > 0) Finance.fmt(s.inflation, 1) else "") }

    Column(
        Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionTitle("الإعدادات")

        // سعر الصرف
        Card2 {
            Text("سعر الصرف", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(bookRate, { bookRate = it }, "سعر الدولار المرجعي (أساس)", Modifier.weight(1f))
                Field(bookDate, { bookDate = it }, "تاريخ سعر البدء", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("حفظ", Modifier.weight(1f)) {
                    val r = bookRate.toDoubleOrNull()
                    if (r != null && r > 1) onMutate { it.fx.book = r; it.fx.bookDate = bookDate.ifBlank { today() } }
                }
                BigButton("جلب الآن", Modifier.weight(1f)) { onRefreshFx() }
            }
            Spacer(Modifier.height(6.dp))
            Muted(fxMsg.ifBlank {
                if (s.fx.upd.isNotBlank()) "آخر تحديث: ${s.fx.upd}"
                else "التحديث التلقائي كل 30 دقيقة عند توفر الإنترنت."
            }, size = 11)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Muted("التحديث التلقائي", size = 13)
                Text(
                    if (s.fx.auto) "مفعل" else "معطل",
                    color = if (s.fx.auto) t.green else t.muted,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onMutate { it.fx.auto = !it.fx.auto } },
                )
            }
        }

        // نسبة الدولار المستهدفة
        Card2 {
            Text("توزيع العملات المستهدف", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Muted("نسبة الدولار المستهدفة من ثروتك (الباقي جنيه)", size = 11)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(split, { split = it }, "نسبة الدولار %", Modifier.weight(1f))
                BigButton("حفظ", Modifier.weight(1f)) {
                    val v = split.toIntOrNull()
                    if (v != null && v in 0..100) onMutate { it.split = v }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(50, 60, 70, 80).forEach { v ->
                    Chip("$v%", s.split == v, Modifier.weight(1f)) { onMutate { it.split = v }; split = v.toString() }
                }
            }
        }

        // التضخم
        Card2 {
            Text("التضخم (تحديث يدوي من بيانات البنك المركزي)", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(inflation, { inflation = it }, "التضخم السنوي %", Modifier.weight(1f))
                BigButton("حفظ", Modifier.weight(1f)) {
                    val v = inflation.toDoubleOrNull()
                    if (v != null) onMutate {
                        it.inflation = v; it.inflationMonthly = v / 12.0; it.inflationPeriod = today()
                    }
                }
            }
            if (s.inflation > 0) {
                Spacer(Modifier.height(4.dp))
                Muted("المحفوظ: ${Finance.fmt(s.inflation, 1)}% شهريًا ${Finance.fmt(s.inflationMonthly, 2)}%${if (s.inflationPeriod.isNotBlank()) " — محدّث ${s.inflationPeriod}" else ""}", size = 10)
            }
        }

        // المظهر
        Card2 {
            Text("المظهر", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("فاتح", s.theme != "dark") { onMutate { it.theme = "light" } }
                Chip("داكن", s.theme == "dark") { onMutate { it.theme = "dark" } }
            }
        }

        // البيانات
        Card2 {
            Text("البيانات والنسخ الاحتياطي", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            BigButton("تصدير نسخة احتياطية (JSON)") { onExportBackup() }
            Spacer(Modifier.height(8.dp))
            BigButton("تصدير Excel (xlsx)") { onExportExcel() }
            Spacer(Modifier.height(8.dp))
            Field(importText, { importText = it }, "الصق محتوى النسخة الاحتياطية (JSON) للاستيراد")
            Spacer(Modifier.height(8.dp))
            BigButton("استيراد (يستبدل البيانات)", danger = true) {
                if (importText.isNotBlank()) onImportBackup(importText)
            }
        }

        Card2 {
            Text("عن التطبيق", color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Muted("MIZANATI INVEST · الإصدار 1.2.5", size = 12)
            Muted("إدارة واستثمار محافظك بعملتين: جنيه ودولار. يحسب العائد المركب (XIRR)، أثر الصرف، التركّز، والانتظام.", size = 11)
            Spacer(Modifier.height(6.dp))
            Muted("بياناتك محفوظة محليًا على جهازك فقط (local storage).", size = 10)
        }
    }
}
