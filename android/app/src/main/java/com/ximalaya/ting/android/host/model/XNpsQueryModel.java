package com.ximalaya.ting.android.host.model;

import java.io.Serializable;
import java.util.List;

/** Decompiled XNPS query model. Kotlin-generated copy/equals boilerplate is omitted. */
public final class XNpsQueryModel implements Serializable {
    private String title;
    private int type;
    private int componentType;
    private String dictionaryCode;
    private Boolean mustSelect;
    private long id;
    private List<CustomButtonModel> customButtons;
    private QuestionnaireFormText questionnaireFormText;
    private List<XNpsQuestionnaireFormScore> questionnaireFormScores;

    public XNpsQueryModel(
            String title,
            int type,
            int componentType,
            String dictionaryCode,
            Boolean mustSelect,
            long id,
            List<CustomButtonModel> customButtons,
            QuestionnaireFormText questionnaireFormText,
            List<XNpsQuestionnaireFormScore> questionnaireFormScores
    ) {
        this.title = title;
        this.type = type;
        this.componentType = componentType;
        this.dictionaryCode = dictionaryCode;
        this.mustSelect = mustSelect;
        this.id = id;
        this.customButtons = customButtons;
        this.questionnaireFormText = questionnaireFormText;
        this.questionnaireFormScores = questionnaireFormScores;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public int getType() { return type; }
    public void setType(int type) { this.type = type; }
    public int getComponentType() { return componentType; }
    public void setComponentType(int componentType) { this.componentType = componentType; }
    public String getDictionaryCode() { return dictionaryCode; }
    public void setDictionaryCode(String dictionaryCode) { this.dictionaryCode = dictionaryCode; }
    public Boolean getMustSelect() { return mustSelect; }
    public void setMustSelect(Boolean mustSelect) { this.mustSelect = mustSelect; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public List<CustomButtonModel> getCustomButtons() { return customButtons; }
    public void setCustomButtons(List<CustomButtonModel> customButtons) { this.customButtons = customButtons; }
    public QuestionnaireFormText getQuestionnaireFormText() { return questionnaireFormText; }
    public void setQuestionnaireFormText(QuestionnaireFormText questionnaireFormText) { this.questionnaireFormText = questionnaireFormText; }
    public List<XNpsQuestionnaireFormScore> getQuestionnaireFormScores() { return questionnaireFormScores; }
    public void setQuestionnaireFormScores(List<XNpsQuestionnaireFormScore> questionnaireFormScores) { this.questionnaireFormScores = questionnaireFormScores; }
}
