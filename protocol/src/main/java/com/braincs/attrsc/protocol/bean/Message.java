package com.braincs.attrsc.protocol.bean;

/**
 * Created by Shuai
 * 24/05/2019.
 */

public class Message {
    private String group;
    private String face;

    public Message(String group) {
        this.group = group;
    }

    public Message(String group, String face) {
        this.group = group;
        this.face = face;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getFace() {
        return face;
    }

    public void setFace(String face) {
        this.face = face;
    }
}
