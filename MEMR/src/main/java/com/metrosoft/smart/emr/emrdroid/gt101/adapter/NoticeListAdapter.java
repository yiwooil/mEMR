package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.OrderAdapter.ViewHolder;

import android.app.Activity;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class NoticeListAdapter extends BaseAdapter {
	private Context context;
	private ArrayList<HashMap<String,Object>> arrayList;
	
	public NoticeListAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList) {
		this.context = context;
		this.arrayList = arrayList;
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
		HashMap<String,Object> map = this.arrayList.get(position);
		
		String title = (String)map.get("title");
		String apdate = (String)map.get("apdate");
		String empnm = (String)map.get("empnm");
		String desc = title + "   " + apdate + " " + empnm;
		
		// 
		View row = convertView;
		ViewHolder viewHolder;
		if(row==null){
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(R.layout.notice_list_row, null);
			viewHolder = new ViewHolder();
			viewHolder.title = (TextView)row.findViewById(R.id.notice_list_row_title);
			row.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder)row.getTag();
		}
		
		SpannableStringBuilder spannable = new SpannableStringBuilder(desc);
		spannable.setSpan(new RelativeSizeSpan(0.6f), title.length(), desc.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		viewHolder.title.setText(spannable);
	
		return row;
	}
	
    class ViewHolder{
    	TextView title;
    }

}
