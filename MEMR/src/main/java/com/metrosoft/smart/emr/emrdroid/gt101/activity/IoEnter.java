package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.R.style;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.Calendar;

public class IoEnter extends MyActivity {
    private int REQUEST_TIME = 1;

    private String mXmlIoEnter, mXmlIo;

    private String mPid;
    private String mBededt;

    private int mChkDateYear, mChkDateMonth, mChkDateDay;
    private int mChkDateHour, mChkDateMinute;

    private EditText mOralC;
    private EditText mOralV;
    private EditText mPateC;
    private EditText mPateV;
    private EditText mBloodC;
    private EditText mBloodV;
    private EditText mUrine;
    private EditText mDrsu;
    private EditText mSvoC;
    private EditText mStool;
    private EditText mVomit;
    private EditText mOthers;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.io_enter);

        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");


        mOralC = (EditText) findViewById(R.id.oralcEditText);
        mOralV = (EditText) findViewById(R.id.oralvEditText);
        mPateC = (EditText) findViewById(R.id.patecEditText);
        mPateV = (EditText) findViewById(R.id.patevEditText);
        mBloodC = (EditText) findViewById(R.id.bloodcEditText);
        mBloodV = (EditText) findViewById(R.id.bloodvEditText);
        mUrine = (EditText) findViewById(R.id.urineEditText);
        mDrsu = (EditText) findViewById(R.id.drsuEditText);
        mSvoC = (EditText) findViewById(R.id.svocEditText);
        mStool = (EditText) findViewById(R.id.stoolEditText);
        mVomit = (EditText) findViewById(R.id.vomitEditText);
        mOthers = (EditText) findViewById(R.id.othersEditText);

        registerForContextMenu(mOralC);
        registerForContextMenu(mPateC);
        registerForContextMenu(mBloodC);
        registerForContextMenu(mSvoC);

        init();
        setListener();

        // 등록된 값 조회
        getIoOneRow();
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
        ((Button) findViewById(R.id.chkDate)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                DialogDatePicker();
            }
        });
        ((Button) findViewById(R.id.chkTime)).setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                DialogTimePicker();
            }
        });
        final Button okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // 저장
                save();
            }
        });
        final Button cancelButton = (Button) findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                setResult(RESULT_CANCELED, null);
                finish();
            }
        });
        final Button dButton = (Button) findViewById(R.id.dButton);
        dButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mChkDateHour = 10;
                mChkDateMinute = 0;
                displayChkTime();
                getIoOneRow();
            }
        });
        final Button eButton = (Button) findViewById(R.id.eButton);
        eButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mChkDateHour = 18;
                mChkDateMinute = 0;
                displayChkTime();
                getIoOneRow();
            }
        });
        final Button nButton = (Button) findViewById(R.id.nButton);
        nButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mChkDateHour = 22;
                mChkDateMinute = 0;
                displayChkTime();
                getIoOneRow();
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
                getIoOneRow();
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
	    		getIoOneRow();
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
                getIoOneRow();
            }
        }
    }

    private String getChkDate() {
        return getDateString(mChkDateYear, mChkDateMonth, mChkDateDay);
    }

    private String getChkTime() {
        return getTimeString(mChkDateHour, mChkDateMinute);
    }

    private void getIoOneRow() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String mode = "ioq1";
                String chkDate = getChkDate();
                String chkTime = getChkTime();
                try {
                    String url = "TprServlet?hospitalid=" + hospitalId +
                            "&pid=" + mPid +
                            "&bededt=" + mBededt +
                            "&mode=" + mode +
                            "&chkdate=" + chkDate +
                            "&chktime=" + chkTime;
                    mXmlIo = getXml(url);
                } catch (Exception e) {
                    return;
                }
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetIoOneRow();
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

    private void afterGetIoOneRow() {
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            // 값 초기화
            mOralC.setText("");
            mOralV.setText("");
            mPateC.setText("");
            mPateV.setText("");
            mBloodC.setText("");
            mBloodV.setText("");
            mUrine.setText("");
            mDrsu.setText("");
            mSvoC.setText("");
            mStool.setText("");
            mVomit.setText("");
            mOthers.setText("");

            ResultSetHelper rs = new ResultSetHelper(mXmlIo, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                Toast.makeText(IoEnter.this, R.string.no_data_message, Toast.LENGTH_LONG).show();
            } else {
                String oralc = rs.getString(0, "oral_c");
                String oralv = rs.getString(0, "oral_v");
                String patec = rs.getString(0, "pate_c");
                String patev = rs.getString(0, "pate_v");
                String bloodc = rs.getString(0, "blood_c");
                String bloodv = rs.getString(0, "blood_v");
                String urine = rs.getString(0, "urine");
                String drsu = rs.getString(0, "dr_su");
                String svoc = rs.getString(0, "s_v_o_c");
                String stool = rs.getString(0, "stool");
                String vomit = rs.getString(0, "vomit");
                String others = rs.getString(0, "others");

                if ("0".equals(oralv)) oralv = "";
                if ("0".equals(patev)) patev = "";
                if ("0".equals(bloodv)) bloodv = "";
                if ("0".equals(urine)) urine = "";
                if ("0".equals(drsu)) drsu = "";
                if ("0".equals(stool)) stool = "";
                if ("0".equals(vomit)) vomit = "";
                if ("0".equals(others)) others = "";

                mOralC.setText(oralc);
                mOralV.setText(oralv);
                mPateC.setText(patec);
                mPateV.setText(patev);
                mBloodC.setText(bloodc);
                mBloodV.setText(bloodv);
                mUrine.setText(urine);
                mDrsu.setText(drsu);
                mSvoC.setText(svoc);
                mStool.setText(stool);
                mVomit.setText(vomit);
                mOthers.setText(others);
            }
        } catch (Exception ex) {
            Toast.makeText(IoEnter.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void save() {
        final String oralc = mOralC.getText().toString();
        final String oralv = mOralV.getText().toString();
        final String patec = mPateC.getText().toString();
        final String patev = mPateV.getText().toString();
        final String bloodc = mBloodC.getText().toString();
        final String bloodv = mBloodV.getText().toString();
        final String urine = mUrine.getText().toString();
        final String drsu = mDrsu.getText().toString();
        final String svoc = mSvoC.getText().toString();
        final String stool = mStool.getText().toString();
        final String vomit = mVomit.getText().toString();
        final String others = mOthers.getText().toString();

        if (oralv.equals("") && patev.equals("") && bloodv.equals("") && urine.equals("") && drsu.equals("") && svoc.equals("") && stool.equals("") && vomit.equals("") && others.equals("")) {
            showSimpleDialog(getString(R.string.no_data_input_message));
            return;
        }

        mDialog = ProgressDialog.show(this, "", getString(R.string.process_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();

                String mode = "ios";
                String chkDate = getChkDate();
                String chkTime = getChkTime();

                String url = "TprServlet?hospitalid=" + hospitalId +
                        "&pid=" + mPid +
                        "&bededt=" + mBededt +
                        "&mode=" + mode +
                        "&chkdate=" + chkDate +
                        "&chktime=" + chkTime +
                        "&oralc=" + getHangul(oralc) +
                        "&oralv=" + oralv +
                        "&patec=" + getHangul(patec) +
                        "&patev=" + patev +
                        "&bloodc=" + getHangul(bloodc) +
                        "&bloodv=" + bloodv +
                        "&urine=" + urine +
                        "&drsu=" + drsu +
                        "&svoc=" + getHangul(svoc) +
                        "&stool=" + stool +
                        "&vomit=" + vomit +
                        "&others=" + others +
                        "&userid=" + userId;
                mXmlIoEnter = getXml(url);
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
        String xml = mXmlIoEnter.substring(0, 2); // 뒤에 엔터문자가 따라온다. 엔터를 분리하여야 정확히 비교된다.
        if (xml.compareTo("OK") == 0) {
            setResult(RESULT_OK, null);
            finish();
        } else {
            showSimpleDialog(mXmlIoEnter);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, view, menuInfo);

        if (view.getId() == R.id.oralcEditText) {
            menu.add(0, 1, 0, "밥");
            menu.add(0, 2, 0, "죽");
            menu.add(0, 3, 0, "미음");
            menu.add(0, 4, 0, "국");
            menu.add(0, 5, 0, "물");
            menu.add(0, 6, 0, "우유");
            menu.add(0, 7, 0, "기타");
        } else if (view.getId() == R.id.bloodcEditText) {
            menu.add(0, 11, 0, "W/B");
            menu.add(0, 12, 0, "PRC");
            menu.add(0, 13, 0, "FFP");
            menu.add(0, 14, 0, "PLT");
        } else if (view.getId() == R.id.svocEditText) {
            menu.add(0, 21, 0, "N");
            menu.add(0, 22, 0, "D");
            menu.add(0, 23, 0, "T");
            menu.add(0, 24, 0, "M");
            menu.add(0, 25, 0, "L");
            menu.add(0, 26, 0, "E");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId >= 1 && itemId <= 10) {
            mOralC.setText(item.getTitle());
        } else if (itemId >= 11 && itemId <= 20) {
            mBloodC.setText(item.getTitle());
        } else if (itemId >= 21 && itemId <= 30) {
            mSvoC.setText(item.getTitle());
        }
        return true;
    }
}
