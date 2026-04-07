package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ChartAdapter extends BaseAdapter {
	private Context context;
	private ArrayList<HashMap<String,Object>> arrayList;
	
	public ChartAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList) {
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
		return position;
	}
	@Override
	public long getItemId(int position) {
		return position;
	}
	@Override
	public View getView (int position, View convertView, ViewGroup parent) {

		View row = convertView;
		HashMap<String,Object> map = this.arrayList.get(position);
		
		String div = (String)map.get("div");
		String exdt = (String)map.get("exdt");
		String bdiv = (String)map.get("bdiv");
		String c_case = (String)map.get("c_case");
		String rmk1 = (String)map.get("rmk1");
		
		if (!div.equals("1")) {
			// 내용줄을 조금 높이기 위한 작업
			rmk1 += "\r\n";
		}
		// 
		LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
		row = inflater.inflate(R.layout.chart_row, null);
		
		TextView textView;
		
		// 일자 (EXDT)
		textView = (TextView)row.findViewById(R.id.exdt_chart_row);
		textView.setText(exdt);
		textView.setVisibility(View.GONE);
		// 구번 (C_CASE)
		textView = (TextView)row.findViewById(R.id.c_case_chart_row);
		textView.setText(c_case);
		// 내용(RMK1)
		textView = (TextView)row.findViewById(R.id.rmk1_chart_row);
		textView.setText(rmk1);
		
		if (div.equals("1")) {
			//  날자구분선
			row.setBackgroundColor(Color.LTGRAY);
			((TextView)row.findViewById(R.id.rmk1_chart_row)).setTextColor(Color.BLACK);
//			((TextView)row.findViewById(R.id.c_case_chart_row)).setVisibility(View.GONE); // 안보이게 처리한다.
		}
	
		return row;
	}
}
