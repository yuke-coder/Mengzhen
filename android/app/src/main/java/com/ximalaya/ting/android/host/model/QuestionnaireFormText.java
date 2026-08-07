package com.ximalaya.ting.android.host.model;

import java.io.Serializable;

/** Decompiled model used by XNpsCardView. Kotlin-generated copy/equals boilerplate is omitted. */
public final class QuestionnaireFormText implements Serializable {
    private String content;
    private String dictionaryCode;
    private int componentType;

    public QuestionnaireFormText(String content, String dictionaryCode, int componentType) {
        this.content = content;
        this.dictionaryCode = dictionaryCode;
        this.componentType = componentType;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getDictionaryCode() { return dictionaryCode; }
    public void setDictionaryCode(String dictionaryCode) { this.dictionaryCode = dictionaryCode; }
    public int getComponentType() { return componentType; }
    public void setComponentType(int componentType) { this.componentType = componentType; }
}
