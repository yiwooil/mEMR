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
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Base64;
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
import com.metrosoft.smart.emr.emrdroid.gt101.pdf.PdfInkSignView;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Utils;

import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLEncoder;

public class EmrScanView extends MyActivity implements OnCheckedChangeListener {
    private String mXmlPatientInfo;
    private String mPid;
    private String mBededt;
    private String mBdiv;
    private String mExdt;
    private String mSeq;
    private String mRptcd;
    private String mPath;// 2026.04.10 WOOIL - mPath2제거, mPath2; // 2013.09.09 WOOIL - 자인컴은 PATH2가 있음.
    private String mFullUrl;// 2026.04.10 WOOIL - mFullUrl2 제거, mFullUrl2; // 2013.09.09 WOOIL - 자인컴은 2가 있음.
    private String mFrom; // 2014.03.10 - 동의서열람에서 왔으면 녹음파일을 듣는기능을 넣자.
    private String mSubPageList; // 2022.03.22 WOOIL - 서브페이지 리스트
    private String mSubPageNo; // 2022.03.22 WOOIL - 서브페이지 여부
    private int mPageCount; // 2022.03.22 - 동의서 페이지 수
    private String mp4Xml;
    private String picXml;
    private String mTsaStatus; // 2026.04.10 WOOIL - TSA 상태(S.성공)
    private String mTsaDate; // 2026.04.10 WOOIL - TSA 일자(yyyymmdd)

    private String[] mPathPage = new String[15]; // 2022.03.22 WOOIL - 1~10 페이지의 파일 정보
    private String[] mUrlPage = new String[15]; // 2022.03.23 WOOIL - 1~10 페이지 정보
    private boolean[] mIsDownloadPdf = new boolean[15]; // 2026.04.28 WOOIL - PDF문서를 한 번만 다운로드하기 위한 변수
    private MediaPlayer player;

    private WebView mWebView;
    private TextView mPatientInfoTextView;
    private PdfInkSignView mPdfView; // 2026.04.15 WOOIL - PDF를 보여주기 위한 용도
    private int mCurrentPageNo = 0; // 2026.04.15 WOOIL - PDF를 보여주기 위한 용도

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

