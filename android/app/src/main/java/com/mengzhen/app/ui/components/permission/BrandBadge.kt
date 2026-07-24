package com.mengzhen.app.ui.components.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mengzhen.app.data.tutorial.TutorialBrand
import com.mengzhen.app.data.tutorial.TutorialRepository

/**
 * BrandBadge 品牌识别条（设计 §4.2）
 * 左侧：品牌 Chip + "已按你的设备自动匹配"；右侧：「切换品牌」TextButton。
 * 点击弹出 ModalBottomSheet，品牌顺序按权重：华为/荣耀 → VIVO → 小米 → OPPO → 其他 → 三星。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandBadge(
    currentBrand: TutorialBrand,
    onBrandChange: (TutorialBrand) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    currentBrand.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "已按你的设备自动匹配",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = { showSheet = true }) {
            Text("切换品牌", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                Text(
                    "选择你的设备品牌",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))
                TutorialRepository.brandsByWeight.forEach { brand ->
                    val selected = brand == currentBrand
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .clickable {
                                onBrandChange(brand)
                                showSheet = false
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            brand.displayName,
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}
