package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.EmrScanAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class SignedConsentFormList extends MyActivity {

    static final int FR_DATE_DIALOG_ID = 0;
    static final int TO_DATE_DIALOG_ID = 1;

    private static final int SEL_PATIENT = 2001;
    private static final int RE_SAVE = 2002; // ConsentForm 호출 정보


    private ArrayList<HashMap<String, Object>> mArrayList;
    private String mXmlPatientInfo, xml;

    private String mPid;
    private String mBededt;
    private String mBedodt;
    private String mBdiv;

    private int mFrYear, mFrMonth, mFrDay;
    private int mToYear, mToMonth, mToDay;


    private Button mFrDateButton;
    private Button mToDateButton;
    private TextView mPatientInfoTextView;
    private ListView mSignedCertificateList;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.signed_certificate_list, "이전");

        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mBedodt = intent.getStringExtra("bedodt");
        mXmlPatientInfo = intent.getStringExtra("patientinfo");
        mBdiv = intent.getStringExtra("bdiv");

        // 오류방지용
        if (mPid == null) mPid = "";
        if (mBededt == null) mBededt = "";
        if (mBedodt == null) mBedodt = "";
        if (mBdiv == null) mBdiv = "2"; // 1.외래 2.입원 3.응급  기본 입원

        // -----------------------------------------------------
        // 환자정보
        // -----------------------------------------------------
        mPatientInfoTextView = (TextView) findViewById(R.id.patient_info_text_view);

        mFrDateButton = (Button) findViewById(R.id.pickFrDate);
        mFrDateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //showDialog(FR_DATE_DIALOG_ID);
                DialogDatePicker(FR_DATE_DIALOG_ID);
            }
        });
        mToDateButton = (Button) findViewById(R.id.pickToDate);
        mToDateButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //showDialog(TO_DATE_DIALOG_ID);
                DialogDatePicker(TO_DATE_DIALOG_ID);
            }
        });
        // 2024.11.22 WOOIL - 외래.응급실 환자도 기간을 정할 수 있게 수정
        //if (mBdiv.equals("1") || mBdiv.equals("3")) {
        //    mFrDateButton.setVisibility(View.GONE);
        //    mToDateButton.setVisibility(View.GONE);
        //    ((TextView) findViewById(R.id.pickDateBar)).setVisibility(View.GONE);
        //}


        mSignedCertificateList = (ListView) findViewById(R.id.signed_certificate_list);
        mSignedCertificateList.setOnItemClickListener(new GridView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> selectedMap = (HashMap<String, Object>) parent.getAdapter().getItem(position);
                Intent intent = new Intent(SignedConsentFormList.this, EmrScanView.class);
                intent.putExtra("pid", mPid);
                intent.putExtra("bededt", mBededt);
                intent.putExtra("bdiv", (String) selectedMap.get("bdiv"));
                intent.putExtra("exdt", (String) selectedMap.get("exdt"));
                intent.putExtra("seq", (String) selectedMap.get("seq"));
                intent.putExtra("rptcd", (String) selectedMap.get("rptcd"));
                intent.putExtra("path", (String) selectedMap.get("path"));
                intent.putExtra("path2", (String) selectedMap.get("path2"));
                intent.putExtra("from", "signed");
                intent.putExtra("patientinfo", mXmlPatientInfo);
                intent.putExtra("sub_page_list", (String) selectedMap.get("sub_page_list")); // 2022.03.22 WOOIL - 서브페이지 리스트
                intent.putExtra("sub_page_no", (String) selectedMap.get("sub_page_no")); // 2022.03.22 WOOIL - 서브페이지 여부
                startActivity(intent);
            }
        });
        // 2022.03.23 WOOIL - 삭제기능추가
        mSignedCertificateList.setOnItemLongClickListener(new GridView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> selectedMap = (HashMap<String, Object>) parent.getAdapter().getItem(position);
                final String bdiv = (String) selectedMap.get("bdiv");
                final String exdt = (String) selectedMap.get("exdt");
                final String seq = (String) selectedMap.get("seq");
                final String rptcd = (String) selectedMap.get("rptcd");
                final String subPageList = (String) selectedMap.get("sub_page_list");

                // 2026.02.03 WOOIL - 수정 메뉴 추가
                PopupMenu menu = new PopupMenu(SignedConsentFormList.this, view);
                menu.getMenu().add(0, 1, 1, "수정");
                menu.getMenu().add(0, 2, 2, "삭제");
                menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        // TODO Auto-generated method stub
                        if (item.getItemId() == 1) {
                            reSaveEmrScan(selectedMap);
                        } else if (item.getItemId() == 2) {
                            // 삭제
                            deleteEmrScan(bdiv, exdt, seq, rptcd, subPageList);
                            return true;
                        }
                        return false;
                    }
                });
                menu.show();

                return true;
            }
        });

        if ("".equalsIgnoreCase(mPid)) {
            // 특정환자를 선택하지 않고 넘어왔으므로 환자를 선택하는 버튼을 보이게 한다.
            setButton1(true, "환자선택", BUTTON_TYPE_NONE);
        }

        // 조회기간 초기화
        initFrToDate();
        displayFrDate();
        displayToDate();

        // 조회
        DisplayPatientInfo();
        getList();

    }

    @Override
    public void onClickQueryButton(View v) {
        getList();
    }

    @Override
    public void onClickButton1(View v) {
        // 환자선택 창을 띄운다.
        Intent intent = new Intent(this, SelectPatientDialog.class);
        startActivityForResult(intent, SEL_PATIENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) { // 액티비티가 정상적으로 종료되었을 경우
            if (requestCode == SEL_PATIENT) {
                // 선택된 환자를 보여줌
                mPid = data.getStringExtra("pid");
                mBededt = data.getStringExtra("bededt");
                mBedodt = data.getStringExtra("bedodt");
                mXmlPatientInfo = data.getStringExtra("patientinfo");
                // 조회기간 초기화
                initFrToDate();
                displayFrDate();
                displayToDate();
                // 다시조회
                DisplayPatientInfo();
                getList();
            }
            if (requestCode == RE_SAVE) {
                getList();
            }
        }
    }

    private void initFrToDate() {
        Calendar c = Calendar.getInstance();
        if (mBededt.equals("")) {
            mFrYear = c.get(Calendar.YEAR);
            mFrMonth = c.get(Calendar.MONTH);
            mFrDay = c.get(Calendar.DAY_OF_MONTH);
        } else {
            mFrYear = Integer.parseInt(mBededt.substring(0, 4));
            mFrMonth = Integer.parseInt(mBededt.substring(4, 6)) - 1;
            mFrDay = Integer.parseInt(mBededt.substring(6, 8));
        }
        if (mBdiv.equals("1")) {
            mToYear = Integer.parseInt(mBededt.substring(0, 4));
            mToMonth = Integer.parseInt(mBededt.substring(4, 6)) - 1;
            mToDay = Integer.parseInt(mBededt.substring(6, 8));
        } else {
            if (mBedodt.equals("")) {
                mToYear = c.get(Calendar.YEAR);
                mToMonth = c.get(Calendar.MONTH);
                mToDay = c.get(Calendar.DAY_OF_MONTH);
            } else {
                mToYear = Integer.parseInt(mBedodt.substring(0, 4));
                mToMonth = Integer.parseInt(mBedodt.substring(4, 6)) - 1;
                mToDay = Integer.parseInt(mBedodt.substring(6, 8));
            }
        }
    }

    private void displayFrDate() {
        mFrDateButton.setText(super.getFormattedDate(getFrDate()));
    }

    private void displayToDate() {
        String frDate = getFrDate();
        mToDateButton.setText(super.getFormattedDate(getToDate()));
    }

    private String getFrDate() {
        String yearString = Integer.toString(mFrYear);
        String monthString = Integer.toString(mFrMonth + 101);
        String dayString = Integer.toString(mFrDay + 100);
        String ret = yearString + monthString.substring(1, 3) + dayString.substring(1, 3);

        return ret;
    }

    private String getToDate() {
        String yearString = Integer.toString(mToYear);
        String monthString = Integer.toString(mToMonth + 101);
        String dayString = Integer.toString(mToDay + 100);
        String ret = yearString + monthString.substring(1, 3) + dayString.substring(1, 3);

        return ret;
    }

    private void DialogDatePicker(final int id) {
        DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            // onDateSet method
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                if (id == FR_DATE_DIALOG_ID) {
                    mFrYear = year;
                    mFrMonth = monthOfYear;
                    mFrDay = dayOfMonth;
                    displayFrDate();
                } else {
                    mToYear = year;
                    mToMonth = monthOfYear;
                    mToDay = dayOfMonth;
                    displayToDate();
                }
                // 변경된 일자로 조회
                getList();
            }
        };
        if (id == FR_DATE_DIALOG_ID) {
            DatePickerDialog alert = new DatePickerDialog(this, mDateSetListener, mFrYear, mFrMonth, mFrDay);
            alert.show();
        } else {
            DatePickerDialog alert = new DatePickerDialog(this, mDateSetListener, mToYear, mToMonth, mToDay);
            alert.show();
        }
    }

    private void getList() {
        if (mPid.equals("") || mBededt.equals("")) {
            //showSimpleDialog("환자를 선택하세요.");
            return;
        }

        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String url = "";
                String frDate = getFrDate();
                String toDate = getToDate();
                String mode = "0";

                // 환자정보
                if (mXmlPatientInfo == null) mXmlPatientInfo = "";

                //
                url = "ChartServlet?mode=5&hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt + "&frdt=" + frDate + "&todt=" + toDate;
                xml = getXml(url);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            afterGetList();
                        } catch (Exception e) {
                            Log.d("EmrDroid", "dialog.dismiss exception");
                            Log.d("EmrDroid", e.getMessage());
                        }
                    }
                });
            }
        }).start();
        ;
    }

    // ----------------------------------------------------------------------------------------------------
    // 기타서식(이미지스캔) 리스트 를 화면에 출력
    // ----------------------------------------------------------------------------------------------------
    private void afterGetList() {
        DisplayPatientInfo();

        //ArrayList<HashMap<String,Object>> mylist = new ArrayList<HashMap<String,Object>>();
        mArrayList = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        ResultSetHelper rs;

        // xml해부
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }

            rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
                mSignedCertificateList.setAdapter(null);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();
                String dirPath = getFilesDir().getAbsolutePath() + "/emrscan";
                File dir = new File(dirPath);
                // 기존에 파일이 있으면 삭제

                String[] children = dir.list();
                if (children != null) {
                    for (int ii = 0; ii < children.length; ii++) {
                        String childrenFileName = children[ii];
                        File f = new File(dirPath + "/" + childrenFileName);
                        f.delete();
                    }
                }

                for (int i = 0; i < rs.getRecordCount(); i++) {
                    String subPageNo = rs.getString(i, "sub_page_no");
                    // 2022.03.22 WOOIL - 첫번째 페이지만 목록에 조회되도록함.
                    //                    sub_page_no에 값이 있으면 두번째 페이지 부터임
                    if ("".equalsIgnoreCase(subPageNo)) {
                        map = new HashMap<String, Object>();
                        map.put("pid", rs.getString(i, "pid"));
                        map.put("bdiv", rs.getString(i, "bdiv"));
                        map.put("exdt", rs.getString(i, "exdt"));
                        map.put("seq", rs.getString(i, "seq"));
                        map.put("rptcd", rs.getString(i, "rptcd"));
                        map.put("rptnm", rs.getString(i, "rptnm"));
                        map.put("path", rs.getString(i, "path"));
                        map.put("path2", rs.getString(i, "path2"));
                        map.put("filename", rs.getString(i, "exdt") + "-" + rs.getString(i, "seq") + ".png");
                        map.put("dirpath", dirPath);
                        map.put("sub_page_list", rs.getString(i, "sub_page_list")); // 2022.03.22 WOOIL - 서브페이지 리스트
                        map.put("sub_page_no", rs.getString(i, "sub_page_no")); // 2022.03.22 WOOIL - 서브페이지 여부
                        map.put("ccf_id", rs.getString(i, "ccf_id")); // 2026.02.04 WOOIL
                        map.put("ccf_name", rs.getString(i, "ccf_name")); // 2026.02.04 WOOIL
                        map.put("ccf_filename", rs.getString(i, "ccf_filename")); // 2026.02.04 WOOIL
                        map.put("emr_scan_class", rs.getString(i, "emr_scan_class")); // 2026.02.04 WOOIL
                        map.put("dptcd", rs.getString(i, "dptcd")); // 2026.02.04 WOOIL
                        map.put("drid", rs.getString(i, "drid")); // 2026.02.04 WOOIL
                        map.put("qfycd", rs.getString(i, "qfycd")); // 2026.02.04 WOOIL
                        mArrayList.add(map);
                    }
                }

                EmrScanAdapter adapter = null;
                adapter = new EmrScanAdapter(this, mArrayList, null); // 2026.02.19 WOOIL - 팝업메뉴로 동작하게 수정.
                //adapter = new EmrScanAdapter(this, mArrayList, new EmrScanAdapter.OnModifyClickListener() {
                //    @Override
                //    public void onModifyClick(HashMap<String, Object> map) {
                //        reSaveEmrScan(map);
                //    }
                //});
                mSignedCertificateList.setAdapter(adapter);

            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    private void DisplayPatientInfo() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                mPatientInfoTextView.setText(mXmlPatientInfo);
            }
        });
    }

    private void reSaveEmrScan(final HashMap<String, Object> map) {

        final String pid = (String) map.get("pid");
        final String bededt = (String) map.get("bededt");
        final String ccfId = (String) map.get("ccf_id");
        final String ccfName = (String) map.get("ccf_name");
        final String ccfFileName = (String) map.get("ccf_filename");
        final String exdt = (String) map.get("exdt");
        final String seq = (String) map.get("seq");
        final String emrScanClass = (String) map.get("emr_scan_class");
        final String subPageList = (String) map.get("sub_page_list");
        final String bdiv = (String) map.get("bdiv");
        final String bedodt = (String) map.get("bedodt");
        final String dptcd = (String) map.get("dptcd");
        final String drid = (String) map.get("drid");
        final String qfycd = (String) map.get("qfycd");
        final String preSaved = "";
        final String preSavedBdiv = "";
        final String hxType = "";

        Intent intent = new Intent(SignedConsentFormList.this, ConsentForm.class);
        intent.putExtra("ccfId", ccfId);
        intent.putExtra("ccfName", ccfName);
        intent.putExtra("ccfFileName", ccfFileName);
        intent.putExtra("pid", pid);
        intent.putExtra("bededt", bededt);
        intent.putExtra("bdiv", bdiv);
        intent.putExtra("dptcd", dptcd);
        intent.putExtra("drid", drid);
        intent.putExtra("qfycd", qfycd);
        intent.putExtra("bedodt", bedodt);
        intent.putExtra("exdt", exdt);
        intent.putExtra("seq", seq);
        intent.putExtra("emrScanClass", emrScanClass);
        intent.putExtra("subPageList", subPageList);
        intent.putExtra("preSaved", preSaved); // 공백
        intent.putExtra("preSavedBdiv", preSavedBdiv); // 공백
        intent.putExtra("hx_type", hxType); // 공백
        intent.putExtra("re_save_yn", "Y"); // 2026.02.04 WOOIL - 동의서를 다시 작성함 여부

        startActivityForResult(intent, RE_SAVE);
    }

    private void deleteEmrScan(final String bdiv, final String exdt, final String seq, final String rptcd, final String subPageList) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("확인");
        dialog.setMessage("삭제하시겠습니까?");
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                actionDeleteEmrScan(bdiv, exdt, seq, rptcd, subPageList);
            }
        });
        dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void actionDeleteEmrScan(final String bdiv, final String exdt, final String seq, final String rptcd, final String subPageList) {
        mDialog = ProgressDialog.show(SignedConsentFormList.this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String url = "";
                String mode = "12";

                // 이미지 삭제(실제로는 테이블에 플래그를 넣는다).
                url = "ChartServlet?hospitalid=" + hospitalId +
                        "&userid=" + userId +
                        "&pid=" + mPid +
                        "&bdiv=" + bdiv +
                        "&exdt=" + exdt +
                        "&seq=" + seq +
                        "&rptcd=" + rptcd +
                        "&sub_page_list=" + subPageList +
                        "&mode=" + mode;
                final String xml = getXml(url);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            afterActionDeleteEmrScan(xml);
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
    }

    private void afterActionDeleteEmrScan(String xml) {
        if (xml.equalsIgnoreCase("y")) getList(); // 성공. 다시 조회.
    }

}
