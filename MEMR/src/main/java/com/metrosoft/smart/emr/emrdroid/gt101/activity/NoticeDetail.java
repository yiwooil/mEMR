package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

public class NoticeDetail extends MyActivity {
    private WebView mWebView;

    private String mApdt;
    private String mSeq;

    private String mXml;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.notice_detail, getString(R.string.notice));
        super.setQuickMenuOff();

        mWebView = (WebView) findViewById(R.id.notice_detail_view);
        mWebView.setWebViewClient(new NoticeDetailWebViewClient());

        // 파라메터 셋팅
        Intent intent = getIntent();
        mApdt = intent.getStringExtra("apdt");
        mSeq = intent.getStringExtra("seq");

        if (savedInstanceState == null) {
            getNoticeDetail();
        } else {
            mXml = savedInstanceState.getString("xml");
            afterGetNoticeDetail();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("xml", mXml);
    }

    @Override
    public void onClickQueryButton(View v) {
        getNoticeDetail();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
            mWebView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);

    }

    private void getNoticeDetail() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                mXml = "";
                String url = "NoticeServlet?hospitalid=" + hospitalId +
                        "&userid=" + userId +
                        "&mode=1" +
                        "&apdt=" + mApdt +
                        "&seq=" + mSeq;
                mXml = getXml(url);
                mHandler.post(new Runnable() {
                    public void run() {
                        afterGetNoticeDetail();
                        mDialog.dismiss();
                    }
                });
            }
        }).start();
        ;
    }

    private void afterGetNoticeDetail() {
        // 오류발생
        if (super.getXmlError() == true) {
            super.showToastText(super.getXmlErrorMessage());
            return;
        }
        //mWebView.loadData(mXml, "text/html", "utf-8"); // <-- 한글깨짐
        mWebView.loadDataWithBaseURL(null, mXml, "text/html", "utf-8", null); // <-- 제대로 나옴.
//		Toast.makeText(this, mXml, Toast.LENGTH_LONG).show();
    }

    private class NoticeDetailWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
