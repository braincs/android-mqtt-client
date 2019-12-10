package com.braincs.attrsc.androidmqttclient.mock;

import com.braincs.attrsc.androidmqttclient.IProtoView;
import com.braincs.attrsc.androidmqttclient.utils.FileUtil;
import com.braincs.attrsc.protocol.MqttService;
import com.braincs.attrsc.protocol.protobuf.Protocol;


/**
 * Created by Shuai
 * 14/05/2019.
 */

public class MockM5DelGroup extends Thread {

    private final String group;

    private IProtoView view;

    public MockM5DelGroup(String group) {
        this.group = group;
    }
    public MockM5DelGroup(IProtoView view, String group) {
        this.group = group;
        this.view = view;
    }

    @Override
    public void run() {

        boolean isExisted = FileUtil.checkFolderUnderAppFolder(group);
        MockMessage mockMessage = new MockMessage(MockMessage.Type.del_group);
        boolean isSuccess;
        String message;

        if (isExisted){
            String folder = FileUtil.delFolderUnderAppFolder(group);
            message = "delete a group success";
            isSuccess = true;

        }else {
            isSuccess = false;
            message = "group is not exist";
        }
        mockMessage.setMessage(message);
        mockMessage.setObject(group);
        mockMessage.setSuccess(isSuccess);

        if (view != null){
            view.updateView(mockMessage);
        }
        System.out.println("--- MOCK: delete a group success ---");
        Protocol.Response.Builder builder = Protocol.Response.newBuilder();
        Protocol.Response response = builder.setSuccess(isSuccess?0:1)
                .setErr(message)
                .build();

        MqttService.responseDelGroup(group, response);
//        String topic = MqttService.TOPIC_PANEL_DEL_GROUP_RES+ group;
//        Log.d("Debug", "topic: "+topic);
//        MqttService.publish(topic, response.toByteArray(), 2, false);
    }
}
