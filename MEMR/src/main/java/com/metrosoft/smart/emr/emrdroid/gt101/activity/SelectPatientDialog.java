package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.DateUtil;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.ArrayList;
import java.util.HashMap;

public class SelectPatientDialog extends MyActivity implements OnItemClickListener, OnClickListener {

    //protected ProgressDialog mDialog = null;
    //protected Handler mHandler = new Handler();

    private String mSearchXml = "";

    Button mBackButton = null;
    Button mSearchButton = null;
    EditText mSearchText = null;
    ListView mPatListView = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.select_patient_dialiog);

        mBackButton = (Button) findViewById(R.id.back_button);
        mSearchButton = (Button) findViewById(R.id.search_button);
        mSearchText = (EditText) findViewById(R.id.search_text);
        mPatListView = (ListView) findViewById(R.id.patient_list);

        mBackButton.setOnClickListener(this);
        mSearchButton.setOnClickListener(this);
        mPatListView.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        if (view.getId() == R.id.back_button) {
            finish();
        } else if (view.getId() == R.id.search_button) {
            String searchText = mSearchText.getText().toString();
            Log.d("EmrDroid", "searchText=" + searchText);
            if (searchText.equals("")) return;
            // 검색어 입력창의 키보드 내리기
            hideKeyboard();
            // 조회
            getPatientList(searchText);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO Auto-generated method stub
        HashMap<String, Object> selectedMap = (HashMap<String, Object>) mPatListView.getAdapter().getItem(position);
        String pid = (String) selectedMap.get("pid");
        String bededt = (String) selectedMap.get("bededt");
        String bedodt = (String) selectedMap.get("bedodt");
        String patientinfo = (String) selectedMap.get("pnm") + " "
                + (String) selectedMap.get("psexage") + " "
                + (String) selectedMap.get("dptcd") + " "
                + (String) selectedMap.get("ward") + " "
                + DateUtil.getFormattedDate((String) selectedMap.get("bededt")) + "~"
                + DateUtil.getFormattedDate((String) selectedMap.get("bedodt"));

        Intent intent = getIntent(); // 이 액티비티를 시작하게 한 인텐트를 호출
        intent.putExtra("pid", pid);
        intent.putExtra("bededt", bededt);
        intent.putExtra("bedodt", bedodt);
        intent.putExtra("patientinfo", patientinfo);
        setResult(RESULT_OK, intent); // 추가 정보를 넣은 후 다시 인텐트를 반환합니다.
        finish(); // 액티비티 종료

    }

    // 검색어 입력창의 키보드 내리기
    private void hideKeyboard() {
        EditText text = (EditText) findViewById(R.id.search_text);
        InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(text.getWindowToken(), 0);
    }

    private void getPatientList(final String searchText) {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {

            @Override
            public void run() {
                // TODO Auto-generated method stub
                String hospitalId = EmrSettingsUtil.getHospitalId(getBaseContext());
                String userId = EmrSettingsUtil.getUserId(getBaseContext());
                //String searchText=mSearchText.getText().toString();

                mSearchXml = "";
                String url = "InPatientListServlet" +
                        "?mode=5" +
                        "&sortorder=1" +
                        "&hospitalid=" + hospitalId +
                        "&userid=" + userId +
                        "&searchtext=" + getHangul(searchText);
                mSearchXml = getXml(url);


                // 종료
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetPatientList();
                            mDialog.dismiss();
                        } catch (Exception ex) {
                        }
                    }
                });
            }

        }).start();
    }

    private void afterGetPatientList() {
        ResultSetHelper rs;

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        // xml해부
        try {
            // 리스트 지움.
            mPatListView.setAdapter(null);
            // 조회결과값
            String xml = mSearchXml;
            //Log.d("EmrDroid","xml=" + xml);
            if (xml.equals("")) return;
            rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
                Log.d("EmrDroid", "error=" + rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                Log.d("EmrDroid", "rs count=0");
                showSimpleDialog(R.string.no_data_message);
            } else {
                Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();
                Log.d("EmrDroid", "rs count=" + rs.getRecordCount());

                for (int i = 0; i < rs.getRecordCount(); i++) {

                    String psex = rs.getString(i, "psex");
                    String bthdt = rs.getString(i, "bthdt");
                    String ward = rs.getString(i, "ward");
                    int ageY = DateUtil.getAgeYear(bthdt);

                    map = new HashMap<String, Object>();
                    map.put("image", psex.equals("M") ? R.drawable.man_icon : R.drawable.woman_icon);
                    map.put("pnm", rs.getString(i, "pnm"));
                    map.put("psexage", rs.getString(i, "psex") + "/" + ageY);
                    map.put("dptcd", rs.getString(i, "dptcd"));
                    map.put("ward", rs.getString(i, "ward"));
                    map.put("pdrnm", rs.getString(i, "pdrnm"));
                    map.put("pid", rs.getString(i, "pid"));
                    map.put("qfycd", rs.getString(i, "qfycd"));
                    map.put("qfycdnm", rs.getString(i, "qfycdnm"));
                    map.put("dxd", rs.getString(i, "dxd"));
                    //
                    String bededt = rs.getString(i, "bededt");
                    String bedodt = rs.getString(i, "bedodt");
                    map.put("bededt", bededt);
                    map.put("bedodt", bedodt);
                    //
                    String disp_bededt = DateUtil.getFormattedDate(bededt);
                    String disp_bedodt = DateUtil.getFormattedDate(bedodt);
                    String disp_bededt_bedodt = "";
                    if ("외래".equalsIgnoreCase(ward)) {
                        disp_bededt_bedodt = disp_bededt;
                    } else if ("응급".equalsIgnoreCase(ward)) {
                        disp_bededt_bedodt = disp_bededt;
                    } else {
                        if (bedodt.equals("")) disp_bedodt = "재원중";
                        disp_bededt_bedodt = disp_bededt + "~" + disp_bedodt;
                    }
                    map.put("disp_bededt_bedodt", disp_bededt_bedodt);
                    //
                    mylist.add(map);
                }
                SimpleAdapter adapter = new SimpleAdapter(this, mylist, R.layout.in_patient_list_row,
                        new String[]{"image", "pnm", "psexage", "dptcd", "ward", "pdrnm", "qfycdnm", "disp_bededt_bedodt", "dxd"},
                        new int[]{R.id.patient_list_row_image
                                , R.id.patient_list_row_pnm
                                , R.id.patient_list_row_psexage
                                , R.id.patient_list_row_dptcd
                                , R.id.patient_list_row_ward
                                , R.id.patient_list_row_pdrnm
                                , R.id.patient_list_row_qfycdnm
                                , R.id.patient_list_row_disp_bededt_bedodt
                                , R.id.patient_list_row_dxd
                        });
                mPatListView.setAdapter(adapter);

            }
        } catch (Exception ex) {
            //showSimpleDialog(ex.getMessage());
        }
    }

