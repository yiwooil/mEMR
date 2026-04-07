package com.metrosoft.smart.emr.emrdroid.gt101.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.ProgressDialog;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;

public class Utils {
	public static void downFile(Context context, String srcPath, String dstPath) throws Exception {
		// http://www.androidsnippets.com/download-an-http-file-to-sdcard-with-progress-notification
		//try {
			// 폴더가 있는지 보고 없으면 만든다.
			String dstDir = context.getFilesDir().getAbsolutePath();
			makeFolder(dstDir);
			makeFolder(dstDir + File.separator + "mp4");  // 녹음파일 다운용 폴더
			makeFolder(dstDir + File.separator + "Form"); // 이미지동의서 다운용 폴더
			makeFolder(dstDir + File.separator + "Sign"); // 의사 사인 다운용 폴더

			// set download url
			//String path=getImageFilePath(1);

			URL url = new URL(srcPath);
			
			// create connection
			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			
			// set up some things on the connection 
			urlConnection.setRequestMethod("GET");
			//urlConnection.setDoOutput(true);
			urlConnection.setDoOutput(false); // <-- 이렇게 해야 동작함.

			// file
			//String saveFile = dstDir + File.separator + mPopuppicKey + File.separator + dstPath;
			String saveFile = dstPath;
			File file = new File(saveFile);
			
			// this wilee be userd to write the downloaded data into the file we created
			FileOutputStream fos = new FileOutputStream(file);
			OutputStream out = new BufferedOutputStream(fos);
			
			// this will be used in read the data from the internet
			InputStream is = urlConnection.getInputStream();
			
			// this is the total size of the file
			//int totalSize = urlConnection.getContentLength();
			// variable to store total downloaded bytes
			//int downloadedSize = 0;
			
			// create buffer...
			byte[] buffer = new byte[1024];
			int bufferLength = 0; // used to store a temporary size of the buffer
			
			// now, read through the input buffer and write the content to the file
			while((bufferLength = is.read(buffer))>0){
				// add the data in the buffer to the file in the file output stream
				//fos.write(buffer, 0 ,bufferLength);
				out.write(buffer, 0 ,bufferLength);
				// add up the size so we know how much is download
				//downloadedSize += bufferLength;
				// this is where you would do something to report the progress, like the maybe
				//updateProgress(downloadedSize, totalSize);
			}
			// close the output stream when done
			//fos.close();
			out.flush();
			out.close();

		//} catch (MalformedURLException e) {
		//	// TODO Auto-generated catch block
		//	//e.printStackTrace();
		//	//Log.d("EmrDroid","error in downFile1=" + e.getMessage());
		//} catch (IOException e) {
		//	// TODO Auto-generated catch block
		//	//e.printStackTrace();
		//	//Log.d("EmrDroid","error in downFile2=" + e.getMessage());
		//} catch (Exception e) {
		//	// TODO Auto-generated catch block
		//	//e.printStackTrace();
		//	//Log.d("EmrDroid","error in downFile3=" + e.getMessage());
		//}
	}
	
	private static void updateProgress(int downloadedSize, int totalSize){
		// 아무것도 하지 않는다.
	}

	private static void makeFolder(String folder){
		File dir = new File(folder);
		if (!dir.exists()) {
			dir.mkdirs();
		}
	}
	
	public static float getPixelFromDip(Context context, float dipValue){
		float pixel= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,	dipValue, context.getResources().getDisplayMetrics());
		return pixel;
	}
	
	public static float getDipFromPixel(Context context, float pixel){
		float dip= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, pixel, context.getResources().getDisplayMetrics());
		return dip;
	}
	
	public static float toFloat(String v){
		try{
			return Float.parseFloat(v);
		}catch(Exception ex){
			return 0;
		}
	}

	// 2024.04.26 WOOIL
	public static boolean toBoolean(String v){
		try{
			return Boolean.parseBoolean(v);
		}catch(Exception ex){
			return false;
		}
	}

	public static String getFormattedDate(String v) {
		if (v == null) return "";
		if (v.equals("")) return "";
		if (v.length() < 8) return v;
		if (v.length() > 8) return v;

		return v.substring(0, 4) + "." + v.substring(4, 6) + "." + v.substring(6, 8);
	}

	public static String getFormattedTime(String v) {
		if (v == null) return "";
		if (v.equals("")) return "";
		if (v.length() == 4) {
			return v.substring(0, 2) + ":" + v.substring(2, 4);
		} else if (v.length() == 6) {
			return v.substring(0, 2) + ":" + v.substring(2, 4) + ":" + v.substring(4, 6);
		} else {
			return "";
		}
	}
}
