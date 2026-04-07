package com.metrosoft.smart.emr.emrdroid.gt101.z_notused;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

public class CertificatePaper extends MyActivity {
	private String mXml;
	private Handler handler = new Handler();
	
	WebView mWebView;
	String mMst3cd;
	String mCdnm;
	String mPid;
	String mBededt;

	@Override 
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	super.onCreate(savedInstanceState, R.layout.certificate_paper, "< " + getString(R.string.certificate_list));
        
        Intent intent = getIntent();
        mMst3cd = intent.getStringExtra("mst3cd");
        mCdnm = intent.getStringExtra("cdnm");
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        
        ((TextView)findViewById(R.id.certificate_title)).setText(mCdnm);
        mWebView = (WebView)findViewById(R.id.certificate_view);
        //webView.getSettings().setLoadWithOverviewMode(true);
        //webView.getSettings().setUseWideViewPort(true);
        //webView.getSettings().setJavaScriptEnabled(true);  // 웹뷰에서 자바스크립트실행가능.
        mWebView.setWebViewClient(new CertificatePaperWebViewClient());
        
		if (savedInstanceState==null) {
			getCertificatePaper();
		}
		else {
			mXml=savedInstanceState.getString("xml");
			afterGetCertificatePaper();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("xml", mXml);
	}
	
	@Override
	public void onClickQueryButton(View v) {
		getCertificatePaper();		
	}

	private class CertificatePaperWebViewClient extends WebViewClient { 
        @Override 
        public boolean shouldOverrideUrlLoading(WebView view, String url) { 
            view.loadUrl(url); 
            return true; 
        } 
    }
	
	private void getCertificatePaper() {
		mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
        	public void run() {
            	String hospitalId=getHospitalId();
            	String userId=getUserId();
        		String url = "CertificatePaperServlet?mode=1&hospitalid=" + hospitalId + "&userid=" + userId + "&mst3cd=" + mMst3cd + "&pid=" + mPid + "&bededt=" + mBededt;
        		mXml = getXml(url);
				// ����
    			handler.post(new Runnable() {
    				public void run() {
						// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
						// 이를 방지함.
    					try {
	    					afterGetCertificatePaper();
	    					mDialog.dismiss();
    					}catch(Exception e) {
    						;
    					}
    				}
    			});
        	}
        }).start();;
	}
	
	private void afterGetCertificatePaper() {
		// 오류발생
		if(super.getXmlError()==true) {
			super.showToastText(super.getXmlErrorMessage());
			return;
		}
		//webView.loadData(xml, "text/html", "utf-8"); // <-- 한글 깨짐
		mWebView.loadDataWithBaseURL(null, mXml, "text/html", "utf-8", null); // <-- 제대로 나옴.
	}

}
