package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.ConsentFormListAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.ArrayList;
import java.util.HashMap;

//import android.widget.ListView;
//import android.widget.SimpleAdapter;

public class ConsentFormList extends MyActivity {
    private Activity mActivity;

    private final int REQ_SELECT_PATIENT = 1;
    private final int REQ_CONSENT_FORM = 2;

    private String xml, xmlChild, xmlPreSaved, mCXml;

    private String mPid;
    private String mBededt;
    private String mBdiv;
    private String mDptcd; // 2019.10.29 WOOIL - 진료과코드
    private String mBedodt; // 2021.08.10 WOOIL - 퇴원일(외래는 접수시간)
    private String mDrid; // 2024.06.21 WOOIL - 의사ID
    private String mQfycd; // 2024.06.24 WOOIL - 자격
    private String mXmlPatientInfo;

    private ExpandableListView mList;


    @Override
    public void onCreate(Bundle savedInstanceState) {

        // 환자를 물고 왔을 수 있다.
        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mXmlPatientInfo = intent.getStringExtra("patientinfo");
        mBdiv = intent.getStringExtra("bdiv");
        mDptcd = intent.getStringExtra("dptcd"); // 2019.10.29 WOOIL - 진료과코드
        mBedodt = intent.getStringExtra("bedodt"); // 2021.08.10 WOOIL - 퇴원일(외래는 진료일시)
        mDrid =  intent.getStringExtra("drid"); // 2024.06.21 WOOIL - 의사ID
        mQfycd =  intent.getStringExtra("qfycd"); // 2024.06.24 WOOIL - 자격

        // 환자가 없이 넘어오는 경우가 있음.
        if (mPid == null) mPid = "";
        if (mBededt == null) mBededt = "";
        if (mXmlPatientInfo == null) mXmlPatientInfo = "";
        if (mBdiv == null) mBdiv = "";
        if (mDptcd == null) mDptcd = "";
        if (mBedodt == null) mBedodt = "";
        if (mDrid == null) mDrid = "";
        if (mQfycd == null) mQfycd = "";

        String fromTitle = intent.getStringExtra("fromtitle");
        if (fromTitle == null) fromTitle = "";
        if ("".equalsIgnoreCase(fromTitle)) fromTitle = getString(R.string.inpatient_list);

        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.certificate_list, fromTitle);

        mActivity = this;

        mList = (ExpandableListView) findViewById(R.id.certificate_list);

        // 이벤트연결
        mList.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
            @Override
            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                // TODO Auto-generated method stub
                HashMap<String, Object> selectedMap = (HashMap<String, Object>) parent.getExpandableListAdapter().getChild(groupPosition, childPosition);
                final String ccfId = (String) selectedMap.get("ccf_id");
                final String ccfName = (String) selectedMap.get("ccf_name");
                final String preSaved = (String) selectedMap.get("pre_saved");
                final String ccfFileName = (String) selectedMap.get("ccf_filename");
                final String exdt = (String) selectedMap.get("exdt");
                final String seq = (String) selectedMap.get("seq");
                final String emrScanClass = (String) selectedMap.get("emr_scan_class");
                final String subPageList = (String) selectedMap.get("sub_page_list");
                final String preSavedBdiv = (String) selectedMap.get("pre_saved_bdiv");
                final String hxType = (String) selectedMap.get("hx_type");

                if ( "1".equalsIgnoreCase(mBdiv)) {
                    checkCancelAndCallConformForm(ccfId, ccfName, preSaved, ccfFileName, exdt, seq, emrScanClass, subPageList, preSavedBdiv, hxType);
                } else {
                    callConformForm(ccfId, ccfName, preSaved, ccfFileName, exdt, seq, emrScanClass, subPageList, preSavedBdiv, hxType);
                }

                return false;
            }
        });
        mList.setOnItemLongClickListener(new GridView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> selectedMap = (HashMap<String, Object>) (HashMap<String, Object>) parent.getAdapter().getItem(position);
                String preSaved = (String) selectedMap.get("pre_saved");
                final String preSavedBdiv = (String) selectedMap.get("pre_saved_bdiv");
                final String exdt = (String) selectedMap.get("exdt");
                final String seq = (String) selectedMap.get("seq");
                final String subPageList = (String) selectedMap.get("sub_page_list");

                //showSimpleDialog("preSaved="+preSaved);
                //showSimpleDialog("preSavedBdiv="+preSavedBdiv);
                //showSimpleDialog("exdt="+exdt);
                //showSimpleDialog("seq="+seq);
                //showSimpleDialog("subPageList="+subPageList);

                /*
                 * 2024.06.11 WOOIL - 테스트할 때 만 풀고 하자...
                String msg = EmrSettingsUtil.getUncaughtExceptionMessage(mActivity);
                if(!"".equals(msg)){
                    showSimpleDialog(msg);
                }
                */


                if (preSaved == null) return false;
                if (preSaved.equalsIgnoreCase("y")) {
                    PopupMenu menu = new PopupMenu(ConsentFormList.this, view);
                    menu.getMenu().add(0, 1, 0, "삭제");
                    menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            // TODO Auto-generated method stub
                            deletePreSaved(preSavedBdiv, exdt, seq, subPageList);
                            return false;
                        }
                    });
                    menu.show();
                }

                return true;
            }
        });


        if ("".equalsIgnoreCase(mPid)) {
            // 특정환자를 선택하지 않고 넘어왔으므로 환자를 선택하는 버튼을 보이게 한다.
            setButton1(true, "환자선택", BUTTON_TYPE_NONE);
        }

