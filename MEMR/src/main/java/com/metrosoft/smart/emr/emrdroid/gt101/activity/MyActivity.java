package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ServletHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Device;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;

// custom_title_bar.xml
// CTB는 custom_titme_bar의 이니셜문자임.
// 해더의 백그라운드는 style/CustomTitleTheme를 사용한다.

public class MyActivity extends Activity {
    public final static int BUTTON_TYPE_NONE = 0;
    public final static int BUTTON_TYPE_EDIT = 1;
    public final static int BUTTON_TYPE_OPTION = 2;
    public final static int BUTTON_TYPE_SIGN = 3;
    public final static int BUTTON_TYPE_SAVE = 4;

    //private SharedPreferences mPrefs = null;
    private ServletHelper mServletHelper = new ServletHelper();

    protected ProgressDialog mDialog = null;
    protected Handler mHandler = new Handler();

    private TextView mTitleTextView = null;
    private Button mBackButton = null;
    private Button mQueryButton = null;
    private Button mButton1 = null;
    private Button mButton2 = null;
    private Button mButton3 = null;
    private Button mButton4 = null;
    private Button mButton5 = null;
    private Button mLinkButton1 = null;
    private Button mLinkButton2 = null;
    private Button mLinkButton3 = null;

    private boolean mIsOptionMenu = true; // 옵션메뉴사용가능여부

    private boolean mGetXmlError = false; // getXml 오류여부
    private String mGetXmlErrorMessage = ""; // getXml 오류메세지

    protected boolean mCallConfigSetting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void onCreate(Bundle savedInstanceState, View view, String backButtonText) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(view);

