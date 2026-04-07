package com.metrosoft.smart.emr.emrdroid.gt101.z_practice;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MyTab3Sub01 extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* First Tab Content */
		TextView textView = new TextView(this);
		textView.setText("MyTab3Sub01");
		setContentView(textView);

	}
}
