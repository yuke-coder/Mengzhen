package com.tencent.qqmusic.business.customskin.player.adapter.custom;

import android.graphics.Rect;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import com.tencent.qqmusic.business.customskin.skin2.model.CS2SkinConfig;
import com.tencent.qqmusic.sword.SwordProxy;
import com.tencent.qqmusic.sword.SwordSwitches;
import kotlin.jvm.internal.Intrinsics;
import org.jetbrains.annotations.NotNull;

/* JADX INFO: loaded from: Q:\qqmusic-20.6.5.8-dex\classes3.dex */
public final class k0 extends RecyclerView.ItemDecoration {
    /* JADX DEBUG: Don't trust debug lines info. Lines numbers was adjusted: min line is 1 */
    public void getItemOffsets(@NotNull Rect rect, @NotNull View view, @NotNull RecyclerView recyclerView, @NotNull RecyclerView.State state) {
        byte[] bArr = SwordSwitches.switches10;
        if (bArr == null || ((bArr[167] >> 4) & 1) <= 0 || !SwordProxy.proxyMoreArgs(new Object[]{rect, view, recyclerView, state}, this, 102141).isSupported) {
            Intrinsics.checkNotNullParameter(rect, "outRect");
            Intrinsics.checkNotNullParameter(view, "view");
            Intrinsics.checkNotNullParameter(recyclerView, "parent");
            Intrinsics.checkNotNullParameter(state, CS2SkinConfig.BG_SAFETY_KEY_STATE);
            if (recyclerView.getChildAdapterPosition(view) == 0) {
                rect.left = com.tencent.qqmusiccommon.util.j0.a(16.0f);
                rect.right = com.tencent.qqmusiccommon.util.j0.a(8.0f);
            } else {
                rect.left = com.tencent.qqmusiccommon.util.j0.a(8.0f);
                rect.right = com.tencent.qqmusiccommon.util.j0.a(8.0f);
            }
        }
    }
}


