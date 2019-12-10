package com.braincs.attrsc.protocol.bean;


import com.braincs.attrsc.protocol.protobuf.Protocol;

import io.reactivex.Observer;

/**
 * Created by Shuai
 * 01/07/2019.
 */
public class Config {
    public final static int MQTT_QOS_HIGH = 2;
    public final static int MQTT_QOS_NORMAL = 1;
    public final static int MQTT_QOS_LOW = 0;
    public final static boolean MQTT_RETAINED = false;

    /**
     * serverUrl : 服务器地址（协议+地址+端口号）
     */
    private String serverUrl = "tcp://10.156.2.132:1883";
//    private String serverUrl = "tcp://10.199.0.123:21883";
    /**
     * USERNAME : 用户名
     */
    private String USERNAME = "admin";

    /**
     * PASSWORD : 密码
     */
    private String PASSWORD = "admin";

    /**
     * clientID : 用户唯一标识
     */
    private String clientID;

    /**
     * startObserver : 启动监听
     */
    private Observer<Protocol.Start> startObserver;

    /**
     * addGroupObserver : 加组监听
     */
    private Observer<Protocol.Group> addGroupObserver;

    /**
     * delGroupObserver : 删组监听
     */
    private Observer<Message> delGroupObserver;

    /**
     * addFaceObserver : 加脸监听
     */
    private Observer<Protocol.Face> addFaceObserver;

    /**
     * delFaceObserver : 删脸监听
     */
    private Observer<Message> delFaceObserver;

    /**
     * snapShotObserver : 抓拍监听
     */
    private Observer<Snapshot> snapShotObserver;


    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setUSERNAME(String USERNAME) {
        this.USERNAME = USERNAME;
    }

    public void setPASSWORD(String PASSWORD) {
        this.PASSWORD = PASSWORD;
    }

    public void setClientID(String clientID) {
        this.clientID = clientID;
    }

    public void setStartObserver(Observer<Protocol.Start> startObserver) {
        this.startObserver = startObserver;
    }

    public void setAddGroupObserver(Observer<Protocol.Group> addGroupObserver) {
        this.addGroupObserver = addGroupObserver;
    }

    public void setDelGroupObserver(Observer<Message> delGroupObserver) {
        this.delGroupObserver = delGroupObserver;
    }

    public void setAddFaceObserver(Observer<Protocol.Face> addFaceObserver) {
        this.addFaceObserver = addFaceObserver;
    }

    public void setDelFaceObserver(Observer<Message> delFaceObserver) {
        this.delFaceObserver = delFaceObserver;
    }

    public void setSnapShotObserver(Observer<Snapshot> snapShotObserver) {
        this.snapShotObserver = snapShotObserver;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getUSERNAME() {
        return USERNAME;
    }

    public String getPASSWORD() {
        return PASSWORD;
    }

    public String getClientID() {
        return clientID;
    }

    public Observer<Protocol.Start> getStartObserver() {
        return startObserver;
    }

    public Observer<Protocol.Group> getAddGroupObserver() {
        return addGroupObserver;
    }

    public Observer<Message> getDelGroupObserver() {
        return delGroupObserver;
    }

    public Observer<Protocol.Face> getAddFaceObserver() {
        return addFaceObserver;
    }

    public Observer<Message> getDelFaceObserver() {
        return delFaceObserver;
    }

    public Observer<Snapshot> getSnapShotObserver() {
        return snapShotObserver;
    }

    public static class Builder {
        private String serverUrl = "tcp://10.199.0.123:21883";
        private String USERNAME = "admin";
        private String PASSWORD = "admin";
        private String clientID;
        private Observer<Protocol.Start> startObserver;
        private Observer<Protocol.Group> addGroupObserver;
        private Observer<Message> delGroupObserver;
        private Observer<Protocol.Face> addFaceObserver;
        private Observer<Message> delFaceObserver;
        private Observer<Snapshot> snapShotObserver;


        public Builder setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder setUSERNAME(String USERNAME) {
            this.USERNAME = USERNAME;
            return this;
        }

        public Builder setPASSWORD(String PASSWORD) {
            this.PASSWORD = PASSWORD;
            return this;
        }

        public Builder setClientID(String clientID) {
            this.clientID = clientID;
            return this;
        }

        public Builder setStartObserver(Observer<Protocol.Start> startObserver) {
            this.startObserver = startObserver;
            return this;
        }

        public Builder setAddGroupObserver(Observer<Protocol.Group> addGroupObserver) {
            this.addGroupObserver = addGroupObserver;
            return this;
        }

        public Builder setDelGroupObserver(Observer<Message> delGroupObserver) {
            this.delGroupObserver = delGroupObserver;
            return this;
        }

        public Builder setAddFaceObserver(Observer<Protocol.Face> addFaceObserver) {
            this.addFaceObserver = addFaceObserver;
            return this;
        }

        public Builder setDelFaceObserver(Observer<Message> delFaceObserver) {
            this.delFaceObserver = delFaceObserver;
            return this;
        }

        public Builder setSnapShotObserver(Observer<Snapshot> snapShotObserver) {
            this.snapShotObserver = snapShotObserver;
            return this;
        }

        public Config create() {
            Config config = new Config();
            if (serverUrl != null) {
                config.setServerUrl(serverUrl);
            }
            if (USERNAME != null) {
                config.setUSERNAME(USERNAME);
            }
            if (PASSWORD != null) {
                config.setPASSWORD(PASSWORD);
            }
            if (clientID != null) {
                config.setClientID(clientID);
            }
            if (startObserver != null) {
                config.setStartObserver(startObserver);
            }
            if (snapShotObserver != null) {
                config.setSnapShotObserver(snapShotObserver);
            }
            if (addFaceObserver != null) {
                config.setAddFaceObserver(addFaceObserver);
            }
            if (delFaceObserver != null) {
                config.setDelFaceObserver(delFaceObserver);
            }
            if (addGroupObserver != null) {
                config.setAddGroupObserver(addGroupObserver);
            }
            if (delGroupObserver != null) {
                config.setDelGroupObserver(delGroupObserver);
            }
            return config;
        }
    }
}
