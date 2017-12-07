package com.disappointedpig.dpmidi.ui;


import android.os.Bundle;

import static com.disappointedpig.dpmidi.ui.CustomUIFieldChangedEvent.FieldTypes.BOOLEAN;
import static com.disappointedpig.dpmidi.ui.CustomUIFieldChangedEvent.FieldTypes.BUNDLE;
import static com.disappointedpig.dpmidi.ui.CustomUIFieldChangedEvent.FieldTypes.FLOAT;
import static com.disappointedpig.dpmidi.ui.CustomUIFieldChangedEvent.FieldTypes.INTEGER;
import static com.disappointedpig.dpmidi.ui.CustomUIFieldChangedEvent.FieldTypes.STRING;

public class CustomUIFieldChangedEvent {

    public enum FieldTypes { UNKNOWN, BOOLEAN, INTEGER, FLOAT, STRING, BUNDLE }

    private int fieldId;
    private FieldTypes fieldType;
    private String stringValue;
    private boolean booleanValue;
    private int intValue;
    private float floatValue;
    private Bundle bundleValue;

    public CustomUIFieldChangedEvent(int id, String v) {
        this.fieldId = id;
        this.stringValue = v;
        this.fieldType = STRING;
    }

    public CustomUIFieldChangedEvent(int id, boolean v) {
        this.fieldId = id;
        this.booleanValue = v;
        this.fieldType = BOOLEAN;
    }

    public CustomUIFieldChangedEvent(int id, int v) {
        this.fieldId = id;
        this.intValue = v;
        this.fieldType = INTEGER;
    }

    public CustomUIFieldChangedEvent(int id, float v) {
        this.fieldId = id;
        this.floatValue = v;
        this.fieldType = FLOAT;
    }

    public CustomUIFieldChangedEvent(int id, Bundle v) {
        this.fieldId = id;
        this.bundleValue = v;
        this.fieldType = BUNDLE;
    }
    public int getId() {
        return this.fieldId;
    }

    public int getIntValue() {
        return this.intValue;
    }

    public boolean getBooleanValue() {
        return this.booleanValue;
    }

    public float getFloatValue() {
        return this.floatValue;
    }

    public String getStringValue() {
        return this.stringValue;
    }

    public FieldTypes getFieldType() {
        return this.fieldType;
    }

    public Bundle getBundleValue() {
        return this.bundleValue;
    }
}
