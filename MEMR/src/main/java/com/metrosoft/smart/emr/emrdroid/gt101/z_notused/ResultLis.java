package com.metrosoft.smart.emr.emrdroid.gt101.z_notused;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.ResultLisAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.view.*;

public class ResultLis  extends MyActivity {
	static final int FR_DATE_DIALOG_ID = 0;
	static final int TO_DATE_DIALOG_ID = 1;

	private String pid;
	private String bededt;
	private String xmlPatientInfo,xml;

	private int frYear,frMonth,frDay;
	private int toYear,toMonth,toDay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, R.layout.result_lis, "< " + getString(R.string.inpatient_list));

		// 이벤트연결
		// 일자
		((Button)findViewById(R.id.pickFrDate)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(FR_DATE_DIALOG_ID);
			}
		});
		((Button)findViewById(R.id.pickToDate)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showDialog(TO_DATE_DIALOG_ID);
			}
		});

		// 파라메터 셋팅
		Intent intent = getIntent();
		pid = intent.getStringExtra("pid");
		bededt = intent.getStringExtra("bededt");
		// 기본값셋팅. 오류방지용
		if (pid==null) pid="";
		if (bededt==null) bededt="";

		if (savedInstanceState==null) {
			// 조회기간 초기화
			initFrToDate();
			displayFrDate();
			displayToDate();
			// 조회
			getResultLis();
		}
		else {
			xmlPatientInfo=savedInstanceState.getString("xmlPatientInfo");
			xml=savedInstanceState.getString("xml");
			frYear=savedInstanceState.getInt("frYear");
			frMonth=savedInstanceState.getInt("frMonth");
			frDay=savedInstanceState.getInt("frDay");
			toYear=savedInstanceState.getInt("toYear");
			toMonth=savedInstanceState.getInt("toMonth");
			toDay=savedInstanceState.getInt("toDay");
			displayFrDate();
			displayToDate();
			// 화면에 다시 출력
			afterGetResultLis();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("xmlPatientInfo", xmlPatientInfo);
		outState.putString("xml", xml);
		outState.putInt("frYear", frYear);
		outState.putInt("frMonth", frMonth);
		outState.putInt("frDay", frDay);
		outState.putInt("toYear", toYear);
		outState.putInt("toMonth", toMonth);
		outState.putInt("toDay", toDay);
	}

	@Override
	public void onClickQueryButton(View v) {
		getResultLis();
	}

	private void initFrToDate() {
		Calendar c = Calendar.getInstance();
		if (bededt.equals("")) {
			frYear = c.get(Calendar.YEAR);
			frMonth = c.get(Calendar.MONTH);
			frDay = c.get(Calendar.DAY_OF_MONTH);
		}
		else {
			frYear=Integer.parseInt(bededt.substring(0, 4));
			frMonth=Integer.parseInt(bededt.substring(4, 6)) - 1;
			frDay=Integer.parseInt(bededt.substring(6, 8));
		}
//        if (bededt.equals("")) {
		toYear = c.get(Calendar.YEAR);
		toMonth = c.get(Calendar.MONTH);
		toDay = c.get(Calendar.DAY_OF_MONTH);
//        }
//        else {
//	        toYear=Integer.parseInt(bededt.substring(0, 4));
//	        toMonth=Integer.parseInt(bededt.substring(4, 6)) - 1;
//	        toDay=Integer.parseInt(bededt.substring(6, 8));
//        }


	}

	private void displayFrDate() {
		((Button)findViewById(R.id.pickFrDate)).setText(
				new StringBuilder()
						// Month is 0 based so add 1
						.append(frYear).append(".")
						.append(frMonth + 1).append(".")
						.append(frDay).append(" ")
		);
	}

	private void displayToDate() {
		((Button)findViewById(R.id.pickToDate)).setText(
				new StringBuilder()
						// Month is 0 based so add 1
						.append(toYear).append(".")
						.append(toMonth + 1).append(".")
						.append(toDay).append(" ")
		);
	}

	private String getFrDate() {
		String yearString = Integer.toString(frYear);
		String monthString = Integer.toString(frMonth+101);
		String dayString = Integer.toString(frDay+100);
		String ret = yearString + monthString.substring(1, 3) + dayString.substring(1, 3);

		return ret;
	}

	private String getToDate() {
		String yearString = Integer.toString(toYear);
		String monthString = Integer.toString(toMonth+101);
		String dayString = Integer.toString(toDay+100);
		String ret = yearString + monthString.substring(1, 3) + dayString.substring(1, 3);

		return ret;
	}

	private void getResultLis() {
		//
		if (pid.equals("") || bededt.equals("")) return;

		xml="";
		mDialog = ProgressDialog.show(ResultLis.this, "",getString(R.string.query_wait_message), true);
		new Thread(new Runnable() {
			public void run() {
				String hospitalId=getHospitalId();
				String userId=getUserId();
				String url="";
				String frDate=getFrDate();
				String toDate=getToDate();
				// 환자정보
				url = "InPatientInformationServlet?hospitalid=" + hospitalId + "&pid=" + pid + "&bededt=" + bededt;
				xmlPatientInfo = getXml(url);
				// 처방
				url = "ResultLisServlet?hospitalid=" + hospitalId + "&pid=" + pid + "&bededt=" + bededt + "&frdt=" + frDate + "&todt=" + toDate;
				xml = getXml(url);
				mHandler.post(new Runnable() {
					public void run() {
						// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
						// 이를 방지함.
						try {
							mDialog.dismiss();
							afterGetResultLis();
						}catch(Exception e) {
							Log.d("EmrDroid","dialog.dismiss exception");
						}
					}
				});
			}
		}).start();
	}

	private void afterGetResultLis() {
		((TextView)findViewById(R.id.patientInfoTextView)).setText(xmlPatientInfo);

		ListView list=(ListView)findViewById(R.id.result_lis_list);

		ArrayList<HashMap<String,Object>> mylist = new ArrayList<HashMap<String,Object>>();
		HashMap<String,Object> map = null;

		ResultSetHelper rs;

		// xml해부
		try {
			// 오류발생
			if(super.getXmlError()==true) {
				super.showToastText(super.getXmlErrorMessage());
				return;
			}
			if (xml==null || xml.equals("")) {
				showSimpleDialog("조회된 자료가 없습니다.");
				return;
			}
			rs = new ResultSetHelper(xml,EmrSettingsUtil.getMaskYn(getBaseContext()));

			if (rs.getReturnCode()<0) {
				showSimpleDialog(rs.getReturnDesc());
			}
			else if (rs.getReturnCode()==0) {
				showSimpleDialog(R.string.no_data_message);
			}
			else {
				Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();

				for (int i=0 ; i<rs.getRecordCount() ; i++) {
					map = new HashMap<String,Object>();

					map.put("orddt", rs.getString(i,"orddt"));
					map.put("abbrnm", rs.getString(i,"abbrnm"));
					map.put("rstval", rs.getString(i,"rstval"));
					map.put("beforerstval", rs.getString(i,"beforerstval"));
					map.put("referchk", rs.getString(i,"referchk"));
					map.put("panicchk", rs.getString(i,"panicchk"));
					map.put("deltachk", rs.getString(i,"deltachk"));
					map.put("reference", rs.getString(i,"reference"));
					map.put("unit", rs.getString(i,"unit"));
					map.put("spcnm", rs.getString(i,"spcnm"));
					map.put("majnm", rs.getString(i,"majnm"));

					mylist.add(map);
				}
//				SimpleAdapter adapter = new SimpleAdapter(this, mylist, R.layout.chart_row,
//		                new String[] {"IMAGE","ODT","ONM","OQTY","OUNIT","ORDCNT","ODAYCNT","RMK"},
//		                new int[] {R.id.image_chart_row,R.id.odt_chart_row,R.id.onm_chart_row,R.id.oqty_chart_row,R.id.ounit_chart_row,R.id.ordcnt_chart_row,R.id.odaycnt_chart_row,R.id.rmk_chart_row});
//				list.setAdapter(adapter);
				ResultLisAdapter adapter = new ResultLisAdapter(this,mylist);
				list.setAdapter(adapter);

			}
		}
		catch(Exception ex) {
			showSimpleDialog(ex.getMessage());
		}
	}

	private DatePickerDialog.OnDateSetListener mFrDateSetListener =
			new DatePickerDialog.OnDateSetListener() {
				public void onDateSet(DatePicker view, int year,int monthOfYear, int dayOfMonth) {
					frYear = year;
					frMonth = monthOfYear;
					frDay = dayOfMonth;
					displayFrDate();
				}
			};
	private DatePickerDialog.OnDateSetListener mToDateSetListener =
			new DatePickerDialog.OnDateSetListener() {
				public void onDateSet(DatePicker view, int year,int monthOfYear, int dayOfMonth) {
					toYear = year;
					toMonth = monthOfYear;
					toDay = dayOfMonth;
					displayToDate();
				}
			};

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case FR_DATE_DIALOG_ID:
				return new DatePickerDialog(this,
						mFrDateSetListener,
						frYear, frMonth, frDay);
			case TO_DATE_DIALOG_ID:
				return new DatePickerDialog(this,
						mToDateSetListener,
						toYear, toMonth, toDay);
		}
		return null;
	}

