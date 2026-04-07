package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.OrderAdapter.ViewHolder;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.DateUtil;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ResultLisAdapter extends BaseAdapter {
	private Context context;
	private ArrayList<HashMap<String,Object>> arrayList;
	
	public ResultLisAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList) {
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
		
		ViewHolder viewHolder;
		if(row==null){
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(R.layout.result_lis_row, null);
			viewHolder = new ViewHolder();
//			viewHolder.orddt = (TextView)row.findViewById(R.id.orddt_result_lis_row);
			viewHolder.abbrnm = (TextView)row.findViewById(R.id.abbrnm_result_lis_row);
			viewHolder.rstval = (TextView)row.findViewById(R.id.rstval_result_lis_row);
			viewHolder.beforerstval = (TextView)row.findViewById(R.id.beforerstval_result_lis_row);
			viewHolder.referchk = (TextView)row.findViewById(R.id.referchk_result_lis_row);
			viewHolder.panicchk = (TextView)row.findViewById(R.id.panicchk_result_lis_row);
			viewHolder.deltachk = (TextView)row.findViewById(R.id.deltachk_result_lis_row);
			viewHolder.reference = (TextView)row.findViewById(R.id.reference_result_lis_row);
			viewHolder.unit = (TextView)row.findViewById(R.id.unit_result_lis_row);
			viewHolder.spcnm = (TextView)row.findViewById(R.id.spcnm_result_lis_row);
			viewHolder.majnm = (TextView)row.findViewById(R.id.majnm_result_lis_row);
			row.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder)row.getTag();
		}
		
		// 초기화
//		viewHolder.orddt.setText("");
		viewHolder.abbrnm.setText("");
		viewHolder.rstval.setText("");
		viewHolder.beforerstval.setText("");
		viewHolder.referchk.setText("");
		viewHolder.panicchk.setText("");
		viewHolder.deltachk.setText("");
		viewHolder.reference.setText("");
		viewHolder.unit.setText("");
		viewHolder.spcnm.setText("");
		viewHolder.majnm.setText("");
		//row.setBackgroundColor(Color.WHITE);
		row.setBackgroundResource(R.drawable.shape_listview_row);


		// orientation = 0 : 세로
		//               1 : 가로
		//               2 : 세로(뒤집힌 세로)
		//               3 : 가로(뒤집힌 가로)
		//int orientation = ((Activity)this.context).getWindowManager().getDefaultDisplay().getOrientation();
		
		String orddt=(String)map.get("orddt");
		String abbrnm=(String)map.get("abbrnm");
		String spcnm=(String)map.get("spcnm");
		String majnm=(String)map.get("majnm");
		String beforerstval=(String)map.get("beforerstval");
		String unit=(String)map.get("unit");
		String referchk=(String)map.get("referchk");
		String panicchk=(String)map.get("panicchk");
		String deltachk=(String)map.get("deltachk");
		String isdateline=(String)map.get("isdateline");
		
		orddt=orddt.trim();
		referchk=referchk.trim();
		panicchk=panicchk.trim();
		deltachk=deltachk.trim();
		
	
		boolean abnormal=false;
		if (!referchk.equals("") || !panicchk.equals("") || !deltachk.equals("")) {
			abnormal=true;
			//row.setBackgroundColor(Color.LTGRAY);
			row.setBackgroundResource(R.drawable.shape_listview_reverse_row);
		}

//		viewHolder.orddt.setText(DateUtil.getFormattedDate(orddt));
//		if (abnormal==true) viewHolder.orddt.setTextColor(Color.BLACK);
		//
		viewHolder.abbrnm.setText(abbrnm);
		if (abnormal==true) viewHolder.abbrnm.setTextColor(Color.BLACK);
		// 
		viewHolder.rstval.setText((String)map.get("rstval"));
		if (abnormal==true) viewHolder.rstval.setTextColor(Color.BLACK);
		// 
		viewHolder.beforerstval.setText(beforerstval);
		if (abnormal==true) viewHolder.beforerstval.setTextColor(Color.BLACK);
		// 
		viewHolder.referchk.setText(referchk);
		if (abnormal==true) viewHolder.referchk.setTextColor(Color.BLACK);
		// 
		viewHolder.panicchk.setText(panicchk);
		if (abnormal==true) viewHolder.panicchk.setTextColor(Color.BLACK);
		// 
		viewHolder.deltachk.setText(deltachk);
		if (abnormal==true) viewHolder.deltachk.setTextColor(Color.BLACK);
		// 
		viewHolder.reference.setText((String)map.get("reference"));
		if (abnormal==true) viewHolder.reference.setTextColor(Color.BLACK);
		// 
		viewHolder.unit.setText(unit);
		if (abnormal==true) viewHolder.unit.setTextColor(Color.BLACK);
		// 
		viewHolder.spcnm.setText(spcnm);
		if (abnormal==true) viewHolder.spcnm.setTextColor(Color.BLACK);
		// 
		viewHolder.majnm.setText(majnm);
		if (abnormal==true) viewHolder.majnm.setTextColor(Color.BLACK);
		
		if(spcnm.equals("")){
			viewHolder.spcnm.setVisibility(View.GONE);
		}else{
			viewHolder.spcnm.setVisibility(View.VISIBLE);
		}
		if(majnm.equals("")){
			viewHolder.majnm.setVisibility(View.GONE);
		}else{
			viewHolder.majnm.setVisibility(View.VISIBLE);
		}
		
		if(isdateline.equals("1")){
			row.setBackgroundColor(this.context.getResources().getColor(R.color.rowdivbackground));
			//viewHolder.abbrnm.setTextSize(TypedValue.COMPLEX_UNIT_DIP, context.getResources().getDimension(R.dimen.large_text_size));
			//viewHolder.abbrnm.setTypeface(null, Typeface.ITALIC);
		}else{
			//viewHolder.abbrnm.setTextSize(TypedValue.COMPLEX_UNIT_DIP, context.getResources().getDimension(R.dimen.normal_text_size));
			//viewHolder.abbrnm.setTypeface(null, Typeface.NORMAL);
		}

		// 세로이면
		/*
		if (orientation==0 || orientation==2) {
			if (spcnm.equals("") && majnm.equals("") && beforerstval.equals("") && unit.equals("")) {
    			((TextView)row.findViewById(R.id.spcnm_result_lis_row)).setVisibility(View.GONE);    			
    			((TextView)row.findViewById(R.id.majnm_result_lis_row)).setVisibility(View.GONE);    			
    			((TextView)row.findViewById(R.id.beforerstval_result_lis_row)).setVisibility(View.GONE);    			
    			((TextView)row.findViewById(R.id.unit_result_lis_row)).setVisibility(View.GONE);
    			// 가짜필드
    			((TextView)row.findViewById(R.id.dummy_orddt_result_lis_row)).setVisibility(View.GONE);    			
    			//((TextView)row.findViewById(R.id.dummy_abbrnm_result_lis_row)).setVisibility(View.GONE);    	
			}
		}
		*/
		
		return row;
	}
	
    class ViewHolder{
//    	TextView orddt;
    	TextView abbrnm;
    	TextView rstval;
    	TextView beforerstval;
    	TextView referchk;
    	TextView panicchk;
    	TextView deltachk;
    	TextView reference;
    	TextView unit;
    	TextView spcnm;
    	TextView majnm;
    }
}
