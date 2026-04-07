package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.OrderAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class NrChartWrite extends MyActivity {

    public static final String MODE_INSERT = "I";
    public static final String MODE_UPDATE = "U";

    private EditText mDateEditText;
    private EditText mTimeEditText;
    private Button mSaveButton;
    private Button mCancelButton;
    private Button mAiButton;

    private TextView mOriginalTextView;
    private EditText mOriginalEditText;
    private TextView mAiResultTextView;
    private EditText mAiResultEditText;

    private String mMode;
    private String mPid;
    private String mBededt;
    private String mBdiv;
    private String mDrid;

    private String mWdate;
    private String mSeq;
    private String mWtime;
    private String mResult;
    private String mEmpid;
    private String mEmpnm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Dialog 형태로 보이게
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.nr_chart_write);

        // 화면 크기를 dialog처럼 적당히 크게
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.95f);
        params.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.6f);
        getWindow().setAttributes(params);

        // 바깥 터치로 닫히지 않게
        setFinishOnTouchOutside(false);

        initControls();
        getIntentExtra();
        bindData();
        bindEvent();

        // 신규등록일 때만 선택 가능 (수정모드는 막힘)
        if (!MODE_UPDATE.equals(mMode)) {

            // 일자 클릭
            mDateEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDateTimePicker();
                }
            });

            // 시간 클릭
            mTimeEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTimePicker();
                }
            });
        }
    }

    private void initControls() {
        mDateEditText = (EditText) findViewById(R.id.nr_chart_write_date);
        mTimeEditText = (EditText) findViewById(R.id.nr_chart_write_time);

        mSaveButton = (Button) findViewById(R.id.nr_chart_write_save);
        mCancelButton = (Button) findViewById(R.id.nr_chart_write_cancel);
        mAiButton = (Button) findViewById(R.id.nr_chart_write_ai);

        mOriginalTextView = (TextView) findViewById(R.id.nr_chart_write_original_label);
        mOriginalEditText = (EditText) findViewById(R.id.nr_chart_write_original);
        mAiResultTextView = (TextView) findViewById(R.id.nr_chart_write_ai_result_label);
        mAiResultEditText = (EditText) findViewById(R.id.nr_chart_write_ai_result);

        mDateEditText.setFocusable(false);
        mDateEditText.setClickable(true);

        mTimeEditText.setFocusable(false);
        mTimeEditText.setClickable(true);

        mDateEditText.setInputType(InputType.TYPE_NULL);
        mTimeEditText.setInputType(InputType.TYPE_NULL);

        // 내용 입력칸 여러 줄
        mOriginalEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mOriginalEditText.setSingleLine(false);

        mAiResultEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        mAiResultEditText.setSingleLine(false);

        boolean nrChartAiYn = EmrSettingsUtil.getNrChartAiYn(this);
        if (nrChartAiYn) {
            mAiButton.setVisibility(View.VISIBLE);
            mAiResultTextView.setVisibility(View.VISIBLE);
            mAiResultEditText.setVisibility(View.VISIBLE);
        } else {
            mAiButton.setVisibility(View.GONE);
            mAiResultTextView.setVisibility(View.GONE);
            mAiResultEditText.setVisibility(View.GONE);
        }
    }

    private void getIntentExtra() {
        if (getIntent() == null) return;

        mMode = getIntent().getStringExtra("mode");
        mPid = getIntent().getStringExtra("pid");
        mBededt = getIntent().getStringExtra("bededt");
        mBdiv = getIntent().getStringExtra("bdiv");
        mDrid = getIntent().getStringExtra("drid");

        mWdate = getIntent().getStringExtra("wdate");
        mSeq = getIntent().getStringExtra("seq");
        mWtime = getIntent().getStringExtra("wtime");
        mResult = getIntent().getStringExtra("result");
        mEmpid = getIntent().getStringExtra("empid");
        mEmpnm = getIntent().getStringExtra("empnm");

        if (mMode == null) mMode = MODE_INSERT;
        if (mPid == null) mPid = "";
        if (mBededt == null) mBededt = "";
        if (mBdiv == null) mBdiv = "";

        if (mWdate == null) mWdate = "";
        if (mSeq == null) mSeq = "";
        if (mWtime == null) mWtime = "";
        if (mResult == null) mResult = "";
        if (mEmpid == null) mEmpid = "";
        if (mEmpnm == null) mEmpnm = "";
    }

    private void bindData() {
        mDateEditText.setText(mWdate);
        mTimeEditText.setText(mWtime);

        mOriginalEditText.setText(mResult);
        mAiResultEditText.setText("");

        // 수정 모드이면 일자/시간 수정 불가
        if (MODE_UPDATE.equals(mMode)) {
            mDateEditText.setEnabled(false);
            mDateEditText.setFocusable(false);
            mDateEditText.setClickable(false);

            mTimeEditText.setEnabled(false);
            mTimeEditText.setFocusable(false);
            mTimeEditText.setClickable(false);
        } else {
            // 신규등록 시 기본값
            if ("".equals(mWdate)) {
                if ("2".equalsIgnoreCase(mBdiv)) {
                    mDateEditText.setText(getCurrentDateYYYYMMDD());
                } else {
                    mDateEditText.setText(mBededt);
                }
            }
            if ("".equals(mWtime)) mTimeEditText.setText(getCurrentTimeHHmm());
        }
    }

    private void bindEvent() {
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickSave();
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        mAiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickAi();
            }
        });
    }

    private void onClickSave() {
        String wdate = mDateEditText.getText().toString().trim();
        String wtime = mTimeEditText.getText().toString().trim();

        String original = mOriginalEditText.getText().toString().trim();
        String ai = mAiResultEditText.getText().toString().trim();

        // AI 결과가 있으면 우선 저장
        String result = "".equals(ai) ? original : ai;

        if ("".equals(wdate)) {
            Toast.makeText(this, "일자를 입력하세요.", Toast.LENGTH_SHORT).show();
            //mDateEditText.requestFocus();
            return;
        }

        if ("".equals(wtime)) {
            Toast.makeText(this, "시간을 입력하세요.", Toast.LENGTH_SHORT).show();
            //mTimeEditText.requestFocus();
            return;
        }

        if ("".equals(result)) {
            Toast.makeText(this, "내용을 입력하세요.", Toast.LENGTH_SHORT).show();
            mOriginalEditText.requestFocus();
            return;
        }

        // 저장
        writeNrChart(wdate, wtime, result);
    }

    private void onClickAi() {
        String currentText = mOriginalEditText.getText().toString();

        if ("".equals(currentText.trim())) {
            Toast.makeText(this, "구조화할 내용을 먼저 입력하세요.", Toast.LENGTH_SHORT).show();
            mOriginalEditText.requestFocus();
            return;
        }

        // 지금 단계에서는 보관만 확실히 하고,
        // 실제 AI 호출 결과가 오면 mResultEditText에 반영
        requestAiStructuredText(currentText);
    }

    private String getCurrentDateYYYYMMDD() {
        java.util.Calendar c = java.util.Calendar.getInstance();

        int year = c.get(java.util.Calendar.YEAR);
        int month = c.get(java.util.Calendar.MONTH) + 1;
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);

        return String.format("%04d%02d%02d", year, month, day);
    }
    private String getCurrentTimeHHmm() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int hour = c.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = c.get(java.util.Calendar.MINUTE);

        String hourString = Integer.toString(hour + 100).substring(1, 3);
        String minuteString = Integer.toString(minute + 100).substring(1, 3);

        return hourString + minuteString;
    }

    private void showDateTimePicker() {
        final java.util.Calendar c = java.util.Calendar.getInstance();

        DatePickerDialog dateDialog = new DatePickerDialog(
                NrChartWrite.this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, final int year, final int month, final int dayOfMonth) {

                        // 날짜 먼저 설정
                        String date = String.format("%04d%02d%02d", year, month + 1, dayOfMonth);
                        mDateEditText.setText(date);
                    }
                },
                c.get(java.util.Calendar.YEAR),
                c.get(java.util.Calendar.MONTH),
                c.get(java.util.Calendar.DAY_OF_MONTH)
        );

        dateDialog.show();
    }

    private void showTimePicker() {
        final java.util.Calendar c = java.util.Calendar.getInstance();

        TimePickerDialog timeDialog = new TimePickerDialog(
                NrChartWrite.this,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

                        String time = String.format("%02d%02d", hourOfDay, minute);
                        mTimeEditText.setText(time);
                    }
                },
                c.get(java.util.Calendar.HOUR_OF_DAY),
                c.get(java.util.Calendar.MINUTE),
                true
        );

        timeDialog.show();
    }

    private void writeNrChart(final String wdate, final String wtime, final String result){
        //mDialog = ProgressDialog.show(NrChartWrite.this, "", "간호기록지 저장 중입니다.", true);
        mDialog = new ProgressDialog(NrChartWrite.this, R.style.CustomProgressDialog);
        mDialog.setMessage("간호기록지 저장 중입니다.          @");
        mDialog.setCancelable(false);
        mDialog.show();

        new Thread(new Runnable() {
            public void run() {
                try {
                    String hospitalId = getHospitalId();
                    String userId = getUserId();
                    String url = "";

                    String mode = "17"; // 간호기록 저장
                    url += "ChartServlet";
                    url += "?hospitalid=" + hospitalId;
                    url += "&userid=" + userId;
                    url += "&mode=" + mode;
                    url += "&pid=" + mPid;
                    url += "&bededt=" + mBededt;
                    url += "&bdiv=" + mBdiv;
                    url += "&wdate=" + wdate;
                    url += "&seq=" + mSeq;
                    url += "&wtime=" + wtime;
                    url += "&pdrid=" + mDrid;
                    url += "&result=" + URLEncoder.encode(result, "UTF-8");

                    final String xml = getXml(url);

                    mHandler.post(new Runnable() {
                        public void run() {
                            // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                            // 이를 방지함.
                            try {
                                mDialog.dismiss();

                                if (xml.startsWith("Y") == false) {
                                    showSimpleDialog(xml);
                                } else {
                                    setResult(RESULT_OK, getIntent());
                                    finish();
                                }

                            } catch (Exception e) {
                                Log.d("EmrDroid", "dialog.dismiss exception");
                                Log.d("EmrDroid", e.getMessage());
                            }
                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    showSimpleDialog(e.getMessage());
                } catch (Exception e) {
                    showSimpleDialog(e.getMessage());
                }
            }
        }).start();
    }

    private void requestAiStructuredText(final String currentText) {
        //mDialog = ProgressDialog.show(NrChartWrite.this, "", "A.I. 처리 중입니다.", true);
        mDialog = new ProgressDialog(NrChartWrite.this, R.style.CustomProgressDialog);
        mDialog.setMessage("A.I. 처리 중입니다.          @");
        mDialog.setCancelable(false);
        mDialog.show();

        new Thread(new Runnable() {
            public void run() {
                try {
                    String wdate = mDateEditText.getText().toString().trim();
                    String wtime = mTimeEditText.getText().toString().trim();

                    String hospitalId = getHospitalId();
                    String userId = getUserId();
                    String url = "";

                    String mode = "4"; // AI호출
                    url += "CommonCodeServlet";
                    url += "?hospitalid=" + hospitalId;
                    url += "&userid=" + userId;
                    url += "&mode=" + mode;
                    url += "&pid=" + mPid;
                    url += "&bededt=" + mBededt;
                    url += "&bdiv=" + mBdiv;
                    url += "&wdate=" + wdate;
                    url += "&wtime=" + wtime;
                    url += "&currenttext=" + URLEncoder.encode(currentText, "UTF-8");

                    final String xml = getXml(url, 120); // 120초 대기

                    mHandler.post(new Runnable() {
                        public void run() {
                            // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                            // 이를 방지함.
                            try {
                                mDialog.dismiss();

                                afterRequestAiStructuredText(xml);

                            } catch (Exception e) {
                                Log.d("EmrDroid", "dialog.dismiss exception");
                                Log.d("EmrDroid", e.getMessage());
                            }

                        }
                    });
                } catch (UnsupportedEncodingException e) {
                    showSimpleDialog(e.getMessage());
                } catch (Exception e) {
                    showSimpleDialog(e.getMessage());
                }
            }
        }).start();
    }

    private void afterRequestAiStructuredText(final String xml) {
        ResultSetHelper rs;
        try{
            rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));

            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                final String result = rs.getString(0, "result");
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAiResultEditText.setText(result);
                        enableAiResultEdit(); // 수정할 수 있게...
                    }
                });
            }
        } catch (Exception e) {
            Toast.makeText(NrChartWrite.this, "A.I. 응답 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }

    }

    private void enableAiResultEdit() {
        mAiResultEditText.setEnabled(true);
        mAiResultEditText.setFocusable(true);
        mAiResultEditText.setFocusableInTouchMode(true);
        mAiResultEditText.setClickable(true);
        mAiResultEditText.setLongClickable(true);
        mAiResultEditText.setCursorVisible(true);
    }

}