//    protected class ResultLisAdapter extends BaseAdapter {
//    	private Context context;
//    	private ArrayList<HashMap<String,Object>> arrayList;
//
//    	public ResultLisAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList) {
//    		this.context = context;
//    		this.arrayList = arrayList;
//    	}
//    	@Override
//    	public int getCount() {
//    		//return 2;
//    		return this.arrayList.size();
//    	}
//    	@Override
//    	public Object getItem(int position) {
//    		return position;
//    	}
//    	@Override
//    	public long getItemId(int position) {
//    		return position;
//    	}
//    	@Override
//    	public View getView (int position, View convertView, ViewGroup parent) {
//
//    		View row = convertView;
//    		HashMap<String,Object> map = this.arrayList.get(position);
//
//    		LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
//			row = inflater.inflate(R.layout.result_lis_row, null);
//
//    		// orientation = 0 : 세로
//    		//               1 : 가로
//    		//               2 : 세로(뒤집힌 세로)
//    		//               3 : 가로(뒤집힌 가로)
//			int orientation = getWindowManager().getDefaultDisplay().getOrientation();
//
//			String abbrnm=(String)map.get("abbrnm");
//    		String spcnm=(String)map.get("spcnm");
//    		String majnm=(String)map.get("majnm");
//    		String beforerstval=(String)map.get("beforerstval");
//    		String unit=(String)map.get("unit");
//    		String referchk=(String)map.get("referchk");
//    		String panicchk=(String)map.get("panicchk");
//    		String deltachk=(String)map.get("deltachk");
//
//    		referchk=referchk.trim();
//    		panicchk=panicchk.trim();
//    		deltachk=deltachk.trim();
//
//    		boolean abnormal=false;
//    		if (!referchk.equals("") || !panicchk.equals("") || !deltachk.equals("")) {
//    			abnormal=true;
//    			row.setBackgroundColor(Color.LTGRAY);
//    		}
//
//    		TextView textView;
//    		//
//    		textView = (TextView)row.findViewById(R.id.orddt_result_lis_row);
//    		textView.setText((String)map.get("orddt"));
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//    		//
//    		textView = (TextView)row.findViewById(R.id.abbrnm_result_lis_row);
//    		textView.setText(abbrnm);
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//    		//
//    		textView = (TextView)row.findViewById(R.id.rstval_result_lis_row);
//    		textView.setText((String)map.get("rstval"));
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//    		//
//    		textView = (TextView)row.findViewById(R.id.beforerstval_result_lis_row);
//    		textView.setText(beforerstval);
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//    		//
//    		textView = (TextView)row.findViewById(R.id.referchk_result_lis_row);
//    		textView.setText(referchk);
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//    		//
//    		textView = (TextView)row.findViewById(R.id.panicchk_result_lis_row);
//    		textView.setText(panicchk);
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//    		//
//    		textView = (TextView)row.findViewById(R.id.deltachk_result_lis_row);
//    		textView.setText(deltachk);
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//    		//
//    		textView = (TextView)row.findViewById(R.id.reference_result_lis_row);
//    		textView.setText((String)map.get("reference"));
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//    		//
//    		textView = (TextView)row.findViewById(R.id.unit_result_lis_row);
//    		textView.setText(unit);
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//    		//
//    		textView = (TextView)row.findViewById(R.id.spcnm_result_lis_row);
//    		textView.setText(spcnm);
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//    		//
//    		textView = (TextView)row.findViewById(R.id.majnm_result_lis_row);
//    		textView.setText(majnm);
//    		if (abnormal==true) textView.setTextColor(Color.BLACK);
//
//    		// 세로이면
//    		if (orientation==0 || orientation==2) {
//    			if (spcnm.equals("") && majnm.equals("") && beforerstval.equals("") && unit.equals("")) {
//	    			((TextView)row.findViewById(R.id.spcnm_result_lis_row)).setVisibility(View.GONE);
//	    			((TextView)row.findViewById(R.id.majnm_result_lis_row)).setVisibility(View.GONE);
//	    			((TextView)row.findViewById(R.id.beforerstval_result_lis_row)).setVisibility(View.GONE);
//	    			((TextView)row.findViewById(R.id.unit_result_lis_row)).setVisibility(View.GONE);
//	    			// 가짜필드
//	    			((TextView)row.findViewById(R.id.dummy_orddt_result_lis_row)).setVisibility(View.GONE);    			
//	    			((TextView)row.findViewById(R.id.dummy_abbrnm_result_lis_row)).setVisibility(View.GONE);    	
//    			}
//    		}
//    		
//    		return row;
//    	}
//    }

}
