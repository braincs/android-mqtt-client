package com.braincs.attrsc.androidmqttclient.mock;

import android.util.Log;

import com.braincs.attrsc.androidmqttclient.IProtoView;
import com.braincs.attrsc.androidmqttclient.utils.FileUtil;
import com.braincs.attrsc.androidmqttclient.utils.NetImageUtil;
import com.braincs.attrsc.protocol.MqttService;
import com.braincs.attrsc.protocol.protobuf.Protocol;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Shuai
 * 16/05/2019.
 */

public class MockM5AddFace extends Thread {
    private IProtoView view;
    private Protocol.Face face;

    public MockM5AddFace(Protocol.Face face) {
        this.face = face;
    }
    public MockM5AddFace(IProtoView view, Protocol.Face face) {
        this.face = face;
        this.view = view;
    }

    @Override
    public void run() {
        //download image from url
        MockMessage mockMessage = new MockMessage(MockMessage.Type.add_face);
        String message = "";
        String urlStr = face.getUrl();
//        System.out.println("---url: "+urlStr +" ---");
        Protocol.Response response;
        URL url = null;
        try {
            url = new URL(urlStr);
        } catch (MalformedURLException e) {
            message = "error decode url";
            Log.e("debug", message);
            mockMessage.setMessage(message);
            mockMessage.setSuccess(false);
            mockMessage.setObject(face);
            if (view != null){
                view.updateView(mockMessage);
            }
            Protocol.Response.Builder builder = Protocol.Response.newBuilder();
            response = builder.setSuccess(1)
                    .setErr(message)
                    .build();
//            String topic = MqttService.TOPIC_PANEL_ADD_FACE_RES+ face.getGroup() +"/" + face.getFace();
//            Log.d("Debug", "response topic: "+topic);
//            MqttService.publish(topic, response.toByteArray(), 2, false);
            MqttService.responseAddFace(face, response);
            e.printStackTrace();
        }
        File faceFile = FileUtil.createFileUnderGroup(face.getGroup(), face.getFace());
        if (faceFile == null){
            //group not init
            message = "error group has not been init";
            Log.e("debug", message);
            mockMessage.setMessage(message);
            mockMessage.setSuccess(false);
            mockMessage.setObject(face);
            if (view != null){
                view.updateView(mockMessage);
            }
            Protocol.Response.Builder builder = Protocol.Response.newBuilder();
            response = builder.setSuccess(1)
                    .setErr(message)
                    .build();
//            String topic = MqttService.TOPIC_PANEL_ADD_FACE_RES+ face.getGroup() +"/" + face.getFace();
//            Log.d("Debug", "response topic: "+topic);
//            MqttService.publish(topic, response.toByteArray(), 2, false);
            MqttService.responseAddFace(face, response);
            return;
        }


        boolean downloadFace = NetImageUtil.downloadImage2SDCard(url, faceFile);

        if (downloadFace){
            message = "add a face success";
            mockMessage.setSuccess(true);
            mockMessage.setObject(face);
            mockMessage.setMessage(message);
            Log.d("Debug","--- MOCK: add a face "+downloadFace +" ---");
            Protocol.Response.Builder builder = Protocol.Response.newBuilder();
            response = builder.setSuccess(0)
                    .setErr(message)
                    .build();
        } else {
            message = "fail to download face image";
            mockMessage.setSuccess(false);
            mockMessage.setObject(face);
            mockMessage.setMessage(message);
            Protocol.Response.Builder builder = Protocol.Response.newBuilder();
            response = builder.setSuccess(1)
                    .setErr(message)
                    .build();
        }

        if (view != null){
            view.updateView(mockMessage);
        }
//        String topic = MqttService.TOPIC_PANEL_ADD_FACE_RES+ face.getGroup() +"/" + face.getFace();
//        Log.d("Debug", "response topic: "+topic);
//        MqttService.publish(topic, response.toByteArray(), 2, false);
        MqttService.responseAddFace(face, response);
    }
}