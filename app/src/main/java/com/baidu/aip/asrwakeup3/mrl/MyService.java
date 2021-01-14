package com.baidu.aip.asrwakeup3.mrl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.baidu.aip.asrwakeup3.core.recog.MyRecognizer;
import com.baidu.aip.asrwakeup3.core.recog.RecogResult;
import com.baidu.aip.asrwakeup3.core.recog.listener.IRecogListener;
import com.baidu.aip.asrwakeup3.core.recog.listener.MessageStatusRecogListener;
import com.baidu.aip.asrwakeup3.core.wakeup.MyWakeup;
import com.baidu.aip.asrwakeup3.core.wakeup.listener.IWakeupListener;
import com.baidu.aip.asrwakeup3.core.wakeup.listener.RecogWakeupListener;
import com.baidu.speech.EventListener;
import com.baidu.speech.EventManager;
import com.baidu.speech.EventManagerFactory;
import com.baidu.speech.asr.SpeechConstant;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MyService extends Service implements IStatus {
    private static final String TAG = MyService.class.getSimpleName();
    protected MyRecognizer myRecognizer;
    protected MyWakeup myWakeup;

    private int backTrackInMs = 1500;


    public MyService() {
    }

    Handler handler = new Handler() {

        /*
         * @param msg
         */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            handleMsg(msg);
        }

    };

    protected void handleMsg(Message msg) {
//        super.handleMsg(msg);
        if (msg.what == STATUS_WAKEUP_SUCCESS) { // 唤醒词识别成功的回调，见RecogWakeupListener
            // 此处 开始正常识别流程
            Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put(SpeechConstant.ACCEPT_AUDIO_VOLUME, false);
            params.put(SpeechConstant.VAD, SpeechConstant.VAD_DNN);
            // 使用1537中文模型。其它PID参数请看文档
            params.put(SpeechConstant.PID, 1537);
            if (backTrackInMs > 0) {
                // 方案1  唤醒词说完后，直接接句子，中间没有停顿。开启回溯，连同唤醒词一起整句识别。
                // System.currentTimeMillis() - backTrackInMs ,  表示识别从backTrackInMs毫秒前开始
                params.put(SpeechConstant.AUDIO_MILLS, System.currentTimeMillis() - backTrackInMs);
            }
            myRecognizer.cancel();
            myRecognizer.start(params);

        }else if(msg.what==21567){
            String str=" ";
            if(msg.obj!=null){
                str = msg.obj.toString();
            }
            Toast.makeText(this,"唤醒成功:"+str,Toast.LENGTH_LONG).show();

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();

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
                .setSmallIcon(R.mipmap.ic_launcher)// 设置状态栏内的小图标
                .setContentTitle("MrL正在后台等待您的呼唤...")
                .setContentIntent(pendingIntent)// 设置PendingIntent
                .setContentText("尝试说：“百度一下”")
                .build();
        notification.flags|= Notification.FLAG_NO_CLEAR;
        startForeground(1, notification);


        // 参数一：唯一的通知标识；参数二：通知消息。
        startForeground(110, notification);// 开始前台服务

        /*
        以下为语音唤醒部分
         */
//        EventManager wp= EventManagerFactory.create(this,"wp");
//        EventListener yourListener = new EventListener() {
//            @Override
//            public void onEvent(String name, String params, byte [] data, int
//                    offset, int length) {
//                Log.d(TAG, String.format("event: name=%s, params=%s", name, params));
//                //唤醒事件
//                if(name.equals("wp.data")){
//                    try {
//                        JSONObject json = new JSONObject(params);
//                        int errorCode = json.getInt("errorCode");
//                        if(errorCode == 0){
//                            //唤醒成功
//                            Log.d("进入到唤醒成功"," "+params);
//                            Toast.makeText(getApplicationContext(),"唤醒成功:"+params,Toast.LENGTH_LONG).show();
//
//                        } else {
//                            //唤醒失败
//                        }
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
//                } else if("wp.exit".equals(name)){
//                    //唤醒已停止
//                }
//            }
//        };
//        wp.registerListener(yourListener);
//        HashMap map = new HashMap();
//        map.put(SpeechConstant.WP_WORDS_FILE, "assets://WakeUp.bin");
//        String json = new JSONObject(map).toString();
//        wp.send(SpeechConstant.WAKEUP_START, json, null, 0, 0);


        /*
        以下为唤醒加识别
         */
        IRecogListener recogListener = new MessageStatusRecogListener(handler){
            @Override
            public void onAsrFinalResult(String[] results, RecogResult recogResult) {
                super.onAsrFinalResult(results, recogResult);
                Log.d("识别结果：",results[0]);
            }
        };
        // 改为 SimpleWakeupListener 后，不依赖handler，但将不会在UI界面上显示
        myRecognizer = new MyRecognizer(this, recogListener);

        IWakeupListener listener = new RecogWakeupListener(handler);
        myWakeup = new MyWakeup(this, listener);
        myWakeup.setEventListener(listener); // 替换原来的 listener

        new Thread(() -> start()).start();
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy()");
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