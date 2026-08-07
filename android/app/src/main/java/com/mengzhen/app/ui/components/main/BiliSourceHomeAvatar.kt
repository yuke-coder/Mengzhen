package com.mengzhen.app.ui.components.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mengzhen.app.R
import com.mengzhen.app.data.model.UserInfo

/** Bilibili 9.5.0 HomeTopLeft.kt AvatarLayout/AvatarBox source port. */
@Composable
internal fun BiliSourceHomeAvatar(
    user: UserInfo?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loggedIn = user != null
    val avatarUrl = user?.avatarUrl
    Box(
        modifier = modifier
            .wrapContentSize()
            .semantics {
                contentDescription = if (loggedIn) "我的" else "登录"
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (!loggedIn) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.bili_nologin_avatar),
                    contentDescription = "登录",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else if (avatarUrl.isNullOrBlank()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                )
            } else {
                AsyncImage(
                    model = absoluteAvatarUrl(avatarUrl),
                    contentDescription = "我的",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        }
    }
}
