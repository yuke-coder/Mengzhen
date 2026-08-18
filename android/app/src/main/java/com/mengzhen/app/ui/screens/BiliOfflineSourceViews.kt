package com.mengzhen.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mengzhen.app.R
import com.mengzhen.app.bilibili.BiliCacheItem
import com.mengzhen.app.bilibili.BiliImportProgress
import java.util.Locale

private val BiliItemShape = RoundedCornerShape(6.dp)

@Composable
internal fun BiliOfflineToolbar(
    canEdit: Boolean,
    editMode: Boolean,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleColor = colorResource(R.color.theme_color_primary_tr_title)
    val iconColor = colorResource(R.color.theme_color_primary_tr_icon)
    val iconFont = remember { FontFamily(Font(R.font.bili_source_iconfont)) }

    Box(modifier = modifier.height(44.dp)) {
        Text(
            text = "已缓存视频",
            color = titleColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 120.dp),
        )

        if (editMode) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .clickable(onClick = onEdit)
                    .padding(horizontal = 12.dp)
                    .semantics {
                        contentDescription = "完成编辑"
                        role = Role.Button
                    },
            ) {
                Text(
                    text = "完成",
                    color = titleColor,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .padding(end = 12.dp),
            ) {
                BiliHeaderIcon(
                    glyph = "\uEAFE",
                    label = "搜索",
                    color = iconColor,
                    fontFamily = iconFont,
                    onClick = onSearch,
                )
                BiliHeaderIcon(
                    glyph = "\uEB16",
                    label = "设置",
                    color = iconColor,
                    fontFamily = iconFont,
                    onClick = onSettings,
                )
                if (canEdit) {
                    BiliHeaderIcon(
                        glyph = "\uEAF0",
                        label = "编辑",
                        color = iconColor,
                        fontFamily = iconFont,
                        onClick = onEdit,
                    )
                }
            }
        }
    }
}

@Composable
private fun BiliHeaderIcon(
    glyph: String,
    label: String,
    color: Color,
    fontFamily: FontFamily,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(24.dp)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = label
                role = Role.Button
            },
    ) {
        Text(
            text = glyph,
            color = color,
            fontFamily = fontFamily,
            fontSize = 24.sp,
            lineHeight = 24.sp,
        )
    }
}

@Composable
internal fun BiliOfflineSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val iconFont = remember { FontFamily(Font(R.font.bili_source_iconfont)) }
    val textColor = colorResource(R.color.Ga10)
    val hintColor = colorResource(R.color.Ga5)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.height(44.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colorResource(R.color.bili_offline_search_surface))
                .padding(horizontal = 10.dp),
        ) {
            Text(
                text = "\uEAFE",
                color = hintColor,
                fontFamily = iconFont,
                fontSize = 16.sp,
                lineHeight = 16.sp,
            )
            Spacer(modifier = Modifier.width(6.dp))
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                ),
                cursorBrush = SolidColor(colorResource(R.color.Pi5)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "搜索已下载的内容",
                                color = hintColor,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight()
                .clickable(onClick = onCancel)
                .padding(horizontal = 12.dp)
                .semantics {
                    contentDescription = "取消搜索"
                    role = Role.Button
                },
        ) {
            Text(
                text = "取消",
                color = textColor,
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }
    }
}

