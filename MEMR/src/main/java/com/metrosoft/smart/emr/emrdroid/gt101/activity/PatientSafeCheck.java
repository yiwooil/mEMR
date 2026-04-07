package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.Manifest; // 2025.12.08 WOOIL
import android.content.pm.PackageManager; // 2025.12.08 WOOIL
import android.os.Build; // 2025.12.08 WOOIL

import org.json.JSONException;
import java.util.HashMap;
import java.util.List;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import com.journeyapps.barcodescanner.CompoundBarcodeView; // 2025.12.08 WOOIL
import com.journeyapps.barcodescanner.BarcodeCallback; // 2025.12.08 WOOIL
import com.journeyapps.barcodescanner.BarcodeResult; // 2025.12.08 WOOIL

import com.google.zxing.ResultPoint; // 2025.12.08 WOOIL


public class PatientSafeCheck extends MyActivity implements OnClickListener {
    private String mXmlPid;
    private String mXmlSpcno;
    private String mXmlBldno;
    private String mXmlSave;

    private Vibrator mVibrator;

    private TextView mPidInfo;
    private EditText mPid;
    private TextView mPidDisp;
    private TextView mPnmDisp;
    private TextView mWardDisp;
    // 검체
    private TextView mSpcnoInfo;
    private EditText mSpcno;
    private ImageView mSpcnoOk;
    private ImageView mSpcnoErr;
    private LinearLayout mSpcnoButtons;
    private Button mPtntCallInSpc;
    private Button mBldnoCallInSpc;
    private Button mInfnoCallInSpc;
    private Button mSpcnoContinue;
    // 혈액
    private TextView mBldnoInfo;
    private EditText mBldno;
    private ImageView mBldnoOk;
    private ImageView mBldnoErr;
    private LinearLayout mBldnoButtons;
    private Button mPtntCallInBld;
    private Button mSpcnoCallInBld;
    private Button mInfnoCallInBld;
    private Button mBldnoContinue;
    // 수액
    private TextView mInfnoInfo;
    private EditText mInfno;
    private ImageView mInfnoOk;
    private ImageView mInfnoErr;
    private LinearLayout mInfnoButtons;
    private Button mPtntCallInInf;
    private Button mSpcnoCallInInf;
    private Button mBldnoCallInInf;
    private Button mInfnoContinue;
    // 바코드 스캐너 뷰
    private CompoundBarcodeView mBarcodeScanner; // 2025.12.08 WOOIL
    // 현재 스캔 결과를 넣어 줄 타겟 EditText
    private EditText mCurrentScanTarget; // 2025.12.08 WOOIL
    // 카메라 퍼미션용
    private static final int REQ_CAMERA_PERMISSION = 1001; // 2025.12.08 WOOIL

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.patient_safe_check, "");
        super.setQueryButton(false);

        mXmlPid = "";
        mXmlSpcno = "";
        mXmlBldno = "";
        mXmlSave = "";

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mPidInfo = (TextView) findViewById(R.id.pid_info);
        mPid = (EditText) findViewById(R.id.pid);
        mPidDisp = (TextView) findViewById(R.id.pid_disp);
        mPnmDisp = (TextView) findViewById(R.id.pnm_disp);
        mWardDisp = (TextView) findViewById(R.id.ward_disp);
        // 검체
        mSpcnoInfo = (TextView) findViewById(R.id.spcno_info);
        mSpcno = (EditText) findViewById(R.id.spcno);
        mSpcnoOk = (ImageView) findViewById(R.id.spcno_ok);
        mSpcnoErr = (ImageView) findViewById(R.id.spcno_err);
        mSpcnoButtons = (LinearLayout) findViewById(R.id.spcno_buttons);
        mPtntCallInSpc = (Button) findViewById(R.id.ptnt_call_in_spc);
        mBldnoCallInSpc = (Button) findViewById(R.id.bldno_call_in_spc);
        mInfnoCallInSpc = (Button) findViewById(R.id.infno_call_in_spc);
        mSpcnoContinue = (Button) findViewById(R.id.spcno_continue);
        // 혈액
        mBldnoInfo = (TextView) findViewById(R.id.bldno_info);
        mBldno = (EditText) findViewById(R.id.bldno);
        mBldnoOk = (ImageView) findViewById(R.id.bldno_ok);
        mBldnoErr = (ImageView) findViewById(R.id.bldno_err);
        mBldnoButtons = (LinearLayout) findViewById(R.id.bldno_buttons);
        mPtntCallInBld = (Button) findViewById(R.id.ptnt_call_in_bld);
        mSpcnoCallInBld = (Button) findViewById(R.id.spcno_call_in_bld);
        mInfnoCallInBld = (Button) findViewById(R.id.infno_call_in_bld);
        mBldnoContinue = (Button) findViewById(R.id.bldno_continue);
        // 수액
        mInfnoInfo = (TextView) findViewById(R.id.infno_info);
        mInfno = (EditText) findViewById(R.id.infno);
        mInfnoOk = (ImageView) findViewById(R.id.infno_ok);
        mInfnoErr = (ImageView) findViewById(R.id.infno_err);
        mInfnoButtons = (LinearLayout) findViewById(R.id.infno_buttons);
        mPtntCallInInf = (Button) findViewById(R.id.ptnt_call_in_inf);
        mSpcnoCallInInf = (Button) findViewById(R.id.spcno_call_in_inf);
        mBldnoCallInInf = (Button) findViewById(R.id.bldno_call_in_inf);
        mInfnoContinue = (Button) findViewById(R.id.infno_continue);

        setOnPid();

        setPidTextWatcher();
        setSpcnoTextWatcher();
        setBldnoTextWatcher();
        setInfnoTextWatcher();

        // 검체
        mPtntCallInSpc.setOnClickListener(this);
        mBldnoCallInSpc.setOnClickListener(this);
        mInfnoCallInSpc.setOnClickListener(this);
        mSpcnoContinue.setOnClickListener(this);
        // 혈액
        mPtntCallInBld.setOnClickListener(this);
        mSpcnoCallInBld.setOnClickListener(this);
        mInfnoCallInBld.setOnClickListener(this);
        mBldnoContinue.setOnClickListener(this);
        // 수액
        mPtntCallInInf.setOnClickListener(this);
        mSpcnoCallInInf.setOnClickListener(this);
        mBldnoCallInInf.setOnClickListener(this);
        mInfnoContinue.setOnClickListener(this);

        // 2025.12.08 WOOIL - 바코드 스캐너 뷰 초기화
        mBarcodeScanner = (CompoundBarcodeView) findViewById(R.id.barcode_scanner);
        if (mBarcodeScanner != null) {
            mBarcodeScanner.decodeContinuous(mBarcodeCallback);
            mBarcodeScanner.setVisibility(View.GONE);
        }

        // 2025.12.08 WOOIL - 공통 포커스 리스너
        View.OnFocusChangeListener scanFocusListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && v instanceof EditText) {
                    mCurrentScanTarget = (EditText) v;
                    startCameraWithPermissionCheck();
                }
            }
        };

        // 2025.12.08 WOOIL - 세 개의 EditText에 같은 리스너 등록
        mPid.setOnFocusChangeListener(scanFocusListener);
        mSpcno.setOnFocusChangeListener(scanFocusListener);
        mBldno.setOnFocusChangeListener(scanFocusListener);
        mInfno.setOnFocusChangeListener(scanFocusListener);

        mPid.clearFocus();
        mPid.requestFocus();

    }

    // 2025.12.08 WOOIL
    @Override
    protected void onResume() {
        super.onResume();
        if (mBarcodeScanner != null &&
                mBarcodeScanner.getVisibility() == View.VISIBLE) {
            mBarcodeScanner.resume();
        }
    }

    // 2025.12.08 WOOIL
    @Override
    protected void onPause() {
        super.onPause();
        if (mBarcodeScanner != null) {
            mBarcodeScanner.pause();
        }
    }

    private void afterPidScan() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                HashMap<String, String> param = new HashMap<String, String>();
                param.clear();
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String pid = mPid.getText().toString();
                mXmlPid = "";
                param.put("hospitalid", hospitalId);
                param.put("userid", userId);
                param.put("mode", "6");
                param.put("pid", pid);
                mXmlPid = getXml("InPatientListServlet", param);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try {
                            afterGetPatient();
                            mDialog.dismiss();
                        } catch (Exception ex) {
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetPatient() {
        ResultSetHelper rs;
        try {
            rs = new ResultSetHelper(mXmlPid, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
                clearScanField();
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
                clearScanField();
            } else {
                final String pid = rs.getString(0, "pid");
                final String pnm = rs.getString(0, "pnm");
                final String psex = rs.getString(0, "psex");
                final String age = rs.getString(0, "age");
                final String ward = rs.getString(0, "ward");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mPidDisp.setText(pid);
                        mPnmDisp.setText(pnm + " " + psex + "/" + age);
                        mWardDisp.setText(ward);
                    }
                });

                clearScanField();
                setOnSpcno();
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void afterSpcnoScan() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                HashMap<String, String> param = new HashMap<String, String>();
                param.clear();
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String spcno = mSpcno.getText().toString();
                mXmlSpcno = "";
                param.put("hospitalid", hospitalId);
                param.put("userid", userId);
                param.put("mode", "8");
                param.put("spcno", spcno);
                mXmlSpcno = getXml("ChartServlet", param);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try {
                            afterGetSpcno();
                            mDialog.dismiss();
                        } catch (Exception ex) {
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetSpcno() {
        ResultSetHelper rs;
        try {
            rs = new ResultSetHelper(mXmlSpcno, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
                String spcno = mSpcno.getText().toString();
                saveCheckResult("검체", spcno, "ERR");
                clearScanField();
            } else if (rs.getReturnCode() == 0) {
                String spcno = mSpcno.getText().toString();
                saveCheckResult("검체", spcno, "ERR");
                setOnSpcnoErr();
            } else {
                String pid = mPidDisp.getText().toString();
                String spcno_pid = rs.getString(0, "pid");
                if (spcno_pid.equals(pid)) {
                    String spcno = mSpcno.getText().toString();
                    saveCheckResult("검체", spcno, "OK");
                    setOnSpcnoOk();
                } else {
                    String spcno = mSpcno.getText().toString();
                    saveCheckResult("검체", spcno, "ERR");
                    setOnSpcnoErr();
                    showSimpleDialog(spcno_pid);
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void afterBldnoScan() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                HashMap<String, String> param = new HashMap<String, String>();
                param.clear();
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String bldno = mBldno.getText().toString();
                mXmlBldno = "";
                param.put("hospitalid", hospitalId);
                param.put("userid", userId);
                param.put("mode", "9");
                param.put("bldno", bldno);
                mXmlBldno = getXml("ChartServlet", param);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try {
                            afterGetBldno();
                            mDialog.dismiss();
                        } catch (Exception ex) {
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetBldno() {
        ResultSetHelper rs;
        try {
            rs = new ResultSetHelper(mXmlBldno, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
                String bldno = mBldno.getText().toString();
                saveCheckResult("혈액", bldno, "ERR");
                clearScanField();
            } else if (rs.getReturnCode() == 0) {
                String bldno = mBldno.getText().toString();
                saveCheckResult("혈액", bldno, "ERR");
                setOnBldnoErr();
            } else {
                String pid = mPidDisp.getText().toString();
                String bldno_pid = rs.getString(0, "pid");
                if (bldno_pid.equals(pid)) {
                    String bldno = mBldno.getText().toString();
                    saveCheckResult("혈액", bldno, "OK");
                    setOnBldnoOk();
                } else {
                    String bldno = mBldno.getText().toString();
                    saveCheckResult("혈액", bldno, "ERR");
                    setOnBldnoErr();
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void afterInfnoScan() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try {
                            afterGetInfno();
                            mDialog.dismiss();
                        } catch (Exception ex) {
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetInfno() {
        ResultSetHelper rs;
        try {
            String pid = mPidDisp.getText().toString();
            String infno_pid = mInfno.getText().toString();
            if (infno_pid.equals(pid)) {
                String infno = mInfno.getText().toString();
                saveCheckResult("수액", infno, "OK");
                setOnInfnoOk();
            } else {
                String infno = mInfno.getText().toString();
                saveCheckResult("수액", infno, "ERR");
                setOnInfnoErr();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setOnPid() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPidInfo.setVisibility(View.VISIBLE);
                mPid.setVisibility(View.VISIBLE);
                //
                mPidDisp.setVisibility(View.GONE);
                mPnmDisp.setVisibility(View.GONE);
                mWardDisp.setVisibility(View.GONE);
                // 검체
                mSpcnoInfo.setVisibility(View.GONE);
                mSpcno.setVisibility(View.GONE);
                mSpcnoOk.setVisibility(View.GONE);
                mSpcnoErr.setVisibility(View.GONE);
                mSpcnoButtons.setVisibility(View.GONE);
                // 혈액
                mBldnoInfo.setVisibility(View.GONE);
                mBldno.setVisibility(View.GONE);
                mBldnoOk.setVisibility(View.GONE);
                mBldnoErr.setVisibility(View.GONE);
                mBldnoButtons.setVisibility(View.GONE);
                // 수액
                mInfnoInfo.setVisibility(View.GONE);
                mInfno.setVisibility(View.GONE);
                mInfnoOk.setVisibility(View.GONE);
                mInfnoErr.setVisibility(View.GONE);
                mInfnoButtons.setVisibility(View.GONE);
                //
                mPid.requestFocus();
            }
        });
    }

    private void setOnSpcno() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPidInfo.setVisibility(View.GONE);
                mPid.setVisibility(View.GONE);
                //
                mPidDisp.setVisibility(View.VISIBLE);
                mPnmDisp.setVisibility(View.VISIBLE);
                mWardDisp.setVisibility(View.VISIBLE);
                // 검체
                mSpcnoInfo.setVisibility(View.VISIBLE);
                mSpcno.setVisibility(View.VISIBLE);
                mSpcnoOk.setVisibility(View.GONE);
                mSpcnoErr.setVisibility(View.GONE);
                mSpcnoButtons.setVisibility(View.VISIBLE);
                mBldnoCallInSpc.setVisibility(View.VISIBLE);
                mInfnoCallInSpc.setVisibility(View.VISIBLE);
                mSpcnoContinue.setVisibility(View.GONE);
                // 혈액
                mBldnoInfo.setVisibility(View.GONE);
                mBldno.setVisibility(View.GONE);
                mBldnoOk.setVisibility(View.GONE);
                mBldnoErr.setVisibility(View.GONE);
                mBldnoButtons.setVisibility(View.GONE);
                // 수액
                mInfnoInfo.setVisibility(View.GONE);
                mInfno.setVisibility(View.GONE);
                mInfnoOk.setVisibility(View.GONE);
                mInfnoErr.setVisibility(View.GONE);
                mInfnoButtons.setVisibility(View.GONE);
                //
                mSpcno.requestFocus();
            }
        });

    }

    private void setOnSpcnoOk() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPidInfo.setVisibility(View.GONE);
                mPid.setVisibility(View.GONE);
                //
                mPidDisp.setVisibility(View.VISIBLE);
                mPnmDisp.setVisibility(View.VISIBLE);
                mWardDisp.setVisibility(View.VISIBLE);
                // 검체
                mSpcnoInfo.setVisibility(View.VISIBLE);
                mSpcno.setVisibility(View.GONE);
                mSpcnoOk.setVisibility(View.VISIBLE);
                mSpcnoErr.setVisibility(View.GONE);
                mSpcnoButtons.setVisibility(View.VISIBLE);
                mBldnoCallInSpc.setVisibility(View.GONE);
                mInfnoCallInSpc.setVisibility(View.GONE);
                mSpcnoContinue.setVisibility(View.VISIBLE);
                // 혈액
                mBldnoInfo.setVisibility(View.GONE);
                mBldno.setVisibility(View.GONE);
                mBldnoOk.setVisibility(View.GONE);
                mBldnoErr.setVisibility(View.GONE);
                mBldnoButtons.setVisibility(View.GONE);
                // 수액
                mInfnoInfo.setVisibility(View.GONE);
                mInfno.setVisibility(View.GONE);
                mInfnoOk.setVisibility(View.GONE);
                mInfnoErr.setVisibility(View.GONE);
                mInfnoButtons.setVisibility(View.GONE);
                //
                mSpcnoOk.requestFocus();
            }
        });
    }

    private void setOnSpcnoErr() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPidInfo.setVisibility(View.GONE);
                mPid.setVisibility(View.GONE);
                //
                mPidDisp.setVisibility(View.VISIBLE);
                mPnmDisp.setVisibility(View.VISIBLE);
                mWardDisp.setVisibility(View.VISIBLE);
                // 검체
                mSpcnoInfo.setVisibility(View.VISIBLE);
                mSpcno.setVisibility(View.GONE);
                mSpcnoOk.setVisibility(View.GONE);
                mSpcnoErr.setVisibility(View.VISIBLE);
                mSpcnoButtons.setVisibility(View.VISIBLE);
                mBldnoCallInSpc.setVisibility(View.GONE);
                mInfnoCallInSpc.setVisibility(View.GONE);
                mSpcnoContinue.setVisibility(View.VISIBLE);
                // 혈액
                mBldnoInfo.setVisibility(View.GONE);
                mBldno.setVisibility(View.GONE);
                mBldnoOk.setVisibility(View.GONE);
                mBldnoErr.setVisibility(View.GONE);
                mBldnoButtons.setVisibility(View.GONE);
                // 수액
                mInfnoInfo.setVisibility(View.GONE);
                mInfno.setVisibility(View.GONE);
                mInfnoOk.setVisibility(View.GONE);
                mInfnoErr.setVisibility(View.GONE);
                mInfnoButtons.setVisibility(View.GONE);
                //
                mSpcnoErr.requestFocus();
                //
                mVibrator.vibrate(1000);// 1초간 진동
            }
        });
    }

    private void setOnBldno() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPidInfo.setVisibility(View.GONE);
                mPid.setVisibility(View.GONE);
                //
                mPidDisp.setVisibility(View.VISIBLE);
                mPnmDisp.setVisibility(View.VISIBLE);
                mWardDisp.setVisibility(View.VISIBLE);
                // 혈액
                mBldnoInfo.setVisibility(View.VISIBLE);
                mBldno.setVisibility(View.VISIBLE);
                mBldnoOk.setVisibility(View.GONE);
                mBldnoErr.setVisibility(View.GONE);
                mBldnoButtons.setVisibility(View.VISIBLE);
                mSpcnoCallInBld.setVisibility(View.VISIBLE);
                mBldnoContinue.setVisibility(View.GONE);
                // 검체
                mSpcnoInfo.setVisibility(View.GONE);
                mSpcno.setVisibility(View.GONE);
                mSpcnoOk.setVisibility(View.GONE);
                mSpcnoErr.setVisibility(View.GONE);
                mSpcnoButtons.setVisibility(View.GONE);
                // 수액
                mInfnoInfo.setVisibility(View.GONE);
                mInfno.setVisibility(View.GONE);
                mInfnoOk.setVisibility(View.GONE);
                mInfnoErr.setVisibility(View.GONE);
                mInfnoButtons.setVisibility(View.GONE);
                //
                mBldno.requestFocus();
            }
        });
    }

    private void setOnBldnoOk() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPidInfo.setVisibility(View.GONE);
                mPid.setVisibility(View.GONE);
                //
                mPidDisp.setVisibility(View.VISIBLE);
                mPnmDisp.setVisibility(View.VISIBLE);
                mWardDisp.setVisibility(View.VISIBLE);
                // 혈액
                mBldnoInfo.setVisibility(View.VISIBLE);
                mBldno.setVisibility(View.GONE);
                mBldnoOk.setVisibility(View.VISIBLE);
                mBldnoErr.setVisibility(View.GONE);
                mBldnoButtons.setVisibility(View.VISIBLE);
                mSpcnoCallInBld.setVisibility(View.GONE);
                mBldnoContinue.setVisibility(View.VISIBLE);
                // 검체
                mSpcnoInfo.setVisibility(View.GONE);
                mSpcno.setVisibility(View.GONE);
                mSpcnoOk.setVisibility(View.GONE);
                mSpcnoErr.setVisibility(View.GONE);
                mSpcnoButtons.setVisibility(View.GONE);
                // 수액
                mInfnoInfo.setVisibility(View.GONE);
                mInfno.setVisibility(View.GONE);
                mInfnoOk.setVisibility(View.GONE);
                mInfnoErr.setVisibility(View.GONE);
                mInfnoButtons.setVisibility(View.GONE);
                //
                mBldnoOk.requestFocus();
            }
        });
    }

    private void setOnBldnoErr() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPidInfo.setVisibility(View.GONE);
                mPid.setVisibility(View.GONE);
                //
                mPidDisp.setVisibility(View.VISIBLE);
                mPnmDisp.setVisibility(View.VISIBLE);
                mWardDisp.setVisibility(View.VISIBLE);
                // 혈액
                mBldnoInfo.setVisibility(View.VISIBLE);
                mBldno.setVisibility(View.GONE);
                mBldnoOk.setVisibility(View.GONE);
                mBldnoErr.setVisibility(View.VISIBLE);
                mBldnoButtons.setVisibility(View.VISIBLE);
                mSpcnoCallInBld.setVisibility(View.GONE);
                mBldnoContinue.setVisibility(View.VISIBLE);
                // 검체
                mSpcnoInfo.setVisibility(View.GONE);
                mSpcno.setVisibility(View.GONE);
                mSpcnoOk.setVisibility(View.GONE);
                mSpcnoErr.setVisibility(View.GONE);
                mSpcnoButtons.setVisibility(View.GONE);
                // 수액
                mInfnoInfo.setVisibility(View.GONE);
                mInfno.setVisibility(View.GONE);
                mInfnoOk.setVisibility(View.GONE);
                mInfnoErr.setVisibility(View.GONE);
                mInfnoButtons.setVisibility(View.GONE);
                //
                mBldnoErr.requestFocus();
                //
                mVibrator.vibrate(1000);// 1초간 진동
            }
        });
    }

    private void setOnInfno() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPidInfo.setVisibility(View.GONE);
                mPid.setVisibility(View.GONE);
                //
                mPidDisp.setVisibility(View.VISIBLE);
                mPnmDisp.setVisibility(View.VISIBLE);
                mWardDisp.setVisibility(View.VISIBLE);
                // 수액
                mInfnoInfo.setVisibility(View.VISIBLE);
                mInfno.setVisibility(View.VISIBLE);
                mInfnoOk.setVisibility(View.GONE);
                mInfnoErr.setVisibility(View.GONE);
                mInfnoButtons.setVisibility(View.VISIBLE);
                mSpcnoCallInInf.setVisibility(View.VISIBLE);
                mBldnoCallInInf.setVisibility(View.VISIBLE);
                mInfnoContinue.setVisibility(View.GONE);
                // 검체
                mSpcnoInfo.setVisibility(View.GONE);
                mSpcno.setVisibility(View.GONE);
                mSpcnoOk.setVisibility(View.GONE);
                mSpcnoErr.setVisibility(View.GONE);
                mSpcnoButtons.setVisibility(View.GONE);
                // 혈액
                mBldnoInfo.setVisibility(View.GONE);
                mBldno.setVisibility(View.GONE);
                mBldnoOk.setVisibility(View.GONE);
                mBldnoErr.setVisibility(View.GONE);
                mBldnoButtons.setVisibility(View.GONE);
                //
                mInfno.requestFocus();
            }
        });

    }

    private void setOnInfnoOk() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPidInfo.setVisibility(View.GONE);
                mPid.setVisibility(View.GONE);
                //
                mPidDisp.setVisibility(View.VISIBLE);
                mPnmDisp.setVisibility(View.VISIBLE);
                mWardDisp.setVisibility(View.VISIBLE);
                // 수액
                mInfnoInfo.setVisibility(View.VISIBLE);
                mInfno.setVisibility(View.GONE);
                mInfnoOk.setVisibility(View.VISIBLE);
                mInfnoErr.setVisibility(View.GONE);
                mInfnoButtons.setVisibility(View.VISIBLE);
                mSpcnoCallInInf.setVisibility(View.GONE);
                mBldnoCallInInf.setVisibility(View.GONE);
                mInfnoContinue.setVisibility(View.VISIBLE);
                // 검체
                mSpcnoInfo.setVisibility(View.GONE);
                mSpcno.setVisibility(View.GONE);
                mSpcnoOk.setVisibility(View.GONE);
                mSpcnoErr.setVisibility(View.GONE);
                mSpcnoButtons.setVisibility(View.GONE);
                // 혈액
                mBldnoInfo.setVisibility(View.GONE);
                mBldno.setVisibility(View.GONE);
                mBldnoOk.setVisibility(View.GONE);
                mBldnoErr.setVisibility(View.GONE);
                mBldnoButtons.setVisibility(View.GONE);
                //
                mInfnoOk.requestFocus();
            }
        });
    }

    private void setOnInfnoErr() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPidInfo.setVisibility(View.GONE);
                mPid.setVisibility(View.GONE);
                //
                mPidDisp.setVisibility(View.VISIBLE);
                mPnmDisp.setVisibility(View.VISIBLE);
                mWardDisp.setVisibility(View.VISIBLE);
                // 수액
                mInfnoInfo.setVisibility(View.VISIBLE);
                mInfno.setVisibility(View.GONE);
                mInfnoOk.setVisibility(View.GONE);
                mInfnoErr.setVisibility(View.VISIBLE);
                mInfnoButtons.setVisibility(View.VISIBLE);
                mSpcnoCallInInf.setVisibility(View.GONE);
                mBldnoCallInInf.setVisibility(View.GONE);
                mInfnoContinue.setVisibility(View.VISIBLE);
                // 검체
                mSpcnoInfo.setVisibility(View.GONE);
                mSpcno.setVisibility(View.GONE);
                mSpcnoOk.setVisibility(View.GONE);
                mSpcnoErr.setVisibility(View.GONE);
                mSpcnoButtons.setVisibility(View.GONE);
                // 혈액
                mBldnoInfo.setVisibility(View.GONE);
                mBldno.setVisibility(View.GONE);
                mBldnoOk.setVisibility(View.GONE);
                mBldnoErr.setVisibility(View.GONE);
                mBldnoButtons.setVisibility(View.GONE);
                //
                mBldnoErr.requestFocus();
                //
                mVibrator.vibrate(1000);// 1초간 진동
            }
        });
    }

    private void clearScanField() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                mPid.setText("");
                mSpcno.setText("");
                mBldno.setText("");
                mInfno.setText("");
            }
        });
    }

    private void setPidTextWatcher() {
        mPid.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                String pid = s.toString();
                if (pid.length() >= 9) {
                    afterPidScan();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private void setSpcnoTextWatcher() {
        mSpcno.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                String spcno = s.toString();
                if (spcno.length() >= 10) {
                    afterSpcnoScan();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    private void setBldnoTextWatcher() {
        mBldno.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                String bldno = s.toString();
                if (bldno.length() >= 10) {
                    afterBldnoScan();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }


    private void setInfnoTextWatcher() {
        mInfno.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
                String infno = s.toString();
                if (infno.length() >= 9) {
                    afterInfnoScan();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v.getId() == R.id.ptnt_call_in_spc || v.getId() == R.id.ptnt_call_in_bld || v.getId() == R.id.ptnt_call_in_inf) {
            clearScanField();
            setOnPid();
        } else if (v.getId() == R.id.bldno_call_in_spc || v.getId() == R.id.bldno_call_in_inf) {
            clearScanField();
            setOnBldno();
        } else if (v.getId() == R.id.spcno_call_in_bld || v.getId() == R.id.spcno_call_in_inf) {
            clearScanField();
            setOnSpcno();
        } else if (v.getId() == R.id.infno_call_in_spc || v.getId() == R.id.infno_call_in_bld) {
            clearScanField();
            setOnInfno();
        } else if (v.getId() == R.id.spcno_continue) {
            clearScanField();
            setOnSpcno();
        } else if (v.getId() == R.id.bldno_continue) {
            clearScanField();
            setOnBldno();
        } else if (v.getId() == R.id.infno_continue) {
            clearScanField();
            setOnInfno();
        }
    }

    private void saveCheckResult(final String chktype, final String chkdata, final String chkresult) {
        //mDialog = ProgressDialog.show(this, "", "처리중입니다", true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                HashMap<String, String> param = new HashMap<String, String>();
                param.clear();
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String pid = mPidDisp.getText().toString();
                mXmlSave = "";
                param.put("hospitalid", hospitalId);
                param.put("userid", userId);
                param.put("mode", "10");
                param.put("pid", pid);
                param.put("chktype", chktype);
                param.put("chkdata", chkdata);
                param.put("chkresult", chkresult);
                mXmlSave = getXml("ChartServlet", param);

                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        try {
                            afterSaveCheckResult();
                            //mDialog.dismiss();
                        } catch (Exception ex) {
                        }
                    }
                });
            }
        }).start();
    }

    private void afterSaveCheckResult() {

    }

    // 2025.12.08 WOOIL
    private BarcodeCallback mBarcodeCallback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result == null || result.getText() == null) {
                return;
            }

            String value = result.getText();

            // 한 번 읽었으면 현재 타겟 EditText 에 값 세팅
            if (mCurrentScanTarget != null) {
                mCurrentScanTarget.setText(value);
                mCurrentScanTarget.setSelection(value.length()); // 커서 끝으로
                mCurrentScanTarget.clearFocus();
            }

            // 카메라/스캔 정지 및 숨김
            stopCamera();
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
            // 필요 없으면 비워 둡니다
        }
    };

    // 2025.12.08 WOOIL
    private void startCamera() {
        if (mBarcodeScanner == null) return;
        // 2026.01.02 WOOIL - 카메라를 사용하지 않는 병원은 카메라를 보이지 않게 한다.
        if (EmrSettingsUtil.getBarcodeScannerYn(getBaseContext()) == false) return;

        mBarcodeScanner.setVisibility(View.VISIBLE);
        mBarcodeScanner.resume();
    }

    // 2025.12.08 WOOIL
    private void stopCamera() {
        if (mBarcodeScanner == null) return;

        mBarcodeScanner.pause();
        mBarcodeScanner.setVisibility(View.GONE);
    }

    // 2025.12.08 WOOIL
    private void startCameraWithPermissionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        REQ_CAMERA_PERMISSION);
                return;
            }
        }

        // 퍼미션이 이미 허용된 경우
        startCamera();
    }

    // 2025.12.08 WOOIL
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults != null
                    && grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 허용됨 → 카메라 시작
                startCamera();
            } else {
                // 거부됨 → 그냥 스캔 없이 입력하게 둠
                stopCamera();
            }
        }
    }


}
