package com.liepy.mrl;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.liepy.mrl.myReceiver.LocationReceiver;

public class MainActivity extends AppCompatActivity {
    private LocationReceiver locationReceiver;
    private String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        //注册广播接收器
//        locationReceiver = new LocationReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction("location.reportsucc");
//        registerReceiver(locationReceiver, filter);
//        LocationReceiver.print("onCreateMainActivity");

        Log.d(TAG,"已创建MainActivity");
        Intent intent = getIntent();
        String pkg = intent.getStringExtra("pkg");
        Intent intent2 = getPackageManager().getLaunchIntentForPackage(pkg);
        startActivityForResult(intent2,0);
        Log.d(TAG,"已打开app");

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        finish();
    }
}