package com.liepy.mrl;

import android.Manifest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragmentCompat;

import com.liepy.mrl.MyDialog.MyTrueFalseDialog;
import com.liepy.mrl.util.SpeechSynthesis;
import com.liepy.mrl.util.StatusBarUtil;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

//    private LocationReceiver locationReceiver;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
//        Intent i = new Intent(this,MainActivity.class);
//        startActivity(i);

//注册广播并监听
//        locationReceiver = new LocationReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction("location.reportsucc");
//        registerReceiver(locationReceiver, filter);
        /**
         * 隐藏标题栏
         */
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.hide();
        }
        //原标题栏添加返回键
//        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
//        }

        /**
         * 沉浸试状态栏部分
         */
        //当FitsSystemWindows设置 true 时，会在屏幕最上方预留出状态栏高度的 padding
        StatusBarUtil.setRootViewFitsSystemWindows(this,false);
        //设置状态栏透明
        StatusBarUtil.setTranslucentStatus(this);
        //一般的手机的状态栏文字和图标都是白色的, 可如果你的应用也是纯白色的, 或导致状态栏文字看不清
        //所以如果你是这种情况,请使用以下代码, 设置状态使用深色文字图标风格, 否则你可以选择性注释掉这个if内容
        if (!StatusBarUtil.setStatusBarDarkTheme(this, true)) {
            //如果不支持设置深色风格 为了兼容总不能让状态栏白白的看不清, 于是设置一个状态栏颜色为半透明,
            //这样半透明+白=灰, 状态栏的文字能看得清
            StatusBarUtil.setStatusBarColor(this,0x55000000);
        }
//        StatusBarUtil.setStatusBarColor(this,R.color.my_ic_launcher_background);


        /**
         * 加载子布局
         */
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }

        //检查并请求应用所需权限
        initPermission();

        if (!Settings.canDrawOverlays(this)) {
            //若未授权则请求悬浮窗权限
            showDialogs();
        }

        Button startButton = findViewById(R.id.run_button);
        Button quitButton = findViewById(R.id.quit_button);
        Button testButton = findViewById(R.id.test_button);

        quitButton.setEnabled(false);

        startButton.setOnClickListener(v -> {
            startService(new Intent(this, MyService.class));
            quitButton.setEnabled(true);
            startButton.setEnabled(false);
            startButton.setText("已开启");
        });

        quitButton.setOnClickListener(v -> {
            stopService(new Intent(this, MyService.class));
            quitButton.setEnabled(false);
            startButton.setEnabled(true);
            startButton.setText("开启");
        });

        testButton.setOnClickListener(v -> {
            testVoice();
        });


    }

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    // 设置沉浸状态栏
    public void setTranslucentStatus(boolean on) {
        Window win = getWindow();
        WindowManager.LayoutParams winParams = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on) {
            winParams.flags |= bits;
        } else {
            winParams.flags &= ~bits;
        }
        win.setAttributes(winParams);
    }

    private void testVoice(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        //这里设置语音合成参数的默认值
//        pref.getString("name","小O");
        String speaker = pref.getString("speaker", "0");
        String volume = pref.getString("volume", "7");
        String speed = pref.getString("speed", "7");
        String pitch = pref.getString("pitch", "7");


        SpeechSynthesis sst = new SpeechSynthesis(this, handler);
        sst.initTTs(speaker,speed,volume,pitch);
        sst.speak("我现在的声音是这样的，你觉得怎么样？");
    }

    private void showDialogs() {
        final MyTrueFalseDialog dialog = new MyTrueFalseDialog(this);
        dialog.setTitle("温馨提示");
        dialog.setContent("部分底层功能需要获取悬浮窗权限，点击“同意”后，将自动跳转到授权页面，请手动授权。");
        dialog.setSure("同意");
        dialog.getTvSure().setTextColor(Color.parseColor("#0079FF"));
        dialog.getTvCancel().setText("拒绝");
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        dialog.getTvCancel().setOnClickListener(v -> {
            dialog.dismiss();
        });
        dialog.getTvSure().setOnClickListener(v -> {
            dialog.dismiss();
            requestPermissions();
        });
    }

    public void requestPermissions(){
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, 0);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
//        unregisterReceiver(locationReceiver);
        super.onDestroy();
    }

    /**
     * android 6.0 以上需要动态申请权限
     */
    private void initPermission() {
        String[] permissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE,
        };

        ArrayList<String> toApplyList = new ArrayList<>();

        for (String perm : permissions) {
            if (PackageManager.PERMISSION_GRANTED != ContextCompat.checkSelfPermission(this, perm)) {
                toApplyList.add(perm);
                // 进入到这里代表没有权限.
            }
        }
        String[] tmpList = new String[toApplyList.size()];
        if (!toApplyList.isEmpty()) {
            ActivityCompat.requestPermissions(this, toApplyList.toArray(tmpList), 123);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 此处为android 6.0以上动态授权的回调，用户自行实现。
        switch (requestCode) {
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] ==PackageManager.PERMISSION_GRANTED) {
                    // Permission Granted 授予权限
                    //处理授权之后逻辑

                } else {
                    // Permission Denied 权限被拒绝
                    Toast.makeText(this,"您拒绝了授权，部分功能将无法正常使用",Toast.LENGTH_LONG).show();
                }

                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}