package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

public class MemrMainActivity extends Activity {

    private final static int PERMISSION_STATE = 0;


    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.memr_activity_main);

        // 2021.07.09 WOOIL - 권한체크
        boolean bCheck = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED ||
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)

                bCheck = true;
        }

        if (bCheck == true) {

            // 권한을 받아야함.
            requestPermissions(new String[]{Manifest.permission.CAMERA
                            , Manifest.permission.RECORD_AUDIO
                            , Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }
                    , PERMISSION_STATE);

        } else {

            Intent i = new Intent(this, Splash.class);
            startActivity(i);

            finish();

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        Intent i = new Intent(this, Splash.class);
        startActivity(i);

        finish();

    }

}
