  # Android端基于Mqtt协议数据传输

architecture：

![Mqtt-client-server-arch.jpg](images/Mqtt-client-server-arch.jpg)

server端参考：[SpringBoot 集成Mqtt，protobuf服务端搭建](https://www.jianshu.com/p/e21caf8338b8)

## 关于Mqtt

MQTT是一个机器对机器(M2M)/“**物联网**”连接协议。它被设计为一种非常**轻量级**的**发布/订阅消息传输**。它适用于需要少量代码占用和/或网络带宽昂贵的远程位置的连接。

例如，它已被用于通过卫星链路与代理通信的传感器、通过与医疗服务提供商的偶尔拨号连接的传感器，以及一系列家庭自动化和小型设备场景。

它也是**移动应用程序**的理想选择，因为优点：

- 体积小、
- 功耗低、
- 数据包最少，
- 并且可以有效地将信息分发给一个或多个接收者

> *MQTT is a machine-to-machine (M2M)/"Internet of Things" connectivity protocol. It was designed as an extremely lightweight publish/subscribe messaging transport. It is useful for connections with remote locations where a small code footprint is required and/or network bandwidth is at a premium. For example, it has been used in sensors communicating to a broker via satellite link, over occasional dial-up connections with healthcare providers, and in a range of home automation and small device scenarios. It is also ideal for mobile applications because of its small size, low power usage, minimised data packets, and efficient distribution of information to one or many receivers* 

## 关于 protobuf

Protobuf （全称 Protocol Buffers），是Google公司开发的一种数据描述语言，类似于XML能够将结构化数据序列化，可用于数据存储、通信协议等方面。

简单点来说就是类似于Json、Xml，最主要的优点是比Json、Xml速度快，相信不久的将来应用会更加广泛。

android中使用protobuf，过程是这样的：

- 1、定义proto文件；
- 2、使用该文件生成对应的java类；
- 3、利用该java类实现数据传输；



## 背景介绍

项目中涉及面板机与服务器之间进行通讯。在协议选择上，选择了轻量级的Mqtt + protobuf。旨在解决数据上的轻量级传输，和后台服务器的动态拓展。

因为是从0到1的开发过程，以此进行总结。

**建议：**由于protobuf包后的数据不方便展示，所以先调通Mqtt通信，在添加protobuf协议。

由于服务端API较复杂不适合一开始Mqtt协议的调试，所以构建了轻量级的服务端Mqtt-server用于debug，具体过程参考我的另外一篇文章：[SpringBoot 集成Mqtt，protobuf服务端搭建](https://www.jianshu.com/p/e21caf8338b8)



## 实现简单通信

要通信，先协议。通讯前先定义好通讯协议。好比需要沟通前，我们必须先商量好说哪国的语言是一样的。

参考tag：v1.0

### 简单协议

实现消息回环

```
服务端订阅 
[topic]:     in/mqtt/loop/message
服务端发布
[topic]:     out/mqtt/loop/message

客户端订阅
[topic]:     out/mqtt/loop/message
客户端发布
[topic]:     in/mqtt/loop/message
```

服务端和客户端都需要订阅彼此的topic，并向对方的topic 发送消息

**注意：**不能同时订阅和发布到相同topic，会产生回环，消息会不断接收发送，[自激震荡]。

为简单起见这里只实现：**客户端发送消息，服务端接受消息**

```
服务端订阅 
[topic]:     mqtt/loop/message

客户端发布：
[topic]:     mqtt/loop/message
```

### 创建客户端

使用Service包装客户端`MqttAndroidClient`

依赖Paho库，创建Paho 的 `MqttAndroidClient`，这是所有通讯的核心：

Paho `MqttAndroidClient` 构造方法如下：

- 传入context：Android上下文
- 传入 serverURI： **broker地址** `IP地址+端口号` 
- 传入clientID：需要具有唯一标识，这里使用 `前缀+UUID`的方式

```java
package org.eclipse.paho.android.service;
//...
/**
	 * Constructor - create an MqttAndroidClient that can be used to communicate with an MQTT server on android
	 * 
	 * @param context 
	 *            object used to pass context to the callback. 
	 * @param serverURI
	 *            specifies the protocol, host name and port to be used to
	 *            connect to an MQTT server
	 * @param clientId
	 *            specifies the name by which this connection should be
	 *            identified to the server
	 */
	public MqttAndroidClient(Context context, String serverURI,
			String clientId) {
		this(context, serverURI, clientId, null, Ack.AUTO_ACK);
	}
```

初始化客户端，在Service的 `onCreate()` 调用

```java
private void init() {
				String serverURI = HOST; //服务器地址（协议+地址+端口号）
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
```

设置监听，

- 当连接成功后，设置客户端订阅消息
- 当连接失败后，尝试重新建立连接

```java
//MQTT是否连接成功的监听
    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            Log.i(TAG, "连接成功 ");
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
```

### 通过客户端发布消息

为方便起见，设置为静态方法

通过创建的mqttAndroidClient客户端发布消息

- topic：与服务端监听的topic一致，并非是客户端监听的topic
- message：要发布的String
- qos：服务质量 0-2 
- retained：是否message被服务器保持

```java
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
```



### 服务端搭建

参考我的这篇文章 [SpringBoot 集成Mqtt，protobuf服务端搭建](https://www.jianshu.com/p/e21caf8338b8)

run springboot 项目，进行sub监听topic：`mqtt/loop/message`

```bash

***********************************************************************
Message Arrived at Time: 2019-12-10 12:01:52.724  Topic: mqtt/loop/message  Message: test-mqtt
***********************************************************************

```

## 完整版Mqtt通讯

项目实现面板机与服务器间的通信

具体业务：

- 启动、停止面板机
- 心跳
- 加组、删组（人脸组）
- 加脸、删脸（人脸底库）
- 抓拍消息
- 识别消息

### 完整协议

#### use: mqtt & protobuf

[topic] 定义规范：`产品类别/产品名/产品类别id/消息流向/功能`

[消息流向] 规范：流向服务器端为 in， 流向客户端为 out

#### open camera:

```protobuf
// request
[topic]:     panel/m5/12345/out/start
message Mode {
    enum Mode {
        capture   = 0;
        recognize = 1;
    }
    Mode  mode                  = 1;
    int32 health_check_interval = 2;
}
[last will]: panel/m5/12345/out/stop

// response (LOOP)
[topic]:     panel/m5/12345/in/check
[last will]: panel/m5/12345/in/disconnect
```

#### recognize message:

```protobuf
// response (LOOP)
[topic]: panel/m5/12345/in/recognize
message Recognize {
    message Result {
        string group = 1;
        string face  = 2;
        float  score = 3;
    }
    int    id   = 1; // message id
    Result top  = 2;

    required bytes image = 100; //crop face image binary ...
    optional bytes full  = 101; //full face image binary ...
}
```

#### add group:

```protobuf
// request
[topic]: panel/m5/12345/out/add_group
message Group {
    string group     = 1;
    int32  top       = 2;
    float  threshold = 3;
}

// response
[topic]: panel/m5/12345/int/add_group/:group
message Response {
    required int32  success = 1; // 0: success    others: error code
    optional string err     = 2; // "error: delete face falied. reason: xxxxxxx"
}
```

#### del group:

```protobuf
// request
[topic]: panel/m5/12345/out/del_group/:group

//response
[topic]: panel/m5/12345/in/del_group/:group
message Response {
    required int32  success = 1; // 0: success    others: error code
    optional string err     = 2; // "error: delete face falied. reason: xxxxxxx"
}
```



#### add face:

```protobuf
// request
[topic]: panel/m5/12345/out/add_face
message Face {
    string group
    string face
    bytes  image binary ...
}

// response
[topic]: panel/m5/12345/in/add_face/:group/:face
message Response {
    required int32  success = 1; // 0: success    others: error code
    optional string err     = 2; // "error: delete face falied. reason: xxxxxxx"
}
```

#### del face:

```protobuf
// request
[topic]: panel/m5/12345/out/del_face/:group/:face

// response
[topic]: panel/m5/12345/in/del_face/:group/:face
message Response {
    required int32  success = 1; // 0: success    others: error code
    optional string err     = 2; // "error: delete face falied. reason: xxxxxxx"
}
```

#### manual snapshot:

```protobuf
// request
[topic]: panel/m5/12345/out/snapshot

// response
[topic]: panel/m5/12345/in/snapshot
message Snapshot {
    bytes image = 1; // snapshot image binary ...
}
```



### 创建 protocol 模块封装mqtt通信

#### 依赖

创建 Android Library 命名：protocol. 此库用于实现mqtt通讯相关业务逻辑

依赖库

- 基于 Eclipse的paho库实现mqtt协议

- 基于rxjava 实现调度
- 基于retrofit2 实现网络请求
- 基于 Google protobuf实现数据传输过程的包装

protocol/gradle.build 中添加依赖

```groovy
dependencies {
    implementation fileTree(dir: 'libs', include: ['*.jar'])

    // 定义protobuf依赖，//使用精简版
    api 'com.google.protobuf:protobuf-java:3.1.0'
    api 'com.google.protobuf:protoc:3.1.0'
    api ('com.squareup.retrofit2:converter-protobuf:2.2.0') {
        exclude group: 'com.google.protobuf', module: 'protobuf-java'
    }

    //mqtt support
    api 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.0'
    api 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'

    //rxjava
    api 'io.reactivex.rxjava2:rxjava:2.2.8'
    api 'io.reactivex.rxjava2:rxandroid:2.1.1'

    implementation 'com.android.support:appcompat-v7:28.0.0'
    testImplementation 'junit:junit:4.12'
    androidTestImplementation 'com.android.support.test:runner:1.0.2'
    androidTestImplementation 'com.android.support.test.espresso:espresso-core:3.0.2'
}
```

#### 搭建.proto文件生成java类的环境

[参考：https://www.jianshu.com/p/1b78eb36ded7](https://www.jianshu.com/p/1b78eb36ded7)

1、 在 `src/main` 下创建protobuf文件夹创建`mqtt.proto`文件

依据协议，映射到proto文件 

![proto-gen.png](images/proto-gen.png)

```protobuf
syntax = "proto3";
option java_package = "com.braincs.attrsc.protocol.protobuf";
option java_outer_classname = "Protocol";
package pb;

message Rect {
    int32 left   = 1;
    int32 top    = 2;
    int32 width  = 3;
    int32 height = 4;
}

message RectF {
    float left   = 1;
    float top    = 2;
    float width  = 3;
    float height = 4;
}

message Response {
    int32  success = 1;
    string err     = 2;
}

message Start {
    enum Mode {
        capture   = 0;
        recognize = 1;
    }
    Mode  mode                  = 1;
    bool  is_open               = 2;
    int32 health_check_interval = 3;
}

message Group {
    string group = 1;
    int32  top = 2;
    float  threshold = 3;
}

message Face {
    string group     = 1;
    string face      = 2;
    string name      = 3;

    string url       = 10;
    bytes  image     = 100;
}

message Capture {
    int32 track = 1;
    int32 seq_num = 2;
    
    int64 timestamp = 10;
    float quality = 11;
    
    bytes crop = 100;
    RectF crop_rect  = 101;

    bytes full = 200;
}

message Recognize {
    message Result {
        string face  = 1;
        string name  = 2;

        float  score = 11;
    }
    repeated Result top  = 1;
    string group = 2;

    bytes crop  = 100;
    bytes full  = 101;
}

message SnapShot {
    bytes image = 100;
}

message Upgrade {
    int32  timeout = 1;
    string url = 10;
}

message UpgradeProgress {
    int32 progress = 1;
}

message Status {
    string version   = 1;
    string algorithm = 2;

    string local_ip  = 10;
}
```

2、 在根Project/build.gradle中加入protobuf插件

```groovy
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.2.0'
        classpath 'com.google.protobuf:protobuf-gradle-plugin:0.8.6' //添加这行
    }
}
```

3、在protocol/build.gradle中加入如下配置，

顶部加上

```groovy
apply plugin: 'com.android.library'
apply plugin: 'com.google.protobuf' //添加
```

android{}中加入

```groovy
sourceSets {
        main {
            // 自动生成的java资源路径
            java {
                srcDir 'src/main/java'
            }
            // 定义proto文件目录
            proto {
                srcDir 'src/main/protobuf'
                include '**/*.proto'
            }
        }
    }
```

android{}同级加入：

```groovy
protobuf {
    protoc {
        artifact = 'com.google.protobuf:protoc:3.1.0'
    }

    generateProtoTasks {
        all().each { task ->
            task.builtins {
                remove java
            }
            task.builtins {
                java {}
                // Add cpp output without any option.
                // DO NOT omit the braces if you want this builtin to be added.
                cpp {}
            }
        }
    }
    //生成目录
    generatedFilesBaseDir = "$projectDir/src/generated"
}
```

同步项目即可生成proto对应的java类

如果未能正常生成：

- 安装proto支持插件，Settings-->Plugins-->搜索protobuf-->找到Protobuf Support点击安装
- 重启as此时porto文件会有一个彩环，并且编写proto文件时也会有相应的提示

#### 加入 `Rxjava` 承接Mqtt消息

Android内部使用Rxjava进行消息通讯，由于篇幅原因，在这里不在赘述 `Rxjava`的具体实现

以启动消息为例：

1、创建ObservableQueue 消息队列，用于MqttMessage的队列

```java
ObservableOnSubscribe<MqttMessage> messageQueueObservable = new ObservableOnSubscribe<MqttMessage>() {
            @Override
            public void subscribe(ObservableEmitter<MqttMessage> emitter) throws Exception {
                msqStartEmitter = emitter;
            }
        };
```

2、创建Observable

```java
Observable<Protocol.Start> msqStartObservable = Observable.create(messageQueueObservable)
                .map(mqttStartFilter)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io());
```

3、关联对应的Observer

```java
if (observerStart != null)
            msqStartObservable.subscribe(observerStart);
```

4、创建消息转换器mqttStartFilter

Observable提供map功能，能自定义将消息转换成protobuf包装的消息体

```java
private static Function<MqttMessage, Protocol.Start> mqttStartFilter = new Function<MqttMessage, Protocol.Start>() {

        @Override
        public Protocol.Start apply(MqttMessage mqttMessage) throws Exception {
            return Protocol.Start.parseFrom(mqttMessage.getPayload());
        }
    };
```

5、关联mqtt消息与rxjava 消息

初始化时候，设置了mqtt消息回调

```java
private void init(){
	//...
	mqttAndroidClient.setCallback(mqttCallback); //设置监听订阅消息的回调
	//...
}
```

接受mqtt消息，并通过`msqStartEmitter.onNext(message);`将消息传入`rxjava` 队列

```java
    private MqttCallback mqttCallback = new MqttCallback() {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d(TAG, "》》》》》》》》》》》" + topic);
            Log.i(TAG, "收到消息： " + new String(message.getPayload()));
            //...
          
            if (topic.equals(TOPIC_PANEL_SATAUS)) {
                msqStartEmitter.onNext(message);
            }
            //...
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
```

### 创建 Activity 使用rxjava与底层 MqttService进行通信

1、在 `onCreate()` 中启动Service，并传入rxjava的 Observer

```java
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
```

2、实现Observer，监听Mqtt消息 [Client-Sub]

以打开相机消息为例：

```java
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
```

**注意：**在Activity退出时候，需要释放对应的disposer，否则会内存泄漏

3、实现pub消息 [Client-Pub]

```java
public void onClickSnapshot(View view) {
        //test snapshot response
        Protocol.SnapShot.Builder builder = Protocol.SnapShot.newBuilder();
        Protocol.SnapShot snapShot = builder.setImage(ByteString.copyFrom(data))
                .build();
        Log.d(TAG, "pub an snapshot");
        MqttService.responseSnapshot(snapShot);
    }
```

## 与服务端联调

在pc端启动 springboot工程，使用以下curl命令进行调试

**流程：**使用curl命令，触发服务端的pub消息，客户端接收sub消息，并返回response。



1、启动相机

```bash
❯ curl -X POST "http://127.0.0.1:8080/start" -d "modeStr=recognize&health_check_interval=20"
```

2、添加组

组名：test1

阈值：92

top值：1 

```bash
❯ curl -XPOST "http://127.0.0.1:8080/group/add?url=mqtt://admin:admin@panel/m5/12345&group_name=test1&threshold=92.&top=1"
```

3、向组中添加人脸

组名：test1

cert_no：3333333

image_url：图片地址

**注意：** image_url 需填写本机的ip地址，确保Client和Server都在一个局域网内

```bash
❯ curl -XPOST "http://127.0.0.1:8080/face/add?url=mqtt://admin:admin@panel/m5/12345&group_name=test1&cert_no=3333333&name=xxx&image_url=http://10.156.2.75:8080/static/test.png"
```

效果图

![demo-mqtt](images/demo-mqtt.png)

工程开源地址 [github](https://github.com/braincs/android-mqtt-client) 欢迎star，一起探讨，提issue，等等等~

