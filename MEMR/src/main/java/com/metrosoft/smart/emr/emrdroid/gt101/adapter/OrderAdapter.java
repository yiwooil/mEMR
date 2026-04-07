package com.metrosoft.smart.emr.emrdroid.gt101.adapter;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.DateUtil;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class OrderAdapter extends BaseAdapter {
	private Context context;
	private ArrayList<HashMap<String,Object>> arrayList;
	private LayoutInflater inflater;
	
	public OrderAdapter(Context context, ResultSetHelper rs) throws JSONException {
		this.context = context;
		this.arrayList = new ArrayList<HashMap<String,Object>>();
		this.inflater = ((Activity)this.context).getLayoutInflater();
		
		setData(rs);
	}
	private void setData(ResultSetHelper rs) throws JSONException {
		HashMap<String,Object> map = null;

		String bkOdt="";
		String bkBdiv="";
		for (int i=0 ; i<rs.getRecordCount() ; i++) {
			map = new HashMap<String,Object>();
			String odt=rs.getString(i,"odt");
			long ono=rs.getLong(i, "ono");
			String bdiv=rs.getString(i,"bdiv");
			if(bkOdt.equals(odt)==false||bkBdiv.equals(bdiv)==false) {
				// 일자가 변경되었다.
				String orddate=DateUtil.getFormattedDate(odt);
				if(bdiv.equals("1")) orddate+=" 외래";
				else if(bdiv.equals("3")) orddate+=" 응급실";
				//map.put("image",null);
				map.put("odt", rs.getString(i,"odt"));
				map.put("ono", "");
				map.put("onm", orddate);
				map.put("oqty", "");
				map.put("ounit", "");
				map.put("ordcnt", "");
				map.put("odaycnt", "");
				map.put("rmk", "");
				map.put("odivcd", "DATE_LINE");
				map.put("odivcdnm", "");
				map.put("alwfg", "");
				map.put("alwfgnm", "");
				map.put("dcfg", "");
				map.put("exdrid","");
				map.put("exdrnm","");
				map.put("exdrempnm","");
				map.put("prnfg","");
				map.put("rmk1", "");
				map.put("rmk2", "");
				map.put("rmk3", "");
				map.put("rmk4", "");
				map.put("rmk5", "");
				map.put("rmk6", "");
				this.arrayList.add(map);
				map = new HashMap<String,Object>();
			}
			bkOdt=odt;
			bkBdiv=bdiv;
			String odivcd = rs.getString(i,"odivcd");

			// rmk를 rmk1 ~ rmk6으로 분리한다.
			String rmk=rs.getString(i,"rmk");
			String tmpRmk=rmk+"$$$$$"; // 오류방지용
			String rmk1[]={"","","","","",""};
			int rmkIndexFr=0;
			int rmkIndexTo=0;
			for (int rmkTurn=0;rmkTurn<6;rmkTurn++) {
				rmkIndexTo=tmpRmk.indexOf("$",rmkIndexFr);
				if (rmkIndexTo==-1) break;
				rmk1[rmkTurn]=tmpRmk.substring(rmkIndexFr, rmkIndexTo);
				rmkIndexFr=rmkIndexTo+1;
			}

			// 처방명
			String onm=rs.getString(i,"onm");
			if (odivcd.equals("D")) {
				// 식사처방인 경우
				String exdt=rs.getString(i,"exdt");
				String fldcd1=rs.getString(i,"fldcd1");
				if(fldcd1.equals("1")) {
					if(ono<5000){
						onm=DateUtil.getFormattedDate(exdt) + " 아침부터 " + onm;
					}else{
						onm=DateUtil.getFormattedDate(exdt) + " 아침 " + onm;
					}
				}
				else if(fldcd1.equals("2")) {
					if(ono<5000){
						onm=DateUtil.getFormattedDate(exdt) + " 점심부터 " + onm;
					}else{
						onm=DateUtil.getFormattedDate(exdt) + " 점심 " + onm;
					}
				}
				else if(fldcd1.equals("3")) {
					if(ono<5000){
						onm=DateUtil.getFormattedDate(exdt) + " 저녁부터 " + onm;
					}else{
						onm=DateUtil.getFormattedDate(exdt) + " 저녁 " + onm;
					}
				}
			}else if(odivcd.endsWith("S")) {
				// 메시지 처방
				// 리마크를 처방명으로 하고, 리마크는 지운다.
				onm = rmk1[0];
				if(!rmk1[1].equals("")) onm += " " + rmk1[1];
				if(!rmk1[2].equals("")) onm += " " + rmk1[2];
				if(!rmk1[3].equals("")) onm += " " + rmk1[3];
				if(!rmk1[4].equals("")) onm += " " + rmk1[4];
				if(!rmk1[5].equals("")) onm += " " + rmk1[5];
				rmk1[0]="";
				rmk1[1]="";
				rmk1[2]="";
				rmk1[3]="";
				rmk1[4]="";
				rmk1[5]="";
			}
			
			
			map.put("odt", rs.getString(i,"odt"));
			map.put("ono", rs.getString(i,"ono"));
			map.put("onm", onm);
			map.put("oqty", getNumFmt(rs.getString(i,"oqty")));
			map.put("ounit", rs.getString(i,"ounit"));
			map.put("ordcnt", getNumFmt(rs.getString(i,"ordcnt")));
			map.put("odaycnt", rs.getString(i,"odaycnt"));
			map.put("rmk", rs.getString(i,"rmk"));
			map.put("odivcd", rs.getString(i,"odivcd"));
			map.put("odivcdnm", rs.getString(i,"odivcdnm"));
			map.put("alwfg", rs.getString(i,"alwfg"));
			map.put("alwfgnm", rs.getString(i,"alwfgnm"));
			map.put("dcfg", rs.getString(i,"dcfg"));
			map.put("ostscd", rs.getString(i,"ostscd"));
			map.put("exdrid", rs.getString(i,"exdrid"));
			map.put("prnfg", rs.getString(i,"prnfg"));
			map.put("exdrnm", rs.getString(i,"exdrnm"));
			map.put("exdrempnm", rs.getString(i,"exdrempnm"));


			// 식이처방이면 rmk가 추가식및선택이다.
			if (odivcd.equals("D")) {
				//
			}
			
			map.put("rmk1", rmk1[0]);
			map.put("rmk2", rmk1[1]);
			map.put("rmk3", rmk1[2]);
			map.put("rmk4", rmk1[3]);
			map.put("rmk5", rmk1[4]);
			map.put("rmk6", rmk1[5]);
			
			this.arrayList.add(map);
		}
		
	}
	// 소숫점 밑에 0을 제거한다.
	private String getNumFmt(String s){
		try{
			Double d = Double.parseDouble(s);
			Long l = d.longValue();
			if(d==(double)l){
				// 소수밑에 없는 경우임.
				return l.toString();
			}else{
				return d.toString();
			}
		}catch(NumberFormatException e){
			return s;
		}
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
	public int getViewTypeCount() {
		// ListView에서 사용하는 row의 갯수를 반환한다.
		// 만일, row가 추가되거나 없어지면 수정해야함.
		// getItemViewType도 같이 수정해야함.
		return 5;
	}
	@Override 
	public int getItemViewType(int position) {
		HashMap<String,Object> map = this.arrayList.get(position);
		String odivcd = (String)map.get("odivcd");
		return getODiv(odivcd);
	}
	@Override
	public View getView (int position, View convertView, ViewGroup parent) {

		View row = convertView;
		HashMap<String,Object> map = this.arrayList.get(position);
		
		String odivcd = (String)map.get("odivcd");
		String odivcdnm = (String)map.get("odivcdnm");
		String dcfg = (String)map.get("dcfg");
		String exdrnm = (String)map.get("exdrnm");
		String prnfg = (String)map.get("prnfg");
		String alwfg = (String)map.get("alwfg");
		String alwfgnm = (String)map.get("alwfgnm");
		
		if("".equals(odivcdnm)) odivcdnm = odivcd;
		if("".equals(exdrnm)) exdrnm = (String)map.get("exdrempnm");
		if("".equals(exdrnm)) exdrnm = (String)map.get("exdrid");
		String prnfgnm="";
		if("1".equals(prnfg)) prnfgnm = "PRN";
		if("".equals(alwfgnm)) alwfgnm = alwfg;
		
		int availProc = Runtime.getRuntime().availableProcessors();
		long total = Runtime.getRuntime().totalMemory();
		long free = Runtime.getRuntime().freeMemory();
		long max = Runtime.getRuntime().maxMemory();


		// odivcd에 따라 모양을 달리 해야하기 때문에 convertView를 사용할 수 없다.
		//LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
		int odiv=getODiv(odivcd);
		Log.d("EmrDroid","odivcd=" + odivcd + ", odiv=" + odiv + ",total=" + Long.toString(total));
		
		ViewHolder viewHolder;
		if(row==null) {
			//row = inflater.inflate(R.layout.order_row, null);
			if (odiv==1) {
				// 일자구분선임.
				row = inflater.inflate(R.layout.order_row_odivcd_d, null);
				viewHolder = new ViewHolder();
				viewHolder.odt = (TextView)row.findViewById(R.id.odt_chart_row);
				viewHolder.dcfgnm = (TextView)row.findViewById(R.id.dcfgnm_chart_row);
				viewHolder.ono = (TextView)row.findViewById(R.id.ono_chart_row);
				viewHolder.onm = (TextView)row.findViewById(R.id.onm_chart_row);
				viewHolder.oqty = null;
				viewHolder.ounit = null;
				viewHolder.ordcnt = null;
				viewHolder.odaycnt = null;
				viewHolder.alwfgnm = (TextView)row.findViewById(R.id.alwfgnm_chart_row);
				viewHolder.ostscd = (TextView)row.findViewById(R.id.ostscd_chart_row);
				viewHolder.exdrnm = (TextView)row.findViewById(R.id.exdrnm_chart_row);
				viewHolder.prnfg = (TextView)row.findViewById(R.id.prnfg_chart_row);
				viewHolder.odivcdnm = (TextView)row.findViewById(R.id.odivcdnm_chart_row);
				viewHolder.rmk1 = (TextView)row.findViewById(R.id.rmk1_chart_row);
				viewHolder.rmk2 = (TextView)row.findViewById(R.id.rmk2_chart_row);
				viewHolder.rmk3 = (TextView)row.findViewById(R.id.rmk3_chart_row);
				viewHolder.rmk4 = (TextView)row.findViewById(R.id.rmk4_chart_row);
				viewHolder.rmk5 = (TextView)row.findViewById(R.id.rmk5_chart_row);
				viewHolder.rmk6 = (TextView)row.findViewById(R.id.rmk6_chart_row);
			}
			else if (odiv==2) {
				// 수가명만 있음.
				row = inflater.inflate(R.layout.order_row_odivcd_d, null);
				viewHolder = new ViewHolder();
				viewHolder.odt = (TextView)row.findViewById(R.id.odt_chart_row);
				viewHolder.dcfgnm = (TextView)row.findViewById(R.id.dcfgnm_chart_row);
				viewHolder.ono = (TextView)row.findViewById(R.id.ono_chart_row);
				viewHolder.onm = (TextView)row.findViewById(R.id.onm_chart_row);
				viewHolder.oqty = null;
				viewHolder.ounit = null;
				viewHolder.ordcnt = null;
				viewHolder.odaycnt = null;
				viewHolder.alwfgnm = (TextView)row.findViewById(R.id.alwfgnm_chart_row);
				viewHolder.ostscd = (TextView)row.findViewById(R.id.ostscd_chart_row);
				viewHolder.exdrnm = (TextView)row.findViewById(R.id.exdrnm_chart_row);
				viewHolder.prnfg = (TextView)row.findViewById(R.id.prnfg_chart_row);
				viewHolder.odivcdnm = (TextView)row.findViewById(R.id.odivcdnm_chart_row);
				viewHolder.rmk1 = (TextView)row.findViewById(R.id.rmk1_chart_row);
				viewHolder.rmk2 = (TextView)row.findViewById(R.id.rmk2_chart_row);
				viewHolder.rmk3 = (TextView)row.findViewById(R.id.rmk3_chart_row);
				viewHolder.rmk4 = (TextView)row.findViewById(R.id.rmk4_chart_row);
				viewHolder.rmk5 = (TextView)row.findViewById(R.id.rmk5_chart_row);
				viewHolder.rmk6 = (TextView)row.findViewById(R.id.rmk6_chart_row);
			}
			else if (odiv==3) {
				// 투여량,단위 없음.
				row = inflater.inflate(R.layout.order_row_odivcd_t, null);
				viewHolder = new ViewHolder();
				viewHolder.odt = (TextView)row.findViewById(R.id.odt_chart_row);
				viewHolder.dcfgnm = (TextView)row.findViewById(R.id.dcfgnm_chart_row);
				viewHolder.ono = (TextView)row.findViewById(R.id.ono_chart_row);
				viewHolder.onm = (TextView)row.findViewById(R.id.onm_chart_row);
				viewHolder.oqty = null;
				viewHolder.ounit = null;
				viewHolder.ordcnt = (TextView)row.findViewById(R.id.ordcnt_chart_row);
				viewHolder.odaycnt = (TextView)row.findViewById(R.id.odaycnt_chart_row);
				viewHolder.alwfgnm = (TextView)row.findViewById(R.id.alwfgnm_chart_row);
				viewHolder.ostscd = (TextView)row.findViewById(R.id.ostscd_chart_row);
				viewHolder.exdrnm = (TextView)row.findViewById(R.id.exdrnm_chart_row);
				viewHolder.prnfg = (TextView)row.findViewById(R.id.prnfg_chart_row);
				viewHolder.odivcdnm = (TextView)row.findViewById(R.id.odivcdnm_chart_row);
				viewHolder.rmk1 = (TextView)row.findViewById(R.id.rmk1_chart_row);
				viewHolder.rmk2 = (TextView)row.findViewById(R.id.rmk2_chart_row);
				viewHolder.rmk3 = (TextView)row.findViewById(R.id.rmk3_chart_row);
				viewHolder.rmk4 = (TextView)row.findViewById(R.id.rmk4_chart_row);
				viewHolder.rmk5 = (TextView)row.findViewById(R.id.rmk5_chart_row);
				viewHolder.rmk6 = (TextView)row.findViewById(R.id.rmk6_chart_row);
			}
			else if (odiv==4) {
				// 투여횟수,단위 없음.
				row = inflater.inflate(R.layout.order_row_odivcd_g, null);
				viewHolder = new ViewHolder();
				viewHolder.odt = (TextView)row.findViewById(R.id.odt_chart_row);
				viewHolder.dcfgnm = (TextView)row.findViewById(R.id.dcfgnm_chart_row);
				viewHolder.ono = (TextView)row.findViewById(R.id.ono_chart_row);
				viewHolder.onm = (TextView)row.findViewById(R.id.onm_chart_row);
				viewHolder.oqty = (TextView)row.findViewById(R.id.oqty_chart_row);
				viewHolder.ounit = null;
				viewHolder.ordcnt = null;
				viewHolder.odaycnt = (TextView)row.findViewById(R.id.odaycnt_chart_row);
				viewHolder.alwfgnm = (TextView)row.findViewById(R.id.alwfgnm_chart_row);
				viewHolder.ostscd = (TextView)row.findViewById(R.id.ostscd_chart_row);
				viewHolder.exdrnm = (TextView)row.findViewById(R.id.exdrnm_chart_row);
				viewHolder.prnfg = (TextView)row.findViewById(R.id.prnfg_chart_row);
				viewHolder.odivcdnm = (TextView)row.findViewById(R.id.odivcdnm_chart_row);
				viewHolder.rmk1 = (TextView)row.findViewById(R.id.rmk1_chart_row);
				viewHolder.rmk2 = (TextView)row.findViewById(R.id.rmk2_chart_row);
				viewHolder.rmk3 = (TextView)row.findViewById(R.id.rmk3_chart_row);
				viewHolder.rmk4 = (TextView)row.findViewById(R.id.rmk4_chart_row);
				viewHolder.rmk5 = (TextView)row.findViewById(R.id.rmk5_chart_row);
				viewHolder.rmk6 = (TextView)row.findViewById(R.id.rmk6_chart_row);
			}
			else {
				row = inflater.inflate(R.layout.order_row, null);
				viewHolder = new ViewHolder();
				viewHolder.odt = (TextView)row.findViewById(R.id.odt_chart_row);
				viewHolder.dcfgnm = (TextView)row.findViewById(R.id.dcfgnm_chart_row);
				viewHolder.ono = (TextView)row.findViewById(R.id.ono_chart_row);
				viewHolder.onm = (TextView)row.findViewById(R.id.onm_chart_row);
				viewHolder.oqty = (TextView)row.findViewById(R.id.oqty_chart_row);
				viewHolder.ounit = (TextView)row.findViewById(R.id.ounit_chart_row);
				viewHolder.ordcnt = (TextView)row.findViewById(R.id.ordcnt_chart_row);
				viewHolder.odaycnt = (TextView)row.findViewById(R.id.odaycnt_chart_row);
				viewHolder.alwfgnm = (TextView)row.findViewById(R.id.alwfgnm_chart_row);
				viewHolder.ostscd = (TextView)row.findViewById(R.id.ostscd_chart_row);
				viewHolder.exdrnm = (TextView)row.findViewById(R.id.exdrnm_chart_row);
				viewHolder.prnfg = (TextView)row.findViewById(R.id.prnfg_chart_row);
				viewHolder.odivcdnm = (TextView)row.findViewById(R.id.odivcdnm_chart_row);
				viewHolder.rmk1 = (TextView)row.findViewById(R.id.rmk1_chart_row);
				viewHolder.rmk2 = (TextView)row.findViewById(R.id.rmk2_chart_row);
				viewHolder.rmk3 = (TextView)row.findViewById(R.id.rmk3_chart_row);
				viewHolder.rmk4 = (TextView)row.findViewById(R.id.rmk4_chart_row);
				viewHolder.rmk5 = (TextView)row.findViewById(R.id.rmk5_chart_row);
				viewHolder.rmk6 = (TextView)row.findViewById(R.id.rmk6_chart_row);
			}
			
			row.setTag(viewHolder);
		}else{
			viewHolder = (ViewHolder)row.getTag();
		}

		// 초기화
		if(viewHolder.odt!=null) viewHolder.odt.setText("");
		if(viewHolder.dcfgnm!=null) viewHolder.dcfgnm.setText("");
		if(viewHolder.ono!=null) viewHolder.ono.setText("");
		if(viewHolder.onm!=null) viewHolder.onm.setText("");
		if(viewHolder.oqty!=null) viewHolder.oqty.setText("");
		if(viewHolder.ounit!=null) viewHolder.ounit.setText("");
		if(viewHolder.ordcnt!=null) viewHolder.ordcnt.setText("");
		if(viewHolder.odaycnt!=null) viewHolder.odaycnt.setText("");
		if(viewHolder.alwfgnm!=null) viewHolder.alwfgnm.setText("");
		if(viewHolder.ostscd!=null) viewHolder.ostscd.setText("");
		if(viewHolder.exdrnm!=null) viewHolder.exdrnm.setText("");
		if(viewHolder.prnfg!=null) viewHolder.prnfg.setText("");
		if(viewHolder.odivcdnm!=null) viewHolder.odivcdnm.setText("");
		if(viewHolder.rmk1!=null) viewHolder.rmk1.setText("");
		if(viewHolder.rmk2!=null) viewHolder.rmk2.setText("");
		if(viewHolder.rmk3!=null) viewHolder.rmk3.setText("");
		if(viewHolder.rmk4!=null) viewHolder.rmk4.setText("");
		if(viewHolder.rmk5!=null) viewHolder.rmk5.setText("");
		if(viewHolder.rmk6!=null) viewHolder.rmk6.setText("");
		// 색 초기화
		//row.setBackgroundColor(this.context.getResources().getColor(R.color.background));

		// 처방일자(ODT)
		viewHolder.odt.setText((String)map.get("odt"));
		viewHolder.odt.setVisibility(View.GONE); // 일단 안보이게 처리한다.
		// dc여부
		viewHolder.dcfgnm.setText(getDcfgNm(dcfg));
		// 처방번호(ONO)
		String onoString=(String)map.get("ono");
		if(!onoString.equals("")) onoString+=".";
		viewHolder.ono.setText(onoString);
		// 처방명(ONM)
		viewHolder.onm.setText((String)map.get("onm"));
		
		if (odiv==1) {
			// 날자구분선
			row.setBackgroundColor(this.context.getResources().getColor(R.color.rowdivbackground));
			viewHolder.onm.setTextColor(this.context.getResources().getColor(R.color.foreground));
		}
		else if (odiv==2) {
			// D:식사 E: S:메시지 A:입원지시
			// 수가명만
		}
		else if (odiv==3) {
			// T:처치 Lxx:임상병리검사 R:방사선 O:물리치료 C:협의진료
			// 투여량,단위 없음.
			// 횟수(ORDCNT)
			viewHolder.ordcnt.setText((String)map.get("ordcnt"));
			// 일수(ODAYCNT)
    		viewHolder.odaycnt.setText((String)map.get("odaycnt"));
		}
		else if (odiv==4) {
			// G:치료재료
			// 투여횟수,단위 없음
			// 투여량(OQTY)
			viewHolder.oqty.setText((String)map.get("oqty"));
			// 일수(ODAYCNT)
    		viewHolder.odaycnt.setText((String)map.get("odaycnt"));
		}
		else {
			// 투여량(OQTY)
			viewHolder.oqty.setText((String)map.get("oqty"));
			// 투여단위(OUNIT)
			viewHolder.ounit.setText((String)map.get("ounit"));
			// 횟수(ORDCNT)
			viewHolder.ordcnt.setText((String)map.get("ordcnt"));
			// 일수(ODAYCNT)
    		viewHolder.odaycnt.setText((String)map.get("odaycnt"));
		}
		// 급여구분
		viewHolder.alwfgnm.setText(alwfgnm);
		// 처방상태
		viewHolder.ostscd.setText((String)map.get("ostscd"));
		// 처방의
		viewHolder.exdrnm.setText(exdrnm);
		// prn처방여부
		viewHolder.prnfg.setText(prnfgnm);
		// 2번째 줄 -------------------------------------------------------
		// 처방종류
		viewHolder.odivcdnm.setText(odivcdnm);
		
		if(odivcd.equals("DATE_LINE")){
			// 일자구분선에는 필요없음.
			viewHolder.alwfgnm.setVisibility(View.GONE);
			viewHolder.ostscd.setVisibility(View.GONE);
			viewHolder.exdrnm.setVisibility(View.GONE);
			viewHolder.odivcdnm.setVisibility(View.GONE);
		}

		// 리마크(RMK1)
		viewHolder.rmk1.setText((String)map.get("rmk1"));
		viewHolder.rmk2.setText((String)map.get("rmk2"));
		viewHolder.rmk3.setText((String)map.get("rmk3"));
		viewHolder.rmk4.setText((String)map.get("rmk4"));
		viewHolder.rmk5.setText((String)map.get("rmk5"));
		viewHolder.rmk6.setText((String)map.get("rmk6"));
		// 리마크가 없으면 리마크제거
		if (((String)map.get("rmk1")).equals("")) viewHolder.rmk1.setVisibility(View.GONE);    			
		if (((String)map.get("rmk2")).equals("")) viewHolder.rmk2.setVisibility(View.GONE);    			
		if (((String)map.get("rmk3")).equals("")) viewHolder.rmk3.setVisibility(View.GONE);    			
		if (((String)map.get("rmk4")).equals("")) viewHolder.rmk4.setVisibility(View.GONE);    			
		if (((String)map.get("rmk5")).equals("")) viewHolder.rmk5.setVisibility(View.GONE);    			
		if (((String)map.get("rmk6")).equals("")) viewHolder.rmk6.setVisibility(View.GONE);

		// dc처방이면 처방명에 줄가게 처리
		if(dcfg.equals("1")||dcfg.equals("3")){
			viewHolder.onm.setPaintFlags(viewHolder.onm.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
		}else{
			// strike thru 취소
			viewHolder.onm.setPaintFlags(viewHolder.onm.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
		}



		return row;
	}
	// 처방종류(odivcd)에 따라 모양이 약간씩 다름.
    private int getODiv(String odivcd) {
    	int odiv=0;
		if (odivcd.equals("DATE_LINE")) {
			// 일자구분선임.
			odiv=1;
		}else if (odivcd.equals("D") || odivcd.equals("E") || odivcd.equals("A")|| odivcd.equals("S")) {
			// D:식사 E: S:메시지 A:입원지시
			// 수가명만 있음.
			odiv=2;
		}else if (odivcd.equals("T") || odivcd.equals("R") || odivcd.equals("Q") || odivcd.equals("O") || odivcd.equals("C") || odivcd.startsWith("L")) {
			// T:처치 Lxx:임상병리검사 R:방사선 Q:기능검사 O:물리치료 C:협의진료
			// 투여량,단위 없음.
			odiv=3;
		}else if (odivcd.equals("G")) {
			// G:치료재료
			// 투여횟수,단위 없음.
			odiv=4;
		}else {
			odiv=0;
		}
		return odiv;
    }
	// 처방종류 명칭
    /*
    private String getOdivcdNm(String odivcd) {
    	String odivcdnm=odivcd;
		if (odivcd.equals("A")) {
			odivcdnm = "입원지시";
		}else if (odivcd.equals("B")) {
			odivcdnm = "혈액";
		}else if (odivcd.equals("C")) {
			odivcdnm = "협의진료";
		}else if (odivcd.equals("D")) {
			odivcdnm = "식이";
		}else if (odivcd.equals("G")) {
			odivcdnm = "치료재료";
		}else if (odivcd.startsWith("L")) {
			odivcdnm = "진단검사";
		}else if (odivcd.equals("MF")) {
			odivcdnm = "외용약";
		}else if (odivcd.equals("MI")) {
			odivcdnm = "주사약";
		}else if (odivcd.equals("MO")) {
			odivcdnm = "먹는약";
		}else if (odivcd.equals("O")) {
			odivcdnm = "재활치료";
		}else if (odivcd.equals("Q")) {
			odivcdnm = "기능검사";
		}else if (odivcd.equals("R")) {
			odivcdnm = "영상진단";
		}else if (odivcd.equals("S")) {
			odivcdnm = "메시지";
		}else if (odivcd.equals("T")) {
			odivcdnm = "처치";
		}else if (odivcd.equals("X")) {
			odivcdnm = "진단서/동의서";
		}
		return odivcdnm;
    }
    */
	// 급여구분 명칭
    /*
    private String getAlwfgNm(String alwfg) {
    	String alwfgnm=alwfg;
    	if(alwfg.equals("0")) {
    		alwfgnm="급여";
    	}else if(alwfg.equals("1")) {
    		alwfgnm="비급";
    	}else if(alwfg.equals("2")) {
    		alwfgnm="비보";
    	}else if(alwfg.equals("4")) {
    		alwfgnm="백";
    	}
    	return alwfgnm;
    }
    */
	// dc구분 명칭
    private String getDcfgNm(String dcfg) {
    	String dcfgnm=dcfg;
    	if(dcfg.equals("0")) {
    		dcfgnm="";
    	}else if(dcfg.equals("1")) {
    		dcfgnm="dc";
    	}else if(dcfg.equals("3")) {
    		dcfgnm="반";
    	}
    	return dcfgnm;
    }

    /***
     * 
     *
     */
    class ViewHolder{
    	TextView odt;
    	TextView dcfgnm;
    	TextView ono;
    	TextView onm;
    	TextView oqty;
    	TextView ounit;
    	TextView ordcnt;
    	TextView odaycnt;
    	TextView alwfgnm;
    	TextView ostscd;
    	TextView exdrnm;
    	TextView prnfg;
    	TextView odivcdnm;
    	TextView rmk1;
    	TextView rmk2;
    	TextView rmk3;
    	TextView rmk4;
    	TextView rmk5;
    	TextView rmk6;
    }
}
