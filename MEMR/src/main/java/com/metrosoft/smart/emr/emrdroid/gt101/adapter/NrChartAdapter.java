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

public class NrChartAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<HashMap<String,Object>> arrayList;

    public NrChartAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }
    @Override
    public int getCount() {
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

        View row = convertView;
        HashMap<String,Object> map = this.arrayList.get(position);

        String wdate = (String)map.get("wdate");
        //String seq = (String)map.get("seq");
        String wtime = (String)map.get("wtime");
        String result = (String)map.get("result");
        //String empid = (String)map.get("empid");
        String empnm = (String)map.get("empnm");

        //
        LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
        row = inflater.inflate(R.layout.nr_chart_row, null);

        TextView textView;

        // 일자
        textView = (TextView)row.findViewById(R.id.wdate_nr_chart_row);
        textView.setText(wdate);
        // 시간
        textView = (TextView)row.findViewById(R.id.wtime_nr_chart_row);
        textView.setText(wtime);
        // 내용
        textView = (TextView)row.findViewById(R.id.result_nr_chart_row);
        textView.setText(result);
        // 작성자
        textView = (TextView)row.findViewById(R.id.empnm_nr_chart_row);
        textView.setText(empnm);

        return row;
    }
}
