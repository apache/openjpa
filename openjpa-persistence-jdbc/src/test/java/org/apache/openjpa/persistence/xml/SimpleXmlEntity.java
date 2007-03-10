package org.apache.openjpa.persistence.xml;

public class SimpleXmlEntity {

    private long id;
    private int version;
    private String stringField;

    public String getStringField() {
        return stringField;
    }

    public void setStringField(String stringField) {
        this.stringField = stringField;
    }
}
