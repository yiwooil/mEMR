package com.metrosoft.smart.emr.emrdroid.gt101.view;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.data.CcfValues;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

public class FingerPaintView extends View {
	public static final int MODE_NONE = 0;
	public static final int MODE_PEN = 1;
	public static final int MODE_ERASER = 2;
	
	private static final float MINP = 0.25f;
    private static final float MAXP = 0.75f;
    
    private Context mContext;
    private CcfValues mCcfValues;
    
    private Bitmap  mBitmap;
    private Canvas  mCanvas;
    private Path    mPath;
    private Paint   mBitmapPaint;
    
    //private Bitmap mTextBitmap;
    //private Canvas mTextCanvas;
    private Paint  mTextPaint;
    
    private Paint   mPaint;
    
    private int mPenMode;
    private float mPenWidth;
    private float mEraserWidth;
    
    public FingerPaintView(Context c, int penWidth, int eraserWidth) {
        super(c);
        
        mContext = c;
        mPenWidth = penWidth;
        mEraserWidth = eraserWidth;
        
        initPen();
        initTextPen();
        
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);
        
        mPenMode = MODE_NONE;
        
    }
    
    public void setPenWidth(int penWidth){
    	mPenWidth = penWidth;
    	if(mPenMode==MODE_PEN){
			mPaint.setStrokeWidth(mPenWidth);
			Log.d("EmrDroid","FingerPaitView setPenWidth width=" + mPenWidth + ", width=" + mPaint.getStrokeWidth());
    	}
    }
    
    public void setEraserWidth(int eraserWidth){
    	mEraserWidth = eraserWidth;
    	if(mPenMode==MODE_ERASER){
			mPaint.setStrokeWidth(mEraserWidth*10);
			Log.d("EmrDroid","FingerPaitView setEraserWidth width=" + mEraserWidth + ", width=" + mPaint.getStrokeWidth());
    	}
    }
    
    public void clear(Drawable d, CcfValues ccfValues){
    	mCcfValues = ccfValues;
    	
    	this.setDrawingCacheEnabled(true);
    	this.setBackgroundDrawable(d);
    	mPenMode = MODE_PEN;
    }
    
    public Bitmap getSignedBitmap(){
    	//return mBitmap;
    	return this.getDrawingCache();
    }
    
    public void setPenMode(int mode){
    	mPenMode = mode;
    	if(mPenMode==MODE_PEN){
    		//initPen();
    		mPaint.setStrokeWidth(mPenWidth);
    		mPaint.setXfermode(null);
    	}else if(mPenMode==MODE_ERASER){
    		mPaint.setStrokeWidth(mEraserWidth*10);
    		mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
    	}else{
    		//
    	}
    }
    
    private void initPen(){
    	mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setColor(getResources().getColor(R.color.pencolor));
        //mPaint.setColor(Color.RED);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        //mPaint.setStrokeWidth(1);
        mPaint.setStrokeWidth(mPenWidth);
    }
    
    private void initTextPen(){
	    mTextPaint = new Paint();
	    mTextPaint.setTextSize(getPixelFromDip(16.0f));
	    mTextPaint.setColor(Color.BLACK);
	    mTextPaint.setTextAlign(Paint.Align.LEFT);
	    mTextPaint.setStyle(Style.FILL);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(w>0 && h>0){
        	Log.d("EmrDroid","FingerPaintView w=" + w + ", h=" + h);
	        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
	        mCanvas = new Canvas(mBitmap);
	        //mTextBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
	        //mTextCanvas = new Canvas(mTextBitmap);
        }
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
    	Log.d("EmrDroid","FingerPaintView onDraw penWidth=" + mPaint.getStrokeWidth());
        super.onDraw(canvas);
        // 글씨 쓰기
        writePatientInfo(canvas);
        //canvas.drawColor(R.color.background);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        // 사인 실시간 반영
        if(mPenMode==MODE_PEN){
        	canvas.drawPath(mPath, mPaint);
        }
    }
    
    private void writePatientInfo(Canvas canvas){
    	if(mCcfValues!=null){
    		int count = mCcfValues.getCount();
    		for(int i=0;i<count;i++){
    			float x = mCcfValues.getX(i);
    			float y = mCcfValues.getY(i);
    			float fx = getPixelFromDip(x);
    			float fy = getPixelFromDip(y);
    			String ccfValue = mCcfValues.getValue(i);
    			
    			Rect rect = new Rect();
    			mTextPaint.getTextBounds(ccfValue, 0, ccfValue.length(), rect);
    			//rect.set(ifx, ify+rect.top, ifx+rect.width(), ify+rect.bottom);
    			fy += rect.height();
    			canvas.drawText(ccfValue, fx, fy, mTextPaint);
    			//Log.d("EmrDroid","x=" + x + ", y=" + y + ", fx=" + fx + ", fy=" + fy);
    		}
    	}
    }
    
    private float getPixelFromDip(float dipValue){
    	//return dipValue;
    	return Utils.getPixelFromDip(mContext, dipValue);
    }
    
//    @Override 
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//    	int width = MeasureSpec.getSize(widthMeasureSpec);
//    	int height = MeasureSpec.getSize(heightMeasureSpec);
//    	setMeasuredDimension(width, height);
//    }
    
    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;
    
    private void touch_start(float x, float y) {
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }
    private void touch_move(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
        // 재우개 실시간 반영
        if(mPenMode==MODE_ERASER){
        	mCanvas.drawPath(mPath, mPaint);
        }
    }
    private void touch_up() {
        mPath.lineTo(mX, mY);
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // kill this so we don't double draw
        mPath.reset();
        // 이하 추가
    	//mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if(mPenMode==MODE_NONE){
    		// 일단 아무것도 하지 않음.
    	}else{
	        float x = event.getX();
	        float y = event.getY();
	        
	        switch (event.getAction()) {
	            case MotionEvent.ACTION_DOWN:
	                touch_start(x, y);
	                invalidate();
	                break;
	            case MotionEvent.ACTION_MOVE:
	                touch_move(x, y);
	                invalidate();
	                break;
	            case MotionEvent.ACTION_UP:
	                touch_up();
	                invalidate();
	                break;
	        }
    	}
        return true;
    }
}
