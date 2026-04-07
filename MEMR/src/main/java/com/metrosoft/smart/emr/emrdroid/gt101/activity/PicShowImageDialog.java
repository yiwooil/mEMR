package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

public class PicShowImageDialog extends Activity implements OnClickListener {

    protected ProgressDialog mDialog = null;

    private Button mBackButton;
    private Button mRotateButton0, mRotateButton1, mRotateButton2, mRotateButton3;
    private int mRotate;
    private WebView mWebView;

    private String mUrl;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        if (savedInstanceState == null) {
            mUrl = intent.getStringExtra("url");
        } else {
            mUrl = savedInstanceState.getString("mUrl");
        }

        setContentView(R.layout.pic_show_image);

        mBackButton = (Button) findViewById(R.id.back_button);
        mRotateButton0 = (Button) findViewById(R.id.rotate_button_0);
        mRotateButton1 = (Button) findViewById(R.id.rotate_button_1);
        mRotateButton2 = (Button) findViewById(R.id.rotate_button_2);
        mRotateButton3 = (Button) findViewById(R.id.rotate_button_3);

        mWebView = (WebView) findViewById(R.id.pic_show_web);
        mWebView.setWebViewClient(new MyWebViewClient());


        mBackButton.setOnClickListener(this);
        mRotateButton0.setOnClickListener(this);
        mRotateButton1.setOnClickListener(this);
        mRotateButton2.setOnClickListener(this);
        mRotateButton3.setOnClickListener(this);

        mRotate = 0;
        loadImage(mRotate);
    }

    private void loadImage(int rotate) {
        // rotate 0.0도 회전 1.90도 회전 2.180도 회전 3.270도 회전
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);

        String url = "<img width='100%' height='100%' src=\"" + mUrl + "\">";
        url = "";
        url += "<head>";
        url += "<style type='text/css'>";
        url += ".rotate90{";
        url += "    -webkit-transform: rotate(90deg);";
        url += "    -moz-transform: rotate(90deg);";
        url += "    -o-transform: rotate(90deg);";
        url += "    -ms-transform: rotate(90deg);";
        url += "    transform: rotate(90deg);";
        url += "}";
        url += ".rotate180{";
        url += "    -webkit-transform: rotate(180deg);";
        url += "    -moz-transform: rotate(180deg);";
        url += "    -o-transform: rotate(180deg);";
        url += "    -ms-transform: rotate(180deg);";
        url += "    transform: rotate(180deg);";
        url += "}";
        url += ".rotate270{";
        url += "    -webkit-transform: rotate(270deg);";
        url += "    -moz-transform: rotate(270deg);";
        url += "    -o-transform: rotate(270deg);";
        url += "    -ms-transform: rotate(270deg);";
        url += "    transform: rotate(270deg);";
        url += "}";
        url += "</style>";
        url += "</head>";
        url += "<body>";
        url += "<img width='100%' height='100%' src=\"" + mUrl + "\"";
        if (rotate == 1) {
            url += "class=\"rotate90\"";
        } else if (rotate == 2) {
            url += "class=\"rotate180\"";
        } else if (rotate == 3) {
            url += "class=\"rotate270\"";
        }
        url += ">";
        url += "</body>";
        url += "</html>";

        mWebView.loadDataWithBaseURL(null, url, "text/html", "utf-8", null);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.setInitialScale(BIND_AUTO_CREATE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("mUrl", mUrl);
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        if (view.getId() == R.id.back_button) {
            finish();
        } else if (view.getId() == R.id.rotate_button_0) {
            loadImage(0);
        } else if (view.getId() == R.id.rotate_button_1) {
            loadImage(1);
        } else if (view.getId() == R.id.rotate_button_2) {
            loadImage(2);
        } else if (view.getId() == R.id.rotate_button_3) {
            loadImage(3);
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            //mIsOnPageFinished=true;
            if (mDialog != null) {
                mDialog.dismiss();
                mDialog = null;
            }
        }

    }


}
