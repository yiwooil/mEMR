package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Device;

import org.json.JSONException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

public class Login extends MyActivity {
    /**
     * Called when the activity is first created.
     */
    private String userId;
    private String password;
    private String hospitalId;
    private String hospitalName;
    private String maskYn;
    private String servletUseYn;
    private String servletIp;
    private String emrCompany;
    private String xml;
    private ArrayList<String> hospitalIdList = new ArrayList<String>();
    private ArrayList<String> hospitalNameList = new ArrayList<String>();
    private ArrayList<String> maskYnList = new ArrayList<String>();
    private ArrayList<String> servletUseYnList = new ArrayList<String>();
    private ArrayList<String> servletIpList = new ArrayList<String>();
    private ArrayList<String> emrCompanyList = new ArrayList<String>();

    private LinearLayout mLayout;
    private EditText mLoginUserId;
    private TextView mVersionTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        Log.d("EmrDroid", "Login");

        // 로고 이미지
        ImageView imageLogo = (ImageView) findViewById(R.id.logo_image);
        imageLogo.setScaleType(ScaleType.FIT_XY);

        mLayout = (LinearLayout) View.inflate(Login.this, R.layout.login_dialog, null);

        // 앱 버전 정보 표시
        mVersionTextView = (TextView) mLayout.findViewById(R.id.versionTextView);
        if (mVersionTextView != null) {
            mVersionTextView.setText(getAppVersionName());
            // 버전 정보를 길게 누르면 APK 다운로드 URL 호출
            mVersionTextView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    openApkDownloadUrl();
                    return true;
                }
            });
        }

        // 로그인 다이얼로그
        /*
        AlertDialog.Builder builder = new AlertDialog.Builder(Login.this);
        builder.setView(mLayout);
        //builder.setTitle(R.string.login_dialog_title);
        View customTitleView = getLayoutInflater().inflate(R.layout.login_dialog_header, null);
        builder.setCustomTitle(customTitleView);
        builder.setInverseBackgroundForced(true); // 완벽하지는 않지만 검은색 테두리를 없애는 효과가 있음.
        builder.setCancelable(false);
        builder.show();
        */

        Intent intent = getIntent();
        String isLicenseCheckError = intent.getStringExtra("is_license_check_error");
        if (isLicenseCheckError == null)
            isLicenseCheckError = ""; // 2022.12.15 WOOIL - null이 넘어오는 경우가 있음.
        String titleText = getString(R.string.login_dialog_title) + isLicenseCheckError;

        final Dialog dlg = new Dialog(Login.this);
        dlg.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        dlg.setContentView(mLayout);
        dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dlg.getWindow().setBackgroundDrawableResource(R.color.temr_titlebackground);
        dlg.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.login_icon);
        dlg.setTitle(titleText);
        dlg.setCancelable(false);
        dlg.show();

        final Button okButton = (Button) mLayout.findViewById(R.id.okButton);
        okButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                int index = ((Spinner) mLayout.findViewById(R.id.hospitalListSpinner)).getSelectedItemPosition();
                userId = mLoginUserId.getText().toString().trim();
                password = ((EditText) mLayout.findViewById(R.id.loginPassword)).getText().toString().trim();
                hospitalId = hospitalIdList.get(index);
                hospitalName = hospitalNameList.get(index);
                maskYn = maskYnList.get(index);
                servletUseYn = servletUseYnList.get(index);
                servletIp = servletIpList.get(index);
                emrCompany = emrCompanyList.get(index);

                // 이하를 막으면 안됨.
                // afterChkUser에 있어서 중복으로 생각하여 막으면 로그인 오류가 발생함.
                SharedPreferences prefs = getSharedPreferences("emrdroid", MODE_PRIVATE);
                SharedPreferences.Editor ed = prefs.edit();
                //ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
                ed.putString("userId", userId);
                ed.putString("hospitalId", hospitalId);
                ed.putString("hospitalName", hospitalName);
                ed.putString("maskYn", maskYn);
                ed.putString("servletUseYn", servletUseYn);
                ed.putString("servletIp", servletIp);
                ed.putString("emrCompany", emrCompany);
                ed.commit();

                if (userId == null || userId.length() == 0) return;
                if (password == null || password.length() == 0) return;
                chkUser();
            }
        });
        final Button cancelButton = (Button) mLayout.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });

        // 로그인id 기본 영문자판으로 나타나게
        mLoginUserId = (EditText) mLayout.findViewById(R.id.loginUserId);
        mLoginUserId.setText(getUserId()); // 2023.03.23 WOOIL - 추가. 이전에 로글인한 ID가 기본으로 표시되도록 수정.
        mLoginUserId.setPrivateImeOptions("defaultInputmode=english;");

        loadHospitalList(mLayout); // 병원목록
    }

    // 사용자id,비밀번호 체크
    private void chkUser() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.process_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                // 2013.09.09 WOOIL - ver=2로 넘기면 yes 뒤에 사용자명이 붙어서 넘어온다.
                try {
                    String url = "LoginServlet" +
                            "?hospitalid=" + hospitalId +
                            "&userid=" + userId +
                            "&password=" + URLEncoder.encode(password, "UTF-8") +
                            "&ver=2";

                    xml = getXml(url);
                    Log.d("EmrDroid", "LoginServlet xml = " + xml);
                    mHandler.post(new Runnable() {
                        public void run() {
                            // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                            // 이를 방지함.
                            try {
                                afterChkUser();
                                mDialog.dismiss();
                            } catch (Exception e) {
                                ;
                            }
                        }
                    });
                }  catch (UnsupportedEncodingException e) {
                    showSimpleDialog(e.getMessage());
                }catch (Exception e) {
                    showSimpleDialog(e.getMessage());
                }
            }
        }).start();
    }

    private void afterChkUser() {
        // 오류발생
        if (super.getXmlError() == true) {
            super.showToastText(super.getXmlErrorMessage());
            return;
        }
        if (xml.startsWith("yes")) {

            SharedPreferences prefs = getSharedPreferences("emrdroid", MODE_PRIVATE);
            SharedPreferences.Editor ed = prefs.edit();
            //ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
            ed.putString("userId", userId);
            ed.putString("hospitalId", hospitalId);
            ed.putString("hospitalName", hospitalName);
            ed.putString("maskYn", maskYn);
            ed.putString("servletUseYn", servletUseYn);
            ed.putString("servletIp", servletIp);
            ed.putString("emrCompany", emrCompany);
            ed.putString("userName", xml.substring(3));
            ed.commit();

            getHospitalInformation();

//	    	//Intent i = new Intent(Login.this, MainMenu.class);
//	    	//Intent i = new Intent(Login.this, MainMenuGrid.class);
//			Intent i = new Intent(Login.this, InPatientList.class);
//	    	startActivity(i);
//	    	finish();
        } else {
            showSimpleDialog(xml);
        }

    }

    private void getHospitalInformation() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.process_wait_message), true);
        new Thread(new Runnable() {
            public void run() {

                String url = "HospitalInformationServlet" +
                             "?hospitalid=" + hospitalId +
                             "&mode=2";

                xml = getXml(url);
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetHospitalInformation();
                            mDialog.dismiss();
                        } catch (Exception e) {
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetHospitalInformation() {
        // 오류발생
        if (super.getXmlError() == true) {
            super.showToastText(super.getXmlErrorMessage());
            return;
        }
        ResultSetHelper rsHelper = null;
        try {
            rsHelper = new ResultSetHelper(xml, false);
            if (rsHelper.getReturnCode() < 0) {
                super.showToastText(super.getXmlErrorMessage());
            } else if (rsHelper.getRecordCount() == 0) {
                super.showToastText("");
            } else {
                // 병원정보 : 병원에서 읽은 정보로 대체
                SharedPreferences prefs = getSharedPreferences("emrdroid", MODE_PRIVATE);
                SharedPreferences.Editor ed = prefs.edit();
                //ed.clear(); 이전정보를 다시 사용하기 위하여 정보를 저장함.
                try {
                    ed.putString("maskYn", "");
                    ed.putString("maskYn", rsHelper.getString(0, "mask_yn"));
                } catch (JSONException e) {
                }
                try {
                    ed.putString("emrCompany", "metrosoft");
                    ed.putString("emrCompany", rsHelper.getString(0, "emr_company"));
                } catch (JSONException e) {
                }
                // 2024.04.23 WOOIL - 동의서 리스트를 기본 펼치지 않을지 여부
                try {
                    ed.putString("collapseYn", "");
                    ed.putString("collapseYn", rsHelper.getString(0, "collapse_yn"));
                } catch (JSONException e) {
                }
                try {
                    ed.putString("inPatientListDoctDeptnm", "");
                    ed.putString("inPatientListDoctDeptnm", rsHelper.getString(0, "in_patient_list_doct_deptnm"));
                } catch (JSONException e) {
                }
                try {
                    ed.putString("inPatientListDoctPopupButtonHideYn", "");
                    ed.putString("inPatientListDoctPopupButtonHideYn", rsHelper.getString(0, "in_patient_list_doct_popup_button_hide_yn"));
                } catch (JSONException e) {
                }
                try {
                    ed.putString("tprEditButtonHideYn", "");
                    ed.putString("tprEditButtonHideYn", rsHelper.getString(0, "tpr_edit_button_hide_yn"));
                } catch (JSONException e) {
                }
                try {
                    ed.putString("ccfImageFormat", "");
                    ed.putString("ccfImageFormat", rsHelper.getString(0, "ccf_image_format"));
                } catch (JSONException e) {
                }
                try {
                    ed.putString("patientSafeCheckYn", "");
                    ed.putString("patientSafeCheckYn", rsHelper.getString(0, "patient_safe_check_yn"));
                } catch (JSONException e) {
                }
                try {
                    ed.putString("certificateHideYn", "");
                    ed.putString("certificateHideYn", rsHelper.getString(0, "certificate_hide_yn"));
                } catch (JSONException e) {
                }
                try {
                    ed.putString("barcodeScannerYn", "");
                    ed.putString("barcodeScannerYn", rsHelper.getString(0, "barcode_scanner_yn"));
                } catch (JSONException e) {
                }
                try {
                    ed.putString("nrChartAiYn", "");
                    ed.putString("nrChartAiYn", rsHelper.getString(0, "nr_chart_ai_yn"));
                } catch (JSONException e) {
                }
                // 임시저장동의서 리스트 조회시 동의서 별로 기본으로 접힐지 여부
                try {
                    ed.putString("presavedConsentFormListCollapseYn", "");
                    ed.putString("presavedConsentFormListCollapseYn", rsHelper.getString(0, "presaved_consent_form_list_collapse_yn"));
                } catch (JSONException e) {
                }
                ed.commit();
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
        // 재원환자리스트 호출
        Intent i = new Intent(Login.this, InPatientList.class);
        startActivity(i);
        finish();
    }

    private String getWifiMacAddress() {
        return Device.getWifiMacAddress(this);
    }

    private void loadHospitalList(final LinearLayout layout) {
        mDialog = ProgressDialog.show(this, "", getString(R.string.process_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String packageName = getPackageName();
                String wifiMacAddress = getWifiMacAddress();
                SharedPreferences pref = getSharedPreferences("licensekeyno", MODE_PRIVATE);
                String licenseKeyNo = pref.getString("licensekeyno", "");

                String url = "BasecampServlet" +
                             "?mode=hospitallist" +
                             "&wifimacaddress=" + wifiMacAddress +
                             "&licensekeyno=" + licenseKeyNo;


                xml = getXml(url, true);
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            setHospitalList(layout);
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
        ;

    }

    private void setHospitalList(LinearLayout layout) {
        ResultSetHelper record;
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                super.showSimpleDialog(super.getXmlErrorMessage());
                return;
            }
            record = new ResultSetHelper(xml, false);
            if (record.getReturnCode() <= 0) {
                // 오류
                Toast.makeText(this, record.getReturnDesc(), Toast.LENGTH_LONG).show();
                //super.showSimpleDialog(record.getReturnDesc());
                // 오류가 발생하여도 동작하도록 처리.
                // 이전에 로그인 했던 자료로 처리함.
                SharedPreferences prefs = getSharedPreferences("emrdroid", MODE_PRIVATE);
                hospitalIdList.add(prefs.getString("hospitalId", ""));
                hospitalNameList.add(prefs.getString("hospitalName", ""));
                maskYnList.add(prefs.getString("maskYn", ""));
                servletUseYnList.add(prefs.getString("servletUseYn", ""));
                servletIpList.add(prefs.getString("servletIp", ""));
                emrCompanyList.add(prefs.getString("emrCompany", ""));
            } else {
                int cnt = record.getRecordCount();
                for (int i = 0; i < cnt; i++) {
                    hospitalIdList.add(record.getString(i, "hospital_id"));
                    hospitalNameList.add(record.getString(i, "hospital_name"));
                    maskYnList.add(record.getString(i, "mask_yn"));
                    servletUseYnList.add(record.getString(i, "servlet_use_yn"));
                    servletIpList.add(record.getString(i, "servlet_ip"));
                    emrCompanyList.add(record.getString(i, "emr_company"));
                }
            }
            Spinner spin = (Spinner) layout.findViewById(R.id.hospitalListSpinner);
            spin.setBackgroundColor(layout.getDrawingCacheBackgroundColor());
            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, hospitalNameList);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spin.setAdapter(adapter);
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
            super.showSimpleDialog(ex.getMessage());
        }

    }

    // 앱의 실제 versionName을 가져온다.
    private String getAppVersionName() {
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);

            if (packageInfo.versionName == null || packageInfo.versionName.length() == 0) {
                return "0.0.0";
            }

            return packageInfo.versionName;

        } catch (Exception e) {
            return "0.0.0";
        }
    }

    // APK 다운로드 URL 호출
    private void openApkDownloadUrl() {
        //일단 막는다.
        //try {
        //    Intent intent = new Intent(Intent.ACTION_VIEW);
        //    String apkUrl = "http://www.metrosoft.co.kr/MEMR/SETUP/MEMR.apk";
        //    Uri uri = Uri.parse(apkUrl);
        //    intent.setData(uri);
        //    startActivity(intent);
        //} catch (Exception e) {
        //    showSimpleDialog(e.getMessage());
        //}
    }
}