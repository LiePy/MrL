package com.baidu.aip.asrwakeup3.mrl.util;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.RecogResult;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.baidu.aip.asrwakeup3.core.wakeup.MyWakeup;
import com.baidu.aip.asrwakeup3.core.wakeup.listener.IWakeupListener;
import com.baidu.aip.asrwakeup3.core.wakeup.listener.RecogWakeupListener;
import com.baidu.aip.asrwakeup3.mrl.MyService;
import com.baidu.speech.EventManager;
import com.baidu.speech.asr.SpeechConstant;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class WakeUpAndRecog {
    private static final String TAG = MyService.class.getSimpleName();
    private Context context;
    private Handler handler;

    // ================== 语音识别参数设置 ==========================

    protected MyRecognizer myRecognizer;
    protected MyWakeup myWakeup;
    protected IWakeupListener listener;

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
    //监听唤醒词然后录制识别
    public WakeUpAndRecog(Context context,Handler handler){
        this.context = context;
        this.handler = handler;
    }

    public void wakeupAndRec() {
        IRecogListener recogListener = new MessageStatusRecogListener(handler) {
            @Override
            public void onAsrFinalResult(String[] results, RecogResult recogResult) {
                super.onAsrFinalResult(results, recogResult);
                Log.d("识别结果：", results[0]);
            }
        };
        // 改为 SimpleWakeupListener 后，不依赖handler，但将不会在UI界面上显示
        myRecognizer = new MyRecognizer(context, recogListener);

        listener = new RecogWakeupListener(handler);
        myWakeup = new MyWakeup(context, listener);
        myWakeup.setEventListener(listener); // 替换原来的 listener

        new Thread(() -> start()).start();
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
}
