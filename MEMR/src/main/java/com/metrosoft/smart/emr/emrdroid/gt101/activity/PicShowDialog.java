package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.PicShowAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ServletHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class PicShowDialog extends Activity implements OnItemClickListener, OnClickListener {
    protected ProgressDialog mDialog = null;
    protected Handler mHandler = new Handler();

    private String mXml;
    private String mPid;
    private String mBededt;
    private String mExdt;
    private String mSeq;

    private Button mBackButton;
    private ListView mListView;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mExdt = intent.getStringExtra("exdt");
        mSeq = intent.getStringExtra("seq");

        setContentView(R.layout.pic_show);

        mBackButton = (Button) findViewById(R.id.back_button);

        /** Layout으로 부터 ListView에 대한 객체를 얻는다. **/
        mListView = (ListView) findViewById(R.id.pic_list);


        mBackButton.setOnClickListener(this);
        /* Listener for selecting a item */
        mListView.setOnItemClickListener(this);

        setPicList();

    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        if (view.getId() == R.id.back_button) {
            finish();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO Auto-generated method stub
        HashMap<String, Object> map = (HashMap<String, Object>) parent.getAdapter().getItem(position);
        String hospitalId = EmrSettingsUtil.getHospitalId(getBaseContext());
        String picPath = (String) map.get("pic_path");
        String picUrl = "EmrScanServlet?hospitalid=" + hospitalId + "&path=" + picPath + "&mode=5";
        Log.d("EmrDroid", "picUrl=" + picUrl);
        String picUrlFull = getFullUrl(picUrl);
        Log.d("EmrDroid", "picUrlFull=" + picUrlFull);
        //
        Intent intent = new Intent(this, PicShowImageDialog.class);
        intent.putExtra("url", picUrlFull);
        startActivity(intent);
    }

    private void setPicList() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = EmrSettingsUtil.getHospitalId(getBaseContext());
                String url = "ChartServlet" +
                        "?hospitalid=" + hospitalId +
                        "&pid=" + mPid +
                        "&bededt=" + mBededt +
                        "&exdt=" + mExdt +
                        "&seq=" + mSeq +
                        "&mode=7";
                mXml = getXml(url);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterSetPicList();
                            mDialog.dismiss();
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();

    }

    private void afterSetPicList() {
        ResultSetHelper rs;

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        // xml해부
        try {
            // 리스트 지움.
            mListView.setAdapter(null);
            // 조회결과값
            String xml = mXml;
            Log.d("EmrDroid", "after : xml=" + xml);
            if (xml.equals("")) return;
            // xml to ResultSet
            rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                //showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                //showSimpleDialog(R.string.no_data_message);
            } else {

                for (int i = 0; i < rs.getRecordCount(); i++) {

                    String title = rs.getString(i, "title");
                    String picPath = rs.getString(i, "pic_path");
                    String exdt = rs.getString(i, "exdt");
                    String seq = rs.getString(i, "seq");
                    String picIdx = rs.getString(i, "pic_idx");

                    map = new HashMap<String, Object>();
                    map.put("title", title);
                    map.put("pic_path", picPath);
                    map.put("exdt", exdt);
                    map.put("seq", seq);
                    map.put("pic_idx", picIdx);
                    //
                    mylist.add(map);
                }

                PicShowAdapter adapter = new PicShowAdapter(this, mylist);
                mListView.setAdapter(adapter);
            }
        } catch (Exception ex) {
            //showSimpleDialog(ex.getMessage());
        }

    }

    protected String getXml(String url) {
        try {
            String servletUseYn = "";
            String servletIp = "";
            // 병원자료를 읽을 때 WAS가 Basecamp와 다른 서버에 접속해야하는지 정의
            servletUseYn = EmrSettingsUtil.getServletUseYn(getBaseContext());
            Log.d("EmrDroid", "servletUseYn = " + servletUseYn);
            servletIp = EmrSettingsUtil.getServletIp(getBaseContext());
            Log.d("EmrDroid", "servletIp = " + servletIp);
            if (servletUseYn.equalsIgnoreCase("y") == false) servletIp = "";

            Log.d("EmrDroid", "servletIp = " + servletIp);
            ServletHelper servletHelper = new ServletHelper();
            servletHelper.setServletIp(servletIp);
            return servletHelper.getXml(url);
        } catch (Exception e) {
            return null;
        }
    }

    protected String getFullUrl(String url) {
        String servletUseYn = "";
        String servletIp = "";
        // 병원자료를 읽을 때 WAS가 Basecamp와 다른 서버에 접속해야하는지 정의
        servletUseYn = EmrSettingsUtil.getServletUseYn(getBaseContext());
        Log.d("EmrDroid", "servletUseYn = " + servletUseYn);
        servletIp = EmrSettingsUtil.getServletIp(getBaseContext());
        Log.d("EmrDroid", "servletIp = " + servletIp);
        if (servletUseYn.equalsIgnoreCase("y") == false) servletIp = "";
        ServletHelper servletHelper = new ServletHelper();
        servletHelper.setServletIp(servletIp);

        return servletHelper.getFullUrl(url);
    }

}
