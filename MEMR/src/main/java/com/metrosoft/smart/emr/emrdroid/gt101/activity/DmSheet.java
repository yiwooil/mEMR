package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.ArrayList;

public class DmSheet extends MyActivity {
    protected static final int DM_ENTER = 0;

    private String mXmlPatientInfo, mXmlDm;

    private String mPid;
    private String mBededt;
    private String mBdiv;

    private ResultSetHelper mRs = null;
    private DmGraphicsView mDmView;
    private int mColorType = 0;
    private String patientInfo = "";
    AlertDialog mDmColordialog;

    private TextView mPatientInfoTextView;

    private float ratio = 1; // TPR디자인을 갤럭시탭 1280 * 800 으로했는데 노트10.1 은 2560*1600 이어서 작게표시된다. 이를 보완하기 위한 값.

    public void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mXmlPatientInfo = intent.getStringExtra("patientinfo");
        mBdiv = intent.getStringExtra("bdiv");
        if (mBdiv == null) mBdiv = "2"; // 1.외래 2.입원 3.응급  기본 입원

        // 해상도을 구하자
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int deviceWidth = displayMetrics.widthPixels;
        int deviceHeight = displayMetrics.heightPixels;
        ratio = 1; // 기본1
        if (deviceWidth < deviceHeight) {
            // 세로
            if (deviceWidth > 800) ratio = deviceWidth / 800f;
            //if(deviceWidth>=1600) ratio=2;
        } else {
            // 가로
            if (deviceWidth > 1280) ratio = deviceWidth / 1280f;
            //if(deviceWidth>=2500) ratio=2;
        }

        String fromTitle = intent.getStringExtra("fromtitle");
        if (fromTitle == null) fromTitle = "";
        if (fromTitle.equals("")) fromTitle = "닫기";

