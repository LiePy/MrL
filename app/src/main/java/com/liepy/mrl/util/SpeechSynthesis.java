package com.liepy.mrl.util;

import android.content.Context;
import android.os.Handler;

import com.liepy.mrl.sample.listener.UiMessageListener;
import com.liepy.mrl.sample.util.Auth;
import com.liepy.mrl.sample.util.IOfflineResourceConst;
import com.baidu.tts.chainofresponsibility.logger.LoggerProxy;
import com.baidu.tts.client.SpeechSynthesizer;
import com.baidu.tts.client.SpeechSynthesizerListener;
import com.baidu.tts.client.TtsMode;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static com.liepy.mrl.util.Util.print;


public class SpeechSynthesis implements IOfflineResourceConst {
    private Context context;
    private Handler handler;
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

    public SpeechSynthesis(Context context, Handler handler){
        this.context = context;
        this.handler = handler;

        getParam();
    }

    public void getParam(){
        //获取appId、appKey等必须参数
        appId = Auth.getInstance(context).getAppId();
        appKey = Auth.getInstance(context).getAppKey();
        secretKey = Auth.getInstance(context).getSecretKey();
        sn = Auth.getInstance(context).getSn(); // 纯离线合成必须有此参数；离在线合成SDK没有此参数
    }

    //语音合成初始化
    public void initTTs(String speaker,String speed,String volume,String pitch) {
        LoggerProxy.printable(true); // 日志打印在logcat中
        boolean isSuccess;
        if (!isOnlineSDK) {
            // 检查2个离线资源是否可读
            isSuccess = checkOfflineResources(TEXT_FILENAME, MODEL_FILENAME);
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
        mSpeechSynthesizer.setContext(context);

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

    //合成并播放
    public void speak(String text) {
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

    public static boolean checkOfflineResources(String TEXT_FILENAME, String MODEL_FILENAME) {
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

    public static void checkResult(int result, String method) {
        if (result != 0) {
            print("error code :" + result + " method:" + method);
        }
    }
}
