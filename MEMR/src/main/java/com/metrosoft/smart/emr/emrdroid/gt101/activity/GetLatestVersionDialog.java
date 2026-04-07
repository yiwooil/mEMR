package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ServletHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import org.json.JSONException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class GetLatestVersionDialog extends Activity implements CompoundButton.OnCheckedChangeListener, Button.OnClickListener {

    private ServletHelper mServletHelper = new ServletHelper();
    protected ProgressDialog mDialog = null;
    protected Handler mHandler = new Handler();

    private String mXml = "";

    private String mMetrosoftUrl = "";
    private String mGoogleUrl = "";

    private TextView mCurrentVersionTextView;
    private TextView mLatestVersionTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
		/*
    	super.onCreate(savedInstanceState, R.layout.set_config, "<�ݱ�");
    	*/
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.get_latest_version_dialog);
    	
    	/*
    	super.setQueryButton(false);
    	super.setQuickMenuOff();
    	*/
        mCurrentVersionTextView = (TextView) findViewById(R.id.current_version_textview);
        mLatestVersionTextView = (TextView) findViewById(R.id.latest_version_textview);
        // 초기화
        mCurrentVersionTextView.setText(" ");
        mLatestVersionTextView.setText(" ");

//        // 셋팅을 읽는다.
//        SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
//    	String firstInPatientList=prefs.getString("first_in_patient_list", "true");

//        // 컨트롤 초기화 및 리스너
//        CheckBox firstInPatientListCheckBox = (CheckBox)findViewById(R.id.first_in_patient_list_checkbox);
//		firstInPatientListCheckBox.setChecked(firstInPatientList.equals("true"));
//		firstInPatientListCheckBox.setOnCheckedChangeListener(this);

        // 현재버전을 화면에 출력한다.
        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
            mCurrentVersionTextView.setText(versionName);
        } catch (NameNotFoundException e) {
            mCurrentVersionTextView.setText(" ");
        }

        // 업데이트버튼 처리
        Button goPatchGoogleButton = (Button) findViewById(R.id.go_patch_google_button);
        goPatchGoogleButton.setOnClickListener(this);
        Button goPatchMetrosoftButton = (Button) findViewById(R.id.go_patch_metrosoft_button);
        goPatchMetrosoftButton.setOnClickListener(this);
        goPatchMetrosoftButton.setVisibility(View.GONE); // 일단 안보이게 처리

        // 최신버전 불러오기
        String packageName = this.getPackageName();
        this.getApkVersion(packageName);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//		if (buttonView.getId() == R.id.first_in_patient_list_checkbox) {
//			setPreferenceString("first_in_patient_list", isChecked==true?"true":"false");
//		}
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.go_patch_metrosoft_button) {
            if (!"".equals(mMetrosoftUrl)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mMetrosoftUrl)));
            }
        } else if (v.getId() == R.id.go_patch_google_button) {
            if (!"".equals(mGoogleUrl)) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(mGoogleUrl)));
            }
        }
    }

//	private void setPreferenceString(String key, String value) {
//		SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
//		SharedPreferences.Editor ed = prefs.edit();
//		ed.putString(key, value);
//		ed.commit();
//	}

    private void getApkVersion(final String packageName) {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String pkgName = packageName;
                if (packageName.equals(EmrSettingsUtil.PACKAGE_MEMR)) {
                    pkgName = "EmrDroid.GT101";
                }
                //String hospitalId=EmrSettingsUtil.getHospitalId(getBaseContext());
                //String userId=EmrSettingsUtil.getUserId(getBaseContext());
                mXml = "";
                String url = "ApkVersionServlet?apkname=" + pkgName;
                try {
                    mServletHelper.setServletIp("");
                    mXml = mServletHelper.getXml(url, null);
                } catch (KeyManagementException e) {
                    mXml = "";
                } catch (MalformedURLException e) {
                    mXml = "";
                } catch (NoSuchAlgorithmException e) {
                    mXml = "";
                } catch (IOException e) {
                    mXml = "";
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        afterGetApkVersion();
                        mDialog.dismiss();
                    }
                });
            }
        }).start();
    }

    private void afterGetApkVersion() {
        try {
            ResultSetHelper rsHelper = new ResultSetHelper(mXml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rsHelper.getRecordCount() > 0) {
                mLatestVersionTextView.setText(rsHelper.getString(0, "version_name"));
                mMetrosoftUrl = rsHelper.getString(0, "metrosoft_url");
                mGoogleUrl = rsHelper.getString(0, "google_url");
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }

}
