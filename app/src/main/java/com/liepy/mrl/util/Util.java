package com.liepy.mrl.util;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.liepy.mrl.R;
import com.liepy.mrl.SettingsActivity;

import java.util.List;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class Util {

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

    public static Contact getContact(Context context, String str){
        /**
         * @param context
         * @param str 包含联系人的文字
         */
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI, null, null,
                null, null);
        while (cursor.moveToNext()){
            String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
            if(str.contains(name)){
                String contactId = cursor.getString(cursor
                        .getColumnIndex(ContactsContract.Contacts._ID));
                //获取联系人电话号码
                Cursor phoneCursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=" +
                                contactId, null, null);
                if(phoneCursor.moveToNext()) {
                    String phone = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    phone = phone.replace("-", "");
                    phone = phone.replace(" ", "");

                    Contact contact = new Contact(name, phone);

                    return contact;
                }
                phoneCursor.close();
            }
        }
        cursor.close();
        return null;
    }

    public static void call(Context context, String phone){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:"+phone));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static class Contact{
        public String name;
        public String phone;
        public Contact(String name,String phone){
            this.name = name;
            this.phone = phone;
        }
    }

}
