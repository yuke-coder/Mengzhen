package com.ximalaya.ting.android.host.model;

/** Decompiled model used by XNpsCardView. Kotlin-generated copy/equals boilerplate is omitted. */
public final class CustomButtonModel {
    private String label;
    private int colorStyle;
    private String url;
    private int action;

    public CustomButtonModel(String label, int colorStyle, String url, int action) {
        this.label = label;
        this.colorStyle = colorStyle;
        this.url = url;
        this.action = action;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getColorStyle() { return colorStyle; }
    public void setColorStyle(int colorStyle) { this.colorStyle = colorStyle; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getAction() { return action; }
    public void setAction(int action) { this.action = action; }
}
