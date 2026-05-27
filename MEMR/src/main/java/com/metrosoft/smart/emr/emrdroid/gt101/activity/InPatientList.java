package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.support.annotation.NonNull;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.AbsoluteSizeSpan;
import android.view.ContextMenu;
import android.view.DisplayCutout;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.CommonCode;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.ConsentFormList;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.DmSheet;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.IoSheet;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.Order;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.PatientHosHx;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.PatientSafeCheck;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.PresavedConsentFormList;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.SignedConsentFormList;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.TprSheet;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.DateUtil;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class InPatientList extends MyActivity implements ListView.OnItemClickListener
        , ListView.OnScrollListener
        , ListView.OnTouchListener
        , ListView.OnItemLongClickListener
        , TabHost.OnTabChangeListener {
    private final String IN_PATIENT_LIST = "0";
    private final String IN_PATIENT_SEARCH = "1";
    private final String OUT_PATIENT_LIST = "2";

    private long backKeyClick = 0;
    private long backKeyClickTime;

    private String mCXml;
    private String mInXml; // 재원환자용
    private String mSearchXml; // 환자검색용
    private String mConditionWardCode, mConditionWardCodeName;
    private String mConditionDeptCode, mConditionDeptCodeName;
    private String mConditionPdridCode, mConditionPdridCodeName;
    private String mSortOrder; //
    // 외래환자용
    private String mOutXml; // 외래환자용
    private String mOutDeptCode, mOutDeptCodeName;
    private String mOutPdridCode, mOutPdridCodeName;
    private int mOutExdtYear, mOutExdtMonth, mOutExdtDay;
    private int mSearchExdtYear, mSearchExdtMonth, mSearchExdtDay; // 2021.09.30 WOOIL - 환자검색시 사용 할 일자
    private int mSearchExdtToYear, mSearchExdtToMonth, mSearchExdtToDay; // 2023.03.27 WOOIL - 검색을 일자 범위로 하도록 종료일 추가
    private String mSearchIofg; // 2025.08.07 WOOIL - 0.외래+입원 1.외래 2.입원
    private String mOutSortOrder; //
    //private int mPageNo=1; // 다음에 읽어야 할 페이지. <1 이면 끝까지 읽은 것임.
    private int mFirstVisibleItem = 0; // 재원환자와 환자검색탭을 이동시 재원환자리스트뷰 맨 위로 올 줄번호
    private int mOutFirstVisibleItem = 0; // 재원환자와 환자검색탭을 이동시 외래환자리스트뷰 맨 위로 올 줄번호
    //private boolean mLockListView=false; // 현재 재 검색중인지??


    private TabHost mTabHost;

    private Button mWardButton;
    private Button mDeptButton;
    private Button mPdridButton;
    private Button mSortOrderButton;
    private Button mSearchButton;
    private Button mOutExdtButton;
    private Button mSearchExdtButton; // 2021.09.30 WOOIL - 환자검색시 사용할 일자
    private Button mSearchExdtToButton; // 2023.03.27 WOOIL - 검색을 일자 범위로 하도록 종료일 추가
    private Button mSearchIofgButton; // 2025.08.07 WOOIL - 전체(외래+입원), 외래, 입원
    private Button mClearButton; // 2025.08.07 WOOIL - 화면지움
    private Button mOutDeptButton;
    private Button mOutPdridButton;
    private Button mOutSortOrderButton;
    private CheckBox mOutRsvInOnlyCheckBox; // 2026.04.09 wooil - 입원예정자암
    private ListView mPatListView;
	
	/* dialog로 변경하였으나, 나중에 필요시 참고하기 위하여 소스는 남겨둠.
	PopupWindow mPopup=null;
	View mPopupView=null;
	*/

    HashMap<String, Object> mSelectedMap = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.in_patient_list, getString(R.string.exit));
        super.mCallConfigSetting = true;

        super.setButton1(true, "정렬옵션", BUTTON_TYPE_OPTION);

        if (EmrSettingsUtil.getPatientSafeCheckYn(getBaseContext()) == true) {
            super.setLinkButton1(true, "안전");
        } else {
            super.setLinkButton1(false, "안전");
        }

        // -----------------------------------------------------
        // 탭생성
        // createTabIndicator : MyActivity에 있음.
        // -----------------------------------------------------
        mTabHost = (TabHost) findViewById(R.id.tabHost);
        // findViewById를 이용해 TabHost인스턴스를 얻은경우 꼭 호출 필요
        mTabHost.setup();
        // Tab builder 객체
        TabHost.TabSpec spec;
        // 재원환자 세팅 & 등록  *****
        spec = mTabHost.newTabSpec(IN_PATIENT_LIST);    // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(IN_PATIENT_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_in_patient);             // Tab 내용
        mTabHost.addTab(spec);                             // Tab 등록
        // 외래환자 세팅 & 등록 *****
        spec = mTabHost.newTabSpec(OUT_PATIENT_LIST);   // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(OUT_PATIENT_LIST)));    // Tab 제목
        spec.setContent(R.id.tab_out_patient);            // Tab 내용
        mTabHost.addTab(spec);                             // Tab 등록
        // 환자검색 세팅 & 등록 *****
        spec = mTabHost.newTabSpec(IN_PATIENT_SEARCH);  // Tab Builder 객체 생성
        spec.setIndicator(createTabIndicator(mTabHost.getContext(), getTabText(IN_PATIENT_SEARCH)));    // Tab 제목
        spec.setContent(R.id.tab_patient_search);        // Tab 내용
        mTabHost.addTab(spec);                             // Tab 등록
        // 처음 등록된 Tab을 보여줌.
        mTabHost.setCurrentTab(0);
        // 현재 선택된 탭의 제목을 화면의 제목으로 바꾼다.
        setMyTitle(getTabText(mTabHost.getCurrentTabTag()));

        // 2025.08.08 WOOIL - 사용자가 종료할 때 선택한 탭을 유지하기 위해...
        String currentTabId = EmrSettingsUtil.getCurrentTabId(getBaseContext());
        if(currentTabId==OUT_PATIENT_LIST){
            mTabHost.setCurrentTab(1); // 외래 환자
        } else if(currentTabId==IN_PATIENT_SEARCH){
            mTabHost.setCurrentTab(2); // 환자 검색
        } else {
            mTabHost.setCurrentTab(0); // 병동 환자
        }
        setMyTitle(getTabText(mTabHost.getCurrentTabTag()));
        // 리스터연결 - 기본 탭을 설정하고 리스너를 연결하자.
        //            그렇지 않으면 텝을 변경할 때 이벤트가 발생하여 이상동작하는 경우가 있다.
        mTabHost.setOnTabChangedListener(this);



        // 이전에 선택한 값을 다시 사용한다.
        mConditionWardCode = EmrSettingsUtil.getWardCode(getBaseContext());
        mConditionWardCodeName = EmrSettingsUtil.getWardCodeName(getBaseContext(), "모든병동");
        mConditionDeptCode = EmrSettingsUtil.getDeptCode(getBaseContext());
        mConditionDeptCodeName = EmrSettingsUtil.getDeptCodeName(getBaseContext(), "모든진료과");
        mConditionPdridCode = EmrSettingsUtil.getPdridCode(getBaseContext());
        mConditionPdridCodeName = EmrSettingsUtil.getPdridCodeName(getBaseContext(), "모든의사");
        mSortOrder = "1";
        mSortOrder = EmrSettingsUtil.getSortOrder(getBaseContext(), "1");

        // 외래환자리스트용
        mOutDeptCode = EmrSettingsUtil.getOutDeptCode(getBaseContext());
        mOutDeptCodeName = EmrSettingsUtil.getOutDeptCodeName(getBaseContext(), "모든진료과");
        mOutPdridCode = EmrSettingsUtil.getOutPdridCode(getBaseContext());
        mOutPdridCodeName = EmrSettingsUtil.getOutPdridCodeName(getBaseContext(), "모든의사");
        mOutSortOrder = "1";
        mOutSortOrder = EmrSettingsUtil.getOutSortOrder(getBaseContext(), "1");

        // 환자검색 - 외래+입원, 외래, 입원
        mSearchIofg = "0";
        mSearchIofg = EmrSettingsUtil.getSearchIofg(getBaseContext(), "0");

        // ------------------------------------------------------
        // 병동, 진료과  버튼 초기화
        // ------------------------------------------------------
        initConditionButtons();

        // ------------------------------------------------------
        // Context menu 및 리스너
        // ------------------------------------------------------
        // 재원환자리스트
        mPatListView = (ListView) findViewById(R.id.patient_list);
        //registerForContextMenu(mPatListView);  --> popup menu로 변경
        mPatListView.setOnItemClickListener(this);
        mPatListView.setOnScrollListener(this);
        mPatListView.setOnTouchListener(this);
        mPatListView.setOnItemLongClickListener(this);
        
        mSortOrderButton.setVisibility(View.GONE);
        mOutSortOrderButton.setVisibility(View.GONE);

        // 초기화
        mInXml = "";
        mSearchXml = "";
        mOutXml = "";

        // 환자리스트 조회
        if (savedInstanceState == null) {
//        	mPageNo=1;
            mFirstVisibleItem = 0;
            mOutFirstVisibleItem = 0;
            getPatientList();
        } else {
            mInXml = savedInstanceState.getString("inXml");
            mSearchXml = savedInstanceState.getString("searchXml");
            mOutXml = savedInstanceState.getString("outXml");
            if (mInXml == null) mInXml = "";
            if (mSearchXml == null) mSearchXml = "";
            if (mOutXml == null) mOutXml = "";
            mConditionWardCode = savedInstanceState.getString("conditionWardCode");
            mConditionWardCodeName = savedInstanceState.getString("conditionWardCodeName");
            mConditionDeptCode = savedInstanceState.getString("conditionDeptCode");
            mConditionDeptCodeName = savedInstanceState.getString("conditionDeptCodeName");
            mConditionPdridCode = savedInstanceState.getString("conditionPdridCode");
            mConditionPdridCodeName = savedInstanceState.getString("conditionPdridCodeName");
//        	mPageNo=savedInstanceState.getInt("mPageNo");
            mFirstVisibleItem = savedInstanceState.getInt("mFirstVisibleItem");
            mTabHost.setCurrentTab(savedInstanceState.getInt("currentTab"));
            setSearchText(savedInstanceState.getString("searchText"));
            mWardButton.setText(mConditionWardCodeName);
            mDeptButton.setText(mConditionDeptCodeName);
            mPdridButton.setText(mConditionPdridCodeName);
            // 외래환자용
            mOutDeptCode = savedInstanceState.getString("outDeptCode");
            mOutDeptCodeName = savedInstanceState.getString("outDeptCodeName");
            mOutPdridCode = savedInstanceState.getString("outPdridCode");
            mOutPdridCodeName = savedInstanceState.getString("outPdridCodeName");
            mOutDeptButton.setText(mOutDeptCodeName);
            mOutPdridButton.setText(mOutPdridCodeName);
            mOutExdtYear = savedInstanceState.getInt("outExdtYear");
            mOutExdtMonth = savedInstanceState.getInt("outExdtMonth");
            mOutExdtDay = savedInstanceState.getInt("outExdtDay");
            displayOutExdt();
            // 2021.09.30 WOOIL - 환자검색용
            mSearchExdtYear = savedInstanceState.getInt("searchExdtYear");
            mSearchExdtMonth = savedInstanceState.getInt("searchExdtMonth");
            mSearchExdtDay = savedInstanceState.getInt("searchExdtDay");
            displaySearchExdt();
            // 2023.03.27 WOOIL - 검색을 일자 범위로 하도록 종료일 추가
            mSearchExdtToYear = savedInstanceState.getInt("searchExdtToYear");
            mSearchExdtToMonth = savedInstanceState.getInt("searchExdtToMonth");
            mSearchExdtToDay = savedInstanceState.getInt("searchExdtToDay");
            displaySearchExdtTo();

            afterGetPatientList(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("inXml", mInXml);
        outState.putString("searchXml", mSearchXml);
        outState.putString("outXml", mOutXml);
        outState.putInt("currentTab", mTabHost.getCurrentTab());
        outState.putString("searchText", getSearchText());
        outState.putString("conditionWardCode", mConditionWardCode);
        outState.putString("conditionWardCodeName", mConditionWardCodeName);
        outState.putString("conditionDeptCode", mConditionDeptCode);
        outState.putString("conditionDeptCodeName", mConditionDeptCodeName);
        outState.putString("conditionPdridCode", mConditionPdridCode);
        outState.putString("conditionPdridCodeName", mConditionPdridCodeName);
        outState.putString("outDeptCode", mOutDeptCode);
        outState.putString("outDeptCodeName", mOutDeptCodeName);
        outState.putString("outPdridCode", mOutPdridCode);
        outState.putString("outPdridCodeName", mOutPdridCodeName);
        outState.putInt("outExdtYear", mOutExdtYear);
        outState.putInt("outExdtMonth", mOutExdtMonth);
        outState.putInt("outExdtDay", mOutExdtDay);
//		outState.putInt("mPageNo",mPageNo);
        outState.putInt("mFirstVisibleItem", mFirstVisibleItem);
        // 2021.09.30 WOOIL - 환자검색용
        outState.putInt("searchExdtYear", mSearchExdtYear);
        outState.putInt("searchExdtMonth", mSearchExdtMonth);
        outState.putInt("searchExdtDay", mSearchExdtDay);
        // 2023.03.27 WOOIL - 검색을 일자 범위로 하도록 종료일 추가
        outState.putInt("searchExdtToYear", mSearchExdtToYear);
        outState.putInt("searchExdtToMonth", mSearchExdtToMonth);
        outState.putInt("searchExdtToDay", mSearchExdtToDay);
    }

    // 검색어 입력창의 키보드 내리기
    private void hideKeyboard() {
        EditText text = (EditText) findViewById(R.id.search_text);
        InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(text.getWindowToken(), 0);
    }

    @Override
    public void onClickBackButton(View v) {
        // 종료확인 Dialog 창을 띄워서 처리하는 부분.
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.exit_dialog_title);
        dialog.setMessage(R.string.exit_dialog_message);
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                finish();
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

    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        final int duration = 2000;
        backKeyClick++;
        if (backKeyClick == 1) {
            backKeyClickTime = System.currentTimeMillis();
            Toast t = Toast.makeText(this, R.string.exit_confirm_message, Toast.LENGTH_SHORT);
            t.setDuration(Toast.LENGTH_LONG);
            t.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {
                        ;
                    }
                    backKeyClick = 0;
                }
            }).start();
        } else {
//			if(currentTime-backKeyClickTime<=duration) {
            super.onBackPressed();
//			}
//			backKeyClick=0;
        }

    }

    @Override
    public void onClickQueryButton(View v) {
        // 재원환자조회
//		mPageNo=1;
        mFirstVisibleItem = 0;
        getPatientList();
    }

    @Override
    public void onClickButton1(View v) {
        SortOrderDialog dialog = new SortOrderDialog(InPatientList.this);
        dialog.show();
    }

    @Override
    public void onClickLinkButton1(View v) {
        Intent intent;
        intent = new Intent(InPatientList.this, PatientSafeCheck.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) { // 액티비트가 정상적으로 종료되었을 경우
            if (requestCode == CommonCode.WARD_CODE) { // InformationInput에서 호출한 경우에만 처리합니다.
                mConditionWardCode = data.getStringExtra("code");
                mConditionWardCodeName = data.getStringExtra("codenm");
                mWardButton.setText(mConditionWardCodeName);
                mFirstVisibleItem = 0;
                EmrSettingsUtil.setWardCode(getBaseContext(), mConditionWardCode, mConditionWardCodeName);
                getPatientList();
            } else if (requestCode == CommonCode.DEPT_CODE) {
                mConditionDeptCode = data.getStringExtra("code");
                mConditionDeptCodeName = data.getStringExtra("codenm");
                mDeptButton.setText(mConditionDeptCodeName);
                mFirstVisibleItem = 0;
                EmrSettingsUtil.setDeptCode(getBaseContext(), mConditionDeptCode, mConditionDeptCodeName);
                // 2026.02.12 WOOIL - 과가 변경되면 기존에 선택되었던 의사를 초기화한다.
                mConditionPdridCode = "";
                mConditionPdridCodeName = "모든의사";
                mPdridButton.setText(mConditionPdridCodeName);
                EmrSettingsUtil.setPdridCode(getBaseContext(), mConditionPdridCode, mConditionPdridCodeName);
                getPatientList();
            } else if (requestCode == CommonCode.DOCT_CODE) {
                mConditionPdridCode = data.getStringExtra("code");
                mConditionPdridCodeName = data.getStringExtra("codenm");
                mPdridButton.setText(mConditionPdridCodeName);
                mFirstVisibleItem = 0;
                EmrSettingsUtil.setPdridCode(getBaseContext(), mConditionPdridCode, mConditionPdridCodeName);
                getPatientList();
            } else if (requestCode == CommonCode.OUT_DEPT_CODE) {
                mOutDeptCode = data.getStringExtra("code");
                mOutDeptCodeName = data.getStringExtra("codenm");
                mOutDeptButton.setText(mOutDeptCodeName);
                mFirstVisibleItem = 0;
                EmrSettingsUtil.setOutDeptCode(getBaseContext(), mOutDeptCode, mOutDeptCodeName);
                getPatientList();
            } else if (requestCode == CommonCode.OUT_DOCT_CODE) {
                mOutPdridCode = data.getStringExtra("code");
                mOutPdridCodeName = data.getStringExtra("codenm");
                mOutPdridButton.setText(mOutPdridCodeName);
                mFirstVisibleItem = 0;
                EmrSettingsUtil.setOutPdridCode(getBaseContext(), mOutPdridCode, mOutPdridCodeName);
                getPatientList();
            }
        }
    }

    private void initConditionButtons() {
        // 병동선택버튼
        mWardButton = (Button) findViewById(R.id.ward_button);
        mWardButton.setText(mConditionWardCodeName);
        mWardButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(InPatientList.this, CommonCode.class);
                intent.putExtra("mode", CommonCode.WARD_CODE);
                intent.putExtra("default", mConditionWardCode);
                startActivityForResult(intent, CommonCode.WARD_CODE);
            }
        });
        // 진료과선택버튼
        mDeptButton = (Button) findViewById(R.id.dept_button);
        mDeptButton.setText(mConditionDeptCodeName);
        mDeptButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(InPatientList.this, CommonCode.class);
                intent.putExtra("mode", CommonCode.DEPT_CODE);
                intent.putExtra("default", mConditionDeptCode);
                startActivityForResult(intent, CommonCode.DEPT_CODE);
            }
        });
        // 의사선택버튼
        mPdridButton = (Button) findViewById(R.id.pdrid_button);
        mPdridButton.setText(mConditionPdridCodeName);
        mPdridButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(InPatientList.this, CommonCode.class);
                intent.putExtra("mode", CommonCode.DOCT_CODE);
                intent.putExtra("default", mConditionPdridCode);
                intent.putExtra("dptcd", mConditionDeptCode);
                startActivityForResult(intent, CommonCode.DOCT_CODE);
            }
        });
        // 진료일자선택버튼(외래환자리스트)
        mOutExdtButton = (Button) findViewById(R.id.out_exdt_button);
        mOutExdtButton.setText("");
        mOutExdtButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                DialogDatePicker();
            }
        });
        // 일자
        Calendar c = Calendar.getInstance();
        mOutExdtYear = c.get(Calendar.YEAR);
        mOutExdtMonth = c.get(Calendar.MONTH);
        mOutExdtDay = c.get(Calendar.DAY_OF_MONTH);
        displayOutExdt();
        // 진료과선택버튼(외래환자리스트)
        mOutDeptButton = (Button) findViewById(R.id.out_dept_button);
        mOutDeptButton.setText(mOutDeptCodeName);
        mOutDeptButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(InPatientList.this, CommonCode.class);
                intent.putExtra("mode", CommonCode.OUT_DEPT_CODE);
                intent.putExtra("default", mOutDeptCode);
                startActivityForResult(intent, CommonCode.OUT_DEPT_CODE);
            }
        });
        // 의사선택버튼(외래환자리스트)
        mOutPdridButton = (Button) findViewById(R.id.out_pdrid_button);
        mOutPdridButton.setText(mOutPdridCodeName);
        mOutPdridButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(InPatientList.this, CommonCode.class);
                intent.putExtra("mode", CommonCode.OUT_DOCT_CODE);
                intent.putExtra("default", mOutPdridCode);
                intent.putExtra("dptcd", mOutDeptCode);
                startActivityForResult(intent, CommonCode.OUT_DOCT_CODE);
            }
        });
        // 옵션에 따라 의사선택버튼 숨김
        if (EmrSettingsUtil.getInPatientListDoctPopupButtonHideYn(getBaseContext())) {
            mPdridButton.setVisibility(View.GONE);
            mOutPdridButton.setVisibility(View.GONE);
        }
        // 정렬순서 선택 버튼(1.환자명 순 2.병동 순 3.진료과+환자명 순)
        mSortOrderButton = (Button) findViewById(R.id.sort_order_button);
        mSortOrderButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                SortOrderDialog dialog = new SortOrderDialog(InPatientList.this);
                dialog.show();
            }
        });
        // 정렬순서 선택 버튼(외래환자리스트)
        mOutSortOrderButton = (Button) findViewById(R.id.out_sort_order_button);
        mOutSortOrderButton.setVisibility(View.GONE);
        // 2026.04.09 WOOIL - 입원예정자만(외래환자리스트)
        mOutRsvInOnlyCheckBox = (CheckBox) findViewById(R.id.out_rsv_in_only_checkbox);
        mOutRsvInOnlyCheckBox.setChecked(false);
        mOutRsvInOnlyCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mOutFirstVisibleItem = 0;
                getPatientList();
            }
        });

        // 2021.09.30 WOOIL - 환자 검색시 사용할 일자선택버튼
        mSearchExdtButton = (Button) findViewById(R.id.search_exdt_button);
        mSearchExdtButton.setText("");
        mSearchExdtButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                SearchExdtDialogDatePicker();
            }
        });
        // 2023.03.27 WOOIL - 검색을 일자 범위로 하도록 종료일 추가
        mSearchExdtToButton = (Button) findViewById(R.id.search_exdt_to_button);
        mSearchExdtToButton.setText("");
        mSearchExdtToButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                SearchExdtToDialogDatePicker();
            }
        });
        // 2025.08.07 WOOIL - 환자 검색시 외래+입원,외래만,입원만 검색할지 선택하는 버튼 추가
        mSearchIofgButton = (Button) findViewById(R.id.search_iofg_button);
        mSearchIofgButton.setText("");
        mSearchIofgButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                SearchIofgDialog dialog = new SearchIofgDialog(InPatientList.this);
                dialog.show();
            }
        });
        dispSearchIofg();
        // 2025.08.07 WOOIL - 화면지움
        mClearButton = (Button) findViewById(R.id.clear_button);
        mClearButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                mPatListView.setAdapter(null);// 리스트 지움.
            }
        });

        // 2021.09.30 WOOIL - 검색용 일자 초기화
        mSearchExdtYear = c.get(Calendar.YEAR);
        mSearchExdtMonth = c.get(Calendar.MONTH);
        mSearchExdtDay = c.get(Calendar.DAY_OF_MONTH);
        displaySearchExdt();
        // 2023.03.27 WOOIL - 검색을 일자 범위로 하도록 종료일 추가
        mSearchExdtToYear = c.get(Calendar.YEAR);
        mSearchExdtToMonth = c.get(Calendar.MONTH);
        mSearchExdtToDay = c.get(Calendar.DAY_OF_MONTH);
        displaySearchExdtTo();
        // 환자검색버튼
        mSearchButton = (Button) findViewById(R.id.search_button);
        mSearchButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                if (mTabHost.getCurrentTabTag().equals(IN_PATIENT_SEARCH)) {
                    String searchText = getSearchText();
                    if (searchText.equals("")) {
                        showNoSearchTextToast();
                        return;
                    }
                }
                // 검색어 입력창의 키보드 내리기
                hideKeyboard();
                // 조회
                getPatientList();
            }
        });
    }

    private void showNoSearchTextToast(){
        Toast t = Toast.makeText(this, "검색어를 입력하세요.", Toast.LENGTH_SHORT);
        t.setDuration(Toast.LENGTH_SHORT);
        t.show();
    }

    private void DialogDatePicker() {
        DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            // onDateSet method
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mOutExdtYear = year;
                mOutExdtMonth = monthOfYear;
                mOutExdtDay = dayOfMonth;
                displayOutExdt();
                getPatientList();
            }
        };
        DatePickerDialog alert = new DatePickerDialog(this, mDateSetListener, mOutExdtYear, mOutExdtMonth, mOutExdtDay);
        alert.show();
    }

    private void displayOutExdt() {
        mOutExdtButton.setText(
                new StringBuilder()
                        // Month is 0 based so add 1
                        .append(mOutExdtYear).append(".")
                        .append(mOutExdtMonth + 1).append(".")
                        .append(mOutExdtDay).append(" ")
        );
    }

    private void dispSearchIofg() {
        String text = "전체"; // 외래+입원
        if(mSearchIofg.equals("1")) text = "외래";
        if(mSearchIofg.equals("2")) text = "입원";
        mSearchIofgButton.setText(text);
    }

    private void SearchExdtDialogDatePicker() {
        DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            // onDateSet method
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mSearchExdtYear = year;
                mSearchExdtMonth = monthOfYear;
                mSearchExdtDay = dayOfMonth;
                displaySearchExdt();
            }
        };
        DatePickerDialog alert = new DatePickerDialog(this, mDateSetListener, mSearchExdtYear, mSearchExdtMonth, mSearchExdtDay);
        alert.show();
    }

    private void displaySearchExdt() {
        mSearchExdtButton.setText(
                new StringBuilder()
                        // Month is 0 based so add 1
                        .append(mSearchExdtYear).append(".")
                        .append(mSearchExdtMonth + 1).append(".")
                        .append(mSearchExdtDay).append(" ")
        );
    }

    private void SearchExdtToDialogDatePicker() {
        DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            // onDateSet method
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mSearchExdtToYear = year;
                mSearchExdtToMonth = monthOfYear;
                mSearchExdtToDay = dayOfMonth;
                displaySearchExdtTo();
            }
        };
        DatePickerDialog alert = new DatePickerDialog(this, mDateSetListener, mSearchExdtToYear, mSearchExdtToMonth, mSearchExdtToDay);
        alert.show();
    }

    private void displaySearchExdtTo() {
        mSearchExdtToButton.setText(
                new StringBuilder()
                        // Month is 0 based so add 1
                        .append(mSearchExdtToYear).append(".")
                        .append(mSearchExdtToMonth + 1).append(".")
                        .append(mSearchExdtToDay).append(" ")
        );
    }

    /*
    private void setListener() {
    	// 정렬순서 확인, 취소버튼 리스너
        final Button applyButton = (Button)mPopupView.findViewById(R.id.apply_button);
        applyButton.setOnClickListener(new Button.OnClickListener() {
        	public void onClick(View view) {
        		getPatientList();
        		mPopup.dismiss();
        	}
        });

        final Button cancelButton = (Button)mPopupView.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new Button.OnClickListener() {
        	public void onClick(View view) {
        		mPopup.dismiss();
        		// 이전 선택을 유지시킨다.
        		RadioGroup rgSort = (RadioGroup)mPopupView.findViewById(R.id.rg_sort);
        		if(mSortOrder.equals("1")){
        			rgSort.check(R.id.rb_sort_pnm);
        		}else if(mSortOrder.equals("2")){
        			rgSort.check(R.id.rb_sort_ward);
        		}else if(mSortOrder.equals("3")){
        			rgSort.check(R.id.rb_sort_dept);
        		}

        	}
        });
    }
    */

    private String getTabText(String tag) {
        String tagText = "";
        if (tag.equals(IN_PATIENT_LIST)) {
            tagText = "재원환자";
            super.setQueryButton(true);
        } else if (tag.equals(OUT_PATIENT_LIST)) {
            tagText = "외래환자";
            super.setQueryButton(true);
        } else if (tag.equals(IN_PATIENT_SEARCH)) {
            tagText = "환자검색";
            super.setQueryButton(false);
        }
        return tagText;
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
        String dptcd = (String) selectedMap.get("dptcd"); // 2019.10.29 WOOIL - 진료과코드
        String ward = (String) selectedMap.get("ward"); // 2021.08.09 WOOIL - 병동코드...입원인지 외래인지 구분하기 위한 용도
        String drid = (String) selectedMap.get("drid"); // 2024.06.21 WOOIL - 의사ID
        String qfycd = (String) selectedMap.get("qfycd"); // 2024.06.24 WOOIL - 자격코드
        String patientinfo = getPatientInfo(selectedMap);

        String bdiv = getBdiv(ward);  // 2021.08.09 WOOIL - ward추가

        switch (idx) {
            case 1: // 처방조회
            case 2: // 기록지조회
            case 3: // 간호기록지조회
            case 4: // 투약기록지조회
            case 5: // 영상진단결과조회
            case 6: // 기능검사결과조회
            case 7: // 진단검사결과조회
            case 8: // 기타서식조회
                intent = new Intent(this, Order.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("fromtitle", getTitle());
                intent.putExtra("inittype", idx);
                intent.putExtra("bdiv", bdiv); // 2021.08.09 WOOIL - ward추가
                intent.putExtra("drid",drid); // 2026.03.18 WOOIL - 추치의 추가
                startActivity(intent);
                break;
            case 301: // TPR 조회
                intent = new Intent(this, TprSheet.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("fromtitle", getTitle());
                startActivity(intent);
                break;
            case 302: // DM 조회
                intent = new Intent(this, DmSheet.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("fromtitle", getTitle());
                startActivity(intent);
                break;
            case 303: // IO 조회
                intent = new Intent(this, IoSheet.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("fromtitle", getTitle());
                startActivity(intent);
                break;
            case 304: // Labor Record 조회
                intent = new Intent(this, LaborRecord.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("fromtitle", getTitle());
                startActivity(intent);
                break;

            case 101: // 동의서
                // 2025.07.16 WOOIL - 외래환자면 접수내역이 취소되었는지 점검한다.

                if ("1".equalsIgnoreCase(bdiv)){
                    checkCancelAndCallConformFormList(pid, bededt, patientinfo, bdiv, dptcd, bedodt, drid, qfycd);
                }
                else{
                    callConformFormList(pid, bededt, patientinfo, bdiv, dptcd, bedodt, drid, qfycd);
                }
                break;
            case 102: // 동의서열람
                intent = new Intent(this, SignedConsentFormList.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("fromtitle", getTitle());
                intent.putExtra("bdiv", bdiv); // 2021.08.09 WOOIL - ward추가
                startActivity(intent);
                break;
            case 103: // 임시저장동의서목록
                intent = new Intent(this, PresavedConsentFormList.class);
                startActivity(intent);
                break;
            case 201: // 입내원이력
                intent = new Intent(this, PatientHosHx.class);
                intent.putExtra("pid", pid);
                intent.putExtra("bededt", bededt);
                intent.putExtra("patientinfo", patientinfo);
                intent.putExtra("fromtitle", getTitle());
                startActivity(intent);
                break;
        }
    }

    private String getBdiv(String ward) {
        String strRet = "";
        if (mTabHost.getCurrentTabTag().equals(IN_PATIENT_LIST)) {
            strRet = "2";
        } else if (mTabHost.getCurrentTabTag().equals(OUT_PATIENT_LIST)) {
            strRet = "1";
        } else {
            strRet = ("외래".equalsIgnoreCase(ward) ? "1" : "2");
        }
        return strRet;
    }

    // 2025.07.16 WOOIL - 동의서리스트 페이지를 호출하는 함수.
    private void callConformFormList(String pid, String bededt, String patientinfo, String bdiv, String dptcd, String bedodt, String drid, String qfycd) {
        Intent intent = null;
        intent = new Intent(this, ConsentFormList.class);
        intent.putExtra("pid", pid);
        intent.putExtra("bededt", bededt);
        intent.putExtra("patientinfo", patientinfo);
        intent.putExtra("fromtitle", getTitle());
        intent.putExtra("bdiv", bdiv);
        intent.putExtra("dptcd", dptcd); // 2019.10.29 WOOIL - 진료과코드
        intent.putExtra("bedodt", bedodt); // 2021.08.10 WOOIL - 퇴원일(외래는 진료일시)
        intent.putExtra("drid", drid); // 2024.06.21 WOOIL - 의사ID
        intent.putExtra("qfycd", qfycd); // 2024.06.24 WOOIL - 자격코드
        startActivity(intent);
    }

    // 2025.07.16 WOOIL - 접수내역이 취소되었는지 검사하고 취소되지 않았으면 동의서리스트 페이지로 넘어간다.
    private void checkCancelAndCallConformFormList(final String pid, final String bededt, final String patientinfo, final String bdiv, final String dptcd, final String bedodt, final String drid, final String qfycd) {
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
                param.put("pid", pid);
                param.put("exdt", bededt);
                param.put("dptcd", dptcd);
                param.put("hms", bedodt);
                mCXml = getXml("InPatientListServlet", param);
                // 종료
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterCheckCancelAndCallConformFormList(pid, bededt, patientinfo, bdiv, dptcd, bedodt, drid, qfycd);
                            mDialog.dismiss();
                        } catch (Exception ex) {
                        }
                    }
                });
            }
        }).start();
    }

    private void afterCheckCancelAndCallConformFormList(final String pid, final String bededt, final String patientinfo, final String bdiv, final String dptcd, final String bedodt, final String drid, final String qfycd) {
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
                    callConformFormList(pid, bededt, patientinfo, bdiv, dptcd, bedodt, drid, qfycd);
                }
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        // 2022.03.04 WOOIL - 메뉴를 POPUP MENU로 변경하여 이곳은 막음.
        //
        //// 메뉴를 등록하는 부분
        //// 메뉴를 선택하여 호출하는 부분은 callActivity에 있음.
        //super.onCreateContextMenu(menu, view, menuInfo);
        //
        //ListView listView = (ListView)view;
        //int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
        //mSelectedMap = (HashMap<String,Object>)listView.getAdapter().getItem(position);
        //
        //String pnm = (String)mSelectedMap.get("pnm");
        //String psexage = (String)mSelectedMap.get("psexage");
        //
        ////menu.setHeaderTitle("Menu " + pnm + " " + psexage);
        //
        //menu.add(0,1,0,"처방조회");      // 처방조회
        //menu.add(0,2,0,"기록지조회");
        //menu.add(0,3,0,"투약기록지조회");
        //menu.add(0,4,0,"영상진단결과"); // 영상진단결과
        //menu.add(0,5,0,"진단검사결과"); // 진단검사결과
        //menu.add(0,6,0,"기타서식조회");   // 기타서식
        //
        //// 재원환자 조회시만 보이도록
        //if(mTabHost.getCurrentTabTag().equals(IN_PATIENT_LIST)) {
        //	menu.add(0,7,0,"TPR조회");        // TPR조회
        //	menu.add(0,8,0,"DM조회");         // DM조회
        //	menu.add(0,9,0,"IO조회");         // DM조회
        //}
        //menu.add(0,13,0,"입내원이력");   // 입내원이력
        //
        //// 동의서목록은 TEMR은 기본이 아니다.
        //// 개발자 디바이스에서는 동의서목록 메뉴가 보이게 처리
        //if(getPackageName().equalsIgnoreCase(EmrSettingsUtil.PACKAGE_MEMR)){
        //	if(EmrSettingsUtil.getCertificateHideYn(getBaseContext())==false){
        //		menu.add(0,10,0,"동의서목록"); // 동의서목록
        //		menu.add(0,11,0,"동의서열람"); // 동의서열람
        //		menu.add(0,12,0,"임시저장동의서목록"); // 임시저장동의서목록
        //	}
        //}
        //
        //LayoutInflater layout = getLayoutInflater();
        //View v = layout.inflate(R.layout.custom_dialog_title_bar, null);
        //TextView tv = (TextView)v.findViewById(R.id.custom_dialog_title_bar_text);
        //tv.setText(pnm + " " + psexage);
        //menu.setHeaderView(v);
        //
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // 2022.03.04 WOOIL - 메뉴를 POPUP MENU로 변경하여 이곳은 막음.
        //
        //AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)item.getMenuInfo();
        //int position = ((AdapterView.AdapterContextMenuInfo)menuInfo).position;
        //HashMap<String,Object> selectedMap = (HashMap<String,Object>)mPatListView.getAdapter().getItem(position);
        //
        //callActivity(item.getItemId(),selectedMap);
        //
        return true;
    }

    /*
    private String getSortOrder() {
		String sortOrder="1";
		RadioGroup rgSort = (RadioGroup)mPopupView.findViewById(R.id.rg_sort);
		int checkedId = rgSort.getCheckedRadioButtonId();
		if(checkedId==R.id.rb_sort_pnm){
			sortOrder="1";
		}else if(checkedId==R.id.rb_sort_ward){
			sortOrder="2";
		}else if(checkedId==R.id.rb_sort_dept){
			sortOrder="3";
		}
    
		return sortOrder;
    }
    */
    // 환자검색조건
    private String getSearchText() {
        EditText text = (EditText) findViewById(R.id.search_text);
        return text.getText().toString();
    }

    private void setSearchText(String s) {
        EditText text = (EditText) findViewById(R.id.search_text);
        text.setText(s);
    }

    // 재원환자리스트
    private void getPatientList() {
        // 환자 검색인데 검색어가 없으면 종료한다.
        if (mTabHost.getCurrentTabTag().equals(IN_PATIENT_SEARCH)) {
            String searchText = getSearchText();
            if(searchText.equals("")) return;
        }
//    	mLockListView=true;
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                HashMap<String, String> param = new HashMap<String, String>();
                param.clear();
                String hospitalId = getHospitalId();
                String userId = getUserId();
                //String sortOrder=mSortOrder;//getSortOrder();
                if (mTabHost.getCurrentTabTag().equals(IN_PATIENT_LIST)) {
                    // 재원환자조회
                    //mXml="";
                    mInXml = "";
                    param.put("hospitalid", hospitalId);
                    param.put("userid", userId);
                    param.put("sortorder", mSortOrder);
                    param.put("mode", "0");
                    param.put("ward", mConditionWardCode);
                    param.put("dept", mConditionDeptCode);
                    param.put("pdrid", mConditionPdridCode);
                    mInXml = getXml("InPatientListServlet", param);
                } else if (mTabHost.getCurrentTabTag().equals(OUT_PATIENT_LIST)) {
                    // 외래환자조회
                    mOutXml = "";
                    param.put("hospitalid", hospitalId);
                    param.put("userid", userId);
                    param.put("sortorder", mOutSortOrder);
                    param.put("mode", "outp");
                    param.put("exdt", getOutExdt());
                    param.put("dept", mOutDeptCode);
                    param.put("pdrid", mOutPdridCode);
                    param.put("rsv_in_only", isOutRsvInOnly() ? "y" : "");
                    mOutXml = getXml("InPatientListServlet", param);
                } else {
                    // 환자이름으로검색
                    mSearchXml = "";
                    String searchText = getSearchText();
                    param.put("hospitalid", hospitalId);
                    param.put("userid", userId);
                    param.put("sortorder", mSortOrder);
                    param.put("mode", "1");
                    param.put("searchtext", getHangul(searchText));
                    param.put("exdt", getSearchExdt());
                    param.put("exdtto", getSearchExdtTo());
                    param.put("searchiofg", mSearchIofg);
                    mSearchXml = getXml("InPatientListServlet", param);
                }
                // 종료
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            //mXml=makeXml(mInXml);
                            afterGetPatientList(true);
                            mDialog.dismiss();
//	    					mLockListView=false;
                        } catch (Exception ex) {
//    						mLockListView=false;
                        }
                    }
                });
            }
        }).start();
    }

    private String getOutExdt() {
        String yy = new StringBuilder().append(mOutExdtYear).toString();
        String mm = new StringBuilder().append(mOutExdtMonth + 1).toString();// Month is 0 based so add 1
        String dd = new StringBuilder().append(mOutExdtDay).toString();
        if (mm.length() == 1) mm = "0" + mm;
        if (dd.length() == 1) dd = "0" + dd;
        StringBuilder exdt = new StringBuilder().append(yy).append(mm).append(dd);
        return exdt.toString();
    }

    private String getSearchExdt() {
        String yy = new StringBuilder().append(mSearchExdtYear).toString();
        String mm = new StringBuilder().append(mSearchExdtMonth + 1).toString();// Month is 0 based so add 1
        String dd = new StringBuilder().append(mSearchExdtDay).toString();
        if (mm.length() == 1) mm = "0" + mm;
        if (dd.length() == 1) dd = "0" + dd;
        StringBuilder exdt = new StringBuilder().append(yy).append(mm).append(dd);
        return exdt.toString();
    }

    private String getSearchExdtTo() {
        String yy = new StringBuilder().append(mSearchExdtToYear).toString();
        String mm = new StringBuilder().append(mSearchExdtToMonth + 1).toString();// Month is 0 based so add 1
        String dd = new StringBuilder().append(mSearchExdtToDay).toString();
        if (mm.length() == 1) mm = "0" + mm;
        if (dd.length() == 1) dd = "0" + dd;
        StringBuilder exdt = new StringBuilder().append(yy).append(mm).append(dd);
        return exdt.toString();
    }

    private String makeXml(String xml) {
        return xml;
//    	//Log.d("EmrDroid","makeXml : mPageNo="+mPageNo);
//    	String retXml="";
//    	// 1페이지 검색이면 xml을 그대로 반환
//    	// 2페이지 이상이면 mXml과 합쳐서 반환
//    	// 조회된 건이 0건이면 mPageNo를 0으로 만들고 아니면 +1 한다.
//    	if(mPageNo==1){
//    		retXml = xml;
//    		mPageNo++;
//    		return retXml;
//    	}
//
//		try {
//			int count=ResultSetHelper.getRecordCount(xml);
//			if(count<0){
//				retXml = xml; // 오류가 있으면 오류를 표시하기 위해
//				mPageNo=0; // 검색종료
//				mFirstVisibleItem=0;
//				//Log.d("EmrDroid","makeXml : 오류");
//			}else if(count==0){
//				retXml = mXml; // 더이상 건수가 없으면 기존자료 유지
//				mPageNo=0; // 검색종료
//				//Log.d("EmrDroid","makeXml : 건수0");
//			}else{
//				retXml = ResultSetHelper.concateResultSet(mXml, xml);
//				mPageNo++;
//				//Log.d("EmrDroid","makeXml : 정상");
//			}
//		} catch (JSONException e) {
//			// TODO Auto-generated catch block
//			retXml = xml; // 오류가 있으면 오류를 표시하기 위해
//			mPageNo=0; // 검색종료
//			mFirstVisibleItem=0;
//			//Log.d("EmrDroid","makeXml : Exception");
//			//Log.d("EmrDroid","makeXml : " + e.getMessage());
//		}
//		//Log.d("EmrDroid","retXml=" + retXml);
//		return retXml;
    }

    private String getXmlString() {
        String xml = "";
        if (mTabHost.getCurrentTabTag().equals(IN_PATIENT_LIST)) {
            xml = mInXml;
        } else if (mTabHost.getCurrentTabTag().equals(OUT_PATIENT_LIST)) {
            xml = mOutXml;
        } else {
            xml = mSearchXml;
        }
        if (xml == null) xml = ""; // 혹시나
        return xml;
    }

    private void afterGetPatientList(boolean showQueryCountMessage) {
        ResultSetHelper rs;

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
            mPatListView.setAdapter(null);
            // 조회결과값
            String xml = getXmlString();
            //Log.d("EmrDroid","after : xml=" + xml);
            if (xml.equals("")) return;
            // xml to ResultSet
            rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                if (showQueryCountMessage) {
                    Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();
                }

                boolean isDoctDeptnm = EmrSettingsUtil.getInPatientListDoctDeptnm(getBaseContext());
                for (int i = 0; i < rs.getRecordCount(); i++) {

                    String psex = rs.getString(i, "psex");
                    String bthdt = rs.getString(i, "bthdt");
                    int ageY = DateUtil.getAgeYear(bthdt);

                    map = new HashMap<String, Object>();
                    map.put("image", psex.equals("M") ? R.drawable.man_icon : R.drawable.woman_icon);
                    map.put("pnm", rs.getString(i, "pnm"));
                    map.put("psexage", rs.getString(i, "psex") + "/" + ageY);
                    map.put("dptcd", rs.getString(i, "dptcd"));
                    map.put("drid", rs.getString(i, "drid")); // 2024.06.21 WOOIL - 의사ID
                    map.put("ward", rs.getString(i, "ward"));
                    if (isDoctDeptnm) {
                        map.put("pdrnm", rs.getString(i, "dptnm"));
                    } else {
                        map.put("pdrnm", rs.getString(i, "pdrnm"));
                    }
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
                    if (bedodt.equals("")) disp_bedodt = "재원중";
                    String disp_bededt_bedodt = disp_bededt + "~" + disp_bedodt;
                    // 외래는 재원중이라는 말을 표시하지 않는다.
                    if (mTabHost.getCurrentTabTag().equals(OUT_PATIENT_LIST)) {
                        String hms = rs.getString(i, "hms");
                        hms = hms.substring(0, 4);
                        disp_bededt_bedodt = disp_bededt + " " + super.getFormattedTime(hms);
                    }
                    if (mTabHost.getCurrentTabTag().equals(IN_PATIENT_SEARCH)) {
                        // 환자검색된 내역중 외래내역
                        if (bedodt.length() == 6) {
                            String hms = bedodt;
                            hms = hms.substring(0, 4);
                            disp_bededt_bedodt = disp_bededt + " " + super.getFormattedTime(hms);
                        }
                    }
                    map.put("disp_bededt_bedodt", disp_bededt_bedodt);
                    //
                    mylist.add(map);
                }
                SimpleAdapter adapter = new SimpleAdapter(this, mylist, R.layout.in_patient_list_row,
                        new String[]{"image", "pnm", "psexage", "dptcd", "ward", "pdrnm", "qfycdnm", "disp_bededt_bedodt", "dxd", "pid"},
                        new int[]{R.id.patient_list_row_image
                                , R.id.patient_list_row_pnm
                                , R.id.patient_list_row_psexage
                                , R.id.patient_list_row_dptcd
                                , R.id.patient_list_row_ward
                                , R.id.patient_list_row_pdrnm
                                , R.id.patient_list_row_qfycdnm
                                , R.id.patient_list_row_disp_bededt_bedodt
                                , R.id.patient_list_row_dxd
                                , R.id.patient_list_row_pid
                        });

                mPatListView.setAdapter(adapter);

                // 재원환자리스트이고
                if (mTabHost.getCurrentTabTag().equals(IN_PATIENT_LIST) && mFirstVisibleItem > 0) {
                    mPatListView.setSelection(mFirstVisibleItem);
                } else if (mTabHost.getCurrentTabTag().equals(OUT_PATIENT_LIST) && mOutFirstVisibleItem > 0) {
                    mPatListView.setSelection(mOutFirstVisibleItem);
                }
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO Auto-generated method stub
        // ListView 클릭시 처리방법을 잊지않기 위해 만들어 둠.
        // 2011.10.31 WOOIL - 일단 막음.
        //mSelectedMap = (HashMap<String,Object>)(parent.getAdapter().getItem(position));
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // TODO Auto-generated method stub
//		// 현재 가장 처음에 보이는 셀번호와 보여지는 셀번호를 더한값이
//		// 전체의 숫자와 동일해지면 가장 아래로 스크롤 되었다고 가정합니다.
        if (mTabHost.getCurrentTabTag().equals(IN_PATIENT_LIST)) {
            mFirstVisibleItem = firstVisibleItem;
        } else if (mTabHost.getCurrentTabTag().equals(OUT_PATIENT_LIST)) {
            mOutFirstVisibleItem = firstVisibleItem;
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // TODO Auto-generated method stub
//		if(scrollState == 0) Log.d("EmrDroid", "scrolling stopped..."); // 정지상태
//		else mButtonsLayout.setVisibility(View.INVISIBLE); // 움직임
        hideKeyboard();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void onTabChanged(String tabId) {
        // TODO Auto-generated method stub
        EmrSettingsUtil.setCurrentTabId(getBaseContext(), tabId);

        setMyTitle(getTabText(mTabHost.getCurrentTabTag()));
        hideKeyboard();
        if (mInXml == null) mInXml = "";
        if (mOutXml == null) mOutXml = "";
        if (tabId.equals(IN_PATIENT_LIST) && mInXml.equals("")) {
            getPatientList();
        } else if (tabId.equals(OUT_PATIENT_LIST) && mOutXml.equals("")) {
            getPatientList();
        } else {
            afterGetPatientList(false);
        }
    }

    private class SearchIofgDialog extends Dialog {

        RadioGroup radioGroup;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.in_patient_list_search_iofg_dialog);

            // 제목
            TextView tv = (TextView) findViewById(R.id.custom_dialog_title_bar_text);
            tv.setText("입원 외래 선택");

            radioGroup = (RadioGroup) findViewById(R.id.rg_search_iofg);
            initRadioButton();
            setListener();
        }
        public SearchIofgDialog(@NonNull Context context) {
            super(context);
        }
        private void initRadioButton() {
            RadioButton radio;
            if (mSearchIofg.equals("0")) {
                radio = (RadioButton) findViewById(R.id.rb_search_iofg_0);
                radio.setChecked(true);
            } else if (mSearchIofg.equals("1")) {
                radio = (RadioButton) findViewById(R.id.rb_search_iofg_1);
                radio.setChecked(true);
            } else if (mSearchIofg.equals("2")) {
                radio = (RadioButton) findViewById(R.id.rb_search_iofg_2);
                radio.setChecked(true);
            }
        }
        private void setListener() {
            final Button applyButton = (Button) findViewById(R.id.apply_button);
            applyButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View view) {
                    int checkedId = radioGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.rb_search_iofg_0) {
                        mSearchIofg = "0";
                        EmrSettingsUtil.setSearchIofg(getBaseContext(), mSearchIofg);
                    } else if (checkedId == R.id.rb_search_iofg_1) {
                        mSearchIofg = "1";
                        EmrSettingsUtil.setSearchIofg(getBaseContext(), mSearchIofg);
                    } else if (checkedId == R.id.rb_search_iofg_2) {
                        mSearchIofg = "2";
                        EmrSettingsUtil.setSearchIofg(getBaseContext(), mSearchIofg);
                    }
                    dismiss();
                    dispSearchIofg();
                }
            });
            final Button cancelButton = (Button) findViewById(R.id.cancel_button);
            cancelButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View view) {
                    dismiss();
                }
            });
        }
    }

    private class SortOrderDialog extends Dialog {
        // 이 다이얼로그의 타이틀은 trp_color_dialog.xml에 있음.
        RadioGroup radioGroup;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.in_patient_list_sort_dialog);

            // 제목
            TextView tv = (TextView) findViewById(R.id.custom_dialog_title_bar_text);
            tv.setText("정렬순서");

            radioGroup = (RadioGroup) findViewById(R.id.rg_sort);
            initRadioButton();
            setListener();
        }

        public SortOrderDialog(Context context) {
            super(context);
        }

        private void initRadioButton() {
            RadioButton radio;
            if (mTabHost.getCurrentTabTag().equals(OUT_PATIENT_LIST)) {
                // 외래접수환자리스트조회
                findViewById(R.id.rb_out_sort_exdt).setVisibility(View.VISIBLE);
                findViewById(R.id.rb_out_sort_pnm).setVisibility(View.VISIBLE);
                findViewById(R.id.rb_out_sort_dptcd).setVisibility(View.VISIBLE);
                findViewById(R.id.rb_out_sort_dptcd_pnm).setVisibility(View.VISIBLE);
                // 재원환자용
                findViewById(R.id.rb_sort_pnm).setVisibility(View.GONE);
                findViewById(R.id.rb_sort_ward).setVisibility(View.GONE);
                findViewById(R.id.rb_sort_dept).setVisibility(View.GONE);
                findViewById(R.id.rb_sort_bed_in_date_time).setVisibility(View.GONE); // 입원일시
            } else {
                // 외래접수환자리스트조회
                findViewById(R.id.rb_out_sort_exdt).setVisibility(View.GONE);
                findViewById(R.id.rb_out_sort_pnm).setVisibility(View.GONE);
                findViewById(R.id.rb_out_sort_dptcd).setVisibility(View.GONE);
                findViewById(R.id.rb_out_sort_dptcd_pnm).setVisibility(View.GONE);
                // 재원환자용
                findViewById(R.id.rb_sort_pnm).setVisibility(View.VISIBLE);
                findViewById(R.id.rb_sort_ward).setVisibility(View.VISIBLE);
                findViewById(R.id.rb_sort_dept).setVisibility(View.VISIBLE);
                findViewById(R.id.rb_sort_bed_in_date_time).setVisibility(View.VISIBLE); // 입원일시
            }
            if (mTabHost.getCurrentTabTag().equals(OUT_PATIENT_LIST)) {
                if (mOutSortOrder.equals("1")) {
                    radio = (RadioButton) findViewById(R.id.rb_out_sort_exdt);
                    radio.setChecked(true);
                } else if (mOutSortOrder.equals("2")) {
                    radio = (RadioButton) findViewById(R.id.rb_out_sort_pnm);
                    radio.setChecked(true);
                } else if (mOutSortOrder.equals("3")) {
                    radio = (RadioButton) findViewById(R.id.rb_out_sort_dptcd);
                    radio.setChecked(true);
                } else if (mOutSortOrder.equals("4")) {
                    radio = (RadioButton) findViewById(R.id.rb_out_sort_dptcd_pnm);
                    radio.setChecked(true);
                }
            } else {
                if (mSortOrder.equals("1")) {
                    radio = (RadioButton) findViewById(R.id.rb_sort_pnm);
                    radio.setChecked(true);
                } else if (mSortOrder.equals("2")) {
                    radio = (RadioButton) findViewById(R.id.rb_sort_ward);
                    radio.setChecked(true);
                } else if (mSortOrder.equals("3")) {
                    radio = (RadioButton) findViewById(R.id.rb_sort_dept);
                    radio.setChecked(true);
                } else if (mSortOrder.equals("4")) {
                    radio = (RadioButton) findViewById(R.id.rb_sort_bed_in_date_time);
                    radio.setChecked(true);
                }
            }
        }

        private void setListener() {
            final Button applyButton = (Button) findViewById(R.id.apply_button);
            applyButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View view) {
                    int checkedId = radioGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.rb_sort_pnm) {
                        mSortOrder = "1";
                        EmrSettingsUtil.setSortOrder(getBaseContext(), mSortOrder);//mSortOrderButton.setText("환자명 순");
                    } else if (checkedId == R.id.rb_sort_ward) {
                        mSortOrder = "2";
                        EmrSettingsUtil.setSortOrder(getBaseContext(), mSortOrder);//mSortOrderButton.setText("병동 순");
                    } else if (checkedId == R.id.rb_sort_dept) {
                        mSortOrder = "3";
                        EmrSettingsUtil.setSortOrder(getBaseContext(), mSortOrder);//mSortOrderButton.setText("진료과+환자명 순");
                    } else if (checkedId == R.id.rb_sort_bed_in_date_time) {
                        mSortOrder = "4";
                        EmrSettingsUtil.setSortOrder(getBaseContext(), mSortOrder);//mSortOrderButton.setText("입원일시 순");
                    } else if (checkedId == R.id.rb_out_sort_exdt) {
                        mOutSortOrder = "1";
                        EmrSettingsUtil.setOutSortOrder(getBaseContext(), mOutSortOrder);
                    } else if (checkedId == R.id.rb_out_sort_pnm) {
                        mOutSortOrder = "2";
                        EmrSettingsUtil.setOutSortOrder(getBaseContext(), mOutSortOrder);
                    } else if (checkedId == R.id.rb_out_sort_dptcd) {
                        mOutSortOrder = "3";
                        EmrSettingsUtil.setOutSortOrder(getBaseContext(), mOutSortOrder);
                    } else if (checkedId == R.id.rb_out_sort_dptcd_pnm) {
                        mOutSortOrder = "4";
                        EmrSettingsUtil.setOutSortOrder(getBaseContext(), mOutSortOrder);
                    }
