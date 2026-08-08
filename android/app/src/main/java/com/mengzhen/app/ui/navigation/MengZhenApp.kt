package com.mengzhen.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mengzhen.app.ui.components.main.XimalayaSourceBottomNavigation
import com.mengzhen.app.ui.feedback.AppNoticeHost
import com.mengzhen.app.ui.screens.BiliCacheScreen
import com.mengzhen.app.ui.screens.BiliHomeAvatarDestinationScreen
import com.mengzhen.app.ui.screens.XimalayaAppSettingsScreen
import com.mengzhen.app.ui.screens.XimalayaNotificationSettingsScreen
import com.mengzhen.app.ui.screens.XimalayaPrivacySettingsScreen
import com.mengzhen.app.ui.screens.FeedbackScreen
import com.mengzhen.app.ui.screens.XimalayaFeedbackDetailScreen
import com.mengzhen.app.ui.screens.XimalayaFeedbackChooseTypeScreen
import com.mengzhen.app.ui.screens.XimalayaFeedbackHistoryScreen
import com.mengzhen.app.ui.screens.XimalayaFeedbackRecordInfoScreen
import com.mengzhen.app.ui.screens.XimalayaFeedbackRecordScreen
import com.mengzhen.app.ui.screens.XimalayaFeedbackSuccessScreen
import com.mengzhen.app.ui.screens.HistoryScreen
import com.mengzhen.app.ui.screens.LandingScreen
import com.mengzhen.app.ui.screens.LoginScreen
import com.mengzhen.app.ui.screens.PermissionSettingsScreen
import com.mengzhen.app.ui.screens.PermissionTutorialScreen
import com.mengzhen.app.ui.screens.QqMusicPersonalInfoScreen
import com.mengzhen.app.ui.screens.SettingsScreen
import com.mengzhen.app.ui.screens.TemplatesScreen
import com.mengzhen.app.ui.screens.XimalayaAlarmEditorScreen
import com.mengzhen.app.ui.screens.XimalayaAlarmHelpScreen
import com.mengzhen.app.ui.screens.XimalayaAlarmManagerScreen
import com.mengzhen.app.ui.screens.XimalayaAlarmRingScreen
import com.mengzhen.app.ui.screens.XimalayaAudioSearchScreen
import com.mengzhen.app.ui.screens.XimalayaSoundDetailsScreen
import com.mengzhen.app.ui.screens.XimalayaSystemPermissionScreen

private val topLevelRoutes = setOf(
    Screen.Settings.route,
    Screen.BiliCache.route,
    Screen.Tasks.route,
    Screen.Profile.route,
)

