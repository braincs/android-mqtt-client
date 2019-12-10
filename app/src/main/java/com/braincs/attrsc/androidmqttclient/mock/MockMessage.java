package com.braincs.attrsc.androidmqttclient.mock;

/**
 * Created by Shuai
 * 04/06/2019.
 */

public class MockMessage {
    public enum Type {
        add_group, del_group, add_face, del_face, recognize, snapshot, other
    }
    private String message;
    private Object object;
    private boolean isSuccess;
    private Type type;

    public MockMessage() {
    }

    public MockMessage(Type type) {
        this.type = type;
    }

    public MockMessage(String message, Object object, boolean isSuccess, Type type) {
        this.message = message;
        this.object = object;
        this.isSuccess = isSuccess;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean success) {
        isSuccess = success;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
