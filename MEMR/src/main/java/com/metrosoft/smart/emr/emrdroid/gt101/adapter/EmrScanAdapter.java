package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.NoticeListAdapter.ViewHolder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class EmrScanAdapter extends BaseAdapter {

	public interface OnModifyClickListener {
		void onModifyClick(HashMap<String, Object> map);
	}

	private Context context;
	private ArrayList<HashMap<String,Object>> arrayList;
	private ArrayList<Bitmap> bitmapList; // 이미지를 메모리에 올려놓기 위함. 스크롤시 속도를 빠르게 하기위하여.
	BitmapFactory.Options options;
	private OnModifyClickListener mModifyListener;

	public EmrScanAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList, OnModifyClickListener listener) {
		this.context = context;
		this.arrayList = arrayList;
		this.mModifyListener = listener;
		//
		this.bitmapList = null;
		this.bitmapList = new ArrayList<Bitmap>();
		for(long i=0;i<this.arrayList.size();i++){
			bitmapList.add(null);
		}
		options = new BitmapFactory.Options();
		options.inSampleSize = 4;
		
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
	public View getView (int position, View convertView, ViewGroup parent) {
	
		HashMap<String,Object> map = this.arrayList.get(position);
		
		View row = convertView;
		ViewHolder viewHolder;
		if(row==null){
			LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
			row = inflater.inflate(R.layout.emr_scan_row, null);
			viewHolder = new ViewHolder();
			viewHolder.image = (ImageView)row.findViewById(R.id.image);
			viewHolder.rptnm = (TextView)row.findViewById(R.id.rptnm);
			viewHolder.btnModity = (TextView)row.findViewById(R.id.btn_modify);
			if (mModifyListener == null) viewHolder.btnModity.setVisibility(View.GONE); // 안보이게
			row.setTag(viewHolder);
		}
		else {
			viewHolder = (ViewHolder)row.getTag();
		}
		
		String exdt = (String)map.get("exdt");
		String seq = (String)map.get("seq");
		String rptnm = (String)map.get("rptnm");
		String exdtSeq = getFormattedDate(exdt) + "." + seq;
		String dirPath = (String)map.get("dirpath");
		String fileName = (String)map.get("filename");
	
		Log.d("EmrDroid","fileName=" + dirPath + "/" + fileName);

		// 저장해 놓은 파일에서 이미지를 읽어온다.
		// 메모리에 올려놓고 사용한다. 매번 파일을 읽지 않기 위하여
		Bitmap bitmap = this.bitmapList.get(position);
		if(bitmap==null){
			Log.d("EmrDroid","2) fileName=" + dirPath + "/" + fileName);
			if(fileName.equals("")){
				bitmap = null;
			}else{
				// 원래 이미지크기에서 1/options.inSampleSize 만큼 줄인다.
				// 메모리 사용량을 줄이기 위한 코딩임.
				bitmap = BitmapFactory.decodeFile(dirPath + "/" + fileName, options);
				//bitmap = Bitmap.createScaledBitmap(bitmap, 64, 64, true); // 잉? 오류가...
				this.bitmapList.set(position, bitmap);
			}
		}
		String dispRptnm = rptnm + "  " + exdtSeq;
		SpannableStringBuilder spannable = new SpannableStringBuilder(dispRptnm);
		spannable.setSpan(new RelativeSizeSpan(0.6f), rptnm.length(), dispRptnm.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		
		viewHolder.image.setImageBitmap(bitmap);
		viewHolder.rptnm.setText(spannable);
		//((TextView)row.findViewById(R.id.exdt_seq)).setText(exdtSeq);

		final HashMap<String, Object> rowMap = map; // Java 7 호환(ChagGPT 추천)
		viewHolder.btnModity.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mModifyListener != null) mModifyListener.onModifyClick(rowMap);
			}
		});

		return row;
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
	
    class ViewHolder{
    	ImageView image;
    	TextView rptnm;
		TextView btnModity;
    }
	
}
