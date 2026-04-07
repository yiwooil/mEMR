package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SimpleAdapter;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.ConsentFormListAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.PresavedConsentFormListAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.ResultLisAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.DateUtil;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PresavedConsentFormList extends MyActivity implements ListView.OnScrollListener {

    private Activity mActivity;

    private static final int REQ_RE_SAVE = 2001;
    private static final int REQ_DEPT = 2002;
    private static final int REQ_DOCT = 2003;

    private String mXml;
    private Button mDeptButton;
    private Button mPdridButton;
    private EditText mSearchText;
    private Button mSearchButton;
    private ListView mConsentFormList;

    private String mSortOrder = "0"; // 정렬순서
    private String mConditionDeptCode, mConditionDeptCodeName;
    private String mConditionPdridCode, mConditionPdridCodeName;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState, R.layout.presaved_consent_form_list, "재원환자");

        mActivity = this;

        super.setQueryButton(false); // 조회번튼을 보이지 않게 하자
        super.setButton1(true, "정렬옵션", BUTTON_TYPE_OPTION); // 2025.09.05 WOOIL - 추가

        // 2026.02.20 WOOIL - 진료과선택버튼
        mConditionDeptCode = "";
        mConditionDeptCodeName = "모든진료과";
        mDeptButton = (Button) findViewById(R.id.dept_button);
        mDeptButton.setText(mConditionDeptCodeName);
        // 2026.02.20 WOOIL - 의사선택버튼
        mConditionPdridCode = "";
        mConditionPdridCodeName = "모든의사";
        mPdridButton = (Button) findViewById(R.id.pdrid_button);
        mPdridButton.setText(mConditionPdridCodeName);

        mSearchText = (EditText) findViewById(R.id.search_text);
        mSearchText.setVisibility(View.INVISIBLE); // 화면 구동할 때 포커스가 가면 키보드가 올라와서 올라오지 못하게 막는 용도임.

        mSearchButton = (Button) findViewById(R.id.search_button);
        mConsentFormList = (ListView) findViewById(R.id.consent_form_list);

        mSortOrder = "0";
        mSortOrder = EmrSettingsUtil.getPresavedConsentFormListSortOrder(getBaseContext(), "0");

        setListener();

        if (savedInstanceState == null) {
            getPresavedConsendFormList();
        } else {
            mXml = savedInstanceState.getString("mXml");
            afterGetPresavedConsendFormList(false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // 2024.03.19 WOOIL - 대구W 병원에서 앱이 죽는 현상이 발견됨.
        //                    에러를 찾아보니 TransactionTooLargeException 였음.
        //                    이 부분을 막으니 정상동작함.
        //outState.putString("mXml", mXml);
    }

    @Override
    public void onClickButton1(View v) {
        SortOrderDialog dialog = new SortOrderDialog(PresavedConsentFormList.this);
        dialog.show();
    }

    private void setListener() {
        mDeptButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(PresavedConsentFormList.this, CommonCode.class);
                intent.putExtra("mode", CommonCode.DEPT_CODE);
                intent.putExtra("default", mConditionDeptCode);
                startActivityForResult(intent, REQ_DEPT);
            }
        });
        mPdridButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(PresavedConsentFormList.this, CommonCode.class);
                intent.putExtra("mode", CommonCode.DOCT_CODE);
                intent.putExtra("default", mConditionPdridCode);
                intent.putExtra("dptcd", mConditionDeptCode);
                startActivityForResult(intent, REQ_DOCT);
            }
        });
        mSearchButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View view) {
                // 검색어 입력창의 키보드 내리기
                hideKeyboard();
                // 조회
                getPresavedConsendFormList();
            }
        });
        mConsentFormList.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> map = (HashMap<String, Object>) (parent.getAdapter().getItem(position));
                // 2026.04.01 WOOIL - 한 임시저장동의서를 다른 단말기에서 시차를 추고 처리한 경우가 있음
                //                    사인을 받으러 가기 전에 삭제여부를 확인하자...
                callConsentForm(map);
            }
        });
        mConsentFormList.setOnItemLongClickListener(new GridView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                HashMap<String, Object> map = (HashMap<String, Object>) (HashMap<String, Object>) parent.getAdapter().getItem(position);
                String preSaved = (String) map.get("pre_saved");
                final String pid = (String) map.get("pid");
                final String preSavedBdiv = (String) map.get("pre_saved_bdiv");
                final String exdt = (String) map.get("exdt");
                final String seq = (String) map.get("seq");
                final String subPageList = (String) map.get("sub_page_list");
                final String isdateline = (String) map.get("isdateline"); // 2025.09.08 WOOIL -

                if("1".equals(isdateline)) return false;
                if (preSaved == null) return false;
                if (preSaved.equalsIgnoreCase("y")) {
                    PopupMenu menu = new PopupMenu(PresavedConsentFormList.this, view);
                    menu.getMenu().add(0,1,0,"삭제");
                    menu.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                        public boolean onMenuItemClick(MenuItem item) {
                            // TODO Auto-generated method stub
                            deletePreSaved(pid, preSavedBdiv, exdt, seq, subPageList);
                            return false;
                        }
                    });
                    menu.show();
                }

                return true;
            }
        });
    }

    @Override
    public void onClickQueryButton(View v) {
        // 재원환자조회
        getPresavedConsendFormList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 액티비티가 정상적으로 종료되었을 경우
        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_RE_SAVE){
                // 동의서 수정
                getPresavedConsendFormList();
            } else if (requestCode == REQ_DEPT) {
                // 진료과선택
                mConditionDeptCode = data.getStringExtra("code");
                mConditionDeptCodeName = data.getStringExtra("codenm");
                mDeptButton.setText(mConditionDeptCodeName);
                //EmrSettingsUtil.setDeptCode(getBaseContext(), mConditionDeptCode, mConditionDeptCodeName);
                // 2026.02.12 WOOIL - 과가 변경되면 기존에 선택되었던 의사를 초기화한다.
                mConditionPdridCode = "";
                mConditionPdridCodeName = "모든의사";
                mPdridButton.setText(mConditionPdridCodeName);
                //EmrSettingsUtil.setPdridCode(getBaseContext(), mConditionPdridCode, mConditionPdridCodeName);
                getPresavedConsendFormList();
            } else if (requestCode == REQ_DOCT) {
                // 의사선택
                mConditionPdridCode = data.getStringExtra("code");
                mConditionPdridCodeName = data.getStringExtra("codenm");
                mPdridButton.setText(mConditionPdridCodeName);
                //EmrSettingsUtil.setPdridCode(getBaseContext(), mConditionPdridCode, mConditionPdridCodeName);
                getPresavedConsendFormList();
            }
        }
    }

    private void getPresavedConsendFormList() {
        //mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        showProgressDialog(getString(R.string.query_wait_message));
        new Thread(new Runnable() {
            public void run() {
                HashMap<String, String> param = new HashMap<String, String>();
                param.clear();
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String searchText = mSearchText.getText().toString();

                mXml = "";
                param.put("mode", "14");
                param.put("hospitalid", hospitalId);
                param.put("userid", userId);
                param.put("searchtext", getHangul(searchText));
                param.put("sortorder", mSortOrder);
                param.put("dept", mConditionDeptCode); // 2026.02.20 WOOIL - 진료과
                param.put("pdrid", mConditionPdridCode); // 2026.02.20 WOOIL - 의사
                mXml = getXml("CertificatePaperServlet", param);

                // 종료
                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterGetPresavedConsendFormList(true);
                            mDialog.dismiss();
                        } catch (Exception ex) {
                        }
                    }
                });
            }
        }).start();

    }

    private void afterGetPresavedConsendFormList(boolean inQuery) {
        ResultSetHelper rs;

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();

        // xml해부
        try {
            // 오류발생
            if (inQuery) {
                if (super.getXmlError() == true) {
                    super.showToastText(super.getXmlErrorMessage());
                    return;
                }
            }
            // 리스트 지움.
            mConsentFormList.setAdapter(null);
            // 조회결과값
            if (mXml.equals("")) return;
            int read_count = 0;
            // xml to ResultSet
            rs = new ResultSetHelper(mXml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                String bkExdt = ""; // 2025.09.05 WOOIL - 일자가 변경되는지 검사
                String bkCcfnm = ""; // 2025.10.17 WOOIL - 동의서가 변경되는지 검사
                for (int i = 0; i < rs.getRecordCount(); i++) {
                    String strSubPageNo = rs.getString(i, "sub_page_no");
                    String strSubPageList = rs.getString(i, "sub_page_list");

                    if ("".equals(strSubPageNo)) {
                        read_count++;

                        // 2025.09.05 WOOIL - 진료일자+환자명 순으로 정렬된 경우만
                        if ("1".equals(mSortOrder)) {
                            String exdt = rs.getString(i, "exdt");
                            if (bkExdt.equals(exdt) == false) {
                                HashMap<String, Object> map = new HashMap<String, Object>();
                                map.put("exdt", exdt);
                                map.put("isdateline", "1");
                                mylist.add(map);
                                bkExdt = exdt;
                            }
                        }
                        // 2025.10.17 WOOIL - 동의서명+환자명 순으로 정렬된 경우만
                        if ("2".equals(mSortOrder)) {
                            String ccfnm = rs.getString(i, "ccf_name");
                            if (bkCcfnm.equals(ccfnm) == false) {
                                HashMap<String, Object> map = new HashMap<String, Object>();
                                map.put("exdt", ccfnm);
                                map.put("isdateline", "1");
                                mylist.add(map);
                                bkCcfnm = ccfnm;
                            }
                        }

                        String psex = rs.getString(i, "psex");
                        String bthdt = rs.getString(i, "bthdt");
                        int ageY = DateUtil.getAgeYear(bthdt);

                        String ward = rs.getString(i, "ward");

                        HashMap<String, Object> map = new HashMap<String, Object>();
                        map.put("image", psex.equals("M") ? R.drawable.man_icon : R.drawable.woman_icon);
                        map.put("pnm", rs.getString(i, "pnm"));
                        map.put("psexage", rs.getString(i, "psex") + "/" + ageY);
                        map.put("dptcd", rs.getString(i, "dptcd"));
                        map.put("drid", rs.getString(i, "drid")); // 2024.06.21 WOOIL - 의사ID
                        map.put("ward", ward);
                        map.put("pdrnm", rs.getString(i, "pdrnm")); // 2024.06.24 WOOIL - dptnm -> pdrnm
                        map.put("pid", rs.getString(i, "pid"));
                        map.put("qfycd", rs.getString(i, "qfycd"));
                        map.put("qfycdnm", rs.getString(i, "qfycdnm"));
                        map.put("ccf_name", rs.getString(i, "ccf_name"));
                        //
                        String bededt = rs.getString(i, "bededt");
                        String bedodt = rs.getString(i, "bedodt"); // 퇴원일(외래는 진료일)
                        map.put("bededt", bededt);
                        map.put("bedodt", bedodt);
                        //
                        String disp_bededt = super.getFormattedDate(bededt);
                        String disp_bedodt = super.getFormattedDate(bedodt);
                        String disp_bededt_bedodt = disp_bededt;
                        if("외래".equalsIgnoreCase(ward)){
                            // 2024.06.24 WOOIL - 왜래는 진료일자만
                        }else {
                            if (bedodt.equals("")) disp_bedodt = "재원중";
                            disp_bededt_bedodt += "~" + disp_bedodt;
                        }
                        map.put("disp_bededt_bedodt", disp_bededt_bedodt);
                        //
                        map.put("ccf_id", rs.getString(i, "ccf_id"));
                        map.put("pre_saved", "Y");
                        map.put("ccf_filename", rs.getString(i, "ccf_filename"));
                        map.put("exdt", rs.getString(i, "exdt"));
                        map.put("seq", rs.getString(i, "seq"));
                        map.put("emr_scan_class", rs.getString(i, "emr_scan_class"));
                        map.put("ccf_exdt_seq", super.getFormattedDate(rs.getString(i, "exdt")) + "." + rs.getString(i, "seq"));
                        //
                        map.put("sub_page_no", strSubPageNo);
                        map.put("sub_page_list", strSubPageList);
                        map.put("pre_saved_bdiv", rs.getString(i, "pre_saved_bdiv"));
                        map.put("isdateline", "");
                        //
                        mylist.add(map);
                    }
                }
                PresavedConsentFormListAdapter adapter = new PresavedConsentFormListAdapter(this, mylist);
                mConsentFormList.setAdapter(adapter);

                /*
                SimpleAdapter adapter = new SimpleAdapter(this, mylist, R.layout.presaved_consent_form_list_row,
                        new String[]{"image", "pnm", "psexage", "dptcd", "ward", "pdrnm", "qfycdnm", "disp_bededt_bedodt", "ccf_name", "pid", "ccf_exdt_seq"},
                        new int[]{R.id.pscf_image
                                , R.id.pscf_pnm
                                , R.id.pscf_psexage
                                , R.id.pscf_dptcd
                                , R.id.pscf_ward
                                , R.id.pscf_pdrnm
                                , R.id.pscf_qfycdnm
                                , R.id.pscf_disp_bededt_bedodt
                                , R.id.pscf_ccf_name
                                , R.id.pscf_pid
                                , R.id.pscf_exdt_seq
                        });
                mConsentFormList.setAdapter(adapter);
                */
            }
            // 2022.03.23 WOOIL - 검색용 키보드가 기본으로 올라오지 않게
            hideKeyboard();

            Toast.makeText(this, read_count + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
            // 2022.03.23 WOOIL - 검색용 키보드가 기본으로 올라오지 않게
            hideKeyboard();
            showSimpleDialog(ex.getMessage());
        }

    }

    private void deletePreSaved(final String pid, final String preSavedBdiv, final String exdt, final String seq, final String subPageList) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("확인");
        dialog.setMessage("삭제하시겠습니까?");
        dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                actionDeletePreSaved(pid, preSavedBdiv, exdt, seq, subPageList);
            }
        });
        dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void actionDeletePreSaved(final String pid, final String preSavedBdiv, final String exdt, final String seq, final String subPageList) {
        //mDialog = ProgressDialog.show(PresavedConsentFormList.this, "", getString(R.string.query_wait_message), true);
        showProgressDialog(getString(R.string.query_wait_message));
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = getHospitalId();
                String userId = getUserId();
                String url = "";
                String mode = "13";

                // 임시저장 이미지 삭제(실제로는 테이블에 플래그를 넣는다).
                url = "ChartServlet?hospitalid=" + hospitalId +
                        "&userid=" + userId +
                        "&pid=" + pid +
                        "&pre_saved_bdiv=" + preSavedBdiv +
                        "&exdt=" + exdt +
                        "&seq=" + seq +
                        "&sub_page_list=" + subPageList +
                        "&mode=" + mode;
                final String xml = getXml(url);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            afterActionDeletePreSaved(xml);
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
    }

    private void afterActionDeletePreSaved(String xml) {
        if (xml.equalsIgnoreCase("y")) getPresavedConsendFormList(); // 성공. 다시 조회.
    }

    // 검색어 입력창의 키보드 내리기
    private void hideKeyboard() {
        EditText text = (EditText) findViewById(R.id.search_text);
        text.setVisibility(View.VISIBLE);
        InputMethodManager inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(text.getWindowToken(), 0);
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        // TODO Auto-generated method stub
        // 2022.03.23 WOOIL - 검색용 키보드가 기본으로 올라오지 않게
        hideKeyboard();
    }

    private class SortOrderDialog extends Dialog {
        // 이 다이얼로그의 타이틀은 trp_color_dialog.xml에 있음.
        RadioGroup radioGroup;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(R.layout.presaved_consent_form_list_sort_dialog);

            // 제목
            TextView tv = (TextView) findViewById(R.id.custom_dialog_title_bar_text);
            tv.setText("정렬순서");

            radioGroup = (RadioGroup) findViewById(R.id.rg_sort);
            initRadioButton();
            setListener();
        }

        public SortOrderDialog(Context context) {
            super(context);
        }

        private void initRadioButton() {
            RadioButton radio;
            if (mSortOrder.equals("0")) {
                radio = (RadioButton) findViewById(R.id.rb_sort_pnm_exdt);
                radio.setChecked(true);
            } else if (mSortOrder.equals("1")) {
                radio = (RadioButton) findViewById(R.id.rb_sort_exdt_pnm);
                radio.setChecked(true);
            } else if (mSortOrder.equals("2")) {
                radio = (RadioButton) findViewById(R.id.rb_sort_form_pnm);
                radio.setChecked(true);
            }
        }

        private void setListener() {
            final Button applyButton = (Button) findViewById(R.id.apply_button);
            applyButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View view) {
                    int checkedId = radioGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.rb_sort_pnm_exdt) {
                        mSortOrder = "0";
                        EmrSettingsUtil.setPresavedConsentFormListSortOrder(getBaseContext(), mSortOrder);
                    } else if (checkedId == R.id.rb_sort_exdt_pnm) {
                        mSortOrder = "1";
                        EmrSettingsUtil.setPresavedConsentFormListSortOrder(getBaseContext(), mSortOrder);
                    } else if (checkedId == R.id.rb_sort_form_pnm) {
                        mSortOrder = "2";
                        EmrSettingsUtil.setPresavedConsentFormListSortOrder(getBaseContext(), mSortOrder);
                    }
                    getPresavedConsendFormList();
                    dismiss();
                }
            });
            final Button cancelButton = (Button) findViewById(R.id.cancel_button);
            cancelButton.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View view) {
                    dismiss();
                }
            });
        }
    }

    private void callConsentForm(final HashMap<String, Object> map) {
        final String isdateline = (String) map.get("isdateline"); // 2025.09.08 WOOIL -

        if("1".equals(isdateline)) return;
        getCertificateDelCheck(map);
    }

    private void getCertificateDelCheck(final HashMap<String, Object> map) {
        //mDialog = ProgressDialog.show(PresavedConsentFormList.this, "", getString(R.string.query_wait_message), true);
        showProgressDialog("동의서가 삭제되었거나 이동되었는지 점검 중입니다.");
        new Thread(new Runnable() {
            public void run() {
                final String hospitalId = getHospitalId();
                final String userId = getUserId();
                final String pid = (String) map.get("pid");
                final String exdt = (String) map.get("exdt");
                final String seq = (String) map.get("seq");
                final String preSavedBdiv = (String) map.get("pre_saved_bdiv");

                String url = "";
                String mode = "22";

                // 임시저장 이미지 삭제(실제로는 테이블에 플래그를 넣는다).
                url = "CertificatePaperServlet" +
                      "?hospitalid=" + hospitalId +
                      "&userid=" + userId +
                      "&pid=" + pid +
                      "&bdiv=" + preSavedBdiv +
                      "&exdt=" + exdt +
                      "&seq=" + seq +
                      "&presaved_yn=Y" +
                      "&mode=" + mode;
                final String xml = getXml(url);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            mDialog.dismiss();
                            afterGetCertificateDelCheck(map, xml);
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
    }

    private void afterGetCertificateDelCheck(final HashMap<String, Object> map, final String xml) {
        ResultSetHelper rs;

        // xml해부
        try {
            // 조회결과값
            if (xml.equals("")) return;

            rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                showSimpleDialog(R.string.no_data_message);
            } else {
                String delfg = rs.getString(0, "delfg");
                if ("".equalsIgnoreCase(delfg) == false) {
                    showSimpleDialog("삭제되거나 이동된 동의서입니다.");
                } else {
                    final String pid = (String) map.get("pid");
                    final String bededt = (String) map.get("bededt");
                    final String ccfId = (String) map.get("ccf_id");
                    final String ccfName = (String) map.get("ccf_name");
                    final String preSaved = (String) map.get("pre_saved");
                    final String ccfFileName = (String) map.get("ccf_filename");
                    final String exdt = (String) map.get("exdt");
                    final String seq = (String) map.get("seq");
                    final String emrScanClass = (String) map.get("emr_scan_class");
                    final String subPageList = (String) map.get("sub_page_list");
                    final String preSavedBdiv = (String) map.get("pre_saved_bdiv");
                    final String bedodt = (String) map.get("bedodt");
                    final String dptcd = (String) map.get("dptcd"); // 2024.06.21 WOOIL -
                    final String drid = (String) map.get("drid"); // 2024.06.21 WOOIL -
                    final String qfycd = (String) map.get("qfycd"); // 2024.06.24 WOOIL -
                    final String isdateline = (String) map.get("isdateline"); // 2025.09.08 WOOIL -
                    final String hxType = "";

                    Intent intent = new Intent(PresavedConsentFormList.this, ConsentForm.class);
                    intent.putExtra("ccfId", ccfId);
                    intent.putExtra("ccfName", ccfName);
                    intent.putExtra("preSaved", preSaved);
                    intent.putExtra("ccfFileName", ccfFileName);
                    intent.putExtra("pid", pid);
                    intent.putExtra("bededt", bededt);
                    intent.putExtra("bdiv", preSavedBdiv); // 2021.07.23 WOOIL -
                    intent.putExtra("dptcd", dptcd); // 2021.07.23 WOOIL -
                    intent.putExtra("drid", drid); // 2024.06.21 WOOIL -
                    intent.putExtra("qfycd", qfycd); // 2024.06.21 WOOIL -
                    intent.putExtra("bedodt", bedodt); // 2024.03.14 WOOIL - 퇴원일(외래는 진료일시)
                    intent.putExtra("exdt", exdt);
                    intent.putExtra("seq", seq);
                    intent.putExtra("emrScanClass", emrScanClass);
                    intent.putExtra("subPageList", subPageList); // 2021.07.23 WOOIL -
                    intent.putExtra("preSavedBdiv", preSavedBdiv);
                    intent.putExtra("hx_type", hxType);
                    startActivityForResult(intent, REQ_RE_SAVE);

                }
            }
        } catch (Exception ex) {
            showSimpleDialog(ex.getMessage());
        }
    }

    private void showProgressDialog(final String msg) {
        try {
            // 기존 dialog가 있으면 제거
            dismissProgressDialog();

            mDialog = new ProgressDialog(this, R.style.CustomProgressDialog);
            mDialog.setMessage(msg);
            mDialog.setCancelable(false);
            mDialog.show();

        } catch (Exception e) {
            // Activity 종료 상태 등 예외 방지
            e.printStackTrace();
        }
    }

    private void dismissProgressDialog() {
        try {
            if (mDialog != null && mDialog.isShowing()) {
                mDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mDialog = null;
        }
    }
}
