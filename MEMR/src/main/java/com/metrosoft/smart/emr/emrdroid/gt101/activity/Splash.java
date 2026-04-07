/*
 * 접속하고 있는 단말기가 등록된 기기인지 점검한다.
 * 등록되지 않은 기기이면 등록창이 나타난다.
 * 등록된 기기이면 로그인창으로 이동한다.
 */
package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.DateUtil;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Device;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import org.json.JSONException;

public class Splash extends MyActivity {
    private ProgressDialog dialog;
    //private boolean isRegister;
    private Handler handler;

    private String packageName;
    private String wifiMacAddress;
    private String licenseKeyNo; // 2021.06.03 WOOIL - 라이센스 키를 사용하자.

    private String mChkXml;
    private String mXml;
    private String mLatestVersion, mGoogleUrl;
    private String mIsError;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* 2022.12.15 WOOIL - TEMR 고려하는 부분 제거
		// 실행 package에 따른 이미지 처리
		packageName = this.getPackageName();
		Log.d("EmrDroid", "packageName = " + packageName);
		if(packageName.equalsIgnoreCase("com.metrosoft.temr")){
	        setContentView(R.layout.temr_splash);			
		}else{
	        setContentView(R.layout.splash);			
		}
		*/
        setContentView(R.layout.splash);

        ImageView imageLogo = (ImageView) findViewById(R.id.logo_image);
        imageLogo.setScaleType(ScaleType.FIT_XY);

