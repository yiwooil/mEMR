package com.metrosoft.smart.emr.emrdroid.gt101.z_notused;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.ResultRadAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;

public class ResultRad extends MyActivity {
	static final int FR_DATE_DIALOG_ID = 0;
	static final int TO_DATE_DIALOG_ID = 1;

	private String mPid;
	private String mBededt;
	private String mXmlPatientInfo,mXmlOrderRad,mXmlRadResult;

	private int mFrYear,mFrMonth,mFrDay;
	private int mToYear,mToMonth,mToDay;
	private int mSelectedPostion;
	private HashMap<String,Object> mSelectedMap;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, R.layout.result_rad, "< " + getString(R.string.inpatient_list));

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
		ExpandableListView listView = (ExpandableListView)findViewById(R.id.result_rad_list_order);
		listView.setGroupIndicator(null);
		listView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				// TODO Auto-generated method stub
				ResultRadAdapter adapter=(ResultRadAdapter)parent.getExpandableListAdapter();
				mSelectedPostion=groupPosition;
				mSelectedMap = (HashMap<String,Object>)(adapter.getGroup(groupPosition));
				if(parent.isGroupExpanded(groupPosition)==false){
					getRadResult();
				}
				return false;
			}
		});

		// 파라메터 셋팅
		Intent intent = getIntent();
		mPid = intent.getStringExtra("pid");
		mBededt = intent.getStringExtra("bededt");
		// 기본값셋팅. 오류방지용
		if (mPid==null) mPid="";
		if (mBededt==null) mBededt="";

		if (savedInstanceState==null) {
			// 조회기간 초기화
			initFrToDate();
			displayFrDate();
			displayToDate();
			// 조회
			getOrderRad();
		}
		else {
			mXmlPatientInfo=savedInstanceState.getString("xmlPatientInfo");
			mXmlOrderRad=savedInstanceState.getString("xmlOrderRad");
			mFrYear=savedInstanceState.getInt("frYear");
			mFrMonth=savedInstanceState.getInt("frMonth");
			mFrDay=savedInstanceState.getInt("frDay");
			mToYear=savedInstanceState.getInt("toYear");
			mToMonth=savedInstanceState.getInt("toMonth");
			mToDay=savedInstanceState.getInt("toDay");
			displayFrDate();
			displayToDate();
			// 화면에 다시 출력
			afterGetOrderRad();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("xmlPatientInfo", mXmlPatientInfo);
		outState.putString("xmlOrderRad", mXmlOrderRad);
		outState.putInt("frYear", mFrYear);
		outState.putInt("frMonth", mFrMonth);
		outState.putInt("frDay", mFrDay);
		outState.putInt("toYear", mToYear);
		outState.putInt("toMonth", mToMonth);
		outState.putInt("toDay", mToDay);
	}

	@Override
	public void onClickQueryButton(View v) {
		getOrderRad();
	}

	private void getOrderRad() {
		//
		if (mPid.equals("") || mBededt.equals("")) return;

		mXmlOrderRad="";
		mDialog = ProgressDialog.show(ResultRad.this, "",getString(R.string.query_wait_message), true);
		new Thread(new Runnable() {
			public void run() {
				String hospitalId=getHospitalId();
				String userId=getUserId();
				String url="";
				String frDate=getFrDate();
				String toDate=getToDate();
				// 환자정보
				url = "InPatientInformationServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt;
				mXmlPatientInfo = getXml(url);
				// 처방
				url = "ChartServlet?hospitalid=" + hospitalId +
						"&pid=" + mPid +
						"&bededt=" + mBededt +
						"&frdt=" + frDate +
						"&todt=" + toDate +
						"&odivcd=R";
				mXmlOrderRad = getXml(url);
				mHandler.post(new Runnable() {
					public void run() {
						// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
						// 이를 방지함.
						try {
							mDialog.dismiss();
							afterGetOrderRad();
						}catch(Exception e) {
							Log.d("EmrDroid","dialog.dismiss exception");
						}
					}
				});
			}
		}).start();
	}

	private void afterGetOrderRad() {

		((TextView)findViewById(R.id.patientInfoTextView)).setText(mXmlPatientInfo);

		ExpandableListView list=(ExpandableListView)findViewById(R.id.result_rad_list_order);

		ArrayList<HashMap<String,Object>> mylist = new ArrayList<HashMap<String,Object>>();
		HashMap<String,Object> map = null;

		ArrayList<HashMap<String,Object>> mylist2 = new ArrayList<HashMap<String,Object>>(); // 결과용
		HashMap<String,Object> map2 = null; // 결과용

		ResultSetHelper rs;

		// xml해부
		try {
			// 오류발생
			if(super.getXmlError()==true) {
				super.showToastText(super.getXmlErrorMessage());
				return;
			}
			rs = new ResultSetHelper(mXmlOrderRad,EmrSettingsUtil.getMaskYn(getBaseContext()));

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
					String odt=rs.getString(i,"odt");
					String orddate=odt.substring(0,4)+"/"+odt.substring(4,6)+"/"+odt.substring(6,8);
					map.put("orddate", orddate);
					map.put("odt", rs.getString(i,"odt"));
					map.put("ono", rs.getString(i,"ono"));
					map.put("onm", rs.getString(i,"onm"));

					mylist.add(map);

					// 결과용
					map2 = new HashMap<String,Object>();
					map2.put("show", "");
					map2.put("result", "");

					mylist2.add(map2);
				}
//				SimpleAdapter adapter = new SimpleAdapter(this, mylist, R.layout.result_rad_row_order,
//		                new String[] {"orddate","ono","onm"},
//		                new int[] {R.id.orddate,R.id.ono,R.id.onm});
//				list.setAdapter(adapter);
				ResultRadAdapter adapter = new ResultRadAdapter(this,mylist,mylist2);
				list.setAdapter(adapter);
			}
		}
		catch(Exception ex) {
			showSimpleDialog(ex.getMessage());
		}
	}

	private void getRadResult() {
		mXmlRadResult="";
		mDialog = ProgressDialog.show(ResultRad.this, "",getString(R.string.query_wait_message), true);
		new Thread(new Runnable() {
			public void run() {
				String url="";
				String hospitalId=getHospitalId();
				String userId=getUserId();
				String odt=mSelectedMap.get("odt").toString();
				String ono=mSelectedMap.get("ono").toString();
				// 결과
				url = "ResultRadServlet?hospitalid=" + hospitalId +
						"&pid=" + mPid +
						"&bededt=" + mBededt +
						"&odt=" + odt +
						"&ono=" + ono +
						"";
				mXmlRadResult = getXml(url);
				mHandler.post(new Runnable() {
					public void run() {
						// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
						// 이를 방지함.
						try {
							mDialog.dismiss();
							afterGetRadResult();
						}catch(Exception e) {
							Log.d("EmrDroid","dialog.dismiss exception");
						}
					}
				});
			}
		}).start();
	}

	private void afterGetRadResult() {
		ResultSetHelper rs;

		// xml해부
		try {
			// 오류발생
			if(super.getXmlError()==true) {
				super.showToastText(super.getXmlErrorMessage());
				return;
			}

			rs = new ResultSetHelper(mXmlRadResult,EmrSettingsUtil.getMaskYn(getBaseContext()));

			if (rs.getReturnCode()<0) {
				showSimpleDialog(rs.getReturnDesc());
			}
			else if (rs.getReturnCode()==0) {
				showSimpleDialog(R.string.no_data_message);
			}
			else {
				String resultText="";
				resultText="접수일자 : " + getFormattedDate(rs.getString(0, "acptdt")) + " " +
						"촬영일자 : " + getFormattedDate(rs.getString(0, "phtdt")) + " " +
						"판독일자 : " + getFormattedDate(rs.getString(0, "rptdt")) + "\n\n";
				resultText+=rs.getString(0, "rptxt");

				ExpandableListView list=(ExpandableListView)findViewById(R.id.result_rad_list_order);
				ResultRadAdapter adapter=(ResultRadAdapter)list.getExpandableListAdapter();
				adapter.setRadResult(mSelectedPostion, "1", resultText);
			}
		}
		catch(Exception ex) {
			showSimpleDialog(ex.getMessage());
		}
	}

	private void initFrToDate() {
		Calendar c = Calendar.getInstance();
		if (mBededt.equals("")) {
			mFrYear = c.get(Calendar.YEAR);
			mFrMonth = c.get(Calendar.MONTH);
			mFrDay = c.get(Calendar.DAY_OF_MONTH);
		}
		else {
			mFrYear=Integer.parseInt(mBededt.substring(0, 4));
			mFrMonth=Integer.parseInt(mBededt.substring(4, 6)) - 1;
			mFrDay=Integer.parseInt(mBededt.substring(6, 8));
		}

		mToYear = c.get(Calendar.YEAR);
		mToMonth = c.get(Calendar.MONTH);
		mToDay = c.get(Calendar.DAY_OF_MONTH);


	}

	private void displayFrDate() {
		((Button)findViewById(R.id.pickFrDate)).setText(
				new StringBuilder()
						// Month is 0 based so add 1
						.append(mFrYear).append(".")
						.append(mFrMonth + 1).append(".")
						.append(mFrDay).append(" ")
		);
	}

	private void displayToDate() {
		((Button)findViewById(R.id.pickToDate)).setText(
				new StringBuilder()
						// Month is 0 based so add 1
						.append(mToYear).append(".")
						.append(mToMonth + 1).append(".")
						.append(mToDay).append(" ")
		);
	}

	private String getFrDate() {
		String yearString = Integer.toString(mFrYear);
		String monthString = Integer.toString(mFrMonth+101);
		String dayString = Integer.toString(mFrDay+100);
		String ret = yearString + monthString.substring(1, 3) + dayString.substring(1, 3);

		return ret;
	}

	private String getToDate() {
		String yearString = Integer.toString(mToYear);
		String monthString = Integer.toString(mToMonth+101);
		String dayString = Integer.toString(mToDay+100);
		String ret = yearString + monthString.substring(1, 3) + dayString.substring(1, 3);

		return ret;
	}

