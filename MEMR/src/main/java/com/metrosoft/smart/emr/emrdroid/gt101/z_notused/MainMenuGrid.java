package com.metrosoft.smart.emr.emrdroid.gt101.z_notused;

import java.util.ArrayList;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.ConsentFormList;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.GetLatestVersionDialog;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.InPatientList;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.Login;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.NoticeList;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.Order;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.TprSheet;
import com.metrosoft.smart.emr.emrdroid.gt101.z_practice.FingerPaint;
import com.metrosoft.smart.emr.emrdroid.gt101.z_practice.MyDynamicTab;
import com.metrosoft.smart.emr.emrdroid.gt101.z_practice.MyStaticTab;
import com.metrosoft.smart.emr.emrdroid.gt101.z_practice.MyTab3;
import com.metrosoft.smart.emr.emrdroid.gt101.z_practice.MyViewFlipper;
import com.metrosoft.smart.emr.emrdroid.gt101.z_practice.QrScan;
import com.metrosoft.smart.emr.emrdroid.gt101.z_practice.TouchPaint;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.Toast;

public class MainMenuGrid extends MyActivity implements OnItemClickListener {
	private String xml;
	private long backKeyClick=0;
	private long backKeyClickTime;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, R.layout.main_menu_grid, "< " + getString(R.string.exit));

		super.setQueryButton(false);

		GridView gridView=(GridView)findViewById(R.id.menuGrid);
		gridView.setOnItemClickListener(this);

		getMenuList();

		// 재원환자리스트를 바로 뛰울지
		SharedPreferences prefs = getSharedPreferences("config", MODE_PRIVATE);
		String firstInPatientList=prefs.getString("first_in_patient_list", "true");
		if (firstInPatientList.equals("true")) {
			startActivity(new Intent(this, InPatientList.class));
		}
	}

	@Override
	public void onClickBackButton(View v) {
		// 종료확인 Dialog 창을 띄워서 처리하는 부분.
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		dialog.setTitle(R.string.exit_dialog_title);
		dialog.setMessage(R.string.exit_dialog_message);
		dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		dialog.setCancelable(false);
		dialog.show();
	}

	@Override
	public void onBackPressed() {
		long currentTime=System.currentTimeMillis();
		final int duration=2000;
		backKeyClick++;
		if(backKeyClick==1) {
			backKeyClickTime=System.currentTimeMillis();
			Toast t=Toast.makeText(this, R.string.exit_confirm_message, Toast.LENGTH_SHORT);
			t.setDuration(Toast.LENGTH_LONG);
			t.show();

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(duration);
					}catch(InterruptedException e) {
						;
					}
					backKeyClick=0;
				}
			}).start();
		}
		else {
//			if(currentTime-backKeyClickTime<=duration) {
			super.onBackPressed();
//			}
//			backKeyClick=0;
		}

	}

	private void getMenuList() {
		// 추후 데이터베이스에서 멘뉴를 가져오게 할 경우를 대비하여 미리 코딩하여놓는다.
		mDialog = ProgressDialog.show(MainMenuGrid.this, "",getString(R.string.query_wait_message), true);
		new Thread(new Runnable() {
			public void run() {
				String hospitalId=getHospitalId();
				String userId=getUserId();
				String url="";
				// 메뉴리스트
				//url = "MainMenuServlet?hospitalid=" + hospitalId + "&userid=" + userId ;
				//xml = servletHelper.getXml(MainMenuGrid.this, url);
				mHandler.post(new Runnable() {
					public void run() {
						afterGetMenuList();
						mDialog.dismiss();
					}
				});
			}
		}).start();;
	}

	private void afterGetMenuList() {
		GridView list=(GridView)findViewById(R.id.menuGrid);

		ArrayList<HashMap<String,Object>> mylist = new ArrayList<HashMap<String,Object>>();
		HashMap<String,Object> map = null;

		// 실제이미지는 getView에서 처리한다.

		// 공지사항
		map = new HashMap<String,Object>();
		map.put("key", "notice");
		map.put("text", getString(R.string.notice));
		map.put("image", "");
		mylist.add(map);
		// 재원환자리스트
		map = new HashMap<String,Object>();
		map.put("key", "inpatientlist");
		map.put("text", getString(R.string.inpatient_list));
		map.put("image", "");
		mylist.add(map);
//    	// 처방조회
//		map = new HashMap<String,Object>();
//		map.put("key", "order");
//		map.put("text", getString(R.string.order));
//		map.put("image", "");
//		mylist.add(map);
//    	// 임상병리결과조회
//		map = new HashMap<String,Object>();
//		map.put("key", "resultlis");
//		map.put("text", getString(R.string.result_lis));
//		map.put("image", "");
//		mylist.add(map);
//    	// 기타서식
//		map = new HashMap<String,Object>();
//		map.put("key", "emrscan");
//		map.put("text", getString(R.string.emr_scan));
//		map.put("image", "");
//		mylist.add(map);
//    	// TPR
//		map = new HashMap<String,Object>();
//		map.put("key", "tpr");
//		map.put("text", getString(R.string.tpr));
//		map.put("image", "");
//		mylist.add(map);
//    	// 증명서
//		map = new HashMap<String,Object>();
//		map.put("key", "certificatelist");
//		map.put("text", getString(R.string.certificate_list));
//		map.put("image", "");
//		mylist.add(map);
//    	// QR코드조회
//		map = new HashMap<String,Object>();
//		map.put("key", "qrscan");
//		map.put("text", getString(R.string.qr_scan));
//		map.put("image", "");
//		mylist.add(map);
		// 환경설정
		map = new HashMap<String,Object>();
		map.put("key", "set_config");
		map.put("text", getString(R.string.set_config));
		map.put("image", "");
		mylist.add(map);
		// 재로그인
		map = new HashMap<String,Object>();
		map.put("key", "logoff");
		map.put("text", getString(R.string.logoff));
		map.put("image", "");
		mylist.add(map);
		// ----------------------------------------
		// 개발자 테스트 용
		// ----------------------------------------
		if(super.isTestDeveloper()==true) {
			// 탭
			map = new HashMap<String,Object>();
			map.put("key", "my_static_tab");
			map.put("text", getString(R.string.my_static_tab));
			map.put("image", "");
			mylist.add(map);
			// 탭
			map = new HashMap<String,Object>();
			map.put("key", "my_dynamic_tab");
			map.put("text", getString(R.string.my_dynamic_tab));
			map.put("image", "");
			mylist.add(map);
			// 플립
			map = new HashMap<String,Object>();
			map.put("key", "my_view_flipper");
			map.put("text", getString(R.string.my_view_flipper));
			map.put("image", "");
			mylist.add(map);
			// 플립
			map = new HashMap<String,Object>();
			map.put("key", "my_tab3");
			map.put("text", getString(R.string.my_tab3));
			map.put("image", "");
			mylist.add(map);
			// 그림판
			map = new HashMap<String,Object>();
			map.put("key", "touchpaint");
			map.put("text", "TouchPaint");
			map.put("image", "");
			mylist.add(map);
			// 그림판
			map = new HashMap<String,Object>();
			map.put("key", "fingerpaint");
			map.put("text", "FingerPaint");
			map.put("image", "");
			mylist.add(map);
		}
		// adapter 연결
		MainMenuGridAdapter adapter = new MainMenuGridAdapter(this,mylist);
		list.setAdapter(adapter);

	}

	protected class MainMenuGridAdapter extends BaseAdapter {
		private Context context;
		ArrayList<HashMap<String,Object>> menuList;

		public MainMenuGridAdapter(Context context,ArrayList<HashMap<String,Object>> menuList) {
			this.context=context;
			this.menuList=menuList;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return this.menuList.size();
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return this.menuList.get(position);
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			View row;
			HashMap<String,Object> map = this.menuList.get(position);

			if (convertView==null) {
				LayoutInflater inflater = ((Activity)this.context).getLayoutInflater();
				row = inflater.inflate(R.layout.main_menu_grid_row, null);
			}
			else {
				row = convertView;
			}

			// 그림의 모서리를 둥들게 해보자.
			//Resources res = context.getResources();
			//Bitmap bitmap = BitmapFactory.decodeResource(res,R.drawable.patient_list_button_icon);
			//Bitmap roundedBitmap = getRoundedCornerBitmap(bitmap);
			//((ImageView)row.findViewById(R.id.image)).setImageBitmap(roundedBitmap);

			//((ImageView)row.findViewById(R.id.image)).setImageResource((Integer)map.get("image"));
    		/*
    		String key=(String)map.get("key");
    		if (key.equals("logoff")) {
    			((ImageView)row.findViewById(R.id.image)).setImageResource(R.drawable.bright_ball_logoff);
    		}
    		else if (key.equals("set_config")) {
    			((ImageView)row.findViewById(R.id.image)).setImageResource(R.drawable.set_config);
    		}
    		else if (key.equals("notice")) {
    			((ImageView)row.findViewById(R.id.image)).setImageResource(R.drawable.notice);
    		}
    		else {
    			((ImageView)row.findViewById(R.id.image)).setImageResource(R.drawable.patient_list_button_icon);
    		}
    		((TextView)row.findViewById(R.id.text)).setText((String)map.get("text"));
    		*/

			return row;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
		// TODO Auto-generated method stub
		HashMap<String,Object> selectedMap=(HashMap<String,Object>)parent.getAdapter().getItem(position);
		String key=(String)selectedMap.get("key");
		if (key.equals("notice")) {
			startActivity(new Intent(this, NoticeList.class));
		}
		else if (key.equals("inpatientlist")) {
			startActivity(new Intent(this, InPatientList.class));
		}
		else if (key.equals("order")) {
			startActivity(new Intent(this, Order.class));
		}
		else if (key.equals("resultlis")) {
			startActivity(new Intent(this, ResultLis.class));
		}
		else if (key.equals("emrscan")) {
			startActivity(new Intent(this, EmrScan.class));
		}
		else if (key.equals("tpr")) {
			startActivity(new Intent(this, TprSheet.class));
		}
		else if (key.equals("certificatelist")) {
			startActivity(new Intent(this, ConsentFormList.class));
		}
		else if (key.equals("qrscan")) {
			startActivity(new Intent(this, QrScan.class));
		}
		else if (key.equals("set_config")) {
			startActivity(new Intent(this, GetLatestVersionDialog.class));
		}
		else if (key.equals("logoff")) {
			finish();
			startActivity(new Intent(this, Login.class));
		}
		// --------------------------------------------------
		// 개발자용
		// --------------------------------------------------
		else if (key.equals("my_static_tab")) {
			startActivity(new Intent(this, MyStaticTab.class));
		}
		else if (key.equals("my_dynamic_tab")) {
			startActivity(new Intent(this, MyDynamicTab.class));
		}
		else if (key.equals("my_view_flipper")) {
			startActivity(new Intent(this, MyViewFlipper.class));
		}
		else if (key.equals("my_tab3")) {
			startActivity(new Intent(this, MyTab3.class));
		}
		else if (key.equals("touchpaint")) {
			startActivity(new Intent(this, TouchPaint.class));
		}
		else if (key.equals("fingerpaint")) {
			startActivity(new Intent(this, FingerPaint.class));
		}
	}

	public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),bitmap.getHeight(),Config.ARGB_8888);
		Canvas canvas = new Canvas(output);
		final int color = 0xff424242;
		final Paint paint = new Paint();
		final Rect rect = new Rect(0,0,bitmap.getWidth(),bitmap.getHeight());
		final RectF rectF = new RectF(rect);
		final float roundPx = 12;
		paint.setAntiAlias(true);
		canvas.drawARGB(0,0,0,0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF,roundPx,roundPx,paint);
		paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap,rect,rect,paint);
		return output;
	}
}
