package com.metrosoft.smart.emr.emrdroid.gt101.z_notused;

//<!-- 갤럭시 탭 7인치 용으로 개발된 소스임 -->
//<!-- 갤럭시 탭 10.1인치 용으로 수정하므로 소스를 변경하여야하는데 -->
//<!-- 만일을 위하여 기존 소스를 백업하여둠. -->

import java.util.ArrayList;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.Order;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.TprEnter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.content.Intent;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.DashPathEffect;

import android.view.View;
import android.view.MotionEvent;

import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import android.app.Dialog;

public class Tpr_backup extends MyActivity {
	protected static final int TPR_ENTER = 0;

	private String mXmlPatientInfo,mXmlTpr;

	private String mPid;
	private String mBededt;
	private String mBdiv;

	private ResultSetHelper mRs=null;
	private TprGraphicsView mTprView;
	private int mColorType=0;
	private String patientInfo="";

	public void onCreate(Bundle savedInstanceState) {
		Intent intent = getIntent();
		mPid = intent.getStringExtra("pid");
		mBededt = intent.getStringExtra("bededt");
		mBdiv = intent.getStringExtra("bdiv");
		if (mBdiv==null) mBdiv="2"; // 1.외래 2.입원 3.응급  기본 입원

		String fromTitle = intent.getStringExtra("fromtitle");
		if(fromTitle==null) fromTitle="";
		if(fromTitle.equals("")) fromTitle="닫기";

		LinearLayout linear = (LinearLayout)View.inflate(this, R.layout.tpr, null);
		super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, linear, "< " + fromTitle);
		super.setButton1(true,"등록",BUTTON_TYPE_EDIT);
		super.setButton2(true,"옵션",BUTTON_TYPE_OPTION);
		super.setLinkButton1(true,"처방조회");

		mTprView = new TprGraphicsView(this);
		linear.addView(mTprView);

		// 버튼 리스너
		setListener();

