package com.liepy.mrl.myReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LocationReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        print("收到广播");
        String intentAction = intent.getAction();
        assert intentAction != null;
        if (intentAction.equals("location.reportsucc")) {
            print("收到广播2");
            String pkg = intent.getStringExtra("pkg");
            startApp(pkg, context);
        }
    }

    public static void startApp(String pkg, Context mContext){
        print("正在打开apping");
        Intent intent = mContext.getPackageManager().getLaunchIntentForPackage(pkg);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        mContext.startActivity(intent);
    }

    public static void print(String str){
        Log.d("skksag",str);
    }
}