//    protected class ResultRadAdapter extends BaseExpandableListAdapter {
//    	private Context context;
//    	private ArrayList<HashMap<String,Object>> arrayList;
//    	private ArrayList<HashMap<String,Object>> resultList;
//
//    	public ResultRadAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList,ArrayList<HashMap<String,Object>> resultList) {
//    		this.context = context;
//    		this.arrayList = arrayList;
//    		this.resultList = resultList;
//    	}
//    	// 판독결과를 저장한다.
//    	public void setRadResult(int position, String show, String result) {
//			HashMap<String,Object> map = null;
//			map = new HashMap<String,Object>();
//			map.put("show", show);
//			map.put("result", result);
//			this.resultList.set(position, map);
//
//			notifyDataSetChanged();
//    	}
//		@Override
//		public Object getChild(int groupPosition, int childPosition) {
//			// TODO Auto-generated method stub
//    		HashMap<String,Object> map = this.resultList.get(groupPosition);
//    		return map;
//		}
//		@Override
//		public long getChildId(int groupPosition, int childPosition) {
//			// TODO Auto-generated method stub
//			return childPosition;
//		}
//		@Override
//		public int getChildrenCount(int groupPosition) {
//			// TODO Auto-generated method stub
//			return 1;
//		}
//		@Override
//		public Object getGroup(int groupPosition) {
//			// TODO Auto-generated method stub
//    		HashMap<String,Object> map = this.arrayList.get(groupPosition);
//    		return map;
//		}
//		@Override
//		public int getGroupCount() {
//			// TODO Auto-generated method stub
//			return this.arrayList.size();
//		}
//		@Override
//		public long getGroupId(int groupPosition) {
//			// TODO Auto-generated method stub
//			return groupPosition;
//		}
//		@Override
//		public View getGroupView(int groupPosition, boolean isExpanded,	View convertView, ViewGroup parent) {
//			// TODO Auto-generated method stub
//    		View row = convertView;
//    		HashMap<String,Object> map = this.arrayList.get(groupPosition);
//
//    		if (row==null) {
//    			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
//    			row = inflater.inflate(R.layout.result_rad_row_order, null);
//    		}
//
//    		String orddate = (String)map.get("orddate");
//    		String ono = (String)map.get("ono");
//    		String onm = (String)map.get("onm");
//
//    		TextView textView;
//    		// 처방일자
//    		textView = (TextView)row.findViewById(R.id.orddate);
//    		textView.setText(orddate);
//    		// 처방번호
//    		textView = (TextView)row.findViewById(R.id.ono);
//    		textView.setText(ono);
//    		// 처방명
//    		textView = (TextView)row.findViewById(R.id.onm);
//    		textView.setText(onm);
//
//    		return row;
//		}
//		@Override
//		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
//			// TODO Auto-generated method stub
//    		View row = convertView;
//    		HashMap<String,Object> map = this.resultList.get(groupPosition);
//
//    		if (row==null) {
//    			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
//    			row = inflater.inflate(R.layout.result_rad_row_order_result, null);
//    		}
//
//    		String result=(String)map.get("result");
//
//    		TextView textView;
//    		// 결과
//    		textView = (TextView)row.findViewById(R.id.result_rad_text);
//    		textView.setText(result);
//    		
//    		return row;
//		}
//		@Override
//		public boolean hasStableIds() {
//			// TODO Auto-generated method stub
//			return false;
//		}
//		@Override
//		public boolean isChildSelectable(int groupPosition, int childPosition) {
//			// TODO Auto-generated method stub
//			return false;
//		}    	
//    }
}
