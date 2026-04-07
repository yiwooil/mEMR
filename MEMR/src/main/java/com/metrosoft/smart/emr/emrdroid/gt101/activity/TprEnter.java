package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.R.style;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.Calendar;


public class TprEnter extends MyActivity {
    private int REQUEST_TIME = 1;

    private String mXmlTprEnter, mXmlTpr;

    private String mPid;
    private String mBededt;

    private int mChkDateYear, mChkDateMonth, mChkDateDay;
    private int mChkDateHour, mChkDateMinute;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tpr_enter);

        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");

        init();
        setListener();

        // 등록된 값 조회
        getTprOneRow();
    }

    @Override
    protected void onApplyThemeResource(Resources.Theme theme, int resid, boolean first) {
        super.onApplyThemeResource(theme, resid, first);
        // 이 다이얼로그의 제목과 테두리를 없앤다.
        theme.applyStyle(style.Theme_Panel, true);
    }

    private void init() {
        Calendar c = Calendar.getInstance();
        // 일자
        mChkDateYear = c.get(Calendar.YEAR);
        mChkDateMonth = c.get(Calendar.MONTH);
        mChkDateDay = c.get(Calendar.DAY_OF_MONTH);
        // 시간
        mChkDateHour = c.get(Calendar.HOUR_OF_DAY);
        mChkDateMinute = c.get(Calendar.MINUTE);

        displayChkDate();
        displayChkTime();
    }

    private void displayChkDate() {
        Button button = (Button) findViewById(R.id.chkDate);
        button.setText(
                new StringBuilder()
                        // Month is 0 based so add 1
                        .append(mChkDateYear).append(".")
                        .append(mChkDateMonth + 1).append(".")
                        .append(mChkDateDay).append(" ")
        );
    }

    private void displayChkTime() {
        int hh = mChkDateHour;
        int mm = mChkDateMinute;
        String ampm = "AM";
        if (hh > 12) {
            hh = hh - 12;
            ampm = "PM";
        }
        Button button = (Button) findViewById(R.id.chkTime);
        button.setText(
                new StringBuilder()
                        // Month is 0 based so add 1
                        .append(hh).append(":")
                        .append(mm).append(" ")
                        .append(ampm).append(" ")
        );
    }

    private String getDateString(int year, int month, int day) {
        String yearString = Integer.toString(year);
        String monthString = Integer.toString(month + 101);
        String dayString = Integer.toString(day + 100);
        String ret = yearString + monthString.substring(1, 3) + dayString.substring(1, 3);

        return ret;
    }

    private String getTimeString(int hour, int minute) {
        String hourString = Integer.toString(hour + 100);
        String minuteString = Integer.toString(minute + 100);
        String ret = hourString.substring(1, 3) + minuteString.substring(1, 3);

        return ret;
    }

    private void setListener() {
        ((Button) findViewById(R.id.chkDate)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DialogDatePicker();
            }
        });
        ((Button) findViewById(R.id.chkTime)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                DialogTimePicker();
            }
        });
        final Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                // 저장
                save();
            }
        });
        final Button cancelButton = (Button) findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
    }

    private void DialogDatePicker() {
        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            // onDateSet method
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mChkDateYear = year;
                mChkDateMonth = monthOfYear;
                mChkDateDay = dayOfMonth;
                displayChkDate();
                // 등록된 값 조회
                getTprOneRow();
            }
        };
        DatePickerDialog dlg = new DatePickerDialog(this, dateSetListener, mChkDateYear, mChkDateMonth, mChkDateDay);
        dlg.show();
    }

    private void DialogTimePicker() {
        Intent intent = new Intent(this, TimePickDialog.class);
        intent.putExtra("hour", mChkDateHour);
        intent.putExtra("minute", mChkDateMinute);
        startActivityForResult(intent, REQUEST_TIME);
		/*
	    TimePickerDialog.OnTimeSetListener timeSetListener = new TimePickerDialog.OnTimeSetListener() {
	        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
	    		mChkDateHour = hourOfDay;
	    		mChkDateMinute = minute;
	    		displayChkTime();
	    		// 등록된 값 조회
	    		getTprOneRow();
	        }
	    };
	    TimePickerDialog dlg = new TimePickerDialog(this, timeSetListener, mChkDateHour, mChkDateMinute, true);
	    dlg.show();
	    */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TIME) {
            if (resultCode == RESULT_OK) {
                mChkDateHour = data.getIntExtra("hour", mChkDateHour);
                mChkDateMinute = data.getIntExtra("minute", mChkDateMinute);
                displayChkTime();
                // 등록된 값 조회
                getTprOneRow();
            }
        }
    }

    private String getChkDate() {
        return getDateString(mChkDateYear, mChkDateMonth, mChkDateDay);
    }

    private String getChkTime() {
        return getTimeString(mChkDateHour, mChkDateMinute);
    }

    private void getTprOneRow() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String mode = "2";
                String chkDate = getChkDate();
                String chkTime = getChkTime();
                try {
                    String url = "TprServlet?hospitalid=" + hospitalId +
                            "&pid=" + mPid +
                            "&bededt=" + mBededt +
                            "&mode=" + mode +
                            "&chkdate=" + chkDate +
                            "&chktime=" + chkTime;
                    mXmlTpr = getXml(url);
                } catch (Exception e) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetTprOneRow();
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

    private void afterGetTprOneRow() {
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            // 값 초기화
            ((EditText) findViewById(R.id.bpMaxEditText)).setText("");
            ((EditText) findViewById(R.id.bpMinEditText)).setText("");
            ((EditText) findViewById(R.id.pulseEditText)).setText("");
            ((EditText) findViewById(R.id.breathEditText)).setText("");
            ((EditText) findViewById(R.id.tempEditText)).setText("");

            ResultSetHelper rs = new ResultSetHelper(mXmlTpr, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                Toast.makeText(TprEnter.this, R.string.no_data_message, Toast.LENGTH_LONG).show();
            } else {
                ((EditText) findViewById(R.id.bpMaxEditText)).setText(rs.getString(0, "maxbp"));
                ((EditText) findViewById(R.id.bpMinEditText)).setText(rs.getString(0, "minbp"));
                ((EditText) findViewById(R.id.pulseEditText)).setText(rs.getString(0, "pr"));
                ((EditText) findViewById(R.id.breathEditText)).setText(rs.getString(0, "rr"));
                ((EditText) findViewById(R.id.tempEditText)).setText(rs.getString(0, "tmp"));
            }
        } catch (Exception ex) {
            Toast.makeText(TprEnter.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void save() {
        String bpMax = ((EditText) findViewById(R.id.bpMaxEditText)).getText().toString();
        String bpMin = ((EditText) findViewById(R.id.bpMinEditText)).getText().toString();
        String pr = ((EditText) findViewById(R.id.pulseEditText)).getText().toString();
        String rr = ((EditText) findViewById(R.id.breathEditText)).getText().toString();
        String tmp = ((EditText) findViewById(R.id.tempEditText)).getText().toString();

        if (bpMax.equals("") && bpMin.equals("") && tmp.equals("") && pr.equals("") && rr.equals("")) {
            showSimpleDialog(getString(R.string.no_data_input_message));
            return;
        }

        mDialog = ProgressDialog.show(this, "", getString(R.string.process_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();

                String mode = "1";
                String chkDate = getChkDate();
                String chkTime = getChkTime();
                String bpMax = ((EditText) findViewById(R.id.bpMaxEditText)).getText().toString();
                String bpMin = ((EditText) findViewById(R.id.bpMinEditText)).getText().toString();
                String bp = bpMax + "/" + bpMin;
                String tmpCase = "고막";
                String pr = ((EditText) findViewById(R.id.pulseEditText)).getText().toString();
                String rr = ((EditText) findViewById(R.id.breathEditText)).getText().toString();
                String tmp = ((EditText) findViewById(R.id.tempEditText)).getText().toString();

                String url = "TprServlet?hospitalid=" + hospitalId +
                        "&pid=" + mPid +
                        "&bededt=" + mBededt +
                        "&mode=" + mode +
                        "&chkdate=" + chkDate +
                        "&chktime=" + chkTime +
                        "&bp=" + bp +
                        "&bpmax=" + bpMax +
                        "&bpmin=" + bpMin +
                        "&tmp=" + tmp +
                        "&tmpcase=" + getHangul(tmpCase) +
                        "&pr=" + pr +
                        "&rr=" + rr +
                        "&userid=" + userId;
                mXmlTprEnter = getXml(url);
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterSave();
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

    private void afterSave() {
        // 오류발생
        if (super.getXmlError() == true) {
            super.showToastText(super.getXmlErrorMessage());
            return;
        }
        String xml = mXmlTprEnter.substring(0, 2); // 뒤에 엔터문자가 따라온다. 엔터를 분리하여야 정확히 비교된다.
        if (xml.compareTo("OK") == 0) {
            setResult(RESULT_OK, null);
            finish();
        } else {
            showSimpleDialog(mXmlTprEnter);
        }
    }

}
