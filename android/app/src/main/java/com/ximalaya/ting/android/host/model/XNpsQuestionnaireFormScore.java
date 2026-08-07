package com.ximalaya.ting.android.host.model;

import java.io.Serializable;

/** Decompiled model used by XNpsCardView. Kotlin-generated copy/equals boilerplate is omitted. */
public final class XNpsQuestionnaireFormScore implements Serializable {
    private QuestionnaireFormTag questionnaireFormTag;
    private String name;
    private int score;

    public XNpsQuestionnaireFormScore(QuestionnaireFormTag questionnaireFormTag, String name, int score) {
        this.questionnaireFormTag = questionnaireFormTag;
        this.name = name;
        this.score = score;
    }

    public QuestionnaireFormTag getQuestionnaireFormTag() { return questionnaireFormTag; }
    public void setQuestionnaireFormTag(QuestionnaireFormTag questionnaireFormTag) { this.questionnaireFormTag = questionnaireFormTag; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
}
