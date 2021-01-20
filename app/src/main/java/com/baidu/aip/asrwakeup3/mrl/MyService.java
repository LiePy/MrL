package com.baidu.aip.asrwakeup3.mrl;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.baidu.aip.asrwakeup3.mrl.sample.util.AutoCheck2;
import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.RecogResult;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.baidu.aip.asrwakeup3.core.wakeup.MyWakeup;
import com.baidu.aip.asrwakeup3.core.wakeup.listener.IWakeupListener;
import com.baidu.aip.asrwakeup3.core.wakeup.listener.RecogWakeupListener;
import com.baidu.aip.asrwakeup3.mrl.sample.control.InitConfig;
import com.baidu.aip.asrwakeup3.mrl.sample.listener.UiMessageListener;
import com.baidu.aip.asrwakeup3.mrl.sample.util.Auth;
import com.baidu.aip.asrwakeup3.mrl.sample.util.IOfflineResourceConst;
import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.asr.SpeechConstant;
import com.baidu.tts.chainofresponsibility.logger.LoggerProxy;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MyService extends Service implements IStatus, IOfflineResourceConst {
    private static final String TAG = MyService.class.getSimpleName();

    // ================== 语音识别参数设置 ==========================

    protected MyRecognizer myRecognizer;
    protected MyWakeup myWakeup;
    protected IWakeupListener listener;
    private EventManager asr;

    protected boolean enableOffline = false; // 测试离线命令词，需要改成true
    private int backTrackInMs = 1000;    //唤醒后的间隔等待时间，这时候小O会说“我在”

    // ================== 语音合成精简版初始化参数设置 ==========================

    protected String appId;
    protected String appKey;
    protected String secretKey;
    protected String sn; // 纯离线合成SDK授权码；离在线合成SDK没有此参数

    // TtsMode.MIX; 离在线融合，在线优先； TtsMode.ONLINE 纯在线； 没有纯离线
//    private TtsMode ttsMode = DEFAULT_SDK_TTS_MODE;
    private TtsMode ttsMode = TtsMode.ONLINE;   //在线语音合成

    private boolean isOnlineSDK = TtsMode.ONLINE.equals(DEFAULT_SDK_TTS_MODE);
    // ================ 纯离线sdk或者选择TtsMode.ONLINE  以下参数无用;
    private static final String TEMP_DIR = "/sdcard/baiduTTS"; // 重要！请手动将assets目录下的3个dat 文件复制到该目录

    // 请确保该PATH下有这个文件
    private static final String TEXT_FILENAME = TEMP_DIR + "/" + TEXT_MODEL;

    // 请确保该PATH下有这个文件 ，m15是离线男声
    private static final String MODEL_FILENAME = TEMP_DIR + "/" + VOICE_MALE_MODEL;

    protected SpeechSynthesizer mSpeechSynthesizer;


    public MyService() {
    }

    Handler handler = new Handler() {

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                handleMsg(msg);
            } catch (@SuppressLint("NewApi") InterruptedException | CameraAccessException | PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void handleMsg(Message msg) throws InterruptedException, CameraAccessException, PackageManager.NameNotFoundException {
//        super.handleMsg(msg);
        if (msg.what == STATUS_WAKEUP_SUCCESS) { // 唤醒词识别成功的回调，见RecogWakeupListener
            // 此处 开始正常识别流程

            String str_wp = msg.obj.toString();
            print("唤醒词："+str_wp);

            switch (str_wp){
                case "小O小O":
                case "小O在吗":
                case "你好小O":
                    speak("我在。");
                    Log.d("我在","111111111111");
                    Toast.makeText(this,"我在。",Toast.LENGTH_LONG).show();
                    Thread.sleep(backTrackInMs);//等待“我在”说完再开始录音
                    recog();
                    break;
                case "打开电灯":
                    speak("好的");
                    CameraManager manager = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
                    manager.setTorchMode("0",true);
                    speak("电灯已打开");
                    break;
                case "关闭电灯":
                    speak("好的");
                    CameraManager manager2 = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
                    manager2.setTorchMode("0",false);
                    speak("电灯已关闭");
                    break;
                case "播放":
                case "暂停":
                case "拍照":
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(intent);
                    break;
                case "上一首":
                case "下一首":
                default:
            }
        }else if(msg.what==21567){  //识别成功的回调，修改了源码，见MesStatusRecogListener
            String str11=" ";
            if(msg.obj!=null){
                str11 = msg.obj.toString(); //唤醒后你说的话识别成文字
            }
            Toast.makeText(this,"你说:"+str11,Toast.LENGTH_SHORT).show();
            cmd(str11);

        }
    }

    public void cmd(String str){
        if (str.contains("打开")){
            speak("好的");
            int result = openAppByName(str);
            if(result==0){
                speak("没有找到该程序");
            }
        }else if(str.contains("搜索")||str.contains("百度")){
            Pattern p  = Pattern.compile(".*?(搜索|百度)(一下)?(.*)");
            Matcher m = p.matcher(str);
            search(str);
            if(m.find()){
                print(m.group(0));
                print(m.group(1));
//                print(m.group(2));

            }
        }
    }

    public void search(String arg){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.baidu.com/s?wd="+arg));
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(intent);
    }

    public void startAppByPkg(String pkg){
        Intent intent = getApplicationContext().getPackageManager().getLaunchIntentForPackage(pkg);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        startActivity(intent);
    }

    public int openAppByName(String str){
        //应用过滤条件
        Intent mainIntent = new Intent(Intent.ACTION_MAIN,null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager mPackageManager = getApplicationContext().getPackageManager();

        List<ApplicationInfo> mPkgList = mPackageManager.getInstalledApplications(0);
        for (ApplicationInfo pif : mPkgList){
            String name = pif.loadLabel(mPackageManager).toString();
            String pgkName = pif.packageName;
            String cls = pif.name;
            print(name+"///"+pgkName+"///"+cls);
            if(str.contains(name)){
                speak("正在打开"+name);
                startAppByPkg(pgkName);
                print("找到App:"+name);
                return 1;
            }
        }
        print(str+"没有找到app");
        return 0;
    }


    public void recog(){
        Map<String, Object> params = new LinkedHashMap<String, Object>();
        params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
        params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
        // 使用1537中文模型。其它PID参数请看文档
        params.put(SpeechConstant.PID, 1537);
//            if (backTrackInMs > 0) {
//                // 方案1  唤醒词说完后，直接接句子，中间没有停顿。开启回溯，连同唤醒词一起整句识别。
//                // System.currentTimeMillis() - backTrackInMs ,  表示识别从backTrackInMs毫秒前开始
//                params.put(SpeechConstant.AUDIO_MILLS, System.currentTimeMillis() - backTrackInMs);
//            }
        myRecognizer.cancel();
        myRecognizer.start(params);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();

        appId = Auth.getInstance(this).getAppId();
        appKey = Auth.getInstance(this).getAppKey();
        secretKey = Auth.getInstance(this).getSecretKey();
        sn = Auth.getInstance(this).getSn(); // 纯离线合成必须有此参数；离在线合成SDK没有此参数

        //读取本地设置
        SharedPreferences pref = getSharedPreferences("setData",MODE_PRIVATE);
        pref.getString("name","");

        //显示前台通知
        showNotification();

        //开启唤醒识别等待
        wakeupAndRec();

        initTTs();
//        new Thread(() -> initTTs()).start();

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void showNotification(){
        String CHANNEL_ONE_ID = "CHANNEL_ONE_ID";
        String CHANNEL_ONE_NAME= "CHANNEL_ONE_ID";
        NotificationChannel notificationChannel= null;
//进行8.0的判断
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel= new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager= (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }
        Intent intent = new Intent(this,SettingsActivity.class);
        PendingIntent pendingIntent= PendingIntent.getActivity(this, 0, intent, 0);
        Notification notification= new Notification.Builder(this).setChannelId(CHANNEL_ONE_ID)
                .setTicker("Nature")
                .setSmallIcon(R.mipmap.my_ic_launcher)// 设置状态栏内的小图标
                .setContentTitle("小O正在后台等待您的呼唤...")
                .setContentIntent(pendingIntent)// 设置PendingIntent
                .setContentText("尝试说：“小O小O”")
                .build();
        notification.flags|= Notification.FLAG_NO_CLEAR;
        startForeground(1, notification);

        // 参数一：唯一的通知标识；参数二：通知消息。
        startForeground(110, notification);// 开始前台服务
    }

    //监听唤醒词然后录制识别
    public void wakeupAndRec(){
        IRecogListener recogListener = new MessageStatusRecogListener(handler){
            @Override
            public void onAsrFinalResult(String[] results, RecogResult recogResult) {
                super.onAsrFinalResult(results, recogResult);
                Log.d("识别结果：",results[0]);
            }
        };
        // 改为 SimpleWakeupListener 后，不依赖handler，但将不会在UI界面上显示
        myRecognizer = new MyRecognizer(this, recogListener);

        listener = new RecogWakeupListener(handler);
        myWakeup = new MyWakeup(this, listener);
        myWakeup.setEventListener(listener); // 替换原来的 listener

        new Thread(() -> start()).start();
    }

    //语音合成初始化
    private void initTTs() {
        LoggerProxy.printable(true); // 日志打印在logcat中
        boolean isSuccess;
        if (!isOnlineSDK) {
            // 检查2个离线资源是否可读
            isSuccess = checkOfflineResources();
            if (!isSuccess) {
                return;
            } else {
                print("离线资源存在并且可读, 目录：" + TEMP_DIR);
            }
        }
        // 日志更新在UI中，可以换成MessageListener，在logcat中查看日志
        SpeechSynthesizerListener listener = new UiMessageListener(handler);

        // 1. 获取实例
        mSpeechSynthesizer = SpeechSynthesizer.getInstance();
        mSpeechSynthesizer.setContext(this);

        // 2. 设置listener
        mSpeechSynthesizer.setSpeechSynthesizerListener(listener);

        // 3. 设置appId，appKey.secretKey
        int result = mSpeechSynthesizer.setAppId(appId);
        checkResult(result, "setAppId");
        result = mSpeechSynthesizer.setApiKey(appKey, secretKey);
        checkResult(result, "setApiKey");

        // 4. 如果是纯离线SDK需要离线功能的话
        if (!isOnlineSDK) {
            // 文本模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
            mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, TEXT_FILENAME);
            // 声学模型文件路径 (离线引擎使用)， 注意TEXT_FILENAME必须存在并且可读
            mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, MODEL_FILENAME);

            mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_MIX_MODE, SpeechSynthesizer.MIX_MODE_DEFAULT);
            // 该参数设置为TtsMode.MIX生效。
            // MIX_MODE_DEFAULT 默认 ，wifi状态下使用在线，非wifi离线。在线状态下，请求超时6s自动转离线
            // MIX_MODE_HIGH_SPEED_SYNTHESIZE_WIFI wifi状态下使用在线，非wifi离线。在线状态下， 请求超时1.2s自动转离线
            // MIX_MODE_HIGH_SPEED_NETWORK ， 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线
            // MIX_MODE_HIGH_SPEED_SYNTHESIZE, 2G 3G 4G wifi状态下使用在线，其它状态离线。在线状态下，请求超时1.2s自动转离线

        }

        // 5. 以下setParam 参数选填。不填写则默认值生效
        // 设置在线发声音人： 0 普通女声（默认） 1 普通男声  3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "4");
        // 设置合成的音量，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "9");
        // 设置合成的语速，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "7");
        // 设置合成的语调，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "7");

        // mSpeechSynthesizer.setAudioStreamType(AudioManager.MODE_IN_CALL); // 调整音频输出

        if (sn != null) {
            // 纯离线sdk这个参数必填；离在线sdk没有此参数
            mSpeechSynthesizer.setParam(PARAM_SN_NAME, sn);
        }

        // x. 额外 ： 自动so文件是否复制正确及上面设置的参数
        Map<String, String> params = new HashMap<>();
        // 复制下上面的 mSpeechSynthesizer.setParam参数
        // 上线时请删除AutoCheck的调用
        if (!isOnlineSDK) {
            params.put(SpeechSynthesizer.PARAM_TTS_TEXT_MODEL_FILE, TEXT_FILENAME);
            params.put(SpeechSynthesizer.PARAM_TTS_SPEECH_MODEL_FILE, MODEL_FILENAME);
        }

        // 检测参数，通过一次后可以去除，出问题再打开debug
//        InitConfig initConfig = new InitConfig(appId, appKey, secretKey, ttsMode, params, listener);
//        AutoCheck2.getInstance(getApplicationContext()).check(initConfig, new Handler() {
//            @Override
//            /**
//             * 开新线程检查，成功后回调
//             */
//            public void handleMessage(Message msg) {
//                if (msg.what == 100) {
//                    AutoCheck2 autoCheck = (AutoCheck2) msg.obj;
//                    synchronized (autoCheck) {
//                        String message = autoCheck.obtainDebugMessage();
//                        print(message); // 可以用下面一行替代，在logcat中查看代码
//                        // Log.w("AutoCheckMessage", message);
//                    }
//                }
//            }
//
//        });

        // 6. 初始化
        result = mSpeechSynthesizer.initTts(ttsMode);
        checkResult(result, "initTts");
    }

    //合成并播放
    private void speak(String text) {
        /* 以下参数每次合成时都可以修改
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "0");
         *  设置在线发声音人： 0 普通女声（默认） 1 普通男声  3 情感男声<度逍遥> 4 情感儿童声<度丫丫>
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, "5"); 设置合成的音量，0-9 ，默认 5
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, "5"); 设置合成的语速，0-9 ，默认 5
         *  mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, "5"); 设置合成的语调，0-9 ，默认 5
         *
         */
//        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, "4");

        if (mSpeechSynthesizer == null) {
            print("[ERROR], 初始化失败");
            return;
        }
        int result = mSpeechSynthesizer.speak(text);
        print("合成并播放");
        checkResult(result, "speak");
    }

    private boolean checkOfflineResources() {
        String[] filenames = {TEXT_FILENAME, MODEL_FILENAME};
        for (String path : filenames) {
            File f = new File(path);
            if (!f.canRead()) {
                print("[ERROR] 文件不存在或者不可读取，请从demo的assets目录复制同名文件到："
                        + f.getAbsolutePath());
                print("[ERROR] 初始化失败！！！");
                return false;
            }
        }
        return true;
    }

    private void checkResult(int result, String method) {
        if (result != 0) {
            print("error code :" + result + " method:" + method);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
        myRecognizer.release();
        myWakeup.release();
        stopForeground(true);// 停止前台服务--参数：表示是否移除之前的通知
        super.onDestroy();
    }

    // 点击“开始识别”按钮
    // 基于DEMO唤醒词集成第2.1, 2.2 发送开始事件开始唤醒
    private void start() {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put(SpeechConstant.WP_WORDS_FILE, "assets:///WakeUp.bin");
        // "assets:///WakeUp.bin" 表示WakeUp.bin文件定义在assets目录下

        // params.put(SpeechConstant.ACCEPT_AUDIO_DATA,true);
        // params.put(SpeechConstant.IN_FILE,"res:///com/baidu/android/voicedemo/wakeup.pcm");
        // params里 "assets:///WakeUp.bin" 表示WakeUp.bin文件定义在assets目录下
        myWakeup.start(params);
    }

    private void print(String message) {
        Log.i(TAG, message);
//        Toast.makeText(this, message,Toast.LENGTH_LONG).show();
    }

//implement EventListener时需要重写以下方法
//    @Override
//    public void onEvent(String name, String params, byte[] data, int offset, int length) {
//        String logTxt = "name: " + name;
//
//        if (name.equals(SpeechConstant.CALLBACK_EVENT_ASR_PARTIAL)) {
//            // 识别相关的结果都在这里
//            if (params == null || params.isEmpty()) {
//                return;
//            }
//            if (params.contains("\"nlu_result\"")) {
//                // 一句话的语义解析结果
//                if (length > 0 && data.length > 0) {
//                    logTxt += ", 语义解析结果：" + new String(data, offset, length);
//                }
//            } else if (params.contains("\"partial_result\"")) {
//                // 一句话的临时识别结果
//                logTxt += ", 临时识别结果：" + params;
//            }  else if (params.contains("\"final_result\""))  {
//                // 一句话的最终识别结果
//                logTxt += ", 最终识别结果：" + params;
//                speak(params);
//            }  else {
//                // 一般这里不会运行
//                logTxt += " ;params :" + params;
//                if (data != null) {
//                    logTxt += " ;data length=" + data.length;
//                }
//            }
//        } else {
//            // 识别开始，结束，音量，音频数据回调
//            if (params != null && !params.isEmpty()){
//                logTxt += " ;params :" + params;
//            }
//            if (data != null) {
//                logTxt += " ;data length=" + data.length;
//            }
//        }
//    }
}