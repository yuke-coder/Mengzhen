package TekEngineLib.Lyric;

import androidx.annotation.Nullable;
import java.util.Objects;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekFontShadow {
    public float blurRadius;
    public float color_a;
    public float color_b;
    public float color_g;
    public float color_r;
    public boolean enable;
    public float offsetX;
    public float offsetY;

    public TekFontShadow(boolean z, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        this.enable = z;
        this.color_r = f;
        this.color_g = f2;
        this.color_b = f3;
        this.color_a = f4;
        this.blurRadius = f5;
        this.offsetX = f6;
        this.offsetY = f7;
    }

    public boolean equals(@Nullable Object obj) {
        if (obj == null || !(obj instanceof TekFontShadow)) {
            return super.equals(obj);
        }
        TekFontShadow tekFontShadow = (TekFontShadow) obj;
        return this.enable == tekFontShadow.enable && Float.compare(this.color_r, tekFontShadow.color_r) == 0 && Float.compare(this.color_g, tekFontShadow.color_g) == 0 && Float.compare(this.color_b, tekFontShadow.color_b) == 0 && Float.compare(this.color_a, tekFontShadow.color_a) == 0 && Float.compare(this.blurRadius, tekFontShadow.blurRadius) == 0 && Float.compare(this.offsetX, tekFontShadow.offsetX) == 0 && Float.compare(this.offsetY, tekFontShadow.offsetY) == 0;
    }

    public int hashCode() {
        return Objects.hash(Boolean.valueOf(this.enable), Float.valueOf(this.color_r), Float.valueOf(this.color_g), Float.valueOf(this.color_b), Float.valueOf(this.color_a), Float.valueOf(this.blurRadius), Float.valueOf(this.offsetX), Float.valueOf(this.offsetY));
    }
}