@Composable
internal fun BiliOfflineDownloadingItem(
    items: List<BiliCacheItem>,
    artworkLocation: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val first = items.first()
    val text1 = colorResource(R.color.Ga10)
    val text3 = colorResource(R.color.Ga5)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Text(
            text = "正在缓存",
            color = text1,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 12.dp, bottom = 4.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(102.dp)
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 139.dp, height = 78.dp)
                    .clip(BiliItemShape)
                    .background(colorResource(R.color.Ga2)),
            ) {
                AsyncImage(
                    model = artworkLocation,
                    contentDescription = first.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                ) {
                    Text(
                        text = "${items.size} 个内容",
                        color = Color.White,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.fillMaxHeight().weight(1f)) {
                Text(
                    text = first.title,
                    color = text1,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (first.subtitle.isNotBlank() && first.subtitle != first.title) {
                    Text(
                        text = first.subtitle,
                        color = colorResource(R.color.Ga8),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "正在缓存",
                        color = text3,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val size = items.sumOf(BiliCacheItem::audioSize)
                    if (size > 0L) {
                        Text(
                            text = formatBiliStorageSize(size),
                            color = text3,
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                        )
                    }
                }
                LinearProgressIndicator(
                    color = colorResource(R.color.theme_color_secondary),
                    trackColor = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BiliOfflineDownloadedItem(
    item: BiliCacheItem,
    artworkLocation: String?,
    selected: Boolean,
    extracted: Boolean,
    editMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text1 = colorResource(R.color.Ga10)
    val text2 = colorResource(R.color.Ga8)
    val text3 = colorResource(R.color.Ga5)
    val iconFont = remember { FontFamily(Font(R.font.bili_source_iconfont)) }
    val subtitle = item.subtitle.takeIf { it.isNotBlank() && it != item.title }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(102.dp)
            .combinedClickable(
                enabled = !extracted,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(top = 12.dp, end = 12.dp, bottom = 12.dp),
    ) {
        if (editMode) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .width(44.dp)
                    .fillMaxHeight()
                    .clickable(enabled = !extracted, onClick = onToggle),
            ) {
                if (!extracted) {
                    BiliSelectionMark(selected = selected)
                }
            }
        } else {
            Spacer(modifier = Modifier.width(12.dp))
        }

        Box(
            modifier = Modifier
                .size(width = 139.dp, height = 78.dp)
                .clip(BiliItemShape)
                .background(colorResource(R.color.Ga2)),
        ) {
            AsyncImage(
                model = artworkLocation,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.durationSeconds > 0L) {
                Text(
                    text = formatOfflineDuration(item.durationSeconds),
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                )
            }
            if (extracted) {
                Text(
                    text = "已提取",
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 3.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f),
        ) {
            Text(
                text = item.title,
                color = text1,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                minLines = 1,
                maxLines = if (subtitle == null) 2 else 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 20.dp),
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = text2,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.heightIn(min = 17.dp),
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (item.owner.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\uEB79",
                        color = text3,
                        fontFamily = iconFont,
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = item.owner,
                        color = text3,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (item.audioSize > 0L) {
                Text(
                    text = formatBiliStorageSize(item.audioSize),
                    color = text3,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BiliSelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    val idleColor = colorResource(R.color.Ga5)
    val selectedColor = colorResource(R.color.Pi5)
    Canvas(modifier = modifier.size(20.dp)) {
        if (selected) {
            drawCircle(color = selectedColor)
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.27f, size.height * 0.52f),
                end = Offset(size.width * 0.44f, size.height * 0.68f),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.44f, size.height * 0.68f),
                end = Offset(size.width * 0.75f, size.height * 0.34f),
                strokeWidth = 1.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
        } else {
            drawCircle(
                color = idleColor,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
    }
}

@Composable
internal fun BiliOfflineStorageBar(
    usedBytes: Long,
    availableBytes: Long,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.padding(vertical = 5.dp),
    ) {
        Text(
            text = storageSummary(usedBytes, availableBytes),
            color = colorResource(R.color.Ga5),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun BiliOfflineBottomBar(
    allSelected: Boolean,
    selectionEnabled: Boolean,
    selectedCount: Int,
    importing: Boolean,
    progress: BiliImportProgress?,
    onToggleAll: () -> Unit,
    onExtract: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionEnabled = selectedCount > 0 && !importing
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(48.dp)
            .background(colorResource(R.color.theme_color_primary_tr_background)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxHeight()
                .clickable(
                    enabled = selectionEnabled && !importing,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggleAll,
                )
                .padding(start = 14.dp, end = 12.dp),
        ) {
            BiliSelectionMark(selected = allSelected)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "全选",
                color = colorResource(R.color.Ga8),
                fontSize = 16.sp,
                lineHeight = 22.sp,
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight()
                .clickable(enabled = actionEnabled, onClick = onExtract)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = when {
                    importing && progress?.percent != null ->
                        "${progress.current}/${progress.total}  ${progress.percent}%"
                    importing -> "正在提取"
                    selectedCount > 0 -> "提取音频（$selectedCount）"
                    else -> "提取音频"
                },
                color = colorResource(
                    if (actionEnabled || importing) R.color.Pi5 else R.color.Ga4,
                ),
                fontSize = 16.sp,
                lineHeight = 22.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun BiliOfflineSearchSummary(
    query: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "共找到关于${query}的${count}个内容",
        color = colorResource(R.color.Ga5),
        fontSize = 12.sp,
        lineHeight = 17.sp,
        modifier = modifier.padding(start = 12.dp, top = 6.dp, end = 12.dp, bottom = 2.dp),
    )
}

@Composable
internal fun BiliOfflineEmpty(
    modifier: Modifier = Modifier,
    message: String = "这里还什么都没有呢～",
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(top = 72.dp, bottom = 32.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.img_holder_empty_style2),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            color = colorResource(R.color.Ga5),
            fontSize = 14.sp,
            lineHeight = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 320.dp).wrapContentWidth(),
        )
    }
}

private fun formatOfflineDuration(seconds: Long): String {
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remain = seconds % 60
    return if (hours > 0L) {
        String.format(Locale.CHINA, "%d:%02d:%02d", hours, minutes, remain)
    } else {
        String.format(Locale.CHINA, "%d:%02d", minutes, remain)
    }
}

private fun storageSummary(usedBytes: Long, availableBytes: Long): String =
    "已使用${formatBiliStorageSize(usedBytes)} / 剩余${formatBiliStorageSize(availableBytes)}"

private fun formatBiliStorageSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format(Locale.CHINA, "%.1fGB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> String.format(Locale.CHINA, "%.1fMB", bytes / 1_048_576.0)
    bytes >= 1_024L -> String.format(Locale.CHINA, "%.1fKB", bytes / 1_024.0)
    else -> "${bytes.coerceAtLeast(0L)}B"
}
