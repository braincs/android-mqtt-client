package com.braincs.attrsc.protocol;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.braincs.attrsc.protocol.bean.Config;
import com.braincs.attrsc.protocol.bean.Message;
import com.braincs.attrsc.protocol.bean.Snapshot;
import com.braincs.attrsc.protocol.protobuf.Protocol;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Arrays;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Shuai
 * 09/12/2019.
 */
public class MqttService extends Service {
    public final String TAG = MqttService.class.getSimpleName();
    private static MqttAndroidClient mqttAndroidClient;
    private MqttConnectOptions mMqttConnectOptions;
    //    public String HOST = "tcp://10.156.2.132:1883";//服务器地址（协议+地址+端口号）
    private static String HOST = "tcp://10.199.0.123:21883";//服务器地址（协议+地址+端口号）
    private static String USERNAME = "admin";//用户名
    private static String PASSWORD = "admin";//密码
    private static String CLIENTID;//客户端ID，一般以客户端唯一标识符表示，这里用设备序列号表示


    private static String DEVICE_TYPE = "panel";//device /product
    private static String PRODUCT_NAME = "m5";//device /product
//    private static String DEVICE_TYPE = "${DEVICE_TYPE}";//device /product
//    private static String PRODUCT_NAME = "${PRODUCT_NAME}";//device /product
    private static String IP_PREFIX = "panel/m5/";//device /product
    private static volatile boolean isChecking = false;
    private static final Object sync = new Object();

    //region topics samples
    private static String TOPIC_PANEL_SATAUS = "panel/m5/12345/out/start";//status 主题
    private static String TOPIC_PANEL_CHECK = "panel/m5/12345/in/check";//check 主题
    private static String TOPIC_PANEL_RECOGNIZE = "panel/m5/12345/in/recognize";//recognize 主题
    private static String TOPIC_PANEL_ADD_GROUP = "panel/m5/12345/out/add_group";//group 主题
    private static String TOPIC_PANEL_ADD_GROUP_RES = "panel/m5/12345/in/add_group/";//group 主题
    private static String TOPIC_PANEL_DEL_GROUP = "panel/m5/12345/out/del_group/+";//group 主题
    private static String TOPIC_PANEL_DEL_GROUP_RES = "panel/m5/12345/in/del_group/";//group 主题
    private static String TOPIC_PANEL_ADD_FACE = "panel/m5/12345/out/add_face";//face 主题
    private static String TOPIC_PANEL_ADD_FACE_RES = "panel/m5/12345/in/add_face/";//face 主题
    private static String TOPIC_PANEL_DEL_FACE = "panel/m5/12345/out/del_face/+/+";//face 主题
    private static String TOPIC_PANEL_DEL_FACE_RES = "panel/m5/12345/in/del_face/";//face 主题
    private static String TOPIC_PANEL_SNAPSHOT = "panel/m5/12345/out/snapshot";//snapshot 主题
    private static String TOPIC_PANEL_SNAPSHOT_RES = "panel/m5/12345/in/snapshot";//snapshot result 主题

    //endregion

    //region last_will
    private static String LAST_WILL_PANEL_SATAUS = "panel/m5/12345/out/stop";//status 遗言
    private static String LAST_WILL_PANEL_CHECK = "panel/m5/12345/in/disconnect";//check 遗言
    //endregion

    private ObservableEmitter<MqttMessage> msqStartEmitter;
    private ObservableEmitter<MqttMessage> msqGroupEmitter;
    private ObservableEmitter<MqttMessage> msqFaceEmitter;
    private ObservableEmitter<Snapshot> msqSnapshotEmitter;
    private ObservableEmitter<Message> msqDelFaceEmitter;
    private ObservableEmitter<Message> msqDelGroupEmitter;

    private static Observer<Protocol.Start> observerStart;
    private static Observer<Protocol.Group> observerAddGroup;
    private static Observer<Protocol.Face> observerAddFace;
    private static Observer<Snapshot> observerSnapshot;
    private static Observer<Message> observerDelFace;
    private static Observer<Message> observerDelGroup;

    private static Handler mHandler;
    private static HandlerThread mHandlerThread;

