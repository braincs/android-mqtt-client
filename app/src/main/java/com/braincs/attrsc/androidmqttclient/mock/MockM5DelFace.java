package com.braincs.attrsc.androidmqttclient.mock;

import android.util.Log;

import com.braincs.attrsc.androidmqttclient.IProtoView;
import com.braincs.attrsc.androidmqttclient.utils.FileUtil;
import com.braincs.attrsc.protocol.MqttService;
import com.braincs.attrsc.protocol.bean.Message;
import com.braincs.attrsc.protocol.protobuf.Protocol;


import java.io.File;

/**
 * Created by Shuai
 * 16/05/2019.
 */

public class MockM5DelFace extends Thread {
//    private String face;
//    private String group;
    private Message delFaceMessage;
    private IProtoView view;

//    public MockM5DelFace(String group, String face) {
//        this.group = group;
//        this.face = face;
//    }

//    public MockM5DelFace(IProtoView view, String group, String face) {
//        this.view = view;
//        this.group = group;
//        this.face = face;
//    }

    public MockM5DelFace(IProtoView view, Message delFaceMessage){
        this.delFaceMessage = delFaceMessage;
        this.view = view;
    }

    @Override
    public void run() {
//        System.out.println("--- MOCK: delete a face success ---");
        File face = FileUtil.getFileUnderGroup(delFaceMessage.getGroup(), delFaceMessage.getFace());
        Protocol.Response.Builder builder = Protocol.Response.newBuilder();
        Protocol.Response response;
        String message = "";
        boolean isSuccess = false;
        if (face != null){
            //group  exist
            if (face.exists()){
                //
                boolean delete = face.delete();
                if (delete) {
                    message = "success delete face ";
                    response = builder.setSuccess(0)
                            .setErr(message)
                            .build();
                    isSuccess = true;
                    Log.d("Debug", message);
                }else {
                    message = "error during delete face operation";
                    response = builder.setSuccess(1)
                            .setErr(message)
                            .build();
                    Log.d("Debug", message);

                }
            }else {
                //face not exist
                message = "error face not exist";
                response = builder.setSuccess(1)
                        .setErr(message)
                        .build();
                Log.d("Debug", message);
            }
        }else {
            message = "error group not exist";
            response = builder.setSuccess(1)
                    .setErr(message)
                    .build();
            Log.d("Debug", message);
        }

        MockMessage mockMessage = new MockMessage(message, face, isSuccess, MockMessage.Type.del_face);
        if (view != null){
            view.updateView(mockMessage);
        }

        MqttService.responseDelFace(delFaceMessage, response);
//        MqttService.publish(topic, response.toByteArray(), 2, false);
    }
}