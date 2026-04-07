package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.data.PointDilatation;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;
import com.metrosoft.smart.emr.emrdroid.gt101.view.LaborRecordGraphView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LaborRecord extends MyActivity {
    private TextView mPatientInfoTextView;
    private TextView mPatientInfoTextView2;
    private LaborRecordGraphView mGraphView;
    private ListView mLaborRecordListView;

    private String mXmlPatientInfo, mXmlPatientInfo2;
    private String mPid;
    private String mBededt;
    private String mBdiv;

    private String mXmlLaborRecord;

    private ResultSetHelper mRs = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //super.onCreate(savedInstanceState);
        //setContentView(R.layout.labor_record);
        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mXmlPatientInfo = intent.getStringExtra("patientinfo");
        mBdiv = intent.getStringExtra("bdiv");
        if (mBdiv == null) mBdiv = "2"; // 1.외래 2.입원 3.응급  기본 입원

        String fromTitle = intent.getStringExtra("fromtitle");
        if (fromTitle == null) fromTitle = "";
        if (fromTitle.equals("")) fromTitle = "닫기";

        LinearLayout linear = (LinearLayout) View.inflate(this, R.layout.labor_record, null);
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, linear, fromTitle);

        mPatientInfoTextView = (TextView) findViewById(R.id.patient_info_text_view);
        mPatientInfoTextView2 = (TextView) findViewById(R.id.patient_info_text_view2);
        mGraphView = (LaborRecordGraphView) findViewById(R.id.labor_record_graph_view);
        mLaborRecordListView = (ListView) findViewById(R.id.labor_record_list_view);

        DisplayPatientInfo();

        if (savedInstanceState == null) {
            getLaborRecorcd();
        } else {
            mXmlLaborRecord = savedInstanceState.getString("xmlLaborRecord");
            afterGetLaborRecord();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString("xmlLaborRecord", mXmlLaborRecord);
    }

    @Override
    public void onClickQueryButton(View v) {
        getLaborRecorcd();
    }

    private void DisplayPatientInfo() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                mPatientInfoTextView.setText(mXmlPatientInfo);
            }
        });
    }

    private void DisplayPatientInfo2() {
        this.runOnUiThread(new Runnable() {
            public void run() {
                mPatientInfoTextView2.setText(Html.fromHtml(mXmlPatientInfo2));
            }
        });
    }

    private void MakePatientInfo2() throws Exception {
        String sp1 = "&nbsp;";
        String sp2 = "&nbsp;&nbsp;";
        String sp3 = "&nbsp;&nbsp;&nbsp;";
        String sp4 = "&nbsp;&nbsp;&nbsp;&nbsp;";
        String sp5 = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;";

        mXmlPatientInfo2 = "";
        //mXmlPatientInfo2 += "<p style='line-height: 3.5;'>";
        mXmlPatientInfo2 += "<font face='monospace'>";
        mXmlPatientInfo2 += "T <u>" + sp3 + mRs.getString(0, "fld_t").trim() + sp3 + "</u> ";
        mXmlPatientInfo2 += "P <u>" + sp3 + mRs.getString(0, "fld_p").trim() + sp3 + "</u> ";
        mXmlPatientInfo2 += "A <u>" + sp3 + mRs.getString(0, "fld_a").trim() + sp3 + "</u> ";
        mXmlPatientInfo2 += "L <u>" + sp3 + mRs.getString(0, "fld_l").trim() + sp3 + "</u> ";
        mXmlPatientInfo2 += "L.M.P <u>" + sp1 + super.getFormattedDate(mRs.getString(0, "l_m_p")) + sp1 + "</u> ";
        mXmlPatientInfo2 += "E.D.C <u>" + sp1 + super.getFormattedDate(mRs.getString(0, "e_d_c")) + sp1 + "</u> ";
        mXmlPatientInfo2 += "G.A. <u>" + sp2 + mRs.getString(0, "g_a").trim() + sp2 + "</u> ";
        mXmlPatientInfo2 += "Onset of Labor <u>" + sp2+ mRs.getString(0, "onset_labor").trim() + sp2 + "</u> ";
        mXmlPatientInfo2 += "Membrane of Admission <u>" + sp2 + mRs.getString(0, "mem_adm").trim() + sp2 + "</u> ";
        mXmlPatientInfo2 += "Blood Type <u>" + sp2 + mRs.getString(0, "bld_type").trim() + sp2 + "</u> ";
        mXmlPatientInfo2 += "Rh <u>" + sp2 + mRs.getString(0, "bld_rh").trim() + sp2 + "</u> ";
        mXmlPatientInfo2 += "Hb/Hct <u>" + sp2 + mRs.getString(0, "hb_hct").trim() + sp2 + "</u> ";
        mXmlPatientInfo2 += "gm% <u>" + sp2 + mRs.getString(0, "gm_per").trim() + sp2 + "%</u> ";
        mXmlPatientInfo2 += "Show <u>" + sp2 + mRs.getString(0, "show").trim() + sp2 + "</u>";
        mXmlPatientInfo2 += "</font>";
        //mXmlPatientInfo2 += "</p>";
    }

    private void getLaborRecorcd() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                // 입원환자만 가능
                String url = "ChartServlet?hospitalid=" + hospitalId + "&userid=" + userId + "&pid=" + mPid + "&bededt=" + mBededt + "&mode=15";
                mXmlLaborRecord = getXml(url);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetLaborRecord();
                            mDialog.dismiss();
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetLaborRecord() {
        try {
            // 오류발생
            if (super.getXmlError() == true) {
                super.showToastText(super.getXmlErrorMessage());
                return;
            }
            mRs = new ResultSetHelper(mXmlLaborRecord, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (mRs.getReturnCode() < 0) {
                showSimpleDialog(mRs.getReturnDesc());
            } else if (mRs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                DisplayPatientInfo();

                int len = mRs.getRecordCount();

                // Labor Record 헤더 정보 출력
                MakePatientInfo2();
                DisplayPatientInfo2();

                // 그래프를 그리는 용도
                List<PointDilatation> points = new ArrayList<>();
                for (int i = 0; i < len; i++) {
                    String exdt = mRs.getString(i, "exdt");
                    String extm = mRs.getString(i, "extm");
                    float value = (float)mRs.getDouble(i, "dilatation");
                    points.add(new PointDilatation(i, exdt, extm, value));
                }
                mGraphView.setDataPoints(points);


                // 리스트 뷰에 보여줄 자료를 만든다.
                ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
                for (int i = 0; i < len; i++) {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    map.put("exdt", super.getFormattedDate(mRs.getString(i,"exdt")));
                    map.put("extm", super.getFormattedTime(mRs.getString(i,"extm")));
                    map.put("b_p", mRs.getString(i,"b_p"));
                    map.put("p", mRs.getString(i,"p"));
                    map.put("f_h_t", mRs.getString(i,"f_h_t"));
                    map.put("b_t", mRs.getString(i,"b_t"));
                    map.put("position", mRs.getString(i,"position"));
                    map.put("station", mRs.getString(i,"station"));
                    map.put("dilatation", mRs.getString(i,"dilatation"));
                    map.put("interval", mRs.getString(i,"interval"));
                    map.put("duration", mRs.getString(i,"duration"));
                    map.put("membrane", mRs.getString(i,"membrane"));
                    map.put("medi_rmk" ,mRs.getString(i,"medi_rmk"));
                    map.put("medi_rmk2", mRs.getString(i,"medi_rmk2"));
                    map.put("empnm", mRs.getString(i,"empnm"));

                    mylist.add(map);
                }
                SimpleAdapter adapter = new SimpleAdapter(this, mylist, R.layout.labor_record_list_view_row,
                        new String[]{"exdt", "extm", "b_p", "p", "f_h_t", "b_t", "positation", "station", "dilatation", "interval", "duration", "membrane", "med_rmk", "med_rmk2", "empnm"},
                        new int[]{R.id.labor_record_list_view_row_exdt
                                , R.id.labor_record_list_view_row_extm
                                , R.id.labor_record_list_view_row_bp
                                , R.id.labor_record_list_view_row_p
                                , R.id.labor_record_list_view_row_fht
                                , R.id.labor_record_list_view_row_bt
                                , R.id.labor_record_list_view_row_position
                                , R.id.labor_record_list_view_row_station
                                , R.id.labor_record_list_view_row_dilatation
                                , R.id.labor_record_list_view_row_interval
                                , R.id.labor_record_list_view_row_duration
                                , R.id.labor_record_list_view_row_membrane
                                , R.id.labor_record_list_view_row_med_rmk
                                , R.id.labor_record_list_view_row_med_rmk2
                                , R.id.labor_record_list_view_row_empnm
                        });
                mLaborRecordListView.setAdapter(adapter);


            }
        } catch (Exception ex) {
            mRs = null;
            Toast.makeText(LaborRecord.this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

}
