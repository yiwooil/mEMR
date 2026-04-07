package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.DateUtil;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ResultLisSpcnoAdapter extends BaseAdapter {
	private Context context;
	private ArrayList<HashMap<String,Object>> arrayList;
	
	public ResultLisSpcnoAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList) {
		this.context = context;
		this.arrayList = arrayList;
	}

	@Override
	public int getCount() {
		//return 2;
		return this.arrayList.size();
	}
	@Override
	public Object getItem(int position) {
		return this.arrayList.get(position);
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View row = convertView;
		HashMap<String,Object> map = this.arrayList.get(position);
		
		if (row==null) {
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(R.layout.result_lis_row_spcno, null);
		}
		
		String orddt = (String)map.get("orddt");
		String deptcd = (String)map.get("deptcd");
		String ward = (String)map.get("ward");
		String room = (String)map.get("room");
		String ordnm = (String)map.get("ordnm");
		String alltestnm = (String)map.get("alltestnm");
		String spcnm = (String)map.get("spcnm");
		String stsnm = (String)map.get("stsnm");
		String vfydt = (String)map.get("vfydt");
		String vfytm = (String)map.get("vfytm");
		String rcvdt = (String)map.get("rcvdt");
		String rcvtm = (String)map.get("rcvtm");
		String spcno = (String)map.get("spcno");

		TextView textView;
		// 처방일
		textView = (TextView)row.findViewById(R.id.orddt);
		textView.setText(DateUtil.getFormattedDate(orddt));
		// 진료과
		textView = (TextView)row.findViewById(R.id.deptcd);
		textView.setText(deptcd);
		// 병동/병실
		textView = (TextView)row.findViewById(R.id.ward);
		textView.setText(ward+"/"+room);
		// 처방의
		textView = (TextView)row.findViewById(R.id.ordnm);
		textView.setText(ordnm);
		// 검사명
		if(alltestnm.length()>30) alltestnm=alltestnm.substring(0, 30); // 너무 길면 잘라서...
		textView = (TextView)row.findViewById(R.id.alltestnm);
		textView.setText(alltestnm);
		// 검체
		textView = (TextView)row.findViewById(R.id.spcnm);
		textView.setText(spcnm);
		// 상태
		textView = (TextView)row.findViewById(R.id.stsnm);
		textView.setText(stsnm);
		// 검사일시
		textView = (TextView)row.findViewById(R.id.vfydtm);
		textView.setText(DateUtil.getFormattedDate(vfydt)+" "+DateUtil.getFormattedTime(vfytm));
		// 접수일시
		textView = (TextView)row.findViewById(R.id.rcvdtm);
		textView.setText(DateUtil.getFormattedDate(rcvdt)+" "+DateUtil.getFormattedTime(rcvtm));
		// 검체번호
		textView = (TextView)row.findViewById(R.id.spcno);
		textView.setText(spcno);
		
		return row;
	}
}