        afterOnCreate(backButtonText);
    }

    protected void onCreate(Bundle savedInstanceState, int layoutResID, String backButtonText) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(layoutResID);

        afterOnCreate(backButtonText);
    }

    private void afterOnCreate(String backButtonText) {
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title_bar);

        /*
        RelativeLayout topBgLayout = (RelativeLayout)findViewById(R.id.top_bg_layout);
        topBgLayout.setVisibility(View.GONE);
        LinearLayout topBgDivbarLayout = (LinearLayout)findViewById(R.id.top_bg_divbar_layout);
        topBgDivbarLayout.setVisibility(View.GONE);
        */

        mCallConfigSetting = false;
        initCTB(backButtonText);
    }

    protected void setMyTitle(CharSequence title) {
        // 타이틀
        super.setTitle(title);
        mTitleTextView.setText(getTitle());
    }

    // 화면 초기화
    private void initCTB(String backButtonText) {
        // 타이틀
        mTitleTextView = (TextView) findViewById(R.id.titleCTB);
        mTitleTextView.setText(getTitle());

        // 뒤로가기버튼 기본 동작
        mBackButton = (Button) findViewById(R.id.backButtonCTB);
        mBackButton.setText(backButtonText);
        mBackButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onClickBackButton(v);
            }
        });
        // 2019.10.29 WOOIL - 뒤로가기버튼을 길게 누르면 환경설정화면으로 들어간다.
        mBackButton.setOnLongClickListener(new Button.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(mCallConfigSetting) callConfigSetting();
                return true;
            }
        });
        // 조회버튼 기본 동작
        mQueryButton = (Button) findViewById(R.id.queryButtonCTB);
        mQueryButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onClickQueryButton(v);
            }
        });
        // 버튼 기본 동작 1 -- 기본 안보임.
        mButton1 = (Button) findViewById(R.id.button1CTB);
        mButton1.setVisibility(View.GONE);
        mButton1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onClickButton1(v);
            }
        });
        // 버튼 기본 동작 2 -- 기본 안보임.
        mButton2 = (Button) findViewById(R.id.button2CTB);
        mButton2.setVisibility(View.GONE);
        mButton2.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onClickButton2(v);
            }
        });
        // 버튼 기본 동작 3 -- 기본 안보임.
        mButton3 = (Button) findViewById(R.id.button3CTB);
        mButton3.setVisibility(View.GONE);
        mButton3.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onClickButton3(v);
            }
        });
        // 버튼 기본 동작 4 -- 기본 안보임.
        mButton4 = (Button) findViewById(R.id.button4CTB);
        mButton4.setVisibility(View.GONE);
        mButton4.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onClickButton4(v);
            }
        });
        // 버튼 기본 동작 5 -- 기본 안보임.
        mButton5 = (Button) findViewById(R.id.button5CTB);
        mButton5.setVisibility(View.GONE);
        mButton5.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onClickButton5(v);
            }
        });
        // 연결 버튼 기본 동작 1 -- 기본 안보임.
        mLinkButton1 = (Button) findViewById(R.id.linkButton1CTB);
        mLinkButton1.setVisibility(View.GONE);
        mLinkButton1.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onClickLinkButton1(v);
            }
        });
        // 연결 버튼 기본 동작 2 -- 기본 안보임.
        mLinkButton2 = (Button) findViewById(R.id.linkButton2CTB);
        mLinkButton2.setVisibility(View.GONE);
        mLinkButton2.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onClickLinkButton2(v);
            }
        });
        // 연결 버튼 기본 동작 3 -- 기본 안보임.
        mLinkButton3 = (Button) findViewById(R.id.linkButton3CTB);
        mLinkButton3.setVisibility(View.GONE);
        mLinkButton3.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                onClickLinkButton3(v);
            }
        });
    }

    private void callConfigSetting() {
        startActivity(new Intent(this, ConfigSetting.class));
    }

    // ---------------------------------------------
    // 재정의가 가능하도록 함수를 정의한다.
    // ---------------------------------------------
    // 뒤로(종료)버튼시 호출됨. finish가 기본동작임.
    public void onClickBackButton(View v) {
        finish();
    }

    // 조회버튼 클릭시 호출됨.
    public void onClickQueryButton(View v) {
    }

    // 버튼1 클릭시 호출됨.
    public void onClickButton1(View v) {
    }

    // 버튼2 클릭시 호출됨.
    public void onClickButton2(View v) {
    }

    // 버튼3 클릭시 호출됨.
    public void onClickButton3(View v) {
    }

    // 버튼4 클릭시 호출됨.
    public void onClickButton4(View v) {
    }

    // 버튼5 클릭시 호출됨.
    public void onClickButton5(View v) {
    }

    // 링크버튼1 클릭시 호출됨.
    public void onClickLinkButton1(View v) {
    }

    // 링크버튼2 클릭시 호출됨.
    public void onClickLinkButton2(View v) {
    }

    // 링크버튼2 클릭시 호출됨.
    public void onClickLinkButton3(View v) {
    }

    // 조회버튼
    protected void setQueryButton(boolean isVisible) {
        mQueryButton.setVisibility(isVisible == true ? View.VISIBLE : View.GONE);
    }

    // 버튼 보이기여부및 TEXT, IMAGE 셋팅
    private void setButton(Button button, boolean isVisible, String buttonText, int buttonType) {
        button.setVisibility(isVisible == true ? View.VISIBLE : View.GONE);
        button.setText(buttonText);
        if (buttonType == BUTTON_TYPE_EDIT) {
            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.edit_button_icon, 0, 0, 0);
            button.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.button_drawable_padding));
        } else if (buttonType == BUTTON_TYPE_OPTION) {
            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.option_button_icon, 0, 0, 0);
            button.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.button_drawable_padding));
        } else if (buttonType == BUTTON_TYPE_SIGN) {
            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.sign_button_icon, 0, 0, 0);
            button.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.button_drawable_padding));
        } else if (buttonType == BUTTON_TYPE_SAVE) {
            button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.save_button_icon, 0, 0, 0);
            button.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.button_drawable_padding));
        }
    }

    // 버튼1의 보이기 여부 및 TEXT 셋팅
    protected void setButton1(boolean isVisible, String buttonText, int buttonType) {
        setButton(mButton1, isVisible, buttonText, buttonType);
    }

    // 버튼2의 보이기 여부 및 TEXT 셋팅
    protected void setButton2(boolean isVisible, String buttonText, int buttonType) {
        setButton(mButton2, isVisible, buttonText, buttonType);
    }

    // 버튼3의 보이기 여부 및 TEXT 셋팅
    protected void setButton3(boolean isVisible, String buttonText, int buttonType) {
        setButton(mButton3, isVisible, buttonText, buttonType);
    }

    // 버튼4의 보이기 여부 및 TEXT 셋팅
    protected void setButton4(boolean isVisible, String buttonText, int buttonType) {
        setButton(mButton4, isVisible, buttonText, buttonType);
    }

    // 버튼5의 보이기 여부 및 TEXT 셋팅
    protected void setButton5(boolean isVisible, String buttonText, int buttonType) {
        setButton(mButton5, isVisible, buttonText, buttonType);
    }

    // 연결 버튼1의 보이기 여부 및 TEXT 셋팅
    protected void setLinkButton1(boolean isVisible, String buttonText) {
        mLinkButton1.setVisibility(isVisible == true ? View.VISIBLE : View.GONE);
        mLinkButton1.setText(buttonText);
    }

    // 연결 버튼2의 보이기 여부 및 TEXT 셋팅
    protected void setLinkButton2(boolean isVisible, String buttonText) {
        mLinkButton2.setVisibility(isVisible == true ? View.VISIBLE : View.GONE);
        mLinkButton2.setText(buttonText);
    }

    // 연결 버튼3의 보이기 여부 및 TEXT 셋팅
    protected void setLinkButton3(boolean isVisible, String buttonText) {
        mLinkButton3.setVisibility(isVisible == true ? View.VISIBLE : View.GONE);
        mLinkButton3.setText(buttonText);
    }

    // 공지사항과 환경설정화면에서는 메뉴를 사용하지 못하게 하기위함.
    protected void setQuickMenuOff() {
        mIsOptionMenu = false;
    }

    protected boolean getXmlError() {
        return mGetXmlError;
    }

    protected String getXmlErrorMessage() {
        return mGetXmlErrorMessage;
    }

    private void setXmlError(boolean error, String errorMessage) {
        mGetXmlError = error;
        mGetXmlErrorMessage = errorMessage;
    }

    // 서버에서 자료를 읽어오는 함수
    protected String getXml(String url) {
        return getXml(url,null, 30,false);
    }

    protected String getXml(String url, int readTimeOut) {
        return getXml(url, null, readTimeOut, false);
    }

    protected String getXml(String url, boolean isBC) {
        return getXml(url, null, 30, isBC);
    }

    protected String getXml(String url, HashMap<String, String> param) {
        return getXml(url, param, 30, false);
    }

    protected String getXml(String url, HashMap<String, String> param, boolean isBC) {
        return getXml(url, param, 30, isBC);
    }

    protected String getXml(String url, HashMap<String, String> param, int readTmeOut, boolean isBC) {
        setXmlError(false, "");
        if (isOnNetwork() == false) {
            setXmlError(true, getString(R.string.network_connection_close_message));
            return null;
        }
        try {
            String servletUseYn = "";
            String servletIp = "";
            if (isBC == true) {
                // Basecamp로 연결시는 기본으로 사용한다.
                servletIp = "";
            } else {
                // 병원자료를 읽을 때 WAS가 Basecamp와 다른 서버에 접속해야하는지 정의
                servletUseYn = EmrSettingsUtil.getServletUseYn(getBaseContext());
                Log.d("EmrDroid", "servletUseYn = " + servletUseYn);
                servletIp = EmrSettingsUtil.getServletIp(getBaseContext());
                Log.d("EmrDroid", "servletIp = " + servletIp);
                if (servletUseYn.equalsIgnoreCase("y") == false) servletIp = "";
            }
            Log.d("EmrDroid", "servletIp = " + servletIp);
            mServletHelper.setServletIp(servletIp);
            return mServletHelper.getXml(url, param, readTmeOut);
        } catch (Exception e) {
            setXmlError(true, e.getLocalizedMessage());
            return null;
        }
    }

    protected String getFullUrl(String url) {
        String servletUseYn = "";
        String servletIp = "";
        // 병원자료를 읽을 때 WAS가 Basecamp와 다른 서버에 접속해야하는지 정의
        servletUseYn = EmrSettingsUtil.getServletUseYn(getBaseContext());
        Log.d("EmrDroid", "servletUseYn = " + servletUseYn);
        servletIp = EmrSettingsUtil.getServletIp(getBaseContext());
        Log.d("EmrDroid", "servletIp = " + servletIp);
        if (servletUseYn.equalsIgnoreCase("y") == false) servletIp = "";
        mServletHelper.setServletIp(servletIp);

        return mServletHelper.getFullUrl(url);
    }


    // 서버에서 자료(이미지)를 읽어오는 함수
    protected Bitmap getBitmap(String imageUrl) {
        setXmlError(false, "");
        if (isOnNetwork() == false) {
            setXmlError(true, getString(R.string.network_connection_close_message));
            return null;
        }
        try {
            return mServletHelper.getBitmap(imageUrl);
        } catch (Exception e) {
            setXmlError(true, e.getLocalizedMessage());
            return null;
        }
    }

    // 서버에 파일을 올리는 함수
    protected String uploadPngFile(String fileName, String uploadFileName, String addParam) {
        setXmlError(false, "");
        if (isOnNetwork() == false) {
            setXmlError(true, getString(R.string.network_connection_close_message));
            return null;
        }
        try {
            return mServletHelper.uploadPngFile(fileName, uploadFileName, addParam);
        } catch (Exception e) {
            setXmlError(true, e.getLocalizedMessage());
            return null;
        }
    }

    protected boolean downloadAndSave(String serverPath, String localPath) {
        setXmlError(false, "");
        if (isOnNetwork() == false) {
            setXmlError(true, getString(R.string.network_connection_close_message));
            return false;
        }
        try {
            return mServletHelper.downFileAndSave(serverPath, localPath); //  .uploadPngFile(fileName,uploadFileName,addParam);
        } catch (Exception e) {
            setXmlError(true, e.getLocalizedMessage());
            return false;
        }
    }

    // 네트워크 연결 확인
    private boolean isOnNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (ni != null) {
            if (ni.isConnected() == true) return true;
        }
        ni = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (ni != null) {
            if (ni.isConnected() == true) return true;
        }
        ni = cm.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET); // 유선 LAN
        if (ni != null) {
            if (ni.isConnected() == true) return true;
        }

        return false;
        /*
        boolean isWifiAvail = false;
        boolean isWifiConn = false;
        boolean isMobileAvail = false;
        boolean isMobileConn = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        isWifiAvail = ni.isAvailable();
        isWifiConn = ni.isConnected();
        NetworkInfo ni2 = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (ni2 != null) {
            isMobileAvail = ni2.isAvailable();
            isMobileConn = ni2.isConnected();
        }

        return (isWifiConn || isMobileConn);
        */
    }

    // 한글깨짐방지용
    protected String getHangul(String s) {
        String ret;
        try {
            //ret = java.net.URLEncoder.encode(new String(s.getBytes("utf-8")));
            ret = java.net.URLEncoder.encode(s, "UTF-8");
            // ret = java.net.URLEncoder.encode(new String(s.getBytes("euc-kr")));
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            ret = "";
        }
        return ret;
    }

    // 로그인 정보 - 병원id
    protected String getHospitalId() {
        return EmrSettingsUtil.getHospitalId(getBaseContext());
    }

    // 로그인 정보 - 사용자id
    protected String getUserId() {
        return EmrSettingsUtil.getUserId(getBaseContext());
    }

    // 로그인 정보 - 개발자인지
    protected boolean isTestDeveloper() {
        //String wifiMacAddress = new Device(this).getWifiMacAddress();
        String wifiMacAddress = Device.getWifiMacAddress(this);
        Log.d("EmrDroid", "wifiMacAddress=" + wifiMacAddress);
        if (wifiMacAddress.equals("3C:5A:37:C0:5F:44"))
            return true;
        if (wifiMacAddress.equals("3C:5A:37:C6:77:54"))
            return true;
        if (wifiMacAddress.equals("CC:F9:E8:A3:9B:A7"))
            return true; // 갤럭시 탭 10.1
        return false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 동의서목록은 TEMR은 기본이 아니다.
        // 개발자 디바이스에서는 동의서목록 메뉴가 보이게 처리
        if (getPackageName().equalsIgnoreCase(EmrSettingsUtil.PACKAGE_MEMR)) {//||EmrSettingsUtil.isDeveloper(this)){
            getMenuInflater().inflate(R.menu.quick2, menu);
        } else {
            getMenuInflater().inflate(R.menu.quick, menu);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mIsOptionMenu == false) {
            return false;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.notice_menu) {
            // 공지사항
            startActivity(new Intent(this, NoticeList.class));
            return true;
        } else if (itemId == R.id.set_config_menu) {
            // 환경설정
            startActivity(new Intent(this, ConfigSetting.class));
            return true;
        } else if (itemId == R.id.consent_list) {
            // 동의서목록
            startActivity(new Intent(this, ConsentFormList.class));
            return true;
        } else if (itemId == R.id.consent_list_query) {
            // 동의서열람
            startActivity(new Intent(this, SignedConsentFormList.class));
            return true;
        }
        return (super.onOptionsItemSelected(item));
    }

    // ------------------------
    // 공통으로 사용하는 함수정의
    // ------------------------

    protected String getFormattedDate(String v) {
        return Utils.getFormattedDate(v);
        //if (v == null) return "";
        //if (v.equals("")) return "";
        //if (v.length() < 8) return v;
        //if (v.length() > 8) return v;
        //return v.substring(0, 4) + "." + v.substring(4, 6) + "." + v.substring(6, 8);
    }

    protected String getFormattedTime(String v) {
        return Utils.getFormattedTime(v);
        //if (v == null) return "";
        //if (v.equals("")) return "";
        //if (v.length() == 4) {
        //    return v.substring(0, 2) + ":" + v.substring(2, 4);
        //} else if (v.length() == 6) {
        //    return v.substring(0, 2) + ":" + v.substring(2, 4) + ":" + v.substring(4, 6);
        //} else {
        //    return "";
        //}
    }

    protected Date addDate(Date date, long n) {
        return new Date(date.getTime() + (n * 1000 * 60 * 60 * 24));
    }

    protected void showSimpleDialog(int messageId) {
        showSimpleDialog(getString(messageId));
    }

    protected void showSimpleDialog(String message) {
        showSimpleDialog(message, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
    }

    protected void showSimpleDialog(String message, DialogInterface.OnClickListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton(R.string.ok, listener);
        AlertDialog alert = builder.create();
        alert.show();
    }

    protected void showToastText(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    protected void showToastText(String msg, boolean isShort) {
        if (isShort == true) {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    // 탭의 모양을 변경
    protected static View createTabIndicator(final Context context, final String text) {
        View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);
        return view;
    }

}
