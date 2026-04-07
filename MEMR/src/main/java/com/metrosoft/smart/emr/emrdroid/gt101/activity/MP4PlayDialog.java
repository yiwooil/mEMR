package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.adapter.MP4Adapter;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ServletHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class MP4PlayDialog extends Activity implements OnItemClickListener, OnClickListener {

    protected ProgressDialog mDialog = null;
    protected Handler mHandler = new Handler();

    private boolean mDownloadAndSave;
    private String mXml;
    private String mPid;
    private String mBededt;
    private String mExdt;
    private String mSeq;

    private Button mBackButton;
    private Button mRunButton;
    private Button mPauseButton;
    private Button mStopButton;
    private ListView mListView;
    //private MP4Adapter mMP4Adapter;

    private MediaPlayer mMediaPlayer;
    //private ArrayList<HashMap<String,Object>> mArrayList;
    private String mMp4Path;
    private boolean isPlaying;
    private boolean isPause;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        Intent intent = getIntent();
        mPid = intent.getStringExtra("pid");
        mBededt = intent.getStringExtra("bededt");
        mExdt = intent.getStringExtra("exdt");
        mSeq = intent.getStringExtra("seq");

        setContentView(R.layout.mp4_play);

        mBackButton = (Button) findViewById(R.id.back_button);
        mRunButton = (Button) findViewById(R.id.run_button);
        mRunButton.setVisibility(View.GONE); // 일단 안보이게 처리
        mPauseButton = (Button) findViewById(R.id.pause_button);
        mStopButton = (Button) findViewById(R.id.stop_button);

        /** Layout으로 부터 ListView에 대한 객체를 얻는다. **/
        mListView = (ListView) findViewById(R.id.mp4_list);

        //mArrayList = new ArrayList<HashMap<String,Object>>();

        //mMP4Adapter = new MP4Adapter(this, mArrayList);

        //mListView.setAdapter(mMP4Adapter);

        mBackButton.setOnClickListener(this);
        mRunButton.setOnClickListener(this);
        mPauseButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);
        /* Listener for selecting a item */
        mListView.setOnItemClickListener(this);

        String dirPath = getFilesDir().getAbsolutePath();
        File dir = new File(dirPath);

        // 폴더가 없으면 생성
        if (!dir.exists()) {
            Log.d("EmrDroid", "폴더생성");
            dir.mkdirs();
        }

        // 안드로이드에 사인 이미지 파일을 쓴다.
        mMp4Path = dirPath + "/mp4444.mp4";

        isPlaying = false;
        isPause = false;

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                // TODO
                // Do something when playing is completed  
            }
        });
	    /*
        mMediaPlayer.setOnPreparedListener(new OnPreparedListener(){

			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				mp.start();
			}
       	
        });
        */
        setPlayList();

    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        if (view.getId() == R.id.back_button) {
            mMediaPlayer.stop();
            finish();
        } else if (view.getId() == R.id.run_button) {
            //
        } else if (view.getId() == R.id.pause_button) {
            if (!isPlaying) return;
            if (!isPause) {
                mMediaPlayer.pause();
                isPause = true;
                mPauseButton.setText("다시재생");
            } else {
                mMediaPlayer.start();
                isPause = false;
                mPauseButton.setText("일단정지");
            }
        } else if (view.getId() == R.id.stop_button) {
            mMediaPlayer.stop();
            isPlaying = false;
            isPause = false;
            mPauseButton.setText("일단정지");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // TODO Auto-generated method stub
        HashMap<String, Object> map = (HashMap<String, Object>) parent.getAdapter().getItem(position);
        String hospitalId = EmrSettingsUtil.getHospitalId(getBaseContext());
        String mp4Path = (String) map.get("mp4_path");
        String mp4Url = "EmrScanServlet?hospitalid=" + hospitalId + "&path=" + mp4Path + "&mode=4";
        Log.d("EmrDroid", "mp4Url=" + mp4Url);
        String mp4UrlFull = getFullUrl(mp4Url);
        Log.d("EmrDroid", "mp4UrlFull=" + mp4UrlFull);
        //playMusic(mp4UrlFull);
        downAndSave(mp4UrlFull);
    }

    private void downAndSave(final String url) {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                mDownloadAndSave = downloadAndSave(url);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterDownAndSave();
                            mDialog.dismiss();
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();
    }

    private void afterDownAndSave() {
        if (mDownloadAndSave) {
            this.playMusic(mMp4Path);
        } else {
            Log.d("EmrDroid", "download failure.");
        }
    }

    private void setPlayList() {
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true);
        new Thread(new Runnable() {
            public void run() {
                String hospitalId = EmrSettingsUtil.getHospitalId(getBaseContext());
                String url = "ChartServlet" +
                        "?hospitalid=" + hospitalId +
                        "&pid=" + mPid +
                        "&bededt=" + mBededt +
                        "&exdt=" + mExdt +
                        "&seq=" + mSeq +
                        "&mode=6";
                mXml = getXml(url);

                mHandler.post(new Runnable() {
                    public void run() {
                        // 조회중 화면이 전환되는 경우 dialog가 사라져서 오류가 발생한다.
                        // 이를 방지함.
                        try {
                            afterSetPlayList();
                            mDialog.dismiss();
                        } catch (Exception e) {
                            ;
                        }
                    }
                });
            }
        }).start();

    }

    private void afterSetPlayList() {
        ResultSetHelper rs;

        ArrayList<HashMap<String, Object>> mylist = new ArrayList<HashMap<String, Object>>();
        HashMap<String, Object> map = null;

        // xml해부
        try {
            // 리스트 지움.
            mListView.setAdapter(null);
            // 조회결과값
            String xml = mXml;
            Log.d("EmrDroid", "after : xml=" + xml);
            if (xml.equals("")) return;
            // xml to ResultSet
            rs = new ResultSetHelper(xml, EmrSettingsUtil.getMaskYn(getBaseContext()));
            if (rs.getReturnCode() < 0) {
                //showSimpleDialog(rs.getReturnDesc());
            } else if (rs.getReturnCode() == 0) {
                //showSimpleDialog(R.string.no_data_message);
            } else {

                for (int i = 0; i < rs.getRecordCount(); i++) {

                    String title = rs.getString(i, "title");
                    String mp4Path = rs.getString(i, "mp4_path");
                    String exdt = rs.getString(i, "exdt");
                    String seq = rs.getString(i, "seq");
                    String mp4Idx = rs.getString(i, "mp4_idx");

                    map = new HashMap<String, Object>();
                    map.put("title", title);
                    map.put("mp4_path", mp4Path);
                    map.put("exdt", exdt);
                    map.put("seq", seq);
                    map.put("mp4_idx", mp4Idx);
                    //
                    mylist.add(map);
                }

                MP4Adapter adapter = new MP4Adapter(this, mylist);
                mListView.setAdapter(adapter);
            }
        } catch (Exception ex) {
            //showSimpleDialog(ex.getMessage());
        }

    }

    public void playMusic(String mp4File) {
        try {
            FileInputStream fis = new FileInputStream(mp4File);
            FileDescriptor fd = fis.getFD();
            mMediaPlayer.reset();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setDataSource(fd);//(mp4File);
            fis.close();
            mMediaPlayer.setVolume(1, 1);
            //mMediaPlayer.prepareAsync();
            //mMediaPlayer.start();
            mMediaPlayer.prepare();
            mMediaPlayer.start();
            isPlaying = true;
            isPause = false;
	        /*
	        mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
	             public void onCompletion(MediaPlayer mp) {
	                 // TODO
	                 // Do something when playing is completed  
	             }
	        });
	        mMediaPlayer.setOnPreparedListener(new OnPreparedListener(){

				@Override
				public void onPrepared(MediaPlayer mp) {
					// TODO Auto-generated method stub
					mMediaPlayer.start();
				}
	        	
	        });
	        */

        } catch (IOException e) {
            Log.v("SimplePlayer", e.getMessage());
        }
    }

    protected String getXml(String url) {
        try {
            String servletUseYn = "";
            String servletIp = "";
            // 병원자료를 읽을 때 WAS가 Basecamp와 다른 서버에 접속해야하는지 정의
            servletUseYn = EmrSettingsUtil.getServletUseYn(getBaseContext());
            Log.d("EmrDroid", "servletUseYn = " + servletUseYn);
            servletIp = EmrSettingsUtil.getServletIp(getBaseContext());
            Log.d("EmrDroid", "servletIp = " + servletIp);
            if (servletUseYn.equalsIgnoreCase("y") == false) servletIp = "";

            Log.d("EmrDroid", "servletIp = " + servletIp);
            ServletHelper servletHelper = new ServletHelper();
            servletHelper.setServletIp(servletIp);
            return servletHelper.getXml(url);
        } catch (Exception e) {
            return null;
        }
    }

    protected String getFullUrl(String url) {
        String servletUseYn = "";
        String servletIp = "";
        // 병원자료를 읽을 때 WAS가 Basecamp와 다른 서버에 접속해야하는지 정의
        servletUseYn = EmrSettingsUtil.getServletUseYn(getBaseContext());
        Log.d("EmrDroid", "servletUseYn = " + servletUseYn);
        servletIp = EmrSettingsUtil.getServletIp(getBaseContext());
        Log.d("EmrDroid", "servletIp = " + servletIp);
        if (servletUseYn.equalsIgnoreCase("y") == false) servletIp = "";
        ServletHelper servletHelper = new ServletHelper();
        servletHelper.setServletIp(servletIp);

        return servletHelper.getFullUrl(url);
    }

    protected boolean downloadAndSave(String serverPath) {
        String servletUseYn = "";
        String servletIp = "";
        // 병원자료를 읽을 때 WAS가 Basecamp와 다른 서버에 접속해야하는지 정의
        servletUseYn = EmrSettingsUtil.getServletUseYn(getBaseContext());
        Log.d("EmrDroid", "servletUseYn = " + servletUseYn);
        servletIp = EmrSettingsUtil.getServletIp(getBaseContext());
        Log.d("EmrDroid", "servletIp = " + servletIp);
        if (servletUseYn.equalsIgnoreCase("y") == false) servletIp = "";
        ServletHelper servletHelper = new ServletHelper();
        servletHelper.setServletIp(servletIp);

        return servletHelper.downFileAndSave(serverPath, mMp4Path);
    }

}
