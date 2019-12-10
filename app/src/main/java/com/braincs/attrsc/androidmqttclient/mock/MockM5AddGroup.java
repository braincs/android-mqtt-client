package com.braincs.attrsc.androidmqttclient.mock;


import com.braincs.attrsc.androidmqttclient.IProtoView;
import com.braincs.attrsc.androidmqttclient.utils.FileUtil;
import com.braincs.attrsc.protocol.MqttService;
import com.braincs.attrsc.protocol.protobuf.Protocol;

/**
 * Created by Shuai
 * 14/05/2019.
 */

public class MockM5AddGroup extends Thread {
    private IProtoView view;
    private Protocol.Group group;

    public MockM5AddGroup(Protocol.Group group) {
        this.group = group;
    }

    public MockM5AddGroup(IProtoView mView,Protocol.Group group) {
        this.group = group;
        this.view = mView;
    }
    @Override
    public void run() {
        String groupFolder = FileUtil.createFolderUnderAppFolder(group.getGroup());// +"_"+group.getThreshold()+"_"+ group.getTop());
        System.out.println("--- MOCK: add a group success ---");
        String message = "";
        int code = 1;
        if (view != null && groupFolder != null){
            message = "add a group success";
            code = 0;
            MockMessage mockMessage = new MockMessage(message,group, true, MockMessage.Type.add_group);
            view.updateView(mockMessage);
        }else {
            message = "fail to add a group";
            code = 1;
            MockMessage mockMessage = new MockMessage(message,group, true, MockMessage.Type.add_group);
            view.updateView(mockMessage);
        }
        Protocol.Response.Builder builder = Protocol.Response.newBuilder();
        Protocol.Response response = builder.setSuccess(code)
                .setErr(message)
                .build();

        MqttService.responseAddGroup(group, response);
//        String topic = MqttService.TOPIC_PANEL_ADD_GROUP_RES+ group.getGroup();
//        Log.d("Debug", "topic: "+topic);
//        MqttService.publish(topic, response.toByteArray(), 2, false);
    }
}
