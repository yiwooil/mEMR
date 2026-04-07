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

public class DmEnter extends MyActivity {
    private int REQUEST_TIME = 1;

    private String mXmlDmEnter, mXmlDm;

    private String mPid;
    private String mBededt;

    private int mChkDateYear, mChkDateMonth, mChkDateDay;
    private int mChkDateHour, mChkDateMinute;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.dm_enter);

        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");

        init();
        setListener();

        // 등록된 값 조회
        getDmOneRow();
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
        DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            // onDateSet method
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                mChkDateYear = year;
                mChkDateMonth = monthOfYear;
                mChkDateDay = dayOfMonth;
                displayChkDate();
                // 등록된 값 조회
                getDmOneRow();
            }
        };
        DatePickerDialog alert = new DatePickerDialog(this, mDateSetListener, mChkDateYear, mChkDateMonth, mChkDateDay);
        alert.show();
    }

    private void DialogTimePicker() {
        Intent intent = new Intent(this, TimePickDialog.class);
        intent.putExtra("hour", mChkDateHour);
        intent.putExtra("minute", mChkDateMinute);
        startActivityForResult(intent, REQUEST_TIME);
		/*
	    TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
	        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
	    		mChkDateHour = hourOfDay;
	    		mChkDateMinute = minute;
	    		displayChkTime();
	    		// 등록된 값 조회
	    		getTprOneRow();
	        }
	    };
	    TimePickerDialog alert = new TimePickerDialog(this,	mTimeSetListener, mChkDateHour, mChkDateMinute, true);
	    alert.show();
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
                getDmOneRow();
            }
        }
    }

    private String getChkDate() {
        return getDateString(mChkDateYear, mChkDateMonth, mChkDateDay);
    }

    private String getChkTime() {
        return getTimeString(mChkDateHour, mChkDateMinute);
    }

    private void getDmOneRow() {
        final String hospitalId = getHospitalId();
        final String userId = getUserId();
        final String mode = "dmq1";
        final String chkDate = getChkDate();
        final String chkTime = getChkTime();

        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                try {
                    String url = "TprServlet?hospitalid=" + hospitalId +
                            "&pid=" + mPid +
                            "&bededt=" + mBededt +
                            "&mode=" + mode +
                            "&chkdate=" + chkDate +
                            "&chktime=" + chkTime;
                    mXmlDm = getXml(url);
                } catch (Exception e) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetDmOneRow();
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

    private void afterGetDmOneRow() {
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            // 값 초기화
            ((EditText) findViewById(R.id.nvalueEditText)).setText("");

            ResultSetHelper rs = new ResultSetHelper(mXmlDm, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                Toast.makeText(DmEnter.this, R.string.no_data_message, Toast.LENGTH_LONG).show();
            } else {
                ((EditText) findViewById(R.id.nvalueEditText)).setText(rs.getString(0, "n_value"));
            }
        } catch (Exception ex) {
            Toast.makeText(DmEnter.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void save() {
        final String hospitalId = getHospitalId();
        final String userId = getUserId();
        final String mode = "dms";
        final String chkDate = getChkDate();
        final String chkTime = getChkTime();
        final String nvalue = ((EditText) findViewById(R.id.nvalueEditText)).getText().toString();

        if (nvalue.equals("")) {
            showSimpleDialog(getString(R.string.no_data_input_message));
            return;
        }

        //showSimpleDialog("chkDate=" + chkDate + ", chkTime=" + chkTime);
        //if(1==1)return;

        mDialog = ProgressDialog.show(this, "", getString(R.string.process_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                //String hospitalId=getHospitalId();
                //String userId=getUserId();

                //String mode = "dms";
                //String chkDate = getChkDate();
                //String chkTime = getChkTime();
                //String dm = ((EditText)findViewById(R.id.dmEditText)).getText().toString();

                String url = "TprServlet?hospitalid=" + hospitalId +
                        "&pid=" + mPid +
                        "&bededt=" + mBededt +
                        "&mode=" + mode +
                        "&chkdate=" + chkDate +
                        "&chktime=" + chkTime +
                        "&nvalue=" + nvalue +
                        "&userid=" + userId;
                mXmlDmEnter = getXml(url);
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
        String xml = mXmlDmEnter.substring(0, 2); // 뒤에 엔터문자가 따라온다. 엔터를 분리하여야 정확히 비교된다.
        if (xml.compareTo("OK") == 0) {
            setResult(RESULT_OK, null);
            finish();
        } else {
            showSimpleDialog(mXmlDmEnter);
        }
    }

}