//    				mPageNo=1;
//    				mFirstVisibleItem=0;
                    getPatientList();
                    dismiss();
                }
            });
            final Button cancelButton = (Button) findViewById(R.id.cancel_button);
            cancelButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View view) {
                    dismiss();
                }
            });
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO Auto-generated method stub
        mSelectedMap = (HashMap<String, Object>) (parent.getAdapter().getItem(position));
        //String pnm = (String)mSelectedMap.get("pnm");
        //String psexage = (String)mSelectedMap.get("psexage");

        TextView tv = (TextView) view.findViewById(R.id.patient_list_row_disp_bededt_bedodt);
        PopupMenu menu = new PopupMenu(InPatientList.this, tv);

        //menu.getMenu().add(pnm+" "+psexage);

        // 2022.12.15 WOOIL - 동의서 관련 메뉴를 앞쪽으로 올린다.(이용구 상무님 지시사항)
        menu.getMenu().add(0, 101, 0, "동의서목록"); // 동의서목록
        menu.getMenu().add(0, 102, 0, "동의서열람"); // 동의서열람
        menu.getMenu().add(0, 103, 0, "임시저장동의서목록"); // 임시저장동의서목록

        menu.getMenu().add(0, 1, 0, "처방조회");      // 처방조회
        menu.getMenu().add(0, 2, 0, "기록지조회");
        menu.getMenu().add(0, 3, 0, "간호기록지조회");
        menu.getMenu().add(0, 4, 0, "투약기록지조회");
        menu.getMenu().add(0, 5, 0, "영상진단결과"); // 영상진단결과
        menu.getMenu().add(0, 6, 0, "기능검사결과"); // 기능검사결과
        menu.getMenu().add(0, 7, 0, "진단검사결과"); // 진단검사결과
        menu.getMenu().add(0, 8, 0, "기타서식조회");   // 기타서식

        // 재원환자 조회시만 보이도록
        if (mTabHost.getCurrentTabTag().equals(IN_PATIENT_LIST)) {
            menu.getMenu().add(0, 301, 0, "TPR조회");         // TPR조회
            menu.getMenu().add(0, 302, 0, "DM조회");         // DM조회
            menu.getMenu().add(0, 303, 0, "IO조회");         // DM조회
            menu.getMenu().add(0, 304, 0, "Labor Record");  // Labor Record DM조회
        }
        menu.getMenu().add(0, 201, 0, "입내원이력");   // 입내원이력


		/* 2022.12.15 WOOIL - TEMR 부분은 삭제한다.
		// 동의서목록은 TEMR은 기본이 아니다.
		// 개발자 디바이스에서는 동의서목록 메뉴가 보이게 처리
		if(getPackageName().equalsIgnoreCase(EmrSettingsUtil.PACKAGE_MEMR)){
			if(EmrSettingsUtil.getCertificateHideYn(getBaseContext())==false){
				menu.getMenu().add(0,11,0,"동의서목록"); // 동의서목록
				menu.getMenu().add(0,12,0,"동의서열람"); // 동의서열람
				menu.getMenu().add(0,13,0,"임시저장동의서목록"); // 임시저장동의서목록
			}
		}
		*/

        menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // TODO Auto-generated method stub
                callActivity(item.getItemId(), mSelectedMap);
                return false;
            }

        });

        menu.show();

        return false;
    }
    private boolean isOutRsvInOnly() {
        return mOutRsvInOnlyCheckBox != null
                && mOutRsvInOnlyCheckBox.isChecked();
    }}
