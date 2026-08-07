package TekEngineLib.State;

/* JADX INFO: loaded from: D:\QQ音乐\qqmusic-20.6.5.8-dex\classes21.dex */
public class TekGyroscopeData {
    public Double timestamp;
    public Double x;
    public Double y;
    public Double z;

    public TekGyroscopeData(Double d, Double d2, Double d3, Double d4) {
        Double dValueOf = Double.valueOf(0.0d);
        this.timestamp = dValueOf;
        this.x = dValueOf;
        this.timestamp = d;
        this.x = d2;
        this.y = d3;
        this.z = d4;
    }

    public String toString() {
        return "TekGyroscopeData{timestamp=" + this.timestamp + ", x=" + this.x + ", y=" + this.y + ", z=" + this.z + '}';
    }
}
