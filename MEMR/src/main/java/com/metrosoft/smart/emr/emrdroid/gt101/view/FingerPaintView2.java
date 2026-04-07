package com.metrosoft.smart.emr.emrdroid.gt101.view;

import java.util.ArrayList;
import java.util.List;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.data.CcfValues;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.FloatMath;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class FingerPaintView2 extends ImageView implements OnTouchListener { // ������extends View ����.
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
    
    // ���� Ȯ�� ��� ����
    private float MAX_SCALE = 2f;
    protected Matrix mMatrix;
    private final float[] mMatrixValues = new float[9];
    private int mWidth;
    private int mHeight;
    private int mIntrinsicWidth;
    private int mIntrinsicHeight;
    private float mPrevDistance;
    private boolean isScaling;
    private float mPrevMoveX;
    private float mPrevMoveY;
    private GestureDetector mDetector;
    private float mScale;
    private float mMinScale;
    
    public FingerPaintView2(Context c, int penWidth, int eraserWidth) {
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
    	
    	this.initialize(); // Scaling�� �߰�
    }
    
    public void clear(Bitmap bm, CcfValues ccfValues) {
    	Log.d("EmrDroid","FingerPaintView clear");
    	mCcfValues = ccfValues;

    	this.setDrawingCacheEnabled(true);
    	this.setImageBitmap(bm);
    	
    	mPenMode = MODE_PEN;

    	this.initialize();
    }
    
    
    public Bitmap getSignedBitmap(){
    	return this.getDrawingCache();
    }
    
    public void setPenMode(int mode){
    	mPenMode = mode;
    	if(mPenMode==MODE_PEN){
    		//initPen();
    		mPaint.setColor(getResources().getColor(R.color.pencolor));
    		mPaint.setStrokeWidth(mPenWidth);
    		mPaint.setXfermode(null);
    	}else if(mPenMode==MODE_ERASER){
    		mPaint.setStrokeWidth(mEraserWidth*10);
    		mPaint.setColor(Color.YELLOW);
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
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mPenWidth);
    }
    
    private void initTextPen(){
	    mTextPaint = new Paint();
	    mTextPaint.setTextSize(getPixelFromDip(16.0f));
	    mTextPaint.setColor(Color.BLACK);
	    mTextPaint.setTextAlign(Paint.Align.LEFT);
	    mTextPaint.setStyle(Style.FILL);
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
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if(w>0 && h>0){
        	Log.d("EmrDroid","FingerPaintView onSizeChanged w=" + w + ", h=" + h);
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
        // �۾�����
        writePatientInfo(canvas);
        //canvas.drawColor(R.color.background);
        canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
        // ���� �ǽð� �ݿ�.
        if(mPenMode==MODE_PEN){
        	canvas.drawPath(mPath, mPaint);
        }
        // ���찳
        if(mPenMode==MODE_ERASER){
        	canvas.drawPath(mPath, mPaint);
        }
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
    	Log.d("EmrDroid","FingerPaintView touth_start");
        // ���찳. ����� ���� ��������� ǥ�õǵ���
        if(mPenMode==MODE_ERASER){
        	mPaint.setXfermode(null);
        }
        mPath.reset();
        mPath.moveTo(x, y);
        mX = x;
        mY = y;
    }
    private void touch_move(float x, float y) {
    	Log.d("EmrDroid","FingerPaintView touth_move");
        // ���찳. ����� ���� ��������� ǥ�õǵ���
        if(mPenMode==MODE_ERASER){
        	mPaint.setXfermode(null);
        }
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
            mX = x;
            mY = y;
        }
    }
    private void touch_up() {
    	Log.d("EmrDroid","FingerPaintView touth_up");
        // ���찳. ����� ���� ��������� ǥ�õǵ���
        if(mPenMode==MODE_ERASER){
        	mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        }
        mPath.lineTo(mX, mY);

        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        // kill this so we don't double draw
        mPath.reset();


        // ���� �߰�
    	//mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SCREEN));
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	if(mPenMode==MODE_NONE){
    		// �ϴ� �ƹ��͵����� ����.
    	}else{
    		int touchCount = event.getPointerCount();
	        
	        switch (event.getAction()) {
	            case MotionEvent.ACTION_DOWN:
	            case MotionEvent.ACTION_POINTER_1_DOWN:
	            case MotionEvent.ACTION_POINTER_2_DOWN:
	                if (touchCount >= 2) {
	                	// Scaling
	                    float distance = distance(event.getX(0), event.getX(1), event.getY(0), event.getY(1));
	                    mPrevDistance = distance;
	                    // Moving
	                    mPrevMoveX = getMiddleX(event.getX(0),event.getX(1));
	                    mPrevMoveY = getMiddleY(event.getY(0),event.getY(1));
	                    isScaling = true;
	                } else {
	        	        float x = event.getX();
	        	        float y = event.getY();
		                touch_start(x, y);
		                invalidate();
	                }
	                break;
	            case MotionEvent.ACTION_MOVE:
	                if (touchCount >= 2 && isScaling) {
	                	Log.d("EmrDroid", "FingerPaintView ACTION_MOVE");
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
	                } else if (!isScaling) {
		    	        float x = event.getX();
		    	        float y = event.getY();
		                touch_move(x, y);
		                invalidate();
	                }
	                break;
	            case MotionEvent.ACTION_UP:
	            case MotionEvent.ACTION_POINTER_UP:
	            case MotionEvent.ACTION_POINTER_2_UP:
	            	if (!isScaling) {
		                touch_up();
		                invalidate();	            		
	            	}
	                if (event.getPointerCount() <= 1) {
	                    isScaling = false;
	                }
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
    
    @Override
    protected boolean setFrame(int l, int t, int r, int b) {
        mWidth = r - l;
        mHeight = b - t;

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
        } else {
            mMatrix.postScale(mScale, mScale);
            paddingHeight = (b - mHeight) / 2;
            paddingWidth = 0;
        }
        mMatrix.postTranslate(paddingWidth, paddingHeight);

        setImageMatrix(mMatrix);
        mMinScale = mScale;
        zoomTo(mScale, mWidth / 2, mHeight / 2);
        cutting();
        return super.setFrame(l, t, r, b);
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
        this.setScaleType(ScaleType.MATRIX);
        this.mMatrix = new Matrix();
        Drawable d = getDrawable();
        if (d != null) {
            mIntrinsicWidth = d.getIntrinsicWidth();
            mIntrinsicHeight = d.getIntrinsicHeight();
            setOnTouchListener(this);
        }
        mDetector = new GestureDetector(mContext, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                maxZoomTo((int) e.getX(), (int) e.getY());
                cutting();
                return super.onDoubleTap(e);
            }
        });
    }

	@Override
	public boolean onTouch(View v, MotionEvent event) {
        return super.onTouchEvent(event);
	}
    
}
