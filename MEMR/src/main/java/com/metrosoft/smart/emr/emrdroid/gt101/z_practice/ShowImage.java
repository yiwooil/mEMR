package com.metrosoft.smart.emr.emrdroid.gt101.z_practice;

import java.io.FileOutputStream;

import com.metrosoft.smart.emr.emrdroid.gt101.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

public class ShowImage extends Activity {

	private String mFileName;
	private ImageView mImageView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.show_image);
		mImageView=(ImageView)findViewById(R.id.imageView);
		
		Intent intent = getIntent();
		mFileName = intent.getStringExtra("filename");
		
		loadImageFile();
	}
	
	private void loadImageFile(){
		try {
			Log.d("EmrDroid","filename="+mFileName);
			Bitmap bitmap=BitmapFactory.decodeFile(mFileName);
			Log.d("EmrDroid","2");
			mImageView.setImageBitmap(bitmap);
			Log.d("EmrDroid","3");
		}catch(Exception e){
			Log.d("EmrDroid","error : " + e.getMessage().toString());
		}
	}
}