        // 2026.04.14 WOOIL - PDF 보기용 뷰 추가
        mPdfView = new PdfInkSignView(this);
        mPdfView.setVisibility(View.GONE);
        mPdfView.setMode(PdfInkSignView.MODE_NONE);
        android.widget.LinearLayout.LayoutParams pdfParams =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT);
        ((android.view.ViewGroup) mWebView.getParent()).addView(mPdfView, pdfParams);

        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mBdiv = intent.getStringExtra("bdiv");
        mExdt = intent.getStringExtra("exdt");
        mSeq = intent.getStringExtra("seq");
        mRptcd = intent.getStringExtra("rptcd");
        mPath = intent.getStringExtra("path");
        //mPath2 = intent.getStringExtra("path2");
        mXmlPatientInfo = intent.getStringExtra("patientinfo");
        mFrom = intent.getStringExtra("from");
        mSubPageList = intent.getStringExtra("sub_page_list"); // 2022.03.22 WOOIL - 서브페이지 리스트
        mSubPageNo = intent.getStringExtra("sub_page_no"); // 2022.03.22 WOOIL - 서브페이지 여부
        mTsaStatus = intent.getStringExtra("tsa_status"); // 2026.04.10 WOOIL
        mTsaDate = intent.getStringExtra("tsa_date"); // 2026.04.10 WOOIL

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
            mTsaStatus = savedInstanceState.getString("mTsaStatus"); // 2026.04.10 WOOIL
            mTsaDate = savedInstanceState.getString("mTsaDate"); // 2026.04.10 WOOIL
            //mFullUrl2 = savedInstanceState.getString("mFullUrl2");
            mp4Xml = savedInstanceState.getString("mp4Xml");
            picXml = savedInstanceState.getString("picXml");
            // 화면에 다시 출력
            afterGetEmrScanView();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("mFullUrl", mFullUrl);
        outState.putString("mTsaStatus", mTsaStatus);
        outState.putString("mTsaDate", mTsaDate);
        //outState.putString("mFullUrl2", mFullUrl2);
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

                String mode = "7";
                if ("signed".equalsIgnoreCase(mFrom)) {
                    mode = "";
                }
                // 기타서식
                String imagePath = mPath.replace("\\", "/");
                String imageUrl = "EmrScanServlet?hospitalid=" + hospitalId + "&path=" + imagePath + "&mode=" + mode;
                mFullUrl = getFullUrl(imageUrl);
                Log.d("EmrDroid-Servlet", mFullUrl);

                // 여러페이지인 경우 페이지 정보를
                for (int i = 0; i < 15; i++) {
                    mPathPage[i] = "";
                    mUrlPage[i] = "";
                    mIsDownloadPdf[i] = false; // 2026.04.28 WOOIL - 초기화
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
                            mDialog.dismiss();
                            afterGetEmrScanView();
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

        boolean isPdf = isPdfUrl(mFullUrl) || isPdfPath(mFullUrl);

        if (isPdf) {
            mDialog = ProgressDialog.show(this, "", "PDF 문서 처리 중입니다.", true);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final File pdfFile = downloadPdfToLocal(mFullUrl, "emrscan_" + mCurrentPageNo + ".pdf", mCurrentPageNo);
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try { mDialog.dismiss(); } catch (Exception ignore) {}
                                if (!isValidPdfFile(pdfFile)) {
                                    showSimpleDialog("다운로드된 파일이 PDF가 아닙니다.");
                                } else {
                                    showPdfInPdfView(pdfFile);
                                }
                            }
                        });
                    } catch (final Exception e) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                try { mDialog.dismiss(); } catch (Exception ignore) {}
                                showSimpleDialog("PDF 다운로드/표시 오류: " + e.getMessage());
                            }
                        });
                    }
                }
            }).start();
        } else {
            if ("s".equalsIgnoreCase(mTsaStatus) == false) {
                imageUrl = "<img width='100%' src=\"" + mFullUrl + "\">";
            } else {
                String stampDate = getStampDateText(mTsaDate);
                String stampImageBase64 = getStampImageBase64();
                String stampImgSrc = "data:image/png;base64," + stampImageBase64;
                String imageHtml =
                        "<div class='page-wrap'>"
                                + "  <img class='main-img' src='" + mFullUrl + "' />"
                                + "  <div class='stamp-wrap'>"
                                + "    <img class='stamp-img' src='" + stampImgSrc + "' />"
                                + "    <div class='stamp-date'>" + stampDate + "</div>"
                                + "  </div>"
                                + "</div>";
                String html =
                        "<html>"
                                + "<head>"
                                + "<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes' />"
                                + "<style>"
                                + "body { margin:0; padding:0; background:#ffffff; }"
                                + ".page-wrap { position:relative; width:100%; }"
                                + ".main-img { width:100%; height:auto; display:block; }"
                                + ".stamp-wrap { position:absolute; left:10px; top:10px; width:110px; height:110px; }"
                                + ".stamp-img { width:100%; height:100%; display:block; }"
                                + ".stamp-date {"
                                + "  position:absolute;"
                                + "  left:0;"
                                + "  top:53px;"
                                + "  width:100%;"
                                + "  text-align:center;"
                                + "  font-size:14px;"
                                + "  font-weight:bold;"
                                + "  color:#000000;"
                                + "}"
                                + "</style>"
                                + "</head>"
                                + "<body>"
                                + imageHtml
                                + "</body>"
                                + "</html>";
                imageUrl = html;
            }
            mWebView.loadDataWithBaseURL(
                    null, //"android.resource://" + getPackageName() + "/",
                    imageUrl,
                    "text/html",
                    "utf-8",
                    null
            );

            mWebView.getSettings().setSupportZoom(true);
            mWebView.getSettings().setBuiltInZoomControls(true);
            mWebView.getSettings().setUseWideViewPort(true);
            mWebView.setInitialScale(BIND_AUTO_CREATE);
        }
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

    private boolean isPdfUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains(".pdf");
    }
    private boolean isPdfPath(String path) {
        if (path == null) return false;
        return path.toLowerCase().endsWith(".pdf");
    }

    private String getGooglePdfViewerUrl(String pdfUrl) {
        try {
            return "https://docs.google.com/gview?embedded=true&url="
                    + URLEncoder.encode(pdfUrl, "UTF-8");
        } catch (Exception e) {
            return pdfUrl;
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

                mCurrentPageNo = pageNo;

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
                            mDialog.dismiss();
                            afterGetEmrScanView();
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();

    }

    private String getStampDateText(final String exdt) {
        if (exdt == null) return "";
        if (exdt.length() == 8) {
            return exdt.substring(0, 4) + "." + exdt.substring(4, 6) + "." + exdt.substring(6, 8);
        }
        return exdt;
    }

    private String getStampImageBase64() {
        try {
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.timestamp_url);
            if (bitmap == null) return "";

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] imageBytes = baos.toByteArray();

            return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    // 2026.04.15 WOOIL - PDF문서를 다운로드 하다.
    private File downloadPdfToLocal(String url, String fileName, int pageNo) throws Exception {
        String dirPath = getFilesDir().getAbsolutePath() + File.separator + "emrscan_pdf";
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File outFile = new File(dir, fileName);
        // 2026.04.28 WOOIL - 한 번 다운도르한 파일은 다시 다운로드하지 않는다.
        if (mIsDownloadPdf[pageNo] == false) {
            Utils.downFile(this, url, outFile.getAbsolutePath());
            mIsDownloadPdf[pageNo] = true;
        }
        return outFile;
    }

    // 2026.04.15 WOOIL - 올바른 PDF 문서인지 검사
    private boolean isValidPdfFile(File file) {
        FileInputStream fis = null;
        try {
            if (file == null || !file.exists() || file.length() < 5) return false;

            fis = new FileInputStream(file);
            byte[] buf = new byte[5];
            int len = fis.read(buf);
            if (len < 5) return false;

            String header = new String(buf, 0, len, "ISO-8859-1");
            return header.startsWith("%PDF-");
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (Exception ignore) {
            }
        }
    }

    // 2026.04.15 WOOIL - PDF 문서를 표시한다.
    private void showPdfInPdfView(File pdfFile) {
        try {
            mWebView.setVisibility(View.GONE);
            mPdfView.setVisibility(View.VISIBLE);
            mPdfView.setMode(PdfInkSignView.MODE_NONE);

            // EmrScanView는 하나의 PdfInkSignView로 여러 PDF 파일을 번갈아 연다.
            // 이전 페이지 PDF의 sign_image overlay가 다음 페이지에 남지 않도록 초기화한다.
            mPdfView.clearAllOverlays();

            mPdfView.openPdf(pdfFile, 0,null, "");
        } catch (Exception e) {
            showSimpleDialog("PDF 열기 오류: " + e.getMessage());
        }
    }
}
