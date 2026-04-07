package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

public class TimePickDialog extends Activity implements OnClickListener {

    private Integer mHour;
    private Integer mMinute;

    private Button mOkButton;
    private Button mCancelButton;

    private Button mHourPlusButton;
    private Button mHourMinusButton;
    private Button mMinutePlusButton;
    private Button mMinuteMinusButton;

    private EditText mHourText;
    private EditText mMinuteText;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        mHour = intent.getIntExtra("hour", 0);
        mMinute = intent.getIntExtra("minute", 0);

        setContentView(R.layout.time_pick_dialog);

        initControls();

        setHourText(mHour);
        setMinuteText(mMinute);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        if (v.getId() == R.id.ok_button) {
            try {
                String sHour = mHourText.getText().toString();
                int iHour = Integer.parseInt(sHour);
                if (iHour < 0 || iHour > 23) {
                    // 오류
                    final int duration = 2000;
                    Toast t = Toast.makeText(this, "시간을 확인하세요.", Toast.LENGTH_SHORT);
                    t.setDuration(Toast.LENGTH_LONG);
                    t.show();
                    return;
                }
                mHour = iHour;
            } catch (Exception ex) {
            }
            try {
                String sMinute = mMinuteText.getText().toString();
                int iMinute = Integer.parseInt(sMinute);
                if (iMinute < 0 || iMinute > 59) {
                    // 오류
                    final int duration = 2000;
                    Toast t = Toast.makeText(this, "분을 확인하세요.", Toast.LENGTH_SHORT);
                    t.setDuration(Toast.LENGTH_LONG);
                    t.show();
                    return;
                }
                mMinute = iMinute;
            } catch (Exception ex) {
            }

            Intent intent = getIntent(); // 이 액티비티를 시작하게 한 인텐트를 호출
            intent.putExtra("hour", mHour);
            intent.putExtra("minute", mMinute);
            setResult(RESULT_OK, intent); // 추가 정보를 넣은 후 다시 인텐트를 반환합니다.
            finish(); // 액티비티 종료

        } else if (v.getId() == R.id.cancel_button) {
            Intent intent = getIntent(); // 이 액티비티를 시작하게 한 인텐트를 호출
            setResult(RESULT_CANCELED, intent);
            finish(); // 액티비티 종료
        } else if (v.getId() == R.id.hour_plus_button) {
            try {
                String sHour = mHourText.getText().toString();
                Integer iHour = Integer.parseInt(sHour);
                iHour++;
                if (iHour > 23) iHour = 0;
                setHourText(iHour);
            } catch (Exception ex) {
                final int duration = 2000;
                Toast t = Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT);
                t.setDuration(Toast.LENGTH_LONG);
                t.show();
            }
        } else if (v.getId() == R.id.hour_minus_button) {
            try {
                String sHour = mHourText.getText().toString();
                Integer iHour = Integer.parseInt(sHour);
                iHour--;
                if (iHour < 0) iHour = 23;
                setHourText(iHour);
            } catch (Exception ex) {
                final int duration = 2000;
                Toast t = Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT);
                t.setDuration(Toast.LENGTH_LONG);
                t.show();
            }
        } else if (v.getId() == R.id.minute_plus_button) {
            try {
                String sMinute = mMinuteText.getText().toString();
                Integer iMinute = Integer.parseInt(sMinute);
                iMinute++;
                if (iMinute > 59) iMinute = 0;
                setMinuteText(iMinute);
            } catch (Exception ex) {
                final int duration = 2000;
                Toast t = Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT);
                t.setDuration(Toast.LENGTH_LONG);
                t.show();
            }
        } else if (v.getId() == R.id.minute_minus_button) {
            try {
                String sMinute = mMinuteText.getText().toString();
                Integer iMinute = Integer.parseInt(sMinute);
                iMinute--;
                if (iMinute < 0) iMinute = 59;
                setMinuteText(iMinute);
            } catch (Exception ex) {
                final int duration = 2000;
                Toast t = Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT);
                t.setDuration(Toast.LENGTH_LONG);
                t.show();
            }
        }
    }

    private void initControls() {
        mOkButton = (Button) findViewById(R.id.ok_button);
        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mHourPlusButton = (Button) findViewById(R.id.hour_plus_button);
        mHourMinusButton = (Button) findViewById(R.id.hour_minus_button);
        mMinutePlusButton = (Button) findViewById(R.id.minute_plus_button);
        mMinuteMinusButton = (Button) findViewById(R.id.minute_minus_button);

        mOkButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);

        mHourPlusButton.setOnClickListener(this);
        mHourMinusButton.setOnClickListener(this);
        mMinutePlusButton.setOnClickListener(this);
        mMinuteMinusButton.setOnClickListener(this);

        mHourText = (EditText) findViewById(R.id.hour_text);
        mMinuteText = (EditText) findViewById(R.id.minute_text);
    }

    private void setHourText(Integer hour) {
        String sHour = hour.toString();
        if (sHour.length() == 1) sHour = "0" + sHour;
        mHourText.setText(sHour);
    }

    private void setMinuteText(Integer minute) {
        String sMinute = minute.toString();
        if (sMinute.length() == 1) sMinute = "0" + sMinute;
        mMinuteText.setText(sMinute);
    }

}
