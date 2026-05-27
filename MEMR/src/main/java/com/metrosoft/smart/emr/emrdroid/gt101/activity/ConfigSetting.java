package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

public class ConfigSetting extends PreferenceActivity implements OnPreferenceClickListener {

    private long userNameClick = 0;
    private long userNameClickTime;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.config_settings);

        // 현재버전
        Preference currentVersion = (Preference) findPreference("current_version");
        currentVersion.setTitle("버전 " + getVersionName());
        currentVersion.setOnPreferenceClickListener(this);
        // 병원명
        Preference hosName = (Preference) findPreference("hos_name");
        hosName.setTitle(EmrSettingsUtil.getHospitalName(getBaseContext()));
        hosName.setOnPreferenceClickListener(this);
        // 사용자명
        Preference userName = (Preference) findPreference("user_name");
        userName.setTitle(EmrSettingsUtil.getUserName(getBaseContext()));
        userName.setOnPreferenceClickListener(this);
        // 개인정보보호
        Preference privacy = (Preference) findPreference("privacy");
        privacy.setOnPreferenceClickListener(this);
        // 재설치
        Preference install = (Preference) findPreference("install");
        install.setOnPreferenceClickListener(this);
    }

    private String getVersionName() {
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
            return versionName;
        } catch (NameNotFoundException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            return "";
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        // TODO Auto-generated method stub
        final int duration = 2000;
        String key = preference.getKey();
        if (key.equals("current_version")) {
            // 2019.10.29 WOOIL - 일단 막는다.
            //startActivity(new Intent(this, GetLatestVersionDialog.class));
            return true;
        } else if (key.equals("user_name")) {

            long currentTime = System.currentTimeMillis(); // 현재시간
            userNameClick++; // 클릭횟수증가
            if (userNameClick == 1) {
                // 맨처음이면 현제시간을 보관. 5회중 최초클릭시간임.
                userNameClickTime = currentTime;
            }
            if (currentTime - userNameClickTime > duration) {
                // 최초클릭시간에서 일정시간 지났으면 초기화.
                userNameClick = 1;
                userNameClickTime = currentTime;
            }
            if (userNameClick == 5) {
                Intent i = new Intent(this, Register.class);
                i.putExtra("re", "y");
                startActivity(i);
                return true;
            }
        } else if (key.equals("privacy")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = Uri.parse("https://yiwooil.github.io/privacy.htm");
            intent.setData(uri);
            startActivity(intent);
        } else if (key.equals("install")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String apkUrl = "http://www.metrosoft.co.kr/MEMR/SETUP/MEMR.apk";
            Uri uri = Uri.parse(apkUrl);
            intent.setData(uri);
            startActivity(intent);
        }
        return false;
    }

}
