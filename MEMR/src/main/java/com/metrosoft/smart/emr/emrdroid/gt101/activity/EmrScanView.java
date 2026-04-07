/***
 * Excerpted from "Hello, Android! 3e",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/eband3 for more book information.
 ***/
package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;

import org.json.JSONException;

public class EmrScanView extends MyActivity implements OnCheckedChangeListener {
    private String mXmlPatientInfo;
    private String mPid;
    private String mBededt;
    private String mBdiv;
    private String mExdt;
    private String mSeq;
    private String mRptcd;
    private String mPath, mPath2; // 2013.09.09 WOOIL - 자인컴은 PATH2가 있음.
    private String mFullUrl, mFullUrl2; // 2013.09.09 WOOIL - 자인컴은 2가 있음.
    private String mFrom; // 2014.03.10 - 동의서열람에서 왔으면 녹음파일을 듣는기능을 넣자.
    private String mSubPageList; // 2022.03.22 WOOIL - 서브페이지 리스트
    private String mSubPageNo; // 2022.03.22 WOOIL - 서브페이지 여부
    private int mPageCount; // 2022.03.22 - 동의서 페이지 수
    private String mp4Xml;
    private String picXml;

    private String[] mPathPage = new String[15]; // 2022.03.22 WOOIL - 1~10 페이지의 파일 정보
    private String[] mUrlPage = new String[15]; // 2022.03.23 WOOIL - 1~10 페이지 정보
    private MediaPlayer player;

    private WebView mWebView;
    private TextView mPatientInfoTextView;

