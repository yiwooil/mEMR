/***
 * Excerpted from "Hello, Android! 3e",
 * published by The Pragmatic Bookshelf.
 * Copyrights apply to this code. It may not be used to create training material, 
 * courses, books, articles, and the like. Contact us if you are in doubt.
 * We make no guarantees that this code is fit for any purpose. 
 * Visit http://www.pragmaticprogrammer.com/titles/eband3 for more book information.
 ***/
package com.metrosoft.smart.emr.emrdroid.gt101.z_notused;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;
import com.metrosoft.smart.emr.emrdroid.gt101.event.WrapMotionEvent;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class EmrScanView_old extends MyActivity implements OnTouchListener {
	private static final String TAG = "Touch";
	// These matrices will be used to move and zoom image
	Matrix matrix = new Matrix();
	Matrix savedMatrix = new Matrix();

	// We can be in one of these 3 states
	static final int NONE = 0;
	static final int DRAG = 1;
	static final int ZOOM = 2;
	int mode = NONE;

	// Remember some things for zooming
	PointF start = new PointF();
	PointF mid = new PointF();
	float oldDist = 1f;

	private String xmlPatientInfo;
	private Bitmap bitmap;
	private String pid;
	private String bededt;
	private String path;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, R.layout.emr_scan_view_old, "< " + getString(R.string.emr_scan));

		ImageView imageView = (ImageView) findViewById(R.id.emrScanView);
		imageView.setOnTouchListener(this);

		((Button) findViewById(R.id.rotateRightButton)).setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (bitmap != null) {
					rotate(90f, ((ImageView) findViewById(R.id.emrScanView)));
				}
			}
		});
		((Button) findViewById(R.id.rotateLeftButton)).setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				if (bitmap != null) {
					rotate(-90f, ((ImageView) findViewById(R.id.emrScanView)));
				}
			}
		});

		// // ...
		// // Work around a Cupcake bug
		// matrix.setTranslate(1f, 1f);
		// imageView.setImageMatrix(matrix);

		Intent intent = getIntent();
		pid = intent.getStringExtra("pid");
		bededt = intent.getStringExtra("bededt");
		path = intent.getStringExtra("path");

		if (savedInstanceState == null) {
			getEmrScanView();
		} else {
			xmlPatientInfo = savedInstanceState.getString("xmlPatientInfo");
			bitmap = (Bitmap) savedInstanceState.getParcelable("bitmap");
			// 화면에 다시 출력
			afterGetEmrScanView();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("xmlPatientInfo", xmlPatientInfo);
		outState.putParcelable("bitmap", bitmap);
	}

	@Override
	public void onClickQueryButton(View v) {
		getEmrScanView();
	}

	private void rotate(float degrees, ImageView imageView) {
		float[] values = new float[9];
		matrix.getValues(values);
		float w = bitmap.getWidth();
		float h = bitmap.getHeight();
		float x1 = values[Matrix.MTRANS_X];
		float y1 = values[Matrix.MTRANS_Y];
		float scaleX = values[Matrix.MSCALE_X];
		float scaleY = values[Matrix.MSCALE_Y];
		if (scaleX == 0 || scaleX == -0 || scaleY == 0 || scaleY == -0) {
			scaleX = values[Matrix.MSKEW_X];
			scaleY = values[Matrix.MSKEW_Y];
			// 옆으로 누웠음. 가로세로가 서로 바뀌었음.
			w = bitmap.getHeight();
			h = bitmap.getWidth();
		}
		float x2 = x1 + w * scaleX;
		float y2 = y1 + h * scaleY;

		if (x2 < x1) {
			float t = x1;
			x1 = x2;
			x2 = t;
		}
		if (y2 < y1) {
			float t = y1;
			y1 = y2;
			y2 = t;
		}
		float midX = (x2 - x1) / 2f + x1;
		float midY = (y2 - y1) / 2f + y1;

		// Log.d("EmrDroid","x1="+x1+",x2="+x2+",y1="+y1+",y2="+y2+",w="+w+",h="+h+"midX="+midX+",midY="+midY+",scaleX="+scaleX+",scaleY="+scaleY);
		//
		// Log.d("EmrDroid","["+values[6]+"/"+values[7]+"/"+values[8]+"]["+values[0]+"/"+values[4]+"]["+values[1]+"/"+values[3]+"]["+values[2]+"/"+values[5]+"]");
		matrix.postRotate(degrees, midX, midY);
		imageView.setImageMatrix(matrix);
	}

	private void getEmrScanView() {
		mDialog = ProgressDialog.show(EmrScanView_old.this, "", getString(R.string.query_wait_message), true);
		new Thread(new Runnable() {
			public void run() {
				String hospitalId = getHospitalId();
				String userId = getUserId();
				String url = "";
				// 환자정보
				url = "InPatientInformationServlet?hospitalid=" + hospitalId + "&pid=" + pid + "&bededt=" + bededt;
				xmlPatientInfo = getXml(url);
				// 기타서식
				String imagePath = path.replace("\\", "/");
				String imageUrl = "EmrScanServlet?hospitalid=" + hospitalId + "&path=" + imagePath;

				//bitmap = getBitmap(imageUrl);
				mHandler.post(new Runnable() {
					public void run() {
						// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
						// 이를 방지함.
						try {
							afterGetEmrScanView();
							mDialog.dismiss();
						} catch (Exception e) {
							;
						}
					}
				});
			}
		}).start();
	}

	private void afterGetEmrScanView() {
		if (super.getXmlError() == true) {
			super.showToastText(super.getXmlErrorMessage());
			return;
		}
		((TextView) findViewById(R.id.patientInfoTextView)).setText(xmlPatientInfo);

		ImageView imageView = (ImageView) findViewById(R.id.emrScanView);
		imageView.setImageBitmap(bitmap);

		matrix.setTranslate(1f, 1f);
		imageView.setImageMatrix(matrix);

		// 그림을 화면에 다 보이게 하기 위하여 그림의 크기가 화면보다 크면 크기를 조정한다.
		Log.d("EmrDroid", "bitmap width = " + bitmap.getWidth() + ", view width = " + imageView.getWidth());
		Log.d("EmrDroid", "bitmap Height = " + bitmap.getHeight() + ", view height = " + imageView.getHeight());

		if (imageView.getWidth() <= 0)
			return;
		if (imageView.getHeight() <= 0)
			return;

		float scaleX = (float) imageView.getWidth() / (float) bitmap.getWidth();
		float scaleY = (float) imageView.getHeight() / (float) bitmap.getHeight();

		// 더 작은 값으로 처리
		float scale = scaleX < scaleY ? scaleX : scaleY;
		if (scale < 1) {
			matrix.postScale(scale, scale);
			imageView.setImageMatrix(matrix);
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent rawEvent) {
		WrapMotionEvent event = WrapMotionEvent.wrap(rawEvent);
		// ...
		ImageView view = (ImageView) v;

		// Dump touch event to log
		dumpEvent(event);

		// Handle touch events here...
		switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				savedMatrix.set(matrix);
				start.set(event.getX(), event.getY());
				Log.d(TAG, "mode=DRAG");
				mode = DRAG;
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				oldDist = spacing(event);
				Log.d(TAG, "oldDist=" + oldDist);
				if (oldDist > 10f) {
					savedMatrix.set(matrix);
					midPoint(mid, event);
					mode = ZOOM;
					Log.d(TAG, "mode=ZOOM");
				}
				break;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				mode = NONE;
				Log.d(TAG, "mode=NONE");
				break;
			case MotionEvent.ACTION_MOVE:
				if (mode == DRAG) {
					// ...
					matrix.set(savedMatrix);
					matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
				} else if (mode == ZOOM) {
					float newDist = spacing(event);
					Log.d(TAG, "newDist=" + newDist);
					if (newDist > 10f) {
						matrix.set(savedMatrix);
						float scale = newDist / oldDist;
						matrix.postScale(scale, scale, mid.x, mid.y);
					}
				}
				break;
		}

		view.setImageMatrix(matrix);
		return true; // indicate event was handled
	}

	/** Show an event in the LogCat view, for debugging */
	private void dumpEvent(WrapMotionEvent event) {
		// ...
		String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE", "POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;
		sb.append("event ACTION_").append(names[actionCode]);
		if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP) {
			sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			sb.append(")");
		}
		sb.append("[");
		for (int i = 0; i < event.getPointerCount(); i++) {
			sb.append("#").append(i);
			sb.append("(pid ").append(event.getPointerId(i));
			sb.append(")=").append((int) event.getX(i));
			sb.append(",").append((int) event.getY(i));
			if (i + 1 < event.getPointerCount())
				sb.append(";");
		}
		sb.append("]");
		Log.d(TAG, sb.toString());
	}

	/** Determine the space between the first two fingers */
	private float spacing(WrapMotionEvent event) {
		// ...
		float x = event.getX(0) - event.getX(1);
		float y = event.getY(0) - event.getY(1);
		return (float)Math.sqrt(x * x + y * y);
	}

	/** Calculate the mid point of the first two fingers */
	private void midPoint(PointF point, WrapMotionEvent event) {
		// ...
		float x = event.getX(0) + event.getX(1);
		float y = event.getY(0) + event.getY(1);
		point.set(x / 2, y / 2);
	}
}
