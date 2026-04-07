package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class ResultRadAdapter  extends BaseExpandableListAdapter {
	private Context context;
	private ArrayList<HashMap<String,Object>> arrayList;
	private ArrayList<HashMap<String,Object>> resultList;
	
	public ResultRadAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList,ArrayList<HashMap<String,Object>> resultList) {
		this.context = context;
		this.arrayList = arrayList;
		this.resultList = resultList;
	}
	// 판독결과를 저장한다.
	public void setRadResult(int position, String show, String result) {
		HashMap<String,Object> map = null;
		map = new HashMap<String,Object>();
		map.put("show", show);
		map.put("result", result);
		this.resultList.set(position, map);
		
		notifyDataSetChanged();    		
	}
	@Override
	public Object getChild(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		HashMap<String,Object> map = this.resultList.get(groupPosition);
		return map;
	}
	@Override
	public long getChildId(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return childPosition;
	}
	@Override
	public int getChildrenCount(int groupPosition) {
		// TODO Auto-generated method stub
		return 1;
	}
	@Override
	public Object getGroup(int groupPosition) {
		// TODO Auto-generated method stub
		HashMap<String,Object> map = this.arrayList.get(groupPosition);
		return map;
	}
	@Override
	public int getGroupCount() {
		// TODO Auto-generated method stub
		return this.arrayList.size();
	}
	@Override
	public long getGroupId(int groupPosition) {
		// TODO Auto-generated method stub
		return groupPosition;
	}
	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,	View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View row = convertView;
		HashMap<String,Object> map = this.arrayList.get(groupPosition);
		
		if (row==null) {
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(R.layout.result_rad_row_order, null);
		}
		
		String orddate = (String)map.get("orddate");
		String ono = (String)map.get("ono");
		String onm = (String)map.get("onm");
		
		TextView textView;
		// 처방일자
		textView = (TextView)row.findViewById(R.id.orddate);
		textView.setText(orddate);
		// 처방번호
		textView = (TextView)row.findViewById(R.id.ono);
		textView.setText(ono);
		// 처방명
		textView = (TextView)row.findViewById(R.id.onm);
		textView.setText(onm);
		
		return row;
	}
	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View row = convertView;
		HashMap<String,Object> map = this.resultList.get(groupPosition);
		
		if (row==null) {
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(R.layout.result_rad_row_order_result, null);
		}

		String result=(String)map.get("result");
		
		TextView textView;
		// 결과
		textView = (TextView)row.findViewById(R.id.result_rad_text);
		textView.setText(result);
		
		return row;
	}
	@Override
	public boolean hasStableIds() {
		// TODO Auto-generated method stub
		return false;
	}
	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return false;
	}    	
}
