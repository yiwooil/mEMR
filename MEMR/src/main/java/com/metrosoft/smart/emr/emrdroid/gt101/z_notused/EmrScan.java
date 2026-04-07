package com.metrosoft.smart.emr.emrdroid.gt101.z_notused;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
//import android.widget.ImageView;
import android.widget.GridView;
//import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.activity.MyActivity;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.EmrScanAdapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ServletHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;


public class EmrScan extends MyActivity {
	private String xmlPatientInfo,xml;
	private String pid;
	private String bededt;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		super.onCreate(savedInstanceState, R.layout.emr_scan, "< " + getString(R.string.inpatient_list));

		Intent intent = getIntent();
		pid = intent.getStringExtra("pid");
		bededt = intent.getStringExtra("bededt");

		((TextView)findViewById(R.id.test)).setText("zzz");

		// 이벤트연결
		((GridView)findViewById(R.id.emrScanLlist)).setOnItemClickListener(new GridView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
				HashMap<String,Object> selectedMap=(HashMap<String,Object>)parent.getAdapter().getItem(position);
				Intent intent = new Intent(EmrScan.this,EmrScanView_old.class);
				intent.putExtra("pid", pid);
				intent.putExtra("bededt", bededt);
				intent.putExtra("path", (String)selectedMap.get("path"));
				startActivity(intent);
			}
		});

		if (savedInstanceState==null) {
			getEmrScan();
		}
		else {
			xmlPatientInfo=savedInstanceState.getString("xmlPatientInfo");
			xml=savedInstanceState.getString("xml");
			afterGetEmrScan();
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putString("xmlPatientInfo", xmlPatientInfo);
		outState.putString("xml", xml);
	}

	@Override
	public void onClickQueryButton(View v) {
		getEmrScan();
	}

	private void getEmrScan() {
		mDialog = ProgressDialog.show(EmrScan.this, "",getString(R.string.query_wait_message), true);
		new Thread(new Runnable() {
			public void run() {
				String hospitalId=getHospitalId();
				String userId=getUserId();
				String url="";
				// 환자정보
				url = "InPatientInformationServlet?hospitalid=" + hospitalId + "&pid=" + pid + "&bededt=" + bededt;
				xmlPatientInfo = getXml(url);
				// 기타서식
				url = "ChartServlet?mode=1&hospitalid=" + hospitalId + "&pid=" + pid + "&bededt=" + bededt ;
				xml = getXml(url);
				mHandler.post(new Runnable() {
					public void run() {
						// 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
						// 이를 방지함.
						try {
							afterGetEmrScan();
							mDialog.dismiss();
						}catch(Exception e) {
							;
						}
					}
				});
			}
		}).start();;
	}

	private void afterGetEmrScan() {
		((TextView)findViewById(R.id.patientInfoTextView)).setText(xmlPatientInfo);

		//ListView list=(ListView)findViewById(R.id.emrScanLlist);
		GridView list=(GridView)findViewById(R.id.emrScanLlist);

		ArrayList<HashMap<String,Object>> mylist = new ArrayList<HashMap<String,Object>>();
		HashMap<String,Object> map = null;

		ResultSetHelper rs;

		// xml해부
		try {
			// 오류발생
			if(super.getXmlError()==true) {
				super.showToastText(super.getXmlErrorMessage());
				return;
			}

			rs = new ResultSetHelper(xml,EmrSettingsUtil.getMaskYn(getBaseContext()));

			if (rs.getReturnCode()<0) {
				showSimpleDialog(rs.getReturnDesc());
			}
			else if (rs.getReturnCode()==0) {
				showSimpleDialog(R.string.no_data_message);
			}
			else {
				Toast.makeText(this, rs.getReturnCode() + getString(R.string.query_count_message), Toast.LENGTH_SHORT).show();

				for (int i=0 ; i<rs.getRecordCount() ; i++) {
					map = new HashMap<String,Object>();
					map.put("exdt", rs.getString(i,"exdt"));
					map.put("seq", rs.getString(i,"seq"));
					map.put("rptcd", rs.getString(i,"rptcd"));
					map.put("rptnm", rs.getString(i,"rptnm"));
					map.put("path", rs.getString(i,"path"));
					mylist.add(map);
				}

				EmrScanAdapter adapter = null;
				adapter = new EmrScanAdapter(this,mylist, null);
				list.setAdapter(adapter);

			}
		}
		catch(Exception ex) {
			showSimpleDialog(ex.getMessage());
		}
	}

	// 출처 : http://blog.naver.com/PostView.nhn?blogId=huewu&logNo=110095553797&redirect=Dlog&widgetTypeCall=true
	public class BitmapCache extends HashMap<Integer, SoftReference<Bitmap>>{

		public Bitmap get(Integer key) {
			SoftReference<Bitmap> ref = super.get(key);
			if (ref==null) {
				return null;
			}
			else {
				return ref.get();
			}
		}

		public void put(Integer key,Bitmap bitmap) {
			super.put(key, new SoftReference<Bitmap>(bitmap));
		}
	}

	// 내가 만든 스레드...
	private class ImageDownload {
		private final WeakReference<ImageView> imageViewReference;
		private String url;
		private Bitmap bitmap;
		private int position;
		private BitmapCache bitmapCache;

		public ImageDownload(String url,ImageView imageView,int position,BitmapCache bitmapCache) throws Exception {
			this.url=url;
			this.imageViewReference=new WeakReference<ImageView>(imageView);
			this.position=position;
			this.bitmapCache=bitmapCache;
		}

		public void download() throws Exception {
			new Thread(new Runnable() {
				public void run() {
					try {
						ServletHelper servletHelper = new ServletHelper();
						// bitmap의 크기가 너무 크면 OutOfMemoryError가 발생한다.
						// 그래서 64 x 64 크기로 가져온다.
						bitmap = servletHelper.getBitmap(url,64,64);
						mHandler.post(new Runnable() {
							public void run() {
								afterDownload();
							}
						});
					}
					catch(Exception e) {
						Log.d("EmrDroid","getBitmap Error : " + e.getMessage());
					}
				}
			}).start();
		}

		public void afterDownload() {
			try {
				ImageView imageView = imageViewReference.get();
				if (imageView!=null) {
					imageView.setImageBitmap(bitmap);
//	    			bitmapCache.put(position, bitmap);
				}
			}
			catch(Exception e) {
				Log.d("EmrDroid","afterDownload Error : " + e.getMessage());
			}
		}
	}


	// 아래 코딩의 출처
	// http://blog.naver.com/PostView.nhn?blogId=huewu&logNo=110090363656
	// 원본  http://android-developers.blogspot.com/2010/07/multithreading-for-performance.html

	static Bitmap downloadBitmap(String url) {
//    	ServletHelper servlerHelper = new ServletHelper();
//    	Bitmap bitmap = servlerHelper.getBitmap(url);
//    	return bitmap;
/*    	
    	url = "http://180.70.20.31:8080/servlet/" + url;
    	
        final AndroidHttpClient client = AndroidHttpClient.newInstance("Android");
        final HttpGet getRequest = new HttpGet(url);

        try {
            HttpResponse response = client.execute(getRequest);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) { 
                Log.d("EmrDroid", "Error " + statusCode + " while retrieving bitmap from " + url); 
                return null;
            }
            
            final HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inputStream = null;
                try {
                    inputStream = entity.getContent(); 
                    final Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    return bitmap;
                } 
                finally {
                    if (inputStream != null) {
                        inputStream.close();  
                    }
                    entity.consumeContent();
                }
            }
        } 
        catch (Exception e) {
            // Could provide a more explicit error message for IOException or IllegalStateException
            getRequest.abort();
            Log.d("EmrDroid", "Error while retrieving bitmap from " + url + " --> " + e.toString());
        } 
        finally {
            if (client != null) {
                client.close();
            }
        }
*/
		return null;
	}

	private static boolean cancelPotentialDownload(String url, ImageView imageView) {
		BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);

		if (bitmapDownloaderTask != null) {
			String bitmapUrl = bitmapDownloaderTask.url;
			if ((bitmapUrl == null) || (!bitmapUrl.equals(url))) {
				bitmapDownloaderTask.cancel(true);
			} else {
				// The same URL is already being downloaded.
				return false;
			}
		}
		return true;
	}

	private static BitmapDownloaderTask getBitmapDownloaderTask(ImageView imageView) {
		if (imageView != null) {
			Drawable drawable = imageView.getDrawable();
			if (drawable instanceof DownloadedDrawable) {
				DownloadedDrawable downloadedDrawable = (DownloadedDrawable)drawable;
				return downloadedDrawable.getBitmapDownloaderTask();
			}
		}
		return null;
	}

	public class ImageDownloader {

		public void download(String url, ImageView imageView) {
//            BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
//            task.execute(url);
			if (cancelPotentialDownload(url, imageView)) {
				BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
				DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
				imageView.setImageDrawable(downloadedDrawable);
				task.execute(url);//                task.execute(url, cookie);
			}
		}
	}

	/* class BitmapDownloaderTask, see below */
	class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
		private String url;
		private final WeakReference<ImageView> imageViewReference;

		public BitmapDownloaderTask(ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		@Override
		// Actual download method, run in the task thread
		protected Bitmap doInBackground(String... params) {
			// params comes from the execute() call: params[0] is the url.
			return downloadBitmap(params[0]);
		}

		@Override
		// Once the image is downloaded, associates it to the imageView
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}

//            if (imageViewReference != null) {
//                ImageView imageView = imageViewReference.get();
//                if (imageView != null) {
//                    imageView.setImageBitmap(bitmap);
//                }
//            }
			if (imageViewReference != null) {
				ImageView imageView = imageViewReference.get();
				BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
				// Change bitmap only if this process is still associated with it
				if (this == bitmapDownloaderTask) {
					imageView.setImageBitmap(bitmap);
				}
			}
		}
	}

	static class DownloadedDrawable extends ColorDrawable {
		private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

		public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
			super(Color.BLACK);
			bitmapDownloaderTaskReference =
					new WeakReference<BitmapDownloaderTask>(bitmapDownloaderTask);
		}

		public BitmapDownloaderTask getBitmapDownloaderTask() {
			return bitmapDownloaderTaskReference.get();
		}
	}


}
