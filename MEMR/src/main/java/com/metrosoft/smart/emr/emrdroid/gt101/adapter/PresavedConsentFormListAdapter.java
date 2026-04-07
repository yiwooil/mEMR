package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import android.app.Activity;
import android.content.Context;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

public class PresavedConsentFormListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<HashMap<String,Object>> arrayList;

    public PresavedConsentFormListAdapter(Context context, ArrayList<HashMap<String,Object>> arrayList) {
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
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        HashMap<String,Object> map = this.arrayList.get(position);

        ViewHolder viewHolder;
        if (row==null) {
            LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
            row = inflater.inflate(R.layout.presaved_consent_form_list_row, null);
            viewHolder = new ViewHolder();
            viewHolder.pscf_image = (ImageView)row.findViewById(R.id.pscf_image);
            viewHolder.pscf_div_bar = (TextView)row.findViewById(R.id.pscf_div_bar);
            viewHolder.pscf_pnm = (TextView)row.findViewById(R.id.pscf_pnm);
            viewHolder.pscf_pid = (TextView)row.findViewById(R.id.pscf_pid);
            viewHolder.pscf_psexage = (TextView)row.findViewById(R.id.pscf_psexage);
            viewHolder.pscf_dptcd = (TextView)row.findViewById(R.id.pscf_dptcd);
            viewHolder.pscf_ward = (TextView)row.findViewById(R.id.pscf_ward);
            viewHolder.pscf_pdrnm = (TextView)row.findViewById(R.id.pscf_pdrnm);
            viewHolder.pscf_qfycdnm = (TextView)row.findViewById(R.id.pscf_qfycdnm);
            viewHolder.pscf_disp_bededt_bedodt = (TextView)row.findViewById(R.id.pscf_disp_bededt_bedodt);
            viewHolder.pscf_ccf_name = (TextView)row.findViewById(R.id.pscf_ccf_name);
            viewHolder.pscf_exdt_seq = (TextView)row.findViewById(R.id.pscf_exdt_seq);
            row.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder)row.getTag();
        }

        viewHolder.pscf_image.setImageDrawable(null);
        viewHolder.pscf_div_bar.setText("");
        viewHolder.pscf_pnm.setText("");
        viewHolder.pscf_pid.setText("");
        viewHolder.pscf_psexage.setText("");
        viewHolder.pscf_dptcd.setText("");
        viewHolder.pscf_ward.setText("");
        viewHolder.pscf_pdrnm.setText("");
        viewHolder.pscf_qfycdnm.setText("");
        viewHolder.pscf_disp_bededt_bedodt.setText("");
        viewHolder.pscf_ccf_name.setText("");
        viewHolder.pscf_exdt_seq.setText("");
        row.setBackgroundResource(R.drawable.shape_listview_row);

        String isdateline = (String)map.get("isdateline");
        if(isdateline.equals("1")) {
            viewHolder.pscf_div_bar.setVisibility(View.VISIBLE);
            viewHolder.pscf_pnm.setVisibility(View.GONE);
            viewHolder.pscf_pid.setVisibility(View.GONE);
            viewHolder.pscf_psexage.setVisibility(View.GONE);
            viewHolder.pscf_dptcd.setVisibility(View.GONE);
            viewHolder.pscf_ward.setVisibility(View.GONE);
            viewHolder.pscf_pdrnm.setVisibility(View.GONE);
            viewHolder.pscf_qfycdnm.setVisibility(View.GONE);
            viewHolder.pscf_disp_bededt_bedodt.setVisibility(View.GONE);
            viewHolder.pscf_ccf_name.setVisibility(View.GONE);
            viewHolder.pscf_exdt_seq.setVisibility(View.GONE);

            viewHolder.pscf_div_bar.setText(Utils.getFormattedDate((String) map.get("exdt")));

            row.setBackgroundResource(R.drawable.shape_listview_reverse_row);
        } else {
            viewHolder.pscf_div_bar.setVisibility(View.GONE);
            viewHolder.pscf_pnm.setVisibility(View.VISIBLE);
            viewHolder.pscf_pid.setVisibility(View.VISIBLE);
            viewHolder.pscf_psexage.setVisibility(View.VISIBLE);
            viewHolder.pscf_dptcd.setVisibility(View.VISIBLE);
            viewHolder.pscf_ward.setVisibility(View.VISIBLE);
            viewHolder.pscf_pdrnm.setVisibility(View.VISIBLE);
            viewHolder.pscf_qfycdnm.setVisibility(View.VISIBLE);
            viewHolder.pscf_disp_bededt_bedodt.setVisibility(View.VISIBLE);
            viewHolder.pscf_ccf_name.setVisibility(View.VISIBLE);
            viewHolder.pscf_exdt_seq.setVisibility(View.VISIBLE);

            viewHolder.pscf_image.setImageResource((int) map.get("image"));
            viewHolder.pscf_pnm.setText((String) map.get("pnm"));
            viewHolder.pscf_pid.setText((String) map.get("pid"));
            viewHolder.pscf_psexage.setText((String) map.get("psexage"));
            viewHolder.pscf_dptcd.setText((String) map.get("dptcd"));
            viewHolder.pscf_ward.setText((String) map.get("ward"));
            viewHolder.pscf_pdrnm.setText((String) map.get("pdrnm"));
            viewHolder.pscf_qfycdnm.setText((String) map.get("qfynm"));
            viewHolder.pscf_disp_bededt_bedodt.setText((String) map.get("disp_bededt_bedodt"));
            viewHolder.pscf_ccf_name.setText((String) map.get("ccf_name"));
            viewHolder.pscf_exdt_seq.setText((String) map.get("ccf_exdt_seq"));
        }

        return row;
    }
    class ViewHolder{
        ImageView pscf_image;
        TextView pscf_div_bar;
        TextView pscf_pnm;
        TextView pscf_pid;
        TextView pscf_psexage;
        TextView pscf_dptcd;
        TextView pscf_ward;
        TextView pscf_pdrnm;
        TextView pscf_qfycdnm;
        TextView pscf_disp_bededt_bedodt;
        TextView pscf_ccf_name;
        TextView pscf_exdt_seq;
    }

}
