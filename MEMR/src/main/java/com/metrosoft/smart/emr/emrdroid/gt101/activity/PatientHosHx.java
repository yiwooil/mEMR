package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TextView;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class PatientHosHx extends MyActivity implements TabHost.OnTabChangeListener {
    private final String IN_PATIENT_LIST = "0";
    private final String OUT_PATIENT_LIST = "1";
    private final String ER_PATIENT_LIST = "2";

    private TabHost mTabHost;
    private TextView mPatientInfoTextView;

    private String mPid;
    private String mBededt;

    private String mXmlPatientInfo, mXmlIn, mXmlOut, mXmlEr;
    private HashMap<String, Object> mSelectedMap = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 파라메터 셋팅
        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mXmlPatientInfo = intent.getStringExtra("patientinfo");
        // 기본값셋팅. 오류방지용
        if (mPid == null) mPid = "";
        if (mBededt == null) mBededt = "";

        String fromTitle = intent.getStringExtra("fromtitle");
        if (fromTitle == null) fromTitle = "";
        if (fromTitle.equals("")) fromTitle = "닫기";

        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.patient_hos_hx, fromTitle);

        // -----------------------------------------------------
        // 환자정보
        // -----------------------------------------------------
        mPatientInfoTextView = (TextView) findViewById(R.id.patientInfoTextView);
        DisplayPatientInfo();
        // -----------------------------------------------------
        // 탭생성
        // -----------------------------------------------------
        mTabHost = (TabHost) findViewById(R.id.tabHost);
        // findViewById를 이용해 TabHost인스턴스를 얻은경우 꼭 호출 필요
        mTabHost.setup();
        // Tab builder 객체
        TabHost.TabSpec spec;
        //
        spec = mTabHost.newTabSpec(IN_PATIENT_LIST);    // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(IN_PATIENT_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_in_patient);            // Tab 내용
        mTabHost.addTab(spec);                            // Tab 등록
        //
        spec = mTabHost.newTabSpec(OUT_PATIENT_LIST);    // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(OUT_PATIENT_LIST)));// Tab 제목
        spec.setContent(R.id.tab_out_patient);            // Tab 내용
        mTabHost.addTab(spec);                             // Tab 등록
        //
        spec = mTabHost.newTabSpec(ER_PATIENT_LIST);    // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(ER_PATIENT_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_er_patient);            // Tab 내용
        mTabHost.addTab(spec);                            // Tab 등록
        // 처음 등록된 Tab을 보여줌.
        mTabHost.setCurrentTab(0);
        // 리스터연결
        mTabHost.setOnTabChangedListener(this);

        // ------------------------------------------------------
        // Context menu 및 리스너
        // ------------------------------------------------------
        // 재원환자리스트
        ListView listView;
        listView = (ListView) findViewById(R.id.list);
        registerForContextMenu(listView);

