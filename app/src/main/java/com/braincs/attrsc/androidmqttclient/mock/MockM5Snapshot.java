package com.braincs.attrsc.androidmqttclient.mock;

import android.util.Log;

import com.braincs.attrsc.androidmqttclient.IProtoView;
import com.braincs.attrsc.protocol.MqttService;
import com.braincs.attrsc.protocol.protobuf.Protocol;
import com.google.protobuf.ByteString;


/**
 * Created by Shuai
 * 09/05/2019.
 */

public class MockM5Snapshot extends Thread {

    private final static String TAG = MockM5Snapshot.class.getSimpleName();
    private Protocol.SnapShot snapShot;
    private IProtoView view;

    public MockM5Snapshot(byte[] data) {
        Protocol.SnapShot.Builder builder = Protocol.SnapShot.newBuilder();
        snapShot = builder.setImage(ByteString.copyFrom(data))
                .build();
    }
    public MockM5Snapshot(IProtoView view,byte[] data) {
        this.view = view;
        Protocol.SnapShot.Builder builder = Protocol.SnapShot.newBuilder();
        snapShot = builder.setImage(ByteString.copyFrom(data))
                .build();
    }

    public MockM5Snapshot(Protocol.SnapShot snapShot) {
        this.snapShot = snapShot;
    }

    @Override
    public void run() {
        Log.d(TAG, "pub an snapshot");
        String message = "success get snapshot";
        if (view != null){
            view.updateView(new MockMessage(message, snapShot, true, MockMessage.Type.snapshot));
        }
        MqttService.responseSnapshot(snapShot);
//        MqttService.publish(MqttService.TOPIC_PANEL_SNAPSHOT_RES, snapShot.toByteArray(), 2, false);
    }
}
