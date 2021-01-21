package com.baidu.aip.asrwakeup3.mrl.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.baidu.aip.asrwakeup3.mrl.R;
import com.baidu.aip.asrwakeup3.mrl.SettingsActivity;
import com.baidu.tts.client.SpeechSynthesizer;

import java.io.File;
import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class Util {
    protected SpeechSynthesizer mSpeechSynthesizer;
    protected Context mContext;

//    public util(Context context, SpeechSynthesizer mSynthesizer){
//        this.mContext = context;
//        this.mSpeechSynthesizer = mSynthesizer;
//    }

    public static void print(String str){
        Log.d("打印日志：",str);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Notification getNotification(Context context) {
        String CHANNEL_ONE_ID = "CHANNEL_ONE_ID";
        String CHANNEL_ONE_NAME = "CHANNEL_ONE_ID";
        NotificationChannel notificationChannel = null;
//进行8.0的判断
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                    CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager manager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
        }
        Intent intent = new Intent(context, SettingsActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        Notification notification = new Notification.Builder(context).setChannelId(CHANNEL_ONE_ID)
                .setTicker("Nature")
                .setSmallIcon(R.mipmap.my_ic_launcher)// 设置状态栏内的小图标
                .setContentTitle("小O正在后台等待您的呼唤...")
                .setContentIntent(pendingIntent)// 设置PendingIntent
                .setContentText("尝试说：“小O小O”")
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR;
        return notification;
//        context.startForeground(1, notification);// 参数一：唯一的通知标识；参数二：通知消息。
    }

    public static void search(Context context, String arg){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://www.baidu.com/s?wd="+arg));
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
    }

    public static void startAppByPkg(Context context, String pkg){
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        context.startActivity(intent);
    }

    public static int openAppByName(Context context, String str){
        //应用过滤条件
        Intent mainIntent = new Intent(Intent.ACTION_MAIN,null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PackageManager mPackageManager = context.getPackageManager();

        List<ApplicationInfo> mPkgList = mPackageManager.getInstalledApplications(0);
        for (ApplicationInfo pif : mPkgList){
            String name = pif.loadLabel(mPackageManager).toString();
            String pgkName = pif.packageName;
            String cls = pif.name;
            print(name+"///"+pgkName+"///"+cls);
            if(str.contains(name)){
                print("正在打开"+name);
                startAppByPkg(context, pgkName);
                print("找到App:"+name);
                return 1;
            }
        }
        print(str+"没有找到app");
        return 0;
    }

}
