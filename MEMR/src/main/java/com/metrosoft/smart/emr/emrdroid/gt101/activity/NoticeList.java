package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.NoticeListAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class NoticeList extends MyActivity {
    private String mXml;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.notice_list, "�ݱ�");
        super.setQuickMenuOff();

        ListView listView = (ListView) findViewById(R.id.notice_list);
        listView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> map = (HashMap<String, Object>) (parent.getAdapter().getItem(position));
                Intent intent = new Intent(NoticeList.this, NoticeDetail.class);
                intent.putExtra("apdt", (String) map.get("apdt"));
                intent.putExtra("seq", (String) map.get("seq"));
                startActivity(intent);
            }
        });

//        String packageName = getPackageName();
//        // 제목표시줄 밑에 있는 TEMR 로그를 TEMR 페키지만 보이도록 처리
//        RelativeLayout topBgLayout = (RelativeLayout)findViewById(R.id.top_bg_layout);
//        if(!packageName.equals(EmrSettingsUtil.PACKAGE_TEMR)){
//        	topBgLayout.setVisibility(View.GONE);
//        }

        if (savedInstanceState == null) {
            getNoticeList();
        } else {
            mXml = savedInstanceState.getString("xml");
            afterGetNoticeList();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("xml", mXml);
    }

    @Override
    public void onClickQueryButton(View v) {
        getNoticeList();
    }

    private void getNoticeList() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                mXml = "";
                String url = "NoticeServlet?hospitalid=" + hospitalId +
                        "&userid=" + userId +
                        "&mode=3";
                mXml = getXml(url);
                mHandler.post(new Runnable() {
                    public void run() {
                        afterGetNoticeList();
                        mDialog.dismiss();
                    }
                });
            }
        }).start();
        ;
    }

    private void afterGetNoticeList() {
        ResultSetHelper rs;

        ListView list = (ListView) findViewById(R.id.notice_list);

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        // xml해부
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }

            rs = new ResultSetHelper(mXml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();

                for (int i = 0; i < rs.getRecordCount(); i++) {

                    map = new HashMap<String, Object>();

                    String apdt = rs.getString(i, "apdt");
                    String apdate = apdt.substring(4, 6) + "/" + apdt.substring(6, 8);
                    String empnm = rs.getString(i, "empnm");
                    if (empnm.equals("")) empnm = rs.getString(i, "drnm");
                    if (empnm.equals("")) empnm = rs.getString(i, "psid");

                    map.put("apdt", apdt);
                    map.put("apdate", apdate);
                    map.put("seq", rs.getString(i, "seq"));
                    map.put("title", rs.getString(i, "title"));
                    map.put("empnm", empnm);
                    mylist.add(map);
                }
//				SimpleAdapter adapter = new SimpleAdapter(this, mylist, R.layout.notice_list_row,
//						                new String[] {"title","apdate","empnm",},
//						                new int[] {R.id.notice_list_row_title,R.id.notice_list_row_apdate,R.id.notice_list_row_empnm});
//				list.setAdapter(adapter);
                NoticeListAdapter adapter = new NoticeListAdapter(this, mylist);
                list.setAdapter(adapter);

            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

}