    // 2022.03.02 동의서가 여러장인 경우 처리 10장 까지 가능함.
    private RadioGroup mPageGroup;
    private RadioButton mRadioPage1;
    private RadioButton mRadioPage2;
    private RadioButton mRadioPage3;
    private RadioButton mRadioPage4;
    private RadioButton mRadioPage5;
    private RadioButton mRadioPage6;
    private RadioButton mRadioPage7;
    private RadioButton mRadioPage8;
    private RadioButton mRadioPage9;
    private RadioButton mRadioPage10;
    private RadioButton mRadioPage11;
    private RadioButton mRadioPage12;
    private RadioButton mRadioPage13;
    private RadioButton mRadioPage14;
    private RadioButton mRadioPage15;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.emr_scan_view, getString(R.string.emr_scan));

        mPatientInfoTextView = (TextView) findViewById(R.id.patientInfoTextView);
        mWebView = (WebView) findViewById(R.id.webView);
        mWebView.setWebViewClient(new MyWebViewClient());

        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mBdiv = intent.getStringExtra("bdiv");
        mExdt = intent.getStringExtra("exdt");
        mSeq = intent.getStringExtra("seq");
        mRptcd = intent.getStringExtra("rptcd");
        mPath = intent.getStringExtra("path");
        mPath2 = intent.getStringExtra("path2");
        mXmlPatientInfo = intent.getStringExtra("patientinfo");
        mFrom = intent.getStringExtra("from");
        mSubPageList = intent.getStringExtra("sub_page_list"); // 2022.03.22 WOOIL - 서브페이지 리스트
        mSubPageNo = intent.getStringExtra("sub_page_no"); // 2022.03.22 WOOIL - 서브페이지 여부

        if (mFrom == null) mFrom = ""; // 2024.09.09 WOOIL - 오류방지용
        if (mSubPageList == null) mSubPageList = ""; // 2022.03.22 WOOIL - 오류방지용
        if (mSubPageNo == null) mSubPageNo = ""; // 2022.03.22 WOOIL - 오류방지용

        mPageCount = 1;
        if (!"".equals(mSubPageList)) {
            // 여러 페이지로 이루어진 동의서이면
            String pageList[] = mSubPageList.split(";");
            mPageCount = pageList.length + 1;
        }


        if ("signed".equalsIgnoreCase(mFrom)) {
            setButton1(true, "녹음듣기", BUTTON_TYPE_NONE);
            setButton2(true, "사진보기", BUTTON_TYPE_NONE);
        }

        // 2020.04.02 동의서가 여러장인 경우 처리 15장 까지 가능함.
        mPageGroup = (RadioGroup) findViewById(R.id.page_group);
        mPageGroup.setOnCheckedChangeListener(this);
        mRadioPage1 = (RadioButton) findViewById(R.id.page_1);
        mRadioPage2 = (RadioButton) findViewById(R.id.page_2);
        mRadioPage3 = (RadioButton) findViewById(R.id.page_3);
        mRadioPage4 = (RadioButton) findViewById(R.id.page_4);
        mRadioPage5 = (RadioButton) findViewById(R.id.page_5);
        mRadioPage6 = (RadioButton) findViewById(R.id.page_6);
        mRadioPage7 = (RadioButton) findViewById(R.id.page_7);
        mRadioPage8 = (RadioButton) findViewById(R.id.page_8);
        mRadioPage9 = (RadioButton) findViewById(R.id.page_9);
        mRadioPage10 = (RadioButton) findViewById(R.id.page_10);
        mRadioPage11 = (RadioButton) findViewById(R.id.page_11);
        mRadioPage12 = (RadioButton) findViewById(R.id.page_12);
        mRadioPage13 = (RadioButton) findViewById(R.id.page_13);
        mRadioPage14 = (RadioButton) findViewById(R.id.page_14);
        mRadioPage15 = (RadioButton) findViewById(R.id.page_15);
        mRadioPage1.setVisibility(View.GONE); // 1페이지도 안보이게
        mRadioPage2.setVisibility(View.GONE); // 일단 2페이지 안보이게
        mRadioPage3.setVisibility(View.GONE); // 일단 3페이지 안보이게
        mRadioPage4.setVisibility(View.GONE); // 일단 4페이지 안보이게
        mRadioPage5.setVisibility(View.GONE); // 일단 5페이지 안보이게
        mRadioPage6.setVisibility(View.GONE); // 일단 6페이지 안보이게
        mRadioPage7.setVisibility(View.GONE); // 일단 7페이지 안보이게
        mRadioPage8.setVisibility(View.GONE); // 일단 8페이지 안보이게
        mRadioPage9.setVisibility(View.GONE); // 일단 9페이지 안보이게
        mRadioPage10.setVisibility(View.GONE); // 일단 10페이지 안보이게
        mRadioPage11.setVisibility(View.GONE); // 일단 11페이지 안보이게
        mRadioPage12.setVisibility(View.GONE); // 일단 12페이지 안보이게
        mRadioPage13.setVisibility(View.GONE); // 일단 13페이지 안보이게
        mRadioPage14.setVisibility(View.GONE); // 일단 14페이지 안보이게
        mRadioPage15.setVisibility(View.GONE); // 일단 15페이지 안보이게

        if (mPageCount >= 2)
            mRadioPage1.setVisibility(View.VISIBLE); // 2페이지 이상인 경우만 페이지 번호를 보이게 한다.(1페이지도)
        if (mPageCount >= 2) mRadioPage2.setVisibility(View.VISIBLE);
        if (mPageCount >= 3) mRadioPage3.setVisibility(View.VISIBLE);
        if (mPageCount >= 4) mRadioPage4.setVisibility(View.VISIBLE);
        if (mPageCount >= 5) mRadioPage5.setVisibility(View.VISIBLE);
        if (mPageCount >= 6) mRadioPage6.setVisibility(View.VISIBLE);
        if (mPageCount >= 7) mRadioPage7.setVisibility(View.VISIBLE);
        if (mPageCount >= 8) mRadioPage8.setVisibility(View.VISIBLE);
        if (mPageCount >= 9) mRadioPage9.setVisibility(View.VISIBLE);
        if (mPageCount >= 10) mRadioPage10.setVisibility(View.VISIBLE);
        if (mPageCount >= 11) mRadioPage11.setVisibility(View.VISIBLE);
        if (mPageCount >= 12) mRadioPage12.setVisibility(View.VISIBLE);
        if (mPageCount >= 13) mRadioPage13.setVisibility(View.VISIBLE);
        if (mPageCount >= 14) mRadioPage14.setVisibility(View.VISIBLE);
        if (mPageCount >= 15) mRadioPage15.setVisibility(View.VISIBLE);

        // 2022.03.04 WOOIL - 기능을 막자~
        //                  - 앞쪽(처방조회)에 기능을 넣는다.
        //setButton3(true, "삭제", BUTTON_TYPE_NONE); // 2022.03.02 WOOIL - 삭제기능추가

        if (savedInstanceState == null) {
            getEmrScanView();
        } else {
            mFullUrl = savedInstanceState.getString("mFullUrl");
            mFullUrl2 = savedInstanceState.getString("mFullUrl2");
            mp4Xml = savedInstanceState.getString("mp4Xml");
            picXml = savedInstanceState.getString("picXml");
            // 화면에 다시 출력
            afterGetEmrScanView();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("mFullUrl", mFullUrl);
        outState.putString("mFullUrl2", mFullUrl2);
        outState.putString("mp4Xml", mp4Xml);
        outState.putString("picXml", picXml);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//		stopPlay();
    }

    @Override
    public void onClickQueryButton(View v) {
        getEmrScanView();
    }

    @Override
    public void onClickButton1(View v) {
        playMP4();
    }

    @Override
    public void onClickButton2(View v) {
        showPic();
    }

    //@Override
    //public void onClickButton3(View v){
    //	// 2022.03.02 WOOIL - 삭제기능추가
    //	deleteEmrScan();
    //}

    private void setRecButtonText(final String text) {
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                if ("signed".equalsIgnoreCase(mFrom)) {
                    setButton1(true, text, BUTTON_TYPE_NONE);
                }
            }
        });
    }

    private void setPicButtonText(final String text) {
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                if ("signed".equalsIgnoreCase(mFrom)) {
                    setButton2(true, text, BUTTON_TYPE_NONE);
                }

            }

        });
    }

    private void getEmrScanView() {
        mDialog = ProgressDialog.show(EmrScanView.this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String url = "";
				/*
				// 환자정보
				url = "InPatientInformationServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt;
				mXmlPatientInfo = getXml(url);
				*/
                String mode = "7";
                if ("signed".equalsIgnoreCase(mFrom)) {
                    mode = "";
                }
                // 기타서식
                String imagePath = mPath.replace("\\", "/");
                String imageUrl = "EmrScanServlet?hospitalid=" + hospitalId + "&path=" + imagePath + "&mode=" + mode;
                mFullUrl = getFullUrl(imageUrl);
                Log.d("EmrDroid-Servlet", mFullUrl);

                // 자인컴인 경우
                mFullUrl2 = "";
                if (!"".equals(mPath2)) {
                    String imagePath2 = mPath2.replace("\\", "/");
                    String yesNo = getXml("EmrScanServlet?hospitalid=" + hospitalId + "&path=" + imagePath2 + "&mode=2");
                    if ("yes".equalsIgnoreCase(yesNo)) {
                        String imageUrl2 = "EmrScanServlet?hospitalid=" + hospitalId + "&path=" + imagePath2;
                        mFullUrl2 = getFullUrl(imageUrl2);
                    }
                    Log.d("EmrDroid-Servlet", mFullUrl2);
                }

                // 여러페이지인 경우 페이지 정보를
                for (int i = 0; i < 15; i++) {
                    mPathPage[i] = "";
                    mUrlPage[i] = "";
                }
                mPathPage[0] = mPath; // 페이지 선택할때 사용하기 위해 담아놓는다.
                mUrlPage[0] = mFullUrl; // 페이지 선택할때 사용하기 위해 담아놓는다.
                if (!"".equals(mSubPageList)) {
                    // 여러 페이지로 이루어진 동의서이면 2페이지이상 파일정보를 가져온다.
                    String pageList[] = mSubPageList.split(";");
                    int i = 0;
                    for (String seq : pageList) {
                        i++;
                        url = "CertificatePaperServlet?hospitalid=" + hospitalId + "&userid=" + userId + "&pid=" + mPid + "&bdiv=" + mBdiv + "&exdt=" + mExdt + "&seq=" + seq + "&mode=17";
                        String xml = getXml(url);
                        ResultSetHelper rs;
                        try {
                            rs = new ResultSetHelper(xml, false);
                            if (rs.getReturnCode() > 0) {
                                if (rs.getRecordCount() > 0) mPathPage[i] = rs.getString(0, "path");
                            }
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }

                if ("signed".equalsIgnoreCase(mFrom)) {
                    // 녹음파일이 몇개가 있는지 찾아본다.
                    url = "ChartServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt + "&exdt=" + mExdt + "&seq=" + mSeq + "&mode=6";
                    mp4Xml = getXml(url);
                    // 사진파일이 몇개가 있는지 찾아본다.
                    url = "ChartServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt + "&exdt=" + mExdt + "&seq=" + mSeq + "&mode=7";
                    picXml = getXml(url);
                }

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetEmrScanView();
                            mDialog.dismiss();
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetEmrScanView() {
        String imageUrl = "";
        //
        mPatientInfoTextView.setText(mXmlPatientInfo);
        //
        if ("".equals(mFullUrl2)) {
            imageUrl = "<img width='100%' src=\"" + mFullUrl + "\">";
        } else {
            // 2번째 이미지가 있는 경우
            imageUrl = "<img width='100%' src=\"" + mFullUrl + "\">"
                    + "<br><br>"
                    + "<img width='100%' src=\"" + mFullUrl2 + "\">";
        }
        mWebView.loadDataWithBaseURL(null, imageUrl, "text/html", "utf-8", null);
        mWebView.getSettings().setSupportZoom(true);
        mWebView.getSettings().setBuiltInZoomControls(true);
        mWebView.getSettings().setUseWideViewPort(true);
        mWebView.setInitialScale(BIND_AUTO_CREATE);
        try {
            if ("signed".equalsIgnoreCase(mFrom)) {
                // 녹음파일갯수를 버튼에 표시한다.
                ResultSetHelper rs;
                rs = new ResultSetHelper(mp4Xml, false);
                if (rs.getReturnCode() < 0) {
                    setRecButtonText("녹음듣기(0)");
                } else {
                    int cnt = rs.getRecordCount();
                    if (cnt > 0) {
                        setRecButtonText("녹음듣기(" + cnt + ")");
                    } else {
                        setRecButtonText("녹음듣기(0)");
                    }
                }
                // 사진파일갯수를 버틍에 표시한다.
                rs = new ResultSetHelper(picXml, false);
                if (rs.getReturnCode() < 0) {
                    setPicButtonText("사진보기(0)");
                } else {
                    int cnt = rs.getRecordCount();
                    if (cnt > 0) {
                        setPicButtonText("사진보기(" + cnt + ")");
                    } else {
                        setPicButtonText("사진보기(0)");
                    }
                }
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        // 페이지 시작
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mDialog = ProgressDialog.show(EmrScanView.this, "", getString(R.string.query_wait_message), true);
            super.onPageStarted(view, url, favicon);
        }

        // 페이지 로딩중
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            try {
                mDialog.dismiss();
            } catch (Exception e) {
            }
        }

        // 페이지 종료
        @Override
        public void onPageFinished(WebView view, String url) {
            try {
                mDialog.dismiss();
            } catch (Exception e) {
            }
        }
    }

    private void playMP4() {
        Log.d("EmrDroid", "call MP4Play exdt=" + mExdt + ", seq=" + mSeq);

        Intent intent = new Intent(this, MP4PlayDialog.class);
        intent.putExtra("pid", mPid);
        intent.putExtra("bededt", mBededt);
        intent.putExtra("exdt", mExdt);
        intent.putExtra("seq", mSeq);
        startActivity(intent);
    }

    private void showPic() {
        Intent intent = new Intent(this, PicShowDialog.class);
        intent.putExtra("pid", mPid);
        intent.putExtra("bededt", mBededt);
        intent.putExtra("exdt", mExdt);
        intent.putExtra("seq", mSeq);
        startActivity(intent);

    }

    //private void deleteEmrScan(){
    //	AlertDialog.Builder dialog = new AlertDialog.Builder(this);
    //	dialog.setTitle("확인");
    //	dialog.setMessage("삭제하시겠습니까?");
    //	dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
    //		public void onClick(DialogInterface dialog, int which) {
    //			actionDeleteEmrScan();
    //		}
    //	});
    //	dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
    //		public void onClick(DialogInterface dialog, int which) {
    //			dialog.dismiss();
    //		}
    //	});
    //	dialog.setCancelable(false);
    //	dialog.show();
    //}

    //private void actionDeleteEmrScan(){
    //	mDialog = ProgressDialog.show(EmrScanView.this, "", getString(R.string.query_wait_message), true);
    //	new Thread(new Runnable() {
    //		public void run() {
    //			String hospitalId = getHospitalId();
    //			String userId = getUserId();
    //			String url = "";
    //			String mode="12";
    //
    //			// 녹음파일이 몇개가 있는지 찾아본다.
    //			url = "ChartServlet?hospitalid=" + hospitalId +
    //					          "&userid=" + userId +
    //		                      "&pid=" + mPid +
    //		                      "&bdiv=" + mBdiv +
    //		                      "&exdt=" + mExdt +
    //		                      "&seq=" + mSeq +
    //		                      "&rptcd=" + mRptcd +
    //		                      "&mode=" + mode ;
    //			final String xml = getXml(url);
    //
    //			mHandler.post(new Runnable() {
    //				public void run() {
    //					// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
    //					// 이를 방지함.
    //					try {
    //						afterActionDeleteEmrScan(xml);
    //						mDialog.dismiss();
    //					} catch (Exception e) {
    //						;
    //					}
    //				}
    //			});
    //		}
    //	}).start();
    //}

    //private void afterActionDeleteEmrScan(String xml){
    //	if(xml.equalsIgnoreCase("y")) super.onBackPressed(); // 성공. 이 창을 닫는다.
    //	else showSimpleDialog(xml);
    //}

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // TODO Auto-generated method stub
        if (checkedId == R.id.page_1) {
            setPageShow(0);
        } else if (checkedId == R.id.page_2) {
            setPageShow(1);
        } else if (checkedId == R.id.page_3) {
            setPageShow(2);
        } else if (checkedId == R.id.page_4) {
            setPageShow(3);
        } else if (checkedId == R.id.page_5) {
            setPageShow(4);
        } else if (checkedId == R.id.page_6) {
            setPageShow(5);
        } else if (checkedId == R.id.page_7) {
            setPageShow(6);
        } else if (checkedId == R.id.page_8) {
            setPageShow(7);
        } else if (checkedId == R.id.page_9) {
            setPageShow(8);
        } else if (checkedId == R.id.page_10) {
            setPageShow(9);
        } else if (checkedId == R.id.page_11) {
            setPageShow(10);
        } else if (checkedId == R.id.page_12) {
            setPageShow(11);
        } else if (checkedId == R.id.page_13) {
            setPageShow(12);
        } else if (checkedId == R.id.page_14) {
            setPageShow(13);
        } else if (checkedId == R.id.page_15) {
            setPageShow(14);
        }
    }

    private void setPageShow(final int pageNo) {
        mDialog = ProgressDialog.show(EmrScanView.this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                //String userId = getUserId();
                //String url = "";

                String mode = "7";
                if ("signed".equalsIgnoreCase(mFrom)) {
                    mode = "";
                }
                // 기타서식
                if ("".equalsIgnoreCase(mUrlPage[pageNo])) {
                    String imagePath = mPathPage[pageNo].replace("\\", "/"); // 1 ~ 10 페이지
                    String imageUrl = "EmrScanServlet?hospitalid=" + hospitalId + "&path=" + imagePath + "&mode=" + mode;
                    mUrlPage[pageNo] = getFullUrl(imageUrl);
                }
                mFullUrl = mUrlPage[pageNo];
                Log.d("EmrDroid-Servlet", mFullUrl);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetEmrScanView();
                            mDialog.dismiss();
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();

    }


//	private void startPlay(){
//		mDialog = ProgressDialog.show(EmrScanView.this, "", getString(R.string.query_wait_message), true);
//		new Thread(new Runnable() {
//			public void run() {
//				String hospitalId = getHospitalId();
//				String imagePath = mPath.replace("\\", "/");
//				String imageUrl = "EmrScanServlet?hospitalid=" + hospitalId + "&path=" + imagePath + ".mp4" + "&mode=4";
//				mFullUrl = getFullUrl(imageUrl);
//				//String dstPath = EmrScanView.this.getFilesDir() + File.separator + "mp4" + File.separator + "emrscanview_mp4.mp4";
//				//Utils.downFile(EmrScanView.this, mFullUrl, dstPath);
//				Log.d("EmrDroid-Servlet",mFullUrl);
//				
//				mHandler.post(new Runnable() {
//					public void run() {
//						// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
//						// 이를 방지함.
//						try {
//							afterStartPlay();
//							mDialog.dismiss();
//						} catch (Exception e) {
//							;
//						}
//					}
//				});
//			}
//		}).start();
//		
//	}
//	
//	private void afterStartPlay2(){
//		String url=mFullUrl;
//		Intent intent = new Intent(Intent.ACTION_VIEW); 
//        intent.setDataAndType(Uri.parse(url), "video/*");
//        startActivity(intent);   		
//	}
//	
//	private void afterStartPlay(){
//		String mp4File = EmrScanView.this.getFilesDir() + File.separator + "mp4" + File.separator + "emrscanview_mp4.mp4";
//		mp4File = mFullUrl;
//		
//		// 재생
//		if(player!=null){
//			player.stop();
//			player.release();
//			player=null;
//		}
//		
//		try {
//			player = new MediaPlayer();
//			player.setOnCompletionListener(
//					new OnCompletionListener(){
//
//						@Override
//						public void onCompletion(MediaPlayer mp) {
//							// TODO Auto-generated method stub
//							stopPlay();
//						}
//						
//					});
//			player.setOnErrorListener(
//					new OnErrorListener(){
//
//						@Override
//						public boolean onError(MediaPlayer mp, int what, int extra) {
//							// TODO Auto-generated method stub
//							String err = "OnError occured. what = " + what + " ,extra = " + extra;
//				            Toast.makeText(EmrScanView.this, err, Toast.LENGTH_LONG).show();
//							return false;
//						}
//						
//					});
//			player.setAudioStreamType(AudioManager.STREAM_MUSIC);
//			player.setDataSource(mp4File);
//			player.setVolume(1, 1);
//			player.prepare();
//			player.start();
//		} catch (IllegalArgumentException e) {
//			// TODO Auto-generated catch block
//			//e.printStackTrace();
//			Log.d("EmrDroid","error in afterPlayRecord1=" + e.getMessage());
//		} catch (IllegalStateException e) {
//			// TODO Auto-generated catch block
//			//e.printStackTrace();
//			Log.d("EmrDroid","error in afterPlayRecord2=" + e.getMessage());
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			//e.printStackTrace();
//			Log.d("EmrDroid","error in afterPlayRecord3=" + e.getMessage());
//		} catch (Exception e){
//			Log.d("EmrDroid","error in afterPlayRecord4=" + e.getMessage());
//		}
//	}
//	
//	private void stopPlay(){
//		if(player==null) return;
//		
//		player.stop();
//		player.release();
//		player=null;
//	}

}
