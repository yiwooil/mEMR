package com.metrosoft.smart.emr.emrdroid.gt101.view;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.metrosoft.smart.emr.emrdroid.gt101.activity.ConsentForm;
import com.metrosoft.smart.emr.emrdroid.gt101.data.CcfValue;
import com.metrosoft.smart.emr.emrdroid.gt101.data.CcfValues;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Utils;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Paint.Style;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.ImageView;
import android.widget.Toast;

public class FingerPaintView3 extends android.support.v7.widget.AppCompatImageView implements OnTouchListener { // 원래는extends View 였음.
	public static final int MODE_NONE = 0;
	public static final int MODE_PEN = 1;
	public static final int MODE_ERASER = 2;
	
	private static final float MINP = 0.25f;
    private static final float MAXP = 0.75f;
    
    private Context mContext;
    private CcfValues mCcfValues;
    private Bitmap mFirstBitmap;
    private Bitmap mSignBitmap;
	private Bitmap mLoginDrSignBitmap; // 2024.05.30 WOOIL - 로그인의사 사인(>주치의)
    private Bitmap  mBitmap;
    private Canvas  mCanvas;
    private Paint   mBitmapPaint;
    
    //private Bitmap mTextBitmap;
    //private Canvas mTextCanvas;

    private Paint mPaint;
    private Paint mClearPaint;
    private Paint mCursorPaint;
	private Paint mTextPaint;

    private int mPenMode;
    //private int mPenColor; // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
    //private float mPenWidth;
    //private float mEraserWidth;
    
    private String mUserId;
    
    private int mCountDrawn = 0; // 2020.01.30
    
    // 이하 확대 축속 관련
    private float MAX_SCALE = 2f;
    protected Matrix mMatrix = new Matrix(); // 2024.06.28 WOOIL - 이곳에서 NEW
    private final float[] mMatrixValues = new float[9];
    private int mWidth;
    private int mHeight;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;
    private float mPrevDistance;
//    private boolean isScaling;
    private float mPrevMoveX;
    private float mPrevMoveY;
//    private GestureDetector mDetector;
    private float mScale;
    private float mMinScale;
    
    private float mInitScale;
    private float mInitTranX;
    private float mInitTranY;
    
    private int mPicWidth;
    private int mPicHeight;
	private float mShrinkRate;

    private int mFingerAction;
    public static final int ACTION_NOTHING = 0;
    public static final int ACTION_SCALING = 1;
    public static final int ACTION_DRAWING = 2;

	private boolean mFrameInitialized = false; // 2026.03.10 WOOIL - 확대 축소된 값 보존용
	private boolean mUserZoomChanged = false; // 2026.03.10 WOOIL - 확대 축소된 값 보존용
	private boolean mInFrameInit = false; // 2026.03.10 WOOIL - 확대 축소된 값 보존용
	private float mBaseTextSize; // 2026.03.25 WOOIL - 글자의 크기를 확대. 축소하기 위한 변수

	// CcfValue 통지용
	public interface OnCcfValueChangedListener {
		void onCcfValueChanged(int index, CcfValue value);
	}

	private OnCcfValueChangedListener mOnCcfValueChangedListener;

	public void setOnCcfValueChangedListener(OnCcfValueChangedListener listener) {
		this.mOnCcfValueChangedListener = listener;
	}

	public FingerPaintView3(Context c, int penWidth, int eraserWidth, int penColor, Paint penPaint, Paint clearPaint, Paint cursorPaint, Paint textPaint, String userId) {
        super(c);
        Log.d("EmrDroid","FingerPaintView3 FingerPaintView3");
        
        
        mContext = c;
        //mPenWidth = penWidth;
        //mEraserWidth = eraserWidth;
		//mPenColor = penColor; // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
		mUserId = userId; // 2021.11.19 WOOIL - 특별한 사용자ID 처리를 위해
        
        //initPen(); // <-- 2024.05.23 WOOIL - 막음.

        mPaint = penPaint; // 2024.05.23 WOOIL - 넘겨 받아서 사용한다.
        mClearPaint = clearPaint; // 2024.05.23 WOOIL - 넘겨 받아서 사용한다.
        mCursorPaint = cursorPaint; // 2024.05.23 WOOIL - 넘겨 받아서 사용한다.
        mTextPaint = textPaint; // 2024.05.23 WOOIL - 넘겨 받아서 사용한다.
		mBaseTextSize = mTextPaint.getTextSize(); // 2026.03.25 WOOIL - 글자를 확대. 축소하기 위함.

        mBitmap = null;
        mCanvas = null;
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        
        mPenMode = MODE_NONE;
        mFingerAction = ACTION_NOTHING;
        
        mInitScale=1;
        mInitTranX=0;
        mInitTranY=0;

		mPicWidth=800; // 2022.05.04 WOOIL - 기본값. 최초에 개발한 단말기인 경우 이 값이다.
		mPicHeight=1121; // 2022.05.04 WOOIL - 기본값. 최초에 개발한 단말기인 경우 이 값이다.
        
    }

    //public void setPenWidth(int penWidth){
    //	Log.d("EmrDroid","FingerPaintView3 setPenWidth");
    //	mPenWidth = penWidth;
    //	if(mPenMode==MODE_PEN) mPaint.setStrokeWidth(mPenWidth);
    //}
    
    //public void setEraserWidth(int eraserWidth){
    //	Log.d("EmrDroid","FingerPaintView3 setEraserWidth");
    //	mEraserWidth = eraserWidth;
    //	if(mPenMode==MODE_ERASER) mPaint.setStrokeWidth(mEraserWidth*10);
    //}

	// 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
    //public void setPenColor(int penColor){
    //	Log.d("EmrDroid","FingerPaintView3 setPenColor");
    //	mPenColor = penColor;
    //	if(mPenMode==MODE_PEN) mPaint.setColor(penColor);
    //}

	public void setPenMode(int mode){
		Log.d("EmrDroid","FingerPaintView3 setPenMode");
		mPenMode = mode;
		if(mPenMode==MODE_PEN){
			Log.d("EmrDroid","FingerPaintView3 setPenMode Pen");
			mPaint.setColor(getPenColor()); // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
			mPaint.setStrokeWidth(getPenWidth());
			mPaint.setXfermode(null);
		}else if(mPenMode==MODE_ERASER){
			Log.d("EmrDroid","FingerPaintView3 setPenMode Eraser");
			mPaint.setStrokeWidth(getEraserWidth()*10);
			mPaint.setColor(Color.YELLOW);
			mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		}else{
			//
		}
	}

	// 2024.05.21 WOOIL - 변수에서 함수로 변경
	private int getPenColor(){
    	String penColor = EmrSettingsUtil.getCcfPenColor(mContext);
    	int penColorValue = Color.BLACK;
		if ("검정".equalsIgnoreCase(penColor)) penColorValue = Color.BLACK;
		else if ("파랑".equalsIgnoreCase(penColor)) penColorValue = Color.BLUE;
		else if ("빨강".equalsIgnoreCase(penColor))
			penColorValue = Color.rgb(255, 50, 50); // RED_COLOR
		return penColorValue;
	}
	// 2024.05.21 WOOIL - 변수에서 함수로 변경
	private float getPenWidth(){
		float penWidth = EmrSettingsUtil.getCcfPenWidth(mContext);
		return penWidth;
	}
	// 2024.05.21 WOOIL - 변수에서 함수로 변경
	private float getEraserWidth(){
		float penWidth = EmrSettingsUtil.getCcfEraserWidth(mContext);
		return penWidth;
	}


    /*
    public void clear(Drawable d, CcfValues ccfValues){
    	Log.d("EmrDroid","FingerPaintView3 clear(Drawable)");
    	mCcfValues = ccfValues;
    	
    	this.setDrawingCacheEnabled(true);
    	this.setBackgroundDrawable(d);
    	
    	mPenMode = MODE_PEN;
    	
    	this.initialize(); // Scaling용 추가
    }
    */