//        String packageName = getPackageName();
//        // 제목표시줄 밑에 있는 TEMR 로그를 TEMR 페키지만 보이도록 처리
//        RelativeLayout topBgLayout = (RelativeLayout)findViewById(R.id.top_bg_layout);
//        if(!packageName.equals(EmrSettingsUtil.PACKAGE_TEMR)){
//        	topBgLayout.setVisibility(View.GONE);
//        }

        if (savedInstanceState == null) {
            // 조회
            getHosHx();
        } else {
            mXmlPatientInfo = savedInstanceState.getString("xmlPatientInfo");
            mXmlIn = savedInstanceState.getString("xmlIn");
            mXmlOut = savedInstanceState.getString("xmlOut");
            mXmlEr = savedInstanceState.getString("xmlEr");
            // 화면에 다시 출력
            afterGetHosHx();
        }
    }

    private String getTabText(String tag) {
        String tabText = "";
        if (tag.equals(IN_PATIENT_LIST)) {
            tabText = "입원";
        } else if (tag.equals(OUT_PATIENT_LIST)) {
            tabText = "외래";
        } else if (tag.equals(ER_PATIENT_LIST)) {
            tabText = "응급";
        }
        return tabText;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("xmlPatientInfo", mXmlPatientInfo);
        outState.putString("xmlIn", mXmlIn);
        outState.putString("xmlOut", mXmlOut);
        outState.putString("xmlEr", mXmlEr);
    }

    @Override
    public void onClickQueryButton(View v) {
        getHosHx();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        ListView listView = (ListView) view;
        int position = ((AdapterContextMenuInfo) menuInfo).position;
        mSelectedMap = (HashMap<String, Object>) listView.getAdapter().getItem(position);

        String pnm = (String) mSelectedMap.get("pnm");
        String psexage = (String) mSelectedMap.get("psexage");

        menu.setHeaderTitle("Menu " + pnm);
        menu.add(0, 1, 0, R.string.order);      // 처방조회
        menu.add(0, 2, 0, "기록지조회");
        menu.add(0, 3, 0, "투약기록지조회");
        menu.add(0, 4, 0, R.string.result_rad); // 영상진단결과
        menu.add(0, 5, 0, R.string.result_lis); // 진단검사결과
        menu.add(0, 6, 0, R.string.emr_scan);   // 기타서식
        menu.add(0, 7, 0, R.string.tpr);        // TPR조회
        menu.add(0, 8, 0, "동의서목록"); // 동의서목록
        menu.add(0, 9, 0, "동의서열람"); // 동의서목록

        /* 2022.12.16 WOOIL - TEMR 에 관한 부분 삭제
        // 동의서목록은 TEMR은 기본이 아니다.
        if (getPackageName().equalsIgnoreCase(EmrSettingsUtil.PACKAGE_MEMR)) {
            menu.add(0, 8, 0, "동의서목록"); // 동의서목록
            menu.add(0, 9, 0, "동의서열람"); // 동의서목록
        }
        */
        //이화면이 입내원이력이므로 메뉴를 구성하지 않는다.
        //menu.add(0,10,0,R.string.patient_hos_hx);   // 입내원이력

        // 콘텍스트메뉴의 헤더에 커스텀뷰를 연결한다.
        // 커스텀뷰를 연결하므로서 setHeaderTitle은 무시된다.
        LayoutInflater layout = getLayoutInflater();
        View v = layout.inflate(R.layout.custom_dialog_title_bar, null);
        TextView tv = (TextView) v.findViewById(R.id.custom_dialog_title_bar_text);
        tv.setText(pnm + " " + psexage);
        menu.setHeaderView(v);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ListView listView = (ListView) findViewById(R.id.list);
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        int position = ((AdapterContextMenuInfo) menuInfo).position;
        HashMap<String, Object> selectedMap = (HashMap<String, Object>) listView.getAdapter().getItem(position);

        callActivity(item.getItemId(), selectedMap);

        return true;
    }

    private String getPatientInfo(HashMap<String, Object> map) {
        String patientinfo
                = (String) map.get("pnm") + " "
                + (String) map.get("psexage") + " "
                + (String) map.get("dptcd") + " "
                + (String) map.get("ward") + " "
                + super.getFormattedDate((String) map.get("bededt")) + "~"
                + super.getFormattedDate((String) map.get("bedodt"));
        return patientinfo;
    }

    private void callActivity(int idx, HashMap<String, Object> selectedMap) {
        if (selectedMap == null) return;
        Intent intent = null;
        String pid = (String) selectedMap.get("pid");
        String bededt = (String) selectedMap.get("bededt");
        String bedodt = (String) selectedMap.get("bedodt");
        String dptcd = (String) selectedMap.get("dptcd");
        String bdiv = "2"; // 1.외래 2.입원 3.응급
        if (mTabHost.getCurrentTabTag().equals(OUT_PATIENT_LIST)) {
            bdiv = "1";
        } else if (mTabHost.getCurrentTabTag().equals(ER_PATIENT_LIST)) {
            bdiv = "3";
        }
        String patientinfo = getPatientInfo(selectedMap);

        switch (idx) {
            case 1: // 처방조회
            case 2: // 기록지조회
            case 3: // 투약기록지조회
            case 4: // 영상진단결과조회
            case 5: // 전단검사결과조회
            case 6: // 기타서식조회
                intent = new Intent(this, Order.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("bdiv", bdiv);
                intent.putExtra("fromtitle", getTitle());
                intent.putExtra("inittype", idx);
                startActivity(intent);
                break;
//    	case 2: // 진단검사결과조회
//    		intent = new Intent(this,ResultLis.class);
//    		intent.putExtra("pid", pid);
//    		intent.putExtra("bededt", bededt);
//    		intent.putExtra("fromtitle", getTitle());
//    		startActivity(intent);
//    		break;
//    	case 3: // 방사선 판독소견
//    		intent = new Intent(this,ResultRad.class);
//    		intent.putExtra("pid", pid);
//    		intent.putExtra("bededt", bededt);
//    		intent.putExtra("fromtitle", getTitle());
//    		startActivity(intent);
//    		break;
//    	case 4: // 기능검사판독소견
//    		break;
//    	case 5: // 기타서식조회
//    		intent = new Intent(this,EmrScan.class);
//    		intent.putExtra("pid", pid);
//    		intent.putExtra("bededt", bededt);
//    		intent.putExtra("fromtitle", getTitle());
//    		startActivity(intent);
//    		break;
            case 7: // TPR 조회
                intent = new Intent(this, TprSheet.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("bdiv", bdiv);
                intent.putExtra("fromtitle", getTitle());
                startActivity(intent);
                break;
            case 8: // 동의서
                intent = new Intent(this, ConsentFormList.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("bdiv", bdiv);
                intent.putExtra("fromtitle", getTitle());
                startActivity(intent);
                break;
            case 9: // 동의서열람
                intent = new Intent(this, SignedConsentFormList.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("bdiv", bdiv);
                intent.putExtra("fromtitle", getTitle());
                startActivity(intent);
                break;
            case 10: // 입내원이력
    		/* 이곳에 들어오지는 않는다.
    		intent = new Intent(this,PatientHosHx.class);
    		intent.putExtra("pid", pid);
    		intent.putExtra("bededt", bededt);
    		intent.putExtra("patientinfo", patientinfo);
    		intent.putExtra("bdiv", bdiv);
    		intent.putExtra("fromtitle", getTitle());
    		startActivity(intent);
    		*/
                break;
        }

    }

    private String getMode() {
        String tag = mTabHost.getCurrentTabTag();
        String mode = "2";
        if (tag.equals(IN_PATIENT_LIST)) {
            mode = "2";
        } else if (tag.equals(OUT_PATIENT_LIST)) {
            mode = "3";
        } else if (tag.equals(ER_PATIENT_LIST)) {
            mode = "4";
        }
        return mode;
    }

    private String getXmlString() {
        String xml = "";
        if (mTabHost.getCurrentTabTag().equals(IN_PATIENT_LIST)) {
            xml = mXmlIn;
        } else if (mTabHost.getCurrentTabTag().equals(OUT_PATIENT_LIST)) {
            xml = mXmlOut;
        } else {
            xml = mXmlEr;
        }
        if (xml == null) xml = ""; // 혹시나...
        return xml;
    }

    private void getHosHx() {
        if (mPid.equals("") || mBededt.equals("")) return;

        mDialog = ProgressDialog.show(PatientHosHx.this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String mode = getMode();
                String url = "";
                // 환자정보
            	/*
            	if(mXmlPatientInfo==null) mXmlPatientInfo="";
            	if(mXmlPatientInfo.equals("")) {
            		url = "InPatientInformationServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt;
    		    	mXmlPatientInfo = getXml(url);
            	}
            	*/
                // 입내원이력
                url = "InPatientListServlet?hospitalid=" + hospitalId +
                        "&mode=" + mode +
                        "&pid=" + mPid +
                        "&bededt=" + mBededt;
                if (mode.equals("2")) {
                    mXmlIn = getXml(url);
                } else if (mode.equals("3")) {
                    mXmlOut = getXml(url);
                } else {
                    mXmlEr = getXml(url);
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            afterGetHosHx();
                        } catch (Exception e) {
                            Log.d("EmrDroid", "dialog.dismiss exception");
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetHosHx() {
        String mode = getMode();
        //((TextView)findViewById(R.id.patientInfoTextView)).setText(mXmlPatientInfo);

        ResultSetHelper rs;

        ListView list = (ListView) findViewById(R.id.list);

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        // xml해부
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            // 리스트 지움.
            list.setAdapter(null);
            // 조회결과값
            String xml = getXmlString();
            if (xml.equals("")) return;
            // xml to ResultSet
            rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                for (int i = 0; i < rs.getRecordCount(); i++) {

                    map = new HashMap<String, Object>();
                    String psex = rs.getString(i, "psex");
                    map.put("image", psex.equals("M") ? R.drawable.man_icon : R.drawable.woman_icon);
                    map.put("pnm", rs.getString(i, "pnm"));
                    map.put("psexage", rs.getString(i, "psex") + "/" + rs.getString(i, "age"));
                    map.put("dptcd", rs.getString(i, "dptcd"));
                    map.put("ward", rs.getString(i, "ward"));
                    map.put("pdrnm", rs.getString(i, "pdrnm"));
                    map.put("pid", rs.getString(i, "pid"));
                    map.put("qfycd", rs.getString(i, "qfycd"));
                    map.put("qfycdnm", rs.getString(i, "qfycdnm"));
                    map.put("dxd", rs.getString(i, "dxd"));
                    //
                    String bededt = rs.getString(i, "bededt");
                    String bedodt = rs.getString(i, "bedodt");
                    map.put("bededt", bededt);
                    map.put("bedodt", bedodt);
                    //
                    String disp_bededt = super.getFormattedDate(bededt);
                    String disp_bedodt = super.getFormattedDate(bedodt);
                    String disp_bededt_bedodt = "";
                    if (mode.equals("2")) {
                        if (bedodt.equals("")) disp_bedodt = "재원중";
                        disp_bededt_bedodt = disp_bededt + "~" + disp_bedodt;
                    } else {
                        disp_bededt_bedodt = disp_bededt;
                    }
                    map.put("disp_bededt_bedodt", disp_bededt_bedodt);
                    //
                    mylist.add(map);
                }
                SimpleAdapter adapter;
                adapter = new SimpleAdapter(this, mylist, R.layout.patient_hos_hx_row,
                        new String[]{"dptcd", "ward", "pdrnm", "qfycdnm", "disp_bededt_bedodt", "dxd"},
                        new int[]{R.id.patient_list_row_dptcd
                                , R.id.patient_list_row_ward
                                , R.id.patient_list_row_pdrnm
                                , R.id.patient_list_row_qfycdnm
                                , R.id.patient_list_row_disp_bededt_bedodt
                                , R.id.patient_list_row_dxd
                        });
                list.setAdapter(adapter);
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    @Override
    public void onTabChanged(String tabId) {
        // TODO Auto-generated method stub
        if (mXmlIn == null) mXmlIn = "";
        if (mXmlOut == null) mXmlOut = "";
        if (mXmlEr == null) mXmlEr = "";
        if (tabId.equals(IN_PATIENT_LIST) && mXmlIn.equals("")) {
            getHosHx();
        } else if (tabId.equals(OUT_PATIENT_LIST) && mXmlOut.equals("")) {
            getHosHx();
        } else if (tabId.equals(ER_PATIENT_LIST) && mXmlEr.equals("")) {
            getHosHx();
        } else {
            afterGetHosHx();
        }
    }

    private void DisplayPatientInfo() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                mPatientInfoTextView.setText(mXmlPatientInfo);
            }
        });
    }
}
