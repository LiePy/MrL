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
import android.preference.PreferenceManager;
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
import com.baidu.aip.asrwakeup3.mrl.util.MyRequest;
import com.baidu.aip.asrwakeup3.mrl.util.TulingApi;
import com.baidu.aip.asrwakeup3.mrl.util.util;
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
import static com.baidu.aip.asrwakeup3.mrl.util.util.checkResult;
import static com.baidu.aip.asrwakeup3.mrl.util.util.print;

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

    private String speaker;
    private String volume;
    private String speed;
    private String pitch;

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

    // ================== 图灵机器人参数设置 ==========================
    //接口地址
    private final String urlString = "http://openapi.tuling123.com/openapi/api/v2";

    private final String apiKey = "6837fbca9f0c494aa0745714db9a80a2";

    private util myUtil;


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

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();

        appId = Auth.getInstance(this).getAppId();
        appKey = Auth.getInstance(this).getAppKey();
        secretKey = Auth.getInstance(this).getSecretKey();
        sn = Auth.getInstance(this).getSn(); // 纯离线合成必须有此参数；离在线合成SDK没有此参数

        //读取本地设置
//        SharedPreferences pref = getSharedPreferences("setData",MODE_PRIVATE);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
//        pref.getString("name","小O");
        //这里设置语音合成参数的默认值
        speaker = pref.getString("speaker", "0");
        volume = pref.getString("volume", "7");
        speed = pref.getString("speed", "7");
        pitch = pref.getString("pitch", "7");
        backTrackInMs = pref.getInt("blank", 1000);

        //显示前台通知
        Notification notification = util.getNotification(this);
        startForeground(1,notification);

        //开启唤醒识别等待
        wakeupAndRec();

        //初始化语音合成模块
        initTTs();

        myUtil = new util(this, mSpeechSynthesizer);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void handleMsg(Message msg) throws InterruptedException, CameraAccessException, PackageManager.NameNotFoundException {
//        super.handleMsg(msg);
        if (msg.what == STATUS_WAKEUP_SUCCESS) { // 唤醒词识别成功的回调，见RecogWakeupListener
            // 此处 开始正常识别流程

            String str_wp = msg.obj.toString();
            print("唤醒词：" + str_wp);

            switch (str_wp) {
                case "小O小O":
                case "小O在吗":
                case "你好小O":
                    myUtil.speak("我在。");
                    Log.d("我在", "111111111111");
                    Toast.makeText(this, "我在。", Toast.LENGTH_LONG).show();
                    Thread.sleep(backTrackInMs);//等待“我在”说完再开始录音
                    recog();
                    break;
                case "打开电灯":
                    myUtil.speak("好的");
                    CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    manager.setTorchMode("0", true);
                    myUtil.speak("电灯已打开");
                    break;
                case "关闭电灯":
                    myUtil.speak("好的");
                    CameraManager manager2 = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    manager2.setTorchMode("0", false);
                    myUtil.speak("电灯已关闭");
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
        } else if (msg.what == 21567) {  //识别成功的回调，修改了源码，见MesStatusRecogListener
            String str11 = " ";
            if (msg.obj != null) {
                str11 = msg.obj.toString(); //唤醒后你说的话识别成文字
            }
            Toast.makeText(this, "你说:" + str11, Toast.LENGTH_SHORT).show();
            cmd(str11);

        }
    }

    //语音识别转文字后，指令分类识别
    public void cmd(String str) {
        //说：“打开”+APP的名字，可以打开任意已安装的APP
        if (str.contains("打开")) {
            myUtil.speak("好的");
            int result = util.openAppByName(this, str);
            if (result == 0) {
                myUtil.speak("奇怪，没有找到呀，你确定安装了吗");
            }
        }
        //说：“搜索”或者“百度”或“百度一下”+想搜索的内容，可以直接跳转到百度搜索结果网页
        else if (str.contains("搜索") || str.contains("百度")) {
            myUtil.speak("好的，正在搜索。");
            Pattern p = Pattern.compile(".*?(搜索|百度)(一下){0}");
            Matcher m = p.matcher(str);

            if (m.find()) {
                print(m.end() + "");
                String sub_str = str.substring(m.end() + 1);
                print(sub_str);
                util.search(this, sub_str);
            }
        } else if ((str.contains("打") && str.contains("给")) || str.contains("打电话")) {

        } else {
            String text = TulingApi.chat(str, apiKey);
            myUtil.speak(text);
        }
    }

    public void recog() {
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



    //监听唤醒词然后录制识别
    public void wakeupAndRec() {
        IRecogListener recogListener = new MessageStatusRecogListener(handler) {
            @Override
            public void onAsrFinalResult(String[] results, RecogResult recogResult) {
                super.onAsrFinalResult(results, recogResult);
                Log.d("识别结果：", results[0]);
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
            isSuccess = util.checkOfflineResources(TEXT_FILENAME, MODEL_FILENAME);
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
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEAKER, speaker);
        // 设置合成的音量，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_VOLUME, volume);
        // 设置合成的语速，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_SPEED, speed);
        // 设置合成的语调，0-15 ，默认 5
        mSpeechSynthesizer.setParam(SpeechSynthesizer.PARAM_PITCH, pitch);

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
}