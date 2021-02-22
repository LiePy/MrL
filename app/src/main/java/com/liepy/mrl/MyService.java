package com.liepy.mrl;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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

import com.baidu.aip.asrwakeup3.core.recog.IStatus;
import com.liepy.mrl.MyMediaPlayer.Song;
import com.liepy.mrl.MyMediaPlayer.Utils;
import com.liepy.mrl.sample.util.IOfflineResourceConst;
import com.liepy.mrl.util.SpeechSynthesis;
import com.liepy.mrl.util.TulingApi;
import com.liepy.mrl.util.WakeUpAndRecog;
import com.liepy.mrl.util.Util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.liepy.mrl.util.Util.print;

public class MyService extends Service implements IStatus, IOfflineResourceConst {
    private static final String TAG = MyService.class.getSimpleName();
    private volatile boolean CHATMODE = false;    //是否聊天模式标志，若是，则小O说完后会继续听你说
    private volatile boolean REPLYONCE = false;   //临时判断标志，只接一次话
    private volatile boolean PLAYMUSIC = false;   //播放音乐标志

    List<Song> list;

    //语言唤醒识别实例
    private WakeUpAndRecog war;

    //语言合成实例
    private SpeechSynthesis sst;

    // ================== 语音合成精简版初始化参数 ==========================

    private String speaker;
    private String volume;
    private String speed;
    private String pitch;

    public MyService() {
    }

    Handler handler = new Handler() {

        @SuppressLint("HandlerLeak")
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            try {
                handleMsg(msg);
            } catch (@SuppressLint("NewApi") InterruptedException | CameraAccessException |
                    PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

    };

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();

        //读取本地设置
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        //这里设置语音合成参数的默认值
//        pref.getString("name","小O");
        speaker = pref.getString("speaker", "0");
        volume = pref.getString("volume", "7");
        speed = pref.getString("speed", "7");
        pitch = pref.getString("pitch", "7");

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
                    REPLYONCE = true;
                    sst.speak("我在。");
                    Log.d("我在", "111111111111");
                    Toast.makeText(this, "我在。", Toast.LENGTH_LONG).show();
//                    Thread.sleep(backTrackInMs);//等待“我在”说完再开始录音  //增加了statuscode2判断，已弃用
//                    war.recog();
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
                    sst.speak("好的，正在为您播放本地歌曲：");
                    list = Utils.getmusic(getBaseContext());
                    String name = list.get(0).song;
                    PLAYMUSIC = true;
                    sst.speak(name);
                    break;
                case "暂停":
                case "拍照":
                    sst.speak("好的");
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    startActivity(intent);
                    sst.speak("相机已打开");
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
            cmd(str11);     //分析指令并执行

        } else if (msg.what ==200){     //收到语音合成播放结束消息，修改了源码，见UiMessageListener:63
            int progress = msg.arg1;
            if(CHATMODE){         //控制循环对话，聊天模式
                war.recog();
            }
            if(REPLYONCE){        //控制对话一次
                war.recog();
                REPLYONCE = false;
            }
            if(PLAYMUSIC){      //控制播放歌曲
                String p = list.get(0).path;//获得歌曲的地址
                Utils.play(p);
                PLAYMUSIC = false;
            }
            Log.d("1111111111",progress+"");
        }
    }

    //语音识别转文字后，指令分类识别
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void cmd(String str) throws CameraAccessException {
        //首先判断是否聊天模式，防止聊天模式下触发指令
        if (!CHATMODE){     //非聊天模式

            //说：“打开”+APP的名字，可以打开任意已安装的APP
            if (str.contains("打开")) {
                if(str.contains("电灯")||str.contains("手电筒")){
                    sst.speak("好的");
                    CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    manager.setTorchMode("0", true);
                    sst.speak("闪光灯已打开");
                }else{
                    sst.speak("好的");
                    int result = Util.openAppByName(this, str);
                    if (result == 0) {
                        sst.speak("奇怪，没有找到呀，你确定安装了吗");
                    }
                }
            }

            //说：“关闭”...时，关闭闪关灯或者关闭当前应用程序返回桌面
            else if(str.contains("关闭")){
                if(str.contains("电灯")||str.contains("手电筒")){
                    sst.speak("好的");
                    CameraManager manager2 = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    manager2.setTorchMode("0", false);
                    sst.speak("闪光灯已关闭");
                }else{//退到桌面
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_MAIN);// "android.intent.action.MAIN"
                    intent.addCategory(Intent.CATEGORY_HOME); //"android.intent.category.HOME"
                    startActivity(intent);
                }

            }

            //说：“搜索”或者“百度”或“百度一下”+想搜索的内容，可以直接跳转到百度搜索结果网页
            else if (str.contains("搜索") || str.contains("百度")) {
                sst.speak("好的，正在搜索。");
                Pattern p = Pattern.compile(".*?(搜索|百度)(一下)?");
                Matcher m = p.matcher(str);

                if (m.find()) {
                    print(m.end() + "");
                    String sub_str = str.substring(m.end());
                    print(sub_str);
                    Util.search(this, sub_str);
                }

            //说：打电话给...时，可以从通讯录中读取联系人信息或者直接拨打报的号码============(拨打号码尚未实现)
            } else if ((str.contains("打") && str.contains("给")) || str.contains("打电话")) {
                sst.speak("好的。");
                Util.Contact contact = Util.getContact(this, str);
                if (contact!=null){
                    String name = contact.name;
                    String phone = contact.phone;
                    sst.speak("正在打给"+name);
                    Util.call(this, phone);
                }else{
                    sst.speak("抱歉，通讯录里没有找到这个人");
                }

            }

            //说：“聊天”时，进入聊天模式，循环对话
            else if(str.contains("聊天")){
                CHATMODE = true;    //聊天模式标志开启
                sst.speak("好的，如果不想和我聊了你就说“不聊了”。嗯~~...你想聊啥");
                //war.recog();      //如果直接recog它会录进去自己的声音，自己和自己聊天
            }

            //说：“退出”或“退下”时，关闭后台服务，不在监听唤醒，若在聊天模式也退出聊天模式
            else if(str.contains("退出")||str.contains("退下")){
                Log.d(TAG,"退出ing");
                sst.speak("好的，拜拜");
                CHATMODE = false;   //聊天模式标志关闭
                this.onDestroy();
            }

            //不符合以上条件则默认为与机器人聊天
            else{      //与图灵机器人聊天
                new Thread(() -> {
                    String text = TulingApi.chat(str);
                    sst.speak(text);
                }).start();

            }

        }else{      //聊天模式
            if(str.contains("不聊了")||str.contains("退出")||str.contains("退下")){
                CHATMODE = false;   //聊天模式标志关闭
                sst.speak("好的，已退出聊天模式");
            }else{
                //与图灵机器人聊天，因为调用图灵API接口是耗时操作，需要放在新线程中
                new Thread(() -> {
                    String text = TulingApi.chat(str);
                    sst.speak(text);
                }).start();
            }

        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {       //销毁回收百度语音识别的监听器
        Log.d(TAG, "onDestroy()");
        if(war.myRecognizer != null){
            war.myRecognizer.release();
            Log.d(TAG,"myRecognizer released");
        }
        if(war.myWakeup != null){
            war.myWakeup.release();
            Log.d(TAG,"myWakeup released");
        }
        stopForeground(true);// 停止前台服务--参数：表示是否移除之前的通知
        super.onDestroy();
    }
}