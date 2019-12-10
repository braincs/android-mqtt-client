package com.braincs.attrsc.androidmqttclient.mock;

import android.util.Log;

import com.braincs.attrsc.protocol.MqttService;
import com.braincs.attrsc.protocol.protobuf.Protocol;
import com.google.protobuf.ByteString;

import java.util.TimerTask;

/**
 * Created by Shuai
 * 09/05/2019.
 */

public class MockM5Recognize extends TimerTask {

    private final static String TAG = MockM5Recognize.class.getSimpleName();
    private Protocol.Recognize recognize;

    public MockM5Recognize(byte[] data) {

        Protocol.Recognize.Builder builder = Protocol.Recognize.newBuilder();
        recognize = builder.setGroup("1")
                .setFull(ByteString.copyFrom(data))
                .addTop( Protocol.Recognize.Result.newBuilder()
                        .setFace("face")
                        .setScore(85f)
                        .setName("ma")
                        .build()
                ).build();
    }

    public MockM5Recognize(Protocol.Recognize recognize) {
        this.recognize = recognize;
    }

    @Override
    public void run() {
//        String image = Base64.encodeToString(data, Base64.DEFAULT);

        Log.d(TAG, "pub an recognize");
        MqttService.responseRecognize(recognize);
//        MqttService.publish(MqttService.TOPIC_PANEL_RECOGNIZE, recognize.toByteArray(), 2, false);
    }
}
