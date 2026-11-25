package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mizanati.invest.core.Lock
import kotlinx.coroutines.delay

private fun ms(v: Double): String {
    val s = (v / 1000).toLong() + 1
    return if (s >= 60) "${s / 60} د ${s % 60} ث" else "$s ث"
}

@Composable
private fun pinDots(n: Int, error: Boolean, pinLen: Int) {
    val mz = LocalMz.current
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(pinLen) { i ->
            Box(
                Modifier
                    .size(13.dp)
                    .clip(CircleShape)
                    .let {
                        if (error) it.background(mz.r)
                        else if (i < n) it.background(mz.t)
                        else it.border(1.6.dp, mz.m, CircleShape)
                    },
            )
        }
    }
}

@Composable
private fun keyBtn(label: String, color: Color, onKey: () -> Unit) {
    Box(
        Modifier
            .size(width = 76.dp, height = 58.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onKey),
        contentAlignment = Alignment.Center,
    ) {
        T(label, 24, FontWeight.Bold, color, align = TextAlign.Center)
    }
}

@Composable
fun LockScreen(app: AppState, onUnlock: () -> Unit) {
    val mz = LocalMz.current
    var pin by remember { mutableStateOf("") }
    var err by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var showHint by remember { mutableStateOf(false) }
    val pinLen = Lock.pinLen(app.S)

    fun fail() {
        err = true
        val left = Lock.lockedOut(app.S)
        msg = if (left > 0) "تم قفل التطبيق مؤقتاً — انتظر " + ms(left) else "رمز غير صحيح"
        pin = ""
    }

    fun submit() {
        if (pin.length < 4) return
        if (Lock.unlock(app.S, pin)) {
            app.U()
            onUnlock()
        } else {
            fail()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(mz.c)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Spacer(Modifier.height(30.dp))
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(mz.p),
            contentAlignment = Alignment.Center,
        ) { LogoMark(38.dp) }
        T("ميزانتي", 22, FontWeight.Bold)
        T(if (msg.isEmpty()) "أدخل رمز القفل للمتابعة" else msg, 13, color = if (msg.isEmpty()) mz.m else mz.r)
        pinDots(pin.length, err, pinLen)
        if (showHint) {
            val h = Lock.hint(app.S)
            if (h.isNotEmpty()) Card { T("التلميح: " + h, 13, color = mz.m, align = TextAlign.Center) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { k ->
                        keyBtn(k, mz.t) {
                            if (Lock.lockedOut(app.S) > 0) {
                                fail()
                                return@keyBtn
                            }
                            if (pin.length < Lock.pinLen(app.S)) pin += k
                            err = false
                            if (pin.length == Lock.pinLen(app.S)) submit()
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                keyBtn("تلميح", mz.m) { showHint = true }
                keyBtn("0", mz.t) {
                    if (Lock.lockedOut(app.S) > 0) {
                        fail()
                        return@keyBtn
                    }
                    if (pin.length < Lock.pinLen(app.S)) pin += "0"
                    err = false
                    if (pin.length == Lock.pinLen(app.S)) submit()
                }
                keyBtn("حذف", mz.m) { pin = pin.dropLast(1); err = false }
            }
        }
        T(
            "نسيت الرمز؟ احذف بيانات التطبيق لإلغاء القفل (سيُحذف سجل محافظك)",
            11,
            color = mz.m,
            modifier = Modifier.padding(top = 12.dp),
            align = TextAlign.Center,
        )
    }
}

@Composable
fun LockGate(app: AppState, pane: @Composable () -> Unit) {
    var seenV by remember { mutableIntStateOf(app.version) }
    var unlocked by remember { mutableStateOf(!Lock.enabled(app.S)) }
    if (app.version != seenV) {
        seenV = app.version
        if (Lock.enabled(app.S)) unlocked = false
    }
    if (unlocked) pane() else LockScreen(app) { unlocked = true }
}

@Composable
fun LockSetupDialog(app: AppState, onDone: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf(app.S.optString("lockHint")) }
    var err by remember { mutableStateOf("") }
    val mz = LocalMz.current
    val changing = Lock.enabled(app.S)

    MzDialog(if (changing) "تغيير رمز القفل" else "إعداد القفل", onDone) {
        T("رمز من 4 إلى 6 أرقام يُطلَب عند فتح التطبيق. يُحفظ مشفراً (PBKDF2) داخل بيانات التطبيق ولا يمكن استعادته إذا نسيته.", 12, color = mz.m)
        Field("رمز PIN", pin, { pin = it.filter { ch -> ch.isDigit() }.take(6) }, password = true, keyboard = KeyboardType.Number, ltr = true)
        if (pin.length >= 4) Field("تأكيد الرمز", confirm, { confirm = it.filter { ch -> ch.isDigit() }.take(6) }, password = true, keyboard = KeyboardType.Number, ltr = true)
        Field("تلميح (اختياري)", hint, { hint = it })
        if (err.isNotEmpty()) T(err, 12, color = mz.r)
        Btn("حفظ", Modifier.fillMaxWidth(), enabled = pin.length >= 4) {
            when {
                pin.length < 4 -> err = "الرمز 4 أرقام على الأقل"
                confirm.isNotEmpty() && confirm != pin -> err = "التأكيد لا يطابق الرمز"
                else -> {
                    Lock.setPin(app.S, pin, hint)
                    app.U()
                    onDone()
                }
            }
        }
    }
}

@Composable
fun LockSettingsCard(app: AppState) {
    val mz = LocalMz.current
    val on = Lock.enabled(app.S)
    var setup by remember { mutableStateOf(false) }
    var disable by remember { mutableStateOf(false) }
    Card {
        T("قفل التطبيق", 14, FontWeight.Bold)
        T("رمز PIN يُطلَب عند فتح التطبيق. يُحفظ مشفراً (PBKDF2، 200 ألف تكرار) داخل بيانات التطبيق ولا يمكن استعادته إذا نسيته.", 12, color = mz.m)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(if (on) "تغيير الرمز" else "إعداد القفل", Modifier.weight(1f)) { setup = true }
            if (on) Btn("إيقاف", Modifier.weight(1f), BtnKind.Danger) { disable = true }
        }
    }
    if (setup) LockSetupDialog(app) { setup = false }
    if (disable) {
        ConfirmDialog("سيتم إيقاف قفل التطبيق ويمكن لأي فاتح للجهاز الوصول إلى بياناتك.", "إيقاف القفل", onClose = { disable = false }) {
            Lock.disable(app.S)
            app.U()
        }
    }
}
