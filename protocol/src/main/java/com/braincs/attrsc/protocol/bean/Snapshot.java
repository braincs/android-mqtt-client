package com.braincs.attrsc.protocol.bean;

/**
 * Created by Shuai
 * 22/05/2019.
 */

public class Snapshot {
    private String message;

    public Snapshot() {
    }

    public Snapshot(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