//    protected String getXml(String url) {
//		try {
//			String servletUseYn="";
//			String servletIp="";
//			// 병원자료를 읽을 때 WAS가 Basecamp와 다른 서버에 접속해야하는지 정의
//			servletUseYn=EmrSettingsUtil.getServletUseYn(getBaseContext());
//			Log.d("EmrDroid","servletUseYn = " + servletUseYn);
//			servletIp=EmrSettingsUtil.getServletIp(getBaseContext());
//			Log.d("EmrDroid","servletIp = " + servletIp);
//			if(servletUseYn.equalsIgnoreCase("y")==false) servletIp="";
//
//			Log.d("EmrDroid","servletIp = " + servletIp);
//			ServletHelper servletHelper = new ServletHelper();
//			servletHelper.setServletIp(servletIp);
//			return servletHelper.getXml(url);
//		} catch (Exception e) {
//			return null;
//		}
//	}

//    // 한글깨짐방지용
// 	protected String getHangul(String s) {
// 		String ret;
// 		try {
// 			ret = java.net.URLEncoder.encode(new String(s.getBytes("utf-8")));
// 			// ret = java.net.URLEncoder.encode(new String(s.getBytes("euc-kr")));
// 		} catch (UnsupportedEncodingException e) {
// 			// TODO Auto-generated catch block
// 			ret = "";
// 		}
// 		return ret;
// 	}
}
