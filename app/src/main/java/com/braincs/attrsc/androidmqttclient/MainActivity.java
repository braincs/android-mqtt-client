package com.braincs.attrsc.androidmqttclient;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.braincs.attrsc.protocol.MqttService;
import com.braincs.attrsc.protocol.bean.Config;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Config config = new Config.Builder().setServerUrl("tcp://10.156.2.132:1883")
                .setClientID("12345")
                .create();
        MqttService.startService(this, config);

    }


    public void onClickPub(View view) {
        MqttService.publish("mqtt/loop/message","test-mqtt", Config.MQTT_QOS_HIGH, Config.MQTT_RETAINED);
    }
}
