package com.mizanati.invest.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TAB_LABELS = listOf("المحافظ", "التقارير والأدوات", "الحرية المالية", "الإعدادات")
private val TAB_ICONS = listOf(MzIcons.wallet, MzIcons.reports, MzIcons.freedom, MzIcons.settings)

@Composable
fun AppShell(
    dark: Boolean,
    tab: Int,
    investorName: String,
    onToggleTheme: () -> Unit,
    onTab: (Int) -> Unit,
    onQuickAdd: () -> Unit,
    pane: @Composable (Int) -> Unit,
) {
    val colors = if (dark) DarkColors else LightColors
    CompositionLocalProvider(
        LocalMz provides colors,
        LocalLayoutDirection provides LayoutDirection.Rtl,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    if (colors.dark) {
                        drawRect(colors.bg)
                        drawRect(
                            Brush.radialGradient(
                                listOf(Color(0x26635BFF), Color.Transparent),
                                center = Offset(size.width, 0f),
                                radius = size.maxDimension * .38f,
                            ),
                        )
                        drawRect(
                            Brush.radialGradient(
                                listOf(Color(0x1410B981), Color.Transparent),
                                center = Offset(0f, size.height * .9f),
                                radius = size.maxDimension * .34f,
                            ),
                        )
                    } else {
                        drawRect(Color.White)
                    }
                },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .verticalScroll(rememberScrollState())
                    .padding(PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 96.dp)),
            ) {
                TopBar(investorName, onToggleTheme)
                Spacer(Modifier.height(12.dp))
                pane(tab)
            }
            QuickAddFab(
                onQuickAdd,
                Modifier
                    .align(BiasAbsoluteAlignment(-1f, 1f))
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(start = 13.dp, end = 13.dp, bottom = 78.dp),
            )
            BottomNav(tab, onTab, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun TopBar(investorName: String, onToggleTheme: () -> Unit) {
    val mz = LocalMz.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val logoShape = RoundedCornerShape(16.dp)
            Box(
                Modifier
                    .shadow(10.dp, logoShape, ambientColor = Color(0x5A4F46E5), spotColor = Color(0x5A4F46E5))
                    .size(48.dp)
                    .background(
                        cssGradient(
                            145f,
                            0f to Color(0xFF4F46E5),
                            .55f to Color(0xFF7C3AED),
                            1f to Color(0xFF06B6D4),
                        ),
                        logoShape,
                    ),
                contentAlignment = Alignment.Center,
            ) { LogoMark(30.dp) }
            Column {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "ميزانيتي",
                        style = TextStyle(
                            brush = Brush.horizontalGradient(listOf(Color(0xFF635BFF), Color(0xFF06B6D4))),
                            fontSize = 27.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-.5).sp,
                            lineHeight = 30.sp,
                        ),
                    )
                    Text(
                        "INVEST",
                        Modifier
                            .border(1.dp, Color(0x55635BFF), CircleShape)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        color = mz.p,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.6.sp,
                        lineHeight = 12.sp,
                    )
                }
                Text(
                    if (investorName.isNotEmpty()) "أهلاً $investorName · ثروتك تحت السيطرة"
                    else "ثروتك تحت السيطرة · أرقام واضحة وقرارات أذكى",
                    Modifier.padding(top = 3.dp),
                    color = mz.m,
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        val btnShape = RoundedCornerShape(14.dp)
        val ring = if (mz.dark) {
            cssGradient(135f, 0f to Color(0xFF9B95FF), .55f to Color(0xFF42DCB2), 1f to Color(0xFF38BDF8))
        } else {
            cssGradient(135f, 0f to Color(0xFF635BFF), .55f to Color(0xFF06B6D4), 1f to Color(0xFF10B981))
        }
        Box(
            Modifier
                .shadow(
                    5.dp,
                    btnShape,
                    ambientColor = if (mz.dark) Color(0x55000000) else Color(0x20635BFF),
                    spotColor = if (mz.dark) Color(0x55000000) else Color(0x20635BFF),
                )
                .size(44.dp)
                .clip(btnShape)
                .background(mz.c)
                .border(1.5.dp, ring, btnShape)
                .clickable(role = Role.Button, onClick = onToggleTheme)
                .semantics { contentDescription = "تبديل الوضع" },
            contentAlignment = Alignment.Center,
        ) {
            SvgIcon(MzIcons.moon, 20.dp, if (mz.dark) Color(0xFFD4D0FF) else Color(0xFF5146D8))
        }
    }
}

@Composable
private fun BottomNav(tab: Int, onTab: (Int) -> Unit, modifier: Modifier = Modifier) {
    val mz = LocalMz.current
    Column(modifier.fillMaxWidth().background(mz.c.copy(alpha = .92f))) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(mz.b))
        Row(
            Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            TAB_LABELS.forEachIndexed { index, label ->
                val on = index == tab
                val shape = RoundedCornerShape(14.dp)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier
                            .widthIn(max = 170.dp)
                            .fillMaxWidth()
                            .clip(shape)
                            .background(if (on) mz.c2 else Color.Transparent)
                            .clickable(role = Role.Tab) { onTab(index) }
                            .padding(horizontal = 6.dp, vertical = 9.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val frame = RoundedCornerShape(12.dp)
                        Box(
                            Modifier
                                .padding(bottom = 1.dp)
                                .size(34.dp)
                                .then(
                                    if (on) {
                                        Modifier.background(
                                            cssGradient(145f, 0f to Color(0xFF635BFF), 1f to Color(0xFF6D5EFC)),
                                            frame,
                                        )
                                    } else {
                                        Modifier
                                            .background(mz.c, frame)
                                            .border(
                                                1.5.dp,
                                                if (mz.dark) {
                                                    cssGradient(145f, 0f to Color(0xFF8B83FF), 1f to Color(0xFF3DDC97))
                                                } else {
                                                    cssGradient(
                                                        145f,
                                                        0f to Color(0xFFC4B5FD),
                                                        .55f to Color(0xFF67E8F9),
                                                        1f to Color(0xFF6EE7B7),
                                                    )
                                                },
                                                frame,
                                            )
                                    },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            SvgIcon(
                                TAB_ICONS[index],
                                18.dp,
                                if (on) Color.White else mz.p,
                                fillLastPath = on && index != 3,
                            )
                        }
                        Text(
                            label,
                            color = if (on) mz.p else mz.m,
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(
                cssGradient(
                    135f,
                    0f to Color(0x266557E8),
                    .48f to Color(0x2D4F67E8),
                    1f to Color(0x2610A878),
                ),
            )
            .border(1.dp, Color(0x55FFFFFF), CircleShape)
            .clickable(interaction, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "إضافة عملية جديدة" },
        contentAlignment = Alignment.Center,
    ) {
        Text("＋", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
    }
}
