package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.PictureDrawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.annotation.UiThread;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.data.CcfValue;
import com.metrosoft.smart.emr.emrdroid.gt101.data.CcfValues;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.pdf.PdfFormRuntimeWriter;
import com.metrosoft.smart.emr.emrdroid.gt101.pdf.PdfInkPdfSaver;
import com.metrosoft.smart.emr.emrdroid.gt101.pdf.PdfInkSignView;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Device;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Utils;
import com.metrosoft.smart.emr.emrdroid.gt101.view.FingerPaintView3;
import com.metrosoft.smart.emr.emrdroid.gt101.z_practice.ShowImage;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ConsentForm extends MyActivity implements OnCheckedChangeListener, OnClickListener, OnItemSelectedListener {
    private final int CALL_CAMERA = 1;

    private static final int REQ_SELECT_DOCTOR = 2001;

    private Activity mActivity;

    private long backKeyClick = 0;
    private long backKeyClickTime;

    private String mXml;
    private String[] mCcfValueXml = new String[15];
    private String mSaveXml;
    private String mCcfFileName;
    private String mFullUrl;
    private String mOpHxXml; // 2023.03.07 WOOIL - 수술이력을 담은 XML

    //private String mHosLogoImageUrl;
    private Handler handler = new Handler();

    private MediaRecorder recorder;

    private String mCcfId;
    private String mCcfName;
    private String mPreSaved;
    private String mPid;
    private String mBededt;
    private String mBdiv;
    private String mDptcd; // 2019.10.29 WOOIL - 진료과코드
    private String mBedodt; // 2021.08.10 WOOIL - 퇴원일자(외래는 진료일시)
    private String mExdt;
    private String mSeq;
    private String mEmrScanClass;
    private String mSubPageList; // 2020.05.20 WOOIL - 페이지 리스트
    private String mPreSavedBdiv; // 2022.03.03 WOOIL - 임시저장되었던 BDIV
    private String mHxType; // 2023.03.06 WOOIL - 수술 이력등 종류
    private String mDrid; // 2024.06.21 WOOIL - 의사ID
    private String mQfycd; // 2024.06.24 WOOIL - 자격
    private String mReSaveYn; // 2026.02.04 WOOIL - 동의서를 다시 작성하는지 여부
    //private String mXmlPatientInfo;

    private String mU01PkYn; // 2023.03.07 WOOIL - TU01의 PK사용여부
    private String mU01Opdt; // 2023.03.07 WOOIL - TU01의 PK
    private String mU01Dptcd; // 2023.03.07 WOOIL - TU01의 PK
    private String mU01Opseq; // 2023.03.07 WOOIL - TU01의 PK
    private String mU01Seq; // 2023.03.07 WOOIL - TU01의 PK

    private String mDongExdt; // 2024.08.26 WOOIL - 특정 진료일 자료를 구하고자 할 경우의 일자(비급여 동의서에서 사용하려고 시작)

    private int mPageCount;
    //int mSignStatus;
    private TextView mTitle;
    private WebView mWebView;
    //	private MyView mMyView;
    private ArrayList<MyView> mMyViewList = new ArrayList<MyView>();
    //	private ScaleImageView mSiView; // 확대축소용
    private Button mRecordButton;
    private Button mPicButton;
    private RadioGroup mPenGroup;
    private RadioButton mRadioPen;
    private RadioButton mRadioEraser;
    private RadioButton mRadioNone;
    private Button mUndoSignButton;
    private Spinner mPenWidthSpinner;
    private ArrayList<String> penWidthList = new ArrayList<String>();
    private Spinner mEraserWidthSpinner;
    private ArrayList<String> eraserWidthList = new ArrayList<String>();
    private Spinner mPenColorSpinner; // 2021.08.25 WOOIL - 펜색
    private ArrayList<String> penColorList = new ArrayList<String>(); // 2021.08.25 WOOIL - 선택 가능한 펜색

    // 2020.04.02 동의서가 여러장인 경우 처리 5장 까지 가능함.
    private RadioGroup mPageGroup;
    private RadioButton mRadioPage1;
    private RadioButton mRadioPage2;
    private RadioButton mRadioPage3;
    private RadioButton mRadioPage4;
    private RadioButton mRadioPage5;
    private RadioButton mRadioPage6;
    private RadioButton mRadioPage7;
    private RadioButton mRadioPage8;
    private RadioButton mRadioPage9;
    private RadioButton mRadioPage10;
    private RadioButton mRadioPage11;
    private RadioButton mRadioPage12;
    private RadioButton mRadioPage13;
    private RadioButton mRadioPage14;
    private RadioButton mRadioPage15;
    private EditText mApplyExdt; // 2026.02.02 WOOIL - 사용자가 수정하여 저장할 수 있게
    private TextView mApplyDrnmLabel; // 2026.02.10 WOOIL - 사용자가 의사를 수정할 수 있게
    private EditText mApplyDrnm; // 2026.02.10 WOOIL - 사용자가 의사를 수정할 수 있게

    private boolean mIsPdfConsent = false; // 2026.04.14 WOOIL - PDF용 추가
    private ArrayList<PdfInkSignView> mPdfViewList = new ArrayList<PdfInkSignView>(); // 2026.04.14 WOOIL - PDF용 추가
    private ArrayList<String> mPdfFilePathList = new ArrayList<String>(); // 2026.04.14 WOOIL - PDF용 추가

    private Thread.UncaughtExceptionHandler mDefaultUncaughtExceptionHandler;
    public Thread.UncaughtExceptionHandler getDefaultUncaughtExceptionHandler() {
        return mDefaultUncaughtExceptionHandler;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.consent_form, getString(R.string.certificate_list));

        mActivity = this;

        mDefaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); // 세로로 고정

        mU01PkYn = ""; // 2023.03.07 WOOIL - TU01의 PK사용여부
        mU01Opdt = ""; // 2023.03.07 WOOIL - TU01의 PK
        mU01Dptcd = ""; // 2023.03.07 WOOIL - TU01의 PK
        mU01Opseq = ""; // 2023.03.07 WOOIL - TU01의 PK
        mU01Seq = ""; // 2023.03.07 WOOIL - TU01의 PK

        mDongExdt = ""; // 2024.08.26 WOOIL - 특정 진료일 자료를 구하고자 할 경우의 일자(비급여 동의서에서 사용하려고 시작)

        mPic = null;
        m_mapPic = null; // 촬영용이미지보관용
        m_mapMP4 = null; // 녹음파일보관용

        Intent intent = getIntent();
        mCcfId = intent.getStringExtra("ccfId");
        mCcfName = intent.getStringExtra("ccfName");
        mPreSaved = intent.getStringExtra("preSaved"); // 임시저장동의서에서 넘어온 경우 Y
        mCcfFileName = intent.getStringExtra("ccfFileName");
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mBdiv = intent.getStringExtra("bdiv");
        mDptcd = intent.getStringExtra("dptcd"); // 2019.10.29 WOOIL - 진료과코드
        mBedodt = intent.getStringExtra("bedodt"); // 2021.08.10 WOOIL - 퇴원일자(외래는 진료일시)
        mExdt = intent.getStringExtra("exdt");
        mSeq = intent.getStringExtra("seq");
        mEmrScanClass = intent.getStringExtra("emrScanClass");
        mSubPageList = intent.getStringExtra("subPageList");
        mPreSavedBdiv = intent.getStringExtra("preSavedBdiv");
        mHxType  = intent.getStringExtra("hx_type"); // 수술이력, 일자선택 등
        mDrid = intent.getStringExtra("drid"); // 2024.06.21 WOOIL - 의사ID
        mQfycd = intent.getStringExtra("qfycd"); // 2024.06.24 WOOIL - 자격
        mReSaveYn = intent.getStringExtra("re_save_yn"); // 2026.02.04 WOOIL - 동의서를 다시 작성하는지 여부

        // 호출하는 곳에서 값을 넘지지 않았을 경우 이상 동작 방지
        if (mReSaveYn == null) mReSaveYn = "";

        // 2026.04.14 WOOIL - 동의서 파일이 PDF 문서인지 담아놓는다.
        mIsPdfConsent = isPdfConsentFile();

        mPageCount = 1;
        if (!"".equals(mSubPageList)) {
            // 여러 페이지로 이루어진 동의서이면
            String pageList[] = mSubPageList.split(";");
            mPageCount = pageList.length + 1;
        }

        //mTitle = (TextView) findViewById(R.id.certificate_title);
        //mTitle.setText(mCcfName);
        mWebView = (WebView) findViewById(R.id.certificate_view);
        // mWebView.setVisibility(View.GONE);
        // webView.getSettings().setLoadWithOverviewMode(true);
        // webView.getSettings().setUseWideViewPort(true);
        // webView.getSettings().setJavaScriptEnabled(true); // 웹뷰에서 자바스크립트실행가능.
        mWebView.setWebViewClient(new CertificatePaperWebViewClient());
        // mWebView.setWebChromeClient(new CertificatePaperWebChromeClient());
        //
        mRecordButton = (Button) findViewById(R.id.record_button);
        mPicButton = (Button) findViewById(R.id.pic_button);
        mPenGroup = (RadioGroup) findViewById(R.id.pen_group);
        mPenGroup.setOnCheckedChangeListener(this);
        mRadioPen = (RadioButton) findViewById(R.id.radio_pen);
        mRadioEraser = (RadioButton) findViewById(R.id.radio_eraser);
        mRadioNone = (RadioButton) findViewById(R.id.radio_none);

        mRadioNone.setVisibility(View.GONE);     // 확대축소 모드 숨기기(공간차지X)
        //
        mUndoSignButton = (Button) findViewById(R.id.undo_sign_button);

        // 펜두께 스피터
        for (int i = 0; i < 10; i++) {
            penWidthList.add("펜두께 " + (i + 1));
            eraserWidthList.add("지우개두께 " + (i + 1));
        }
        // 2021.08.25 WOOIL - 펜색 스피너
        penColorList.add("검정");
        penColorList.add("파랑");
        penColorList.add("빨강");
        // 펜
        mPenWidthSpinner = (Spinner) findViewById(R.id.pen_width_spinner);
        //mPenWidthSpinner.setBackgroundColor(getDrawingCacheBackgroundColor());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, penWidthList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPenWidthSpinner.setAdapter(adapter);
        int penWidth = EmrSettingsUtil.getCcfPenWidth(mActivity);
        mPenWidthSpinner.setSelection(penWidth - 1);
        mPenWidthSpinner.setOnItemSelectedListener(this);
        //mPenWidthSpinner.setVisibility(View.GONE); // 2024.01.16 WOOIL - 살림 // 사용하지 말자
        // 지우개
        mEraserWidthSpinner = (Spinner) findViewById(R.id.eraser_width_spinner);
        //mPenWidthSpinner.setBackgroundColor(getDrawingCacheBackgroundColor());
        ArrayAdapter<String> eraserAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, eraserWidthList);
        eraserAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mEraserWidthSpinner.setAdapter(eraserAdapter);
        int eraserWidth = EmrSettingsUtil.getCcfEraserWidth(mActivity);
        mEraserWidthSpinner.setSelection(eraserWidth - 1);
        mEraserWidthSpinner.setOnItemSelectedListener(this);
        mEraserWidthSpinner.setVisibility(View.GONE); // 일단 숨기고 시작
        // 2021.08.25 WOOIL - 펜색
        mPenColorSpinner = (Spinner) findViewById(R.id.pen_color_spinner);
        ArrayAdapter<String> colorAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, penColorList);
        colorAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mPenColorSpinner.setAdapter(colorAdapter);
        String penColor = EmrSettingsUtil.getCcfPenColor(mActivity);
        int penColorIndex = 0;
        if ("검정".equalsIgnoreCase(penColor)) penColorIndex = 0;
        else if ("파랑".equalsIgnoreCase(penColor)) penColorIndex = 1;
        else if ("빨강".equalsIgnoreCase(penColor)) penColorIndex = 2;
        mPenColorSpinner.setSelection(penColorIndex);
        mPenColorSpinner.setOnItemSelectedListener(this);

        // 2020.04.02 동의서가 여러장인 경우 처리 15장 까지 가능함.
        mPageGroup = (RadioGroup) findViewById(R.id.page_group);
        mPageGroup.setOnCheckedChangeListener(this);
        mRadioPage1 = (RadioButton) findViewById(R.id.page_1);
        mRadioPage2 = (RadioButton) findViewById(R.id.page_2);
        mRadioPage3 = (RadioButton) findViewById(R.id.page_3);
        mRadioPage4 = (RadioButton) findViewById(R.id.page_4);
        mRadioPage5 = (RadioButton) findViewById(R.id.page_5);
        mRadioPage6 = (RadioButton) findViewById(R.id.page_6);
        mRadioPage7 = (RadioButton) findViewById(R.id.page_7);
        mRadioPage8 = (RadioButton) findViewById(R.id.page_8);
        mRadioPage9 = (RadioButton) findViewById(R.id.page_9);
        mRadioPage10= (RadioButton) findViewById(R.id.page_10);
        mRadioPage11 = (RadioButton) findViewById(R.id.page_11);
        mRadioPage12 = (RadioButton) findViewById(R.id.page_12);
        mRadioPage13 = (RadioButton) findViewById(R.id.page_13);
        mRadioPage14 = (RadioButton) findViewById(R.id.page_14);
        mRadioPage15 = (RadioButton) findViewById(R.id.page_15);
        mRadioPage1.setVisibility(View.VISIBLE);   // 처음에는 1페이지만 보이게 함
        mRadioPage2.setVisibility(View.GONE); // 일단 2페이지 안보이게
        mRadioPage3.setVisibility(View.GONE); // 일단 3페이지 안보이게
        mRadioPage4.setVisibility(View.GONE); // 일단 4페이지 안보이게
        mRadioPage5.setVisibility(View.GONE); // 일단 5페이지 안보이게
        mRadioPage6.setVisibility(View.GONE); // 일단 6페이지 안보이게
        mRadioPage7.setVisibility(View.GONE); // 일단 7페이지 안보이게
        mRadioPage8.setVisibility(View.GONE); // 일단 8페이지 안보이게
        mRadioPage9.setVisibility(View.GONE); // 일단 9페이지 안보이게
        mRadioPage10.setVisibility(View.GONE); // 일단 10페이지 안보이게
        mRadioPage11.setVisibility(View.GONE); // 일단 11페이지 안보이게
        mRadioPage12.setVisibility(View.GONE); // 일단 12페이지 안보이게
        mRadioPage13.setVisibility(View.GONE); // 일단 13페이지 안보이게
        mRadioPage14.setVisibility(View.GONE); // 일단 14페이지 안보이게
        mRadioPage15.setVisibility(View.GONE); // 일단 15페이지 안보이게

        if (mPageCount >= 2) mRadioPage2.setVisibility(View.VISIBLE);
        if (mPageCount >= 3) mRadioPage3.setVisibility(View.VISIBLE);
        if (mPageCount >= 4) mRadioPage4.setVisibility(View.VISIBLE);
        if (mPageCount >= 5) mRadioPage5.setVisibility(View.VISIBLE);
        if (mPageCount >= 6) mRadioPage6.setVisibility(View.VISIBLE);
        if (mPageCount >= 7) mRadioPage7.setVisibility(View.VISIBLE);
        if (mPageCount >= 8) mRadioPage8.setVisibility(View.VISIBLE);
        if (mPageCount >= 9) mRadioPage9.setVisibility(View.VISIBLE);
        if (mPageCount >= 10) mRadioPage10.setVisibility(View.VISIBLE);
        if (mPageCount >= 11) mRadioPage11.setVisibility(View.VISIBLE);
        if (mPageCount >= 12) mRadioPage12.setVisibility(View.VISIBLE);
        if (mPageCount >= 13) mRadioPage13.setVisibility(View.VISIBLE);
        if (mPageCount >= 14) mRadioPage14.setVisibility(View.VISIBLE);
        if (mPageCount >= 15) mRadioPage15.setVisibility(View.VISIBLE);

        // 적용일 입력칸
        mApplyExdt = (EditText) findViewById(R.id.apply_exdt);

        // 키보드 안 뜨게 (직접 입력 방지)
        mApplyExdt.setFocusable(false);
        mApplyExdt.setClickable(true);
        mApplyExdt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePickerDialog();
            }
        });
        // 서버 현재일자 불러오기
        initApplyDate();

        // 주치의 입력칸
        mApplyDrnmLabel = (TextView) findViewById(R.id.apply_drnm_label);
        mApplyDrnmLabel.setVisibility(View.GONE); // 일단 안보이게...
        mApplyDrnm = (EditText) findViewById(R.id.apply_drnm);
        mApplyDrnm.setText("");
        mApplyDrnm.setVisibility(View.GONE); // 일단 안보이게...
        // 키보드 안 뜨게 (직접 입력 방지)
        mApplyDrnm.setFocusable(false);
        mApplyDrnm.setClickable(true);
        mApplyDrnm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDrnmDialog();
            }
        });


        if ("Y".equalsIgnoreCase(mPreSaved) || "Y".equalsIgnoreCase(mReSaveYn)) {
            setButton1(false, "", BUTTON_TYPE_NONE);
        }else if("수술이력".equalsIgnoreCase(mHxType)){
            setButton1(true, mHxType, BUTTON_TYPE_NONE);
        }else if("일자선택".equalsIgnoreCase(mHxType) && "2".equalsIgnoreCase(mBdiv)){
            // 2024.08.26 WOOIL - 입원만
            //setButton1(true, mHxType, BUTTON_TYPE_NONE);
            // 2024.08.29 WOOIL - 오늘 날짜로 설정한다.
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            Date now = new Date();
            mDongExdt = sdf.format(now);
            setButton1(true, mDongExdt, BUTTON_TYPE_NONE);
        }else{
            setButton1(false, "", BUTTON_TYPE_NONE);
        }
        setButton2(true, "저장", BUTTON_TYPE_SAVE);
        if ("Y".equalsIgnoreCase(mReSaveYn)) {
            // 다시 저장하는 중이면 임시저장 기능을 없앤다.
        }else {
            setButton3(true, "임시저장", BUTTON_TYPE_SAVE);
        }

        mRecordButton.setOnClickListener(this);
        mPicButton.setOnClickListener(this);
        mUndoSignButton.setOnClickListener(this);


        // 제목표시줄 밑에 있는 TEMR 로그를 TEMR 페키지만 보이도록 처리
		/*
        String packageName = getPackageName();
        RelativeLayout topBgLayout = (RelativeLayout)findViewById(R.id.top_bg_layout);
        if(!packageName.equals(EmrSettingsUtil.PACKAGE_TEMR)){
        	topBgLayout.setVisibility(View.GONE);
        }
		*/

        //LinearLayout buttonLayout = (LinearLayout) findViewById(R.id.button_layout);
        //buttonLayout.setVisibility(View.GONE);

		/* 2022.12.15 WOOIL - 별도 처리 부분 막음
		if("SHW-M380S".equalsIgnoreCase(Build.MODEL)){
			// 숨김
			LinearLayout bottomLayout  = (LinearLayout) findViewById(R.id.bottom_layout);
			bottomLayout.setVisibility(View.GONE);
		}
		*/

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.view_layout);
        LayoutParams layoutParams = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);

        // 사인용 뷰 추가
		/*
		mMyView = new MyView(this, penWidth, eraserWidth);
		mMyView.setLayoutParams(layoutParams);
		linearLayout.addView(mMyView, linearLayout.getChildCount()-1);
		mMyView.setVisibility(View.GONE);
		*/

        //int penColorValue = getResources().getColor(R.color.pencolor);
        // 2021.08.25 WOOIL - 저장된 펜색을 초기치로 설정한다.
        int penColorValue = getResources().getColor(R.color.pencolor);
        if ("검정".equalsIgnoreCase(penColor)) penColorValue = Color.BLACK;
        else if ("파랑".equalsIgnoreCase(penColor)) penColorValue = Color.BLUE;
        else if ("빨강".equalsIgnoreCase(penColor))
            penColorValue = Color.rgb(255, 50, 50); // RED_COLOR

        String userId = getUserId();

        // 2024.05.23 WOOIL - 펜
        Paint penPaint = new Paint();
        penPaint.setAntiAlias(true);
        penPaint.setDither(true);
        penPaint.setColor(penColorValue); // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
        penPaint.setStyle(Paint.Style.STROKE);
        penPaint.setStrokeJoin(Paint.Join.ROUND);
        penPaint.setStrokeCap(Paint.Cap.ROUND);
        penPaint.setStrokeWidth(penWidth);

        // 2024.05.23 WOOIL - 화면을 지울 때 사용
        Paint clearPaint = new Paint();
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        // 2024.05.23 WOOIL
        Paint cursorPaint = new Paint();
        cursorPaint.setColor(penColorValue);
        cursorPaint.setStyle(Paint.Style.STROKE);

        // 2024.05.23 WOOIL - 환자 정보등을 출력하기 위한 용도
        Paint textPaint = new Paint();
        textPaint.setTextSize(getPixelFromDip(16.0f));
        textPaint.setColor(Color.BLACK);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < mPageCount; i++) {
            MyView v = new MyView(this, penWidth, eraserWidth, penColorValue, penPaint, clearPaint, cursorPaint, textPaint, userId);
            // 2026.02.10 WOOIL - 리스터 추가
            v.setOnCcfValueChangedListener(new FingerPaintView3.OnCcfValueChangedListener() {
                @Override
                public void onCcfValueChanged(int index, CcfValue value) {
                    // 2026.02.11 WOOIL - 임시 저장한 것이거나 저장한 것을 다시 불러온 경우에는 보이지 않게 한다.
                    if ("Y".equalsIgnoreCase(mPreSaved) || "Y".equalsIgnoreCase(mReSaveYn)) {
                        return;
                    }
                    // 2026.02.10 WOOIL - 의사 정보이면 의사를 변경하는 기능이 보이게...
                    String field = value.getField();
                    if ("drnm".equalsIgnoreCase(field) || "drnm_eng".equalsIgnoreCase(field)) { // 주치의 명(영문명)
                        // 의사명(영문명)을 보여준다.
                        setApplyDrnm(value.getValue());
                        mApplyDrnmLabel.setVisibility(View.VISIBLE); // 의사 정보를 보여주고 선택하는 창을 띄우자
                        mApplyDrnm.setVisibility(View.VISIBLE); // 의사 정보를 보여주고 선택하는 창을 띄우자
                    }
                }
            });
            // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있는 기능 추가
            mMyViewList.add(v);
            mMyViewList.get(i).setLayoutParams(layoutParams);
            linearLayout.addView(mMyViewList.get(i), linearLayout.getChildCount() - 1);
            mMyViewList.get(i).setVisibility(View.GONE);
            // 2026.04.14 WOOIL - PDF 문서를 처리하는 VIEW 추가
            PdfInkSignView pdfView = new PdfInkSignView(this);
            pdfView.setLayoutParams(layoutParams);
            pdfView.setVisibility(View.GONE);
            mPdfViewList.add(pdfView);
            linearLayout.addView(pdfView, linearLayout.getChildCount() - 1);
        }


        // 확대축소용 뷰 추가
		/*
		mSiView = new ScaleImageView(this);
		mSiView.setLayoutParams(layoutParams);
		linearLayout.addView(mSiView, linearLayout.getChildCount()-1);
		mSiView.setVisibility(View.GONE);
		*/

        if (savedInstanceState == null) {
            if ("Y".equalsIgnoreCase(mPreSaved) || "Y".equalsIgnoreCase(mReSaveYn)) {
                getPreSavedCertificatePaper();
            } else {
                getCertificatePaper();
            }
        } else {
            int cnt = 0;
            cnt = savedInstanceState.getInt("m_mapPicCount");
            if (cnt == 0) {
                m_mapPic = null;
            } else {
                if (m_mapPic == null) m_mapPic = new HashMap<Integer, Object>();
                for (int i = 1; i <= cnt; i++) {
                    m_mapPic.put(i, "");
                }
            }
            setPicButtonText("촬영");
            cnt = savedInstanceState.getInt("m_mapMP4Count");
            if (cnt == 0) {
                m_mapMP4 = null;
            } else {
                if (m_mapMP4 == null) m_mapMP4 = new HashMap<Integer, Object>();
                for (int i = 1; i <= cnt; i++) {
                    m_mapMP4.put(i, "");
                }
            }
            setRecordButtonText("녹음");
            mXml = savedInstanceState.getString("xml");
            afterGetCertificatePaper();
        }
    }

    private float getPixelFromDip(float dipValue){
        return Utils.getPixelFromDip(getBaseContext(), dipValue);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("xml", mXml);
        if (m_mapPic == null) {
            outState.putInt("m_mapPicCount", 0);
        } else {
            outState.putInt("m_mapPicCount", m_mapPic.size());
        }
        if (m_mapMP4 == null) {
            outState.putInt("m_mapMP4Count", 0);
        } else {
            outState.putInt("m_mapMP4Count", m_mapMP4.size());
        }
    }

    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        final int duration = 2000;
        backKeyClick++;
        if (backKeyClick == 1) {
            backKeyClickTime = System.currentTimeMillis();
            Toast t = Toast.makeText(this, R.string.close_confirm_message, Toast.LENGTH_SHORT);
            t.setDuration(Toast.LENGTH_SHORT);
            t.show();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(duration);
                    } catch (InterruptedException e) {}
                    backKeyClick = 0;
                }
            }).start();
        } else {
            Intent i = new Intent();
            setResult(RESULT_CANCELED, i);
            deleteTempFile();
            finish();
        }
    }
    @Override
    public void onClickBackButton(View v) {
        Intent i = new Intent();
        setResult(RESULT_CANCELED, i);
        deleteTempFile();
        finish();
    }

    private void deleteTempFile(){
        if (Build.VERSION.SDK_INT >= 30) {
            //String filenames = "";
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File[] files = storageDir.listFiles();
            for(File file : files){
                if(file.getName().startsWith("JPEG_")) {
                    //filenames += file.getName() + ",";
                    file.delete();
                }
            }
            //EmrSettingsUtil.setUncaughtExceptionMessage(mActivity, filenames);
        }
    }

    @Override
    public void onClickQueryButton(View v) {
        m_mapPic = null; // 촬영용이미지보관용
        m_mapMP4 = null; // 녹음파일보관용

        //getCertificatePaper();
        if ("Y".equalsIgnoreCase(mPreSaved) || "Y".equalsIgnoreCase(mReSaveYn)) {
            getPreSavedCertificatePaper();
        } else {
            getCertificatePaper();
        }

    }

    @Override
    public void onClickButton1(View v) {
        // 각종 이력 조회
        if("수술이력".equalsIgnoreCase(mHxType)){
            // 수술이력
            getOpHx();
        }else if("일자선택".equalsIgnoreCase(mHxType)){
            // 일자선택
            getExdtList();
        }
    }

    @Override
    public void onClickButton2(View v) {
        // 저장하기

        // 2023.04.03 WOOIL - 고도일병원에서 저장하기 전에 저장여부를 물어보도록 요청.
        String hospitalId = getHospitalId();
        boolean useSaveYnButton = false;
        if("0036".equalsIgnoreCase(hospitalId)==true) useSaveYnButton = true; // 고도일
        if("0017".equalsIgnoreCase(hospitalId)==true) useSaveYnButton = true; // 2024.08.28 WOOIL - 예손

        if(useSaveYnButton==false) {
            // 고도일 이외
            saveSignImage("");
        }else {
            // 고도일 병원
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("확인");
            dialog.setMessage("저장하시겠습니까?");
            dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    saveSignImage("");
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
    }

    @Override
    public void onClickButton3(View v) {
        // 임시저장하기

        // 2023.04.03 WOOIL - 고도일병원에서 저장하기 전에 저장여부를 물어보도록 요청.
        String hospitalId = getHospitalId();
        boolean useSaveYnButton = false;
        if("0036".equalsIgnoreCase(hospitalId)==true) useSaveYnButton = true; // 고도일
        if("0017".equalsIgnoreCase(hospitalId)==true) useSaveYnButton = true; // 2024.08.28 WOOIL - 예손

        if(useSaveYnButton==false) {
            // 고도일 이외
            saveSignImage("Y");
        }else {
            // 고도일 병원
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle("확인");
            dialog.setMessage("임시저장하시겠습니까?");
            dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    saveSignImage("Y");
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
    }

    // 2026.02.02 WOOIL - 저장일을 현재일자(서버시간)으로 초기 설정함
    private void initApplyDate() {

        // 2026.02.04 WOOIL - 임시저장동의서(실 저장동의서 포함)를 다시 받는 것이면 임시저장 동의서의 저장일이 기본으로 되게0
        // 2026.03.09 WOOIL - 허리나은병원은 항상 현재일자로 설정.
        String hospitalId = getHospitalId();
        if("0133".equalsIgnoreCase(hospitalId)==true){
            // 2026.03.09 WOOIL - 허리나은병원
        }else {
            if ("Y".equalsIgnoreCase(mPreSaved) || "Y".equalsIgnoreCase(mReSaveYn)) {
                setApplyExdt(mExdt);
                return;
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    String hospitalId = getHospitalId();
                    String userId = getUserId();

                    HashMap<String, String> param = new HashMap<String, String>();
                    param.clear();
                    param.put("mode", "2");   // 현재일자, seq 발급 모드
                    param.put("hospitalid", hospitalId);
                    param.put("userid", userId);
                    param.put("pid", mPid);

                    param.put("ccfId", "");
                    param.put("bededt", "");
                    param.put("presave", "");
                    param.put("bdiv", "");

                    // 서버 호출
                    final String xml = getXml("CertificatePaperServlet", param);
                    if(xml==null){
                        return;
                    }else {
                        // XML 파싱해서 sysdt 추출
                        ResultSetHelper rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));

                        final String sysdt = rs.getString(0, "sysdt");
                        setApplyExdt(sysdt);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    // 2025.02.02 WOOIL - 저장일 선택 기능
    private void showDatePickerDialog() {

        final Calendar calendar = Calendar.getInstance();

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(
                ConsentForm.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {

                        // month는 0부터 시작하므로 +1
                        int realMonth = monthOfYear + 1;

                        // YYYYMMDD 형식으로 만들기
                        String date = String.format("%04d%02d%02d", year, realMonth, dayOfMonth);

                        mApplyExdt.setText(date);
                    }
                },
                year, month, day);

        dialog.show();
    }

    // 저장/임시저장 시 서버에 넘길 exdt (mApplyData 역할)
    private String getApplyExdt() {
        try {
            if (mApplyExdt != null) {
                String v = mApplyExdt.getText().toString();
                if (v != null) v = v.trim();
                if (v != null && v.length() == 8) return v;   // YYYYMMDD
            }
        } catch (Exception ignore) {}
        // 적용일이 비어있으면 기존 exdt로 fallback
        return "";
    }

    private void setApplyExdt(final String applyDate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (applyDate != null && applyDate.length() == 8) {
                    mApplyExdt.setText(applyDate);
                }
            }
        });
    }

    // 2026.02.10 WOOIL - 의사 정보를 띄워서 변경하자.
    private void showDrnmDialog() {
        try {
            String applyDrnm = mApplyDrnm.getText().toString();
            Intent intent = new Intent(ConsentForm.this, CommonCode.class);
            intent.putExtra("mode", CommonCode.DOCT_CODE);   // 의사선택
            intent.putExtra("dptcd", "");                // 의사선택 시 진료과 필터
            intent.putExtra("default", applyDrnm);               // 기본 선택(현재 의사)

            startActivityForResult(intent, REQ_SELECT_DOCTOR);
        } catch (Exception ex) {
            Log.d("EmrDroid", "showDrnmDialog error=" + ex.getMessage());
            showSimpleDialogThread(ex.getMessage());
        }
    }

    private void setApplyDrnm(final String applyDrnm){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mApplyDrnm.setText(applyDrnm);
            }
        });
    }

    private void stopRecord() {
        if (recorder == null) return;

        recorder.stop();
        recorder.release();
        recorder = null;

        mRecording = false;
    }

    private void startRecord() {
        try {
            if (recorder != null) {
                recorder.stop();
                recorder.release();
                recorder = null;
            }
            recorder = new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
            recorder.setOutputFile(getRecordFileName());

            recorder.prepare();
            recorder.start();
            mRecording = true;
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            Toast.makeText(mActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            Toast.makeText(mActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG);
        } catch (Exception e) {
            Toast.makeText(mActivity, e.getLocalizedMessage(), Toast.LENGTH_LONG);
        }
    }

    private HashMap<Integer, Object> m_mapMP4 = null;

    private String getRecordFileName() {
        if (m_mapMP4 == null) m_mapMP4 = new HashMap<Integer, Object>();

        Integer idx = m_mapMP4.size() + 1;
        m_mapMP4.put(idx, "");

        String dirPath = getFilesDir().getAbsolutePath();
        String fileName = dirPath + "/consentform_" + idx + ".mp4";
        return fileName;
    }


    private Bitmap pictureDrawable2Bitmap(PictureDrawable pictureDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(pictureDrawable.getIntrinsicWidth()
                , pictureDrawable.getIntrinsicHeight()
                , Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawPicture(pictureDrawable.getPicture());
        return bitmap;
    }
	
	/*
	private Bitmap compressBitmap(Bitmap bitmap){
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 40, stream);
		byte[] byteArray = stream.toByteArray();
		Bitmap compressedBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
		return compressedBitmap;
	}
	*/
	
	/*
	private Bitmap getResizeBitmap(Bitmap bitmap){
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		byte[] byteArray = stream.toByteArray();
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inSampleSize = 6;
		Bitmap resizeBitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
		return resizeBitmap;
	}
	*/

	private void setDongExdtText(final  String text) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                setButton1(true, text, BUTTON_TYPE_NONE);
            }

        });
    }
    private void setRecordButtonText(final String text) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                String dispText = text;
                if (m_mapMP4 != null) {
                    int idx = m_mapMP4.size();
                    dispText = text + "(" + idx + ")";
                }
                mRecordButton.setText(dispText);
            }

        });
    }

    private void setPicButtonText(final String text) {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                String dispText = text;
                if (m_mapPic != null) {
                    int idx = m_mapPic.size();
                    dispText = text + "(" + idx + ")";
                }
                mPicButton.setText(dispText);
            }

        });
    }

    private void showSimpleDialogThread(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                showSimpleDialog(msg);
            }
        });
    }

    private String mFileName;

    private void saveSignImage(final String preSave) {
        // 2026.01.14 WOOIL - PDF 동의서 처리
        //if (mIsPdfConsent) {
        //    savePdfConsent(preSave);
        //    return;
        //}
        // 사인이미지저장.
        //mDialog = ProgressDialog.show(this, "", getString(R.string.process_wait_message), true);
        showProgressDialog(getString(R.string.process_wait_message));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //boolean recording = mRecording; // 녹음했는지 여부...
                    // 녹음 자동중지
                    if (mRecording) {
                        stopRecord();
                        setRecordButtonText("녹음");
                    }

                    String dirPath = getFilesDir().getAbsolutePath();
                    File dir = new File(dirPath);

                    // 폴더가 없으면 생성
                    if (!dir.exists()) {
                        Log.d("EmrDroid", "폴더생성");
                        dir.mkdirs();
                    }

                    // 파일을 png로 할지 jpg로 할지 결정
                    String ccfImagePostfix = getCcfImagePostfix();

                    int width = 0;
                    int height = 0;
                    float initScale = 0;


                    ArrayList<String> savedPdfList = new ArrayList<String>();
                    if (mIsPdfConsent) {
                        // 2026.04.14 WOOIL - 페이지별 PDF를 각각 저장
                        for (int pageIdx = 0; pageIdx < mPageCount; pageIdx++) {
                            setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 이미지(PDF) 추출 중 입니다①.");

                            PdfInkSignView pdfView = mPdfViewList.get(pageIdx);
                            File srcPdf = new File(mPdfFilePathList.get(pageIdx));
                            File outPdf = new File(dirPath + File.separator + "consentform_" + pageIdx + ".pdf");

                            PdfInkPdfSaver.saveAllPages(ConsentForm.this, srcPdf, outPdf, pdfView);
                            savedPdfList.add(outPdf.getAbsolutePath());
                        }
                    } else {
                        // 안드로이드 단말기에 에 사인 이미지 파일을 쓴다.
                        for (int pageIdx = 0; pageIdx < mPageCount; pageIdx++) {

                            setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 이미지 추출 중 입니다①.");

                            String fileName = dirPath + File.separator + "consentform" + "_" + pageIdx + "." + ccfImagePostfix;
                            mFileName = fileName; // 디버깅용.
                            Log.d("EmrDroid", "fileName=" + fileName);

                            setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 이미지 추출 중 입니다②.");

                            mMyViewList.get(pageIdx).postInvalidate();

                            setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 이미지 추출 중 입니다③.");

                            // 2021.06.03 WOOIL - 동의서가 여러 페이지인 경우 열어보지 않은 페이지의 이미지를 추출하려고 하면 오류가 발생함.
                            //                    오류 방지용으로 getSignedBitmap에서 열어보지 않은 페이지면 별도의 작업을 함.
                            //                    이때 width와 height가 필요하여 맨 앞페이지의 width와 height를 넘김.
                            // 2022.04.20 WOOIL - 동의서의 두번째 페이지를 열어보지 않으면 값이 좌측상단에 표시되는 현상 수정
                            //                    initScale을 넘김
                            if (pageIdx == 0) {
                                width = mMyViewList.get(pageIdx).getFrameWidth();
                                height = mMyViewList.get(pageIdx).getFrameHeight();
                                initScale = mMyViewList.get(pageIdx).getInitSacle();
                            }
                            Bitmap bitmap = mMyViewList.get(pageIdx).getSignedBitmap(width, height, initScale);
                            setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 이미지 추출 중 입니다④.");
                            Log.d("EmrDroid", "이미지 추출완료");
                            if (bitmap != null) {

                                setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 이미지 추출 중 입니다⑤.");

                                FileOutputStream output = new FileOutputStream(fileName);
                                if ("jpg".equalsIgnoreCase(ccfImagePostfix)) {
                                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output);
                                } else {
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
                                }
                                output.flush();
                                output.close();

                                setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 이미지 추출 중 입니다⑥.");

                                if (!bitmap.isRecycled()) {
                                    bitmap.recycle();
                                }

                                setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 이미지 추출 중 입니다⑦.");

                                // 2024.08.01 WOOIL - 저장까지 다 성공한 후에 하도록 이동
                                //mMyViewList.get(pageIdx).recycleAll(); // 2021.08.20 WOOIL - 메모리 해제
                            }

                            Log.d("EmrDroid", "fileName 단말기에 저장 완료");

                        }
                    }
                    // finished - 안드로이드에 사인 이미지(or pdf) 파일을 쓴다.

                    setDialogMessage("파일명을 정하는 중 입니다.");

                    // ------------------------------------------------------------------------------------------------------------------------
                    // 2021.12.23 WOOIL - 위에 있는 for문에서 이미지를 단말기에 저장해놓고 이곳에서는 서버에 올린다.
                    //                    왜냐하면 서버에 올리는 기간을 최대한 단축하기 위해서임.
                    // 2024.03.20 WOOIL - 모든 동의서 페이지 이미지를 서버에 올리고
                    //                    오류가 없으면 동의서 정보를 데이터에비스에 저장한다.
                    String hospitalId = getHospitalId();
                    String userId = getUserId();
                    // 2026.02.02 WOIL - 사용자가 저장일을 변경할 수 있다.
                    String applyExdt = getApplyExdt();
                    // 2023.03.20 WOOIL - 저장할 SEQ를 불러온다.
                    //                    첫 페이지에 사용할 SEQ를 구하기 위해 서버에 한번만 갔다오고 두번째 페이지 부터는 SEQ를 1씩 증가시키자
                    //                    FOR 문 안에 if(pageIdx==0)이면 동작하도록 되어있었던 것을 이곳으로 옮겼음.
                    HashMap<String, String> param = new HashMap<String, String>();
                    // 서버에 저장할 파일명을 가져온다.
                    // 2026.02.04 WOOIL - 이곳에는 reSaveYn을 넘기지 않아도 됨.
                    param.clear();
                    param.put("hospitalid", hospitalId);
                    param.put("userid", userId);
                    param.put("ccfId", mCcfId);
                    param.put("pid", mPid);
                    param.put("bededt", mBededt);
                    param.put("bdiv", mBdiv);
                    param.put("mode", "2");
                    param.put("presave", preSave); // Y이면 임시저장
                    param.put("apply_exdt", applyExdt); // 2026.02.12 WOOIL - 사용자가 저장일을 변경할 수 있다.

                    mSaveXml = getXml("CertificatePaperServlet", param);

                    if (mSaveXml == null) {
                        handler.post(new Runnable() {
                            public void run() {
                                mDialog.dismiss();
                                showSaveXml();
                            }
                        });
                        return;
                    }

                    ResultSetHelper rs = new ResultSetHelper(mSaveXml, EmrSettingsUtil.getMaskYn(getBaseContext()));
                    // 사인된 동의서 이미지 파일
                    String sysdt = rs.getString(0, "sysdt");
                    String systm = rs.getString(0, "systm");
                    //int seq = rs.getInt(0, "seq"); // 여기서는 사용하지 않는다.

                    // ------------------------------------------------------------------------------------------------------------------------
                    // 초기화
                    String[] uploadFileName = new String[mPageCount];
                    String[] mp4UploadFileList = new String[mPageCount];
                    String[] picUploadFileList = new String[mPageCount];
                    for (int pageIdx = 0; pageIdx < mPageCount; pageIdx++) {
                        uploadFileName[pageIdx] = "";
                        mp4UploadFileList[pageIdx] = "";
                        picUploadFileList[pageIdx] = "";
                    }

                    // ------------------------------------------------------------------------------------------------------------------------
                    // 2023.03.20 WOOIL - 페이지 별로 저장할 파일명을 만들어 놓는다.
                    //                    서버에 같은 이름으로 파일이 있으면 덮어쓰지 못하는 병원(나무병원)이 있어서 파일이 있는지 여부를 검사한다.
                    String ipAddress = Device.getIpAddress();
                    if (ipAddress == null) ipAddress = "null";
                    ipAddress = ipAddress.replace(".", "_");
                    int imageSeq = 0;
                    String inoutdiv = "I";
                    if (mBdiv.equals("1")) inoutdiv = "O";
                    for (int pageIdx = 0; pageIdx < mPageCount; pageIdx++) {

                        setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 파일명을 검증하는 중 입니다.");

                        while (true) {
                            // 저장폴더 : 년도(YYYY)/일자(YYYYMMDD)/환자ID/입원외래구분
                            // 2026.02.02 WOOIL - sysdt -> applyExdt, systm앞에 sysdt 추가
                            uploadFileName[pageIdx] =
                                    applyExdt.substring(0, 4) + "\\" +
                                    applyExdt + "\\" +
                                    mPid + "\\" +
                                    inoutdiv + "\\" +
                                    "ZZ01" + "_" + sysdt + "_" + systm + "_" + ipAddress + "_" + Integer.toString((++imageSeq) + 1000).substring(1);
                            if ("Y".equalsIgnoreCase(preSave)) {
                                uploadFileName[pageIdx] += "_" + preSave.trim();
                            }
                            if (mIsPdfConsent) {
                                uploadFileName[pageIdx] += ".pdf";
                            } else {
                                uploadFileName[pageIdx] += "." + ccfImagePostfix;
                            }
                            // 서버에 파일이 있는지 확인한다. 반환값이 Y이면 파일이 있는 것임. seq를 하나 증가시켜 다시 확인한다.
                            // 2026.02.04 WOOIL - 이곳에는 reSaveYn을 넘기지 않아도 됨.
                            param.clear();
                            param.put("mode", "18");
                            param.put("hospitalid", hospitalId);
                            param.put("userid", userId);
                            param.put("pid", mPid);
                            param.put("file_name", uploadFileName[pageIdx].replace("\\", "/"));
                            param.put("file_type", mIsPdfConsent ? "pdf" : getCcfImagePostfix()); // 2026.04.14 WOOIL - PDF추가
                            param.put("pre_save", preSave);

                            mSaveXml = getXml("CertificatePaperServlet", param);
                            if (mSaveXml == null) {
                                handler.post(new Runnable() {
                                    public void run() {
                                        mDialog.dismiss();
                                        showSaveXml();
                                    }
                                });
                                return;
                            }

                            String ynStr = mSaveXml;
                            if (!ynStr.startsWith("Y")) break; // 없으면(Y가 아니면) 탈출

                        }
                    }

                    // ------------------------------------------------------------------------------------------------------------------------
                    // 파일을 서버에 올린다.
                    for (int pageIdx = 0; pageIdx < mPageCount; pageIdx++) {

                        setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 파일을 서버에 저장하는 중 입니다.");

                        // 단말기에 저장된 이미지 파일명(페이지별)
                        String fileName = "";
                        if (mIsPdfConsent) {
                            fileName = dirPath + "/consentform" + "_" + pageIdx + ".pdf"; // 2026.04.14 WOOIL - PDF 추가
                        } else {
                            fileName = dirPath + "/consentform" + "_" + pageIdx + "." + ccfImagePostfix;
                        }

                        // 서버에 파일을 올린다.
                        String addParam ="";
                        if (mIsPdfConsent) {
                            addParam = "file_type=pdf&pre_save=" + preSave + "&hospital_id=" + hospitalId;
                        } else {
                            addParam = "file_type=png&pre_save=" + preSave + "&hospital_id=" + hospitalId;
                        }
                        mSaveXml = uploadPngFile(fileName, uploadFileName[pageIdx].replace("\\", "/"), addParam);
                        if (mSaveXml == null) mSaveXml = "파일 업로드 중 오류가 발생했습니다.";
                        if (!mSaveXml.startsWith("success")) {
                            handler.post(new Runnable() {
                                public void run() {
                                    mDialog.dismiss();
                                    showSaveXml();
                                }
                            });
                            Log.d("EmrDroid", "uploadPngFile error=" + mSaveXml);
                            return;
                        }

                        // 녹음 파일을 서버에 올린다.
                        // 녹음이 되었으면...
                        // 첫번째 페이지를 올릴때만 녹음 파일을 올린다.
                        mp4UploadFileList[pageIdx] = ""; // 콤마리스트임.
                        if (m_mapMP4 != null && pageIdx == 0) {
                            for (int i = 1; i <= m_mapMP4.size(); i++) {
                                String mp4FileName = dirPath + "/consentform_" + i + ".mp4";
                                String mp4UploadFile = uploadFileName[pageIdx] + "." + i + ".mp4";
                                addParam = "file_type=mp4&pre_save=" + preSave + "&hospital_id=" + hospitalId;
                                mSaveXml = uploadPngFile(mp4FileName, mp4UploadFile.replace("\\", "/"), addParam);
                                if ("".equalsIgnoreCase(mp4UploadFileList[pageIdx])) {
                                    mp4UploadFileList[pageIdx] = mp4UploadFile;
                                } else {
                                    mp4UploadFileList[pageIdx] += "," + mp4UploadFile;
                                }
                                if (mSaveXml == null) mSaveXml = "파일 업로드 중 오류가 발생했습니다(mp4).";
                                if (!mSaveXml.startsWith("success")) {
                                    handler.post(new Runnable() {
                                        public void run() {
                                            mDialog.dismiss();
                                            showSaveXml();
                                        }
                                    });
                                    return;
                                }
                            }
                        }
                        // end of 녹음 파일을 서버에 올린다.

                        // 사진파일을 서버에 올린다.
                        // 촬영한게 있으면...
                        // 첫번째 페이지를 올릴때만 사진 파일을 올린다.
                        picUploadFileList[pageIdx] = "";
                        if (m_mapPic != null && pageIdx == 0) {
                            for (int i = 1; i <= m_mapPic.size(); i++) {
                                Log.d("EmrDroid", "사진 서버로 전송 " + i);
                                String picFileName = dirPath + "/consentpic_" + i + "." + ccfImagePostfix; // 단말기에 저장된 사진 원본
                                String picUploadFile = uploadFileName[pageIdx] + "." + i + "." + ccfImagePostfix;
                                addParam = "file_type=pic&pre_save=" + preSave + "&hospital_id=" + hospitalId;
                                mSaveXml = uploadPngFile(picFileName, picUploadFile.replace("\\", "/"), addParam);
                                if ("".equalsIgnoreCase(picUploadFileList[pageIdx])) {
                                    picUploadFileList[pageIdx] = picUploadFile;
                                } else {
                                    picUploadFileList[pageIdx] += "," + picUploadFile;
                                }
                                if (mSaveXml == null) mSaveXml = "파일 업로드 중 오류가 발생했습니다(pic).";
                                if (!mSaveXml.startsWith("success")) {
                                    handler.post(new Runnable() {
                                        public void run() {
                                            mDialog.dismiss();
                                            showSaveXml();
                                        }
                                    });
                                    return;
                                }
                            }
                        }
                        // end of 사진파일을 서버에 올린다.
                    }

                    // ------------------------------------------------------------------------------------------------------------------------
                    // 이미지를 저장하는 중에 데이테베이스에 있는 seq 값이 변경되었을 수 있으므로 다시 구한다.
                    // 2026.02.04 WOOIL - 이곳에는 reSaveYn을 넘기지 않아도 됨.
                    param.clear();
                    param.put("hospitalid", hospitalId);
                    param.put("userid", userId);
                    param.put("ccfId", mCcfId);
                    param.put("pid", mPid);
                    param.put("bededt", mBededt);
                    param.put("bdiv", mBdiv);
                    param.put("mode", "2");
                    param.put("presave", preSave); // Y이면 임시저장
                    param.put("apply_exdt", applyExdt); // 2026.02.02 WOOIL - 사용자가 저장일을 변경할 수 있다.

                    String x = getXml("CertificatePaperServlet", param);
                    rs = new ResultSetHelper(x, EmrSettingsUtil.getMaskYn(getBaseContext()));

                    //String sysdt = rs.getString(0, "sysdt"); // 여기서는 사용하지 않는다.
                    //String systm = rs.getString(0, "systm"); // 여기서는 사용하지 않는다.
                    int seq = rs.getInt(0, "seq");
                    int[] uploadSeq = new int[mPageCount];
                    for (int pageIdx = 0; pageIdx < mPageCount; pageIdx++) {
                        uploadSeq[pageIdx] = seq + pageIdx;
                    }

                    // ------------------------------------------------------------------------------------------------------------------------
                    // 오류 없이 파일이 모두 올라갔으면 데이터베이스에 쓴다.
                    // 2024.08.20 WOOIL - 저장정보를 모으로 실제 저장은 아래에서 한 번에 한다.
                    if (mSaveXml.startsWith("success")) {

                        String ccfIdList = "";
                        String seqList = "";
                        String fileNameList = "";
                        String bfSeqList = "";
                        String subPageListList = "";
                        String subPageNoList = "";

                        String bfSeq = mSeq;
                        for (int pageIdx = 0; pageIdx < mPageCount; pageIdx++) {

                            setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 파일 정보를 생성하는 중 입니다.");

                            String ccfId = mCcfId;
                            if (pageIdx > 0) {
                                String pageList[] = mSubPageList.split(";");
                                ccfId = pageList[pageIdx - 1];
                                if ("Y".equalsIgnoreCase(mPreSaved) || "Y".equalsIgnoreCase(mReSaveYn)) bfSeq = ccfId;
                            }

                            //HashMap<String, String> param = new HashMap<String, String>();
                            String subPageList = "";
                            String subPageNo = "";

                            if (pageIdx == 0) {
                                // 2022.03.03 WOOIL - 동의서가 2페이지 이상인 경우
                                subPageList = "";
                                if (mPageCount > 1) {
                                    for (int i = 1; i < mPageCount; i++) {
                                        if (subPageList.equals(""))
                                            subPageList = Integer.toString(uploadSeq[i]);
                                        else
                                            subPageList += ";" + Integer.toString(uploadSeq[i]);
                                    }
                                }
                                subPageNo = "";
                            } else {
                                // 두 번째 페이지부터
                                subPageList = "";
                                subPageNo = Integer.toString(uploadSeq[pageIdx]) + "Y";
                            }

                            // 2024.07.15 WOOIL - 한 번에 서버에 보내도록 자료를 만든다.
                            if (pageIdx == 0) {
                                ccfIdList = ccfId;
                                seqList = Integer.toString(uploadSeq[pageIdx]);
                                fileNameList = uploadFileName[pageIdx];
                                bfSeqList = bfSeq;
                                subPageListList = subPageList;
                                subPageNoList = subPageNo;
                            } else {
                                ccfIdList += "," + ccfId;
                                seqList += "," + Integer.toString(uploadSeq[pageIdx]);
                                fileNameList += "," + uploadFileName[pageIdx];
                                bfSeqList += "," + bfSeq;
                                subPageListList += "," + subPageList;
                                subPageNoList += "," + subPageNo;
                            }
                        }

                        setDialogMessage("동의서 파일 정보를 저장하는 중 입니다.");

                        // 2024.07.15 WOOIL - 저장하러 서버에 한 번만 간다.
                        param.clear();
                        param.put("hospitalid", hospitalId);
                        param.put("userid", userId);
                        param.put("ccfId", ccfIdList);
                        param.put("pid", mPid);
                        param.put("bededt", mBededt);
                        param.put("bdiv", mBdiv);
                        param.put("mode", "3");
                        param.put("seq", seqList);
                        param.put("filename", getHangul(fileNameList));
                        param.put("mp4filelist", getHangul(mp4UploadFileList[0]));
                        param.put("picfilelist", getHangul(picUploadFileList[0]));
                        param.put("sysdt", sysdt);
                        param.put("systm", systm);
                        param.put("rptnm", getHangul(mCcfName));
                        param.put("presave", preSave); // Y이면 임시저장
                        param.put("bf_presaved", mPreSaved); // 이 파일이 임시저장되었던 문서인지?
                        param.put("bf_exdt", mExdt);
                        param.put("bf_seq", bfSeqList);
                        param.put("emr_scan_class", mEmrScanClass);
                        param.put("sub_page_list", subPageListList);
                        param.put("sub_page_no", subPageNoList);
                        param.put("dptcd", mDptcd);
                        param.put("drid", mDrid);
                        param.put("qfycd", mQfycd); // 2024.06.24 WOOIL
                        param.put("apply_exdt", applyExdt); // 2026.02.02 WOOIL
                        param.put("re_save_yn", mReSaveYn); // 2026.02.04 WOOIL

                        mSaveXml = getXml("CertificatePaperServlet", param);
                        Log.d("EmrDroid", "mSaveXml=" + mSaveXml);

                        if (mSaveXml == null || !mSaveXml.startsWith("success")) {
                            handler.post(new Runnable() {
                                public void run() {
                                    mDialog.dismiss();
                                    showSaveXml();
                                }
                            });
                            return;
                        }
                    }

                    // 2024.08.01 WOOIL - 이 곳으로 이동
                    for (int pageIdx = 0; pageIdx < mPageCount; pageIdx++) {
                        mMyViewList.get(pageIdx).recycleAll(); // 2021.08.20 WOOIL - 메모리 해제
                    }

                    Log.d("EmrDroid", "Save end");

                    handler.post(new Runnable() {
                        public void run() {
                            mDialog.dismiss();
                            showSaveXml();
                        }
                    });
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Log.d("EmrDroid", "Save error" + e.getMessage().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("EmrDroid", "error" + e.getMessage().toString());
                }
            }
        }).start();

    }


    private void showSaveXml() {
        // 서버에서 돌려주는 값 뒤에 엔터문자가 있다.
        if (mSaveXml == null) {
            if (getXmlError() == true)
                mSaveXml = getXmlErrorMessage();
            else
                mSaveXml = "오류발생";
        }
        if (mSaveXml.startsWith("success")) {
            //super.showSimpleDialog(getString(R.string.save_success));
        } else {
            // 실패시는 화면을 유지한다.
            if (mSaveXml.startsWith("thread interrupted")) {
                super.showSimpleDialog("작업 시간이 초과되어 강제 종료되었습니다.");
            }else {
                super.showSimpleDialog(mSaveXml);
            }
            return;
        }

        // 성공하면 종료한다.
        String message = getString(R.string.save_success);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO Auto-generated method stub
                        callFinish();
                    }
                });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    private void callFinish() {
        Intent i = new Intent();
        setResult(RESULT_OK, i);
        finish();
    }

    private void showDialogSavedFile() {
        // 저장된 이미지를 불러와서 보여준다.
        // 왜? 개발자 확인용임.
        Intent intent = new Intent(ConsentForm.this, ShowImage.class);
        intent.putExtra("filename", mFileName);
        startActivity(intent);
    }

    private String getCcfImagePostfix() {
        // 파일을 png로 할지 jpg로 할지 결정
        String ccfImageFormat = EmrSettingsUtil.getCcfImageFormat(mActivity);
        String ccfImagePostfix = "";
        if ("jpg".equalsIgnoreCase(ccfImageFormat)) {
            ccfImagePostfix = "jpg";
        } else {
            ccfImagePostfix = "png";
        }
        return ccfImagePostfix;
    }

    private static final int BUFFER_SIZE = 0x1000; // 4K

    public static byte[] toByteArray(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    public static long copy(InputStream from, OutputStream to) throws IOException {
        byte[] buf = new byte[BUFFER_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    private class CertificatePaperWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mIsOnPageFinished = true;
        }

    }

    public void seePageFinished() {
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                for (; ; ) {
                    if (mIsOnPageFinished) break;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        //e.printStackTrace();
                    }
                }
                capturePicture();
            }

        }).start();
    }

    public void capturePicture() {
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
//				mSiView.setVisibility(View.GONE);
                mPic = mWebView.capturePicture();

                Bitmap bitmap = pictureDrawable2Bitmap(new PictureDrawable(mPic));
                //bitmap = getResizeBitmap(bitmap);
                //bitmap = compressBitmap(bitmap);
				/*
				//Drawable d = new BitmapDrawable(bitmap);
				mMyView.setVisibility(View.VISIBLE);
				//mMyView.clear(new PictureDrawable(mPic)); // <-- 이 코딩은 갤럭시 노트 10.1 2014 에디선에서 이미지가 작게됨.
				mMyView.clear(bitmap,null);//mMyView.clear(d, null);
				*/
                mMyViewList.get(0).setVisibility(View.VISIBLE);
                mMyViewList.get(0).clear(bitmap, null);
                mRadioPen.setChecked(true);
                mWebView.setVisibility(View.GONE);
            }

        });
    }

    private void getCertificatePaper() {
        if (isImageConsentFile() || isPdfConsentFile()) {
            getCertificatePaperImage(isPdfConsentFile());
        //} else if (isPdfConsentFile()) {
        //    getCertificatePaperPdf();
        } else {
            //mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
            showProgressDialog(getString(R.string.query_wait_message));
            new Thread(new Runnable() {
                public void run() {
                    String hospitalId = getHospitalId();
                    String userId = getUserId();
                    String url = "CertificatePaperServlet"
                               + "?mode=1"
                               + "&hospitalid=" + hospitalId
                               + "&userid=" + userId
                               + "&ccfid=" + mCcfId
                               + "&pid=" + mPid
                               + "&bededt=" + mBededt
                               + "&bdiv=" + mBdiv;
                    mXml = getXml(url);
                    // 종료
                    handler.post(new Runnable() {
                        public void run() {
                            // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                            // 이를 방지함.
                            try {
                                afterGetCertificatePaper();
                                mDialog.dismiss();
                            } catch (Exception e) {
                                ;
                            }
                        }
                    });
                }
            }).start();
        }
    }

    private String mErrPos = "0";
    private String mErrMsg = "";
    private void getCertificatePaperImage(final boolean isPdf) {
        mErrPos = "0";
        mErrMsg = "";
        showProgressDialog(getString(R.string.query_wait_message));
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();

                try {
                    mPdfFilePathList.clear(); // 2026.04.16 WOOIL - PDF용이지만...

                    for (int pageIdx = 0; pageIdx < mPageCount; pageIdx++) {

                        setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 준비 중입니다...(1)" + (isPdf ? "(pdf)" : ""));

                        String ccfId = mCcfId;
                        if (pageIdx > 0) {
                            String pageList[] = mSubPageList.split(";");
                            ccfId = pageList[pageIdx - 1];
                        }

                        setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 준비 중입니다...(2)" + (isPdf ? "(pdf)" : ""));

                        // 동의서에 출력될 환자정보
                        String url = "CertificatePaperServlet"
                                   + "?hospitalid=" + hospitalId
                                   + "&userid=" + userId
                                   + "&ccfid=" + ccfId
                                   + "&mode=11"
                                   + "&pid=" + mPid
                                   + "&bededt=" + mBededt
                                   + "&bdiv=" + mBdiv
                                   + "&dptcd=" + mDptcd
                                   + "&bedodt=" + mBedodt // 2021.08.10 WOOIL - 퇴원일(외래는 진료일시)
                                   + "&u01_pk_yn=" + mU01PkYn // 2023.03.07 WOOIL
                                   + "&u01_opdt=" + mU01Opdt // 2023.03.07 WOOIL
                                   + "&u01_dptcd=" + mU01Dptcd // 2023.03.07 WOOILt
                                   + "&u01_opseq=" + mU01Opseq // 2023.03.07 WOOIL
                                   + "&u01_seq=" + mU01Seq // 2023.03.07 WOOIL
                                   + "&dong_exdt=" + mDongExdt // 2024.08.29 WOOIL - 비급여 동의서 내용을 일자별로 가져오게 하기 위함.(입원만)
                                   + "";
                        mCcfValueXml[pageIdx] = getXml(url);

                        setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 준비 중입니다...(3)" + (isPdf ? "(pdf)" : ""));

                        // 2022.04.27 WOOIL - 의사 사인
                        ResultSetHelper rsHelper = new ResultSetHelper(mCcfValueXml[pageIdx], false);
                        int rsCount = rsHelper.getRecordCount();
                        for (int ii = 0; ii < rsCount; ii++) {
                            String ccfValue = rsHelper.getString(ii, "ccf_value");
                            if (ccfValue.startsWith("sign_")) {
                                String drid = ccfValue.substring(5);
                                String signUrl = "";
                                signUrl += "EmrScanServlet";
                                signUrl += "?hospitalid=" + hospitalId;
                                signUrl += "&userid=" + userId;
                                signUrl += "&drid=" + drid;
                                signUrl += "&mode=9";
                                String fullUrl = getFullUrl(signUrl);

                                String dstDir = mActivity.getFilesDir().getAbsolutePath();
                                String dstPath = dstDir + File.separator + "Sign" + File.separator + drid;
                                Utils.downFile(mActivity, fullUrl, dstPath);
                            }else if (ccfValue.startsWith("logindrsign_")) {
                                String drid = ccfValue.substring(12);
                                String signUrl = "";
                                signUrl += "EmrScanServlet";
                                signUrl += "?hospitalid=" + hospitalId;
                                signUrl += "&userid=" + userId;
                                signUrl += "&drid=" + drid;
                                signUrl += "&mode=9";
                                String fullUrl = getFullUrl(signUrl);

                                String dstDir = mActivity.getFilesDir().getAbsolutePath();
                                String dstPath = dstDir + File.separator + "Sign" + File.separator + drid;
                                Utils.downFile(mActivity, fullUrl, dstPath);
                            }
                        }

                        setDialogMessage((pageIdx + 1) + "/" + mPageCount + " 페이지 준비 중입니다...(4)" + (isPdf ? "(pdf)" : ""));

                        // 동의서이미지(or PDF)
                        String imageUrl = "";
                        imageUrl += "EmrScanServlet";
                        imageUrl += "?hospitalid=" + hospitalId;
                        imageUrl += "&userid=" + userId;
                        imageUrl += "&ccfid=" + ccfId;
                        imageUrl += "&mode=8";

                        mFullUrl = getFullUrl(imageUrl);

                        String dstDir = mActivity.getFilesDir().getAbsolutePath();
                        String dstPath = dstDir + File.separator + "Form" + File.separator + "imageccf_" + pageIdx;

                        Utils.downFile(mActivity, mFullUrl, dstPath);
                        mPdfFilePathList.add(dstPath); // 2026.04.16 WOOIL - 추가
                        Log.d("EmrDroid-Servlet", mFullUrl);

                    }
                } catch(Exception ex) {
                    mErrMsg += ex.getMessage();
                    StackTraceElement[] elem = ex.getStackTrace();
                    for(int i=0;i<elem.length;i++){
                        mErrMsg += "\n" + elem[i].toString();
                    }
                }

                // 종료
                handler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            if ("".equalsIgnoreCase(mErrMsg)) {
                                if (isPdf) {
                                    // pdf문서 form-field에 값을 뿌리는 함수
                                    applyPdfFormFieldsToDownloadedPages();
                                    afterGetCertificatePaperPdf();
                                } else {
                                    for (int i = 0; i < mPageCount; i++) {
                                        afterGetCertificatePaperImage(i);
                                    }
                                }
                            }else{
                                showSimpleDialog(mErrMsg);
                            }
                            mDialog.dismiss();
                        } catch (Exception e) {}
                    }
                });
            }
        }).start();
    }

    private void getPreSavedCertificatePaper() {
        mErrPos = "0";
        mErrMsg = "";
        //mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        showProgressDialog(getString(R.string.query_wait_message));
        new Thread(new Runnable() {
            public void run() {

                String hospitalId = getHospitalId();
                String userId = getUserId();

                try {
                    mErrPos = "1";
                    for (int i = 0; i < mPageCount; i++) {
                        mErrPos = "2";
                        setDialogMessage((i + 1) + "/" + mPageCount + " 페이지 준비 중입니다...(1)");

                        // 임시저장
                        mErrPos = "3";
                        String imagePath = "";
                        if (i == 0) {
                            imagePath = mCcfFileName.replace("\\", "/");
                        } else {
                            // 두번째 페이지부터는 파일명을 다시 가져와야 한다.
                            mErrPos = "4";
                            String sSeq = mSeq;
                            if (i > 0) {
                                String seqList[] = mSubPageList.split(";");
                                sSeq = seqList[i - 1];
                            }

                            setDialogMessage((i + 1) + "/" + mPageCount + " 페이지 준비 중입니다...(2)");

                            // 임시 저장된 동의서 정보
                            mErrPos = "5";
                            String url = "";
                            url += "CertificatePaperServlet";
                            url += "?hospitalid=" + hospitalId;
                            url += "&userid=" + userId;
                            url += "&mode=16";
                            url += "&pid=" + mPid;
                            url += "&bdiv=" + mBdiv;
                            url += "&exdt=" + mExdt;
                            url += "&seq=" + sSeq;
                            url += "&pre_saved_bdiv=" + mPreSavedBdiv;
                            url += "&re_save_yn=" + mReSaveYn;
                            String xml = getXml(url);

                            mErrPos = "6";
                            if (xml.equals("")){
                                if ("Y".equalsIgnoreCase(mReSaveYn)){
                                    mErrMsg = "저장 동의서 정보를 찾을 수 없습니다.";
                                } else {
                                    mErrMsg = "임시 저장 동의서 정보를 찾을 수 없습니다.";
                                }
                                break;
                            }

                            mErrPos = "7";
                            ResultSetHelper rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));
                            String path = rs.getString(0, "path");
                            imagePath = path.replace("\\", "/");
                        }

                        mErrPos = "8";
                        setDialogMessage((i + 1) + "/" + mPageCount + " 페이지 준비 중입니다...(3)");

                        mErrPos = "9";
                        String imageUrl = "";
                        imageUrl += "EmrScanServlet";
                        imageUrl += "?hospitalid=" + hospitalId;
                        imageUrl += "&path=" + imagePath;
                        imageUrl += "&mode=" + ("Y".equalsIgnoreCase(mReSaveYn) ? "1" : "3"); // 3.임시저장에서 1.실저장에서
                        mFullUrl = getFullUrl(imageUrl);

                        //Log.d("EmrDroid-Servlet", mFullUrl);
                        //Log.d("EmrDroid", "ccffile=" + mCcfFileName);

                        mErrPos = "10";
                        String dstDir = mActivity.getFilesDir().getAbsolutePath();
                        String dstPath = dstDir + File.separator + "Form" + File.separator + "presaved_" + i;

                        setDialogMessage((i + 1) + "/" + mPageCount + " 페이지 준비 중입니다...(4)");

                        mErrPos = "11";
                        Utils.downFile(mActivity, mFullUrl, dstPath);

                        mErrPos = "12";
                        setDialogMessage((i + 1) + "/" + mPageCount + " 페이지 준비 중입니다...(5)");
                    }
                    mErrPos = "13";
                } catch (Exception ex) {
                    if ("Y".equalsIgnoreCase(mReSaveYn)){
                        mErrMsg += "저장 동의서 준비 중 오류가 발생했습니다." + "\n";
                    }else {
                        mErrMsg += "임시 저장 동의서 준비 중 오류가 발생했습니다." + "\n";
                    }
                    mErrMsg += mErrPos + "\n";
                    mErrMsg += ex.getMessage();
                    StackTraceElement[] elem = ex.getStackTrace();
                    for(int i=0;i<elem.length;i++){
                        mErrMsg += "\n" + elem[i].toString();
                    }
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            if ("".equalsIgnoreCase(mErrMsg)) {
                                for (int i = 0; i < mPageCount; i++) {
                                    afterPreSavedCertificatePaper(i);
                                }
                            }else{
                                showSimpleDialog(mErrMsg);
                            }
                            mDialog.dismiss();
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
    }

    boolean mIsOnPageFinished = false;

    private void afterGetCertificatePaper() {
        // 오류발생
        if (super.getXmlError() == true) {
            super.showToastText(super.getXmlErrorMessage());
            return;
        }
        mIsOnPageFinished = false;
        seePageFinished();
        mWebView.loadDataWithBaseURL(null, mXml, "text/html", "utf-8", null); // <-- 제대로 나옴.
        mWebView.setVisibility(View.VISIBLE);
    }

    private void afterPreSavedCertificatePaper(int index) {
        //mSiView.setVisibility(View.GONE);
        String dstDir = mActivity.getFilesDir().getAbsolutePath();
        String pathName = dstDir + File.separator + "Form" + File.separator + "presaved_" + index;//mCcfFileName;

        Bitmap bitmap = BitmapFactory.decodeFile(pathName);

        mMyViewList.get(index).setVisibility(View.VISIBLE);
        mMyViewList.get(index).clear(bitmap, null);
        mRadioPen.setChecked(true);
        mWebView.setVisibility(View.GONE);

        // 이 if문을 뺐더니 나무병원에서 오류가 발생하여 죽었음.
        if (index != 0) {
            mMyViewList.get(index).setVisibility(View.GONE);
        }
    }

    private void afterGetCertificatePaperImage(int index) {
        try {
            //mSiView.setVisibility(View.GONE);

            String dstDir = mActivity.getFilesDir().getAbsolutePath();
            String pathName = dstDir + File.separator + "Form" + File.separator + "imageccf_" + index;//mCcfFileName;

            //BitmapFactory.Options options = new BitmapFactory.Options();
            //options.inSampleSize=1;
            //if(mPageCount>1) options.inSampleSize=2;
            //Bitmap bitmap = BitmapFactory.decodeFile(pathName,options);

            Bitmap bitmap = BitmapFactory.decodeFile(pathName);

            // 출력할 인적자료
            CcfValues ccfValues = new CcfValues();
            ResultSetHelper rsHelper = new ResultSetHelper(mCcfValueXml[index], false);
            int rsCount = rsHelper.getRecordCount();
            for (int i = 0; i < rsCount; i++) {
                String ccfField = rsHelper.getString(i, "ccf_field");
                String ccfX = rsHelper.getString(i, "ccf_x");
                String ccfY = rsHelper.getString(i, "ccf_y");
                String ccfH = rsHelper.getString(i, "ccf_h");
                String ccfW = rsHelper.getString(i, "ccf_w");
                String ccfAutoFit = rsHelper.getString(i, "ccf_auto_fit");
                float x = Utils.toFloat(ccfX);
                float y = Utils.toFloat(ccfY);
                float h = Utils.toFloat(ccfH);
                float w = Utils.toFloat(ccfW);
                boolean autoFit = Utils.toBoolean(ccfAutoFit);
                String ccfValue = rsHelper.getString(i, "ccf_value");
                ccfValues.addCcfValue(ccfField, x, y, w, h, autoFit, ccfValue);
            }

            mMyViewList.get(index).setVisibility(View.VISIBLE);
            mMyViewList.get(index).clear(bitmap, ccfValues);
            mRadioPen.setChecked(true);
            mWebView.setVisibility(View.GONE);

            // 이 if문을 뺐더니 나무병원에서 오류가 발생하여 죽었음.
            if (index != 0) {
                mMyViewList.get(index).setVisibility(View.GONE);
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            //Toast.makeText(mActivity,  "동의서 페이지 구성 중 오류 발생 " + e.getLocalizedMessage() , Toast.LENGTH_SHORT).show();
        } catch (Exception ex){
            //Toast.makeText(mActivity,  "동의서 페이지 구성 중 오류 발생 " + ex.getLocalizedMessage() , Toast.LENGTH_SHORT).show();
        }
    }

    private Picture mPic; // 동의서 이미지

    public class MyView extends FingerPaintView3 {

        // 2021.08.06 WOOIL - penColor 추가. 펜의 색을 사용자가 변경할 수 있게하기 위함.
        public MyView(Context c, int penWidth, int eraserWidth, int penColor, Paint penPaint, Paint clearPaint, Paint cursorPaint, Paint textPaint, String userId) {
            super(c, penWidth, eraserWidth, penColor, penPaint, clearPaint, cursorPaint, textPaint, userId);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // TODO Auto-generated method stub
        if (checkedId == R.id.radio_pen) {
            // 펜
            //모든 페이지에 동일하게 적용
            for (int i = 0; i < mPageCount; i++) {
                mMyViewList.get(i).setPenMode(MyView.MODE_PEN);
            }
        } else if (checkedId == R.id.radio_eraser) {
            // 지우개
            //모든 페이지에 동일하게 적용
            for (int i = 0; i < mPageCount; i++) {
                mMyViewList.get(i).setPenMode(MyView.MODE_ERASER);
            }
        } else if (checkedId == R.id.radio_none) {
            // 확대축소
			/*
			Bitmap bitmap = mMyView.getSignedBitmap();
			Bitmap bm = Bitmap.createBitmap(bitmap);
			mSiView.setImageBitmap(bm);
			mMyView.setPenMode(MyView.MODE_NONE);
			mMyView.setVisibility(View.GONE);
			mSiView.setVisibility(View.VISIBLE);
			mPenWidthSpinner.setVisibility(View.GONE);
			mEraserWidthSpinner.setVisibility(View.GONE);
			*/
        } else if (checkedId == R.id.page_1) {
            setPageShow(0);
        } else if (checkedId == R.id.page_2) {
            setPageShow(1);
        } else if (checkedId == R.id.page_3) {
            setPageShow(2);
        } else if (checkedId == R.id.page_4) {
            setPageShow(3);
        } else if (checkedId == R.id.page_5) {
            setPageShow(4);
        } else if (checkedId == R.id.page_6) {
            setPageShow(5);
        } else if (checkedId == R.id.page_7) {
            setPageShow(6);
        } else if (checkedId == R.id.page_8) {
            setPageShow(7);
        } else if (checkedId == R.id.page_9) {
            setPageShow(8);
        } else if (checkedId == R.id.page_10) {
            setPageShow(9);
        } else if (checkedId == R.id.page_11) {
            setPageShow(10);
        } else if (checkedId == R.id.page_12) {
            setPageShow(11);
        } else if (checkedId == R.id.page_13) {
            setPageShow(12);
        } else if (checkedId == R.id.page_14) {
            setPageShow(13);
        } else if (checkedId == R.id.page_15) {
            setPageShow(14);
        }
    }

    private void setPageShow(int pageIndex) {
        // 동의서 페이지 번호를 클릭한 경우
        for (int i = 0; i < mPageCount; i++) {
            mMyViewList.get(i).setVisibility(View.GONE);
        }
        for (int i = 0; i < mPdfViewList.size(); i++) {
            mPdfViewList.get(i).setVisibility(View.GONE);
        }

        if (mIsPdfConsent){
            if (pageIndex >= 0 && pageIndex < mPdfViewList.size()) {
                mPdfViewList.get(pageIndex).setVisibility(View.VISIBLE);
            }
        } else {
            if (pageIndex >= 0 && pageIndex < mMyViewList.size()) {
                mMyViewList.get(pageIndex).setVisibility(View.VISIBLE);
            }
        }
    }

    private boolean mRecording = false;

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        if (view.getId() == R.id.record_button) {
            if (mRecording == false) {
                startRecord();
                setRecordButtonText("녹음중");
            } else {
                stopRecord();
                setRecordButtonText("녹음");
            }
        } else if (view.getId() == R.id.pic_button) {
            // 촬영
            cameraPic();
        } else if (view.getId() == R.id.undo_sign_button) {
            //mMyView.undoSign();
            mMyViewList.get(getCurrentPageNo()).undoSign();
        }
    }

    String currentPhotoPath;
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if(!storageDir.exists()){
            storageDir.mkdir();
        }
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = imageFile.getAbsolutePath();
        return imageFile;
    }

    private void cameraPic() {

        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (Build.VERSION.SDK_INT >= 30) {
                // 2024.06.10 WOOIL -
                // Cureate the File where the photo should go
                File photoFile = createImageFile();
                // continue only if the File was successfully created
                if(photoFile != null){
                    Uri photoUri = FileProvider.getUriForFile(this, "com.metrosoft.smart.emr.emrdroid.gt101.fileprovider", photoFile);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    startActivityForResult(intent, CALL_CAMERA);
                }else{
                    Toast.makeText(mActivity, "file create error", Toast.LENGTH_LONG);
                }
            }else {
                startActivityForResult(intent, CALL_CAMERA);
            }
        } catch (Exception ex) {
            Toast.makeText(mActivity, ex.getMessage(), Toast.LENGTH_LONG);
        }
		
		/*
		Intent inetent = new Intent(this, CameraT.class);
		startActivity(inetent);
		*/
    }


    private HashMap<Integer, Object> m_mapPic = null;

    @SuppressLint("ShowToast")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("EmrDroid", "촬영이후 requestCode=" + requestCode + ", resultCode=" + resultCode + ", data is null = " + (data == null));
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CALL_CAMERA /*&& !data.equals(null)*/) {
            if (resultCode != 0) {
                try {
                    Log.d("EmrDroid", "사진 처리 시작");

                    String dirPath = getFilesDir().getAbsolutePath();
                    File dir = new File(dirPath);

                    // 폴더가 없으면 생성
                    if (!dir.exists()) {
                        Log.d("EmrDroid", "폴더생성");
                        dir.mkdirs();
                    }

                    if (Build.VERSION.SDK_INT >= 30) {

                        // android 11 이상
                        // 카메라에서 촬영한 이미지가 저장되지 않는다.

                        //Bitmap photo = (Bitmap) data.getExtras().get("data"); 이렇게 하면 thumnail이 넘어옴.

                        Bitmap photo = BitmapFactory.decodeFile(currentPhotoPath);
                        savePicImage(dirPath, null, photo);

                    } else {

                        String szDateTop = "";
                        String[] IMAGE_PROJECTION = {
                                Images.ImageColumns.DATA,
                                Images.Thumbnails.DATA
                        };
                        Uri uriImages = Images.Media.EXTERNAL_CONTENT_URI;
                        Uri uriImagesthum = Images.Thumbnails.EXTERNAL_CONTENT_URI;
                        ContentResolver cr = getContentResolver();
                        Cursor cursorImages = cr.query(uriImages, IMAGE_PROJECTION, null, null, null);
                        if (cursorImages != null && cursorImages.moveToLast()) {
                            szDateTop = cursorImages.getString(0);
                            cursorImages.close();
                        }

                        Log.d("EmrDroid", "szDateTop=" + szDateTop);

                        savePicImage(dirPath, szDateTop, null);

                    }

                } catch (Exception ex) {
                    Log.d("EmrDroid", "error=" + ex.getLocalizedMessage());
                }
            }
        }
        if (requestCode == REQ_SELECT_DOCTOR) {
            if (resultCode == RESULT_OK && data != null) {
                //mDialog = ProgressDialog.show(this, "", "의사 정보를 변경하는 중입니다.", true);
                showProgressDialog("의사 정보를 변경하는 중입니다.");
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            final String drid = data.getStringExtra("code"); // 의사id
                            final String drnm = data.getStringExtra("codenm"); // 의사명
                            final String drnm_eng = data.getStringExtra("drengnm"); // 의사 영문명
                            final String gdrlcid = data.getStringExtra("gdrlcid"); // 면허번호
                            final String sdrlcid = data.getStringExtra("sdrlcid"); // 전문의 번호
                            final String drsign = data.getStringExtra("drsign"); // 의사 사인 정보

                            if ("".equalsIgnoreCase(drid)) return;

                            setApplyDrnm(drnm);
                            for (int i = 0; i < mPageCount; i++) {
                                boolean changed = mMyViewList.get(i).injectCcfValue4Doctor(drid, drnm, drnm_eng, gdrlcid, sdrlcid);

                                boolean changed_drsign = mMyViewList.get(i).injectCcfValue4DrSign(drsign);
                                if (changed_drsign){
                                    // 의사 사인 이미지를 새로 불러온다.
                                    String hospitalId = getHospitalId();
                                    String userId = getUserId();

                                    String signUrl = "";
                                    signUrl += "EmrScanServlet";
                                    signUrl += "?hospitalid=" + hospitalId;
                                    signUrl += "&userid=" + userId;
                                    signUrl += "&drid=" + drid;
                                    signUrl += "&mode=9";
                                    String fullUrl = getFullUrl(signUrl);

                                    String dstDir = mActivity.getFilesDir().getAbsolutePath();
                                    String dstPath = dstDir + File.separator + "Sign" + File.separator + drid;
                                    Utils.downFile(mActivity, fullUrl, dstPath);
                                }

                                if (changed || changed_drsign) mMyViewList.get(i).postInvalidate();
                            }
                        } catch (Exception ex) {
                            showSimpleDialogThread(ex.getLocalizedMessage());
                        }

                        // 종료
                        handler.post(new Runnable() {
                            public void run() {
                                mDialog.dismiss();
                            }
                        });
                    }
                }).start();
            }
        }
    }

    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height;// / 2;
            final int halfWidth = width;// / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private void savePicImage(final String dirPath, final String picPath, final Bitmap picBitmap) {
        //mDialog = ProgressDialog.show(this, "", "사진 이미지를 처리중입니다.", true);
        showProgressDialog("사진 이미지를 처리하는 중입니다.");
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {

                    if (m_mapPic == null) m_mapPic = new HashMap<Integer, Object>();

                    Integer idx = m_mapPic.size() + 1;
                    m_mapPic.put(idx, "");

                    // 저장해 버리자..
                    Bitmap bmp = null;
                    if (picBitmap != null) {
                        bmp = picBitmap;

                        //File file = new File(currentPhotoPath);
                        //bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(file));

                    } else {
                        String szDateTop = picPath;
                        File file = new File(szDateTop);
                        Uri uri = Uri.fromFile(file);
                        //Bitmap bitmap = (Bitmap)data.getExtras().get("data");
                        bmp = Images.Media.getBitmap(getContentResolver(), uri);
                    }
                    int quality = 100; // 파일크기에 별로 영향을 미치지 않는다.

                    // 2024.06.10 WOOIL -- 이미지 크기를 줄이는 부분은 막는다.
                    // byte array로 변환한다.
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    //quality = 50; // 2024.06.07 WOOIL - 서울바른척도에서 사진 화질이 너무 안좋다고 하여...
                    bmp.compress(Bitmap.CompressFormat.PNG, quality, stream);
                    byte[] byteArray = stream.toByteArray();
                    stream.close();
                    // 이미지 크기를 구한다.
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
                    // 축소비율을 구한다.
                    //int reqWidth = 1280;
                    //int reqHeight = 800;
                    //if (options.outWidth > options.outHeight) {
                    //    reqWidth = 1280;
                    //    reqHeight = 800;
                    //}else{
                    //    reqWidth = 800;
                    //    reqHeight = 1280;
                    //}
                    //int inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                    int inSampleSize = 1;
                    if(options.outWidth>=3264) inSampleSize = 2;
					/*
					options.inJustDecodeBounds = false;
					options.inSampleSize = inSampleSize;
					Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
					*/
                    // 이미지 축소처리
                    int ratio = inSampleSize;
                    int dstWidth = options.outWidth;
                    int dstHeight = options.outHeight;
                    Bitmap bitmap = Bitmap.createScaledBitmap(bmp, dstWidth / ratio, dstHeight / ratio, true);
                    //
                    String ccfImagePostfix = getCcfImagePostfix();

                    // 디바이스에 저장. 추후 서버에 올린다.
                    String picFileName = dirPath + "/consentpic_" + idx + "." + ccfImagePostfix;
                    FileOutputStream output = new FileOutputStream(picFileName);
                    if ("jpg".equalsIgnoreCase(ccfImagePostfix)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output);
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.PNG, quality, output);
                    }
                    output.flush();
                    output.close();


                } catch (Exception ex) {

                } finally {
                    mHandler.post(new Runnable() {
                        public void run() {
                            // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                            // 이를 방지함.
                            try {
                                setPicButtonText("촬영");
                                mDialog.dismiss();
                            } catch (Exception e) {
                                ;
                            }
                        }
                    });
                }

            }

        }).start();

    }

    @Override
    public void onItemSelected(AdapterView<?> parentView, View view, int position, long id) {
        // TODO Auto-generated method stub
        if (parentView.getId() == R.id.pen_width_spinner) {
            int penWidth = position + 1;
            EmrSettingsUtil.setCcfPenWidth(mActivity, penWidth);
            //모든 페이지에 동일하게 적용
            //for (int i = 0; i < mPageCount; i++) {
            //    mMyViewList.get(i).setPenWidth(penWidth);
            //}
        } else if (parentView.getId() == R.id.eraser_width_spinner) {
            int eraserWidth = position + 1;
            EmrSettingsUtil.setCcfEraserWidth(mActivity, eraserWidth);
            //모든 페이지에 동일하게 적용
            //for (int i = 0; i < mPageCount; i++) {
            //    mMyViewList.get(i).setEraserWidth(eraserWidth);
            //}
        } else if (parentView.getId() == R.id.pen_color_spinner) {
            // 2021.08.25 WOOIL - 펜색 석택
            String penColor = "검정";
            int penColorValue = Color.BLACK;
            if (position == 0) {
                penColor = "검정";
                penColorValue = Color.BLACK;
            } else if (position == 1) {
                penColor = "파랑";
                penColorValue = Color.BLUE;
            } else if (position == 2) {
                penColor = "빨강";
                penColorValue = Color.rgb(255, 50, 50); // RED_COLOR;
            }
            EmrSettingsUtil.setCcfPenColor(mActivity, penColor);
            //모든 페이지에 동일하게 적용
            //for (int i = 0; i < mPageCount; i++) {
            //    mMyViewList.get(i).setPenColor(penColorValue);
            //}
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parentView) {
        // TODO Auto-generated method stub

    }

    private int getCurrentPageNo() {
        if (mRadioPage1.isChecked() == true) return 0;
        if (mRadioPage2.isChecked() == true) return 1;
        if (mRadioPage3.isChecked() == true) return 2;
        if (mRadioPage4.isChecked() == true) return 3;
        if (mRadioPage5.isChecked() == true) return 4;
        if (mRadioPage6.isChecked() == true) return 5;
        if (mRadioPage7.isChecked() == true) return 6;
        if (mRadioPage8.isChecked() == true) return 7;
        if (mRadioPage9.isChecked() == true) return 8;
        if (mRadioPage10.isChecked() == true) return 9;
        if (mRadioPage11.isChecked() == true) return 10;
        if (mRadioPage12.isChecked() == true) return 11;
        if (mRadioPage13.isChecked() == true) return 12;
        if (mRadioPage14.isChecked() == true) return 13;
        if (mRadioPage15.isChecked() == true) return 14;

        return 0;
    }

    private void getOpHx() {
        // 2023.03.07 WOOIL - 수술이력 조회
        //mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        showProgressDialog(getString(R.string.query_wait_message));
        new Thread(new Runnable() {
            public void run() {

                String hospitalId = getHospitalId();
                String userId = getUserId();
                String url = "";
                //
                url = "CertificatePaperServlet"
                       + "?hospitalid=" + hospitalId
                       + "&userid=" + userId
                       + "&mode=19"
                       + "&pid=" + mPid
                       + "&bededt=" + mBededt
                       + "&bdiv=" + mBdiv
                       + "";
                String xml = getXml(url);
                mOpHxXml = xml;

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetOpHx();
                            mDialog.dismiss();
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetOpHx(){
        if("".equalsIgnoreCase(mOpHxXml)) return;
        try{
            ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
            HashMap<String, Object> map = null;

            ResultSetHelper rsHelper = new ResultSetHelper(mOpHxXml, false);
            int rsCount = rsHelper.getRecordCount();
            if(rsCount<=1){
                Toast.makeText(mActivity, "수술이력이 " + rsCount +  "건 입니다.", Toast.LENGTH_LONG);
                return;
            }
            for (int i = 0; i < rsCount; i++) {
                map = new HashMap<String, Object>();
                map.put("opdt", rsHelper.getString(i, "opdt"));
                map.put("dptcd", rsHelper.getString(i, "dptcd"));
                map.put("opseq", rsHelper.getString(i, "opseq"));
                map.put("seq", rsHelper.getString(i, "seq"));
                map.put("rsvopdt", super.getFormattedDate(rsHelper.getString(i, "rsvopdt")));
                map.put("rsvop", rsHelper.getString(i, "rsvop"));
                map.put("rsvopdrnm", rsHelper.getString(i, "rsvopdrnm"));

                mylist.add(map);
            }

            final SimpleAdapter adapter = new SimpleAdapter(this, mylist, R.layout.consent_form_op_hx_row,
                    new String[] {"rsvopdt", "dptcd", "rsvopdrnm", "rsvop"},
                    new int[] {R.id.rsvopdt, R.id.dptcd, R.id.rsvopdrnm, R.id.rsvop});


            AlertDialog.Builder builder = new AlertDialog.Builder(ConsentForm.this);
            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        HashMap<String,Object> map = (HashMap<String,Object>)adapter.getItem(which);
                        mU01PkYn = "Y";
                        mU01Opdt = (String)map.get("opdt");
                        mU01Dptcd = (String)map.get("dptcd");
                        mU01Opseq = (String)map.get("opseq");
                        mU01Seq = (String)map.get("seq");

                        getCertificatePaper();
                    }catch(Exception ex){
                        showSimpleDialogThread(ex.getLocalizedMessage());
                    }
                }
            });
            builder.setTitle(mHxType);

            AlertDialog  alertDialog = builder.create();
            alertDialog.show();
            alertDialog.getWindow().setLayout(1200, 1400); //Controlling width and height.

        } catch (JSONException e) {
            showSimpleDialog(e.getLocalizedMessage());
        } catch (Exception ex) {
            showSimpleDialog(ex.getLocalizedMessage());
        }
    }

    private void getExdtList(){
        // 2024.08.26 WOOIL - 일자선택(입원일부터 현재일까지 보여준다.)
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            Calendar cal = Calendar.getInstance();

            String frdt = mBededt;
            String todt = mBedodt;
            if ("".equalsIgnoreCase(todt)) {
                Date now = new Date();
                todt = sdf.format(now);
            }
            ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
            HashMap<String, Object> map = null;
            String dong_exdt = todt;
            while (true) {
                map = new HashMap<String, Object>();
                map.put("dong_exdt", dong_exdt);
                mylist.add(map);
                Date ex_date = sdf.parse(dong_exdt);
                cal.setTime(ex_date);
                cal.add(Calendar.DAY_OF_MONTH, -1);
                dong_exdt = sdf.format(cal.getTime());
                if (dong_exdt.compareToIgnoreCase(frdt) < 0) break;
            }

            final SimpleAdapter adapter = new SimpleAdapter(this, mylist, R.layout.consent_form_dong_exdt_hx_row,
                    new String[] {"dong_exdt"},
                    new int[] {R.id.dong_exdt});


            AlertDialog.Builder builder = new AlertDialog.Builder(ConsentForm.this);
            builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        HashMap<String,Object> map = (HashMap<String,Object>)adapter.getItem(which);
                        mDongExdt = (String)map.get("dong_exdt");
                        setDongExdtText(mDongExdt);

                        getCertificatePaper();
                    }catch(Exception ex){
                        showSimpleDialogThread(ex.getLocalizedMessage());
                    }
                }
            });
            builder.setTitle(mHxType);

            AlertDialog  alertDialog = builder.create();
            alertDialog.show();
            alertDialog.getWindow().setLayout(600, 1400); //Controlling width and height.

        }catch(Exception ex){

        }
    }

    private class UncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.d("ConsentForm", "Crash!!!");

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            StringBuilder sb = new StringBuilder();
            sb.append(sw.toString());
            String msg = sb.toString();
            EmrSettingsUtil.setUncaughtExceptionMessage(mActivity, msg); // 2024.06.11 WOOIL
            Toast.makeText(mActivity, msg, Toast.LENGTH_SHORT).show();

            // Try everything to make sure this process goes away.
            // android.os.Process.killProcess(android.os.Process.myPid());
            // System.exit(10);
            mDefaultUncaughtExceptionHandler.uncaughtException(t, e);

        }
    }

    private String toCircledNumber(int n) {
        // 1~20: ①(0x2460) ~ ⑳(0x2473)
        if (n >= 1 && n <= 20) {
            return String.valueOf((char) (0x2460 + (n - 1)));
        }
        return String.valueOf(n);
    }

    private void showProgressDialog(final String msg) {
        try {
            // 기존 dialog가 있으면 제거
            dismissProgressDialog();

            mDialog = new ProgressDialog(this, R.style.CustomProgressDialog);
            mDialog.setMessage(msg);
            mDialog.setCancelable(false);
            mDialog.show();

        } catch (Exception e) {
            // Activity 종료 상태 등 예외 방지
            e.printStackTrace();
        }
    }

    private void dismissProgressDialog() {
        try {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mDialog = null;
        }
    }

    private void setDialogMessage(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mDialog != null) mDialog.setMessage(msg);
            }
        });
    }

    // 외부에서 호출하는 용도
    public void setDialogMessagePublic(final String msg) {
        setDialogMessage(msg);
    }

    // 2026.04.14 WOOIL - PDF문서인지?
    private boolean isPdfConsentFile() {
        return mCcfFileName != null && mCcfFileName.toLowerCase().endsWith(".pdf");
    }

    // 2026.04.14 WOOIL - 이미지 문서인지?
    private boolean isImageConsentFile() {
        if (mCcfFileName == null) return false;
        String name = mCcfFileName.toLowerCase();
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".gif") || name.endsWith(".jpeg");
    }

    // 2026.04.14 WOOIL - PDF 동의서 처리
    private void afterGetCertificatePaperPdf() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            showSimpleDialog("PDF 동의서는 Android 5.0 이상에서만 지원됩니다.");
            return;
        }

        try {
            // 기존 이미지 뷰는 숨김
            for (int i = 0; i < mMyViewList.size(); i++) {
                ((View) mMyViewList.get(i)).setVisibility(View.GONE);
            }

            // webview도 숨김
            mWebView.setVisibility(View.GONE);

            // pdf view 열기
            for (int i = 0; i < mPdfViewList.size(); i++) {
                PdfInkSignView pdfView = mPdfViewList.get(i);
                pdfView.setVisibility(View.GONE);

                if (i < mPdfFilePathList.size()) {
                    File pdfFile = new File(mPdfFilePathList.get(i));
                    pdfView.openPdf(pdfFile, 0);
                }
            }

            if (mPdfViewList.size() > 0) {
                mPdfViewList.get(0).setVisibility(View.VISIBLE);
            }

            mRadioPen.setChecked(true);
            applyCurrentDrawModeToPdfViews();
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    // 2026.04.14 WOOIL - PDF 동의서 처리
    private void applyCurrentDrawModeToPdfViews() {
        int checkedId = mPenGroup.getCheckedRadioButtonId();

        for (int i = 0; i < mPdfViewList.size(); i++) {
            PdfInkSignView pdfView = mPdfViewList.get(i);

            if (checkedId == R.id.radio_pen) {
                pdfView.setMode(PdfInkSignView.MODE_PEN);
            } else if (checkedId == R.id.radio_eraser) {
                pdfView.setMode(PdfInkSignView.MODE_ERASER);
            } else {
                pdfView.setMode(PdfInkSignView.MODE_NONE);
            }

            // 현재 spinner 값 반영
            int penWidth = mPenWidthSpinner.getSelectedItemPosition() + 1;
            int eraserWidth = mEraserWidthSpinner.getSelectedItemPosition() + 1;

            pdfView.setPenWidthPx(penWidth * 2.0f);
            pdfView.setEraserHitPx(eraserWidth * 15.0f);

            int color = Color.BLACK;
            int colorIndex = mPenColorSpinner.getSelectedItemPosition();
            if (colorIndex == 1) color = Color.BLUE;
            else if (colorIndex == 2) color = Color.RED;

            pdfView.setPenColor(color);
        }
    }

    // 2026.04.14 WOOIL - PDF 동의서 저장처리
    private void savePdfConsent(final String preSave) {
        showProgressDialog(getString(R.string.process_wait_message));

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String dirPath = getFilesDir().getAbsolutePath();
                    File dir = new File(dirPath);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    // 페이지별 PDF를 각각 저장
                    ArrayList<String> savedPdfList = new ArrayList<String>();

                    for (int i = 0; i < mPdfViewList.size(); i++) {
                        setDialogMessage((i + 1) + "/" + mPdfViewList.size() + " PDF 저장 중입니다.");

                        PdfInkSignView pdfView = mPdfViewList.get(i);
                        File srcPdf = new File(mPdfFilePathList.get(i));
                        File outPdf = new File(dirPath + File.separator + "consentform_" + i + ".pdf");

                        PdfInkPdfSaver.saveAllPages(ConsentForm.this, srcPdf, outPdf, pdfView);
                        savedPdfList.add(outPdf.getAbsolutePath());
                    }

                    // TODO:
                    // 여기 아래는 서버 저장부에 맞게 별도 처리 필요
                    // 현재 기존 로직은 png/jpg 업로드 기준일 가능성이 큼

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mDialog.dismiss();
                            } catch (Exception ignore) {}
                            Toast.makeText(mActivity, "PDF 저장이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                        }
                    });

                } catch (final Exception ex) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mDialog.dismiss();
                            } catch (Exception ignore) {}
                            showSimpleDialog(ex.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    // 2026.04.15 WOOIL - NULL 방지
    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String mapCcfFieldToPdfField(String ccfField) {
        ccfField = nvl(ccfField).trim();

        if ("".equals(ccfField)) return "";

        // PDF 폼필드명이 기존 CCF field명과 같으면 그대로 사용
        // 예: "drnm", "drnm_eng", "gdrlcid", "sdrlcid" 등
        //if ("drnm".equalsIgnoreCase(ccfField)) return "drnm";

        return ccfField;
    }

    private Map<String, String> buildPdfFieldValues(int pageIndex) {
        Map<String, String> values = new HashMap<String, String>();

        try {
            if (pageIndex < 0 || pageIndex >= mCcfValueXml.length) {
                return values;
            }

            String xml = mCcfValueXml[pageIndex];
            if ("".equals(nvl(xml))) {
                return values;
            }

            // 출력할 인적자료
            CcfValues ccfValues = new CcfValues();
            ResultSetHelper rsHelper = new ResultSetHelper(mCcfValueXml[pageIndex], false);
            int rsCount = rsHelper.getRecordCount();
            for (int i = 0; i < rsCount; i++) {
                String ccfField = rsHelper.getString(i, "ccf_field");
                String ccfX = rsHelper.getString(i, "ccf_x");
                String ccfY = rsHelper.getString(i, "ccf_y");
                String ccfH = rsHelper.getString(i, "ccf_h");
                String ccfW = rsHelper.getString(i, "ccf_w");
                String ccfAutoFit = rsHelper.getString(i, "ccf_auto_fit");
                float x = Utils.toFloat(ccfX);
                float y = Utils.toFloat(ccfY);
                float h = Utils.toFloat(ccfH);
                float w = Utils.toFloat(ccfW);
                boolean autoFit = Utils.toBoolean(ccfAutoFit);
                String ccfValue = rsHelper.getString(i, "ccf_value");
                ccfValues.addCcfValue(ccfField, x, y, w, h, autoFit, ccfValue);
            }

            int count = ccfValues.getCount();
            for (int i = 0; i < count; i++) {
                String ccfField = nvl(ccfValues.getField(i)).trim();
                String value = nvl(ccfValues.getValue(i));

                if ("".equals(ccfField)) continue;

                String pdfField = mapCcfFieldToPdfField(ccfField);
                if ("".equals(pdfField)) continue;

                values.put(pdfField, value);
            }

            // 의사명 UI에서 바뀐 값을 우선 반영하고 싶으면 여기서 덮어쓴다.
            if (mApplyDrnm != null) {
                String drnm = nvl(mApplyDrnm.getText().toString()).trim();
                if (!"".equals(drnm)) {
                    values.put("drnm", drnm);
                }
            }

        } catch (Exception e) {
            Log.e("EmrDroid", "buildPdfFieldValues error: " + e.getMessage());
        }

        return values;
    }

    private void applyPdfFormFieldsToDownloadedPages() throws Exception {
        if (mPdfFilePathList == null || mPdfFilePathList.size() <= 0) return;

        for (int i = 0; i < mPdfFilePathList.size(); i++) {
            String srcPath = mPdfFilePathList.get(i);
            if ("".equals(nvl(srcPath))) continue;

            File srcPdf = new File(srcPath);
            if (!srcPdf.exists()) continue;

            Map<String, String> values = buildPdfFieldValues(i);

            // 값이 하나도 없으면 건너뜀
            if (values == null || values.size() == 0) continue;

            File outPdf = new File(srcPdf.getParent(), "filled_" + srcPdf.getName());

            // 현재 단계에서는 텍스트 필드만 채우고, flatten은 하지 않는 쪽이 좋습니다.
            PdfFormRuntimeWriter.fillOnly(
                    this,
                    srcPdf, // 원본 PDF
                    outPdf, // 결과 PDF
                    values, // 필드명과 값
                    null,   // 사인(signatures)
                    false   // 필드값 수정 못하게 고정 여부
            );

            mPdfFilePathList.set(i, outPdf.getAbsolutePath());

        }
    }
}