        LinearLayout linear = (LinearLayout) View.inflate(this, R.layout.dm, null);
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, linear, fromTitle);
        if (EmrSettingsUtil.getTprButtonHideYn(getBaseContext())) {
            // 등록버튼사용하지 않는다.
        } else {
            super.setButton1(true, "등록", super.BUTTON_TYPE_EDIT);
        }
        super.setLinkButton1(true, "처방");
        super.setLinkButton2(true, "TPR");
        super.setLinkButton3(true, "IO");

        mPatientInfoTextView = (TextView) findViewById(R.id.patientInfoTextView);
        mDmView = new DmGraphicsView(this);
        linear.addView(mDmView);

        // 2025.08.27 WOOIL - 레노버 단말기에서 하단이 잘려서 안 보인는 현상이 발생하여 강제로 여백을 줌.
        int width = ViewGroup.LayoutParams.MATCH_PARENT;
        int height = ViewGroup.LayoutParams.WRAP_CONTENT;
        ViewGroup.MarginLayoutParams params = new LinearLayout.LayoutParams(width, height);
        float bottmAreaHeightPx = getResources().getDimension(R.dimen.bottom_area_height);
        float bottomAreaHeightDp = bottmAreaHeightPx / getResources().getDisplayMetrics().density;
        params.setMargins(params.leftMargin, params.topMargin, params.rightMargin, (int) bottomAreaHeightDp);
        mDmView.setLayoutParams(params);

        // 버튼 리스너
        setListener();

        if (savedInstanceState == null) {
            getPatientInfoAndDm();
        } else {
            mXmlPatientInfo = savedInstanceState.getString("xmlPatientInfo");
            mXmlDm = savedInstanceState.getString("xmlDm");
            afterGetPatientInfo();
            afterGetDm();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == DM_ENTER) {
            if (resultCode == RESULT_OK) {
                getDm();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("xmlPatientInfo", mXmlPatientInfo);
        outState.putString("xmlDm", mXmlDm);
    }

    @Override
    public void onClickQueryButton(View v) {
        getDm();
    }

    @Override
    public void onClickButton1(View v) {
        // 등록
        if (mBdiv.equals("2") == false) {
            super.showSimpleDialog("입원환자만 사용 가능합니다.");
            return;
        }
        Intent intent;
        intent = new Intent(DmSheet.this, DmEnter.class);
        intent.putExtra("pid", mPid);
        intent.putExtra("bededt", mBededt);
        startActivityForResult(intent, DM_ENTER);
    }

    @Override
    public void onClickButton2(View v) {
        // �ɼ�
        if (mBdiv.equals("2") == false) {
            super.showSimpleDialog("입원환자만 사용 가능합니다.");
            return;
        }
        DmColorDialog dmColorDialog = new DmColorDialog(DmSheet.this);
        dmColorDialog.show();
    }

    @Override
    public void onClickLinkButton1(View v) {
        // 처방조회화면 호출
        Intent intent;
        intent = new Intent(DmSheet.this, Order.class);
        intent.putExtra("pid", mPid);
        intent.putExtra("bededt", mBededt);
        intent.putExtra("bdiv", mBdiv);
        intent.putExtra("patientinfo", mXmlPatientInfo);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClickLinkButton2(View v) {
        // TPR조회화면 호출
        Intent intent;
        intent = new Intent(DmSheet.this, TprSheet.class);
        intent.putExtra("pid", mPid);
        intent.putExtra("bededt", mBededt);
        intent.putExtra("bdiv", mBdiv);
        intent.putExtra("patientinfo", mXmlPatientInfo);
        startActivity(intent);
        finish();
    }

    @Override
    public void onClickLinkButton3(View v) {
        // TPR조회화면 호출
        Intent intent;
        intent = new Intent(DmSheet.this, IoSheet.class);
        intent.putExtra("pid", mPid);
        intent.putExtra("bededt", mBededt);
        intent.putExtra("bdiv", mBdiv);
        intent.putExtra("patientinfo", mXmlPatientInfo);
        startActivity(intent);
        finish();
    }

    private void setListener() {

    }

    private void getPatientInfoAndDm() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String url = "";
                // dm - 입원환자만 가능
                if (mBdiv.equals("2")) {
                    url = "TprServlet?mode=dmq&hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt;
                    mXmlDm = getXml(url);
                } else {
                    mXmlDm = "";
                }

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetPatientInfo();
                            afterGetDm();
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

    private void getDm() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                try {
                    // tpr - 입원환자만 가능
                    if (mBdiv.equals("2")) {
                        String url = "TprServlet?mode=dmq&hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt;
                        mXmlDm = getXml(url);
                    } else {
                        mXmlDm = "";
                    }
                } catch (Exception e) {
                    Toast.makeText(DmSheet.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetDm();
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

    private void DisplayPatientInfo() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                mPatientInfoTextView.setText(mXmlPatientInfo);
            }
        });
    }

    private void afterGetPatientInfo() {
        DisplayPatientInfo();
    }

    private void afterGetDm() {
        try {
            if (mBdiv.equals("2") == false) {
                super.showSimpleDialog("입원환자만 조회 가능합니다.");
                return;
            }
            // �����߻�
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            mRs = new ResultSetHelper(mXmlDm, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (mRs.getReturnCode() < 0) {
                showSimpleDialog(mRs.getReturnDesc());
            } else if (mRs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            }
        } catch (Exception ex) {
            mRs = null;
            Toast.makeText(DmSheet.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
        // �ٽ� �׸���
        mDmView.init();
        mDmView.invalidate();
    }

    public class DmGraphicsView extends View {
        // �� ����
        private int backgroundColor;
        private int lineColor;
        private int textColor;
        private int dateLineColor;
        private int dmColor;
        private int importantLineColor;

        // 페인트객체
        private Paint thickLine; // 두꺼운 줄
        private Paint normalLine; // 보통 두께 줄
        private Paint dotLine; // 점선
        private Paint thickPoint; // 점
        private Paint importantLine; // 두꺼운 빨간색 줄
        private Paint dateLine; // 날짜 변경선
        private Paint normalText; // 글씨
        private Paint dmPoint; // DM 표시점
        private Paint dmLine; // DM 연결선
        private Paint dmValueText; // 글씨

        // 화면 스크롤 용
        private float startX = 0;
        private float startY = 0;
        private int orientation = 0;
        private boolean isPort = true;
        private int showColumnCount = 39;
        private int startColNo = 0; // 맨 외쪽에 보여질 인덱스
        private int startRowNo = 0; // 0.체온 1.혈압등

        // canvas 좌표정보
        private float patientInfoCanvasTop = 0;
        private float patientInfoCanvasHeight = 0;
        private float dateCanvasTop = 0;
        private float dateCanvasHeight = 0;
        private float timeCanvasTop = 0;
        private float timeCanvasHeight = 0;
        private float dmCanvasTop = 0;
        private float dmCanvasHeight = 0;
        private float canvasWidth = 0;

        public DmGraphicsView(Context context) {
            super(context);

            // 색을 변경해보자
            setColors(0);
            setPaints();

            startX = 0;
            startY = 0;
            orientation = 0;
            isPort = true;
            showColumnCount = 39;
            startColNo = 0;
            startRowNo = 0;

        }

        public void init() {
            startColNo = 0;
            startRowNo = 0;
        }

        public void setPaints() {
            // 두꺼운 줄
            thickLine = new Paint();
            thickLine.setStrokeWidth(getPixelFromDip(2));
            thickLine.setStyle(Paint.Style.STROKE);
            thickLine.setColor(lineColor);
            thickLine.setAntiAlias(true);
            // 보통 두께 줄
            normalLine = new Paint();
            normalLine.setStrokeWidth(getPixelFromDip(1));
            normalLine.setStyle(Paint.Style.STROKE);
            normalLine.setColor(lineColor);
            normalLine.setAntiAlias(true);
            // 점선
            dotLine = new Paint();
            dotLine.setStrokeWidth(getPixelFromDip(1));
            dotLine.setStyle(Paint.Style.STROKE);
            dotLine.setColor(lineColor);
            dotLine.setAntiAlias(true);
            dotLine.setPathEffect(new DashPathEffect(new float[]{5, 5}, 0));
            // 점
            thickPoint = new Paint();
            thickPoint.setStrokeWidth(getPixelFromDip(5));
            thickPoint.setStyle(Paint.Style.STROKE);
            thickPoint.setColor(lineColor);
            thickPoint.setAntiAlias(true);
            // 두꺼운 빨간색 줄
            importantLine = new Paint();
            importantLine.setStrokeWidth(getPixelFromDip(2));
            importantLine.setStyle(Paint.Style.STROKE);
            importantLine.setColor(importantLineColor);
            importantLine.setAntiAlias(true);
            // 날짜 변경선
            dateLine = new Paint();
            dateLine.setStrokeWidth(getPixelFromDip(2));
            dateLine.setStyle(Paint.Style.STROKE);
            dateLine.setColor(dateLineColor);
            dateLine.setAntiAlias(true);
            // 글씨
            normalText = new Paint();
            float textSize = normalText.getTextSize();
            normalText.setStrokeWidth(getPixelFromDip(1));
            normalText.setStyle(Paint.Style.FILL);
            normalText.setColor(textColor);
            normalText.setTextSize(getPixelFromDip(textSize));
            normalText.setAntiAlias(true);
            // 글씨
            dmValueText = new Paint();
            dmValueText.setStrokeWidth(getPixelFromDip(1));
            dmValueText.setStyle(Paint.Style.FILL);
            dmValueText.setColor(textColor);
            dmValueText.setTextSize(getPixelFromDip(dmValueText.getTextSize()) * Float.parseFloat("1.5"));
            dmValueText.setAntiAlias(true);
            // 온도 표시점
            dmPoint = new Paint();
            dmPoint.setStrokeWidth((5));
            dmPoint.setStyle(Paint.Style.STROKE);
            dmPoint.setColor(dmColor);
            dmPoint.setAntiAlias(true);
            // 온도 연결선
            dmLine = new Paint();
            dmLine.setStrokeWidth(getPixelFromDip(2));
            dmLine.setStyle(Paint.Style.STROKE);
            dmLine.setColor(dmColor);
            dmLine.setAntiAlias(true);

        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            // 이 메소드는 뷰가 생성되고 안드로이드가 모든 것의 크기를 파악한 후에 호출된다.
            // orientation = 0 : 세로
            //               1 : 가로
            //               2 : 세로(뒤집힌 세로)
            //               3 : 가로(뒤집힌 가로)
            // 갤럭시 탭 10.1은 가로로 넓은 것이 세로임.
            orientation = getWindowManager().getDefaultDisplay().getOrientation();
            if (orientation == 0 || orientation == 2) {
                showColumnCount = 39;
            } else {
                showColumnCount = 23;
            }

            // canvas 좌표정보
            float bottomMargin = 30;
            float rightMargin = 30;
            if (isPort == false) {
                bottomMargin = 20;
                rightMargin = 40;
            }
            patientInfoCanvasTop = 20f;
            patientInfoCanvasHeight = 20f;
            // 일자표시부분
            dateCanvasTop = patientInfoCanvasTop + patientInfoCanvasHeight;
            //dateCanvasHeight = 60f; // HOD가 표시된 경우
            dateCanvasHeight = 40f;
            // 시간표시부분
            timeCanvasTop = dateCanvasTop + dateCanvasHeight;
            timeCanvasHeight = 60f;
            // 체온표시부분
            dmCanvasTop = timeCanvasTop + timeCanvasHeight;
            dmCanvasHeight = getDipFromPixel(getHeight()) - dmCanvasTop - bottomMargin;

            canvasWidth = getDipFromPixel(getWidth()) - rightMargin;

            super.onSizeChanged(w, h, oldw, oldh);

            Log.d("EmrDroid", "getWidth=" + getWidth() + ", getHeight()=" + getHeight());
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);

            //if (mRows==null) return true;
            if (mRs == null) return true;
            if (mRs.getReturnCode() <= 0) return true;
            if (mRs.getRecordCount() < showColumnCount) return true;

            float xp = event.getX();
            float yp = event.getY();


            // 그래프가 아니고 환자정보가 출력되는 곳에서는 스크롤 되지 않게 막는다.
            if (yp <= patientInfoCanvasTop + patientInfoCanvasHeight) {
                return true;
            }

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
                    if (scrollByX < 0) {
                        // 화면을 찍어서 왼쪽으로 미는 동작임.
                        // 오른쪽에 있는 자료가 보여지게 처리한다.
                        startColNo += ((scrollByX * -1) / 8);
                        if (startColNo > mRs.getRecordCount() - 3) {
                            startColNo = mRs.getRecordCount() - 2;
                        }
                        //if (mStartColNo<0) mStartColNo=0;
                    } else if (scrollByX > 0) {
                        // 화면을 찍어서 오른쪽으로 미는 동작임.
                        // 왼쪽에 있는 자료가 보여지게 처리한다.
                        startColNo -= (scrollByX / 8);
                        if (startColNo < 0) {
                            startColNo = 0;
                        }
                    }
                    // 다시 시작
                    startX = x;
                    startY = y;
                    // 다시 그리기
                    invalidate();
                    break;
            }

            return true;
        }

        @Override
        protected void onDraw(Canvas canvas) {

            if (mRs == null) return;
            if (mRs.getReturnCode() <= 0) return;

            setBackgroundColor(backgroundColor);


            // 체온자료
            ArrayList<PointF> dmArrayList = new ArrayList<PointF>();


            // 날짜 변경선
            int[] dayBreak = new int[100];
            for (int i = 0; i < 100; i++) {
                dayBreak[i] = 0;
            }

            try {
                if (mRs.getReturnCode() < 0) {

                } else if (mRs.getReturnCode() == 0) {

                } else {

                    double maxValue = 0;//cols.getDouble("MAX_VALUE");
                    double minValue = 0;//cols.getDouble("MIN_VALUE");
                    int iCnt = mRs.getRecordCount();
                    for (int i = 0; i < iCnt; i++) {
                        String dm = mRs.getString(i, "n_value");
                        try {
                            double value = Double.parseDouble(dm);
                            if (i == 0) {
                                maxValue = value;
                                minValue = value;
                            } else {
                                if (value < minValue) minValue = value;
                                if (value > maxValue) maxValue = value;
                            }
                        } catch (Exception ex) {
                        }
                    }
                    minValue -= 10;
                    if (minValue < 0) minValue = 0;
                    minValue = ((int) (minValue / 10)) * 10;
                    maxValue += 10;
                    maxValue = ((int) (maxValue / 10)) * 10;

                    canvas.translate(20, 0);


                    // 환자정보 출력
                    canvasDrawText(canvas, patientInfo, 0, patientInfoCanvasTop, normalText);

                    int xGap = 30;

                    // -------------------------------------------------------------------------
                    // 일자
                    // -------------------------------------------------------------------------

                    // 배경색 입히기
                    Paint p;
                    p = new Paint();
                    p.setStrokeWidth(getPixelFromDip(2));
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(getResources().getColor(R.color.tpr_date_color));
                    p.setAntiAlias(true);
                    canvasDrawRect(canvas, 0, dateCanvasTop, canvasWidth, dateCanvasTop + 40, p);

                    // 월일
                    canvasDrawRect(canvas, 0, dateCanvasTop, canvasWidth, dateCanvasTop + 40, thickLine);
                    canvasDrawRect(canvas, 0, dateCanvasTop, 2 * xGap, dateCanvasTop + 40, thickLine);
                    canvasDrawLine(canvas, 2 * xGap, dateCanvasTop + 20, canvasWidth, dateCanvasTop + 20, dotLine);
                    // 월일-글자
                    canvasDrawText(canvas, "일자", 20, dateCanvasTop + 25, normalText);
                    // 일자 출력
                    String bkChkdt = "";
                    for (int turn = 1; turn <= showColumnCount; turn++) {
                        int i = turn + startColNo;
                        if (i > mRs.getRecordCount()) {
                            // 끝까지 간것이 아니고 , 중간에 종료된 것임.
                            canvasDrawLine(canvas, (turn + 1) * xGap, dateCanvasTop + 0, (turn + 1) * xGap, dateCanvasTop + 40, dateLine);
                            dayBreak[turn + 1] = 1;
                            break;
                        }
                        //
                        String chkdt = mRs.getString(i - 1, "chkdt");
                        String hod = "";
                        String pod = "";
                        if (bkChkdt.equals("") || bkChkdt.equals(chkdt) == false) {
                            // 일자
                            String month = chkdt.substring(4, 6);
                            String day = chkdt.substring(6, 8);
                            float x = (float) (turn + 1) * (float) xGap + (xGap / 3);
                            canvasDrawText(canvas, month, x, dateCanvasTop + 15, normalText);
                            canvasDrawText(canvas, day, x, dateCanvasTop + 35, normalText);
                            canvasDrawText(canvas, hod, x, dateCanvasTop + 55, normalText);
                            if (bkChkdt.equals("") == false) {
                                canvasDrawLine(canvas, (turn + 1) * xGap, dateCanvasTop + 0, (turn + 1) * xGap, dateCanvasTop + 40, dateLine);
                                dayBreak[turn + 1] = 1;
                            }
                            bkChkdt = chkdt;
                        }
                    }

                    // ---------------------------------------------------------------------------
                    // 시간
                    // ---------------------------------------------------------------------------

                    // 배경색 입히기
                    Paint p2;
                    p2 = new Paint();
                    p2.setStrokeWidth(getPixelFromDip(2));
                    p2.setStyle(Paint.Style.FILL);
                    p2.setColor(getResources().getColor(R.color.tpr_time_color));
                    p2.setAntiAlias(true);
                    canvasDrawRect(canvas, 0, timeCanvasTop, canvasWidth, timeCanvasTop + 60, p2);

                    // 오전오후
                    canvasDrawRect(canvas, 0, timeCanvasTop + 0, canvasWidth, timeCanvasTop + 20, thickLine);
                    canvasDrawRect(canvas, 0, timeCanvasTop + 0, 2 * xGap, timeCanvasTop + 20, thickLine);
                    // 시분
                    canvasDrawRect(canvas, 0, timeCanvasTop + 20, canvasWidth, timeCanvasTop + 60, thickLine);
                    canvasDrawRect(canvas, 0, timeCanvasTop + 20, 2 * xGap, timeCanvasTop + 60, thickLine);
                    canvasDrawLine(canvas, 2 * xGap, timeCanvasTop + 40, canvasWidth, timeCanvasTop + 40, dotLine);
                    // 오전오후-글자
                    canvasDrawText(canvas, "오전-오후", 5, timeCanvasTop + 15, normalText);
                    // 시분-글자
                    canvasDrawText(canvas, "시분", 20, timeCanvasTop + 45, normalText);
                    // 세로줄 출력
                    for (float i = 2; true; i++) {
                        if (i * xGap > canvasWidth) break;
                        if (dayBreak[(int) i] == 1) {
                            canvasDrawLine(canvas, i * xGap, timeCanvasTop + 0, i * xGap, timeCanvasTop + 60, dateLine);
                        } else {
                            canvasDrawLine(canvas, i * xGap, timeCanvasTop + 0, i * xGap, timeCanvasTop + 60, normalLine);
                        }
                    }
                    // 시간출력
                    // 데이터 출력
                    for (int turn = 1; turn <= showColumnCount; turn++) {
                        int i = turn + startColNo;
                        if (i > mRs.getRecordCount()) break;
                        //cols=mRows.getJSONObject(i);

                        // 시간
                        String chktm = mRs.getString(i - 1, "chktm");
                        String chkhh = chktm.substring(0, 2);
                        String chkmm = chktm.substring(2, 4);
                        String disphh = chkhh; // 출력용
                        float x = (float) (turn + 1) * (float) xGap + (xGap / 3);
                        // 오전-오후
                        String ampm = "A";
                        if (chktm.compareTo("1300") >= 0 && chktm.compareTo("2359") <= 0) {
                            try {
                                disphh = (Integer.parseInt(chkhh) - 12) + "";
                            } catch (Exception ex) {
                            }
                            ampm = "P";
                        } else if (chktm.compareTo("0000") >= 0 && chktm.compareTo("0059") <= 0) {
                            ampm = "P";
                            disphh = "12";
                        } else {
                            try {
                                disphh = (Integer.parseInt(chkhh)) + "";
                            } catch (Exception ex) {
                            }
                        }
                        if (disphh.length() == 1) {
                            disphh = " " + disphh;
                        }
                        canvasDrawText(canvas, ampm, x, timeCanvasTop + 15, normalText);
                        // 시분
                        canvasDrawText(canvas, disphh, x, timeCanvasTop + 35, normalText);
                        canvasDrawText(canvas, chkmm, x, timeCanvasTop + 55, normalText);
                    }

                    // -------------------------------------------------------------------------
                    // DM
                    // -------------------------------------------------------------------------

                    //위에서 구해놓았음.
                    //maxValue = 600;
                    //minValue = 50;

                    // 배경색 입히기
                    Paint p3;
                    p3 = new Paint();
                    p3.setStrokeWidth(getPixelFromDip(2));
                    p3.setStyle(Paint.Style.FILL);
                    p3.setColor(getResources().getColor(R.color.tpr_tmp_color));
                    p3.setAntiAlias(true);
                    canvasDrawRect(canvas, 0, dmCanvasTop, xGap * 2, dmCanvasTop + dmCanvasHeight, p3);

                    canvasDrawRect(canvas, 0, dmCanvasTop + 0, canvasWidth, dmCanvasTop + dmCanvasHeight, thickLine);

                    // 제목(DM) 출력.. 하지말자
                    //{
                    //	float x = (xGap/3);
                    //	float y = getYPos(maxValue-100, dmCanvasHeight, minValue, maxValue) + dmCanvasTop;
                    //	canvasDrawText(canvas,"D", x, y, normalText);
                    //	y = getYPos(maxValue-120, dmCanvasHeight, minValue, maxValue) + dmCanvasTop;
                    //	canvasDrawText(canvas,"M", x, y, normalText);
                    //}
                    // Y축 좌표값 출력
                    for (double i = minValue; i <= maxValue; i += 10) {
                        float y = getYPos(i, dmCanvasHeight, minValue, maxValue) + dmCanvasTop + 4;
                        float x = (float) 1 * (float) xGap + (xGap / 3) - 5;
                        if (i != minValue && i != maxValue) {
                            canvasDrawText(canvas, (int) i + "", x, y, normalText);
                        }
                    }
                    // 가로줄 출력
                    for (double i = minValue; i <= maxValue; i += 10) {
                        float y = getYPos(i, dmCanvasHeight, minValue, maxValue) + dmCanvasTop;
                        float x = (float) 2 * (float) xGap;
                        if (i == 37) {
                            canvasDrawLine(canvas, x, y, canvasWidth, y, importantLine);
                        } else {
                            canvasDrawLine(canvas, x, y, canvasWidth, y, normalLine);
                        }
                    }
                    // 세로줄 출력
                    for (float i = 1; true; i++) {
                        if (i * xGap > canvasWidth) break;
                        float y1 = getYPos(minValue, dmCanvasHeight, minValue, maxValue) + dmCanvasTop;
                        float y2 = getYPos(maxValue, dmCanvasHeight, minValue, maxValue) + dmCanvasTop;
                        if (i == 2) {
                            canvasDrawLine(canvas, i * xGap, y1, i * xGap, y2, thickLine);
                        } else if (dayBreak[(int) i] == 1) {
                            canvasDrawLine(canvas, i * xGap, y1, i * xGap, y2, dateLine);
                        } else {
                            canvasDrawLine(canvas, i * xGap, y1, i * xGap, y2, normalLine);
                        }
                    }
                    // 데이터 출력
                    for (int turn = 1; turn <= showColumnCount; turn++) {
                        int i = turn + startColNo;
                        if (i > mRs.getRecordCount()) break;
                        // DM
                        String dm = mRs.getString(i - 1, "n_value");
                        try {
                            int value = Integer.parseInt(dm);
                            float x = ((turn + 1) * xGap) + (xGap / 2);
                            float y = getYPos(value, dmCanvasHeight, minValue, maxValue) + dmCanvasTop;
                            dmArrayList.add(new PointF(x, y));
                            canvasDrawPoint(canvas, x, y, dmPoint); // 점을 찍는다.
                            // 점 옆에 값을 출력하는데
                            // 다음 값이 현재 값보다 크면 오른쪽 아랬쪽에 출력하고,
                            //                       작으면 오른쪽 위쪽에 출력한다.
                            // 그래서, 다음 값을 읽어야한다.
                            String dm_next = "";
                            if (i < mRs.getRecordCount()) dm_next = mRs.getString(i, "n_value");
                            int value_next = 0;
                            try {
                                value_next = Integer.parseInt(dm_next);
                            } catch (Exception ex) {
                            }
                            if (value_next > value) {
                                canvasDrawText(canvas, value + "", x + 3, y + 17, dmValueText); // 점 옆.아래에 값을 출력한다.
                            } else {
                                canvasDrawText(canvas, value + "", x + 3, y, dmValueText); // 점 옆.위에 값을 출력한다.
                            }
                        } catch (Exception ex) {
                        }
                    }
                    // 그리자
                    drawLineArrayList(dmArrayList, canvas, dmLine);

                }
            } catch (Exception ex) {
                showSimpleDialog(ex.getMessage());
            }
        }

        private float getYPos(float value, float canvasHeight, double minValue, double maxValue) {
            float v = (float) maxValue - ((float) value - (float) minValue);
            float f = ((float) canvasHeight * ((float) v - (float) minValue)) / ((float) maxValue - (float) minValue);
            return f;
        }

        private float getYPos(double value, float canvasHeight, double minValue, double maxValue) {
            return getYPos((float) value, canvasHeight, minValue, maxValue);
        }

        private void drawLineArrayList(ArrayList<PointF> arrayList, Canvas canvas, Paint paint/*,int color*/) {
            for (int i = 0; i < arrayList.size() - 1; i++) {
                PointF p1 = (PointF) arrayList.get(i);
                PointF p2 = (PointF) arrayList.get(i + 1);
                canvasDrawLine(canvas, p1.x, p1.y, p2.x, p2.y, paint);
            }
        }

        public void setColors(int colorType) {
            mColorType = colorType;
            switch (colorType) {
                case 0:
                    // 기본
                    backgroundColor = Color.WHITE;
                    lineColor = Color.DKGRAY;
                    textColor = Color.DKGRAY;
                    dateLineColor = Color.RED;
                    dmColor = Color.BLUE;
                    importantLineColor = Color.RED;
                    break;
                case 1:
                    // 자정
                    backgroundColor = Color.BLACK;
                    lineColor = Color.GRAY;
                    textColor = Color.GRAY;
                    dateLineColor = Color.argb(175, 255, 0, 0);
                    dmColor = Color.BLUE;
                    importantLineColor = Color.RED;
                    break;
                case 2:
                    // 새벽
                    backgroundColor = Color.GRAY;
                    lineColor = Color.LTGRAY;
                    textColor = Color.LTGRAY;
                    dateLineColor = Color.DKGRAY;
                    dmColor = Color.argb(175, 255, 50, 50);
                    importantLineColor = Color.RED;
                    break;
            }
        }

    }

    private class DmColorDialog extends Dialog {
        // 이 다이얼로그의 타이틀은 trp_color_dialog.xml에 있음.
        RadioGroup colorRadioGroup;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.tpr_color_dialog);

            // 제목
            TextView tv = (TextView) findViewById(R.id.custom_dialog_title_bar_text);
            tv.setText(R.string.select_color);

            initRadioButton();
            setListener();

        }

        public DmColorDialog(Context context) {
            super(context);
        }

        private void initRadioButton() {
            colorRadioGroup = (RadioGroup) findViewById(R.id.colorRadioGroup);
            RadioButton radio;
            switch (mColorType) {
                case 0:
                    radio = (RadioButton) findViewById(R.id.defaultColorRadio);
                    radio.setChecked(true);
                    break;
                case 1:
                    radio = (RadioButton) findViewById(R.id.midnightColorRadio);
                    radio.setChecked(true);
                    break;
                case 2:
                    radio = (RadioButton) findViewById(R.id.dawnColorRadio);
                    radio.setChecked(true);
                    break;
            }
        }

        private void setListener() {
            final Button applyButton = (Button) findViewById(R.id.color_apply_button);
            applyButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View view) {
                    int checkedId = colorRadioGroup.getCheckedRadioButtonId();
                    int colorValue = 0;
                    if (checkedId == R.id.defaultColorRadio) {
                        colorValue = 0;
                    } else if (checkedId == R.id.midnightColorRadio) {
                        colorValue = 1;
                    } else if (checkedId == R.id.dawnColorRadio) {
                        colorValue = 2;
                    }
                    mDmView.setColors(colorValue);
                    mDmView.setPaints();
                    mDmView.invalidate();
                    dismiss();
                }
            });
            final Button cancelButton = (Button) findViewById(R.id.color_cancel_button);
            cancelButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View view) {
                    dismiss();
                }
            });
        }
    }

    private void canvasDrawRect(Canvas canvas, float left, float top, float right, float bottom, Paint paint) {
        canvas.drawRect(getPixelFromDip(left), getPixelFromDip(top), getPixelFromDip(right), getPixelFromDip(bottom), paint);
    }

    private void canvasDrawText(Canvas canvas, String text, float x, float y, Paint paint) {
        canvas.drawText(text, getPixelFromDip(x), getPixelFromDip(y), paint);
    }

    private void canvasDrawLine(Canvas canvas, float startX, float startY, float stopX, float stopY, Paint paint) {
        canvas.drawLine(getPixelFromDip(startX), getPixelFromDip(startY), getPixelFromDip(stopX), getPixelFromDip(stopY), paint);
    }

    private void canvasDrawPoint(Canvas canvas, float x, float y, Paint paint) {
        canvas.drawPoint(getPixelFromDip(x), getPixelFromDip(y), paint);
    }

    private float getPixelFromDip(float dipValue) {
        // 10.1 보다 작은 단말기에서도 동작하도록 ratio를사용하였음.
        return dipValue * ratio;
    }

    private float getDipFromPixel(float pixel) {
        // 10.1 보다 작은 단말기에서도 동작하도록 ratio를사용하였음.
        return pixel / ratio;
    }

}
