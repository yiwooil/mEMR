package com.metrosoft.smart.emr.emrdroid.gt101.z_notused;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;

import android.app.ProgressDialog;
import android.os.Bundle;

import android.view.KeyEvent;
import android.view.View;

import android.webkit.WebView;
import android.webkit.WebViewClient;

public class Notice extends MyActivity {
	private String mXml;

	WebView webView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, R.layout.notice, "닫기");

		webView = (WebView)findViewById(R.id.notice_view);
		webView.setWebViewClient(new NoticetWebViewClient());

		if (savedInstanceState==null) {
			getNotice();
		}
		else {
			mXml=savedInstanceState.getString("xml");
			displayNotice();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("xml", mXml);
	}

	@Override
	public void onClickQueryButton(View v) {
		getNotice();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
			webView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);

	}
	private void getNotice() {
		mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
		new Thread(new Runnable() {
			public void run() {
				String hospitalId=getHospitalId();
				String userId=getUserId();
				mXml="";
				String url = "NoticeServlet?hospitalid=" + hospitalId + "&userid=" + userId + "&mode=0";
				mXml = getXml(url);
				mHandler.post(new Runnable() {
					public void run() {
						displayNotice();
						mDialog.dismiss();
					}
				});
			}
		}).start();;
	}

	private void displayNotice() {
		// 오류발생
		if(super.getXmlError()==true) {
			super.showToastText(super.getXmlErrorMessage());
			return;
		}
		webView.loadData(mXml, "text/html", "utf-8");
//		Toast.makeText(this, mXml, Toast.LENGTH_LONG).show();
	}

	private class NoticetWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}
	}
}