	// 단말기별해상도처리
    public void clear(Bitmap bm, CcfValues ccfValues) {
    	Log.d("EmrDroid","FingerPaintView clear(Bitmap)");
		// 2021.11.23 WOOIL - 화면 해상도를 사용자가 변경할 수 있음.
    	DisplayMetrics metrics =  mContext.getResources().getDisplayMetrics();
		// 2021.08.25 WOOIL - 펜색을 변경하면 오류가 발생함.
		//                    이렇게 변경함.
    	if(mFirstBitmap!=null){
    		mFirstBitmap.recycle();
    		mFirstBitmap = null;
    	}
    	if(mSignBitmap!=null){
    		mSignBitmap.recycle();
    		mSignBitmap = null;    		
    	}
		if(mLoginDrSignBitmap!=null){
			mLoginDrSignBitmap.recycle();
			mLoginDrSignBitmap = null;
		}

		/*
    	if("SM-T515N".equalsIgnoreCase(Build.MODEL)){

			// 현대유비스에서 사용하는 모델
    		mPicWidth=1200;
    		mPicHeight=1612;
			mShrinkRate=1f;

    	}else if("SM-P585N0".equalsIgnoreCase(Build.MODEL)){

			// 광양사랑에서 사용하는 모델
    		mPicWidth=1200;
    		mPicHeight=1684;
			mShrinkRate=1f;

		}else if("SM-T870".equalsIgnoreCase(Build.MODEL)){

			// 대구삼일에서 사용하는 모델
			// 대구백두에서 사용하는 모델
			mPicWidth=1600;
			mPicHeight=2124;
			mShrinkRate=1f;

		}else if("SM-P610".equalsIgnoreCase(Build.MODEL) || "SM-P615N".equalsIgnoreCase(Build.MODEL)){

			// 허리나은, 나무병원, 연세고든
			// 2022.06.14 WOOIL - SM-P615N(대구백두)
    		
    		int w=metrics.widthPixels;
    		int h=metrics.heightPixels;
    		if(w==1200&&h==1936){
        		mPicWidth=1200;
        		mPicHeight=1727;
    		}else if(w==1200&&h==1928){
        		mPicWidth=1200;
        		mPicHeight=1692;
    		}else if(w==1200&&h==1916){
        		mPicWidth=1200;
        		mPicHeight=1640;
    		}else if(w==1200&&h==1904){
        		mPicWidth=1200;
        		mPicHeight=1590;
    		}else if(w==1200&&h==1892){
        		mPicWidth=1200;
        		mPicHeight=1538;
    		}else{
        		mPicWidth=1200;
        		mPicHeight=1692;
    		}
			mShrinkRate=1f;
    		
    	}else if("SM-T733".equalsIgnoreCase(Build.MODEL) || "SM-X610".equalsIgnoreCase(Build.MODEL) || "SM-T735N".equalsIgnoreCase(Build.MODEL)) {

			// 나은필 신규, 본사 신규
			// SM-X610 부천예손
			// SM-T735N 나은필, SNU서울
			int w = metrics.widthPixels;
			int h = metrics.heightPixels;
			if (w == 1600 && h == 2470) {
				mPicWidth = 1600;
				mPicHeight = 2175;
				//mFirstBitmap = Bitmap.createScaledBitmap(bm, 1600, 2175, true);
			} else if (w == 1600 && h == 2458) {
				mPicWidth = 1600;
				mPicHeight = 2124;
				//mFirstBitmap = Bitmap.createScaledBitmap(bm, 1600, 2124, true);
			} else if (w == 1600 && h == 2446) {
				mPicWidth = 1600;
				mPicHeight = 2073;
				//mFirstBitmap = Bitmap.createScaledBitmap(bm, 1600, 2073, true);
			} else if (w == 1600 && h == 2434) {
				mPicWidth = 1600;
				mPicHeight = 2021;
				//mFirstBitmap = Bitmap.createScaledBitmap(bm, 1600, 2021, true);
			} else if (w == 1600 && h == 2416) {
				mPicWidth = 1600;
				mPicHeight = 1945;
				//mFirstBitmap = Bitmap.createScaledBitmap(bm, 1600, 1945, true);
			} else {
				mPicWidth = 1600;
				mPicHeight = 2175;
				//mFirstBitmap = Bitmap.createScaledBitmap(bm, 1600, 2175, true); // 기본값...
			}
			mShrinkRate = 1f;

		}else if("TB370FU".equalsIgnoreCase(Build.MODEL)){
    		// 바른척도,안산튼튼
			// 레노버
			// 해상도 고정. (초기값 & 가상키탐색)
			mPicWidth = 1840;
			if("0040".equalsIgnoreCase(EmrSettingsUtil.getHospitalId(mContext))){
				// 안산튼튼.. 가상키탐색이 3버튼 탐색으로 변경되었다고 함.
				mPicHeight = 2450;
			}else {
				mPicHeight = 2482;
			}
			mShrinkRate=1f;

    	}else if("SHW-M380S".equalsIgnoreCase(Build.MODEL)){

			// 본사
    		mPicWidth=800;
    		mPicHeight=1121;
			mShrinkRate=1f;

    	}else{
    		
    		mPicWidth=800;
    		mPicHeight=1121;
			mShrinkRate=1f;

    	}
    	*/

		mFrameInitialized = false;
		mUserZoomChanged = false;

		// 2025.04.24 WOOIL - 단말기가 추가될 때마다 코딩하는 것이 힘들어 단말기에 해당도를 저장했다 사용하는 것으로 변경함.
		//                    writePatientInfo 함수에서 해상도를 저장한다.


		int w = metrics.widthPixels;
		int h = metrics.heightPixels;

		// 저장되어있는 값을 불러온다.
		mPicWidth = EmrSettingsUtil.getPicWidth(mContext);
		mPicHeight= EmrSettingsUtil.getPicHeight(mContext);

		// 최소 실행인 경우임.
		if(mPicWidth == 0) mPicWidth = w;
		if(mPicHeight == 0) mPicHeight = h;

		mShrinkRate = 1f;

    	mFirstBitmap = Bitmap.createScaledBitmap(bm, mPicWidth, mPicHeight, true);
    	mCcfValues = ccfValues;

    	this.setDrawingCacheEnabled(true);
    	this.setImageBitmap(mFirstBitmap);
    	
    	mPenMode = MODE_PEN;

    	this.initialize();
    }
    
    /*
    public void setNormalScale(){
    	Log.d("EmrDroid","FingerPaintView3 setNormalScale getScale="+getScale()+", mInitScale="+mInitScale+", mWidth="+mWidth+", mHeight="+mHeight);
    	zoomTo((1/getScale())*mInitScale, mWidth/2, mHeight/2);
    	cutting();
    	postInvalidate();
    	Log.d("EmrDroid","FingerPaintView3 setNormalScale 완료");
    }
    */
    
    public void recycleAll(){
    	if(mBitmap!=null){
    		if(!mBitmap.isRecycled()) {
				mBitmap.recycle();
			}
    		mBitmap = null;
    	}
    	if(mFirstBitmap!=null){
    		if(!mFirstBitmap.isRecycled()) {
				mFirstBitmap.recycle();
			}
    		mFirstBitmap = null;
    	}
    }

	// consentForm에 메시지를 띄우기 위한 함수. 디버깅 용으로 사용한다.
	private void showDialogMessage(final String msg) {
		if (mContext instanceof ConsentForm) {
			((ConsentForm)mContext).setDialogMessagePublic(msg);
		}
	}