    private final static int MQTT_QOS_HIGH = 2;
    private final static int MQTT_QOS_NORMAL = 1;
    private final static int MQTT_QOS_LOW = 0;
    private final static boolean MQTT_RETAINED = false;

    @Override
    public void onCreate() {
        Log.i(TAG, "--------------onCreate-------------------");

        initTopicLastWill();
        initRxMQ();
        init();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "--------------onStartCommand-------------------");
//        initTopicLastWill();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    /**
     * 开启服务
     * @deprecated start service with observers, use Config recommended
     * @see #startService(Context, Config)
     */
    public static void startService(Context mContext, String clientID) {
        mContext.startService(new Intent(mContext, MqttService.class));
        CLIENTID = "M_" + clientID;
    }

    /**
     * 开启服务
     * @deprecated start service with observers, use Config recommended
     * @see #startService(Context, Config)
     */
    public static void startService(Context mContext, String clientID,
                                    Observer<Protocol.Start> startObserver,
                                    Observer<Protocol.Group> addGroupObserver,
                                    Observer<Message> delGroupObserver,
                                    Observer<Protocol.Face> addFaceObserver,
                                    Observer<Message> delFaceObserver,
                                    Observer<Snapshot> snapShotObserver) {
        mContext.startService(new Intent(mContext, MqttService.class));
//        char prefix = PRODUCT_NAME.charAt(0);
//        CLIENTID = clientID;
        CLIENTID = "M_" + clientID;
        observerStart = startObserver;
        observerAddGroup = addGroupObserver;
        observerAddFace = addFaceObserver;
        observerDelGroup = delGroupObserver;
        observerDelFace = delFaceObserver;
        observerSnapshot = snapShotObserver;
    }

    /**
     * 开启服务
     * @param mContext context 上下文
     * @param config 配置项
     */
    public static void startService(Context mContext, Config config) {

        CLIENTID = "M_"+ config.getClientID();
        HOST = config.getServerUrl();
        PASSWORD = config.getPASSWORD();
        USERNAME = config.getUSERNAME();

        Log.d("debug", "Host: " +HOST);
        Log.d("debug", "PASSWORD: " +PASSWORD);
        Log.d("debug", "USERNAME: " +USERNAME);
        observerStart = config.getStartObserver();
        observerAddGroup = config.getAddGroupObserver();
        observerAddFace = config.getAddFaceObserver();
        observerDelGroup = config.getDelGroupObserver();
        observerDelFace = config.getDelFaceObserver();
        observerSnapshot = config.getSnapShotObserver();
        mContext.startService(new Intent(mContext, MqttService.class));
    }

    /**
     * 关闭服务
     */
    public static void stopService(Context mContext) {
        mContext.stopService(new Intent(mContext, MqttService.class));
    }

