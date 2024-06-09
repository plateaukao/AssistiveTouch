package com.android.mirror.assisttouch.main;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.provider.Settings;
import com.android.mirror.assisttouch.service.AssistiveTouchService;
import com.android.mirror.assisttouch.utils.SystemsUtils;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkPermissionsAndRun();
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (AssistiveTouchService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void checkPermissionsAndRun() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, 0);
            return;
        }

        if (!SystemsUtils.isAccessibilityServiceEnabled(getApplicationContext(), AssistiveTouchService.class)) {
            openAccessibilitySettings(this);
            return;
        }

        Intent intent = new Intent(this, AssistiveTouchService.class);
        if (isMyServiceRunning()) {
            Intent disableServiceIntent = new Intent("info.plateaukao.action.DISABLE_SERVICE");
            sendBroadcast(disableServiceIntent);
        } else {
            startService(intent);
        }

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (SystemsUtils.isAccessibilityServiceEnabled(getApplicationContext(), AssistiveTouchService.class)){
            finish();
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        checkPermissionsAndRun();
    }

    private void openAccessibilitySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