    public Bitmap getSignedBitmap(int width, int height, float initScale){
    	Log.d("EmrDroid","FingerPaintView3 getSignedBitmap");
		// 2022.04.20 WOOIL - 동의서의 두번째 페이지를 열어보지 않으면 값이 좌측상단에 표시되는 현상 수정
    	mInitScale = initScale;
    	mWidth = width;
    	mHeight = height;
    	//Bitmap bitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true);
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    	//mBitmap.recycle();// 2021.08.25 WOOIL - 펜색을 변경하면 오류가 발생함. 이 부분을 막으니 오류가 없어짐
    	Bitmap firstBitmap = Bitmap.createScaledBitmap(mFirstBitmap, bitmap.getWidth(), bitmap.getHeight(), true);
    	//mFirstBitmap.recycle();// 2021.08.25 WOOIL - 펜색을 변경하면 오류가 발생함. 이 부분을 막으니 오류가 없어짐

    	//
    	Paint paint = mPaint;
		Canvas canvas = new Canvas(bitmap);
    	canvas.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), mClearPaint);
    	float scale=mInitScale;//getScale();
    	float tranX=0;//getTranslateX();
    	float tranY=0;//getTranslateY();
    	for (int i=0;i<mMyPenInfoList.size();i++){
    		MyPenInfo penInfo = mMyPenInfoList.get(i);
    		//scale=penInfo.scale/mInitScale;
    		//tranX=(penInfo.tranX/penInfo.scale)*(bitmapW/firstBitmapW);
    		//tranY=(penInfo.tranY/penInfo.scale)*(bitmapH/firstBitmapH);
    		int penMode = penInfo.penMode;
    		Path path = penInfo.getPath(scale, tranX, tranY);
    		if(penMode==MODE_PEN){
    			paint.setStyle(Paint.Style.STROKE);
    			paint.setStrokeWidth(penInfo.penWidth);
    			//paint.setColor(getResources().getColor(R.color.pencolor));
    			paint.setColor(penInfo.penColor); // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
    			paint.setXfermode(null);
    		}else if(penMode==MODE_ERASER){
    			paint.setStyle(Paint.Style.STROKE);
    			paint.setStrokeWidth(penInfo.penWidth*10);
    			//paint.setColor(Color.WHITE);
    			//paint.setXfermode(null);
    			//paint.setColor(getResources().getColor(R.color.pencolor));
    			paint.setColor(penInfo.penColor); // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
    			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT)); // 완료
				//아래 방법으로 하면 화면이 까맣게 됨. 원인? 모름.
    			//paint.setColor(getResources().getColor(R.color.pencolor));
    			//paint.setXfermode(mClearmode);//new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    		}
    		canvas.drawPath(path, paint);
    	}
    	writePatientInfo(canvas, scale/mInitScale, tranX, tranY, true);

    	Bitmap retBitmap = overlay(firstBitmap, bitmap);
		//firstBitmap.recycle(); // 2022.06.15 WOOIL - 백두병원(SM-P615N)에서 오류가 발생하여 막음.
		//bitmap.recycle(); // 2022.06.15 WOOIL - 백두병원(SM-P615N)에서 오류가 발생하여 막음.
    	return getResizedBitmap(retBitmap);
    }
    
    private Bitmap overlay(Bitmap bmp1, Bitmap bmp2) {
		Bitmap bmOverlay = Bitmap.createBitmap(bmp1.getWidth(), bmp1.getHeight(),  bmp1.getConfig());
		Canvas canvas = new Canvas(bmOverlay);
		canvas.drawBitmap(bmp1, 0, 0, null);
		canvas.drawBitmap(bmp2, 0, 0, null);
		return bmOverlay;    	
    }

    private Bitmap getResizedBitmap(Bitmap bmp) {
    	// 2025.02.14 WOOIL -
    	Matrix matrix = new Matrix();
    	matrix.postScale(mShrinkRate, mShrinkRate);
    	return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
	}

    public void undoSign(){
    	Log.d("EmrDroid","FingerPaintView3 undoSign");
    	if(mCountDrawn>0){
            //mPaintList.remove(mCountDrawn-1);
            mMyPenInfoList.remove(mCountDrawn-1);
            //mPenModeList.remove(mCountDrawn-1);
    		mCountDrawn--;
    		invalidate();
    	}
    }
    
    //private void initPen(){
    //	Log.d("EmrDroid","FingerPaintView3 initPen");
    //	//mPaint = new Paint();
    //    //mPaint.setAntiAlias(true);
    //    //mPaint.setDither(true);
    //    ////mPaint.setColor(mPenColor); // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
	//	//mPaint.setColor(getPenColor()); // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
    //    //mPaint.setStyle(Paint.Style.STROKE);
    //    //mPaint.setStrokeJoin(Paint.Join.ROUND);
    //    //mPaint.setStrokeCap(Paint.Cap.ROUND);
    //    ////mPaint.setStrokeWidth(mPenWidth);
	//	//mPaint.setStrokeWidth(getPenWidth());
    //    //
    //    //mClearPaint = new Paint();
    //	//mClearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    //    //
    //    //mCursorPaint = new Paint();
    //    ////mCursorPaint.setColor(mPenColor); // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
	//	//mCursorPaint.setColor(getPenColor()); // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
	//	//mCursorPaint.setStyle(Paint.Style.STROKE);
	//	//
	//	//mTextPaint = new Paint();
	//	//mTextPaint.setTextSize(getPixelFromDip(16.0f));
	//	//mTextPaint.setColor(Color.BLACK);
	//	//mTextPaint.setTextAlign(Paint.Align.LEFT);
	//	//mTextPaint.setStyle(Style.FILL);
    //}
    
    private void writePatientInfo(Canvas canvas, float scale, float tranX, float tranY, boolean saving){
    	Log.d("EmrDroid","writePatientInfo");

		// 2026.03.25 WOOIL - 글자도 확대.축소하자..
		float oldTextSize = mTextPaint.getTextSize();
		float scaledTextSize = mBaseTextSize * scale;
		if (scaledTextSize < 1f) scaledTextSize = 1f;
		mTextPaint.setTextSize(scaledTextSize);

    	if(mCcfValues!=null){
			//float ratioW = mWidth / (float)mPicWidth; // 2022.05.04 WOOIL - 800f를 mPicWidth로 대체함.제대로 동작하지 않는다.
			//float ratioH = mHeight / (float)mPicHeight; // 2022.05.04 WOOIL - 1121f를 mPicHeight로 대체함.제대로 동작하지 않는다.
    		float ratioW = mWidth / 800f;
    		float ratioH = mHeight / 1121f;
    		//mTextPaint.setTextSize(getPixelFromDip(16.0f*scale));
    		float lineGap = (float)4.0 * ratioH * scale;
        	int count = mCcfValues.getCount();
    		for(int i=0;i<count;i++){

				if (saving == false) {
					if (mOnCcfValueChangedListener != null) {
						mOnCcfValueChangedListener.onCcfValueChanged(i, mCcfValues.getCcfValue(i));
					}
				}

    			float x = mCcfValues.getX(i)*ratioW*scale + tranX;
    			float y = mCcfValues.getY(i)*ratioH*scale + tranY;
    			float fx = x; //getPixelFromDip(x);
    			float fy = y; //getPixelFromDip(y);
				float height = mCcfValues.getH(i)*ratioH*scale; //2026.04.07 WOOIL - 막음//+ tranY;
				float width = mCcfValues.getW(i)*ratioW*scale; //2026.04.07 WOOIL - 막음+ tranX; // 2024.04.26 WOOIL -
				boolean autoFit = mCcfValues.getAutoFit(i);
    			String ccfValue = mCcfValues.getValue(i);

    			if(ccfValue.startsWith("sign_")) {
					// 의사 사인을 출력한다.
					if (mSignBitmap == null) {
						// 한번만 읽기 위한 코딩
						String drid = ccfValue.substring(5);
						String dstDir = mContext.getFilesDir().getAbsolutePath();
						String pathName = dstDir + File.separator + "Sign" + File.separator + drid;
						Bitmap bm = BitmapFactory.decodeFile(pathName);
						if (bm == null) {
							// 사인 이미지가 없음.
						} else {
							float imgW = 120f * ratioW * scale;
							float imgH = 40f * ratioH * scale;
							if(autoFit){
								// 2025.09.01 WOOIL - 사인이미지의 크기를 MEE에서 조절한 크기로 맞춘다.
								imgW = width;// * ratioW * scale;
								imgH = height;// * ratioH * scale;
							}
							mSignBitmap = Bitmap.createScaledBitmap(bm, (int) imgW, (int) imgH, true);
							// 2022.05.04 WOOIL - 흰색을 투명하게 만들자
							mSignBitmap = makeTransparent(mSignBitmap);
							// 2026.03.20 WOOIL - 진하게
							mSignBitmap = enhanceBitmap(mSignBitmap);
							// 2026.03.20 WOOIL - 굵게
							mSignBitmap = expandStroke(mSignBitmap, 1);
							bm.recycle();
						}
					}
					if (mSignBitmap != null) {
						//canvas.drawBitmap(mSignBitmap, x, y, null);
						// 2026.03.20 WOOIL - 사인도 같이 확대 축소되게
						float imgW = 120f;
						float imgH = 40f;
						if(autoFit){
							// 2025.09.01 WOOIL - 사인이미지의 크기를 MEE에서 조절한 크기로 맞춘다.
							imgW = width;// * ratioW * scale;
							imgH = height;// * ratioH * scale;
						}
						imgW *= ratioW * scale;
						imgH *= ratioH * scale;
						RectF rectF = new RectF(x, y, x + imgW, y+ imgH);
						canvas.drawBitmap(mSignBitmap, null, rectF, null);
					}
				}else if(ccfValue.startsWith("logindrsign_")){
						// 의사 사인을 출력한다.
						if(mLoginDrSignBitmap==null){
							// 한번만 읽기 위한 코딩
							String drid = ccfValue.substring(12);
							String dstDir = mContext.getFilesDir().getAbsolutePath();
							String pathName = dstDir + File.separator + "Sign" + File.separator + drid;
							Bitmap bm = BitmapFactory.decodeFile(pathName);
							if(bm==null){
								// 사인 이미지가 없음.
							}else{
								float imgW = 120f*ratioW*scale;
								float imgH = 40f*ratioH*scale;
								if(autoFit){
									// 2025.09.01 WOOIL - 사인이미지의 크기를 MEE에서 조절한 크기로 맞춘다.
									imgW = width;// * ratioW * scale;
									imgH = height;// * ratioH * scale;
								}
								mLoginDrSignBitmap = Bitmap.createScaledBitmap(bm, (int)imgW, (int)imgH, true);
								// 2022.05.04 WOOIL - 흰색을 투명하게 만들자
								mLoginDrSignBitmap = makeTransparent(mLoginDrSignBitmap);
								// 2026.03.20 WOOIL - 진하게
								mLoginDrSignBitmap = enhanceBitmap(mLoginDrSignBitmap);
								// 2026.03.20 WOOIL - 굵게
								mLoginDrSignBitmap = expandStroke(mLoginDrSignBitmap, 1);
								bm.recycle();
							}
						}
						if(mLoginDrSignBitmap!=null){
							//canvas.drawBitmap(mLoginDrSignBitmap, x, y, null);
							// 2026.03.20 WOOIL - 사인도 같이 확대 축소되게
							float imgW = 120f;
							float imgH = 40f;
							if(autoFit){
								// 2025.09.01 WOOIL - 사인이미지의 크기를 MEE에서 조절한 크기로 맞춘다.
								imgW = width;// * ratioW * scale;
								imgH = height;// * ratioH * scale;
							}
							imgW *= ratioW * scale;
							imgH *= ratioH * scale;
							RectF rectF = new RectF(x, y, x + imgW, y+ imgH);
							canvas.drawBitmap(mLoginDrSignBitmap, null, rectF, null);
						}
    			}else{
	    			Rect rect = new Rect();
	    			//float height = mCcfValues.getH(i)*ratioH*scale + tranY;
					//float width = mCcfValues.getW(i)*ratioW*scale + tranX; // 2024.04.26 WOOIL -
	    			String[] ccfV = (ccfValue+System.getProperty("line.separator")).split(System.getProperty("line.separator"));
	    			// 2024.04.26 WOOIL - 글자를 박스안에 출력되도록 제한.
	    			for(int lno=0;lno<ccfV.length;lno++){
	    				if(autoFit){
							int start = 0;
							int end = ccfV[lno].length();
	    					while(true){
	    						while(true) {
	    							// 2024.04.26 WOOIL - 설정해 놓은 너비만큼 만 출력되도록 글자 길이를 정한다.
									mTextPaint.getTextBounds(ccfV[lno], start, end, rect);
									if (rect.width() <= width) break; // 출력하면 됨.
									end--; // 너비를 넘어가면 글자 수를 줄인다.
								}
								if (end <= start) break; // 2026.04.07 WOOIL - 한 글자도 못 들어가는 경우 탈출
								fy += rect.height();
								canvas.drawText(ccfV[lno].substring(start, end), fx, fy, mTextPaint);
								height -= rect.height();
								height -= lineGap; // 줄 사이 간격
								if (height <= 0) break; // 출력할 높이를 벗어나면 중단.
								fy += lineGap; // 줄 사이 간격
								// 2024.04.26 WOOIL - 시작점과 종료점을 조절한다.
								start = end;
								end = ccfV[lno].length();
								if(start>=end) break; // 모든 글자를 출력했음.
							}
						}else {
							mTextPaint.getTextBounds(ccfV[lno], 0, ccfV[lno].length(), rect);
							fy += rect.height();
							canvas.drawText(ccfV[lno], fx, fy, mTextPaint);
							height -= rect.height();
							height -= lineGap; // 줄 사이 간격
							if (height <= 0) break; // 출력할 높이를 벗어나면 중단.
							fy += lineGap; // 줄 사이 간격
						}
	    			}
    			}
    			/*
    			if(mCcfValues.getH(i)<=14.0){
	    			mTextPaint.getTextBounds(ccfValue, 0, ccfValue.length(), rect);
	    			//rect.set(ifx, ify+rect.top, ifx+rect.width(), ify+rect.bottom);
	    			fy += rect.height();
	    			canvas.drawText(ccfValue, fx, fy, mTextPaint);
	    			//Log.d("EmrDroid","writePatientInfo x=" + x + ", y=" + y + ", fx=" + fx + ", fy=" + fy);
    			}else{
	    			String[] ccfV = (ccfValue+System.getProperty("line.separator")).split(System.getProperty("line.separator"));
	    			for(int lno=0;lno<ccfV.length-1;lno++){
		    			mTextPaint.getTextBounds(ccfV[lno], 0, ccfV[lno].length(), rect);
		    			fy += rect.height();
		    			canvas.drawText(ccfV[lno], fx, fy, mTextPaint);
	    			}
    			}
    			*/
    		}
    	}

    	// 해상도 설정
		int w = EmrSettingsUtil.getPicWidth(mContext);
		int h = EmrSettingsUtil.getPicHeight(mContext);

		if(w!=mWidth || h!=mHeight){
			EmrSettingsUtil.setPicWidth(mContext, mWidth);
			EmrSettingsUtil.setPicHeight(mContext, mHeight);

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setMessage("해상도를 조정하였습니다. 동의서를 다시 실행해주세요.")
                   .setPositiveButton("확인", null)
                   .show();
		}

		// 디버깅용
    	boolean bDebug=false;
    	if("mmsdev".equalsIgnoreCase(mUserId)) bDebug=true;
    	if(bDebug==true){
	    	float x=0;
	    	float y=100;
			Rect rect = new Rect();
	    	// 이 기기에서 사용가능한 높이와 너비
	    	String msg = "mWidth="+mWidth+", mHeight="+mHeight;
	    	mTextPaint.getTextBounds(msg, 0, msg.length(), rect);
	    	y += rect.height();
	    	canvas.drawText(msg, x, y, mTextPaint);
	    	msg = "scale="+scale+", tranX="+tranX+" tranY="+tranY;
	    	mTextPaint.getTextBounds(msg, 0, msg.length(), rect);
	    	y += rect.height();
	    	canvas.drawText(msg, x, y, mTextPaint);
	    	msg = "mInitScale="+mInitScale+", mInitTranX="+mInitTranX+", mInitTranY="+mInitTranY;
	    	mTextPaint.getTextBounds(msg, 0, msg.length(), rect);
	    	y += rect.height();
	    	canvas.drawText(msg, x, y, mTextPaint);
	    	// 모델명
	    	msg = "Model="+Build.MODEL+"" ;
	    	mTextPaint.getTextBounds(msg, 0, msg.length(), rect);
	    	y += rect.height();
	    	canvas.drawText(msg, x, y, mTextPaint);
	    	// OS버전
	    	y += rect.height();
	    	msg = "Build.VERSION.SDK_INT="+Build.VERSION.SDK_INT+"" ;
	    	canvas.drawText(msg, x, y, mTextPaint);
	    	// 화면해상도
	    	if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.R){
				y += rect.height();
				WindowManager windowManager = mContext.getSystemService(WindowManager.class);
				WindowMetrics metrics = windowManager.getCurrentWindowMetrics();
				Rect bounds = metrics.getBounds();
				msg = "Screen width=" + bounds.width() + ", height=" + bounds.height() + "";
				canvas.drawText(msg, x, y, mTextPaint);
			}else {
				y += rect.height();
				DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
				msg = "Screen width=" + metrics.widthPixels + ", height=" + metrics.heightPixels + "";
				canvas.drawText(msg, x, y, mTextPaint);
			}
    	}
    }

	public boolean injectCcfValue4Doctor(String drid, String drnm, String drnm_eng, String gdrlcid, String sdrlcid) {
		// 2026.02.11 WOOIL - 외부(ConsentForm)에서 값을 사용자에게 입력받아 이곳으로 넘긴다.
		boolean changed = false;
		if(mCcfValues!=null) {
			int count = mCcfValues.getCount();
			for (int i = 0; i < count; i++) {
				String field = mCcfValues.getField(i);
				String value = mCcfValues.getValue(i);
				if ("drnm".equalsIgnoreCase(field)) {
					if (!drnm.equalsIgnoreCase(value)) {
						changed = true;
						mCcfValues.putValue(i, drnm);
					}
				} else if ("drnm_eng".equalsIgnoreCase(field)) {
					if (!drnm_eng.equalsIgnoreCase(value)) {
						changed = true;
						mCcfValues.putValue(i, drnm_eng);
					}
				} else if ("gdrlcid".equalsIgnoreCase(field)) {
					if (!gdrlcid.equalsIgnoreCase(value)) {
						changed = true;
						mCcfValues.putValue(i, gdrlcid);
					}
				} else if ("sdrlcid".equalsIgnoreCase(field)) {
					if (!sdrlcid.equalsIgnoreCase(value)) {
						changed = true;
						mCcfValues.putValue(i, sdrlcid);
					}
				}
			}
		}
		return changed;
	}

	public boolean injectCcfValue4DrSign(String drsign) {
		// 2026.02.11 WOOIL - 외부(ConsentForm)에서 값을 사용자에게 입력받아 이곳으로 넘긴다.
		boolean changed = false;
		if(mCcfValues!=null) {
			int count = mCcfValues.getCount();
			for (int i = 0; i < count; i++) {
				String field = mCcfValues.getField(i);
				String value = mCcfValues.getValue(i);
				if ("drsign".equalsIgnoreCase(field)) {
					if (!drsign.equalsIgnoreCase(value)) {
						changed = true;
						mCcfValues.putValue(i, drsign);
						// 사인파일을 새로 읽어야 해서 있는 사인이미지를 지운다.
						if(mSignBitmap!=null){
							mSignBitmap.recycle();
							mSignBitmap = null;
						}
					}
				}
			}
		}
		return changed;
	}

    private Bitmap makeTransparent(Bitmap bm) { 
    	int width = bm.getWidth(); 
    	int height = bm.getHeight(); 
    	Bitmap myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888); 
    	int[] allpixels = new int[myBitmap.getHeight() * myBitmap.getWidth()]; 
    	bm.getPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight()); 
    	myBitmap.setPixels(allpixels, 0, width, 0, 0, width, height); 
    	for (int i = 0; i < myBitmap.getHeight() * myBitmap.getWidth(); i++) { 
    		if (allpixels[i] == Color.WHITE) {
				// 하얀색을 투명하게 변환
    			allpixels[i] = Color.alpha(Color.TRANSPARENT); 
    		} 
    	} 
    	myBitmap.setPixels(allpixels, 0, myBitmap.getWidth(), 0, 0, myBitmap.getWidth(), myBitmap.getHeight()); 
    	return myBitmap; 
    }
	private Bitmap enhanceBitmap(Bitmap src) {
		int width = src.getWidth();
		int height = src.getHeight();

		Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		int[] pixels = new int[width * height];
		src.getPixels(pixels, 0, width, 0, 0, width, height);

		for (int i = 0; i < pixels.length; i++) {
			int color = pixels[i];

			int alpha = Color.alpha(color);
			int red   = Color.red(color);
			int green = Color.green(color);
			int blue  = Color.blue(color);

			// 진하게 만드는 핵심 로직
			red   = Math.min(255, (int)(red * 1.3));
			green = Math.min(255, (int)(green * 1.3));
			blue  = Math.min(255, (int)(blue * 1.3));

			alpha = Math.min(255, (int)(alpha * 1.5));

			pixels[i] = Color.argb(alpha, red, green, blue);
		}

		result.setPixels(pixels, 0, width, 0, 0, width, height);
		return result;
	}

	private Bitmap expandStroke(Bitmap src, int radius) {
		if (src == null) return null;

		int width = src.getWidth();
		int height = src.getHeight();

		Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		int[] srcPixels = new int[width * height];
		int[] outPixels = new int[width * height];

		src.getPixels(srcPixels, 0, width, 0, 0, width, height);

		// 원본을 먼저 복사
		System.arraycopy(srcPixels, 0, outPixels, 0, srcPixels.length);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int idx = y * width + x;
				int color = srcPixels[idx];
				int alpha = Color.alpha(color);

				// 충분히 보이는 픽셀만 "사인 획"으로 판단
				if (alpha > 20) {
					int red = Color.red(color);
					int green = Color.green(color);
					int blue = Color.blue(color);

					for (int dy = -radius; dy <= radius; dy++) {
						for (int dx = -radius; dx <= radius; dx++) {
							int nx = x + dx;
							int ny = y + dy;

							if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
								continue;
							}

							// 사각형 확장이 아니라 원형 느낌으로 제한
							if (dx * dx + dy * dy > radius * radius) {
								continue;
							}

							int nIdx = ny * width + nx;
							int oldColor = outPixels[nIdx];
							int oldAlpha = Color.alpha(oldColor);

							// 더 진한 쪽으로 덮어쓰기
							if (oldAlpha < alpha) {
								outPixels[nIdx] = Color.argb(alpha, red, green, blue);
							}
						}
					}
				}
			}
		}

		out.setPixels(outPixels, 0, width, 0, 0, width, height);
		return out;
	}

	private Bitmap loadSignBitmapRaw(String drid) {
		String dstDir = mContext.getFilesDir().getAbsolutePath();
		String pathName = dstDir + File.separator + "Sign" + File.separator + drid;

		Bitmap bm = BitmapFactory.decodeFile(pathName);
		if (bm == null) {
			return null;
		}

		Bitmap processed = bm.copy(Bitmap.Config.ARGB_8888, true);
		bm.recycle();

		processed = makeTransparent(processed);

		// 이전에 적용한 진하게/확장 처리가 있으면 여기서 같이 적용
		 processed = enhanceBitmap(processed);
		 processed = expandStroke(processed, 1);

		return processed;
	}

    private float getPixelFromDip(float dipValue){
    	return Utils.getPixelFromDip(mContext, dipValue);
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(w>0 && h>0){
        	Log.d("EmrDroid","FingerPaintView3 onSizeChanged x=" + String.valueOf(w) + ", h=" + String.valueOf(h));
	        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
	        mCanvas = new Canvas(mBitmap);
        }
    }
    
    private class MyPath{
    	public float x1;
    	public float y1;
    	public float x2;
    	public float y2;
    	public String mode;
    }
    private class MyPenInfo{
    	public float penWidth;
    	public int penMode;
    	public int penColor; // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
    	public float scale;
    	public float tranX;
    	public float tranY;
    	private List<MyPath> pathList = new ArrayList<MyPath>();
    	public void addPath(String mode, float x1, float y1, float x2, float y2){
    		MyPath myPath = new MyPath();
    		myPath.mode=mode;
    		myPath.x1=x1;
    		myPath.y1=y1;
    		myPath.x2=x2;
    		myPath.y2=y2;
    		pathList.add(myPath);
    	}
    	public Path getPath(float scale, float tranX, float tranY){
    		Path path = new Path();
    		for(int j=0;j<this.getCount();j++){
        		String mode=this.getMyPath(j).mode;
    			float x1=this.getMyPath(j).x1*scale + tranX;
    			float y1=this.getMyPath(j).y1*scale + tranY;
    			float x2=this.getMyPath(j).x2*scale + tranX;
    			float y2=this.getMyPath(j).y2*scale + tranY;
    			
    			//Log.d("EmrDroid-path","x1="+x1+", x2="+x2+", y1="+y1+", y2="+y2);
    		
	    		if("s".equals(mode)){
	    			path.moveTo(x1, y1);
	    		}else if("m".equals(mode)){
	    			path.quadTo(x1, y1, x2, y2);
	    		}else if("e".equals(mode)){
	    			path.lineTo(x1, y1);
	    		}
    		}
    		return path;
    	}
    	public int getCount(){
    		return pathList.size();
    	}
    	public MyPath getMyPath(int idx){
    		return pathList.get(idx);
    	}
    }
    private List<MyPenInfo> mMyPenInfoList = new ArrayList<MyPenInfo>();
    
    //private List<Path> mPathList = new ArrayList<Path>();//2020.01.30
    //private List<Paint> mPaintList = new ArrayList<Paint>();//2020.01.30
    //private List<Integer> mPenModeList = new ArrayList<Integer>();//2020.01.30
    //private boolean mTouchUp = false;
    
    private PointF mCursorP = new PointF();
    private PorterDuffXfermode mClearmode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);

    @Override
    protected void onDraw(Canvas canvas) {
    	Log.d("EmrDroid","FingerPaintView onDraw penWidth="+mPaint.getStrokeWidth()+", scale="+getScale()+", tranX="+getTranslateX()+", tranY="+getTranslateY());

    	//2020.01.30
    	super.onDraw(canvas);
		// 이전에 그려놓은 것 지움.
    	mCanvas.drawRect(0, 0, mWidth, mHeight, mClearPaint);
		// 커서를 표시
    	if(mFingerAction==ACTION_DRAWING){
			if(mCursorP.x!=0 && mCursorP.y!=0){
				mCanvas.drawCircle(mCursorP.x, mCursorP.y, 5, mCursorPaint);
			}
    	}
    	//
    	float scale=getScale();
    	float tranX=getTranslateX();
    	float tranY=getTranslateY();
    	//
    	Paint paint = mPaint;
    	// 사인을 다시 그린다.
    	for (int i=0;i<mMyPenInfoList.size();i++){
        	if(i==mMyPenInfoList.size()-1){
        		scale=mInitScale;
        		tranX=mInitTranX;
        		tranY=mInitTranY;
        	}else{
            	scale=getScale();
            	tranX=getTranslateX();
            	tranY=getTranslateY();
        	}
    		MyPenInfo penInfo = mMyPenInfoList.get(i);
    		/*
    		Path path = new Path();
    		for(int j=0;j<penInfo.getCount();j++){
        		String mode=penInfo.getMyPath(j).mode;
    			float x1=penInfo.getMyPath(j).x1*scale + tranX;
    			float y1=penInfo.getMyPath(j).y1*scale + tranY;
    			float x2=penInfo.getMyPath(j).x2*scale + tranX;
    			float y2=penInfo.getMyPath(j).y2*scale + tranY;
    		
	    		if("s".equals(mode)){
	    			path.moveTo(x1, y1);
	    		}else if("m".equals(mode)){
	    			path.quadTo(x1, y1, x2, y2);
	    		}else if("e".equals(mode)){
	    			path.lineTo(x1, y1);
	    		}
    		}
    		*/
    		Path path = penInfo.getPath(scale, tranX, tranY);
    		int penMode = penInfo.penMode;
    		if(penMode==MODE_PEN){
    			paint.setStrokeWidth(penInfo.penWidth);
    			paint.setColor(penInfo.penColor); // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
    			paint.setXfermode(null);
    		}else if(penMode==MODE_ERASER){
    			paint.setStrokeWidth(penInfo.penWidth*10);
    			paint.setColor(Color.YELLOW);
    			paint.setXfermode(mClearmode);//new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    		}
    		mCanvas.drawPath(path, paint);
    	}
		// 환자 정보를 표시
    	scale=getScale();
    	tranX=getTranslateX();
    	tranY=getTranslateY();
    	writePatientInfo(mCanvas, scale/mInitScale, tranX, tranY, false);
    	
    	canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
    	
    	Log.d("EmrDroid","FingerPaintView End of onDraw penWidth="+mPaint.getStrokeWidth()+", scale="+getScale()+", tranX="+getTranslateX()+", tranY="+getTranslateY());    	
    }
    
//    @Override 
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//    	int width = MeasureSpec.getSize(widthMeasureSpec);
//    	int height = MeasureSpec.getSize(heightMeasureSpec);
//    	setMeasuredDimension(width, height);
//    }
    
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

	//위치를 이동했음.
    //private void touch_start(float x, float y) {
    //	Log.d("EmrDroid","FingerPaintView touth_start");
    //	mTouchUp=false;//2020.01.30
    //	mPenModeList.add(mPenMode);//2020.01.30
    //  mPaintList.add(new Paint(mPaint));//2020.01.30
    //  mPathList.add(new Path());//2020.01.30
    //  mPathList.get(mCountDrawn).moveTo(x, y);//2020.01.30
    //  mX = x;
    //  mY = y;
    //}

	//위치를 이동했음.
    //private void touch_move(float x, float y) {
    //	Log.d("EmrDroid","FingerPaintView touth_move");
    //	mTouchUp=false;//2020.01.30
    //    float dx = Math.abs(x - mX);
    //    float dy = Math.abs(y - mY);
    //    if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
    //    	mPathList.get(mCountDrawn).quadTo(mX, mY, (x + mX)/2, (y + mY)/2);//2020.01.30
    //        mX = x;
    //        mY = y;
    //    }
    //}

	//위치를 이동했음.
    //private void touch_up() {
    //	Log.d("EmrDroid","FingerPaintView touth_up");
    //	mTouchUp=true;//2020.01.30
    //    mPathList.get(mCountDrawn).lineTo(mX, mY);//2020.01.30
    //    mCountDrawn++;//2020.01.30
    //}
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	Log.d("EmrDroid","FingerPaintView3 onTouchEvent");
    	if(mPenMode==MODE_NONE){
			// 일단 아무것도하지 않음.
    	}else{
    		int touchCount = event.getPointerCount();
	        
	        switch (event.getAction()) {
	            case MotionEvent.ACTION_DOWN:
	            case MotionEvent.ACTION_POINTER_1_DOWN:
	            case MotionEvent.ACTION_POINTER_2_DOWN:
	            	Log.d("EmrDroid", "FingerPaintView ACTION_DOWN");
	                if (touchCount >= 2) {
	                	
	                    mFingerAction = ACTION_SCALING;
	                	Log.d("EmrDroid","FingerPaintView3 mFingerAction="+mFingerAction);
	                	
	                	
	        	        mCursorP.x = 0;
	        	        mCursorP.y = 0;

	        	        // Scaling
	                    float distance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
	                    mPrevDistance = distance;
	                    
	                    // Moving
	                    mPrevMoveX = getMiddleX(event.getX(0),event.getX(1)); // 두 손가락의 중간지점 x축
	                    mPrevMoveY = getMiddleY(event.getY(0),event.getY(1)); // 두 손가락의 중간지점 y축

						// 확대 축소가 시작됨.
	                    //isScaling = true; 
	                    
	                }else{
	                	
	                	mFingerAction = ACTION_DRAWING;
	                	Log.d("EmrDroid","FingerPaintView3 mFingerAction="+mFingerAction);

						// 손가락 하나로 터치를 시작했음.
						// 그림을 그리자.
	        	        float x = event.getX();
	        	        float y = event.getY();
	        	        
	        	        mCursorP.x = x;
	        	        mCursorP.y = y;

						// 2020.03.30 함수에서 이곳으로 뺌
		                //touch_start(x, y);
		                
		            	//mTouchUp=false;//2020.01.30
		            	//mPenModeList.add(mPenMode);//2020.01.30
		                //mPaintList.add(new Paint(mPaint));//2020.01.30
		                //mPathList.add(new Path());//2020.01.30
		                //mPathList.get(mCountDrawn).moveTo(x, y);//2020.01.30
		                
		                float scale = getScale();
	    	        	float tranX = getTranslateX();
	    	        	float tranY = getTranslateY();

	    	        	mMyPenInfoList.add(new MyPenInfo());
	    	        	mMyPenInfoList.get(mCountDrawn).scale=getScale();
	    	        	mMyPenInfoList.get(mCountDrawn).tranX=getTranslateX();
	    	        	mMyPenInfoList.get(mCountDrawn).tranY=getTranslateY();
	    	        	mMyPenInfoList.get(mCountDrawn).penMode=mPenMode;
	    	        	//mMyPenInfoList.get(mCountDrawn).penColor=mPenColor; // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
						mMyPenInfoList.get(mCountDrawn).penColor=getPenColor(); // 2021.08.06 WOOIL - 펜의 색을 사용자가 변경할 수 있게하기 위함.
	    	        	//mMyPenInfoList.get(mCountDrawn).penWidth=mPenWidth;
						mMyPenInfoList.get(mCountDrawn).penWidth=getPenWidth();
		                mMyPenInfoList.get(mCountDrawn).addPath("s", x/scale-tranX/scale
		                		                                   , y/scale-tranY/scale
		                		                                   , 0
		                		                                   , 0
		                		                                   );
		                
		                mX = x;
		                mY = y;
		                
		                invalidate();
	                }
	                break;
	            case MotionEvent.ACTION_MOVE:
					// 터치하고 움직이는 중
					// 터치한 손가락이 2개이면 화면을 확대나 축소 및 이동시키고
					//                 1개이면 그림을 그린다.
                	Log.d("EmrDroid", "FingerPaintView ACTION_MOVE");
	                if(mFingerAction == ACTION_SCALING) { //if (touchCount >= 2 && isScaling) {
	                	
	                	Log.d("EmrDroid","FingerPaintView3 mFingerAction="+mFingerAction);
	                	
	        	        mCursorP.x = 0;
	        	        mCursorP.y = 0;
	        	        
	                	// Scaling
	                    float dist = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
	                    float scale = (dist - mPrevDistance) / dispDistance();
	                    mPrevDistance = dist;
	                    scale += 1;
	                    scale = scale * scale;
	                    zoomTo(scale, mWidth / 2, mHeight / 2);
	                    cutting();
	                    
	                    // Moving
	                    float distanceX = mPrevMoveX - event.getX();
	                    float distanceY = mPrevMoveY - event.getY();
	                    mPrevMoveX = getMiddleX(event.getX(0),event.getX(1));
	                    mPrevMoveY = getMiddleY(event.getY(0),event.getY(1));
	                    mMatrix.postTranslate(-distanceX, -distanceY);
	                    cutting();
	                    
	                }else if(mFingerAction==ACTION_DRAWING){// else if (!isScaling) {
	                	
	                	Log.d("EmrDroid","FingerPaintView3 mFingerAction="+mFingerAction);

	                	// 그림 그리기
		    	        float x = event.getX();
		    	        float y = event.getY();
		    	        
	        	        mCursorP.x = x;
	        	        mCursorP.y = y;
		    	        
		    	        // 2020.03.30 함수에서 이곳으로 뺌
		                //touch_move(x, y);
		    	        
		    	    	//mTouchUp=false;//2020.01.30
		    	        float dx = Math.abs(x - mX);
		    	        float dy = Math.abs(y - mY);
						// TOUCH_TOLERANCE=4.0 이므로 이동 간격이 4.0 이상인 경우만 동작한다.
		    	        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
		    	        	// quadTo(float x1, float y1, float x2, float y2)
							//    기준점에서 (x1, y1)까지, 그리고 (x2, y2)까지 곡선형태를 그린다.
							// 실제 그림은 drawPath함수를 호출해야 함.
							// onDraw 메노스에서 drawPath함수를 호출하여 그림을 그림.
		    	        	//mPathList.get(mCountDrawn).quadTo(mX, mY, (x + mX)/2, (y + mY)/2);//2020.01.30
		    	        	
		    	        	float scale = getScale();
		    	        	float tranX = getTranslateX();
		    	        	float tranY = getTranslateY();
		    	        	
		    	        	//Log.d("EmrDroid", "FingerPaintView ACTION_MOVE scale=" + scale + ", tranX=" + tranX + ", tranY=" + tranY);
		    	        	
			                mMyPenInfoList.get(mCountDrawn).addPath("m", mX/scale - tranX/scale
			                		                                   , mY/scale - tranY/scale
			                		                                   , (x/scale + mX/scale)/2 - tranX/scale
			                		                                   , (y/scale + mY/scale)/2 - tranY/scale
			                		                                   );//2020.03.31
		    	        	
		    	            mX = x;
		    	            mY = y;
		    	        }
		    	        
		                invalidate(); // 강제로 onDraw를 호출함.
	                }
	                break;
	            case MotionEvent.ACTION_UP:
	            case MotionEvent.ACTION_POINTER_UP:
	            case MotionEvent.ACTION_POINTER_2_UP:
					// 터치를 끝냈음. 정리 작업을 한다.
	            	Log.d("EmrDroid", "FingerPaintView ACTION_UP");
	            	if(mFingerAction==ACTION_DRAWING){//if (!isScaling) {
	            		
	                	Log.d("EmrDroid","FingerPaintView3 mFingerAction="+mFingerAction);

	                	mCursorP.x = 0;
	        	        mCursorP.y = 0;

						//2020.03.30 함수에서 이곳으로 뺌
		                //touch_up();
	            		
		            	//mTouchUp=true;//2020.01.30
		                //mPathList.get(mCountDrawn).lineTo(mX, mY);//2020.01.30
		                
		                float scale = getScale();
	    	        	float tranX = getTranslateX();
	    	        	float tranY = getTranslateY();

	    	        	mMyPenInfoList.get(mCountDrawn).addPath("e", mX/scale-tranX/scale
		                		                                   , mY/scale-tranY/scale
		                		                                   , 0
		                		                                   , 0
		                		                                   );//2020.03.31
		                
		                mCountDrawn++;//2020.01.30
		                
		                invalidate();
		                
	            	}
	            	mFingerAction = ACTION_NOTHING;
                	Log.d("EmrDroid","FingerPaintView3 mFingerAction="+mFingerAction);
	            	/*
	            	if (event.getPointerCount() <= 1) {
	        	        mCursorP.x = 0;
	        	        mCursorP.y = 0;
	        	        
	                    isScaling = false;
	                    
	                    invalidate();
	                }
	                */
	                break;
	        }
    	}
        return true;
    }
    
    private float distance(float x0, float x1, float y0, float y1) {
        float x = x0 - x1;
        float y = y0 - y1;
        return (float)Math.sqrt(x * x + y * y);
    }
    
    private float dispDistance() {
        return (float)Math.sqrt(mWidth * mWidth + mHeight * mHeight);
    }
    
    protected void maxZoomTo(int x, int y) {
        if (mMinScale != getScale() && (getScale() - mMinScale) > 0.1f) {
            // threshold 0.1f
            float scale = mMinScale / getScale();
            zoomTo(scale, x, y);
        } else {
            float scale = MAX_SCALE / getScale();
            zoomTo(scale, x, y);
        }
    }

    public void zoomTo(float scale, int x, int y) {
        if (getScale() * scale < mMinScale) {
            return;
        }
        if (scale >= 1 && getScale() * scale > MAX_SCALE) {
            return;
        }
        mMatrix.postScale(scale, scale);
        // move to center
        mMatrix.postTranslate(-(mWidth * scale - mWidth) / 2, -(mHeight * scale - mHeight) / 2);

        // move x and y distance
        mMatrix.postTranslate(-(x - (mWidth / 2)) * scale, 0);
        mMatrix.postTranslate(0, -(y - (mHeight / 2)) * scale);
        setImageMatrix(mMatrix);

		// 2026.03.10 WOOIL - 확대 축소된 값 보존용
		if (!mInFrameInit) {
			mUserZoomChanged = true;
		}
    }

    public void cutting() {
        int width = (int) (mIntrinsicWidth * getScale());
        int height = (int) (mIntrinsicHeight * getScale());
        if (getTranslateX() < -(width - mWidth)) {
            mMatrix.postTranslate(-(getTranslateX() + width - mWidth), 0);
        }
        if (getTranslateX() > 0) {
            mMatrix.postTranslate(-getTranslateX(), 0);
        }
        if (getTranslateY() < -(height - mHeight)) {
            mMatrix.postTranslate(0, -(getTranslateY() + height - mHeight));
        }
        if (getTranslateY() > 0) {
            mMatrix.postTranslate(0, -getTranslateY());
        }
        if (width < mWidth) {
            mMatrix.postTranslate((mWidth - width) / 2, 0);
        }
        if (height < mHeight) {
            mMatrix.postTranslate(0, (mHeight - height) / 2);
        }
        setImageMatrix(mMatrix);
    }
    
    protected float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mMatrixValues);
        return mMatrixValues[whichValue];
    }

    protected float getScale() {
        return getValue(mMatrix, Matrix.MSCALE_X);
    }
    
    public float getTranslateX() {
        return getValue(mMatrix, Matrix.MTRANS_X);
    }

    protected float getTranslateY() {
        return getValue(mMatrix, Matrix.MTRANS_Y);
    }

    @Override
    public void setImageMatrix(Matrix matrix){
    	super.setImageMatrix(matrix);
    }
    
    public int getFrameWidth(){
    	return mWidth;
    }
    
    public int getFrameHeight(){
    	return mHeight;
    }
    
    public float getInitSacle(){
    	return mInitScale;
    }
    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
    	Log.d("EmrDroid","FingerPaintView3 setFrame");
		boolean changed = super.setFrame(l, t, r, b);

		// 2026.03.10 WOOIL - 이미 초기화가 끝났고, 사용자가 확대/이동한 상태면 다시 초기화하지 않는다.
		if (mFrameInitialized && mUserZoomChanged) {
			return changed;
		}

		// 2026.03.10 WOOIL - 이미 초기화가 끝났고, 굳이 다시 맞출 필요가 없는 경우도 막고 싶으면
		if (mFrameInitialized) {
			return changed;
		}

		// 2026.03.10 WOOIL - 확대 축소된 값 보존용
		mInFrameInit = true;

        mWidth = r - l;
        mHeight = b - t;
        Log.d("EmrDroid","FingerPaintView3 setFrame mWidth="+mWidth+", mHeight="+mHeight);

        mMatrix.reset();
        int r_norm = r - l;
        mScale = (float) r_norm / (float) mIntrinsicWidth;

        int paddingHeight = 0;
        int paddingWidth = 0;
        // scaling vertical
        if (mScale * mIntrinsicHeight > mHeight) {
            mScale = (float) mHeight / (float) mIntrinsicHeight;
            mMatrix.postScale(mScale, mScale);
            paddingWidth = (r - mWidth) / 2;
            paddingHeight = 0;
            // scaling horizontal
        }else {
            mMatrix.postScale(mScale, mScale);
            paddingHeight = (b - mHeight) / 2;
            paddingWidth = 0;
        }
        mMatrix.postTranslate(paddingWidth, paddingHeight);

        setImageMatrix(mMatrix);
        mMinScale = mScale;
        zoomTo(mScale, mWidth / 2, mHeight / 2);
        cutting();
        
        mInitScale=getScale();
        mInitTranX=getTranslateX();
        mInitTranY=getTranslateY();
                
        //return super.setFrame(l, t, r, b);
		mInFrameInit = false; // 2026.03.10 WOOIL - 확대 축소된 값 보존용
		mFrameInitialized = true; // 2026.03.10 WOOIL - 확대 축소된 값 보존용
		return changed; // 2026.03.10 WOOIL - 확대 축소된 값 보존용
    }
    
    private float getMiddleX(float x1, float x2){
    	return x1;
    }
    
    private float getMiddleY(float y1, float y2){
    	return y1;
    }
    
    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        this.initialize();
    }
    
    private void initialize() {
    	Log.d("EmrDroid","FingerPaintView3 initialize");
        this.setScaleType(ScaleType.MATRIX);
        //this.mMatrix = new Matrix(); // 2024.06.28 WOOIL - 선언할 때 new 하는 것으로 수정
        Drawable d = getDrawable();
        if (d != null) {
            mIntrinsicWidth = d.getIntrinsicWidth();
            mIntrinsicHeight = d.getIntrinsicHeight();
            setOnTouchListener(this);
        }
        /*
        mDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                maxZoomTo((int) e.getX(), (int) e.getY());
                cutting();
                return super.onDoubleTap(e);
            }
        });
        */
        mCountDrawn = 0;// 2020.01.30
        mMyPenInfoList.clear();// 2020.03.31
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
        return super.onTouchEvent(event);
	}

}
