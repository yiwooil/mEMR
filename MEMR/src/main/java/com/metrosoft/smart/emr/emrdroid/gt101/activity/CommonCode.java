package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.TabStopSpan;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class CommonCode extends MyActivity {
    public final static int WARD_CODE = 0;
    public final static int DEPT_CODE = 1;
    public final static int DOCT_CODE = 2;
    public final static int OUT_DEPT_CODE = 3;
    public final static int OUT_DOCT_CODE = 4;
    //public final static int TPR_COLOR=3;

    private Integer mMode; // 0.병동 1.진료과 2.의사
    private String mDefault; // 넘어온 기본값
    private String mDptcd; // 의사검색시 진료과
    private String mXml;

    private ListView mListView;
    private ArrayList<HashMap<String, Object>> mCommonCodeList = new ArrayList<HashMap<String, Object>>();

    private Spinner mSpDoctorDept;
    private ArrayAdapter<String> mDeptSpinnerAdapter;

    // 서버에서 받은 "전체 의사목록 원본"
    private ArrayList<HashMap<String, Object>> mDoctorAllList = new ArrayList<HashMap<String, Object>>();

    // 화면에 뿌리는 adapter를 멤버로
    private SimpleAdapter mAdapter;

    // 현재 선택된 진료과(필터)
    private String mSelectedDptcd = "";
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.common_code);

        Intent intent = getIntent();
        int mode = intent.getIntExtra("mode", 0);
        mMode = mode;
        mDefault = intent.getStringExtra("default");
        mDptcd = "";

        String title = "코드선택";
        if (mMode == WARD_CODE) title = "병동선택";
        else if (mMode == DEPT_CODE || mMode == OUT_DEPT_CODE) title = "진료과선택";
        else if (mMode == DOCT_CODE || mMode == OUT_DOCT_CODE) title = "의사선택";

        if (mMode == DOCT_CODE || mMode == OUT_DOCT_CODE) mDptcd = intent.getStringExtra("dptcd");

        // 코드리스트
        LinearLayout layout = (LinearLayout) View.inflate(CommonCode.this, R.layout.common_code_dialog, null);
        AlertDialog.Builder dialog = new AlertDialog.Builder(CommonCode.this);
        // 디이얼로그 창이 제목
        //dialog.setTitle(title);
        View customTitleView = getLayoutInflater().inflate(R.layout.custom_dialog_title_bar, null);
        TextView tv = (TextView) customTitleView.findViewById(R.id.custom_dialog_title_bar_text);
        tv.setText(title);
        dialog.setCustomTitle(customTitleView);
        dialog.setInverseBackgroundForced(true);
        // 내용
        dialog.setView(layout);
        dialog.setCancelable(false);
        // 버튼을 만든다
        final Button okButton = (Button) layout.findViewById(R.id.okButton);
        okButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                int pos = mListView.getCheckedItemPosition();
                if (pos < 0) {
                    // 취소와 동일하에 동작
                    Toast.makeText(CommonCode.this, "선택한 값이 없습니다.", Toast.LENGTH_LONG).show();
                    Intent intent = getIntent(); // 이 액티비티을 시작하게 한 인텐트를 호출
                    intent.putExtra("code", "");
                    intent.putExtra("codenm", "");
                    setResult(RESULT_CANCELED, intent); // 추가 정보를 넣은 후 다시 인텐트를 반환합니다.
                    finish(); // 액티비티 종료
                    return;
                }
                Log.d("EmrDroid", "pos=" + pos);
                String code = (String) (mCommonCodeList.get(pos).get("code"));
                String codenm = (String) (mCommonCodeList.get(pos).get("codenm"));
                Intent intent = getIntent(); // 이 액티비티를 시작하게 한 인텐트를 호출
                intent.putExtra("code", code);
                intent.putExtra("codenm", codenm);
                if (mMode == DOCT_CODE || mMode == OUT_DOCT_CODE){
                    intent.putExtra("drengnm", (String) (mCommonCodeList.get(pos).get("drengnm"))); // 의사 영문명
                    intent.putExtra("gdrlcid", (String) (mCommonCodeList.get(pos).get("gdrlcid"))); // 면허번호
                    intent.putExtra("sdrlcid", (String) (mCommonCodeList.get(pos).get("sdrlcid"))); // 전문의 번호
                    intent.putExtra("drsign", (String) (mCommonCodeList.get(pos).get("drsign"))); // 의사 사인 정보
                    intent.putExtra("dptcd", (String) (mCommonCodeList.get(pos).get("dptcd"))); // 의사의 진료과코드
                    intent.putExtra("dptnm", (String) (mCommonCodeList.get(pos).get("dptnm"))); // 의사의 진료과명

                }
                setResult(RESULT_OK, intent); // 추가 정보를 넣은 후 다시 인텐트를 반환합니다.
                finish(); // 액티비티 종료
            }
        });
        final Button cancelButton = (Button) layout.findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = getIntent(); // 이 액티비티를 시작하게 한 인텐트를 호출
                intent.putExtra("code", "");
                intent.putExtra("codenm", "");
                setResult(RESULT_CANCELED, intent); // 추가 정보를 넣은 후 다시 인텐트를 반환합니다.
                finish(); // 액티비티 종료
            }
        });
        dialog.show();

        mListView = (ListView) layout.findViewById(R.id.common_code_list);
        mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        mSpDoctorDept = (Spinner) layout.findViewById(R.id.sp_doctor_dept);

        if (mMode == DOCT_CODE || mMode == OUT_DOCT_CODE) {
            if (mSpDoctorDept != null) {
                // 특정 진료과를 선택한 경우에는 진료과 콤보를 보이지 않게 한다.
                if ("".equalsIgnoreCase(mDptcd)) {
                    mSpDoctorDept.setVisibility(View.VISIBLE);

                    mSpDoctorDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            String sel = (String) parent.getItemAtPosition(position);
                            // "모든진료과" 선택이면 필터 해제
                            if ("모든진료과".equals(sel)) {
                                mSelectedDptcd = "";
                            } else {
                                mSelectedDptcd = sel; // dptcd 자체를 표시/선택값으로 사용
                            }
                            applyDoctorFilter(); // 서버 재호출 없이 화면만 갱신
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                        }
                    });
                }
            }
        } else {
            if (mSpDoctorDept != null) mSpDoctorDept.setVisibility(View.GONE);
        }

        getCommonCodeList();
    }

    private void getCommonCodeList() {
        // mode = 0 : 병동
        //        1 : 진료과
        //        2 : 의사
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String url = "";
                String mode = "";
                if (mMode == WARD_CODE) mode = "0";
                else if (mMode == DEPT_CODE || mMode == OUT_DEPT_CODE) mode = "1";
                else if (mMode == DOCT_CODE || mMode == OUT_DOCT_CODE) mode = "2";
                // 코드리스트
                mXml = "";
                url = "CommonCodeServlet?hospitalid=" + hospitalId + "&mode=" + mode + "&dptcd=" + mDptcd;
                mXml = getXml(url);
                // 종료
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회 중 화면이 전환도는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetCommonCodeList();
                            mDialog.dismiss();
                        } catch (Exception ex) {
                            ;
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetCommonCodeList() {
        // 오류발생
        if (super.getXmlError() == true) {
            super.showToastText(super.getXmlErrorMessage());
            return;
        }

        mCommonCodeList.clear();
        HashMap<String, Object> map = null;
        ResultSetHelper rs;
        try {
            rs = new ResultSetHelper(mXml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                // 데이터
                if (mMode == WARD_CODE) {
                    map = new HashMap<String, Object>();
                    map.put("code", "");
                    map.put("codenm", "모든병동");
                    map.put("codenm_disp", "모든병동");
                    mCommonCodeList.add(map);
                    for (int i = 0; i < rs.getRecordCount(); i++) {
                        map = new HashMap<String, Object>();
                        map.put("code", rs.getString(i, "wardid"));
                        map.put("codenm", rs.getString(i, "wardnm"));
                        map.put("codenm_disp", rs.getString(i, "wardnm"));
                        mCommonCodeList.add(map);
                    }
                } else if (mMode == DEPT_CODE || mMode == OUT_DEPT_CODE) {
                    map = new HashMap<String, Object>();
                    map.put("code", "");
                    map.put("codenm", "모든진료과");
                    map.put("codenm_disp", "모든진료과");
                    mCommonCodeList.add(map);
                    for (int i = 0; i < rs.getRecordCount(); i++) {
                        map = new HashMap<String, Object>();
                        map.put("code", rs.getString(i, "deptcd"));
                        map.put("codenm", rs.getString(i, "deptnm"));
                        map.put("codenm_disp", rs.getString(i, "deptnm"));
                        mCommonCodeList.add(map);
                    }
                } else {
                    map = new HashMap<String, Object>();
                    map.put("code", "");
                    map.put("codenm", "모든의사");
                    map.put("codenm_disp", "모든의사");
                    map.put("drnm_eng", ""); // 의사 영문명
                    map.put("gdrlcid", ""); // 면허번호
                    map.put("sdrlcid", ""); // 전문의 번호
                    map.put("drsign", ""); // 의사 사인 정보
                    map.put("dptcd", ""); // 의사의 진료과코드
                    map.put("dptnm", ""); // 의사의 진료과명
                    mCommonCodeList.add(map);
                    for (int i = 0; i < rs.getRecordCount(); i++) {
                        map = new HashMap<String, Object>();
                        map.put("code", rs.getString(i, "drid")); // 의사id
                        map.put("codenm", rs.getString(i, "drnm")); // 의사명
                        map.put("codenm_disp", rs.getString(i, "drnm") + "\t" + rs.getString(i, "dptcd")); // 의사명
                        map.put("drengnm", rs.getString(i, "drengnm")); // 의사 영문명
                        map.put("gdrlcid", rs.getString(i, "gdrlcid")); // 면허번호
                        map.put("sdrlcid", rs.getString(i, "sdrlcid")); // 전문의 번호
                        map.put("drsign", rs.getString(i, "drsign")); // 의사 사인 정보
                        // 2026.04.28 WOOIL - 서버 모듈이 다 올라가기 전에 오류가 발생하는 것을 방지하기 위한 코딩임.
                        //                    서버 모율이 다 올라가면 원상복구 하자.
                        try {
                            map.put("dptcd", rs.getString(i, "dptcd")); // 의사의 진료과코드
                        } catch (Exception ex) {
                            map.put("dptcd", ""); // 의사의 진료과코드
                        }
                        try {
                            map.put("dptnm", rs.getString(i, "dptnm")); // 의사의 진료과명
                        } catch (Exception ex) {
                            map.put("dptnm", ""); // 의사의 진료과명
                        }
                        mCommonCodeList.add(map);
                    }
                    mDoctorAllList.clear();
                    mDoctorAllList.addAll(mCommonCodeList);
                    // Spinner(진료과) 구성
                    buildDoctorDeptSpinnerItems();
                    // 현재 선택된 mSelectedDptcd 기준으로 필터 적용
                    applyDoctorFilter();
                }

                //SimpleAdapter adapter = new SimpleAdapter(this, mCommonCodeList, R.layout.common_code_list_row,
                //        new String[]{"codenm"},
                //        new int[]{R.id.common_code_codenm
                //        });
                //mListView.setAdapter(adapter);

                if (mAdapter == null) {
                    mAdapter = new SimpleAdapter(
                            this,
                            mCommonCodeList,
                            R.layout.common_code_list_row,
                            new String[]{"codenm_disp"},
                            new int[]{R.id.common_code_codenm}
                    );
                    mListView.setAdapter(mAdapter);

                    mAdapter.setViewBinder(new SimpleAdapter.ViewBinder() {
                        @Override
                        public boolean setViewValue(View view, Object data, String text) {
                            if (view.getId() == R.id.common_code_codenm
                                    && (mMode == DOCT_CODE || mMode == OUT_DOCT_CODE)) {

                                CheckedTextView tv = (CheckedTextView) view;

                                // 탭 정렬 적용
                                SpannableString ss = new SpannableString(text);
                                // 탭 시작 위치(px): (row 전체 폭 - 체크마크 여유 - dptcd 예상 폭)
                                //int tabPx = tv.getResources().getDisplayMetrics().widthPixels - dp(tv, 1000);
                                int tabPx = 400; // 위 코딩이 잘 안되서 고정....
                                ss.setSpan(new TabStopSpan.Standard(tabPx), 0, ss.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                                tv.setText(ss);
                                return true;
                            }
                            return false;
                        }

                        private int dp(View v, int dp) {
                            return (int) (dp * v.getResources().getDisplayMetrics().density + 0.5f);
                        }
                    });
                } else {
                    mAdapter.notifyDataSetChanged();
                }

                // 기본값 셋팅
                //Log.d("EmrDroid","mDefault=" + mDefault);
                for (int i = 0; i < mListView.getCount(); i++) {
                    String code = (String) (mCommonCodeList.get(i).get("code"));
                    //Log.d("EmrDroid","code=" + code);
                    if (mDefault.equalsIgnoreCase(code)) {
                        mListView.setItemChecked(i, true);
                        break;
                    }
                }
            }
        } catch (Exception ex) {
            ;
        }
    }

    private void buildDoctorDeptSpinnerItems() {
        if (mSpDoctorDept == null) return;

        Set<String> dptSet = new LinkedHashSet<String>();

        for (int i = 0; i < mDoctorAllList.size(); i++) {
            String dptcd = (String) mDoctorAllList.get(i).get("dptcd");
            if (dptcd == null) continue;

            dptcd = dptcd.trim();
            if (dptcd.length() == 0) continue;

            dptSet.add(dptcd);
        }

        // Set → List 변환
        ArrayList<String> sortedList = new ArrayList<String>(dptSet);

        // 정렬 로직
        Collections.sort(sortedList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {

                boolean isTest1 = o1.toUpperCase().startsWith("TEST");
                boolean isTest2 = o2.toUpperCase().startsWith("TEST");

                // TEST 아닌 것이 먼저
                if (isTest1 && !isTest2) return 1;
                if (!isTest1 && isTest2) return -1;

                // 둘 다 같은 그룹이면 알파벳 순
                return o1.compareToIgnoreCase(o2);
            }
        });

        // Spinner items 구성
        ArrayList<String> items = new ArrayList<String>();
        items.add("모든진료과");     // 항상 맨 위
        items.addAll(sortedList);

        mDeptSpinnerAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                items
        );
        mDeptSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpDoctorDept.setAdapter(mDeptSpinnerAdapter);

        mSpDoctorDept.setSelection(0);
    }

    private void applyDoctorFilter() {
        if (!(mMode == DOCT_CODE || mMode == OUT_DOCT_CODE)) return; // 의사 리스트가 아니면 종료

        // 원본에서 다시 구성
        mCommonCodeList.clear();

        if (mSelectedDptcd == null || mSelectedDptcd.trim().length() == 0) {
            // 모든진료과
            mCommonCodeList.addAll(mDoctorAllList);
        } else {
            for (int i = 0; i < mDoctorAllList.size(); i++) {
                String dptcd = (String) mDoctorAllList.get(i).get("dptcd");
                if (dptcd == null) dptcd = "";
                if (mSelectedDptcd.equals(dptcd.trim())) {
                    mCommonCodeList.add(mDoctorAllList.get(i));
                }
            }
        }

        if (mAdapter != null) mAdapter.notifyDataSetChanged();

        // 필터 후 기본값 체크(필요 시)
        if (mDefault != null) {
            for (int i = 0; i < mListView.getCount(); i++) {
                String code = (String) (mCommonCodeList.get(i).get("code"));
                if (mDefault.equalsIgnoreCase(code)) {
                    mListView.setItemChecked(i, true);
                    break;
                }
            }
        }
    }

}
