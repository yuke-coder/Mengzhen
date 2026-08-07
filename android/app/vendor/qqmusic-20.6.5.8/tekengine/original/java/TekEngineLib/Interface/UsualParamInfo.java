package TekEngineLib.Interface;

import java.util.Map;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class UsualParamInfo {
    public static final String kTargetLayerAll = "TargetLayerAll";
    public Map map;
    public String targetLayer;

    private UsualParamInfo() {
    }

    public UsualParamInfo(String str, Map map) {
        this.targetLayer = str;
        this.map = map;
    }
}
