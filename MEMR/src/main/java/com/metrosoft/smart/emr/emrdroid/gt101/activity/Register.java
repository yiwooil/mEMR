/***
 * 기기를 등록하는 화면
 * 단말기의 wifi 맥주소로  기계를 identify한다.
 * 라이센스키를 입력하여 기기를 등록한다.
 */
package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Device;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

public class Register extends MyActivity {
    private SharedPreferences mPrefs = null;
    private Handler mHandler = new Handler();
    private ProgressDialog mDialog = null;
    private String wifiMacAddress;
    private String licenseKeyNo;
    private String xml;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        ImageView imageLogo = (ImageView) findViewById(R.id.logo_image);
        imageLogo.setScaleType(ScaleType.FIT_XY);

        // 다이얼로그
        final LinearLayout layout = (LinearLayout) View.inflate(Register.this, R.layout.register_dialog, null);
        /*
        AlertDialog.Builder dialog = new AlertDialog.Builder(Register.this);
        dialog.setTitle(R.string.register_dialog_title);
        dialog.setView(layout);
        dialog.setCancelable(false);
       	dialog.show();
       	*/

        final Dialog dlg = new Dialog(Register.this);
        dlg.requestWindowFeature(Window.FEATURE_LEFT_ICON);
        dlg.setContentView(layout);
        dlg.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dlg.getWindow().setBackgroundDrawableResource(R.color.temr_titlebackground);
        dlg.setFeatureDrawableResource(Window.FEATURE_LEFT_ICON, R.drawable.login_icon);
        dlg.setTitle(R.string.register_dialog_title);
        dlg.setCancelable(false);
        dlg.show();

        final Button okButton = (Button) layout.findViewById(R.id.okButton);
        okButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                String packageName = getPackageName();
                wifiMacAddress = Device.getWifiMacAddress(Register.this);
                licenseKeyNo = ((EditText) layout.findViewById(R.id.licenseKeyNo)).getText().toString().trim();
                setRegister();
            }
        });
        final Button cancelButton = (Button) layout.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void setRegister() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.process_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String url = "RegisterServlet?mode=register&wifimacaddress=" + wifiMacAddress + "&licensekeyno=" + licenseKeyNo;
                xml = getXml(url, true);
                mHandler.post(new Runnable() {
                    public void run() {
                        mDialog.dismiss();
                        afterSetRegister();
                    }
                });
            }
        }).start();
        ;
    }

    private void afterSetRegister() {
        // 오류발생
        if (super.getXmlError() == true) {
            super.showToastText(super.getXmlErrorMessage());
            return;
        }
        if (xml.equals("yes")) {
            // 라이센스 키를 저장해놓는다. 이후 이 번호를 사용하자.
            SharedPreferences pref = getSharedPreferences("licensekeyno", MODE_PRIVATE);
            SharedPreferences.Editor editor = pref.edit();
            editor.putString("licensekeyno", licenseKeyNo);
            editor.commit();
            // 로그인 화면을 호출한다.
            Intent i;
    		/*
    		String packageName = getPackageName();
    		if(packageName.equals(EmrSettingsUtil.PACKAGE_TEMR)){
		    	i = new Intent(Register.this, LoginTemr.class);
    		}else{
		    	i = new Intent(Register.this, Login.class);
    		}
    		*/
            i = new Intent(Register.this, Login.class);
            startActivity(i);
            // 이 화면 종료
            finish();
        } else if (xml.equals("no")) {
            ;
        } else {
            showSimpleDialog(xml);
        }
    }

//	public boolean isRegister(Context context,String wifiMacAddress) {
//		boolean ret=false;
//    	String url = "RegisterServlet?mode=check&wifimacaddress=" + wifiMacAddress;
//    	try {
//	    	String xml = getXml(url);
//	    	if (xml.equals("yes")) {
//	    		ret=true;
//	    	}
//	    	else if (xml.equals("no")) {
//	    		ret=false;
//	    	}
//	    	else {
//	    		Toast.makeText(context, xml, Toast.LENGTH_LONG).show();
//	    		ret=false;
//	    	}
//		}catch(Exception e) {
//			Toast.makeText(context, e.getLocalizedMessage(), Toast.LENGTH_SHORT);
//			ret=false;
//		}
//		return ret;
//	}
}
