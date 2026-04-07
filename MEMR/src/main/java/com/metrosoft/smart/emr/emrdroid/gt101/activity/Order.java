package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.opengl.Visibility;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.ChartAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.EmrScanAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.MedRecordAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.NrChartAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.OrderAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.ResultLisAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.ResultLisSpcnoAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.ResultRadAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.ResultSpeAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.DateUtil;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import org.json.JSONException;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


public class Order extends MyActivity implements TabHost.OnTabChangeListener {
    private static final int REQ_NR_CHART_WRITE = 2001;

    private final String ORDER_LIST = "0";
    private final String CHART_LIST = "1";
    private final String NR_CHART_LIST="2";
    private final String RESULT_RAD_LIST = "3";
    private final String RESULT_LIS_LIST = "4";
    //private final String RESULT_LIS_LIST_SPCNO = "5";
    private final String EMR_SCAN_LIST = "6";
    private final String MED_RECORD_LIST = "7";
    private final String RESULT_SPE_LIST = "8";

    static final int FR_DATE_DIALOG_ID = 0;
    static final int TO_DATE_DIALOG_ID = 1;

    private boolean mQueryOrder = false;
    private boolean mQueryChart = false;
    private boolean mQueryNrChart = false;
    private boolean mQueryResultRad = false;
    private boolean mQueryResultSpe = false;
    private boolean mQueryResultLis = false;
    private boolean mQueryEmrScan = false;
    private boolean mQueryMedRecord = false;

    private String mPid;
    private String mBededt;
    private String mBedodt;
    private String mBdiv;
    private int mInitType;
    private String mDrid;
    private String mXmlPatientInfo;
    private String mXmlOrder;
    private String mXmlChart;
    private String mXmlNrChart;
    private String mXmlOrderRad;
    private String mXmlOrderSpe;
    private String mXmlResultRad;
    private String mXmlResultSpe;
    private String mXmlResultLis;
    //private String mXmlResultLisSpcno;
    private String mXmlEmrScan;
    //private String mXmlMedRecordMinMaxDodt;
    private String mXmlMedRecord;
    private String mMedRecordMinDodt;
    private String mMedRecordMaxDodt;
    private long mMedRecordLeftCol;
    private long mMedRecordColCount;
    //private String mSpcno;
    private String mXmlExdtLate6; // 2021.08.11 WOOIL - 외래 최근 6 내원일을 가져오기 위한 변수

    private int mFrYear, mFrMonth, mFrDay;
    private int mToYear, mToMonth, mToDay;

    private TabHost mTabHost;
    private Button mFrDateButton;
    private Button mToDateButton;
    //private int mSelectedPostion;
    //private HashMap<String, Object> mSelectedMap;

    private TextView mPatientInfoTextView;
    private ListView mOrderList;
    private ListView mChartList;
    private ListView mNrChartList;
    private ExpandableListView mResultRadListOrder;
    private ExpandableListView mResultSpeListOrder; // 2024.03.05 WOOIL - 기능검사
    //private ListView mSpcnoList;
    private ListView mResultLisList;
    private ListView mMedRecordList;
    private ListView mEmrScanList;

    private TextView mOrderListNoData; // 처방이 없는 경우 보여지는 텍스트
    private TextView mChartListNoData; // 기록지가 없는 경우 보여지는 텍스트
    private TextView mNrChartListNoData; // 간호기록지가 없는 경우 보여지는 텍스트
    private TextView mResultRadListOrderNoData; // 방사선 결과가 없는 경우 보여지는 텍스트
    private TextView mResultSpeListOrderNoData; // 기능검사 결과가 없는 경우 보여지는 텍스트
    private TextView mResultLisListNoData; // 임상별리 결과가 없는 경우 보여지는 텍스트
    private TextView mEmrScanListNoData; // 기타서식이 없는 경우 보여지는 텍스트
    private TextView mMedRecordListNoData; // 투약기록지가 없는 경우 보여지는 텍스트

    private ArrayList<HashMap<String, Object>> mEmrScanListArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mBedodt = intent.getStringExtra("bedodt");
        mXmlPatientInfo = intent.getStringExtra("patientinfo");
        mBdiv = intent.getStringExtra("bdiv");
        mInitType = intent.getIntExtra("inittype", 1);
        mDrid = intent.getStringExtra("drid");

        // 오류방지용
        if (mPid == null) mPid = "";
        if (mBededt == null) mBededt = "";
        if (mBedodt == null) mBedodt = "";
        if (mBdiv == null) mBdiv = "2"; // 1.외래 2.입원 3.응급  기본 입원

