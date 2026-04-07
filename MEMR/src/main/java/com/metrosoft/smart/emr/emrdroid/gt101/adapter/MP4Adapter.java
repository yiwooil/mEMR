package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class MP4Adapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<HashMap<String,Object>> arrayList;
    
    public MP4Adapter(Context c, ArrayList<HashMap<String,Object>> arrayList){
        mContext = c;
        this.arrayList = arrayList;
    }
    public boolean deleteSelected(int sIndex){
        return true;
    }
     
    public int getCount() {
        return this.arrayList.size();
    }
     
    public Object getItem(int position) {
        return this.arrayList.get(position);
    }
     
    public long getItemId(int position) { 
        return position;
    }
     
    public View getView(int position, View convertView, ViewGroup parent) {
        View listViewItem = convertView; 
        if (listViewItem == null) {
            /** Item.xml을 Inflate해 Layout 구성된 View를 얻는다. **/
            LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            listViewItem = vi.inflate(R.layout.mp4_play_item, null);
        }
         
        HashMap<String,Object> map = this.arrayList.get(position);
        String title = (String)map.get("title");
        String exdt = (String)map.get("exdt");
        String seq = (String)map.get("seq");
        String mp4Idx = (String)map.get("mp4_idx");
        String exdtSeq = getFormattedDate(exdt) + "." + seq + "." + mp4Idx;
        
        String dispTitle = title + "  " + exdtSeq;
		SpannableStringBuilder spannable = new SpannableStringBuilder(dispTitle);
		spannable.setSpan(new RelativeSizeSpan(0.6f), title.length(), dispTitle.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		
        /** Title 설정 **/
        TextView tv = (TextView) listViewItem.findViewById(R.id.title);
        tv.setText(spannable);

        /** 구성된 ListView Item을 리턴해 준다. **/
        return listViewItem;
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
