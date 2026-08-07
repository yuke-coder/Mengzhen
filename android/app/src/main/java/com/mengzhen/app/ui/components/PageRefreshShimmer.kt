package com.mengzhen.app.ui.components

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import com.mengzhen.app.R
import com.sankuai.waimai.store.shimmer.SGShimmerFrameLayout
import kotlin.math.max

@Composable
fun PageRefreshShimmer(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.semantics { contentDescription = "正在刷新" },
        factory = { context ->
            val root = LayoutInflater.from(context).inflate(
                R.layout.wm_sc_search_result_skeleton,
                null,
                false,
            ) as SGShimmerFrameLayout
            val density = context.resources.displayMetrics.density
            val dp = { value: Int -> (value * density).toInt() }
            val inflater = LayoutInflater.from(context)

            root.findViewById<LinearLayout>(R.id.skeleton_filter_container).apply {
                repeat(max(1, (context.resources.displayMetrics.widthPixels + dp(85) - 1) / dp(85))) { index ->
                    addView(View(context).apply {
                        background = context.getDrawable(R.drawable.wm_sc_skeleton_rounded_bg_large)
                        layoutParams = LinearLayout.LayoutParams(dp(79), dp(28)).apply {
                            if (index > 0) leftMargin = dp(6)
                        }
                    })
                }
            }

            root.findViewById<LinearLayout>(R.id.skeleton_list_container).apply {
                val availableHeight = context.resources.displayMetrics.heightPixels - dp(224)
                repeat(max(1, (availableHeight + dp(138) - 1) / dp(138))) {
                    addView(
                        inflater.inflate(R.layout.wm_sc_search_result_skeleton_item, this, false),
                        LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        ),
                    )
                }
            }
            root.c()
            root
        },
    )
}
