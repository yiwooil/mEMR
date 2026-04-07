package com.metrosoft.smart.emr.emrdroid.gt101.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Picture;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.PictureDrawable;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

/*
 * private void saveScreenshot(final Picture pic, final String fileName){ //
 * 소스코드를 나중에 참고하기 위하여 남겨둔다. new Thread(new Runnable() {
 * 
 * @Override public void run() {
 * 
 * Bitmap bmp = Bitmap.createBitmap(200,200, Bitmap.Config.ARGB_8888);
 * Bitmap roundBmp = Bitmap.createBitmap(200,200, Bitmap.Config.ARGB_8888);
 * 
 * Canvas canvas = new Canvas(bmp); pic.draw(canvas);
 * 
 * Canvas rCanvas = new Canvas(roundBmp);
 * 
 * //for round image final int color = 0xff424242; final Paint paint = new
 * Paint(); final Rect rect = new Rect(0,0,200, 200); final RectF rectF =
 * new RectF(rect); final float roundPx = 20;
 * 
 * paint.setAntiAlias(true); paint.setColor(color);
 * rCanvas.drawARGB(0,0,0,0); rCanvas .drawRoundRect(rectF, roundPx,
 * roundPx, paint);
 * 
 * paint.setXfermode(new
 * PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)); rCanvas
 * .drawBitmap(bmp, rect, rect, paint);
 * 
 * try { Log.d("EmrDroid","fileName="+fileName); FileOutputStream output =
 * new FileOutputStream(fileName);
 * 
 * roundBmp.compress(Bitmap.CompressFormat.PNG, 90, output);
 * 
 * output.flush(); output.close(); Log.d("EmrDroid","Save success"); //
 * Images.Media.insertImage(getContentResolver(), bmp, fileName, null); }
 * catch (IOException e) { // TODO Auto-generated catch block
 * e.printStackTrace(); Log.d("EmrDroid","Save error" +
 * e.getMessage().toString()); } } }).start(); }
 */

public class DrawableImageView extends ScaleImageView {
	//private Bitmap mBitmap;
	private Canvas mCanvas;
	private Path mPath;
	private Paint mBitmapPaint;
	private boolean mEraserMode;
	private Paint mPen,mEraser; // 사인하는 펜,지우개
	
	private int mSignStatus;

	public DrawableImageView(Context c) {
		super(c);
		this.setPen();
		mPath = new Path();
		mBitmapPaint = new Paint(Paint.DITHER_FLAG);
		mEraserMode=false;
	}

	public void clear(Bitmap bitmap) {
		this.init(bitmap);
		this.invalidate(); // onDraw를 호출함.
	}
	
	private void init(Bitmap bitmap){
		//mBitmap = bitmap;
		super.setImageBitmap(bitmap);
		mCanvas = new Canvas(bitmap);
		mEraserMode=false;
	}

	private void setPen(){
		// 펜
		mPen = new Paint();
		mPen.setAntiAlias(true);
		mPen.setDither(true);
		mPen.setColor(getResources().getColor(R.color.pencolor));
		mPen.setStyle(Paint.Style.STROKE);
		mPen.setStrokeJoin(Paint.Join.ROUND);
		mPen.setStrokeCap(Paint.Cap.ROUND);
		mPen.setStrokeWidth(1);
		// 지우개
		mEraser = new Paint();
		mEraser.setAlpha(0);
		mEraser.setAntiAlias(true);
		//mEraser.setDither(true);
		mEraser.setColor(getResources().getColor(R.color.pencolor));
		//mEraser.setStyle(Paint.Style.STROKE);
		//mEraser.setStrokeJoin(Paint.Join.ROUND);
		//mEraser.setStrokeCap(Paint.Cap.ROUND);
		//mEraser.setStrokeWidth(1);
		mEraser.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
	}
	
	public void setModeClear(){
		mEraserMode=true;
	}
	
	public void setModeDraw(){
		mEraserMode=false;
	}
	
	public void setPen(Paint paint){
		mPen = paint;
	}
	
	public Bitmap getBitmap() {
		Bitmap bitmap = ((BitmapDrawable)this.getDrawable()).getBitmap();
		return bitmap;
	}
	
	public void setSignStatus(int status){
		mSignStatus = status;
		this.invalidate();
	}
	
	public int getSignStatus(){
		return mSignStatus;
	}
	

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		if (mSignStatus == 1) {
			canvas.drawColor(R.color.background);
			//canvas.drawBitmap(getBitmap(), 0, 0, mBitmapPaint);
			canvas.drawBitmap(getBitmap(), mMatrix, mBitmapPaint);
			if(mEraserMode){
				canvas.drawPath(mPath, mEraser);
			}else{
				canvas.drawPath(mPath, mPen);
			}
		}else{
			super.onDraw(canvas);
		}
	}
	
	private float mX, mY;
	private static final float TOUCH_TOLERANCE = 1;

	private void touch_start(float x, float y) {
		if (mSignStatus == 1) {
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
		}
	}

	private void touch_move(float x, float y) {
		if (mSignStatus == 1) {
			float dx = Math.abs(x - mX);
			float dy = Math.abs(y - mY);
			if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
				/*
				 * 화면을 확대하지 않으면 이 코딩이 유효하나
				 * 화면을 확대하면 정확히 동작하지 않아서막고
				 * 아래와 같이 코딩함.
				mPath.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2);
				mX = x;
				mY = y;
				*/
				mPath.lineTo(x, y);
				// commit the path to our offscreen
				if(mEraserMode){
					mCanvas.drawPath(mPath, mEraser);
				}else{
					mCanvas.drawPath(mPath, mPen);
				}
				// kill this so we don't double draw
				mPath.reset();
				mPath.moveTo(x, y);
				mX = x;
				mY = y;
			}
		}
	}

	private void touch_up() {
		if (mSignStatus == 1) {
			mPath.lineTo(mX, mY);
			// commit the path to our offscreen
			if(mEraserMode){
				mCanvas.drawPath(mPath, mEraser);
			}else{
				mCanvas.drawPath(mPath, mPen);
			}
			// kill this so we don't double draw
			mPath.reset();
		}
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mSignStatus == 1) {
			float[] values = new float[9];
			mMatrix.getValues(values);

			// 사용자가 사인하는 중
			// 확대/축소 비율 반영
			float x = event.getX() / super.getScale();
			float y = event.getY() / super.getScale();
			// 확대후 이미지 옮긴 결과 반영
			x -= values[2] / super.getScale();
			y -= values[5] / super.getScale();

			int action=event.getAction();
			switch (action) {
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
		}else{
			super.onTouchEvent(event);
		}
		return true;
	}

}