        handler = new Handler();
        dialog = ProgressDialog.show(this, "", getString(R.string.splash_message), true);
        new Thread(new Runnable() {
            public void run() {
                SharedPreferences pref = getSharedPreferences("licensekeyno", MODE_PRIVATE);
                licenseKeyNo = pref.getString("licensekeyno", "");
                if (licenseKeyNo == null) licenseKeyNo = "";
                wifiMacAddress = Device.getWifiMacAddress(Splash.this);
                if (wifiMacAddress == null)
                    wifiMacAddress = ""; // 보안이 강화되어 WIFI MAC ADDRESS를 못 가져오도록 변경되었음.
                Log.d("EmrDroid", "licenseKeyNo = " + licenseKeyNo + ", wifiMacAddress = " + wifiMacAddress);
                if (licenseKeyNo.equals("")) {
                    mChkXml = "no";
                } else {
                    String url = "RegisterServlet" +
                            "?mode=check" +
                            "&wifimacaddress=" + wifiMacAddress +
                            "&licensekeyno=" + licenseKeyNo; // 2021.06.03 WOOIL - 라이센스 키 사용

                    mChkXml = getXml(url, true);
                    if (mChkXml == null) mChkXml = "";
                }
                handler.post(new Runnable() {
                    public void run() {
                        afterCheck();
                        dialog.dismiss();
                    }
                });
            }
        }).start();
    }

    private void afterCheck() {
        // 2022.12.15 WOOIL - WIFI MAC Address를 체크하던 부분을 없앰.
        if (super.getXmlError() == true) {
            // 오류
            super.showSimpleDialog(super.getXmlErrorMessage(), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    finish();
                }
            });
        } else if ("no".equalsIgnoreCase(mChkXml)) {
            // 등록되지 않은 단말기임.
            callRegisterActivity();
        } else if ("end1".equalsIgnoreCase(mChkXml)) {
            // 유효기간이 만료되었음.
            super.showSimpleDialog("인증키의 유효기간이 만료되었습니다. 새로운 인증키를 등록하세요", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    callRegisterActivity();
                }
            });
        } else if (mChkXml.startsWith("end2")) {
            // 유효기간이 만료예정임.
            String endDate = mChkXml.substring(4);
            super.showSimpleDialog("인증키가 " + DateUtil.getFormattedDate(endDate) + " 까지 사용가능합니다.", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    mIsError = "";
                    //getApkVersion(packageName); // 버전 체크하는 부분을 막고
                    callLoginActivity();          // 바로 로그인 창을 띄운다.
                }
            });
        } else if ("yes".equalsIgnoreCase(mChkXml)) {
            // 인증 성공. 로그인 호출
            mIsError = "";
            //getApkVersion(packageName); // 버전 체크하는 부분을 막고
            callLoginActivity();          // 바로 로그인 창을 띄운다.
        } else {
            // 뭔가 오류가 있지만 로그인 호출. 우리서버에 접속이 안될수도 있으므로...
            mIsError = ".";
            //getApkVersion(packageName); // 버전 체크하는 부분을 막고
            callLoginActivity();          // 바로 로그인 창을 띄운다.
        }

    }

    private void callRegisterActivity() {
        Intent i = new Intent(this, Register.class);
        startActivity(i);
        finish();
    }

    private void callLoginActivity() {
		/*
		if(packageName.equalsIgnoreCase(EmrSettingsUtil.PACKAGE_TEMR)){
	    	Intent i = new Intent(this, LoginTemr.class);
	    	i.putExtra("is_license_check_error", mIsError);
	    	startActivity(i);
		}else{
	    	Intent i = new Intent(this, Login.class);
	    	i.putExtra("is_license_check_error", mIsError);
	    	startActivity(i);
		}
		*/
        Intent i = new Intent(this, Login.class);
        i.putExtra("is_license_check_error", mIsError);
        startActivity(i);
        finish();
    }

	/* 2022.12.15 WOOIL - 버전 체크를 막음
	private void getApkVersion(final String packageName) {
		mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
        	public void run() {
        		String pkgName = packageName;
        		if(packageName.equals(EmrSettingsUtil.PACKAGE_MEMR)){
        			pkgName="EmrDroid.GT101";
        		}
            	//String hospitalId=EmrSettingsUtil.getHospitalId(getBaseContext());
            	//String userId=EmrSettingsUtil.getUserId(getBaseContext());
            	String url = "ApkVersionServlet?apkname=" + pkgName;
        		mXml="";
            	mXml = getXml(url,true);
    			mHandler.post(new Runnable() {
    				public void run() {
    					afterGetApkVersion();
    					mDialog.dismiss();
    				}
    			});
        	}
        }).start();
	}
	*/

	/* 2022.12.15 WOOIL - 버전 체크를 막음
	private void afterGetApkVersion() {
		try {
			mLatestVersion="";
			mGoogleUrl="";
			ResultSetHelper rsHelper = new ResultSetHelper(mXml,false);
			if(rsHelper.getRecordCount()>0) {
				mLatestVersion = rsHelper.getString(0, "version_name");
				//mMetrosoftUrl = rsHelper.getString(0, "metrosoft_url");
				mGoogleUrl = rsHelper.getString(0, "google_url");
			}
			
			String versionName = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA).versionName;
			// 현재 버전
			String[] version = versionName.split("\\.");
			int ver1 = Integer.parseInt(version[0]);
			int ver2 = Integer.parseInt(version[1]);
			int ver3 = Integer.parseInt(version[2]);
			// 서버에 등록된 버전
			String[] latestVersion = mLatestVersion.split("\\.");
			int lver1 = Integer.parseInt(latestVersion[0]);
			int lver2 = Integer.parseInt(latestVersion[1]);
			int lver3 = Integer.parseInt(latestVersion[2]);
			// 서버에 등록된 버전이 더 큰경우만
			if(lver1>ver1){
				// 버전이 다르면
				dialogVersion(versionName);
			}else if(lver2>ver2){
				// 버전이 다르면
				dialogVersion(versionName);
			}else if(lver3>ver3){
				// 버전이 다르면
				dialogVersion(versionName);
			}else{
				// 같으면 시작
				callLoginActivity();
			}
//			if(!versionName.equalsIgnoreCase(mLatestVersion)){
//				// 버전이 다르면
//				dialogVersion(versionName);
//			}else{
//				// 같으면 시작
//				callLoginActivity();
//			}

		} catch (JSONException e) {
			Log.d("EmrDroid","afterGetApkVersion error 1: " + e.getMessage());
			callLoginActivity();
		} catch (NameNotFoundException e) {
			Log.d("EmrDroid","afterGetApkVersion error 2: " + e.getMessage());
			callLoginActivity();
		} catch (NumberFormatException e) {
			Log.d("EmrDroid","afterGetApkVersion error 3: " + e.getMessage());
			callLoginActivity();
		} catch (Exception e) {
			Log.d("EmrDroid","afterGetApkVersion error 4: " + e.getMessage());
			callLoginActivity();
		}
	}
	*/

	/* 2022.12.15 WOOIL - 버전 체크를 막음
	private void dialogVersion(String versionName){
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder
			.setMessage("버전이 변경되었습니다. 업데이트하시겠습니까?")
			.setCancelable(false)
			.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					callGooglePlay();
				}
			})
			.setNegativeButton("No", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub
					callLoginActivity();
				}
			});
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
			
	}
	*/
	
	/* 2022.12.15 WOOIL - 버전 체크를 막음
	private void callGooglePlay(){
		startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(mGoogleUrl)));
		finish();
	}
	*/

	/*
	private String getPackageName(){
		ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningTaskInfo> taskInfo = am.getRunningTasks(1);
		ComponentName topActivity = taskInfo.get(0).topActivity;
		String packageName = topActivity.getPackageName();
		return packageName;
	}
	*/
}
