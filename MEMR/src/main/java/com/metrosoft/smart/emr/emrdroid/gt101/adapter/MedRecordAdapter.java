package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.json.JSONException;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MedRecordAdapter extends BaseAdapter {
	private Context context;
	private ArrayList<HashMap<String,Object>> arrayList;
	private String minDodt;
	private String maxDodt;
	private long leftPosition;

	public MedRecordAdapter(Context context,  ResultSetHelper rs, String minDodt, String maxDodt) throws JSONException, ParseException {
		this.context = context;
		this.arrayList = new ArrayList<HashMap<String,Object>>();
		this.minDodt = minDodt;
		this.maxDodt = maxDodt;
		this.leftPosition = 0; // 맨 왼쪽에 올 일자 인덱스. monDodt로 부터의 차이이다. 시작은 0, 즉 minDate와 동일함.
		
		setArrayList(rs);
	}
	
	public void setLeftPosition(long leftPosition){
		this.leftPosition = leftPosition;
		notifyDataSetChanged(); 
	}
	
	private void setArrayList(ResultSetHelper rs) throws JSONException, ParseException{
		// this.arrayList안에 map이 있다.
		ArrayList<HashMap<String,Object>> array = null;
		HashMap<String,Object> map = null; // 상세
		HashMap<String,Object> map2 = null; // 코드+단위
		boolean find=false;

		for (int i=0 ; i<rs.getRecordCount() ; i++) {
			String ocd=rs.getString(i,"ocd");
			String onm=rs.getString(i,"onm");
			String dunit=rs.getString(i,"dunit");
			String dodt=rs.getString(i,"dodt");
			String dqty=rs.getString(i,"dqty");

			map = new HashMap<String,Object>();
			map.put("ocd", ocd);
			map.put("onm", onm);
			map.put("dunit", dunit);
			map.put("dodt", dodt);
			map.put("dqty", dqty);
			map.put("datepos", getDatePos(rs.getString(i,"dodt")));

			find=false;
			String key=ocd + "," + onm + "," + dunit;
			String key2="";
			for(int j=0;j<this.arrayList.size();j++){
				map2 = this.arrayList.get(j);
				key2=(String)map2.get("keyvalue");
				if(key.equals(key2)==true){
					array = (ArrayList<HashMap<String,Object>>)map2.get("datavalue");
					array.add(map);
					// 기존내역에 있음. 기존내역에 추가
					find=true;
					break;
				}
			}
			// 기존 내역에 없음. 새로이 추가한다.
			if(find==false){
				array = new ArrayList<HashMap<String,Object>>();
				array.add(map);
				map2 = new HashMap<String,Object>();
				map2.put("keyvalue",key);
				map2.put("datavalue",array);
				this.arrayList.add(map2);
			}
		}
	}
	
	private long getDatePos(String dodt) throws ParseException{
		SimpleDateFormat sdf;
		Date minDate;
		Date doDate;
		long diff;

		if ("".equalsIgnoreCase(minDodt)) return 0;

		sdf = new SimpleDateFormat("yyyyMMdd",Locale.KOREA);
		sdf.setLenient(false);
		minDate = sdf.parse(minDodt);
		doDate = sdf.parse(dodt);
		diff = doDate.getDate() - minDate.getDate();
		
		return diff;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return this.arrayList.size();
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return this.arrayList.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View row = convertView;
		if(row==null){
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(R.layout.med_record_row, null);
		}
		HashMap<String,Object> map = this.arrayList.get(position);
		//String key = (String)map.get("keyvalue");
		ArrayList<HashMap<String,Object>> array = (ArrayList<HashMap<String,Object>>)map.get("datavalue");

		// 실제 자료
		HashMap<String,Object> dataMap;
		dataMap = array.get(0);
		
		String ocd = (String)dataMap.get("ocd");
		String onm = (String)dataMap.get("onm");
		String dunit = (String)dataMap.get("dunit");
		
		TextView textView;
		//
		textView = (TextView)row.findViewById(R.id.ocd_row);
		textView.setText(ocd);
		//
		textView = (TextView)row.findViewById(R.id.onm_row);
		textView.setText(onm);
		//
		textView = (TextView)row.findViewById(R.id.dunit_row);
		textView.setText(dunit);
		// 값 초기화
		textView = (TextView)row.findViewById(R.id.ordcnt0_row); textView.setText(" ");
		textView = (TextView)row.findViewById(R.id.ordcnt1_row); textView.setText(" ");
		textView = (TextView)row.findViewById(R.id.ordcnt2_row); textView.setText(" ");
		textView = (TextView)row.findViewById(R.id.ordcnt3_row); textView.setText(" ");
		textView = (TextView)row.findViewById(R.id.ordcnt4_row); textView.setText(" ");
		textView = (TextView)row.findViewById(R.id.ordcnt5_row); textView.setText(" ");
		textView = (TextView)row.findViewById(R.id.ordcnt6_row); textView.setText(" ");
		textView = (TextView)row.findViewById(R.id.ordcnt7_row); textView.setText(" ");
		textView = (TextView)row.findViewById(R.id.ordcnt8_row); textView.setText(" ");
		/*
		textView = (TextView)row.findViewById(R.id.ordcnt9_row); textView.setText(" ");
		textView = (TextView)row.findViewById(R.id.ordcnt10_row); textView.setText(" ");
		textView = (TextView)row.findViewById(R.id.ordcnt11_row); textView.setText(" ");
		*/
		
		for(int i=0;i<array.size();i++){
			dataMap = array.get(i);
			
			long datepos = (Long)dataMap.get("datepos");
			String dqty = (String)dataMap.get("dqty");
			
			datepos = datepos - this.leftPosition;
			textView = null;
			if(datepos==0) textView = (TextView)row.findViewById(R.id.ordcnt0_row);
			else if(datepos==1) textView = (TextView)row.findViewById(R.id.ordcnt1_row);
			else if(datepos==2) textView = (TextView)row.findViewById(R.id.ordcnt2_row);
			else if(datepos==3) textView = (TextView)row.findViewById(R.id.ordcnt3_row);
			else if(datepos==4) textView = (TextView)row.findViewById(R.id.ordcnt4_row);
			else if(datepos==5) textView = (TextView)row.findViewById(R.id.ordcnt5_row);
			else if(datepos==6) textView = (TextView)row.findViewById(R.id.ordcnt6_row);
			else if(datepos==7) textView = (TextView)row.findViewById(R.id.ordcnt7_row);
			else if(datepos==8) textView = (TextView)row.findViewById(R.id.ordcnt8_row);
			/*
			else if(datepos==9) textView = (TextView)row.findViewById(R.id.ordcnt9_row);
			else if(datepos==10) textView = (TextView)row.findViewById(R.id.ordcnt10_row);
			else if(datepos==11) textView = (TextView)row.findViewById(R.id.ordcnt11_row);
			*/
			//
			if(textView!=null) textView.setText(getNumFmt(dqty));
		}
		
		return row;
	}
	// 소숫점 밑에 0을 제거한다.
	private String getNumFmt(String s){
		try{
			Double d = Double.parseDouble(s);
			Long l = d.longValue();
			if(d==(double)l){
				// 소수밑에 없는 경우임.
				return l.toString();
			}else{
				return d.toString();
			}
		}catch(NumberFormatException e){
			return s;
		}
	}

}
