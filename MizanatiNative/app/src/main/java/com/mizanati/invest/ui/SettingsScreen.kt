package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mizanati.invest.calc.Finance
import com.mizanati.invest.data.AppState
import com.mizanati.invest.data.today

// Settings — accordion list matching the original: each row has a colored side bar,
// icon, title, subtitle and expand arrow; body opens in place.

@Composable
private fun Accordion(
    icon: String, title: String, sub: String,
    bar: androidx.compose.ui.graphics.Color, expandedInit: Boolean = false,
    content: @Composable () -> Unit,
) {
    val t = LocalTheme
    var open by remember { mutableStateOf(expandedInit) }
    Column(
        Modifier.fillMaxWidth().background(t.card, RoundedCornerShape(14.dp))
            .padding(start = 4.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { open = !open }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(5.dp).height(46.dp).clip(RoundedCornerShape(4.dp)).background(bar)) {}
            Spacer(Modifier.width(12.dp))
            Text(icon, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = t.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(sub, color = t.muted, fontSize = 11.sp)
            }
            Text(if (open) "▾" else "‹", color = t.muted, fontSize = 16.sp)
        }
        if (open) {
            Column(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) { content() }
        }
    }
}

@Composable
fun SettingsScreen(
    s: AppState,
    fxMsg: String,
    onMutate: ((AppState) -> Unit) -> Unit,
    onRefreshFx: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: (String) -> Unit,
    onExportExcel: () -> Unit,
    onExportTemplate: () -> Unit,
) {
    val t = LocalTheme
    var bookRate by remember(s.fx.book) { mutableStateOf(Finance.fmt(s.fx.book, 2)) }
    var bookDate by remember(s.fx.bookDate) { mutableStateOf(s.fx.bookDate) }
    var split by remember(s.split) { mutableStateOf(s.split.toString()) }
    var importText by remember { mutableStateOf("") }
    var inflation by remember(s.inflation) { mutableStateOf(if (s.inflation > 0) Finance.fmt(s.inflation, 1) else "") }

    Column(
        Modifier.fillMaxSize().background(t.bg).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Accordion("👤", "اسم المستثمر", s.owner.ifBlank { "مستثمر" }, barOf("3D35A6"), expandedInit = true) {
            Field(s.owner, { v -> onMutate { it.owner = v } }, "اكتب اسمك كما يظهر أعلى الشاشة")
        }

        Accordion("⚖️", "نسبة الدولار والجنيه", "دولار: ${s.split}% · جنيه: ${100 - s.split}%", barOf("6C5CE7")) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(split, { split = it }, "نسبة الدولار %", Modifier.weight(1f))
                BigButton("حفظ", Modifier.weight(1f)) {
                    val v = split.toIntOrNull()
                    if (v != null && v in 0..100) onMutate { it.split = v }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(50, 60, 70, 80).forEach { v ->
                    Chip("$v%", s.split == v, Modifier.weight(1f)) { onMutate { it.split = v }; split = v.toString() }
                }
            }
        }

        Accordion("💱", "سعر الصرف والتجاني الجنيه", "الدلاري: ${Finance.fmt(s.fx.book, 2)}", barOf("F59E0B")) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Field(bookRate, { bookRate = it }, "سعر الدولار المرجعي (أساس)", Modifier.weight(1f))
                Field(bookDate, { bookDate = it }, "تاريخ سعر البدء", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BigButton("حفظ", Modifier.weight(1f)) {
                    val r = bookRate.toDoubleOrNull()
                    if (r != null && r > 1) onMutate { it.fx.book = r; it.fx.bookDate = bookDate.ifBlank { today() } }
                }
                BigButton("جلب الآن", Modifier.weight(1f)) { onRefreshFx() }
            }
            Muted(fxMsg.ifBlank {
                if (s.fx.upd.isNotBlank()) "آخر تحديث: ${s.fx.upd}"
                else "التحديث التلقائي كل 30 دقيقة عند توفر الإنترنت."
            }, size = 11)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Muted("التحديث التلقائي", size = 13)
                Text(
                    if (s.fx.auto) "مفعل" else "معطل",
                    color = if (s.fx.auto) t.green else t.muted,
                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onMutate { it.fx.auto = !it.fx.auto } },
                )
            }
        }

        Accordion("🧯", "صندوق الطوارئ", "إعدادات الحرث والوزارة والتصنيف", barOf("EF4444")) {
            Muted("يُدار من تبويب المحافظ ← بطاقة صندوق الطوارئ، وهنا يظهر ملخّصه فقط.", size = 11)
            Muted("الرصيد الحالي: ${Finance.fmt(Finance.efBalance(s.emergencyFund), 0)} ${Finance.curSym(s.emergencyFund.currency)}", size = 12)
        }

        Accordion("✈️", "مخزنة الدولار", "أساسي · USD", barOf("10B981")) {
            Muted("تُدار من تبويب المحافظ: كل محفظة دولار تُحسب بالعملتين، والنسبة المستهدفة تُضبط من «نسبة الدولار والجنيه» أعلاه.", size = 11)
            Muted("عدد محافظ الدولار: ${s.pfs.count { it.cur == "USD" && !it.isEmergency }}", size = 12)
        }

        Accordion("🇪🇬", "مخزنة الجنيه", "أساسي · EGP", barOf("EF4444")) {
            Muted("تُدار من تبويب المحافظ: محافظ الجنيه تُقاس بالجنيه مباشرة.", size = 11)
            Muted("عدد محافظ الجنيه: ${s.pfs.count { it.cur == "EGP" && !it.isEmergency }}", size = 12)
        }

        Accordion("🥇", "مخزنة الذهب EGP", "أساسي · EGP", barOf("F59E0B")) {
            Muted("الأدوات الذهبية تُضبط من تبويب المحافظ ← إضافة أداة ← النوع «ذهب».", size = 11)
            Muted("عدد أدوات الذهب: ${s.ins.count { it.kind == "gold" }}", size = 12)
        }

        Accordion("🏢", "مخزنة العقار", "أساسي · EGP", barOf("6366F1")) {
            Muted("العقارات تُدار من تبويب المحافظ ← إضافة أداة ← النوع «عقار» مع الإيجار الصافي الشهري.", size = 11)
            Muted("عدد أدوات العقار: ${s.ins.count { it.kind == "prop" }}", size = 12)
        }

        Accordion("🏅", "مخزنة الذهاب USD", "أساسي · USD", barOf("0EA5E9")) {
            Muted("محافظ الدولار الخاصة تُدار كما غيرها من تبويب المحافظ.", size = 11)
        }

        Accordion("🔒", "قفل التطبيق", "غير مفعّل", barOf("64748B")) {
            Muted("حماية التطبيق ببصمة أو رمز مرتبطة بحماية قفل هاتفك — تُدار من إعدادات النظام.", size = 11)
        }

        Accordion("📥", "تصدير سجل العمليات", "ملك Excel لأنافة لتصديرها", barOf("F97316")) {
            BigButton("تصدير Excel (xlsx)") { onExportExcel() }
            BigButton("تنزيل قالب فارغ") { onExportTemplate() }
        }

        Accordion("💾", "النسخ الاحتياطي", "حماية بياناتك محليًا واستعادتها عند الحاجة", barOf("0EA5E9")) {
            BigButton("تصدير نسخة احتياطية (JSON)") { onExportBackup() }
            Field(importText, { importText = it }, "الصق محتوى النسخة الاحتياطية (JSON) للاستيراد")
            BigButton("استيراد (يستبدل البيانات)", danger = true) {
                if (importText.isNotBlank()) onImportBackup(importText)
            }
        }

        Accordion("🛠", "مطور البرنامج", "لدعم الفني والاقتراحات", barOf("64748B")) {
            Muted("MIZANATI INVEST — صُمم ليمنحك لوحة تحكم واحدة بجميع محافظك بعملتين.", size = 11)
        }

        // عن التطبيق (banner like the original footer)
        Row(
            Modifier.fillMaxWidth().background(t.card2, RoundedCornerShape(14.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ميزانيتي", color = t.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("الإصدار V.1.2.5", color = t.primary, fontSize = 12.sp,
                modifier = Modifier.background(t.card, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
        }

        SectionTitle("التضخم")
        Card2 {
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
                Muted("المحفوظ: ${Finance.fmt(s.inflation, 1)}% سنويًا — ${Finance.fmt(s.inflationMonthly, 2)}% شهريًا${if (s.inflationPeriod.isNotBlank()) " — محدّث ${s.inflationPeriod}" else ""}", size = 10)
            }
        }

        SectionTitle("المظهر")
        Card2 {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("فاتح", s.theme != "dark") { onMutate { it.theme = "light" } }
                Chip("داكن", s.theme == "dark") { onMutate { it.theme = "dark" } }
            }
        }
    }
}

@Composable
private fun barOf(hex: String) = try {
    androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    LocalTheme.primary
}
