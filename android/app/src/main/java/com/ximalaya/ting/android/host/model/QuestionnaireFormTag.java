package com.ximalaya.ting.android.host.model;

import java.io.Serializable;
import java.util.List;

/** Decompiled model used by XNpsCardView. Kotlin-generated copy/equals boilerplate is omitted. */
public final class QuestionnaireFormTag implements Serializable {
    private List<String> tags;
    private String title;
    private String dictionaryCode;
    private int componentType;

    public QuestionnaireFormTag(List<String> tags, String title, String dictionaryCode, int componentType) {
        this.tags = tags;
        this.title = title;
        this.dictionaryCode = dictionaryCode;
        this.componentType = componentType;
    }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDictionaryCode() { return dictionaryCode; }
    public void setDictionaryCode(String dictionaryCode) { this.dictionaryCode = dictionaryCode; }
    public int getComponentType() { return componentType; }
    public void setComponentType(int componentType) { this.componentType = componentType; }
}
