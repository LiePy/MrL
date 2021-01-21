package com.baidu.aip.asrwakeup3.mrl;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceFragmentCompat;

import com.baidu.aip.asrwakeup3.mrl.MyDialog.MyTrueFalseDialog;
import com.baidu.aip.asrwakeup3.mrl.myReceiver.LocationReceiver;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ADDRESS = 100;
    private LocationReceiver locationReceiver;
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
//        Intent i = new Intent(this,MainActivity.class);
//        startActivity(i);


//        locationReceiver = new LocationReceiver();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction("location.reportsucc");
//        registerReceiver(locationReceiver, filter);

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        initPermission();

        if (!Settings.canDrawOverlays(this)) {
            //若未授权则请求权限
           showDialogs();
        }

//        startService(new Intent(getBaseContext(), MyService.class));

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
        stopService(new Intent(getBaseContext(), MyService.class));
    }

    @Override
    protected void onStop() {
        super.onStop();
        startService(new Intent(getBaseContext(), MyService.class));
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
                    Toast.makeText(this,"您拒绝了授权",Toast.LENGTH_LONG).show();
                }

                break;
            default:
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


}