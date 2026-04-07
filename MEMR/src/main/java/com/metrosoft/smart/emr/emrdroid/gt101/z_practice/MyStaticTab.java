package com.metrosoft.smart.emr.emrdroid.gt101.z_practice;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TabHost;

public class MyStaticTab extends MyActivity implements View.OnTouchListener {
	TabHost tabHost;
	int maxIndex;

	// 터치 이벤트 발생 지점의 x좌표 저장
	float xAtDown;
	float xAtUp;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, R.layout.my_static_tab, "< " + getString(R.string.home));

		tabHost = (TabHost)findViewById(R.id.tabHost);

		// findViewById를 이용해 TabHost인스턴스를 얻은경우 꼭 호출 필요
		tabHost.setup();

		// Tab builder 객체
		TabHost.TabSpec spec;

		// Tab 01 세팅 & 등록
		spec = tabHost.newTabSpec("Tab 00"); // Tab Builder 객체 생성
		spec.setIndicator("Clock");			// Tab 제목
		spec.setContent(R.id.layout);		// Tab 내용
		tabHost.addTab(spec);				// Tab 등록

		// Tab 02 세팅 & 등록
		spec = tabHost.newTabSpec("Tab 01"); // Tab Builder 객체 생성
		spec.setIndicator("Button");		// Tab 제목
		spec.setContent(R.id.theButton);	// Tab 내용
		tabHost.addTab(spec);				// Tab 등록

		maxIndex=1;

		// 처음 등록된 Tab을 보여줌.
		tabHost.setCurrentTab(0);
		tabHost.setOnTouchListener(this);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub
		// 터치 이벤트가 일어난 뷰가 ViewFlipper가 아니면 return
		if(v != tabHost) return false;

		if(event.getAction() == MotionEvent.ACTION_DOWN) {
			xAtDown = event.getX(); // 터치 시작지점 x좌표 저장
		}
		else if(event.getAction() == MotionEvent.ACTION_UP){
			xAtUp = event.getX(); 	// 터치 끝난지점 x좌표 저장

			if( xAtUp < xAtDown ) {
				// 왼쪽 방향 에니메이션 지정
				int index=tabHost.getCurrentTab();
				index++;
				if(index>maxIndex) index=0;
				tabHost.setCurrentTab(index);
			}
			else if (xAtUp > xAtDown){
				// 오른쪽 방향 에니메이션 지정
				int index=tabHost.getCurrentTab();
				index--;
				if(index<0) index=maxIndex;
				tabHost.setCurrentTab(index);
			}
		}
		return true;
	}
}
