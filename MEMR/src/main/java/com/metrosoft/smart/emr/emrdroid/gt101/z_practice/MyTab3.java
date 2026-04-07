package com.metrosoft.smart.emr.emrdroid.gt101.z_practice;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

import android.app.ActivityGroup;
import android.app.LocalActivityManager;
import android.os.Bundle;
import android.widget.TabHost;
import android.content.Intent;

public class MyTab3 extends ActivityGroup {
    private TabHost mTabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_tab3);

        mTabHost = (TabHost)findViewById(R.id.tabHost);
        // findViewById를 이용해 TabHost인스턴스를 얻은경우 꼭 호출 필요
        LocalActivityManager lam = getLocalActivityManager();
        mTabHost.setup(lam);
        // Tab builder 객체
        TabHost.TabSpec spec;
        //
        spec = mTabHost.newTabSpec("MyTab3Sub01");			// Tab Builder 객체 생성
        spec.setIndicator("MyTab3Sub01");					// Tab 제목
        spec.setContent(new Intent(this,MyTab3Sub01.class));// Tab 내용
        mTabHost.addTab(spec);								// Tab 등록
        //
        spec = mTabHost.newTabSpec("MyTab3Sub02");			// Tab Builder 객체 생성
        spec.setIndicator("MyTab3Sub02");					// Tab 제목
        spec.setContent(new Intent(this,MyTab3Sub02.class));// Tab 내용
        mTabHost.addTab(spec);								// Tab 등록
        // 처음 등록된 Tab을 보여줌.
        mTabHost.setCurrentTab(0);
    }
}
