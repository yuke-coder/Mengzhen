package com.mengzhen.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.mengzhen.app.compat.VendorCompat
import com.mengzhen.app.data.tutorial.CompletionRule
import com.mengzhen.app.data.tutorial.PermissionKey
import com.mengzhen.app.data.tutorial.TutorialBrand
import com.mengzhen.app.data.tutorial.TutorialFunnel
import com.mengzhen.app.data.tutorial.TutorialRepository
import com.mengzhen.app.ui.components.permission.*
import com.mengzhen.app.ui.navigation.Screen
import com.mengzhen.app.ui.theme.BrandEnd
import com.mengzhen.app.ui.theme.BrandStart
import kotlinx.coroutines.launch

/**
 * 权限教程页（设计 §3.2）
 *
 * - 一个教程页只讲一个权限项；品牌自动检测，可切换（权重顺序 BottomSheet）
 * - 步骤勾选 DataStore 持久化；onResume 自动滚动并高亮首个未勾步骤
 * - 底部「前往设置」仅系统直达项显示（battery_optimization / close_power_save）
 * - 应急出口零打扰返回首页继续播放
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionTutorialScreen(
    navController: NavController,
    permissionKey: String,
) {
    val permission = PermissionKey.fromPrefKey(permissionKey) ?: run {
        navController.popBackStack()
        return
    }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 品牌：自动检测 + 可切换
    val detectedBrand = remember { TutorialRepository.mapBrand(VendorCompat.detectBrand()) }
    var brand by rememberSaveable { mutableStateOf(detectedBrand.name) }
    val tutorialBrand = TutorialBrand.valueOf(brand)
    val content = remember(tutorialBrand, permission) {
        TutorialRepository.getContent(tutorialBrand, permission)
    }

    // ---- 勾选状态（一次性加载 + 本地即时更新 + 持久化） ----
    var checkedStates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var stepsLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(tutorialBrand, permission) {
        stepsLoaded = false
        TutorialFunnel.trackEnter(context, tutorialBrand, permission) // 漏斗埋点：进入教程页
        val map = mutableMapOf<String, Boolean>()
        content?.cards?.forEach { card ->
            card.steps.forEach { step ->
                map[stepStateKey(card.id, step.index)] = TutorialRepository.isStepChecked(
                    context, tutorialBrand, permission, card.id, step.index
                )
            }
        }
        checkedStates = map
        stepsLoaded = true
    }

    // 首个未勾步（高亮 + 自动滚动目标）
    // ANY 规则教程（方案/路径二选一）：任一卡已做完即不再高亮剩余卡，避免误导"两条路径都要做"
    val nextUncheckedKey = run {
        val c = content
        val anyCardDone = c?.cards?.any { card ->
            card.steps.all { checkedStates[stepStateKey(card.id, it.index)] == true }
        } == true
        if (c?.completionRule == CompletionRule.ANY && anyCardDone) {
            null
        } else {
            c?.cards?.firstNotNullOfOrNull { card ->
                card.steps.firstOrNull { checkedStates[stepStateKey(card.id, it.index)] != true }
                    ?.let { stepStateKey(card.id, it.index) }
            }
        }
    }

    // ---- onResume 回检（批次三）：从系统设置返回时重新定位首个未勾步 ----
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTrigger by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTrigger++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // ---- 自动滚动到首个未勾步 ----
    // 触发时机：首次加载完成 或 每次 ON_RESUME（页面内勾选不触发，避免打断阅读）
    val scrollState = rememberScrollState()
    var columnRootTop by remember(tutorialBrand, permission) { mutableFloatStateOf(-1f) }
    var highlightRootTop by remember(tutorialBrand, permission) { mutableFloatStateOf(-1f) }
    var didAutoScroll by remember(tutorialBrand, permission) { mutableStateOf(false) }
    var lastResumeScrolled by remember(tutorialBrand, permission) { mutableIntStateOf(-1) }
    LaunchedEffect(stepsLoaded, highlightRootTop, columnRootTop, resumeTrigger) {
        // nextUncheckedKey == null（全部勾完）时不滚——highlightRootTop 是过期坐标
        if (!stepsLoaded || nextUncheckedKey == null || highlightRootTop <= 0f || columnRootTop <= 0f) return@LaunchedEffect
        val firstScroll = !didAutoScroll
        val resumeScroll = resumeTrigger > 0 && resumeTrigger != lastResumeScrolled
        if (!firstScroll && !resumeScroll) return@LaunchedEffect
        didAutoScroll = true
        lastResumeScrolled = resumeTrigger
        val target = (highlightRootTop - columnRootTop).toInt() - 80
        scrollState.animateScrollTo(target.coerceAtLeast(0))
    }

    fun toggleStep(cardId: String, stepIndex: Int, checked: Boolean) {
        checkedStates = checkedStates + (stepStateKey(cardId, stepIndex) to checked)
        scope.launch {
            TutorialRepository.setStepChecked(context, tutorialBrand, permission, cardId, stepIndex, checked)
        }
    }

    // 「前往设置」仅系统直达项显示
    val systemSettingAction: (() -> Unit)? = when (permission) {
        PermissionKey.BATTERY_OPTIMIZATION -> {
            { (context as? Activity)?.let { VendorCompat.openBatteryOptimizationSettings(it) } }
        }
        PermissionKey.CLOSE_POWER_SAVE -> {
            { (context as? Activity)?.let { VendorCompat.openPowerSaveModeSettings(it) } }
        }
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(permission.title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
            ) {
                // 主按钮「前往设置」（品牌渐变，52dp）
                if (systemSettingAction != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(BrandStart, BrandEnd)))
                            .clickable(onClick = systemSettingAction),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "前往设置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                // 「完成设置，返回」（有前往设置时 Outlined，否则升级为主按钮）
                if (systemSettingAction != null) {
                    OutlinedButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.primary
                        ),
                    ) {
                        Text(
                            "完成设置，返回",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(BrandStart, BrandEnd)))
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "完成设置，返回",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (content == null) {
            // 防御性空态（正常流程列表层已过滤无内容组合）
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "该品牌暂无此教程，切换品牌试试",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .onGloballyPositioned { columnRootTop = it.boundsInRoot().top }
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // 品牌识别条 + 切换
            BrandBadge(
                currentBrand = tutorialBrand,
                onBrandChange = { newBrand ->
                    brand = newBrand.name
                    scope.launch { scrollState.scrollTo(0) }
                },
            )

            // 应急出口（埋点：睡前流失关键节点）
            EmergencyExit(onClick = {
                scope.launch { TutorialFunnel.trackEmergencyExit(context) }
                navController.popBackStack(Screen.Landing.route, inclusive = false)
            })

            Spacer(Modifier.height(4.dp))

            // 为什么需要
            WhyNeedCard(content.whyNeed)

            Spacer(Modifier.height(24.dp))

            // 操作步骤标题区（多卡时带进度指示）
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "操作步骤",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (content.cards.size > 1) {
                    Spacer(Modifier.width(10.dp))
                    CardProgressRow(content.cards, checkedStates)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 步骤时间线（小卡模式 + 勾选）
            StepTimeline(
                cards = content.cards,
                checkedStates = checkedStates,
                nextUncheckedKey = if (stepsLoaded) nextUncheckedKey else null,
                onStepToggle = ::toggleStep,
                onHighlightPositioned = { y -> highlightRootTop = y },
            )

            // 跨步骤注意事项
            content.notice?.let {
                Spacer(Modifier.height(20.dp))
                NoticeCard(it)
            }

            // 备选路径
            content.alternatePath?.let {
                Spacer(Modifier.height(16.dp))
                AlternateCard(it)
            }

            // 兜底提示
            FallbackTip(content.searchKeyword)

            Spacer(Modifier.height(24.dp))
        }
    }
}
