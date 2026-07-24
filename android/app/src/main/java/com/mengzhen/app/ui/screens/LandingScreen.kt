package com.mengzhen.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mengzhen.app.ui.components.home.*
import com.mengzhen.app.ui.navigation.Screen

/**
 * 首页 —— Web 端 page.tsx 移动端显示的像素级复刻
 *
 * 结构对齐：
 * - DynamicBackground（暗色星空 / 亮色滚动网格，主题驱动）
 * - HomeNavbar（fixed 顶栏：logo + 紫渐变"梦枕" + UserMenu + ThemeToggle）
 * - MobileHero（HeroBackdrop + HeroTitle 逐字 + WordReveal + HeroBadge + 权限设置按钮 + HeroCta + HeroChips）
 * - 15 个内容区（LazySection 静态内容，RevealGroup 入场）
 * - HomeFloatingBar（Hero 按钮与底部 CTA 均滚出视口时浮现）
 * - HomeFooter
 *
 * 跳转：全部 CTA → settings（router.replace 语义：popUpTo Landing inclusive）；
 * 权限设置按钮 → permission_settings（InstallButton 样式保留、逻辑已改）。
 */
@Composable
fun LandingScreen(navController: NavController) {
    // FloatingBar 可见性：Hero 按钮与底部 CTA 均滚出视口时显示（Web showFloatingBar）
    var heroCtaVisible by remember { mutableStateOf(true) }
    var bottomCtaVisible by remember { mutableStateOf(false) }
    val showFloatingBar = !heroCtaVisible && !bottomCtaVisible

    // router.replace("/settings") 语义：替换当前页，返回键不回首页
    val startExperience: () -> Unit = {
        navController.navigate(Screen.Settings.route) {
            popUpTo(Screen.Landing.route) { inclusive = true }
        }
    }

    val heroHeight = with(LocalConfiguration.current) { (screenHeightDp - 48).coerceAtLeast(480).dp }

    Box(Modifier.fillMaxSize()) {
        // 动态背景（星空 / 网格，主题驱动）
        DynamicBackground(Modifier.fillMaxSize())

        LazyColumn(Modifier.fillMaxSize()) {
            // MobileHero：min-h-[calc(100svh-56px)]，pt-4 pb-20，内容 max-w-[23rem] 居中 space-y-4
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(heroHeight)
                ) {
                    HeroBackdrop(Modifier.fillMaxSize())
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp + 16.dp, bottom = 80.dp), // 48dp navbar + pt-4
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        HeroTitle(Modifier.widthIn(max = 352.dp).fillMaxWidth())
                        WordReveal(
                            text = "原生构建·云端同步·本地持久化",
                            delayBase = 1200,
                            wordDelay = 200,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        RevealGroup(delayBase = 300) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                HeroBadge()
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    PermissionSetupButton(onClick = {
                                        navController.navigate(Screen.PermissionSettings.route)
                                    })
                                    HeroCta(
                                        onClick = startExperience,
                                        modifier = Modifier.viewportVisibilityTracker { heroCtaVisible = it },
                                    )
                                }
                                HeroChips()
                            }
                        }
                    }
                }
            }

            // 15 个内容区（LazySection 静态内容照搬）
            item { FeaturesSection() }
            item { AudienceSection() }
            item { DisplayModeSection() }
            item { PainPointsSection() }
            item { TemplatesSection() }
            item { ThreeStepsSection() }
            item {
                StartSection(
                    onStart = startExperience,
                    onCtaPositioned = { bottomCtaVisible = it },
                )
            }
            item { HomeFooter() }
        }

        // fixed 顶栏（z-9999）
        HomeNavbar(navController, Modifier.align(Alignment.TopCenter))

        // FloatingBar（z-500）
        HomeFloatingBar(
            visible = showFloatingBar,
            onStart = startExperience,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
