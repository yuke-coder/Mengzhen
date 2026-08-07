package com.mengzhen.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.mengzhen.app.R
import com.mengzhen.app.data.api.ApiClient
import com.mengzhen.app.data.model.PlaybackDraft
import com.mengzhen.app.data.store.TaskStore
import com.mengzhen.app.scheduler.QuickPlaybackSessionFactory
import com.mengzhen.app.ui.components.AudioUploadSection
import com.mengzhen.app.ui.components.main.XimalayaSourceHomeTopBar
import com.mengzhen.app.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val store = remember { TaskStore.get(context) }
    val user by store.sessionUser.collectAsState()
    var draft by remember { mutableStateOf(store.getDraft()) }

    fun openIdlePlayer(playerDraft: PlaybackDraft) {
        val session = QuickPlaybackSessionFactory.createIdle(
            id = QuickPlaybackSessionFactory.newId(),
            draft = playerDraft,
        )
        QuickPlaybackSessionFactory.save(store, session)
        navController.navigate(Screen.Templates.createRoute(session.id))
    }

    Scaffold(
        topBar = {
            XimalayaSourceHomeTopBar(
                navController = navController,
                onSearch = { navController.navigate(Screen.AudioSearch.createRoute()) },
                onVoiceSearch = {
                    navController.navigate(Screen.AudioSearch.createRoute(voice = true))
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isLoggedIn = user != null
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        AudioUploadSection(
                            draft = draft,
                            onDraftChange = {
                                draft = it
                                store.saveDraft(it)
                            },
                            onSelectionReady = { updatedDraft, selectedAudios ->
                                draft = updatedDraft
                                openIdlePlayer(updatedDraft.copy(audios = selectedAudios))
                            },
                            onOpenAudio = { audio ->
                                openIdlePlayer(draft.copy(audios = listOf(audio)))
                            },
                            onOpenBiliCache = {
                                navController.navigate(Screen.BiliCache.route)
                            },
                            api = ApiClient.get(context),
                            isLoggedIn = isLoggedIn,
                        )
                    }
                }
                item {
                    AppSettingsEntry(
                        onClick = { navController.navigate(Screen.AppSettings.route) },
                    )
                }
                item {
                    BiliHomeAvatarSettingsSection(
                        onClick = {
                            navController.navigate(Screen.HomeAvatarDestination.route)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppSettingsEntry(onClick: () -> Unit) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        factory = { context ->
            android.view.LayoutInflater.from(context)
                .inflate(R.layout.ximalaya_settings_permission_entries, null, false)
        },
        update = { root ->
            root.findViewById<android.view.View>(R.id.main_app_setting)
                .setOnClickListener { onClick() }
        },
    )
}
