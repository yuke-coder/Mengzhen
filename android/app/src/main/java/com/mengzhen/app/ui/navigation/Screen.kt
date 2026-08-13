package com.mengzhen.app.ui.navigation

sealed class Screen(val route: String) {
    object Landing : Screen("landing")
    object Settings : Screen("settings")
    object AudioSearch : Screen("audio_search?voice={voice}") {
        const val ARG_VOICE = "voice"
        fun createRoute(voice: Boolean = false): String = "audio_search?voice=$voice"
    }
    object BiliCache : Screen("bili_cache")
    object Tasks : Screen("tasks")
    object Login : Screen("login")
    object History : Screen("history")
    object Templates : Screen(
        "templates/{taskId}?openTimer={openTimer}&timerMode={timerMode}"
    ) {
        const val ARG_TASK_ID = "taskId"
        const val ARG_OPEN_TIMER = "openTimer"
        const val ARG_TIMER_MODE = "timerMode"
        const val TIMER_MODE_FULL = "full"
        const val TIMER_MODE_COMBINED = "combined"
        fun createRoute(
            taskId: String,
            openTimer: Boolean = false,
            timerMode: String = TIMER_MODE_FULL,
        ): String =
            "templates/${android.net.Uri.encode(taskId)}" +
                "?openTimer=$openTimer&timerMode=${android.net.Uri.encode(timerMode)}"
    }
    object SoundDetails : Screen("sound_details/{taskId}/{audioIndex}") {
        const val ARG_TASK_ID = "taskId"
        const val ARG_AUDIO_INDEX = "audioIndex"
        fun createRoute(taskId: String, audioIndex: Int): String =
            "sound_details/${android.net.Uri.encode(taskId)}/${audioIndex.coerceAtLeast(0)}"
    }
    object AlarmManager : Screen("alarm_manager/{taskId}") {
        const val ARG_TASK_ID = "taskId"
        fun createRoute(taskId: String): String =
            "alarm_manager/${android.net.Uri.encode(taskId)}"
    }
    object AlarmEditor : Screen(
        "alarm_editor/{taskId}?newAlarm={newAlarm}&openTimerAfterSave={openTimerAfterSave}"
    ) {
        const val ARG_TASK_ID = "taskId"
        const val ARG_NEW_ALARM = "newAlarm"
        const val ARG_OPEN_TIMER_AFTER_SAVE = "openTimerAfterSave"
        fun createRoute(
            taskId: String,
            newAlarm: Boolean = false,
            openTimerAfterSave: Boolean = false,
        ): String =
            "alarm_editor/${android.net.Uri.encode(taskId)}" +
                "?newAlarm=$newAlarm&openTimerAfterSave=$openTimerAfterSave"
    }
    object AlarmRing : Screen("alarm_ring/{taskId}") {
        const val ARG_TASK_ID = "taskId"
        fun createRoute(taskId: String): String =
            "alarm_ring/${android.net.Uri.encode(taskId)}"
    }
    object AlarmHelp : Screen("alarm_help")
    object Profile : Screen("profile")
    object ProfileEdit : Screen("profile_edit")
    object AppSettings : Screen("app_settings")
    object PrivacySettings : Screen("privacy_settings")
    object NotificationSettings : Screen("notification_settings")
    object HomeAvatarDestination : Screen("home_avatar_destination")
    object Feedback : Screen("feedback")
    object FeedbackChooseType : Screen("feedback_choose_type")
    object FeedbackDetail : Screen("feedback_detail?feedbackType={feedbackType}") {
        const val ARG_FEEDBACK_TYPE = "feedbackType"
        fun createRoute(feedbackType: String): String =
            "feedback_detail?feedbackType=${android.net.Uri.encode(feedbackType)}"
    }
    object FeedbackSuccess : Screen("feedback_success")
    object FeedbackHistory : Screen("feedback_history")
    object FeedbackRecord : Screen("feedback_record/{feedbackId}") {
        const val ARG_FEEDBACK_ID = "feedbackId"
        fun createRoute(feedbackId: String): String =
            "feedback_record/${android.net.Uri.encode(feedbackId)}"
    }
    object FeedbackRecordInfo : Screen("feedback_record_info/{feedbackId}") {
        const val ARG_FEEDBACK_ID = "feedbackId"
        fun createRoute(feedbackId: String): String =
            "feedback_record_info/${android.net.Uri.encode(feedbackId)}"
    }
    object PermissionSettings : Screen("permission_settings?fromAlarm={fromAlarm}") {
        const val ARG_FROM_ALARM = "fromAlarm"
        fun createRoute(fromAlarm: Boolean = false): String =
            "permission_settings?fromAlarm=$fromAlarm"
    }
    object PermissionTutorial : Screen("permission_tutorial/{permissionKey}") {
        fun createRoute(permissionKey: com.mengzhen.app.data.tutorial.PermissionKey) =
            "permission_tutorial/${permissionKey.prefKey}"
    }
    object SystemPermissions : Screen("system_permissions")
    object AccountSafety : Screen("account_safety")
    object ListeningPreference : Screen("listening_preference")
    object LockScreenSettings : Screen("lock_screen_settings")
}