    /**
     * 发送check心跳包
     * @param delay 时间周期 unit second
     * @param version 固件版本号
     * @param alg_version 算法版本号
     * @param localIP 设备号唯一标识
     */
    public static void startChecking(final int delay, final String version, final String alg_version, final String localIP) {
        final int time_delay = Math.min(Math.max(0, delay), 60);
        if (!isChecking) {
            synchronized (sync) {
                if (!isChecking) {
                    isChecking = true;
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("debug","send heartbeat-----------" );
                            Protocol.Status.Builder builder = Protocol.Status.newBuilder();
                            Protocol.Status status = builder.setVersion(version)
                                    .setAlgorithm(alg_version)
                                    .setLocalIp(localIP).build();
                            publish(MqttService.TOPIC_PANEL_CHECK, status.toByteArray(), 2, false);
                            mHandler.postDelayed(this, time_delay * 1000);
                        }
                    }, delay * 1000);
                }
            }
        }
    }

    public static void stopChecking() {
        if (isChecking) {
            synchronized (sync) {
                if (isChecking) {
                    isChecking = false;
                    mHandler.removeCallbacksAndMessages(null);
                }
            }
        }
    }

    public static void responseAddFace(Protocol.Face face, Protocol.Response response) {
        String topic = MqttService.TOPIC_PANEL_ADD_FACE_RES+ face.getGroup() +"/" + face.getFace();
        Log.d("Debug", "response topic: "+topic);
        publish(topic, response.toByteArray(), MQTT_QOS_HIGH, MQTT_RETAINED);
    }

    public static void responseDelFace(Message delFaceMessage, Protocol.Response response) {
        String topic = MqttService.TOPIC_PANEL_DEL_FACE_RES+ delFaceMessage.getGroup() +"/" + delFaceMessage.getFace();
        Log.d("Debug", "response topic: "+topic);
        publish(topic,response.toByteArray(), MQTT_QOS_HIGH, MQTT_RETAINED );
    }

    public static void responseAddGroup(Protocol.Group group, Protocol.Response response) {
        String topic = MqttService.TOPIC_PANEL_ADD_GROUP_RES+ group.getGroup();
        Log.d("Debug", "topic: "+topic);
        MqttService.publish(topic, response.toByteArray(), MQTT_QOS_HIGH, MQTT_RETAINED);
    }

    public static void responseDelGroup(String group, Protocol.Response response) {
        String topic = MqttService.TOPIC_PANEL_DEL_GROUP_RES+ group;
        Log.d("Debug", "topic: "+topic);
        MqttService.publish(topic, response.toByteArray(), MQTT_QOS_HIGH, MQTT_RETAINED);
    }

    public static void responseRecognize(Protocol.Recognize recognize) {
        MqttService.publish(MqttService.TOPIC_PANEL_RECOGNIZE, recognize.toByteArray(), MQTT_QOS_HIGH, MQTT_RETAINED);
    }

    public static void responseSnapshot(Protocol.SnapShot snapShot) {
        MqttService.publish(MqttService.TOPIC_PANEL_SNAPSHOT_RES, snapShot.toByteArray(), MQTT_QOS_HIGH, MQTT_RETAINED);
    }

    /**
     * 发布 （模拟其他客户端发布消息）
     *
     * @param message 消息
     */
    public static void publish(String topic, String message, int qos, boolean retained) {
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(topic, message.getBytes(), qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void publish(String topic, byte[] message, int qos, boolean retained) {
        try {
            //参数分别为：主题、消息的字节数组、服务质量、是否在服务器保留断开连接后的最后一条消息
            mqttAndroidClient.publish(topic, message, qos, retained);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void initTopicLastWill() {
        IP_PREFIX = DEVICE_TYPE + "/"+ PRODUCT_NAME +"/";
        Log.d(TAG, "============IP: " + IP_PREFIX);
        TOPIC_PANEL_SATAUS = IP_PREFIX + CLIENTID + "/out/start";
        TOPIC_PANEL_CHECK = IP_PREFIX + CLIENTID + "/in/check";
        LAST_WILL_PANEL_SATAUS = IP_PREFIX + CLIENTID + "/out/stop";//status 遗言
        LAST_WILL_PANEL_CHECK = IP_PREFIX + CLIENTID + "/in/disconnect";//check 遗言
        TOPIC_PANEL_RECOGNIZE = IP_PREFIX + CLIENTID + "/in/recognize";//recognize 返回主题
        TOPIC_PANEL_ADD_GROUP = IP_PREFIX + CLIENTID + "/out/add_group";//add_group 主题
        TOPIC_PANEL_ADD_GROUP_RES = IP_PREFIX + CLIENTID + "/in/add_group/";//add_group 返回主题
        TOPIC_PANEL_DEL_GROUP = IP_PREFIX + CLIENTID + "/out/del_group/+";//del_group 主题
        TOPIC_PANEL_DEL_GROUP_RES = IP_PREFIX + CLIENTID + "/in/del_group/";//del_group 返回主题
        TOPIC_PANEL_ADD_FACE = IP_PREFIX + CLIENTID + "/out/add_face";//add_face 主题
        TOPIC_PANEL_DEL_FACE = IP_PREFIX + CLIENTID + "/out/del_face/+/+";//del_face 主题
        TOPIC_PANEL_DEL_FACE_RES = IP_PREFIX + CLIENTID + "/in/del_face/";//del_face 返回主题
        TOPIC_PANEL_SNAPSHOT = IP_PREFIX + CLIENTID + "/out/snapshot";//snapshot 主题
        TOPIC_PANEL_SNAPSHOT_RES = IP_PREFIX + CLIENTID + "/in/snapshot";//snapshot result 主题
    }

    /**
     * 初始化
     */
    private void init() {
        initHandler();

        String serverURI = HOST; //服务器地址（协议+地址+端口号）
//        final TelephonyManager tm = (TelephonyManager) getBaseContext().getSystemService(Context.TELEPHONY_SERVICE);
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            imei = tm.getImei();
//        } else {
//            imei = tm.getDeviceId();
//        }
        Log.i(TAG, "CLIENTID = " + CLIENTID);
        char prefix = PRODUCT_NAME.charAt(0);
        prefix = Character.toUpperCase(prefix);
        String DeviceID =  CLIENTID + "_" + UUID.randomUUID().toString();
        Log.i(TAG, "DeviceID:" + DeviceID);
        Log.i(TAG, "serverURI:" + serverURI);
        mqttAndroidClient = new MqttAndroidClient(this, serverURI, DeviceID);
        mqttAndroidClient.setCallback(mqttCallback); //设置监听订阅消息的回调
        mMqttConnectOptions = new MqttConnectOptions();
        mMqttConnectOptions.setCleanSession(true); //设置是否清除缓存
        mMqttConnectOptions.setConnectionTimeout(10); //设置超时时间，单位：秒
//        mMqttConnectOptions.setKeepAliveInterval(20); //设置心跳包发送间隔，单位：秒
        mMqttConnectOptions.setUserName(USERNAME); //设置用户名
        mMqttConnectOptions.setPassword(PASSWORD.toCharArray()); //设置密码

        // last will message
        boolean doConnect = true;
        String message = "{\"terminal_uid\":\"" + DeviceID + "\"}";

        // 最后的遗嘱last_will
        try {
            mMqttConnectOptions.setWill(LAST_WILL_PANEL_SATAUS, message.getBytes(), MQTT_QOS_HIGH, MQTT_RETAINED);
            mMqttConnectOptions.setWill(LAST_WILL_PANEL_CHECK, message.getBytes(), MQTT_QOS_HIGH, MQTT_RETAINED);
        } catch (Exception e) {
            Log.i(TAG, "Exception Occured", e);
            doConnect = false;
            iMqttActionListener.onFailure(null, e);
        }
        if (doConnect) {
            doClientConnection();
        }
    }

    private void initHandler() {
        if (null == mHandlerThread) {
            mHandlerThread = new HandlerThread(TAG);
            mHandlerThread.start();
        }
        if (null == mHandler) {
            mHandler = new Handler(mHandlerThread.getLooper());
        }
    }

    private void releaseHandler() {
        if (null != mHandler) {
            synchronized (sync) {
                if (null != mHandler) {
                    mHandler.removeCallbacksAndMessages(null);
                    mHandler = null;
                }
            }
        }
        if (null != mHandlerThread) {
            mHandlerThread.quitSafely();
            mHandlerThread = null;
        }
    }

    /**
     * 连接MQTT服务器
     */
    private void doClientConnection() {
        if (!mqttAndroidClient.isConnected() && isConnectIsNomarl()) {
            try {
                mqttAndroidClient.connect(mMqttConnectOptions, null, iMqttActionListener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 判断网络是否连接
     */
    private boolean isConnectIsNomarl() {
        ConnectivityManager connectivityManager = (ConnectivityManager) this.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if (info != null && info.isAvailable()) {
            String name = info.getTypeName();
            Log.i(TAG, "当前网络名称：" + name);
            return true;
        } else {
            Log.i(TAG, "没有可用网络");
            /*没有可用网络的时候，延迟3秒再尝试重连*/
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doClientConnection();
                }
            }, 3000);
            return false;
        }
    }

    //MQTT是否连接成功的监听
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功 ");
            try {
                // 订阅主题，参数：主题、服务质量
                // subscribe必须写在这，不然broker重启后subscribe不会恢复
                mqttAndroidClient.subscribe(TOPIC_PANEL_SATAUS, 2);
                mqttAndroidClient.subscribe(TOPIC_PANEL_ADD_GROUP, 2);
                mqttAndroidClient.subscribe(TOPIC_PANEL_DEL_GROUP, 2);
                mqttAndroidClient.subscribe(TOPIC_PANEL_ADD_FACE, 2);
                mqttAndroidClient.subscribe(TOPIC_PANEL_DEL_FACE, 2);
                mqttAndroidClient.subscribe(TOPIC_PANEL_SNAPSHOT, 2);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            arg1.printStackTrace();
            Log.i(TAG, "连接失败 ");
            /*没有可用网络的时候，延迟3秒再尝试重连*/
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    doClientConnection();//连接失败，重连（可关闭服务器进行模拟）
                }
            }, 5000);
//            doClientConnection();//连接失败，重连（可关闭服务器进行模拟）
        }
    };

    //订阅主题的回调
    private MqttCallback mqttCallback = new MqttCallback() {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d(TAG, "》》》》》》》》》》》" + topic);
            Log.i(TAG, "收到消息： " + new String(message.getPayload()));
            String topic_del_group_pat = TOPIC_PANEL_DEL_GROUP.replace("+", "");
            String topic_del_face_pat = TOPIC_PANEL_DEL_FACE.substring(0, TOPIC_PANEL_DEL_FACE.indexOf("+"));

            if (topic.equals(TOPIC_PANEL_SATAUS)) {
                msqStartEmitter.onNext(message);
                //start sending heartbeat after start
//                Protocol.Start status = Protocol.Start.parseFrom(message.getPayload());
//                Log.d(TAG, status.toString());
            } else if (topic.equals(TOPIC_PANEL_ADD_GROUP)) {
//                Log.d(TAG, "add group meg mqtt");
                msqGroupEmitter.onNext(message);
            } else if (topic.startsWith(topic_del_group_pat)) {
//                String groupName = topic.substring(topic.lastIndexOf('/')+1, topic.length());
//                Log.d(TAG, "del group name: " + groupName);
                String del_group = topic.replace(topic_del_group_pat, "");
                Log.d(TAG, "del group name: " + del_group);
                msqDelGroupEmitter.onNext(new Message(del_group));
//                new MockM5DelGroup(del_group).start();
            } else if (topic.equals(TOPIC_PANEL_ADD_FACE)) {
                Log.d(TAG, topic);
//                Protocol.Face face = Protocol.Face.parseFrom(message.getPayload());
//                Log.d(TAG, face.toString());
                msqFaceEmitter.onNext(message);
            } else if (topic.startsWith(topic_del_face_pat)) {
                String del_group_face = topic.replace(topic_del_face_pat, "");
                Log.d(TAG, del_group_face);
                String[] split = del_group_face.split("/");
                Log.d(TAG, Arrays.toString(split));
                msqDelFaceEmitter.onNext(new Message(split[0], split[1]));
            } else if (topic.equals(TOPIC_PANEL_SNAPSHOT)) {
                Log.d(TAG, topic);
                msqSnapshotEmitter.onNext(new Snapshot("snapshot"));
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken arg0) {

        }

        @Override
        public void connectionLost(Throwable arg0) {
            Log.i(TAG, "连接断开 ");
//            doClientConnection();//连接断开，重连
        }
    };

    //region Rx Message Queue
    private void initRxMQ() {
        //observable for start
        ObservableOnSubscribe<MqttMessage> messageQueueObservable = new ObservableOnSubscribe<MqttMessage>() {
            @Override
            public void subscribe(ObservableEmitter<MqttMessage> emitter) throws Exception {
                msqStartEmitter = emitter;
            }
        };
        Observable<Protocol.Start> msqStartObservable = Observable.create(messageQueueObservable)
                .map(mqttStartFilter)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());

        if (observerStart != null)
            msqStartObservable.subscribe(observerStart);

        //observable for group
        ObservableOnSubscribe<MqttMessage> mqGroupObservable = new ObservableOnSubscribe<MqttMessage>() {
            @Override
            public void subscribe(ObservableEmitter<MqttMessage> emitter) throws Exception {
                msqGroupEmitter = emitter;
            }
        };

        Observable<Protocol.Group> msqGroupObservable = Observable.create(mqGroupObservable)
                .map(mqttGroupFilter)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
        if (observerAddGroup != null)
            msqGroupObservable.subscribe(observerAddGroup);

        //observable for face
        ObservableOnSubscribe<MqttMessage> mqFaceObservable = new ObservableOnSubscribe<MqttMessage>() {
            @Override
            public void subscribe(ObservableEmitter<MqttMessage> emitter) throws Exception {
                msqFaceEmitter = emitter;
            }
        };
        Observable<Protocol.Face> msqFaceObservable = Observable.create(mqFaceObservable)
                .map(mqttFaceFilter)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
        if (observerAddFace != null)
            msqFaceObservable.subscribe(observerAddFace);

        //observable for snapshot
        ObservableOnSubscribe<Snapshot> mqSnapshotObservable = new ObservableOnSubscribe<Snapshot>() {
            @Override
            public void subscribe(ObservableEmitter<Snapshot> emitter) throws Exception {
                msqSnapshotEmitter = emitter;
            }
        };
        Observable<Snapshot> msqSnapshotObservable = Observable.create(mqSnapshotObservable)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
        if (observerSnapshot != null)
            msqSnapshotObservable.subscribe(observerSnapshot);

        //observable for del-face
        ObservableOnSubscribe<Message> mqDelFaceObservable = new ObservableOnSubscribe<Message>() {
            @Override
            public void subscribe(ObservableEmitter<Message> emitter) throws Exception {
                msqDelFaceEmitter = emitter;
            }
        };
        Observable<Message> msqDelFaceObservable = Observable.create(mqDelFaceObservable)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
        if (observerDelFace != null)
            msqDelFaceObservable.subscribe(observerDelFace);

        //observable for del-group
        ObservableOnSubscribe<Message> mqDelGroupObservable = new ObservableOnSubscribe<Message>() {
            @Override
            public void subscribe(ObservableEmitter<Message> emitter) throws Exception {
                msqDelGroupEmitter = emitter;
            }
        };
        Observable<Message> msqDelGroupObservable = Observable.create(mqDelGroupObservable)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
        if (observerDelGroup != null)
            msqDelGroupObservable.subscribe(observerDelGroup);

    }

    private static Function<MqttMessage, Protocol.Start> mqttStartFilter = new Function<MqttMessage, Protocol.Start>() {

        @Override
        public Protocol.Start apply(MqttMessage mqttMessage) throws Exception {
            return Protocol.Start.parseFrom(mqttMessage.getPayload());
        }
    };


    private static Function<MqttMessage, Protocol.Group> mqttGroupFilter = new Function<MqttMessage, Protocol.Group>() {
        @Override
        public Protocol.Group apply(MqttMessage mqttMessage) throws Exception {
            return Protocol.Group.parseFrom(mqttMessage.getPayload());
        }
    };

    private static Function<MqttMessage, Protocol.Face> mqttFaceFilter = new Function<MqttMessage, Protocol.Face>() {
        @Override
        public Protocol.Face apply(MqttMessage mqttMessage) throws Exception {
            return Protocol.Face.parseFrom(mqttMessage.getPayload());
        }
    };

    public static void setStartObserver(Observer<Protocol.Start> observer) {
        observerStart = observer;
    }

    public static void setAddGroupObserver(Observer<Protocol.Group> observer) {
        observerAddGroup = observer;
    }

    public static void setDelGroupObserver(Observer<Message> observer) {
        observerDelGroup = observer;
    }
    public static void setAddFaceObserver(Observer<Protocol.Face> observer) {
        observerAddFace = observer;
    }
    public static void setDelFaceObserver(Observer<Message> observer) {
        observerDelFace = observer;
    }

    public static void setSnapshotObserver(Observer<Snapshot> observer) {
        observerSnapshot = observer;
    }


    //endregion


    @Override
    public void onDestroy() {
        Log.d(TAG, "service onDestroy");
        if (isChecking){
            stopChecking();
        }
        releaseHandler();
        try {
            mqttAndroidClient.disconnect(); //断开连接
            mqttAndroidClient.unregisterResources();
//            mqttAndroidClient.close();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }
}
