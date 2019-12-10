package com.braincs.attrsc.androidmqttclient;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.braincs.attrsc.androidmqttclient.mock.MockM5AddFace;
import com.braincs.attrsc.androidmqttclient.mock.MockM5AddGroup;
import com.braincs.attrsc.androidmqttclient.mock.MockM5DelFace;
import com.braincs.attrsc.androidmqttclient.mock.MockM5DelGroup;
import com.braincs.attrsc.androidmqttclient.mock.MockM5PubMsg;
import com.braincs.attrsc.androidmqttclient.mock.MockM5Snapshot;
import com.braincs.attrsc.androidmqttclient.mock.MockMessage;
import com.braincs.attrsc.androidmqttclient.utils.FileUtil;
import com.braincs.attrsc.protocol.MqttService;
import com.braincs.attrsc.protocol.bean.Config;
import com.braincs.attrsc.protocol.bean.Message;
import com.braincs.attrsc.protocol.bean.Snapshot;
import com.braincs.attrsc.protocol.protobuf.Protocol;
import com.google.protobuf.ByteString;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class ProtocalActivity extends AppCompatActivity implements IProtoView {

    private final String TAG = ProtocalActivity.class.getSimpleName();
    private TextView tvContent;
    private final String clientID = "12345";
    private final String HOST = "tcp://10.156.2.132:1883";
    private boolean m5IsOpen;
    private Disposable disposerStart;
    private Disposable disposerAddGroup;
    private Disposable disposerDelGroup;
    private Disposable disposerAddFace;
    private Disposable disposerDelFace;
    private Disposable disposerSnapshot;
    private byte[] data;
    private Protocol.Recognize def_recognize;
    private IProtoView mView;
    private List<String> groupList = new LinkedList<>();
    private ListView lvGroupList;
    private ArrayAdapter mGroupAdapter;
    private ImageView ivFace;
    private String currentGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protocal);

        mView = this;
        m5IsOpen = false;
        initView();
        initData();

        getPermissions();
        Config config = new Config.Builder().setServerUrl(HOST)
                .setClientID(clientID)
                .setStartObserver(observerStart)
                .setSnapShotObserver(observerSnapshot)
                .setAddGroupObserver(observerAddGroup)
                .setAddFaceObserver(observerAddFace)
                .setDelGroupObserver(observerDelGroup)
                .setDelFaceObserver(observerDelFace)
                .create();
        MqttService.startService(this, config);

    }

    //region observer
    private Observer<Protocol.Start> observerStart = new Observer<Protocol.Start>() {

        @Override
        public void onSubscribe(Disposable d) {
            disposerStart = d;
        }

        @Override
        public void onNext(Protocol.Start start) {
            boolean statusIsOpen = start.getIsOpen();
            int statusCheckInterval = start.getHealthCheckInterval();
            Protocol.Start.Mode statusMode = start.getMode();

            Log.d(TAG, start.toString());
            setReceiveMessage("Receive start request\n是否打开相机：" + statusIsOpen + "，相机检测周期：" + statusCheckInterval + "相机的模式：" + statusMode.toString());

            if (statusIsOpen && !m5IsOpen) {
                m5IsOpen = true;
                Log.d(TAG, "是否打开相机：" + statusIsOpen + "，相机检测周期：" + statusCheckInterval);
                Log.d(TAG, "相机的模式：" + statusMode.toString());
//                MockM5Activity.start(ProtocalActivity.this);
                setStatusMessage("Running");
                MqttService.startChecking(statusCheckInterval, "meg-v1", "meg-v2", "192.168.1.10");
                setSendMessage("Send check message every: " + statusCheckInterval + " s");

            } else if (!statusIsOpen && m5IsOpen) {
                m5IsOpen = false;
                Log.d(TAG, "是否打开相机：" + statusIsOpen + "，相机检测周期：" + statusCheckInterval);
                Log.d(TAG, "相机的模式：" + statusMode.toString());
//                MockM5Activity.close();
                setStatusMessage("Pause");

                MqttService.stopChecking();
                setSendMessage("Send stop checking");

            }
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };

    private Observer<Protocol.Face> observerAddFace = new Observer<Protocol.Face>() {

        @Override
        public void onSubscribe(Disposable d) {
            disposerAddFace = d;
        }

        @Override
        public void onNext(Protocol.Face face) {
            String group = face.getGroup();
            String f = face.getFace();
            String faceName = face.getName();
            ByteString image = face.getImage();
            String url = face.getUrl();

            Log.d(TAG, "Group: " + group + ", face = " + f + ", faceName = " + faceName + ", image = " + image + ", url = " + url);
            setReceiveMessage("Receive add-group request\nGroup: " + group + ", face = " + f + ", faceName = " + faceName + ", image = " + image + ", url = " + url);

            new MockM5AddFace(mView, face).start();
            setSendMessage("Send response add-group success");

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };
    private Observer<Protocol.Group> observerAddGroup = new Observer<Protocol.Group>() {

        @Override
        public void onSubscribe(Disposable d) {
            disposerAddGroup = d;
        }

        @Override
        public void onNext(Protocol.Group group) {
            String name = group.getGroup();
            int top = group.getTop();
            float threshold = group.getThreshold();

            Log.d(TAG, "Group: " + name + ", top = " + top + ", threshold = " + threshold);
            setReceiveMessage("Receive add-group request\nGroup: " + name + ", top = " + top + ", threshold = " + threshold);
            new MockM5AddGroup(mView, group).start();
//            setSendMessage("Send response add-group success");

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };
    private Observer<Message> observerDelGroup = new Observer<Message>() {
        @Override
        public void onSubscribe(Disposable d) {
            disposerDelGroup = d;
        }

        @Override
        public void onNext(Message message) {
            setReceiveMessage("Receive del-group request");
            new MockM5DelGroup(mView, message.getGroup()).start();
//            setSendMessage("Send response del-group success");
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };

    private Observer<Message> observerDelFace = new Observer<Message>() {
        @Override
        public void onSubscribe(Disposable d) {
            disposerDelFace = d;
        }

        @Override
        public void onNext(Message message) {
            setReceiveMessage("Receive del-face request");
            new MockM5DelFace(mView, message).start();

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };
    private Observer<Snapshot> observerSnapshot = new Observer<Snapshot>() {

        @Override
        public void onSubscribe(Disposable d) {
            disposerSnapshot = d;
        }

        @Override
        public void onNext(Snapshot snapshot) {

            Log.d(TAG, "==Get snapshot request==");
            setReceiveMessage("Receive snapshot request");
            new MockM5Snapshot(mView, data).start();
//            setSendMessage("Send snapshot");

        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };
    //endregion

    private void initData() {
        Drawable drawable = getResources().getDrawable(R.drawable.start);
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        data = stream.toByteArray();

        Protocol.Recognize.Builder builder = Protocol.Recognize.newBuilder();
        def_recognize = builder.setGroup("1")
                .setCrop(ByteString.copyFrom(data))
                .setFull(ByteString.copyFrom(data))
                .addTop(Protocol.Recognize.Result.newBuilder()
                        .setFace("face")
                        .setScore(85f)
                        .setName("ma")
                        .build()
                ).build();

        groupList = FileUtil.getFoldersUnderAppFolder();
        mGroupAdapter = new ArrayAdapter<String>(this,
                R.layout.layout_group_list, groupList);
        lvGroupList.setAdapter(mGroupAdapter);
    }

    private Protocol.Recognize buildRecognizeMessage(String group, String faceId, String name, float score){
        Log.d(TAG, "group: " + group + "faceId: " + faceId + "name: " + name + "score: " + score);
        Protocol.Recognize.Builder builder = Protocol.Recognize.newBuilder();
        Protocol.Recognize recognize = builder.setGroup(group)
                .setCrop(ByteString.copyFrom(data))
                .setFull(ByteString.copyFrom(data))
                .addTop(Protocol.Recognize.Result.newBuilder()
                        .setFace(faceId)
                        .setScore(score)
                        .setName(name)
                        .build()
                ).build();
        return recognize;
    }

    private String randomSearchFace() {
        currentGroup = groupList.get(groupList.size() - 1);
        Log.d(TAG, "group: " + currentGroup);
        String groupFile = FileUtil.getFilePathUnderAppFolder(currentGroup);
        //get first face under group
        File[] faces = new File(groupFile).listFiles();
        if (faces != null && faces.length > 1){
            return faces[0].getName();
        }

        return "no_face";
    }



    private void getPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
//                    || checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    ) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.CAMERA
                }, 0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // remove observer of start from service
        Log.d(TAG, "onDestroy call dispose");
        if (disposerAddFace != null) {
            disposerAddFace.dispose();
        }
        if (disposerStart != null) {
            disposerStart.dispose();
        }
        if (disposerAddGroup != null) {
            disposerAddGroup.dispose();
        }

        if (disposerDelGroup != null) {
            disposerDelGroup.dispose();
        }

        if (disposerDelFace != null) {
            disposerDelFace.dispose();
        }

        if (disposerSnapshot != null) {
            disposerSnapshot.dispose();
        }
        MqttService.stopService(this);
    }


    //region Event
    public void onClickDelFace(View view) {
        //test del face response message
        String group = "test";
        String face = "3333333";
        System.out.println("--- test: delete a group success ---");
        Protocol.Response.Builder builder = Protocol.Response.newBuilder();
        Protocol.Response response = builder.setSuccess(0)
                .setErr("")
                .build();

        MqttService.responseDelFace(new Message(group, face), response);
    }

    public void onClickDelGroup(View view) {

        //test del group message
        System.out.println("--- test: delete a group success ---");
        Protocol.Response.Builder builder = Protocol.Response.newBuilder();
        Protocol.Response response = builder.setSuccess(0)
                .setErr("")
                .build();

        String group = "test";
        MqttService.responseDelGroup(group, response);
    }

    public void onClickRecognize(View view) {
        //test recognize response
        // 主动发送识别消息
        setStatusMessage("send recognize message");
        String faceId = randomSearchFace();
        Protocol.Recognize recognize = buildRecognizeMessage(currentGroup, faceId, faceId, 100f);
        MqttService.responseRecognize(recognize);
        setSendMessage("send recognize message success");
    }

    public void onClickSnapshot(View view) {
        //test snapshot response
        Protocol.SnapShot.Builder builder = Protocol.SnapShot.newBuilder();
        Protocol.SnapShot snapShot = builder.setImage(ByteString.copyFrom(data))
                .build();
        Log.d(TAG, "pub an snapshot");
        MqttService.responseSnapshot(snapShot);
    }
    public void onClickTestPub(View view) {
        MockM5PubMsg pubMsg = new MockM5PubMsg("test publish message to server");
        pubMsg.start();
        setSendMessage("send published message success");
    }
    //endregion


    //region UI
    private void initView() {
        tvContent = findViewById(R.id.tvContent);
        tvContent.setMovementMethod(new ScrollingMovementMethod());
        lvGroupList = findViewById(R.id.group_list);
        ivFace = findViewById(R.id.ivFace);

    }

    private void setSendMessage(String msg) {
        tvContent.append("\n<<<<<<<<<< send\n");
        tvContent.append(msg);
//        tvContent.append("\n<---------------------------------->\n");
    }

    private void setReceiveMessage(String msg) {
        tvContent.append("\n>>>>>>>>>> Receive\n");
        tvContent.append(msg);
//        tvContent.append("\n>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>\n");
    }

    private void setStatusMessage(String msg) {
        tvContent.append("\n========== status \n");
        tvContent.append(msg);
//        tvContent.append("\n====================================\n");
    }


    @Override
    public void updateView(final Object object) {

        if (object instanceof MockMessage) {
            final String msg = ((MockMessage) object).getMessage();
            final boolean isSuccess = ((MockMessage) object).isSuccess();
            switch (((MockMessage) object).getType()) {
                case add_group:
                    final Protocol.Group group = (Protocol.Group)((MockMessage) object).getObject();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String name = group.getGroup();
                            if (!groupList.contains(name)) {
                                groupList.add(name);
                                mGroupAdapter.notifyDataSetChanged();
                                setStatusMessage("加组: " + name);
                            } else {
                                setStatusMessage(name + "组已存在");
                            }
                        }
                    });
                    break;

                case add_face:
                    final Protocol.Face face = (Protocol.Face)((MockMessage) object).getObject();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String group = face.getGroup();
                            String faceName = face.getFace();
                            String url = face.getUrl();
                            File file = FileUtil.getFileUnderGroup(group, faceName);
                            if (file != null) {
                                Log.d(TAG, file.getAbsolutePath());
                                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                                ivFace.setImageBitmap(bitmap);
                                setStatusMessage("加脸: " + faceName +" @组: " + group);
                            }else {
                                setStatusMessage("加脸失败: " + msg);
                            }
                        }
                    });
                    break;

                case del_group:
                    final String groupName = (String)((MockMessage) object).getObject();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //delete group
                            if (groupList.contains(groupName)) {
                                groupList.remove(groupName);
                                mGroupAdapter.notifyDataSetChanged();
                                setStatusMessage("删组: " + groupName);
                            } else {
                                setStatusMessage(groupName + "组不存在");
                            }
                        }
                    });
                    break;
                case del_face:
                    final boolean isDelFace = ((MockMessage) object).isSuccess();
                    final File faceFile = ((File) ((MockMessage) object).getObject());

//                    final String parent = faceFile.getParent();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //delete face
                            if (isDelFace) {

                                if (faceFile != null){
                                    String name = faceFile.getName();
                                    String parent = faceFile.getParentFile().getName();
                                    ivFace.setImageResource(android.R.color.transparent);
                                    setStatusMessage("删脸成功: " + name + " @组: " + parent);
                                }
                            } else {
                                setStatusMessage("删脸失败: " + msg);
                            }
                        }
                    });

                    break;
                case recognize:
                    break;
                case snapshot:
                    setStatusMessage(msg);
                    break;
                case other:
                    break;
            }


        }
    }
    //endregion


}