@Composable
fun MengZhenApp(
    showLandingOnStart: Boolean = false,
    onLandingCompleted: () -> Unit = {},
    openPlaybackTaskId: String? = null,
    onPlaybackNavigationConsumed: () -> Unit = {},
    sharedBiliVideo: String? = null,
    onSharedBiliVideoConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val shellRoute = if (currentRoute == Screen.Feedback.route) {
        navController.previousBackStackEntry?.destination?.route
    } else {
        currentRoute
    }

    LaunchedEffect(openPlaybackTaskId, showLandingOnStart) {
        if (showLandingOnStart) return@LaunchedEffect
        val taskId = openPlaybackTaskId ?: return@LaunchedEffect
        navController.navigate(Screen.Templates.createRoute(taskId)) {
            launchSingleTop = true
        }
        onPlaybackNavigationConsumed()
    }

    LaunchedEffect(sharedBiliVideo, showLandingOnStart) {
        if (showLandingOnStart) return@LaunchedEffect
        if (sharedBiliVideo.isNullOrBlank()) return@LaunchedEffect
        navController.navigate(Screen.BiliCache.route) {
            launchSingleTop = true
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (shellRoute in topLevelRoutes) {
                XimalayaSourceBottomNavigation(
                    navController = navController,
                    currentRoute = shellRoute,
                )
            }
        },
    ) { shellPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(shellPadding)
                .consumeWindowInsets(shellPadding),
        ) {
            NavHost(
                navController = navController,
                startDestination = if (showLandingOnStart) {
                    Screen.Landing.route
                } else {
                    Screen.Settings.route
                },
            ) {
                composable(Screen.Landing.route) {
                    LandingScreen(
                        navController = navController,
                        onExperienceStarted = onLandingCompleted,
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(navController)
                }
                composable(
                    route = Screen.AudioSearch.route,
                    arguments = listOf(
                        navArgument(Screen.AudioSearch.ARG_VOICE) {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { entry ->
                    XimalayaAudioSearchScreen(
                        navController = navController,
                        startVoiceInput = entry.arguments
                            ?.getBoolean(Screen.AudioSearch.ARG_VOICE)
                            ?: false,
                    )
                }
                composable(Screen.BiliCache.route) {
                    BiliCacheScreen(
                        navController = navController,
                        sharedVideo = sharedBiliVideo,
                        onSharedVideoConsumed = onSharedBiliVideoConsumed,
                        topLevel = true,
                    )
                }
                composable(Screen.Tasks.route) {
                    XimalayaAlarmManagerScreen(
                        navController = navController,
                        topLevel = true,
                    )
                }
                composable(Screen.Login.route) {
                    LoginScreen(navController)
                }
                composable(Screen.History.route) {
                    HistoryScreen(navController, topLevel = true)
                }
                composable(
                    route = Screen.Templates.route,
                    arguments = listOf(
                        navArgument(Screen.Templates.ARG_TASK_ID) {
                            type = NavType.StringType
                        },
                        navArgument(Screen.Templates.ARG_OPEN_TIMER) {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                        navArgument(Screen.Templates.ARG_TIMER_MODE) {
                            type = NavType.StringType
                            defaultValue = Screen.Templates.TIMER_MODE_FULL
                        },
                    ),
                ) { entry ->
                    TemplatesScreen(
                        navController = navController,
                        taskId = entry.arguments
                            ?.getString(Screen.Templates.ARG_TASK_ID)
                            .orEmpty(),
                        openTimer = entry.arguments
                            ?.getBoolean(Screen.Templates.ARG_OPEN_TIMER)
                            ?: false,
                        timerMode = entry.arguments
                            ?.getString(Screen.Templates.ARG_TIMER_MODE)
                            ?: Screen.Templates.TIMER_MODE_FULL,
                    )
                }
                composable(
                    route = Screen.SoundDetails.route,
                    arguments = listOf(
                        navArgument(Screen.SoundDetails.ARG_TASK_ID) {
                            type = NavType.StringType
                        },
                        navArgument(Screen.SoundDetails.ARG_AUDIO_INDEX) {
                            type = NavType.IntType
                        },
                    ),
                ) { entry ->
                    XimalayaSoundDetailsScreen(
                        navController = navController,
                        taskId = entry.arguments
                            ?.getString(Screen.SoundDetails.ARG_TASK_ID)
                            .orEmpty(),
                        audioIndex = entry.arguments
                            ?.getInt(Screen.SoundDetails.ARG_AUDIO_INDEX)
                            ?: 0,
                    )
                }
                composable(
                    route = Screen.AlarmManager.route,
                    arguments = listOf(
                        navArgument(Screen.AlarmManager.ARG_TASK_ID) {
                            type = NavType.StringType
                        },
                    ),
                ) { entry ->
                    XimalayaAlarmManagerScreen(
                        navController = navController,
                        taskId = entry.arguments
                            ?.getString(Screen.AlarmManager.ARG_TASK_ID)
                            .orEmpty(),
                    )
                }
                composable(
                    route = Screen.AlarmEditor.route,
                    arguments = listOf(
                        navArgument(Screen.AlarmEditor.ARG_TASK_ID) {
                            type = NavType.StringType
                        },
                        navArgument(Screen.AlarmEditor.ARG_NEW_ALARM) {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                        navArgument(Screen.AlarmEditor.ARG_OPEN_TIMER_AFTER_SAVE) {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { entry ->
                    XimalayaAlarmEditorScreen(
                        navController = navController,
                        taskId = entry.arguments
                            ?.getString(Screen.AlarmEditor.ARG_TASK_ID)
                            .orEmpty(),
                        discardOnCancel = entry.arguments
                            ?.getBoolean(Screen.AlarmEditor.ARG_NEW_ALARM)
                            ?: false,
                        openTimerAfterSave = entry.arguments
                            ?.getBoolean(Screen.AlarmEditor.ARG_OPEN_TIMER_AFTER_SAVE)
                            ?: false,
                    )
                }
                composable(
                    route = Screen.AlarmRing.route,
                    arguments = listOf(
                        navArgument(Screen.AlarmRing.ARG_TASK_ID) {
                            type = NavType.StringType
                        },
                    ),
                ) { entry ->
                    XimalayaAlarmRingScreen(
                        navController = navController,
                        taskId = entry.arguments
                            ?.getString(Screen.AlarmRing.ARG_TASK_ID)
                            .orEmpty(),
                    )
                }
                composable(Screen.AlarmHelp.route) {
                    XimalayaAlarmHelpScreen(navController)
                }
                composable(Screen.Profile.route) {
                    QqMusicPersonalInfoScreen(navController)
                }
                composable(Screen.AppSettings.route) {
                    XimalayaAppSettingsScreen(navController)
                }
                composable(Screen.PrivacySettings.route) {
                    XimalayaPrivacySettingsScreen(navController)
                }
                composable(Screen.NotificationSettings.route) {
                    XimalayaNotificationSettingsScreen(navController)
                }
                composable(Screen.HomeAvatarDestination.route) {
                    BiliHomeAvatarDestinationScreen(navController)
                }
                dialog(
                    route = Screen.Feedback.route,
                    dialogProperties = DialogProperties(
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true,
                        usePlatformDefaultWidth = false,
                    ),
                ) {
                    FeedbackScreen(navController)
                }
                composable(Screen.FeedbackChooseType.route) {
                    XimalayaFeedbackChooseTypeScreen(navController)
                }
                composable(
                    route = Screen.FeedbackDetail.route,
                    arguments = listOf(
                        navArgument(Screen.FeedbackDetail.ARG_FEEDBACK_TYPE) {
                            type = NavType.StringType
                            defaultValue = ""
                        },
                    ),
                ) { entry ->
                    XimalayaFeedbackDetailScreen(
                        navController = navController,
                        feedbackType = entry.arguments
                            ?.getString(Screen.FeedbackDetail.ARG_FEEDBACK_TYPE)
                            .orEmpty(),
                    )
                }
                composable(Screen.FeedbackSuccess.route) {
                    XimalayaFeedbackSuccessScreen(navController)
                }
                composable(Screen.FeedbackHistory.route) {
                    XimalayaFeedbackHistoryScreen(navController)
                }
                composable(
                    route = Screen.FeedbackRecord.route,
                    arguments = listOf(
                        navArgument(Screen.FeedbackRecord.ARG_FEEDBACK_ID) {
                            type = NavType.StringType
                        },
                    ),
                ) { entry ->
                    XimalayaFeedbackRecordScreen(
                        navController = navController,
                        feedbackId = entry.arguments
                            ?.getString(Screen.FeedbackRecord.ARG_FEEDBACK_ID)
                            .orEmpty(),
                    )
                }
                composable(
                    route = Screen.FeedbackRecordInfo.route,
                    arguments = listOf(
                        navArgument(Screen.FeedbackRecordInfo.ARG_FEEDBACK_ID) {
                            type = NavType.StringType
                        },
                    ),
                ) { entry ->
                    XimalayaFeedbackRecordInfoScreen(
                        navController = navController,
                        feedbackId = entry.arguments
                            ?.getString(Screen.FeedbackRecordInfo.ARG_FEEDBACK_ID)
                            .orEmpty(),
                    )
                }
                composable(
                    route = Screen.PermissionSettings.route,
                    arguments = listOf(
                        navArgument(Screen.PermissionSettings.ARG_FROM_ALARM) {
                            type = NavType.BoolType
                            defaultValue = false
                        },
                    ),
                ) { entry ->
                    PermissionSettingsScreen(
                        navController = navController,
                        fromAlarm = entry.arguments
                            ?.getBoolean(Screen.PermissionSettings.ARG_FROM_ALARM)
                            ?: false,
                    )
                }
                composable(Screen.PermissionTutorial.route) { entry ->
                    PermissionTutorialScreen(
                        navController = navController,
                        permissionKey = entry.arguments
                            ?.getString("permissionKey")
                            .orEmpty(),
                    )
                }
                composable(Screen.SystemPermissions.route) {
                    XimalayaSystemPermissionScreen(navController)
                }
            }
            AppNoticeHost()
        }
    }
}
