package com.braincs.attrsc.androidmqttclient.mock;

import com.braincs.attrsc.protocol.MqttService;
import com.braincs.attrsc.protocol.bean.Config;

/**
 * Created by Shuai
 * 10/12/2019.
 */
public class MockM5PubMsg extends Thread {
    private final static String TAG = MockM5PubMsg.class.getSimpleName();
    private final static String TOPIC = "mqtt/loop/message";

    public MockM5PubMsg(String message) {
        this(TAG, message);
    }

    private String message;

    public MockM5PubMsg(String name, String message) {
        super(name);
        this.message = message;
    }

    @Override
    public void run() {
        MqttService.publish(TOPIC, message, Config.MQTT_QOS_HIGH, Config.MQTT_RETAINED);

    }
}