		if (savedInstanceState==null) {
			getPatientInfoAndTpr();
		}
		else {
			mXmlPatientInfo=savedInstanceState.getString("xmlPatientInfo");
			mXmlTpr=savedInstanceState.getString("xmlTpr");
			afterGetPatientInfo();
			afterGetTpr();
		}
	}

	@Override
	protected void onActivityResult(int requestCode,int resultCode,Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode==TPR_ENTER) {
			if (resultCode==RESULT_OK) {
				getTpr();
			}
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("xmlPatientInfo", mXmlPatientInfo);
		outState.putString("xmlTpr", mXmlTpr);
	}

	@Override
	public void onClickQueryButton(View v) {
		getTpr();
	}

	@Override
	public void onClickButton1(View v) {
		// 등록
		if(mBdiv.equals("2")==false){
			super.showSimpleDialog("입원환자만 사용 가능합니다.");
			return;
		}
		Intent intent;
		intent = new Intent(Tpr_backup.this, TprEnter.class);
		intent.putExtra("pid", mPid);
		intent.putExtra("bededt", mBededt);
		startActivityForResult(intent,TPR_ENTER);
	}

	@Override
	public void onClickButton2(View v) {
		// 옵션
		if(mBdiv.equals("2")==false){
			super.showSimpleDialog("입원환자만 사용 가능합니다.");
			return;
		}
		ColorDialog colorDialog = new ColorDialog(Tpr_backup.this);
		colorDialog.setTitle(getString(R.string.select_color));
		colorDialog.show();
	}

	@Override
	public void onClickLinkButton1(View v) {
		// 처방등록화면 호출
		Intent intent;
		intent = new Intent(Tpr_backup.this, Order.class);
		intent.putExtra("pid", mPid);
		intent.putExtra("bededt", mBededt);
		intent.putExtra("bdiv", mBdiv);
		startActivity(intent);
		finish();
	}

	private void setListener() {
		((RadioGroup)findViewById(R.id.kind)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				// TODO Auto-generated method stub
				mTprView.init();
				mTprView.invalidate();
			}
		});
	}

	public int getCheckedId() {
		// 0:체온 1:맥박
		int checkedId=0;
		/*
		switch (((RadioGroup)findViewById(R.id.kind)).getCheckedRadioButtonId()) {
		case R.id.temp:
			checkedId=0;
			break;
		case R.id.bp:
			checkedId=1;
			break;
		}
		*/
		int buttonId=((RadioGroup)findViewById(R.id.kind)).getCheckedRadioButtonId();
		if(buttonId==R.id.temp){
			checkedId=0;
		}else if(buttonId==R.id.bp){
			checkedId=1;
		}
		return checkedId;
	}

	private void getPatientInfoAndTpr() {
		mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
		new Thread(new Runnable() {
			public void run() {
				String hospitalId=getHospitalId();
				String userId=getUserId();
				String url="";
				// 환자정보
				if(mXmlPatientInfo==null) mXmlPatientInfo="";
				if(mXmlPatientInfo.equals("")) {
					url = "InPatientInformationServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt + "&bdiv=" + mBdiv;
					mXmlPatientInfo = getXml(url);
				}
				// tpr - 입원환자만 가능
				if(mBdiv.equals("2")){
					url = "TprServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt;
					mXmlTpr = getXml(url);
				}else{
					mXmlTpr="";
				}

				mHandler.post(new Runnable() {
					public void run() {
						// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
						// 이를 방지함.
						try {
							afterGetPatientInfo();
							afterGetTpr();
							mDialog.dismiss();
						}catch(Exception e) {
							;
						}
					}
				});
			}
		}).start();;
	}
	private void getTpr() {
		mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
		new Thread(new Runnable() {
			public void run() {
				String hospitalId=getHospitalId();
				String userId=getUserId();
				try {
					// tpr - 입원환자만 가능
					if(mBdiv.equals("2")){
						String url = "TprServlet?hospitalid=" + hospitalId + "&pid=" + mPid + "&bededt=" + mBededt;
						mXmlTpr = getXml(url);
					}else{
						mXmlTpr="";
					}
				}catch(Exception e) {
					Toast.makeText(Tpr_backup.this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
					return;
				}
				mHandler.post(new Runnable() {
					public void run() {
						// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
						// 이를 방지함.
						try {
							afterGetTpr();
							mDialog.dismiss();
						}catch(Exception e) {
							;
						}
					}
				});
			}
		}).start();;
	}
	private void afterGetPatientInfo() {
		((TextView)findViewById(R.id.patientInfoTextView)).setText(mXmlPatientInfo);
	}
	private void afterGetTpr() {
		try {
			if(mBdiv.equals("2")==false){
				super.showSimpleDialog("입원환자만 조회 가능합니다.");
				return;
			}
			// 오류발생
			if(super.getXmlError()==true) {
				super.showToastText(super.getXmlErrorMessage());
				return;
			}
			mRs = new ResultSetHelper(mXmlTpr,EmrSettingsUtil.getMaskYn(getBaseContext()));
			if (mRs.getReturnCode()<0) {
				showSimpleDialog(mRs.getReturnDesc());
			}
			else if (mRs.getReturnCode()==0) {
				showSimpleDialog(R.string.no_data_message);
			}
		}
		catch(Exception ex) {
			mRs=null;
			Toast.makeText(Tpr_backup.this, ex.getMessage(), Toast.LENGTH_LONG).show();
		}
		// 다시 그리기
		mTprView.init();
		mTprView.invalidate();
	}

	public class TprGraphicsView extends View {
		// 색 정의
		private int backgroundColor;
		private int lineColor;
		private int textColor;
		private int dateLineColor;
		private int tmpColor;
		private int bpColor;
		private int prColor;
		private int rrColor;
		private int importantLineColor;

		// 페인트객체
		private Paint thickLine; // 두꺼운 줄
		private Paint normalLine; // 보통 두께 줄
		private Paint dotLine; // 점선
		private Paint thickPoint; // 점
		private Paint importantLine; // 두꺼운 빨간색 줄
		private Paint dateLine; // 날짜 변경선
		private Paint normalText; // 글씨
		private Paint tmpPoint; // 온도 표시점
		private Paint tmpLine; // 온도 연결선
		private Paint bpLine; // 혈압 연결선
		private Paint prPoint; // 맥박 표시점
		private Paint prLine; // 맥박 연결선
		private Paint rrPoint; // 호흡 표시점
		private Paint rrLine; // 호흡 연결선

		// 화면 스크롤 용
		private float startX=0;
		private float startY=0;
		private int orientation=0;
		private boolean isPort=true;
		private int showColumnCount=17;
		private int startColNo=0; // 맨 외쪽에 보여질 인덱스
		private int startRowNo=0; // 0.체온 1.혈압등

		// canvas 좌표정보
		private float patientInfoCanvasTop = 0;
		private float patientInfoCanvasHeight = 0;
		private float dateCanvasTop = 0;
		private float dateCanvasHeight = 0;
		private float timeCanvasTop = 0;
		private float timeCanvasHeight = 0;
		private float tmpCanvasTop = 0;
		private float tmpCanvasHeight = 0;
		private float bpCanvasTop = 0;
		private float bpCanvasHeight = 0;
		private float canvasWidth = 0;

		public TprGraphicsView(Context context) {
			super(context);

			// 색을 변경해보자
			setColors(0);
			setPaints();

			startX=0;
			startY=0;
			orientation=0;
			isPort=true;
			showColumnCount=17;
			startColNo=0;
			startRowNo=0;

		}

		public void init() {
			startColNo=0;
			startRowNo=0;
		}

		public void setPaints() {
			// 두꺼운 줄
			thickLine = new Paint();
			thickLine.setStrokeWidth(2);
			thickLine.setStyle(Paint.Style.STROKE);
			thickLine.setColor(lineColor);
			thickLine.setAntiAlias(true);
			// 보통 두께 줄
			normalLine = new Paint();
			normalLine.setStrokeWidth(1);
			normalLine.setStyle(Paint.Style.STROKE);
			normalLine.setColor(lineColor);
			normalLine.setAntiAlias(true);
			// 점선
			dotLine = new Paint();
			dotLine.setStrokeWidth(1);
			dotLine.setStyle(Paint.Style.STROKE);
			dotLine.setColor(lineColor);
			dotLine.setAntiAlias(true);
			dotLine.setPathEffect(new DashPathEffect(new float[] {5, 5}, 0));
			// 점
			thickPoint = new Paint();
			thickPoint.setStrokeWidth(5);
			thickPoint.setStyle(Paint.Style.STROKE);
			thickPoint.setColor(lineColor);
			thickPoint.setAntiAlias(true);
			// 두꺼운 빨간색 줄
			importantLine = new Paint();
			importantLine.setStrokeWidth(2);
			importantLine.setStyle(Paint.Style.STROKE);
			importantLine.setColor(importantLineColor);
			importantLine.setAntiAlias(true);
			// 날짜 변경선
			dateLine = new Paint();
			dateLine.setStrokeWidth(2);
			dateLine.setStyle(Paint.Style.STROKE);
			dateLine.setColor(dateLineColor);
			dateLine.setAntiAlias(true);
			// 글씨
			normalText = new Paint();
			normalText.setStrokeWidth(1);
			normalText.setStyle(Paint.Style.FILL);
			normalText.setColor(textColor);
			normalText.setAntiAlias(true);
			// 온도 표시점
			tmpPoint = new Paint();
			tmpPoint.setStrokeWidth(5);
			tmpPoint.setStyle(Paint.Style.STROKE);
			tmpPoint.setColor(tmpColor);
			tmpPoint.setAntiAlias(true);
			// 온도 연결선
			tmpLine = new Paint();
			tmpLine.setStrokeWidth(2);
			tmpLine.setStyle(Paint.Style.STROKE);
			tmpLine.setColor(tmpColor);
			tmpLine.setAntiAlias(true);
			// 혈압 연결선
			bpLine = new Paint();
			bpLine.setStrokeWidth(2);
			bpLine.setStyle(Paint.Style.STROKE);
			bpLine.setColor(bpColor);
			bpLine.setAntiAlias(true);
			// 맥박 표시점
			prPoint = new Paint();
			prPoint.setStrokeWidth(5);
			prPoint.setStyle(Paint.Style.STROKE);
			prPoint.setColor(prColor);
			prPoint.setAntiAlias(true);
			// 맥박 연결선
			prLine = new Paint();
			prLine.setStrokeWidth(2);
			prLine.setStyle(Paint.Style.STROKE);
			prLine.setColor(prColor);
			prLine.setAntiAlias(true);
			// 호흡 표시점
			rrPoint = new Paint();
			rrPoint.setStrokeWidth(5);
			rrPoint.setStyle(Paint.Style.STROKE);
			rrPoint.setColor(rrColor);
			rrPoint.setAntiAlias(true);
			// 호흡 연결선
			rrLine = new Paint();
			rrLine.setStrokeWidth(2);
			rrLine.setStyle(Paint.Style.STROKE);
			rrLine.setColor(rrColor);
			rrLine.setAntiAlias(true);

		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			// 이 메소드는 뷰가 생성되고 안드로이드가 모든 것의 크기를 파악한 후에 호출된다.
			// orientation = 0 : 세로
			//               1 : 가로
			//               2 : 세로(뒤집힌 세로)
			//               3 : 가로(뒤집힌 가로)
			orientation = getWindowManager().getDefaultDisplay().getOrientation();
			if (orientation==0 || orientation==2) {
				isPort=true;
				showColumnCount=17;
			}
			else {
				isPort=false;
				showColumnCount=31;
			}

			// canvas 좌표정보
			float bottomMargin = 30;
			float rightMargin = 30;
			if (isPort==false) {
				bottomMargin = 20;
				rightMargin = 40;
			}
			patientInfoCanvasTop = 20f;
			patientInfoCanvasHeight = 20f;
			dateCanvasTop = patientInfoCanvasTop + patientInfoCanvasHeight;
			dateCanvasHeight = 80f;
			timeCanvasTop = dateCanvasTop + dateCanvasHeight;
			timeCanvasHeight = 60f;
			if (isPort==true) {
				// 세로
				tmpCanvasTop = timeCanvasTop + timeCanvasHeight;
				tmpCanvasHeight = (getHeight() - tmpCanvasTop - 50)/3;
				bpCanvasTop = tmpCanvasTop + tmpCanvasHeight;
				bpCanvasHeight = getHeight() - bpCanvasTop - bottomMargin;
			}
			else {
				// 가로
				tmpCanvasTop = timeCanvasTop + timeCanvasHeight;
				tmpCanvasHeight = getHeight() - tmpCanvasTop - bottomMargin;
				bpCanvasTop = timeCanvasTop + timeCanvasHeight;
				bpCanvasHeight = getHeight() - bpCanvasTop - bottomMargin;
			}

			canvasWidth = getWidth()-rightMargin;

			super.onSizeChanged(w, h, oldw, oldh);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			super.onTouchEvent(event);

			//if (mRows==null) return true;
			if (mRs==null) return true;
			if (mRs.getReturnCode()<=0) return true;
			if (mRs.getRecordCount()<showColumnCount) return true;

			float xp = event.getX();
			float yp = event.getY();


			// 그래프가 아니고 환자정보가 출력되는 곳에서는 스크롤 되지 않게 막는다.
			if ( yp<=patientInfoCanvasTop + patientInfoCanvasHeight) {
				return true;
			}

			float scrollByX=0;
			float scrollByY=0;

			switch(event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					// 시작점
					startX = event.getRawX();
					startY = event.getRawY();
					break;
				case MotionEvent.ACTION_MOVE:
					// 현재점
					float x = event.getRawX();
					float y = event.getRawY();
					// 움직인 거리
					scrollByX = x - startX;
					scrollByY = y - startY;
					// 어느쪽으로 움직일지
					if (scrollByX<0) {
						// 화면을 찍어서 왼쪽으로 미는 동작임.
						// 오른쪽에 있는 자료가 보여지게 처리한다.
						startColNo += ((scrollByX*-1)/8);
						if (startColNo>mRs.getRecordCount()-3) {
							startColNo=mRs.getRecordCount()-2;
						}
						//if (mStartColNo<0) mStartColNo=0;
					}
					else if (scrollByX>0) {
						// 화면을 찍어서 오른쪽으로 미는 동작임.
						// 왼쪽에 있는 자료가 보여지게 처리한다.
						startColNo -= (scrollByX/8);
						if (startColNo<0) {
							startColNo=0;
						}
					}
					// 다시 시작
					startX = x;
					startY = y;
					// 다시 그리기
					invalidate();
					break;
			}

			return true;
		}

		@Override
		protected void onDraw(Canvas canvas) {

			if (mRs==null) return;
			if (mRs.getReturnCode()<=0) return;

			setBackgroundColor(backgroundColor);

			//JSONObject cols;


			// 체온자료
			ArrayList<PointF> tmpArrayList = new ArrayList<PointF>();
			ArrayList<PointF> prArrayList = new ArrayList<PointF>();
			ArrayList<PointF> rrArrayList = new ArrayList<PointF>();


			// 날짜 변경선
			int[] dayBreak = new int[100];
			for (int i=0 ; i<100 ; i++) {
				dayBreak[i]=0;
			}

			try {
				if (mRs.getReturnCode()<0) {
//	    			Utility.showSimpleDialog(Tpr.this, mRs.getReturnDesc());
				}
				else if (mRs.getReturnCode()==0) {
//	    			Utility.showSimpleDialog(Tpr.this,R.string.no_data_message);
				}
				else {
					startRowNo=getCheckedId();

					//String patientInfo = "";//cols.getString("PATIENT_INFO");
					double maxValue = 0;//cols.getDouble("MAX_VALUE");
					double minValue = 0;//cols.getDouble("MIN_VALUE");

					canvas.translate(20, 0);


					// 환자정보 출력
					canvas.drawText(patientInfo, 0, patientInfoCanvasTop, normalText);

					int xGap = 30;

					// -------------------------------------------------------------------------
					// 일자
					// -------------------------------------------------------------------------

					// 월일
					canvas.drawRect(0, dateCanvasTop, canvasWidth, dateCanvasTop+40, thickLine);
					canvas.drawRect(0, dateCanvasTop, 2*xGap,dateCanvasTop+40,thickLine);
					canvas.drawLine(2*xGap, dateCanvasTop+20, canvasWidth, dateCanvasTop+20, dotLine);
					// HOD
					canvas.drawRect(0, dateCanvasTop+40, canvasWidth, dateCanvasTop+60, thickLine);
					canvas.drawRect(0, dateCanvasTop+40, 2*xGap,dateCanvasTop+60,thickLine);
					// POD
					canvas.drawRect(0, dateCanvasTop+60, canvasWidth, dateCanvasTop+80, thickLine);
					canvas.drawRect(0, dateCanvasTop+60, 2*xGap,dateCanvasTop+80,thickLine);
					// 월일-글자
					canvas.drawText("일자", 20, dateCanvasTop+25, normalText);
					// HOD-글자
					canvas.drawText("HOD", 20, dateCanvasTop+55, normalText);
					// POD-글자
					canvas.drawText("POD", 20, dateCanvasTop+75, normalText);
					// 일자 출력
					String bkChkdt="";
					for (int turn=1 ; turn<=showColumnCount ; turn++) {
						int i=turn+startColNo;
						if (i>mRs.getRecordCount()) {
							// 끝까지 간것이 아니고 , 중간에 종료된 것임.
							canvas.drawLine((turn+1)*xGap, dateCanvasTop+0, (turn+1)*xGap, dateCanvasTop+80, dateLine);
							dayBreak[turn+1]=1;
							break;
						}
						//cols=mRows.getJSONObject(i);
						//
						String chkdt = mRs.getString(i-1,"chkdt");
						String hod = mRs.getString(i-1,"hod");
						String pod = mRs.getString(i-1,"pod");
						if (bkChkdt.equals("") || bkChkdt.equals(chkdt)==false) {
							// 일자
							String month = chkdt.substring(4,6);
							String day = chkdt.substring(6,8);
							float x = (float)(turn+1)*(float)xGap + (xGap/3);
							canvas.drawText(month, x, dateCanvasTop+15, normalText);
							canvas.drawText(day, x, dateCanvasTop+35, normalText);
							canvas.drawText(hod, x, dateCanvasTop+55, normalText);
							canvas.drawText(pod, x, dateCanvasTop+75, normalText);
							if (bkChkdt.equals("")==false) {
								canvas.drawLine((turn+1)*xGap, dateCanvasTop+0, (turn+1)*xGap, dateCanvasTop+80, dateLine);
								//canvas.drawLine((turn+2)*xGap, 0, (turn+2)*xGap, 80, thickLine);
								dayBreak[turn+1]=1;
							}
							bkChkdt=chkdt;
						}
					}

					// ---------------------------------------------------------------------------
					// 시간
					// ---------------------------------------------------------------------------

					// 오전오후
					canvas.drawRect(0, timeCanvasTop+0, canvasWidth, timeCanvasTop+20, thickLine);
					canvas.drawRect(0, timeCanvasTop+0, 2*xGap,timeCanvasTop+20,thickLine);
					// 시분
					canvas.drawRect(0, timeCanvasTop+20, canvasWidth, timeCanvasTop+60, thickLine);
					canvas.drawRect(0, timeCanvasTop+20, 2*xGap,timeCanvasTop+60,thickLine);
					canvas.drawLine(2*xGap, timeCanvasTop+40, canvasWidth, timeCanvasTop+40, dotLine);
					// 오전오후-글자
					canvas.drawText("오전-오후", 5, timeCanvasTop+15, normalText);
					// 시분-글자
					canvas.drawText("시분", 20, timeCanvasTop+45, normalText);
					// 세로줄 출력
					for (float i=2 ; true ; i++) {
						if (i*xGap>canvasWidth) break;
						if (dayBreak[(int)i]==1) {
							canvas.drawLine(i*xGap, timeCanvasTop+0, i*xGap, timeCanvasTop+60, dateLine);
						}
						else {
							canvas.drawLine(i*xGap, timeCanvasTop+0, i*xGap, timeCanvasTop+60, normalLine);
						}
					}
					// 시간출력
					// 데이터 출력
					for (int turn=1 ; turn<=showColumnCount ; turn++) {
						int i=turn+startColNo;
						if (i>mRs.getRecordCount()) break;
						//cols=mRows.getJSONObject(i);

						// 시간
						String chktm = mRs.getString(i-1,"chktm");
						String chkhh = chktm.substring(0,2);
						String chkmm = chktm.substring(2,4);
						String disphh = chkhh; // 출력용
						float x = (float)(turn+1)*(float)xGap + (xGap/3);
						// 오전-오후
						String ampm="A";
						if (chktm.compareTo("1300")>=0 && chktm.compareTo("2359")<=0) {
							try {
								disphh = (Integer.parseInt(chkhh)-12) + "";
							}
							catch(Exception ex) {}
							ampm="P";
						}
						else if (chktm.compareTo("0000")>=0 && chktm.compareTo("0059")<=0) {
							ampm="P";
							disphh="12";
						}
						else {
							try {
								disphh = (Integer.parseInt(chkhh)) + "";
							}
							catch(Exception ex) {}
						}
						if (disphh.length()==1) {
							disphh = " " + disphh;
						}
						canvas.drawText(ampm, x, timeCanvasTop+15, normalText);
						// 시분
						canvas.drawText(disphh, x, timeCanvasTop+35, normalText);
						canvas.drawText(chkmm, x, timeCanvasTop+55, normalText);
					}

					// -------------------------------------------------------------------------
					// 체온
					// -------------------------------------------------------------------------
					if (isPort==true || startRowNo==0) {
						maxValue = 42;
						minValue = 35;

						canvas.drawRect(0, tmpCanvasTop+0, canvasWidth, tmpCanvasTop+tmpCanvasHeight, thickLine);

						// 제목(체온) 출력
						{
							float x = (xGap/3);
							float y = getYPos(39.5, tmpCanvasHeight, minValue, maxValue) + tmpCanvasTop;
							canvas.drawText("체", x, y, normalText);
							y = getYPos(37.5, tmpCanvasHeight, minValue, maxValue) + tmpCanvasTop;
							canvas.drawText("온", x, y, normalText);
						}
						// Y축 좌표값 출력
						for (double i=minValue+1 ; i<maxValue ; i++) {
							float y = getYPos(i, tmpCanvasHeight, minValue, maxValue) + tmpCanvasTop + 4;
							float x = (float)1*(float)xGap + (xGap/3);
							canvas.drawText((int)i + "", x, y, normalText);
						}
						// 가로줄 출력
						for (double i=minValue ; i<=maxValue ; i++) {
							float y = getYPos(i, tmpCanvasHeight, minValue, maxValue) + tmpCanvasTop;
							float x = (float)2*(float)xGap;
							if (i==37) {
								canvas.drawLine(x, y, canvasWidth, y, importantLine);
							}
							else {
								canvas.drawLine(x, y, canvasWidth, y, normalLine);
							}
						}
						// 세로줄 출력
						for (float i=1 ; true ; i++) {
							if (i*xGap>canvasWidth) break;
							float y1 = getYPos(minValue, tmpCanvasHeight, minValue, maxValue) + tmpCanvasTop;
							float y2 = getYPos(maxValue, tmpCanvasHeight, minValue, maxValue) + tmpCanvasTop;
							if (i==2) {
								canvas.drawLine(i*xGap, y1, i*xGap, y2, thickLine);
							}
							else if (dayBreak[(int)i]==1) {
								canvas.drawLine(i*xGap, y1, i*xGap, y2, dateLine);
							}
							else {
								canvas.drawLine(i*xGap, y1, i*xGap, y2, normalLine);
							}
						}
						// 데이터 출력
						for (int turn=1 ; turn<=showColumnCount ; turn++) {
							int i=turn+startColNo;
							if (i>mRs.getRecordCount()) break;
							//cols=mRows.getJSONObject(i);
							// 체온
							String tmp = mRs.getString(i-1,"tmp");
							try {
								float value = Float.parseFloat(tmp);
								float x = ((turn+1)*xGap) + (xGap/2);
								float y = getYPos(value,tmpCanvasHeight,minValue,maxValue) + tmpCanvasTop;
								tmpArrayList.add(new PointF(x,y));
								canvas.drawPoint(x, y, tmpPoint);
							}
							catch(Exception ex) {}
						}
						// 그리자
						drawLineArrayList(tmpArrayList,canvas,tmpLine);
					}

					// -------------------------------------------------------------------------
					// 체온이외
					// -------------------------------------------------------------------------
					if (isPort==true || startRowNo==1) {
						maxValue = 230;
						minValue = 0;

						canvas.drawRect(0, bpCanvasTop+0, canvasWidth, bpCanvasTop+bpCanvasHeight, thickLine);

						// 제목(맥박 및 혈압 호흡) 출력
						{
							float x = (xGap/3);
							float y = 0;
							y = getYPos(200, bpCanvasHeight, minValue, maxValue) + bpCanvasTop;
							canvas.drawText("맥", x, y, normalText);
							y = getYPos(180, bpCanvasHeight, minValue, maxValue) + bpCanvasTop;
							canvas.drawText("박", x, y, normalText);
							y = getYPos(150, bpCanvasHeight, minValue, maxValue) + bpCanvasTop;
							canvas.drawText("및", x, y, normalText);
							y = getYPos(115, bpCanvasHeight, minValue, maxValue) + bpCanvasTop;
							canvas.drawText("혈", x, y, normalText);
							y = getYPos(97, bpCanvasHeight, minValue, maxValue) + bpCanvasTop;
							canvas.drawText("압", x, y, normalText);
							y = getYPos(65, bpCanvasHeight, minValue, maxValue) + bpCanvasTop;
							canvas.drawText("호", x, y, normalText);
							y = getYPos(45, bpCanvasHeight, minValue, maxValue) + bpCanvasTop;
							canvas.drawText("흡", x, y, normalText);
						}
						// Y축 좌표값 출력
						for (double i=minValue+10 ; i<maxValue ; i+=10) {
							float y = getYPos(i, bpCanvasHeight, minValue, maxValue) + bpCanvasTop + 4;
							float x = (float)1*(float)xGap + (xGap/4);
							canvas.drawText((int)i + "", x, y, normalText);
						}
						// 가로줄 출력
						for (double i=minValue ; i<=maxValue ; i+=10) {
							float y = getYPos(i, bpCanvasHeight, minValue, maxValue) + bpCanvasTop;
							float x = (float)2*(float)xGap;
							if (i==90) {
								canvas.drawLine(x, y, canvasWidth, y, importantLine);
							}
							else {
								canvas.drawLine(x, y, canvasWidth, y, normalLine);
							}
						}
						// 세로줄 출력
						for (float i=1 ; true ; i++) {
							if (i*xGap>canvasWidth) break;
							float y1 = getYPos(minValue, bpCanvasHeight, minValue, maxValue) + bpCanvasTop;
							float y2 = getYPos(maxValue, bpCanvasHeight, minValue, maxValue) + bpCanvasTop;
							if (i==2) {
								canvas.drawLine(i*xGap, y1, i*xGap, y2, thickLine);
							}
							else if (dayBreak[(int)i]==1) {
								canvas.drawLine(i*xGap, y1, i*xGap, y2, dateLine);
							}
							else {
								canvas.drawLine(i*xGap, y1, i*xGap, y2, normalLine);
							}
						}
						// 데이터 출력
						for (int turn=1 ; turn<=showColumnCount ; turn++) {
							int i=turn+startColNo;
							if (i>mRs.getRecordCount()) break;
							//cols=mRows.getJSONObject(i);
							// 혈압
							String maxBp = mRs.getString(i-1,"maxbp");
							String minBp = mRs.getString(i-1,"minbp");
							try {
								float maxF = Float.parseFloat(maxBp);
								float minF = Float.parseFloat(minBp);
								float xF = getYPos(maxF,bpCanvasHeight,minValue,maxValue) + bpCanvasTop;
								float nF = getYPos(minF,bpCanvasHeight,minValue,maxValue) + bpCanvasTop;
								float x1 = ((turn+1)*xGap) + (xGap/2);
								float x2 = ((turn+1)*xGap) + (xGap/2) + 1;
								canvas.drawRect(x1, xF, x2, nF, bpLine);
							}
							catch(Exception ex) {}
							// PR
							String pr = mRs.getString(i-1,"pr");
							try {
								float value = Float.parseFloat(pr);
								//float x = i*10;
								float x = ((turn +1)*xGap) + (xGap/2);
								float y = getYPos(value,bpCanvasHeight,minValue,maxValue) + bpCanvasTop;
								prArrayList.add(new PointF(x,y));
								canvas.drawPoint(x, y, prPoint);
							}
							catch(Exception ex) {}
							// RR
							String rr = mRs.getString(i-1,"rr");
							try {
								float value = Float.parseFloat(rr);
								//float x = i*10;
								float x = ((turn+1)*xGap) + (xGap/2);
								float y = getYPos(value,bpCanvasHeight,minValue,maxValue) + bpCanvasTop;
								rrArrayList.add(new PointF(x,y));
								canvas.drawPoint(x, y, rrPoint);
							}
							catch(Exception ex) {}
						}
						// 그리자
						drawLineArrayList(prArrayList,canvas,prLine);
						drawLineArrayList(rrArrayList,canvas,rrLine);
					}
				}
			}
			catch(Exception ex) {
				showSimpleDialog(ex.getMessage());
//	    		canvas.drawText(ex.getMessage(), 50, 10, paint);
			}
		}

		private float getYPos(float value, float canvasHeight, double minValue, double maxValue) {
			float v = (float)maxValue - ((float)value - (float)minValue);
			float f = ((float)canvasHeight * ( (float)v - (float)minValue )) / ((float)maxValue - (float)minValue);
			return f;
		}

		private float getYPos(double value, float canvasHeight, double minValue, double maxValue) {
			return getYPos((float)value, canvasHeight, minValue, maxValue);
		}

		private void drawLineArrayList(ArrayList<PointF> arrayList,Canvas canvas,Paint paint/*,int color*/) {
			for (int i=0 ; i<arrayList.size()-1 ; i++) {
				PointF p1 = (PointF)arrayList.get(i);
				PointF p2 = (PointF)arrayList.get(i+1);
				//paint.setColor(color);
				canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint);
			}
		}

		public void setColors(int colorType) {
			mColorType=colorType;
			switch (colorType) {
				case 0:
					// 기본
					backgroundColor = Color.WHITE;
					lineColor = Color.DKGRAY;
					textColor = Color.DKGRAY;
					dateLineColor = Color.RED;
					tmpColor = Color.RED;
					bpColor = Color.BLUE;
					prColor = Color.RED;
					rrColor = Color.BLACK;
					importantLineColor = Color.RED;
					break;
				case 1:
					// 자정
					backgroundColor = Color.BLACK;
					lineColor = Color.GRAY;
					textColor = Color.GRAY;
					dateLineColor = Color.argb(175, 255, 0, 0);
					tmpColor = Color.RED;
					bpColor = Color.BLUE;
					prColor = Color.RED;
					rrColor = Color.WHITE;
					importantLineColor = Color.RED;
					break;
				case 2:
					// 새벽
					backgroundColor = Color.GRAY;
					lineColor = Color.LTGRAY;
					textColor = Color.LTGRAY;
					dateLineColor = Color.DKGRAY;
					tmpColor = Color.argb(175, 255, 50, 50);
					bpColor = Color.BLUE;
					prColor = Color.RED;
					rrColor = Color.WHITE;
					importantLineColor = Color.RED;
					break;
			}
		}
	}

	protected class ColorDialog extends Dialog {
		RadioGroup colorRadioGroup;
		public ColorDialog(Context context) {
			super(context);

			setContentView(R.layout.tpr_color_dialog);

			colorRadioGroup = (RadioGroup)findViewById(R.id.colorRadioGroup);
			RadioButton radio;
			switch(mColorType) {
				case 0:
					radio = (RadioButton)findViewById(R.id.defaultColorRadio);
					radio.setChecked(true);
					break;
				case 1:
					radio = (RadioButton)findViewById(R.id.midnightColorRadio);
					radio.setChecked(true);
					break;
				case 2:
					radio = (RadioButton)findViewById(R.id.dawnColorRadio);
					radio.setChecked(true);
					break;
			}

			Button applyButton = (Button)findViewById(R.id.color_apply_button);
			applyButton.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View view) {
					int checkedId = colorRadioGroup.getCheckedRadioButtonId();
					int colorValue=0;
    				/*
    				switch (checkedId) {
    				case R.id.defaultColorRadio:
    					colorValue=0;
    					break;
    				case R.id.midnightColorRadio:
    					colorValue=1;
    					break;
    				case R.id.dawnColorRadio:
    					colorValue=2;
    					break;
    				}
    				*/
					if(checkedId==R.id.defaultColorRadio){
						colorValue=0;
					}else if(checkedId==R.id.midnightColorRadio){
						colorValue=1;
					}else if(checkedId==R.id.dawnColorRadio){
						colorValue=2;
					}
					mTprView.setColors(colorValue);
					mTprView.setPaints();
					mTprView.invalidate();
					dismiss();
				}
			});
			Button cancelButton = (Button)findViewById(R.id.color_cancel_button);
			cancelButton.setOnClickListener(new Button.OnClickListener() {
				public void onClick(View view) {
					dismiss();
				}
			});
		}
	}

}