//        String packageName = getPackageName();
//        // 제목표시줄 밑에 있는 TEMR 로그를 TEMR 페키지만 보이도록 처리
//        RelativeLayout topBgLayout = (RelativeLayout)findViewById(R.id.top_bg_layout);
//        if(!packageName.equals(EmrSettingsUtil.PACKAGE_TEMR)){
//        	topBgLayout.setVisibility(View.GONE);
//        }

        // 리스트 조회
        if (savedInstanceState == null) {
            getCertificateGroupList("");
        } else {
            mXmlPatientInfo = savedInstanceState.getString("xmlPatientInfo");
            xml = savedInstanceState.getString("xml");
            xmlChild = savedInstanceState.getString("xmlChild");
            xmlPreSaved = savedInstanceState.getString("xmlPreSaved");
            afterGetCertificateGroupList();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("xmlPatientInfo", mXmlPatientInfo);
        outState.putString("xml", xml);
        outState.putString("xmlChild", xmlChild);
        outState.putString("xmlPreSaved", xmlPreSaved);
    }

    @Override
    public void onClickQueryButton(View v) {
        getCertificateGroupList("");
    }

    @Override
    public void onClickButton1(View v) {
        // 환자선택 창을 띄운다.
        Intent intent = new Intent(this, SelectPatientDialog.class);
        startActivityForResult(intent, REQ_SELECT_PATIENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) { // 액티비티가 정상적으로 종료되었을 경우
            if (requestCode == REQ_SELECT_PATIENT) {
                // 선택된 환자를 보여줌
                mPid = data.getStringExtra("pid");
                mBededt = data.getStringExtra("bededt");
                mBdiv = data.getStringExtra("bdiv");
                mXmlPatientInfo = data.getStringExtra("patientinfo");
                // 다시조회
                getCertificateGroupList("1");
            } else if (requestCode == REQ_CONSENT_FORM) {
                // 다시조회
                getCertificateGroupList("1");
            }
        }
    }

    private void getCertificateGroupList(final String queryFlag) {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String url = "";
                if ("".equalsIgnoreCase(queryFlag)) {
                    // 동의서그룹
                    url = "CertificatePaperServlet?mode=4&hospitalid=" + hospitalId + "&userid=" + userId;
                    xml = getXml(url);
                    // 동의서리스트
                    url = "CertificatePaperServlet?mode=0&hospitalid=" + hospitalId + "&userid=" + userId;
                    xmlChild = getXml(url);
                }
                // 임시저장 동의서리스트
                if ("".equalsIgnoreCase(mPid)) {
                    xmlPreSaved = "";
                } else {
                    url = "CertificatePaperServlet?mode=5&hospitalid=" + hospitalId + "&userid=" + userId + "&pid=" + mPid;
                    xmlPreSaved = getXml(url);
                }
                // 종료
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetCertificateGroupList();
                            mDialog.dismiss();
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
        ;
    }

    private void afterGetCertificateGroupList() {
        ResultSetHelper rs, rsChild, rsPreSaved;

        ((TextView) findViewById(R.id.patientInfoTextView)).setText(mXmlPatientInfo);

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        ArrayList<HashMap<String, Object>> mychildlist = new ArrayList<HashMap<String, Object>>();
        //ArrayList<HashMap<String,Object>> mychild = new ArrayList<HashMap<String,Object>>();
        //ArrayList<ArrayList<HashMap<String,Object>>> mychildlist = new ArrayList<ArrayList<HashMap<String,Object>>>();
        HashMap<String, Object> map = null;
        HashMap<String, Object> mapChild = null;

        // xml해부
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            rsChild = new ResultSetHelper(xmlChild, EmrSettingsUtil.getMaskYn(getBaseContext()));
            rsPreSaved = new ResultSetHelper(xmlPreSaved, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rsChild.getReturnCode() < 0) {
                showSimpleDialog(rsChild.getReturnDesc());
            } else if (rsPreSaved.getReturnCode() < 0) {
                showSimpleDialog(rsPreSaved.getReturnDesc());
            } else if (rs.getReturnCode() == 0 && rsChild.getReturnCode() == 0 && rsPreSaved.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();
                // 임시저장된 리스트
                if (rsPreSaved.getRecordCount() > 0) {
                    String ccfGroup = rsPreSaved.getString(0, "ccf_group");
                    map = new HashMap<String, Object>();
                    map.put("ccf_group", ccfGroup);
                    mylist.add(map);

                    for (int i = 0; i < rsPreSaved.getRecordCount(); i++) {
                        String strSubPageNo = rsPreSaved.getString(i, "sub_page_no");
                        String strSubPageList = rsPreSaved.getString(i, "sub_page_list");
                        if ("".equals(strSubPageNo)) {
                            // sub_page_no 에 값이 있으면 2페이지 이상임.
                            mapChild = new HashMap<String, Object>();
                            mapChild.put("ccf_id", rsPreSaved.getString(i, "pre_saved_ccf_id")); // 2020.06.04 WOOIL
                            mapChild.put("ccf_name", rsPreSaved.getString(i, "ccf_name"));
                            mapChild.put("ccf_filename", rsPreSaved.getString(i, "ccf_filename"));
                            mapChild.put("ccf_group", rsPreSaved.getString(i, "ccf_group"));
                            mapChild.put("pre_saved", "Y");
                            mapChild.put("exdt", rsPreSaved.getString(i, "exdt"));
                            mapChild.put("seq", rsPreSaved.getString(i, "seq"));
                            mapChild.put("emr_scan_class", rsPreSaved.getString(i, "emr_scan_class"));
                            mapChild.put("sub_page_list", strSubPageList);
                            mapChild.put("sub_page_no", strSubPageNo);
                            mapChild.put("pre_saved_bdiv", rsPreSaved.getString(i, "pre_saved_bdiv"));
                            mapChild.put("hx_type", ""); // 2023.03.06 WOOIL - 임시저장내역을 가져오는 경우 이력 정보를 조회할 수 없게 한다.

                            mychildlist.add(mapChild);
                        }
                    }
                }
                // 동의서목록그룹리스트
                for (int i = 0; i < rs.getRecordCount(); i++) {
                    String ccfGroup = rs.getString(i, "ccf_group");
                    // 2024.03.13 WOOIL - 한 동의서를 여러 그룹에 넣기 위한 작업.
                    String[] aGroup = ccfGroup.split(";");
                    for(String grp : aGroup) {
                        map = new HashMap<String, Object>();
                        map.put("ccf_group", grp);
                        mylist.add(map);
                    }
                }
                // 동의서목록리스트
                for (int i = 0; i < rsChild.getRecordCount(); i++) {
                    String strSubPageNo = rsChild.getString(i, "sub_page_no");
                    String strSubPageList = rsChild.getString(i, "sub_page_list");
                    if ("".equals(strSubPageNo)) {
                        // sub_page_no 에 값이 있으면 2페이지 이상임.
                        String ccfGroup = rsChild.getString(i, "ccf_group");
                        // 2024.03.13 WOOIL - 한 동의서를 여러 그룹에 넣기 위한 작업.
                        String[] aGroup = ccfGroup.split(";");
                        for(String grp : aGroup) {
                            mapChild = new HashMap<String, Object>();
                            mapChild.put("ccf_id", rsChild.getString(i, "ccf_id"));
                            mapChild.put("ccf_name", rsChild.getString(i, "ccf_name"));
                            mapChild.put("ccf_filename", rsChild.getString(i, "ccf_filename"));
                            mapChild.put("ccf_group", grp);
                            mapChild.put("pre_saved", "");
                            mapChild.put("exdt", "");
                            mapChild.put("seq", "");
                            mapChild.put("emr_scan_class", rsChild.getString(i, "emr_scan_class"));
                            mapChild.put("sub_page_list", strSubPageList);
                            mapChild.put("sub_page_no", strSubPageNo);
                            mapChild.put("pre_saved_bdiv", "");
                            mapChild.put("hx_type", rsChild.getString(i, "hx_type")); // 2023.03.06 WOOIL - 이력정보

                            mychildlist.add(mapChild);
                        }
                    }
                }

                ConsentFormListAdapter adapter = new ConsentFormListAdapter(this, mylist, mychildlist);
                mList.setAdapter(adapter);

                String hosId = EmrSettingsUtil.getHospitalId(this);
                String collapseYn = EmrSettingsUtil.getCollapseYn(this);
                if("Y".equalsIgnoreCase(collapseYn)){
                    // 옵션에 따라 펼치지 않는다.(대구W, 부천예손)
                }else {
                    for (int i = 0; i < adapter.getGroupCount(); i++) {
                        mList.expandGroup(i);
                    }
                }
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    private void deletePreSaved(final String preSavedBdiv, final String exdt, final String seq, final String subPageList) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("확인");
        dialog.setMessage("삭제하시겠습니까?");
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                actionDeletePreSaved(preSavedBdiv, exdt, seq, subPageList);
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

    private void actionDeletePreSaved(final String preSavedBdiv, final String exdt, final String seq, final String subPageList) {
        mDialog = ProgressDialog.show(ConsentFormList.this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String url = "";
                String mode = "13";

                // 임시저장 이미지 삭제(실제로는 테이블에 플래그를 넣는다).
                url = "ChartServlet?hospitalid=" + hospitalId +
                        "&userid=" + userId +
                        "&pid=" + mPid +
                        "&pre_saved_bdiv=" + preSavedBdiv +
                        "&exdt=" + exdt +
                        "&seq=" + seq +
                        "&sub_page_list=" + subPageList +
                        "&mode=" + mode;
                final String xml = getXml(url);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            afterActionDeletePreSaved(xml);
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
    }

    private void afterActionDeletePreSaved(String xml) {
        if (xml.equalsIgnoreCase("y")) getCertificateGroupList("1"); // 성공. 다시 조회.
    }
    private void callConformForm(String ccfId, String ccfName, String preSaved, String ccfFileName, String exdt, String seq, String emrScanClass, String subPageList, String preSavedBdiv, String hxType){
        Intent intent = new Intent(ConsentFormList.this, ConsentForm.class);
        intent.putExtra("ccfId", ccfId);
        intent.putExtra("ccfName", ccfName);
        intent.putExtra("preSaved", preSaved);
        intent.putExtra("ccfFileName", ccfFileName);
        intent.putExtra("pid",  mPid);
        intent.putExtra("bededt", mBededt);
        intent.putExtra("bdiv", mBdiv);
        intent.putExtra("dptcd", mDptcd); // 2019.10.29 WOOIL - 진료과코드
        intent.putExtra("bedodt", mBedodt); // 2021.08.10 WOOIL - 퇴원일(외래는 진료일시)
        intent.putExtra("exdt", exdt);
        intent.putExtra("seq", seq);
        intent.putExtra("emrScanClass", emrScanClass);
        intent.putExtra("subPageList", subPageList);
        intent.putExtra("preSavedBdiv", preSavedBdiv);
        intent.putExtra("hx_type", hxType);
        intent.putExtra("drid", mDrid); // 2024.06.21 WOOIL - 의사ID
        intent.putExtra("qfycd", mQfycd); // 2024.06.24 WOOIL - 자격
        startActivityForResult(intent, REQ_CONSENT_FORM);
    }
    private void checkCancelAndCallConformForm(final String ccfId, final String ccfName, final String preSaved, final String ccfFileName, final String exdt, final String seq, final String emrScanClass, final String subPageList, final String preSavedBdiv, final String hxType) {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                HashMap<String, String> param = new HashMap<String, String>();
                param.clear();
                String hospitalId = getHospitalId();
                String userId = getUserId();
                mCXml = "";
                param.put("hospitalid", hospitalId);
                param.put("userid", userId);
                param.put("mode", "7");
                param.put("pid", mPid);
                param.put("exdt", mBededt);
                param.put("dptcd", mDptcd);
                param.put("hms", mBedodt);
                mCXml = getXml("InPatientListServlet", param);
                // 종료
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterCheckCancelAndCallConformForm(ccfId, ccfName, preSaved, ccfFileName, exdt, seq, emrScanClass, subPageList, preSavedBdiv, hxType);
                            mDialog.dismiss();
                        } catch (Exception ex) {
                        }
                    }
                });
            }
        }).start();
    }

    private void afterCheckCancelAndCallConformForm(String ccfId, String ccfName, String preSaved, String ccfFileName, String exdt, String seq, String emrScanClass, String subPageList, String preSavedBdiv, String hxType) {
        ResultSetHelper rs;
        // xml해부
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            rs = new ResultSetHelper(mCXml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                int cnt = rs.getInt(0, "cnt");
                if (cnt<1) {
                    showSimpleDialog("접수 내역이 취소되었거나 변경된 자료입니다. 환자를 다시 조회 후 작업하세요.");
                } else {
                    callConformForm(ccfId, ccfName, preSaved, ccfFileName, exdt, seq, emrScanClass, subPageList, preSavedBdiv, hxType);
                }
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }

    }
}
