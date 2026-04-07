package com.metrosoft.smart.emr.emrdroid.gt101.z_practice;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AnalogClock;
import android.widget.Button;
import android.widget.TabHost;

public class MyDynamicTab extends MyActivity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, R.layout.my_dynamic_tab, "< " + getString(R.string.home));

		final TabHost tabHost = (TabHost)findViewById(R.id.tabHost);

		tabHost.setup();

		// 'Tab 추가 버튼'이 달린 첫 Tab
		TabHost.TabSpec spec = tabHost.newTabSpec("Tab 00");
		spec.setIndicator("First Tab");
		spec.setContent(R.id.btnAddTab);
		tabHost.addTab(spec);

		tabHost.setCurrentTab(0);

		Button btnAddTab = (Button)findViewById(R.id.btnAddTab);

		// 버튼 클릭 이밴트 리스너
		btnAddTab.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				// Tab Builder 생성
				TabHost.TabSpec spec = tabHost.newTabSpec("New Tab");

				// setContent (analog 시계 생성하는 TabContentFactory 지정)
				spec.setContent(new TabHost.TabContentFactory() {
					@Override
					// TabContentFactory 생성시 호출되는 CallBack
					public View createTabContent(String tag) {
						return (new AnalogClock(MyDynamicTab.this));
					}
				});

				// 추가되는 Tab의 Text를 Clock으로 표시
				spec.setIndicator("Clock");

				// tabHost에 새로운 tab 더함
				tabHost.addTab(spec);
			}
		});
	}
}
