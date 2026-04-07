package com.metrosoft.smart.emr.emrdroid.gt101.z_practice;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;

import android.os.Bundle;
import android.content.Intent;

import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import android.view.View;

public class QrScan extends MyActivity {
//	Utility mUtil = new Utility();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.qr_scan);

		Button qrScanButton = (Button)findViewById(R.id.qrScanButton);
		qrScanButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent("com.google.zxing.client.android.SCAN");
				intent.setPackage("com.google.zxing.client.android");
				intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
				startActivityForResult(intent, 0);
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
				String format = intent.getStringExtra("SCAN_RESULT_FORMAT"); //형식
				String contents = intent.getStringExtra("SCAN_RESULT"); //URL

				Log.d("EmrDroid", "format="+format+",contents="+contents);

				showSimpleDialog("format="+format+",contents="+contents);

//				    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(contents)); //기본브라우저 링크
//				    startActivity(i);
			} else if (resultCode == RESULT_CANCELED) {
				Toast.makeText(QrScan.this, getString(R.string.cancel_message), Toast.LENGTH_SHORT).show();
			}
		}
	}

}
