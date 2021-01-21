package com.baidu.aip.asrwakeup3.mrl;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.wakeup.MyWakeup;
import com.baidu.aip.asrwakeup3.core.wakeup.listener.IWakeupListener;
import com.baidu.aip.asrwakeup3.mrl.sample.util.Auth;
import com.baidu.aip.asrwakeup3.mrl.sample.util.IOfflineResourceConst;
import com.baidu.aip.asrwakeup3.mrl.util.SpeechSynthesis;
import com.baidu.aip.asrwakeup3.mrl.util.TulingApi;
import com.baidu.aip.asrwakeup3.mrl.util.WakeUpAndRecog;
import com.baidu.aip.asrwakeup3.mrl.util.Util;
import com.baidu.speech.EventManager;
import com.baidu.tts.client.SpeechSynthesizer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.baidu.aip.asrwakeup3.mrl.util.Util.print;

public class MyService extends Service implements IStatus, IOfflineResourceConst {
    private static final String TAG = MyService.class.getSimpleName();

    private WakeUpAndRecog war;

    private SpeechSynthesis sst;

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


    protected SpeechSynthesizer mSpeechSynthesizer;

    // ================== 图灵机器人参数设置 ==========================
    //接口地址
    private final String urlString = "http://openapi.tuling123.com/openapi/api/v2";

    private final String apiKey = "6837fbca9f0c494aa0745714db9a80a2";

    private Util myUtil;


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
        Notification notification = Util.getNotification(this);
        startForeground(1,notification);

        //开启唤醒识别等待
        war = new WakeUpAndRecog(this,handler);
        war.wakeupAndRec();

        //初始化语音合成模块
        sst = new SpeechSynthesis(this, handler);
        sst.initTTs(speaker,speed,volume,pitch);

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
                    sst.speak("我在。");
                    Log.d("我在", "111111111111");
                    Toast.makeText(this, "我在。", Toast.LENGTH_LONG).show();
                    Thread.sleep(backTrackInMs);//等待“我在”说完再开始录音
                    war.recog();
                    break;
                case "打开电灯":
                    sst.speak("好的");
                    CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    manager.setTorchMode("0", true);
                    sst.speak("电灯已打开");
                    break;
                case "关闭电灯":
                    sst.speak("好的");
                    CameraManager manager2 = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    manager2.setTorchMode("0", false);
                    sst.speak("电灯已关闭");
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
            sst.speak("好的");
            int result = Util.openAppByName(this, str);
            if (result == 0) {
                sst.speak("奇怪，没有找到呀，你确定安装了吗");
            }
        }
        //说：“搜索”或者“百度”或“百度一下”+想搜索的内容，可以直接跳转到百度搜索结果网页
        else if (str.contains("搜索") || str.contains("百度")) {
            sst.speak("好的，正在搜索。");
            Pattern p = Pattern.compile(".*?(搜索|百度)(一下){0}");
            Matcher m = p.matcher(str);

            if (m.find()) {
                print(m.end() + "");
                String sub_str = str.substring(m.end() + 1);
                print(sub_str);
                Util.search(this, sub_str);
            }
        } else if ((str.contains("打") && str.contains("给")) || str.contains("打电话")) {

        } else {
            String text = TulingApi.chat(str, apiKey);
            sst.speak(text);
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


}