package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

import android.app.Activity;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

public class ConsentFormListAdapter extends BaseExpandableListAdapter {
	private Context context;
	private ArrayList<HashMap<String,Object>> arrayList;
	private ArrayList<HashMap<String,Object>> childList;
	private LayoutInflater inflater;
	
	public ConsentFormListAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList, ArrayList<HashMap<String,Object>> childList){
		this.context = context;
		this.arrayList = arrayList;
		this.childList = childList;
		this.inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public Object getChild(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		HashMap<String,Object> map = this.arrayList.get(groupPosition);
		String ccfGroup = (String)map.get("ccf_group");
		int idx=0;
		for(int i=0;i<childList.size();i++){
			HashMap<String,Object> childMap = this.childList.get(i);
			String childCcfGroup = (String)childMap.get("ccf_group");
			if(childCcfGroup.equalsIgnoreCase(ccfGroup)){
				if(idx==childPosition){
					return childMap;
				}
				idx++;
			}
		}
		return null;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		// TODO Auto-generated method stub
		return childPosition;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		// TODO Auto-generated method stub
		HashMap<String,Object> map = this.arrayList.get(groupPosition);
		String ccfGroup = (String)map.get("ccf_group");
		int cnt=0;
		for(int i=0;i<childList.size();i++){
			HashMap<String,Object> childMap = this.childList.get(i);
			String childCcfGroup = (String)childMap.get("ccf_group");
			if(childCcfGroup.equalsIgnoreCase(ccfGroup)){
				cnt++;
			}
		}
		return cnt;
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
			row = this.inflater.inflate(R.layout.certificate_group_list_row, null);
		}
		
		String ccfGroup = (String)map.get("ccf_group");
		
		TextView textView;
		// 그룹명
		textView = (TextView)row.findViewById(R.id.certificate_group_list_row_group);
		textView.setText(ccfGroup);
		
		return row;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View row = convertView;
		HashMap<String,Object> map = (HashMap<String,Object>)this.getChild(groupPosition,childPosition);
		
		if (row==null) {
			row = this.inflater.inflate(R.layout.certificate_list_row, null);
		}

		TextView textView;
		textView = (TextView)row.findViewById(R.id.certificate_list_row_name);

		String ccfName=(String)map.get("ccf_name");
		String preSaved=(String)map.get("pre_saved");
		if("".equalsIgnoreCase(preSaved)){
			//Log.d("EmrDroid","ccfName="+ccfName);
			textView.setText(ccfName);
		}else{
			String exdt=(String)map.get("exdt");
			String seq=(String)map.get("seq");
			String exdtSeq=getFormattedDate(exdt) + "." + seq;
			String dispCcfName = ccfName + "  " + exdtSeq;
			SpannableStringBuilder spannable = new SpannableStringBuilder(dispCcfName);
			spannable.setSpan(new RelativeSizeSpan(0.6f), ccfName.length(), dispCcfName.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			textView.setText(spannable);
		}
		
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
		return true;
	}
	
	protected String getFormattedDate(String v) {
		if (v == null)
			return "";
		if (v.equals(""))
			return "";
		if (v.length() < 8)
			return v;
		return v.substring(0, 4) + "." + v.substring(4, 6) + "." + v.substring(6, 8);
	}

}
