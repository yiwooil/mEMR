package com.metrosoft.smart.emr.emrdroid.gt101.z_practice;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ViewFlipper;

public class MyViewFlipper extends MyActivity implements View.OnTouchListener, CompoundButton.OnCheckedChangeListener {
	CheckBox mCheckBox;
	ViewFlipper mFlipper;

	// 터치 이벤트 발생 지점의 x좌표 저장
	float xAtDown;
	float xAtUp;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, R.layout.my_view_flipper, "< " + getString(R.string.home));

		mCheckBox = (CheckBox)findViewById(R.id.chkAuto);
		mCheckBox.setOnCheckedChangeListener(this);

		mFlipper = (ViewFlipper)findViewById(R.id.viewFlipper);
		mFlipper.setOnTouchListener(this);

		// ViewFlipper에 동적으로 child view 추가
		TextView tv = new TextView(this);
		tv.setText("View 5\nDynamically added");
		tv.setTextColor(Color.CYAN);
		mFlipper.addView(tv);
	}

	// View.OnTouchListener의 abstract method
	// flipper 터지 이벤트 리스너
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// 터치 이벤트가 일어난 뷰가 ViewFlipper가 아니면 return
		if(v != mFlipper) return false;

		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			xAtDown = event.getX(); // 터치 시작지점 x좌표 저장
		}
		else if(event.getAction() == MotionEvent.ACTION_UP){
			xAtUp = event.getX(); 	// 터치 끝난지점 x좌표 저장

			if( xAtUp < xAtDown ) {
				// 왼쪽 방향 에니메이션 지정
				mFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
				mFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_out));

				// 다음 view 보여줌
				mFlipper.showNext();
			}
			else if (xAtUp > xAtDown){
				// 오른쪽 방향 에니메이션 지정
				mFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_in));
				mFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_right_out));
				// 전 view 보여줌
				mFlipper.showPrevious();
			}
		}
		return true;
	}

	// CompoundButton.OnCheckedChangeListener의 abstract method
	// 책크박스 책크 이벤트 리스너
	@Override
	public void onCheckedChanged(CompoundButton view, boolean isChecked) {

		if(isChecked == true) {
			// 왼쪽 에니메이션 설정
			mFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_in));
			mFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.push_left_out));

			// 자동 Flipping 시작 (간격 3초)
			mFlipper.setFlipInterval(3000);
			mFlipper.startFlipping();
		}
		else
		{
			// 자동 Flipping 해지
			mFlipper.stopFlipping();
		}
	}
}