        String fromTitle = intent.getStringExtra("fromtitle");
        if (fromTitle == null) fromTitle = "";
        if (fromTitle.equals("")) fromTitle = "닫기";

        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.order, fromTitle);

        super.setLinkButton1(true, "TPR");
        super.setLinkButton2(true, "DM");
        super.setLinkButton3(true, "IO");

        // 입원환자가 아니면 사용하지 못하게 한다.
        if (!mBdiv.equals("2")) {
            super.setLinkButton1(false, "TPR");
            super.setLinkButton2(false, "DM");
            super.setLinkButton3(false, "IO");
        }

        //// 제목표시줄 밑에 있는 TEMR 로그를 TEMR 페키지만 보이도록 처리
        //RelativeLayout topBgLayout = (RelativeLayout)findViewById(R.id.top_bg_layout);
        //if(!packageName.equals(EmrSettingsUtil.PACKAGE_TEMR)){
        //	topBgLayout.setVisibility(View.GONE);
        //}

        // -----------------------------------------------------
        // 환자정보
        // -----------------------------------------------------
        mPatientInfoTextView = (TextView) findViewById(R.id.patientInfoTextView);
        DisplayPatientInfo();
        // -----------------------------------------------------
        // 탭생성
        // -----------------------------------------------------
        initTab();
        // -----------------------------------------------------
        // 일자버튼의 이벤트연결
        // 외래,응급환자는 조회기간을 선택할 수 없다.
        // -----------------------------------------------------
        mFrDateButton = (Button) findViewById(R.id.pickFrDate);
        mFrDateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(FR_DATE_DIALOG_ID);
            }
        });
        mToDateButton = (Button) findViewById(R.id.pickToDate);
        mToDateButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                showDialog(TO_DATE_DIALOG_ID);
            }
        });
        // -----------------------------------------------------
        // 처방 이벤트 연결
        // -----------------------------------------------------
        mOrderList = (ListView) findViewById(R.id.order_list);
        // -----------------------------------------------------
        // 차트 이벤트 연결
        // -----------------------------------------------------
        mChartList = (ListView) findViewById(R.id.chart_list);
        // -----------------------------------------------------
        // 간호기록지 이벤트 연결
        // -----------------------------------------------------
        mNrChartList = (ListView) findViewById(R.id.nr_chart_list);
        mNrChartList.setOnItemLongClickListener(new GridView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                final HashMap<String, Object> selectedMap =
                        (HashMap<String, Object>) parent.getAdapter().getItem(position);

                final String wdate = selectedMap.get("wdate") == null ? "" : selectedMap.get("wdate").toString();
                final String seq = selectedMap.get("seq") == null ? "" : selectedMap.get("seq").toString();
                final String wtime = selectedMap.get("wtime") == null ? "" : selectedMap.get("wtime").toString();
                final String result = selectedMap.get("result") == null ? "" : selectedMap.get("result").toString();
                final String empid = selectedMap.get("empid") == null ? "" : selectedMap.get("empid").toString();
                final String empnm = selectedMap.get("empnm") == null ? "" : selectedMap.get("empnm").toString();

                PopupMenu menu = new PopupMenu(Order.this, view);
                if ("".equalsIgnoreCase(wdate) && "".equalsIgnoreCase(wtime) && "".equalsIgnoreCase(seq)) {
                    // 자료가 없는 경우 신규등록만 가능
                    menu.getMenu().add(0, 2, 1, "신규등록");
                } else {
                    menu.getMenu().add(0, 1, 0, "수정");
                    menu.getMenu().add(0, 2, 1, "신규등록");
                }

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {

                        if (item.getItemId() == 1) {
                            // 수정
                            if (empid.equalsIgnoreCase(getUserId()) == false){
                                showSimpleDialog("작성자만 수정 가능합니다.");
                            } else {
                                Intent intent = new Intent(Order.this, NrChartWrite.class);
                                intent.putExtra("pid", mPid);
                                intent.putExtra("bededt", mBededt);
                                intent.putExtra("bdiv", mBdiv);
                                intent.putExtra("drid", mDrid);
                                intent.putExtra("mode", "U");
                                intent.putExtra("wdate", wdate);
                                intent.putExtra("seq", seq);
                                intent.putExtra("wtime", wtime);
                                intent.putExtra("result", result);
                                intent.putExtra("empid", getUserId());
                                startActivityForResult(intent, REQ_NR_CHART_WRITE);
                            }
                            return true;
                        } else if (item.getItemId() == 2) {
                            // 신규등록
                            Intent intent = new Intent(Order.this, NrChartWrite.class);
                            intent.putExtra("pid", mPid);
                            intent.putExtra("bededt", mBededt);
                            intent.putExtra("bdiv", mBdiv);
                            intent.putExtra("drid", mDrid);
                            intent.putExtra("mode", "I");
                            intent.putExtra("wdate", "");
                            intent.putExtra("seq", "");
                            intent.putExtra("wtime", "");
                            intent.putExtra("result", "");
                            intent.putExtra("empid", getUserId());
                            startActivityForResult(intent, REQ_NR_CHART_WRITE);
                            return true;
                        }

                        return false;
                    }
                });

                menu.show();
                return true;
            }
        });
        // -----------------------------------------------------
        // 방사선처방리스트의 이벤트 연결
        // -----------------------------------------------------
        mResultRadListOrder = (ExpandableListView) findViewById(R.id.result_rad_list_order);
        mResultRadListOrder.setGroupIndicator(null);
        mResultRadListOrder.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                // TODO Auto-generated method stub
                ResultRadAdapter adapter = (ResultRadAdapter) parent.getExpandableListAdapter();
                HashMap<String, Object> selectedMap = (HashMap<String, Object>) (adapter.getGroup(groupPosition));

                String odt = selectedMap.get("odt").toString();
                String ono = selectedMap.get("ono").toString();
                String bdiv = selectedMap.get("bdiv").toString();

                if (parent.isGroupExpanded(groupPosition) == false) {
                    getResultRad(groupPosition, odt, ono, bdiv);
                }
                return false;
            }
        });
        // -----------------------------------------------------
        // 기능검사처방리스트의 이벤트 연결
        // -----------------------------------------------------
        mResultSpeListOrder = (ExpandableListView) findViewById(R.id.result_spe_list_order);
        mResultSpeListOrder.setGroupIndicator(null);
        mResultSpeListOrder.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                // TODO Auto-generated method stub
                ResultSpeAdapter adapter = (ResultSpeAdapter) parent.getExpandableListAdapter();
                HashMap<String, Object> selectedMap = (HashMap<String, Object>) (adapter.getGroup(groupPosition));

                String odt = selectedMap.get("odt").toString();
                String ono = selectedMap.get("ono").toString();
                String bdiv = selectedMap.get("bdiv").toString();

                if (parent.isGroupExpanded(groupPosition) == false) {
                    getResultSpe(groupPosition, odt, ono, bdiv);
                }
                return false;
            }
        });
        // 임상병리 결과
        mResultLisList = (ListView) findViewById(R.id.result_lis_list);
        // -----------------------------------------------------
        // 임상병리 검체번호 리스트의 이벤트 연결
        // -----------------------------------------------------
        /*
        mSpcnoList = (ListView) findViewById(R.id.result_lis_list_spcno);
        mSpcnoList.setOnItemClickListener(new GridView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> selectedMap = (HashMap<String, Object>) parent.getAdapter().getItem(position);
                mSpcno = (String) selectedMap.get("spcno");
                getLisResultBySpcno();
            }
        });
        */
        // -----------------------------------------------------
        // 기타서식 상세보기 기능
        // -----------------------------------------------------
        mEmrScanList = (ListView) findViewById(R.id.emr_scan_list);
        mEmrScanList.setOnItemClickListener(new GridView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> selectedMap = (HashMap<String, Object>) parent.getAdapter().getItem(position);
                Intent intent = new Intent(Order.this, EmrScanView.class);
                intent.putExtra("pid", mPid);
                intent.putExtra("bededt", mBededt);
                intent.putExtra("bdiv", (String) selectedMap.get("bdiv"));
                intent.putExtra("exdt", (String) selectedMap.get("exdt"));
                intent.putExtra("seq", (String) selectedMap.get("seq"));
                intent.putExtra("rptcd", (String) selectedMap.get("rptcd"));
                intent.putExtra("path", (String) selectedMap.get("path"));
                intent.putExtra("path2", (String) selectedMap.get("path2"));
                intent.putExtra("patientinfo", mXmlPatientInfo);
                startActivity(intent);
            }
        });
        // -----------------------------------------------------
        // 투약기록지
        // -----------------------------------------------------
        mMedRecordList = (ListView) findViewById(R.id.med_record_list);
        mMedRecordList.setOnTouchListener(new OnTouchListener() {
            // 좌우 스크롤을 위하여
            private float startX = 0;
            private float startY = 0;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // TODO Auto-generated method stub
                float scrollByX = 0;
                float scrollByY = 0;

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 시작점
                        startX = event.getRawX();
                        startY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        // 현재점
                        float x = event.getRawX();
                        float y = event.getRawY();
                        // 움직인 거리
                        scrollByX = x - startX;
                        scrollByY = y - startY;
                        // 어느쪽으로 움직일지
                        if (scrollByX < 0 - 15) {
                            // 화면을 찍어서 왼쪽으로 미는 동작임.
                            // 오른쪽에 있는 자료가 보여지게 처리한다.
                            mMedRecordLeftCol++;
                            if (mMedRecordLeftCol > mMedRecordColCount - 1)
                                mMedRecordLeftCol = mMedRecordColCount - 1;
                            displayMedRecordColHeader();
                            ((MedRecordAdapter) mMedRecordList.getAdapter()).setLeftPosition(mMedRecordLeftCol);
                        } else if (scrollByX > 0 + 15) {
                            // 화면을 찍어서 오른쪽으로 미는 동작임.
                            // 왼쪽에 있는 자료가 보여지게 처리한다.
                            mMedRecordLeftCol--;
                            if (mMedRecordLeftCol < 0) mMedRecordLeftCol = 0;
                            displayMedRecordColHeader();
                            ((MedRecordAdapter) mMedRecordList.getAdapter()).setLeftPosition(mMedRecordLeftCol);
                        }
                        // 다시 시작
                        startX = x;
                        startY = y;
                        // 다시 그리기
                        //invalidate();
                        break;
                }

                return false; // true 를 return 하면 상하스크롤이 안된다.
            }
        });

        mOrderListNoData = (TextView) findViewById(R.id.order_list_no_data);
        mChartListNoData = (TextView) findViewById(R.id.chart_list_no_data);
        mNrChartListNoData = (TextView) findViewById(R.id.nr_chart_list_no_data);
        mResultRadListOrderNoData = (TextView) findViewById(R.id.result_rad_list_order_no_data);
        mResultSpeListOrderNoData = (TextView) findViewById(R.id.result_spe_list_order_no_data);
        mResultLisListNoData = (TextView) findViewById(R.id.result_lis_list_no_data);
        mEmrScanListNoData = (TextView) findViewById(R.id.emr_scan_list_no_data);
        mMedRecordListNoData = (TextView) findViewById(R.id.med_record_list_no_data);

        mOrderListNoData.setVisibility(View.GONE);
        mChartListNoData.setVisibility(View.GONE);
        mNrChartListNoData.setVisibility(View.GONE);
        mResultRadListOrderNoData.setVisibility(View.GONE);
        mResultSpeListOrderNoData.setVisibility(View.GONE);
        mResultLisListNoData.setVisibility(View.GONE);
        mEmrScanListNoData.setVisibility(View.GONE);
        mMedRecordListNoData.setVisibility(View.GONE);

        // 간호기록 신규등록
        mNrChartListNoData.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                PopupMenu menu = new PopupMenu(Order.this, v);
                menu.getMenu().add(0, 2, 0, "신규등록");

                menu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == 2) {
                            Intent intent = new Intent(Order.this, NrChartWrite.class);
                            intent.putExtra("pid", mPid);
                            intent.putExtra("bededt", mBededt);
                            intent.putExtra("bdiv", mBdiv);
                            intent.putExtra("drid", mDrid);
                            intent.putExtra("mode", "I");
                            intent.putExtra("wdate", "");
                            intent.putExtra("seq", "");
                            intent.putExtra("wtime", "");
                            intent.putExtra("result", "");
                            intent.putExtra("empid", getUserId());
                            startActivityForResult(intent, REQ_NR_CHART_WRITE);
                            return true;
                        }
                        return false;
                    }
                });

                menu.show();
                return true;
            }
        });


        // 각 탭별 조회여부, 탭 이동시 최초 한번은 자동조회를 한다.
        mQueryOrder = false;
        mQueryChart = false;
        mQueryNrChart = false;
        mQueryResultRad = false;
        mQueryResultSpe = false;
        mQueryResultLis = false;
        mQueryEmrScan = false;
        mQueryMedRecord = false;

        // 기본탭설정
        if (savedInstanceState == null) {
            // 조회기간 초기화
            initFrToDate();
        } else {
            mXmlPatientInfo = savedInstanceState.getString("xmlPatientInfo");
            mXmlOrder = savedInstanceState.getString("xmlOrder");
            mFrYear = savedInstanceState.getInt("frYear");
            mFrMonth = savedInstanceState.getInt("frMonth");
            mFrDay = savedInstanceState.getInt("frDay");
            mToYear = savedInstanceState.getInt("toYear");
            mToMonth = savedInstanceState.getInt("toMonth");
            mToDay = savedInstanceState.getInt("toDay");
            mXmlChart = savedInstanceState.getString("mXmlChart");
            mXmlOrderRad = savedInstanceState.getString("mXmlOrderRad");
            mXmlOrderSpe = savedInstanceState.getString("mXmlOrderSpe");
            mXmlResultRad = savedInstanceState.getString("mXmlResultRad");
            mXmlResultSpe = savedInstanceState.getString("mXmlResultSpe");
            mXmlResultLis = savedInstanceState.getString("mXmlResultLis");
            //mXmlResultLisSpcno = savedInstanceState.getString("mXmlResultLisSpcno");
            mXmlEmrScan = savedInstanceState.getString("mXmlEmrScan");
            //mXmlMedRecordMinMaxDodt = savedInstanceState.getString("mXmlMedRecordMinMaxDodt");
            mXmlMedRecord = savedInstanceState.getString("mXmlMedRecord");
            mMedRecordMinDodt = savedInstanceState.getString("mMedRecordMinDodt");
            mMedRecordMaxDodt = savedInstanceState.getString("mMedRecordMaxDodt");
            mMedRecordLeftCol = savedInstanceState.getLong("mMedRecordLeftCol");
            mMedRecordColCount = savedInstanceState.getLong("mMedRecordColCount");
            mQueryOrder = savedInstanceState.getBoolean("mQueryOrder", false);
            mQueryChart = savedInstanceState.getBoolean("mQueryChart", false);
            mQueryNrChart = savedInstanceState.getBoolean("mQueryNrChart", false);
            mQueryResultRad = savedInstanceState.getBoolean("mQueryResultRad", false);
            mQueryResultSpe = savedInstanceState.getBoolean("mQueryResultSpe", false);
            mQueryResultLis = savedInstanceState.getBoolean("mQueryResultLis", false);
            mQueryEmrScan = savedInstanceState.getBoolean("mQueryEmrScan", false);
            mQueryMedRecord = savedInstanceState.getBoolean("mQueryMedRecord", false);
            int currentTab = savedInstanceState.getInt("currentTab");
            mTabHost.setCurrentTab(currentTab);
            displayFrDate();
            displayToDate();
            // 화면에 다시 출력
            afterGet();
            //afterGetOrder();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("xmlPatientInfo", mXmlPatientInfo);
        outState.putString("xmlOrder", mXmlOrder);
        outState.putInt("frYear", mFrYear);
        outState.putInt("frMonth", mFrMonth);
        outState.putInt("frDay", mFrDay);
        outState.putInt("toYear", mToYear);
        outState.putInt("toMonth", mToMonth);
        outState.putInt("toDay", mToDay);
        outState.putString("mXmlChart", mXmlChart);
        outState.putString("mXmlOrderRad", mXmlOrderRad);
        outState.putString("mXmlOrderSpe", mXmlOrderSpe);
        outState.putString("mXmlResultRad", mXmlResultRad);
        outState.putString("mXmlResultSpe", mXmlResultSpe);
        outState.putString("mXmlResultLis", mXmlResultLis);
        //outState.putString("mXmlResultLisSpcno", mXmlResultLisSpcno);
        outState.putString("mXmlEmrScan", mXmlEmrScan);
        //outState.putString("mXmlMedRecordMinMaxDodt", mXmlMedRecordMinMaxDodt);
        outState.putString("mXmlMedRecord", mXmlMedRecord);
        outState.putString("mMedRecordMinDodt", mMedRecordMinDodt);
        outState.putString("mMedRecordMaxDodt", mMedRecordMaxDodt);
        outState.putLong("mMedRecordLeftCol", mMedRecordLeftCol);
        outState.putLong("mMedRecordColCount", mMedRecordColCount);
        outState.putBoolean("mQueryOrder", mQueryOrder);
        outState.putBoolean("mQueryChart", mQueryChart);
        outState.putBoolean("mQueryNrChart", mQueryNrChart);
        outState.putBoolean("mQueryResultRad", mQueryResultRad);
        outState.putBoolean("mQueryResultSpe", mQueryResultSpe);
        outState.putBoolean("mQueryResultLis", mQueryResultLis);
        outState.putBoolean("mQueryEmrScan", mQueryEmrScan);
        outState.putBoolean("mQueryMedRecord", mQueryMedRecord);
        outState.putInt("currentTab", mTabHost.getCurrentTab());
    }

    @Override
    public void onClickQueryButton(View v) {
        // 조회버튼 클릭시
        getOrder();
    }

    @Override
    public void onClickLinkButton1(View v) {
        Intent intent;
        intent = new Intent(Order.this, TprSheet.class);
        intent.putExtra("pid", mPid);
        intent.putExtra("bededt", mBededt);
        intent.putExtra("bdiv", mBdiv);
        intent.putExtra("patientinfo", mXmlPatientInfo);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClickLinkButton2(View v) {
        Intent intent;
        intent = new Intent(Order.this, DmSheet.class);
        intent.putExtra("pid", mPid);
        intent.putExtra("bededt", mBededt);
        intent.putExtra("bdiv", mBdiv);
        intent.putExtra("patientinfo", mXmlPatientInfo);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClickLinkButton3(View v) {
        Intent intent;
        intent = new Intent(Order.this, IoSheet.class);
        intent.putExtra("pid", mPid);
        intent.putExtra("bededt", mBededt);
        intent.putExtra("bdiv", mBdiv);
        intent.putExtra("patientinfo", mXmlPatientInfo);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_NR_CHART_WRITE) {
            if (resultCode == RESULT_OK && data != null) {
                getOrder();
            }
        }
    }
    private void initTab() {
        // -----------------------------------------------------
        // 탭생성
        // -----------------------------------------------------
        mTabHost = (TabHost) findViewById(R.id.tabHost);
        // findViewById를 이용해 TabHost인스턴스를 얻은경우 꼭 호출 필요
        mTabHost.setup();
        // Tab builder 객체
        TabHost.TabSpec spec;
        // 처방
        spec = mTabHost.newTabSpec(ORDER_LIST);      // Tab Builder 객체
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(ORDER_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_order);              // Tab 내용
        mTabHost.addTab(spec);                        // Tab 등록
        // 기록지
        spec = mTabHost.newTabSpec(CHART_LIST);     // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(CHART_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_chart);              // Tab 내용
        mTabHost.addTab(spec);                        // Tab 등록
        // 간호 기록지
        spec = mTabHost.newTabSpec(NR_CHART_LIST);     // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(NR_CHART_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_nr_chart);            // Tab 내용
        mTabHost.addTab(spec);                        // Tab 등록
        // 투약기록지
        spec = mTabHost.newTabSpec(MED_RECORD_LIST);        // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(MED_RECORD_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_med_record);        // Tab 내용
        mTabHost.addTab(spec);                        // Tab 등록
        // 영상진단결과
        spec = mTabHost.newTabSpec(RESULT_RAD_LIST);    // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(RESULT_RAD_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_result_rad);            // Tab 내용
        mTabHost.addTab(spec);                            // Tab 등록
        // 기능검사결과
        spec = mTabHost.newTabSpec(RESULT_SPE_LIST);    // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(RESULT_SPE_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_result_spe);            // Tab 내용
        mTabHost.addTab(spec);                            // Tab 등록
        // 진단검사단결과
        spec = mTabHost.newTabSpec(RESULT_LIS_LIST);    // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(RESULT_LIS_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_result_lis);
        mTabHost.addTab(spec);                            // Tab 등록
//        // 진단검사 검체리스트
//        spec = mTabHost.newTabSpec(RESULT_LIS_LIST_SPCNO);	// Tab Builder 객체 생성
//        spec.setIndicator(createTabIndicator(mTabHost.getContext(),getTabText(RESULT_LIS_LIST)));	// Tab 제목
//    	spec.setContent(R.id.tab_result_lis_spcno);
//        mTabHost.addTab(spec);							// Tab 등록
        // 기타서식
        spec = mTabHost.newTabSpec(EMR_SCAN_LIST);        // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(EMR_SCAN_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_emr_scan);                // Tab 내용
        mTabHost.addTab(spec);                            // Tab 등록
        // 처음 등록된 Tab을 보여줌.
        if (mInitType >= 1 && mInitType <= 8) {
            mTabHost.setCurrentTab(mInitType - 1);
        } else {
            mTabHost.setCurrentTab(0);
        }
        // 현재 선택된 탭의 제목을 화면의 제목으로 바꾼다.
        setMyTitle(getTabText(mTabHost.getCurrentTabTag()) + "조회");
        // 리스터연결
        mTabHost.setOnTabChangedListener(this);
        //
        // 2025.02.07 WOOIL - 검체리스트를 조회하지 않고 결과를 바로 보이도록 수정
        //if (EmrSettingsUtil.getEmrCompany(getBaseContext()).equalsIgnoreCase(EmrSettingsUtil.EMR_COMPANY_METROSOFT)) {
        //    // 메트로소프트 사이트
        //} else {
        //    // 메트로소프트 사이트가 아니면 검체번호 리스트가 안보이게처리
        //    LinearLayout layout = (LinearLayout) findViewById(R.id.spcno_layout);
        //    layout.setVisibility(View.GONE);
        //}
    }

    private void initFrToDate() {
        // 2021.08.11 WOOIL - 외래인 경우 조회시작일을 최근 방문 6일 전부터 조회되도록 일자를 설정한다.
        //                    즉.  SELECT TOP 6 EXDT FROM TS21 ... ORDER BY EXDT DESC

        if (mBdiv.equals("2")) {

            // 입원은 이전 화면에서 넘어온 값을 그대로 사용한다.

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

            // 일자를 화면에 출력
            displayFrDate();
            displayToDate();
            // 조회
            getOrder();


        } else {

            // 외래는 값을 가져온다. 최근 6번 내원일중 가장 작은 값을 가져온다.

            mDialog = ProgressDialog.show(Order.this, "", getString(R.string.query_wait_message), true);
            new Thread(new Runnable() {
                public void run() {
                    String hospitalId = getHospitalId();
                    String userId = getUserId();
                    String mode = "11";

                    String url = "ChartServlet?hospitalid=" + hospitalId + "&mode=" + mode + "&pid=" + mPid + "&exdt=" + mBededt;
                    mXmlExdtLate6 = getXml(url);

                    mHandler.post(new Runnable() {
                        public void run() {
                            // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                            // 이를 방지함.
                            try {
                                mDialog.dismiss();
                                afterInitFrToDate();
                            } catch (Exception e) {
                                Log.d("EmrDroid", "dialog.dismiss exception");
                                Log.d("EmrDroid", e.getMessage());
                            }
                        }
                    });
                }
            }).start();

        }
    }

    private void afterInitFrToDate() {
        String frdt = mBededt;
        if (mXmlExdtLate6.equals("")) {
            // 서버에서 값을 반환하지 못했다.
        } else {
            String exdt_late_6 = "";
            try {
                ResultSetHelper rs = new ResultSetHelper(mXmlExdtLate6, EmrSettingsUtil.getMaskYn(getBaseContext()));
                if (rs.getReturnCode() < 0) {
                    // 오류발생
                    //showSimpleDialog(rs.getReturnDesc());
                } else if (rs.getReturnCode() == 0) {
                    // 자료가 없음. 오늘이 최초 내원일임.
                } else {
                    exdt_late_6 = rs.getString(0, "exdt_late_6");
                }
            } catch (JSONException e) {
            }

            if (exdt_late_6.equals("")) {
                // 자료가 없음. 오늘이 최초 내원일임.
            } else {
                frdt = exdt_late_6;
            }
        }

        // 시자일
        mFrYear = Integer.parseInt(frdt.substring(0, 4));
        mFrMonth = Integer.parseInt(frdt.substring(4, 6)) - 1;
        mFrDay = Integer.parseInt(frdt.substring(6, 8));
        // 종료일
        mToYear = Integer.parseInt(mBededt.substring(0, 4));
        mToMonth = Integer.parseInt(mBededt.substring(4, 6)) - 1;
        mToDay = Integer.parseInt(mBededt.substring(6, 8));

        // 일자를 화면에 출력
        displayFrDate();
        displayToDate();
        // 조회
        getOrder();

    }

    private void displayFrDate() {
        mFrDateButton.setText(super.getFormattedDate(getFrDate()));
    }

    private void displayToDate() {
        //String frDate=getFrDate();
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

    private DatePickerDialog.OnDateSetListener mFrDateSetListener =
            new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    mFrYear = year;
                    mFrMonth = monthOfYear;
                    mFrDay = dayOfMonth;
                    displayFrDate();
                }
            };
    private DatePickerDialog.OnDateSetListener mToDateSetListener =
            new DatePickerDialog.OnDateSetListener() {
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    mToYear = year;
                    mToMonth = monthOfYear;
                    mToDay = dayOfMonth;
                    displayToDate();
                }
            };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case FR_DATE_DIALOG_ID:
                return new DatePickerDialog(this,
                        mFrDateSetListener,
                        mFrYear, mFrMonth, mFrDay);
            case TO_DATE_DIALOG_ID:
                return new DatePickerDialog(this,
                        mToDateSetListener,
                        mToYear, mToMonth, mToDay);
        }
        return null;
    }

    private void invisibleAllTabContent() {
        LinearLayout linearLayout;
        linearLayout = (LinearLayout) findViewById(R.id.tab_order);
        linearLayout.setVisibility(View.INVISIBLE);
        linearLayout = (LinearLayout) findViewById(R.id.tab_chart);
        linearLayout.setVisibility(View.INVISIBLE);
        linearLayout = (LinearLayout) findViewById(R.id.tab_nr_chart);
        linearLayout.setVisibility(View.INVISIBLE);
        linearLayout = (LinearLayout) findViewById(R.id.tab_result_rad);
        linearLayout.setVisibility(View.INVISIBLE);
        linearLayout = (LinearLayout) findViewById(R.id.tab_result_spe);
        linearLayout.setVisibility(View.INVISIBLE);
        linearLayout = (LinearLayout) findViewById(R.id.tab_result_lis);
        linearLayout.setVisibility(View.INVISIBLE);
        linearLayout = (LinearLayout) findViewById(R.id.tab_emr_scan);
        linearLayout.setVisibility(View.INVISIBLE);
        linearLayout = (LinearLayout) findViewById(R.id.tab_med_record);
        linearLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onTabChanged(String tabId) {
        // TODO Auto-generated method stub
        setMyTitle(getTabText(mTabHost.getCurrentTabTag()) + "조회");
        // 최초1회 조회
        invisibleAllTabContent();
        String tag = mTabHost.getCurrentTabTag();
        if (tag.equals(ORDER_LIST)) {
            if (mQueryOrder == false) {
                getOrder();
            }
            LinearLayout linearLayout;
            linearLayout = (LinearLayout) findViewById(R.id.tab_order);
            linearLayout.setVisibility(View.VISIBLE);
        } else if (tag.equals(CHART_LIST)) {
            if (mQueryChart == false) {
                getOrder();
            }
            LinearLayout linearLayout;
            linearLayout = (LinearLayout) findViewById(R.id.tab_chart);
            linearLayout.setVisibility(View.VISIBLE);
        } else if (tag.equals(NR_CHART_LIST)) {
            if (mQueryNrChart == false) {
                getOrder();
            }
            LinearLayout linearLayout;
            linearLayout = (LinearLayout) findViewById(R.id.tab_nr_chart);
            linearLayout.setVisibility(View.VISIBLE);
        } else if (tag.equals(RESULT_RAD_LIST)) {
            if (mQueryResultRad == false) {
                getOrder();
            }
            LinearLayout linearLayout;
            linearLayout = (LinearLayout) findViewById(R.id.tab_result_rad);
            linearLayout.setVisibility(View.VISIBLE);
        } else if (tag.equals(RESULT_SPE_LIST)) {
            if (mQueryResultSpe == false) {
                getOrder();
            }
            LinearLayout linearLayout;
            linearLayout = (LinearLayout) findViewById(R.id.tab_result_spe);
            linearLayout.setVisibility(View.VISIBLE);
        } else if (tag.equals(RESULT_LIS_LIST)) {
            if (mQueryResultLis == false) {
                getOrder();
            }
            LinearLayout linearLayout;
            linearLayout = (LinearLayout) findViewById(R.id.tab_result_lis);
            linearLayout.setVisibility(View.VISIBLE);
        } else if (tag.equals(EMR_SCAN_LIST)) {
            if (mQueryEmrScan == false) {
                getOrder();
            }
            LinearLayout linearLayout;
            linearLayout = (LinearLayout) findViewById(R.id.tab_emr_scan);
            linearLayout.setVisibility(View.VISIBLE);
        } else if (tag.equals(MED_RECORD_LIST)) {
            if (mQueryMedRecord == false) {
                getOrder();
            }
            LinearLayout linearLayout;
            linearLayout = (LinearLayout) findViewById(R.id.tab_med_record);
            linearLayout.setVisibility(View.VISIBLE);
        }
    }

    private String getTabText(String tag) {
        String tagText = "";
        if (tag.equals(ORDER_LIST)) {
            tagText = "처방";
        } else if (tag.equals(CHART_LIST)) {
            tagText = "기록지";
        } else if (tag.equals(NR_CHART_LIST)) {
            tagText = "간호기록지";
        } else if (tag.equals(RESULT_RAD_LIST)) {
            tagText = "영상진단결과";
        } else if (tag.equals(RESULT_SPE_LIST)) {
            tagText = "기능검사결과";
        } else if (tag.equals(RESULT_LIS_LIST)) {
            tagText = "진단검사결과";
        } else if (tag.equals(EMR_SCAN_LIST)) {
            tagText = "기타서식";
        } else if (tag.equals(MED_RECORD_LIST)) {
            tagText = "투약기록지";
        }
        return tagText;
    }

    // ----------------------------------------------------------------------------------------------------
    // 자료를 읽는다.
    // 2011.11.02 WOOIL - 차트를 읽는 기능 추가
    // 2011.11.03 WOOIL - 영산진단 읽는 기능 추가
    // ----------------------------------------------------------------------------------------------------
    private void getOrder() {
        if (mPid.equals("") || mBededt.equals("")) {
            showSimpleDialog("환자를 선택하세요.");
            return;
        }

        mDialog = ProgressDialog.show(Order.this, "", getString(R.string.query_wait_message), true);
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

                // 조회
                String selectedTab = mTabHost.getCurrentTabTag();
                if (selectedTab.equals(ORDER_LIST)) {
                    // 처방
                    mode = "0";
                    url = "ChartServlet?hospitalid=" + hospitalId + "&mode=" + mode + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv + "&frdt=" + frDate + "&todt=" + toDate;
                    mXmlOrder = getXml(url);
                    mQueryOrder = true;
                } else if (selectedTab.equals(CHART_LIST)) {
                    // 차트
                    // 2021.08.11 WOOIL - bdiv추가
                    mode = "2";
                    url = "ChartServlet?hospitalid=" + hospitalId + "&mode=" + mode + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv + "&frdt=" + frDate + "&todt=" + toDate;
                    mXmlChart = getXml(url);
                    mQueryChart = true;
                } else if (selectedTab.equals(NR_CHART_LIST)) {
                    // 간호기록지
                    mode = "16";
                    url = "ChartServlet?hospitalid=" + hospitalId + "&mode=" + mode + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv + "&frdt=" + frDate + "&todt=" + toDate;
                    mXmlNrChart = getXml(url);
                    mQueryNrChart = true;
                } else if (selectedTab.equals(RESULT_RAD_LIST)) {
                    // 영상진단처방
                    // 2021.08.11 WOOIL - bdiv추가
                    mode = "0";
                    url = "ChartServlet?hospitalid=" + hospitalId + "&mode=" + mode + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv + "&frdt=" + frDate + "&todt=" + toDate + "&radorderyn=Y";
                    mXmlOrderRad = getXml(url);
                    mQueryResultRad = true;
                } else if (selectedTab.equals(RESULT_SPE_LIST)) {
                    // 기능검사처방
                    mode = "0";
                    url = "ChartServlet?hospitalid=" + hospitalId + "&mode=" + mode + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv + "&frdt=" + frDate + "&todt=" + toDate + "&radorderyn=Z";
                    mXmlOrderSpe = getXml(url);
                    mQueryResultSpe = true;
                } else if (selectedTab.equals(RESULT_LIS_LIST)) {
                    // 진단검사결과
                    // 메트로소프트 버전은 검체리스트를 먼저 보여주고 검체번호를 더치하면 결과를 보이게 수정한다.
                    // 2021.08.11 WOOIL - bdiv추가
                    // 2025.02.07 WOOIL - 검체리스트를 조회하지 않고 결과를 바로 보이도록 수정
                    //if (EmrSettingsUtil.getEmrCompany(getBaseContext()).equalsIgnoreCase(EmrSettingsUtil.EMR_COMPANY_METROSOFT)) {
                    //    url = "ResultLisServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv + "&frdt=" + frDate + "&todt=" + toDate + "&mode=1";
                    //    mXmlResultLisSpcno = getXml(url);
                    //} else {
                        url = "ResultLisServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv + "&frdt=" + frDate + "&todt=" + toDate;
                        mXmlResultLis = getXml(url);
                    //}
                    mQueryResultLis = true;
                } else if (selectedTab.equals(EMR_SCAN_LIST)) {
                    // 기타서식(이미시스캔)
                    // 2021.08.11 WOOIL - bdiv추가
                    url = "ChartServlet?mode=1&hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv + "&frdt=" + frDate + "&todt=" + toDate;
                    mXmlEmrScan = getXml(url);
                    mQueryEmrScan = true;
                } else if (selectedTab.equals(MED_RECORD_LIST)) {
                    // 투약기록지, 최초일 최종일
                    // 2021.08.11 WOOIL - bdiv추가
                    //mode = "3";
                    //url = "ChartServlet?hospitalid=" + hospitalId + "&mode=" + mode + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv + "&frdt=" + frDate + "&todt=" + toDate;
                    //mXmlMedRecordMinMaxDodt = getXml(url);
                    // 투약기록지
                    mode = "4";
                    url = "ChartServlet?hospitalid=" + hospitalId + "&mode=" + mode + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv + "&frdt=" + frDate + "&todt=" + toDate;
                    mXmlMedRecord = getXml(url);
                    mQueryMedRecord = true;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            afterGet();
                        } catch (Exception e) {
                            Log.d("EmrDroid", "dialog.dismiss exception");
                            Log.d("EmrDroid", e.getMessage());
                        }
                    }
                });
            }
        }).start();

    }

    /*
    private void getLisResultBySpcno() {
        if (mSpcno.equals("")) {
            showSimpleDialog("검체번호가 없습니다.");
            return;
        }

        mDialog = ProgressDialog.show(Order.this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String url = "ResultLisServlet?hospitalid=" + hospitalId + "&spcno=" + mSpcno;
                mXmlResultLis = getXml(url);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            afterGetResultLis();
                        } catch (Exception e) {
                            Log.d("EmrDroid", "dialog.dismiss exception");
                            Log.d("EmrDroid", e.getMessage());
                        }
                    }
                });
            }
        }).start();

    }
    */

    // ----------------------------------------------------------------------------------------------------
    // 화면에 출력
    // ----------------------------------------------------------------------------------------------------
    private void afterGet() {
        String selectedTab = mTabHost.getCurrentTabTag();
        if (selectedTab.equals(ORDER_LIST)) {
            afterGetOrder();
        } else if (selectedTab.equals(CHART_LIST)) {
            afterGetChart();
        } else if (selectedTab.equals(NR_CHART_LIST)) {
            afterGetNrChart();
        } else if (selectedTab.equals(RESULT_RAD_LIST)) {
            afterGetOrderRad();
        } else if (selectedTab.equals(RESULT_SPE_LIST)) {
            afterGetOrderSpe();
        } else if (selectedTab.equals(RESULT_LIS_LIST)) {
            // 2025.02.07 WOOIL - 검체리스트를 조회하지 않고 결과를 바로 보이도록 수정
            //if (EmrSettingsUtil.getEmrCompany(getBaseContext()).equalsIgnoreCase(EmrSettingsUtil.EMR_COMPANY_METROSOFT)) {
            //    afterGetResultLisSpcno(); // 첨체번호 리스트를 먼저 조회한다.
            //} else {
                afterGetResultLis();
            //}
        } else if (selectedTab.equals(EMR_SCAN_LIST)) {
            afterGetEmrScan();
        } else if (selectedTab.equals(MED_RECORD_LIST)) {
            afterGetMedRecord();
        }
    }

    private void DisplayPatientInfo() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                mPatientInfoTextView.setText(mXmlPatientInfo);
            }
        });
    }

    // ----------------------------------------------------------------------------------------------------
    // 처방을 화면에 출력
    // ----------------------------------------------------------------------------------------------------
    private void afterGetOrder() {

        Log.d("EmrDroid", "mXmlOrder=" + mXmlOrder);

        DisplayPatientInfo();

        ResultSetHelper rs;

        // xml해부
        try {

            mOrderListNoData.setVisibility(View.GONE);

            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            rs = new ResultSetHelper(mXmlOrder, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getRecordCount() == 0) {
                mOrderListNoData.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();
                OrderAdapter adapter = new OrderAdapter(this, rs);
                mOrderList.setAdapter(adapter);
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // 차트(기록지)를 화면에 출력
    // ----------------------------------------------------------------------------------------------------
    private void afterGetChart() {

        DisplayPatientInfo();

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        ResultSetHelper rs;

        // xml해부
        try {

            mChartListNoData.setVisibility(View.GONE);

            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            rs = new ResultSetHelper(mXmlChart, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getRecordCount() == 0) {
                mChartListNoData.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();
                String bkExdt = "";
                String bkBdiv = "";
                for (int i = 0; i < rs.getRecordCount(); i++) {
                    map = new HashMap<String, Object>();
                    String exdt = rs.getString(i, "exdt");
                    String bdiv = rs.getString(i, "bdiv");
                    if (bkExdt.equals(exdt) == false || bkBdiv.equals(bdiv) == false) {
                        String exdate = getFormattedDate(exdt);
                        if (bdiv.equals("1")) exdate += " 외래";
                        else if (bdiv.equals("3")) exdate += " 응급실";
                        map.put("div", "1"); // 일자구분선인지 여부
                        map.put("exdt", exdt);
                        map.put("c_case", "");
                        map.put("bdiv", "");
                        map.put("rmk1", exdate);
                        mylist.add(map);
                        map = new HashMap<String, Object>();
                    }
                    bkExdt = exdt;
                    bkBdiv = bdiv;

                    map.put("div", "");
                    map.put("exdt", exdt);
                    map.put("bdiv", bdiv);
                    map.put("c_case", rs.getString(i, "c_case"));
                    map.put("rmk1", rs.getString(i, "rmk1"));

                    mylist.add(map);
                }

                ChartAdapter adapter = new ChartAdapter(this, mylist);
                mChartList.setAdapter(adapter);
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    // 2026.03.17 WOOIL - 간호기록지 조회
    private void afterGetNrChart() {

        DisplayPatientInfo();

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        ResultSetHelper rs;

        // xml해부
        try {

            mNrChartListNoData.setVisibility(View.GONE);

            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            rs = new ResultSetHelper(mXmlNrChart, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getRecordCount() == 0) {
                mNrChartListNoData.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();
                for (int i = 0; i < rs.getRecordCount(); i++) {
                    map = new HashMap<String, Object>();
                    map.put("wdate", rs.getString(i, "wdate"));
                    map.put("seq", rs.getString(i, "seq"));
                    map.put("wtime", rs.getString(i, "wtime"));
                    map.put("result", rs.getString(i, "result"));
                    map.put("empid", rs.getString(i, "empid"));
                    map.put("empnm", rs.getString(i, "empnm"));

                    mylist.add(map);
                }
                NrChartAdapter adapter = new NrChartAdapter(this, mylist);
                mNrChartList.setAdapter(adapter);
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // 영상진단결과를 조회하기 위한 처방을 화면에 출력
    // ----------------------------------------------------------------------------------------------------
    private void afterGetOrderRad() {

        DisplayPatientInfo();

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        ArrayList<HashMap<String, Object>> mylist2 = new ArrayList<HashMap<String, Object>>(); // 결과용
        HashMap<String, Object> map2 = null; // 결과용

        ResultSetHelper rs;

        // xml해부
        try {

            mResultRadListOrderNoData.setVisibility(View.GONE);

            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            rs = new ResultSetHelper(mXmlOrderRad, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getRecordCount() == 0) {
                mResultRadListOrderNoData.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();
                for (int i = 0; i < rs.getRecordCount(); i++) {
                    map = new HashMap<String, Object>();
                    String odt = rs.getString(i, "odt");
                    String orddate = super.getFormattedDate(odt);
                    map.put("orddate", orddate);
                    map.put("odt", rs.getString(i, "odt"));
                    map.put("ono", rs.getString(i, "ono"));
                    map.put("onm", rs.getString(i, "onm"));
                    map.put("bdiv", rs.getString(i, "bdiv"));

                    mylist.add(map);

                    // 결과용
                    map2 = new HashMap<String, Object>();
                    map2.put("show", "");
                    map2.put("result", "");

                    mylist2.add(map2);
                }
            }
            ResultRadAdapter adapter = new ResultRadAdapter(this, mylist, mylist2);
            mResultRadListOrder.setAdapter(adapter);

        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // 기능검사결과를 조회하기 위한 처방을 화면에 출력
    // ----------------------------------------------------------------------------------------------------
    private void afterGetOrderSpe() {

        DisplayPatientInfo();

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        ArrayList<HashMap<String, Object>> mylist2 = new ArrayList<HashMap<String, Object>>(); // 결과용
        HashMap<String, Object> map2 = null; // 결과용

        ResultSetHelper rs;

        // xml해부
        try {

            mResultSpeListOrderNoData.setVisibility(View.GONE);

            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            rs = new ResultSetHelper(mXmlOrderSpe, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getRecordCount() == 0) {
                mResultSpeListOrderNoData.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();
                for (int i = 0; i < rs.getRecordCount(); i++) {
                    map = new HashMap<String, Object>();
                    String odt = rs.getString(i, "odt");
                    String orddate = super.getFormattedDate(odt);
                    map.put("orddate", orddate);
                    map.put("odt", rs.getString(i, "odt"));
                    map.put("ono", rs.getString(i, "ono"));
                    map.put("onm", rs.getString(i, "onm"));
                    map.put("bdiv", rs.getString(i, "bdiv"));

                    mylist.add(map);

                    // 결과용
                    map2 = new HashMap<String, Object>();
                    map2.put("show", "");
                    map2.put("result", "");

                    mylist2.add(map2);
                }
                ResultSpeAdapter adapter = new ResultSpeAdapter(this, mylist, mylist2);
                mResultSpeListOrder.setAdapter(adapter);
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // 영상진단결과  자료 조회
    // ----------------------------------------------------------------------------------------------------
    private void getResultRad(final int selectedPostion, final String odt, final String ono, final String bdiv) {
        mXmlResultRad = "";
        mDialog = ProgressDialog.show(Order.this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String url = "";
                String hospitalId = getHospitalId();
                String userId = getUserId();
                // 결과
                url = "ResultRadServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt + "&odt=" + odt + "&ono=" + ono + "&bdiv=" + bdiv;
                mXmlResultRad = getXml(url);
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            afterGetResultRad(selectedPostion);
                        } catch (Exception e) {
                            Log.d("EmrDroid", "dialog.dismiss exception");
                            Log.d("EmrDroid", e.getMessage());
                        }
                    }
                });
            }
        }).start();
    }

    // ----------------------------------------------------------------------------------------------------
    // 기능검사결과  자료 조회
    // ----------------------------------------------------------------------------------------------------
    private void getResultSpe(final int selectedPostion, final String odt, final String ono, final String bdiv) {
        mXmlResultSpe = "";
        mDialog = ProgressDialog.show(Order.this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String url = "";
                String hospitalId = getHospitalId();
                String userId = getUserId();
                // 결과
                url = "ResultRadServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt + "&odt=" + odt + "&ono=" + ono + "&bdiv=" + bdiv + "&mode=1";
                mXmlResultSpe = getXml(url);
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            afterGetResultSpe(selectedPostion);
                        } catch (Exception e) {
                            Log.d("EmrDroid", "dialog.dismiss exception");
                            Log.d("EmrDroid", e.getMessage());
                        }
                    }
                });
            }
        }).start();
    }

    // ----------------------------------------------------------------------------------------------------
    // 영상진단결과 를 화면에 출력
    // ----------------------------------------------------------------------------------------------------
    private void afterGetResultRad(final int selectedPostion) {
        ResultSetHelper rs;

        // xml해부
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }

            rs = new ResultSetHelper(mXmlResultRad, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                //showSimpleDialog(R.string.no_data_message);
                String resultText = "";
                resultText = getString(R.string.no_data_message) + "\n\n";

                ExpandableListView list = (ExpandableListView) findViewById(R.id.result_rad_list_order);
                ResultRadAdapter adapter = (ResultRadAdapter) list.getExpandableListAdapter();
                adapter.setRadResult(selectedPostion, "1", resultText);
            } else {
                String resultText = "";
                resultText = "접수일자 : " + getFormattedDate(rs.getString(0, "acptdt")) + " " +
                        "촬영일자 : " + getFormattedDate(rs.getString(0, "phtdt")) + " " +
                        "판독일자 : " + getFormattedDate(rs.getString(0, "rptdt")) + "\n\n";
                resultText += rs.getString(0, "rptxt");

                ExpandableListView list = (ExpandableListView) findViewById(R.id.result_rad_list_order);
                ResultRadAdapter adapter = (ResultRadAdapter) list.getExpandableListAdapter();
                adapter.setRadResult(selectedPostion, "1", resultText);
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // 기능검사결과 를 화면에 출력
    // ----------------------------------------------------------------------------------------------------
    private void afterGetResultSpe(final int selectedPostion) {
        ResultSetHelper rs;

        // xml해부
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }

            rs = new ResultSetHelper(mXmlResultSpe, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                //showSimpleDialog(R.string.no_data_message);
                String resultText = "";
                resultText = getString(R.string.no_data_message) + "\n\n";

                ExpandableListView list = (ExpandableListView) findViewById(R.id.result_spe_list_order);
                ResultSpeAdapter adapter = (ResultSpeAdapter) list.getExpandableListAdapter();
                adapter.setSpeResult(selectedPostion, "1", resultText);
            } else {
                String resultText = "";
                resultText = "접수일자 : " + getFormattedDate(rs.getString(0, "acptdt")) + " " +
                        "촬영일자 : " + getFormattedDate(rs.getString(0, "phtdt")) + " " +
                        "판독일자 : " + getFormattedDate(rs.getString(0, "rptdt")) + "\n\n";
                resultText += rs.getString(0, "rptxt");

                ExpandableListView list = (ExpandableListView) findViewById(R.id.result_spe_list_order);
                ResultSpeAdapter adapter = (ResultSpeAdapter) list.getExpandableListAdapter();
                adapter.setSpeResult(selectedPostion, "1", resultText);
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // 진단검사결과 를 화면에 출력
    // ----------------------------------------------------------------------------------------------------
    private void afterGetResultLis() {
        DisplayPatientInfo();

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        ResultSetHelper rs;

        // xml해부
        try {

            mResultLisListNoData.setVisibility(View.GONE);

            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            if (mXmlResultLis == null || mXmlResultLis.equals("")) {
                showSimpleDialog("조회된 자료가 없습니다.");
                return;
            }
            rs = new ResultSetHelper(mXmlResultLis, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                mResultLisListNoData.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();

                for (int i = 0; i < rs.getRecordCount(); i++) {
                    String orddt = rs.getString(i, "orddt");

                    if (!orddt.equals("")) {
                        map = new HashMap<String, Object>();

                        map.put("orddt", "");
                        map.put("abbrnm", DateUtil.getFormattedDate(orddt));
                        map.put("rstval", "");
                        map.put("beforerstval", "");
                        map.put("referchk", "");
                        map.put("panicchk", "");
                        map.put("deltachk", "");
                        map.put("reference", "");
                        map.put("unit", "");
                        map.put("spcnm", "");
                        map.put("majnm", "");
                        map.put("isdateline", "1");

                        mylist.add(map);
                    }

                    map = new HashMap<String, Object>();

                    map.put("orddt", orddt);
                    map.put("abbrnm", rs.getString(i, "abbrnm"));
                    map.put("rstval", rs.getString(i, "rstval"));
                    map.put("beforerstval", rs.getString(i, "beforerstval"));
                    map.put("referchk", rs.getString(i, "referchk"));
                    map.put("panicchk", rs.getString(i, "panicchk"));
                    map.put("deltachk", rs.getString(i, "deltachk"));
                    map.put("reference", rs.getString(i, "reference"));
                    map.put("unit", rs.getString(i, "unit"));
                    map.put("spcnm", rs.getString(i, "spcnm"));
                    map.put("majnm", rs.getString(i, "majnm"));
                    map.put("isdateline", "");

                    mylist.add(map);
                }

                ResultLisAdapter adapter = new ResultLisAdapter(this, mylist);
                mResultLisList.setAdapter(adapter);

            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // 진단검사 검체번호 리스트
    // ----------------------------------------------------------------------------------------------------
    /*
    private void afterGetResultLisSpcno() {
        DisplayPatientInfo();

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        ResultSetHelper rs;

        // xml해부
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            if (mXmlResultLisSpcno == null || mXmlResultLisSpcno.equals("")) {
                showSimpleDialog("조회된 자료가 없습니다.");
                return;
            }
            rs = new ResultSetHelper(mXmlResultLisSpcno, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();

                for (int i = 0; i < rs.getRecordCount(); i++) {
                    map = new HashMap<String, Object>();

                    map.put("orddt", rs.getString(i, "orddt"));
                    map.put("deptcd", rs.getString(i, "deptcd"));
                    map.put("ward", rs.getString(i, "ward"));
                    map.put("room", rs.getString(i, "room"));
                    map.put("ordnm", rs.getString(i, "ordnm"));
                    map.put("alltestnm", rs.getString(i, "alltestnm"));
                    map.put("spcnm", rs.getString(i, "spcnm"));
                    map.put("stsnm", rs.getString(i, "stsnm"));
                    map.put("vfydt", rs.getString(i, "vfydt"));
                    map.put("vfytm", rs.getString(i, "vfytm"));
                    map.put("rcvdt", rs.getString(i, "rcvdt"));
                    map.put("rcvtm", rs.getString(i, "rcvtm"));
                    map.put("spcno", rs.getString(i, "spcno"));

                    mylist.add(map);
                }
                ResultLisSpcnoAdapter adapter = new ResultLisSpcnoAdapter(this, mylist);
                mSpcnoList.setAdapter(adapter);

            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }
    */

    // ----------------------------------------------------------------------------------------------------
    // 기타서식(이미지스캔) 리스트 를 화면에 출력
    // ----------------------------------------------------------------------------------------------------
    private void afterGetEmrScan() {
        DisplayPatientInfo();

        //ArrayList<HashMap<String,Object>> mylist = new ArrayList<HashMap<String,Object>>();
        mEmrScanListArray = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        ResultSetHelper rs;

        // xml해부
        try {

            mEmrScanListNoData.setVisibility(View.GONE);

            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }

            rs = new ResultSetHelper(mXmlEmrScan, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                mEmrScanListNoData.setVisibility(View.VISIBLE);
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
                    map = new HashMap<String, Object>();
                    map.put("bdiv", rs.getString(i, "bdiv"));
                    map.put("exdt", rs.getString(i, "exdt"));
                    map.put("seq", rs.getString(i, "seq"));
                    map.put("rptcd", rs.getString(i, "rptcd"));
                    map.put("rptnm", rs.getString(i, "rptnm"));
                    map.put("path", rs.getString(i, "path"));
                    map.put("path2", rs.getString(i, "path2"));
                    map.put("filename", rs.getString(i, "exdt") + "-" + rs.getString(i, "seq") + ".png");
                    map.put("dirpath", dirPath);
                    mEmrScanListArray.add(map);
                }

                //SimpleAdapter adapter = new SimpleAdapter(this, mylist, R.layout.chart_row,
                //        new String[] {"IMAGE","ODT","ONM","OQTY","OUNIT","ORDCNT","ODAYCNT","RMK"},
                //        new int[] {R.id.image_chart_row,R.id.odt_chart_row,R.id.onm_chart_row,R.id.oqty_chart_row,R.id.ounit_chart_row,R.id.ordcnt_chart_row,R.id.odaycnt_chart_row,R.id.rmk_chart_row});
                //list.setAdapter(adapter);

                EmrScanAdapter adapter = null;
                adapter = new EmrScanAdapter(this, mEmrScanListArray, null);
                mEmrScanList.setAdapter(adapter);

                // 서버에서 이미지를 가져와서 저장하고 보여준다.
                // 이미지를 불러와서 리스트 앞에 보여주는 것이 속도도 오래걸리고
                // 그림이 명확하지 않아서 기대하는 효과가 나지 않는다.
                // 서버에 부담만 주는 것 같이서 막는다.
                //downloadScanImageAndSave();
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    //private void downloadScanImageAndSave(){
    //	if(mEmrScanListArray==null) return;
    //	new Thread(new Runnable() {
    //		protected Handler handler = new Handler();
    //		@Override
    //		public void run() {
    //			// TODO Auto-generated method stub
    //        	String hospitalId=getHospitalId();
    //        	String userId=getUserId();
    //        	for(int i=0;i<mEmrScanListArray.size();i++){
    //        		HashMap<String,Object> map = mEmrScanListArray.get(i);
    //        		String imagePath = (String)map.get("path");
    //        		String saveFileName = (String)map.get("filename");
    //        		String dirPath = (String)map.get("dirpath");
    //        		
    //				String imageUrl = "EmrScanServlet?hospitalid=" + hospitalId + "&path=" + imagePath;
    //				ServletHelper servlerHelper = new ServletHelper();
    //				Bitmap bitmap;
    //
    //				File dir = new File(dirPath);
    //				// 폴더가 없으면 생성
    //				if (!dir.exists()) {
    //					Log.d("EmrDroid", "폴더생성");
    //					dir.mkdirs();
    //				}
    //				try {
    //					bitmap = servlerHelper.getBitmap(imageUrl);
    //					// 안드로이드에 파일을 쓴다.
    //					String fileName = dirPath + "/" + saveFileName;
    //					FileOutputStream output = new FileOutputStream(fileName);
    //					bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
    //
    //					output.flush();
    //					output.close();
    //				} catch (Exception e) {
    //					// TODO Auto-generated catch block
    //					Log.d("EmrDroid","downloadScanImageAndSave error" + e.getLocalizedMessage());
    //				}
    //				//
    //        	}
    //        	handler.post(new Runnable() {
    //				public void run() {
    //	            	// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
    //	            	// 이를 방지함.
    //	            	try {
    //	            		afterDownloadScanImageAndSave();
    //	            	}catch(Exception e) {
    //	            		Log.d("EmrDroid", e.getMessage());
    //	            	}
    //				}
    //			});
    //		}
    //	}).start();
    //}

    //private void afterDownloadScanImageAndSave(){
    //	Log.d("EmrDroid", "afterDownloadScanImageAndSave");
    //	
    //	((EmrScanAdapter)mEmrScanList.getAdapter()).notifyDataSetInvalidated();
    //}

    // ----------------------------------------------------------------------------------------------------
    // 투약기록지 조회
    // ----------------------------------------------------------------------------------------------------
    private void afterGetMedRecord() {
        DisplayPatientInfo();

        ResultSetHelper rs;

        try {
            mMedRecordListNoData.setVisibility(View.GONE);

            mMedRecordMinDodt = "";
            mMedRecordMaxDodt = "";

            rs = new ResultSetHelper(mXmlMedRecord, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getRecordCount() == 0) {
                mMedRecordListNoData.setVisibility(View.VISIBLE);
            } else {
                for (int i = 0; i < rs.getRecordCount(); i++) {
                    String dodt = rs.getString(i, "dodt");
                    if ("".equalsIgnoreCase(mMedRecordMinDodt)) mMedRecordMinDodt = dodt;
                    if ("".equalsIgnoreCase(mMedRecordMaxDodt)) mMedRecordMaxDodt = dodt;

                    if (dodt.compareTo(mMedRecordMinDodt) < 0) mMedRecordMinDodt = dodt;
                    if (dodt.compareTo(mMedRecordMaxDodt) > 0) mMedRecordMaxDodt = dodt;
                }
                mMedRecordLeftCol = 0;
                mMedRecordColCount = getMedRecordColCount() + 1;

                // 일자컬럼제목 출력
                displayMedRecordColHeader();

                MedRecordAdapter adapter = null;
                adapter = new MedRecordAdapter(this, rs, mMedRecordMinDodt, mMedRecordMaxDodt);
                mMedRecordList.setAdapter(adapter);
            }

            /*
            rs = new ResultSetHelper(mXmlMedRecordMinMaxDodt, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else {
                if (rs.getReturnCode() > 0) {
                    mMedRecordMinDodt = rs.getString(0, "mindodt");
                    mMedRecordMaxDodt = rs.getString(0, "maxdodt");

                    mMedRecordLeftCol = 0;
                    mMedRecordColCount = getMedRecordColCount() + 1;
                }

                // 일자컬럼제목 출력
                displayMedRecordColHeader();

                rs = new ResultSetHelper(mXmlMedRecord, EmrSettingsUtil.getMaskYn(getBaseContext()));
                if (rs.getReturnCode() < 0) {
                    showSimpleDialog(rs.getReturnDesc());
                } else {
                    MedRecordAdapter adapter = null;
                    adapter = new MedRecordAdapter(this, rs, mMedRecordMinDodt, mMedRecordMaxDodt);
                    mMedRecordList.setAdapter(adapter);
                }
            }
            */
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            showSimpleDialog(e.getMessage());
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            showSimpleDialog(e.getMessage());
        }
    }

    private void displayMedRecordColHeader() {
        SimpleDateFormat sdf;
        Date minDate;
        Date date;
        String dateString, fmtDateString;
        TextView tv;

        sdf = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        sdf.setLenient(false);
        try {
            minDate = sdf.parse(mMedRecordMinDodt);
            // 화면에 9일 까지 보여준다.
            for (long i = 0; i < 9; i++) {
                date = super.addDate(minDate, i + mMedRecordLeftCol);
                dateString = sdf.format(date);
                fmtDateString = super.getFormattedDate(dateString);
                tv = null;
                if (i == 0) tv = (TextView) findViewById(R.id.ordcnt0_column_header);
                else if (i == 1) tv = (TextView) findViewById(R.id.ordcnt1_column_header);
                else if (i == 2) tv = (TextView) findViewById(R.id.ordcnt2_column_header);
                else if (i == 3) tv = (TextView) findViewById(R.id.ordcnt3_column_header);
                else if (i == 4) tv = (TextView) findViewById(R.id.ordcnt4_column_header);
                else if (i == 5) tv = (TextView) findViewById(R.id.ordcnt5_column_header);
                else if (i == 6) tv = (TextView) findViewById(R.id.ordcnt6_column_header);
                else if (i == 7) tv = (TextView) findViewById(R.id.ordcnt7_column_header);
                else if (i == 8) tv = (TextView) findViewById(R.id.ordcnt8_column_header);
	    		/*
	    		else if(i==9) tv = (TextView)findViewById(R.id.ordcnt9_column_header);
	    		else if(i==10) tv = (TextView)findViewById(R.id.ordcnt10_column_header);
	    		else if(i==11) tv = (TextView)findViewById(R.id.ordcnt11_column_header);
	    		*/
                if (tv != null) tv.setText(fmtDateString.subSequence(5, 10)); // 월.일만 출력
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private long getMedRecordColCount() throws ParseException {
        SimpleDateFormat sdf;
        Date minDate;
        Date maxDate;
        long diff;

        // 자료가 없는 경우임.
        if ("".equals(mMedRecordMinDodt) || "".equals(mMedRecordMaxDodt)) return 0;

        sdf = new SimpleDateFormat("yyyyMMdd", Locale.KOREA);
        sdf.setLenient(false);
        minDate = sdf.parse(mMedRecordMinDodt);
        maxDate = sdf.parse(mMedRecordMaxDodt);
        diff = maxDate.getDate() - minDate.getDate();

        return diff;
    }

    // 2022.03.23 WOOIL - 이곳에서 삭제하는 기능은 막는다.
    //private void deleteEmrScan(final String bdiv, final String exdt, final String seq, final String rptcd, final String subPageList){
    //	AlertDialog.Builder dialog = new AlertDialog.Builder(this);
    //	dialog.setTitle("확인");
    //	dialog.setMessage("삭제하시겠습니까?");
    //	dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
    //		public void onClick(DialogInterface dialog, int which) {
    //			actionDeleteEmrScan(bdiv, exdt, seq, rptcd, subPageList);
    //		}
    //	});
    //	dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
    //		public void onClick(DialogInterface dialog, int which) {
    //			dialog.dismiss();
    //		}
    //	});
    //	dialog.setCancelable(false);
    //	dialog.show();
    //}

    // 2022.03.23 WOOIL - 이곳에서 삭제하는 기능은 막는다.
    //private void actionDeleteEmrScan(final String bdiv, final String exdt, final String seq, final String rptcd, final String subPageList){
    //	mDialog = ProgressDialog.show(Order.this, "", getString(R.string.query_wait_message), true);
    //	new Thread(new Runnable() {
    //		public void run() {
    //			String hospitalId = getHospitalId();
    //			String userId = getUserId();
    //			String url = "";
    //			String mode="12";
    //
    //			// 이미지 삭제(실제로는 테이블에 플래그를 넣는다).
    //			url = "ChartServlet?hospitalid=" + hospitalId +
    //					          "&userid=" + userId +
    //		                      "&pid=" + mPid +
    //		                      "&bdiv=" + bdiv +
    //		                      "&exdt=" + exdt +
    //		                      "&seq=" + seq +
    //		                      "&rptcd=" + rptcd +
    //		                      "&sub_page_list=" + subPageList +
    //		                      "&mode=" + mode ;
    //			final String xml = getXml(url);
    //
    //			mHandler.post(new Runnable() {
    //				public void run() {
    //					// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
    //					// 이를 방지함.
    //					try {
    //						mDialog.dismiss();
    //						afterActionDeleteEmrScan(xml);
    //					} catch (Exception e) {
    //						;
    //					}
    //				}
    //			});
    //		}
    //	}).start();
    //}

    // 2022.03.23 WOOIL - 이곳에서 삭제하는 기능은 막는다.
    //private void afterActionDeleteEmrScan(String xml){
    //	if(xml.equalsIgnoreCase("y")) getOrder(); // 성공. 다시 조회.
    //}
}
