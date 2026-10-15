package com.metrosoft.smart.emr.emrdroid.gt101.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.metrosoft.smart.emr.emrdroid.gt101.R;
import com.metrosoft.smart.emr.emrdroid.gt101.helper.ResultSetHelper;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.Device;
import com.metrosoft.smart.emr.emrdroid.gt101.utils.EmrSettingsUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

public class ConsentFormCamera extends MyActivity {

    private static final int REQ_CAMERA = 1;
    private static final int REQ_CAMERA_PERMISSION = 2;

    private String mPid;
    private String mBededt;
    private String mBdiv;
    private String mDptcd;
    private String mBedodt;
    private String mDrid;
    private String mQfycd;
    private String mXmlPatientInfo;

    private String mRptcd = "";
    private String mRptnm = "";
    private TextView mRptnmTextView;
    private AlertDialog mRptDialog;
    private boolean mLoadingRptList;
    private boolean mDestroyed;
    private boolean mSaving;

    private ImageView mPhotoImageView;
    private Bitmap mPhotoBitmap;
    private String mPhotoPath = "";
    private String mPendingPhotoPath = "";
    private int mPreviewRequest;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // 환자 정보와 촬영 화면을 초기화하고 이전 선택값과 사진을 복원한다.
        super.onCreate(savedInstanceState, R.layout.consent_form_camera, getString(R.string.certificate_list));

        Intent intent = getIntent();
        mPid = getStringExtra(intent, "pid");
        mBededt = getStringExtra(intent, "bededt");
        mBdiv = getStringExtra(intent, "bdiv");
        mDptcd = getStringExtra(intent, "dptcd");
        mBedodt = getStringExtra(intent, "bedodt");
        mDrid = getStringExtra(intent, "drid");
        mQfycd = getStringExtra(intent, "qfycd");
        mXmlPatientInfo = getStringExtra(intent, "patientinfo");

        setQueryButton(false);
        setButton1(true, "촬영", BUTTON_TYPE_CAMERA);
        setButton2(true, "저장", BUTTON_TYPE_SAVE);
        setButton3(true, "지우기", BUTTON_TYPE_ERASER);

        TextView patientInfoTextView =
                (TextView) findViewById(R.id.patientInfoTextView);
        patientInfoTextView.setText(mXmlPatientInfo);

        mRptnmTextView = (TextView) findViewById(R.id.rptnmTextView);
        mPhotoImageView = (ImageView) findViewById(R.id.consentPhotoImageView);
        if (savedInstanceState != null) {
            mRptcd = savedInstanceState.getString("rptcd", "");
            mRptnm = savedInstanceState.getString("rptnm", "");
            mPhotoPath = savedInstanceState.getString("photoPath", "");
            mPendingPhotoPath = savedInstanceState.getString("pendingPhotoPath", "");
        }
        showSelectedConsentForm();

        View.OnClickListener selectConsentForm = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 동의서 선택 칸을 누르면 코드 목록을 조회한다.
                getRptcdList();
            }
        };
        mRptnmTextView.setOnClickListener(selectConsentForm);
        if (mPhotoPath.length() > 0) showCapturedPhoto();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // 화면 재생성에 대비해 동의서 선택값과 사진 경로를 저장한다.
        outState.putString("rptcd", mRptcd);
        outState.putString("rptnm", mRptnm);
        outState.putString("photoPath", mPhotoPath);
        outState.putString("pendingPhotoPath", mPendingPhotoPath);
        super.onSaveInstanceState(outState);
    }

    private void showSelectedConsentForm() {
        // 선택한 동의서를 명칭(코드) 형식으로 표시한다.
        mRptnmTextView.setText(mRptcd.length() == 0 && mRptnm.length() == 0
                ? "" : mRptnm + "(" + mRptcd + ")");
    }

    private void getRptcdList() {
        // 서버에서 동의서 코드와 명칭 목록을 조회한다.
        if (mSaving) return;
        // 연속 클릭으로 같은 조회나 선택창이 중복 실행되지 않도록 한다.
        if (mLoadingRptList || (mRptDialog != null && mRptDialog.isShowing())) return;

        final HashMap<String, String> param = new HashMap<String, String>();
        param.put("hospitalid", getHospitalId());
        param.put("userid", getUserId());
        param.put("mode", "23"); // 23: 동의서 코드 목록 조회

        mLoadingRptList = true;
        mDialog = ProgressDialog.show(this, "", getString(R.string.query_wait_message), true, false);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 백그라운드에서 동의서 목록을 요청하고 조회 결과를 전달한다.
                final String response = getXml("CertificatePaperServlet", param);
                final boolean error = getXmlError();
                final String errorMessage = getXmlErrorMessage();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 조회 대기를 종료하고 동의서 선택창 또는 오류를 표시한다.
                        mLoadingRptList = false;
                        if (mDialog != null) {
                            mDialog.dismiss();
                            mDialog = null;
                        }
                        if (mDestroyed || isFinishing()) return;

                        if (error) {
                            showSimpleDialog(errorMessage);
                        } else {
                            showRptcdList(response);
                        }
                    }
                });
            }
        }).start();
    }

    private void showRptcdList(String response) {
        // 서버 응답을 해석해 동의서 목록 선택창을 표시한다.
        try {
            // 기존 ResultSetHelper 응답 형식 사용. 데이터 필드명: rptcd, rptnm.
            ResultSetHelper rs = new ResultSetHelper(response, EmrSettingsUtil.getMaskYn(this));
            if (rs.getReturnCode() < 0) {
                showSimpleDialog(rs.getReturnDesc());
                return;
            }
            if (rs.getRecordCount() == 0) {
                showSimpleDialog(R.string.no_data_message);
                return;
            }

            final String[] codes = new String[rs.getRecordCount()];
            final String[] names = new String[rs.getRecordCount()];
            String[] items = new String[rs.getRecordCount()];
            int checkedItem = -1;
            for (int i = 0; i < rs.getRecordCount(); i++) {
                codes[i] = rs.getString(i, "rptcd");
                names[i] = rs.getString(i, "rptnm");
                items[i] = names[i] + "(" + codes[i] + ")";
                if (codes[i].equals(mRptcd)) checkedItem = i;
            }

            mRptDialog = new AlertDialog.Builder(this)
                    .setTitle("동의서 종류 선택")
                    .setSingleChoiceItems(items, checkedItem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // 선택한 동의서 코드와 명칭을 화면에 반영한다.
                            mRptcd = codes[which];
                            mRptnm = names[which];
                            showSelectedConsentForm();
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } catch (Exception e) {
            showSimpleDialog("동의서 목록을 읽을 수 없습니다. 서버 응답을 확인해주세요.");
        }
    }

    @Override
    protected void onDestroy() {
        // 화면 종료 시 사진 메모리와 대화상자를 정리한다.
        mDestroyed = true;
        mPhotoImageView.setImageDrawable(null);
        if (mPhotoBitmap != null) {
            mPhotoBitmap.recycle();
            mPhotoBitmap = null;
        }
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (mRptDialog != null) {
            mRptDialog.dismiss();
            mRptDialog = null;
        }
        super.onDestroy();
    }

    private String getStringExtra(Intent intent, String key) {
        // 전달된 문자열을 읽고 값이 없으면 빈 문자열을 반환한다.
        String value = intent.getStringExtra(key);
        return value == null ? "" : value;
    }

    @Override
    public void onClickButton1(View v) {
        // 촬영 버튼을 누르면 카메라 권한을 확인하고 촬영을 시작한다.
        if (mSaving) return;
        if (mPendingPhotoPath.length() > 0) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERMISSION);
            return;
        }
        startCamera();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 카메라 권한 승인 시 촬영을 시작하고 거부 시 안내한다.
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showSimpleDialog("촬영하려면 카메라 권한을 허용해주세요.");
            }
        }
    }

    private void startCamera() {
        // 사진 저장 파일을 준비하고 카메라 앱을 실행한다.
        try {
            File directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (directory == null || (!directory.exists() && !directory.mkdirs())) {
                throw new IOException("사진 저장 폴더를 만들 수 없습니다.");
            }
            File photoFile = File.createTempFile("consent_", ".jpg", directory);
            mPendingPhotoPath = photoFile.getAbsolutePath();
            Uri photoUri = FileProvider.getUriForFile(
                    this, getPackageName() + ".fileprovider", photoFile);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                intent.setClipData(ClipData.newRawUri("consent photo", photoUri));
            }
            startActivityForResult(intent, REQ_CAMERA);
        } catch (ActivityNotFoundException e) {
            deletePendingPhoto();
            showSimpleDialog("사용할 수 있는 카메라 앱이 없습니다.");
        } catch (Exception e) {
            deletePendingPhoto();
            showSimpleDialog("카메라를 실행할 수 없습니다. 권한과 저장 공간을 확인해주세요.");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // 촬영 결과를 확인해 사진을 표시하거나 취소된 임시 파일을 삭제한다.
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_CAMERA) return;

        if (resultCode == RESULT_OK && mPendingPhotoPath.length() > 0) {
            File photoFile = new File(mPendingPhotoPath);
            if (photoFile.exists() && photoFile.length() > 0) {
                String previousPhotoPath = mPhotoPath;
                mPhotoPath = mPendingPhotoPath;
                mPendingPhotoPath = "";
                showCapturedPhoto();
                if (previousPhotoPath.length() > 0 && !previousPhotoPath.equals(mPhotoPath)) {
                    new File(previousPhotoPath).delete();
                }
                return;
            }
            showSimpleDialog("촬영한 사진이 저장되지 않았습니다. 다시 촬영해주세요.");
        }
        // 촬영 취소 시 이전 사진은 유지하고 촬영용 임시 파일만 지운다.
        deletePendingPhoto();
    }

    private void deletePendingPhoto() {
        // 촬영용 임시 파일을 삭제하고 경로를 초기화한다.
        if (mPendingPhotoPath.length() > 0) {
            new File(mPendingPhotoPath).delete();
            mPendingPhotoPath = "";
        }
    }

    private void showCapturedPhoto() {
        // 촬영 사진을 백그라운드에서 읽어 화면에 표시한다.
        final String photoPath = mPhotoPath;
        final int request = ++mPreviewRequest;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 화면에 표시할 사진을 읽고 표시 결과를 전달한다.
                Bitmap preview = null;
                try {
                    preview = readPhotoPreview(photoPath);
                } catch (IOException | RuntimeException | OutOfMemoryError e) {
                    // 파일 오류나 메모리 부족은 UI 스레드에서 안내한다.
                }
                final Bitmap photo = preview;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 유효한 최신 사진만 화면에 반영하고 이전 사진 메모리를 해제한다.
                        if (mDestroyed || isFinishing() || request != mPreviewRequest) {
                            if (photo != null) photo.recycle();
                            return;
                        }
                        if (photo == null) {
                            showSimpleDialog("촬영한 사진을 표시할 수 없습니다. 다시 촬영해주세요.");
                            return;
                        }
                        Bitmap previous = mPhotoBitmap;
                        mPhotoBitmap = photo;
                        mPhotoImageView.setImageBitmap(photo);
                        if (previous != null) previous.recycle();
                    }
                });
            }
        }).start();
    }

    private Bitmap readPhotoPreview(String photoPath) throws IOException {
        // 사진을 표시용 크기로 축소하고 촬영 방향을 보정한다.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(photoPath, options);
        // 원본은 그대로 보관하고 화면 표시용 사진만 축소해 메모리 사용을 줄인다.
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 2048) {
            options.inSampleSize *= 2;
        }
        options.inJustDecodeBounds = false;
        Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
        if (bitmap == null) return null;

        int orientation = ExifInterface.ORIENTATION_NORMAL;
        try {
            orientation = new ExifInterface(photoPath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        } catch (IOException e) {
            // 방향 정보가 없는 사진은 원래 방향으로 표시한다.
        }
        return rotatePhotoBitmap(bitmap, orientation);
    }

    private Bitmap rotatePhotoBitmap(Bitmap bitmap, int orientation) {
        // EXIF 방향에 따라 사진을 회전 또는 반전하고 이전 비트맵을 해제한다.
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setScale(1, -1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(270);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(270);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) bitmap.recycle();
            return rotated;
        } catch (RuntimeException | OutOfMemoryError e) {
            bitmap.recycle();
            throw e;
        }
    }

    private File createUploadJpg(String photoPath) throws IOException {
        // 원본 해상도의 사진에 방향 보정을 적용하여 업로드용 JPG를 만든다.
        Bitmap bitmap = null;
        File uploadFile = null;
        try {
            int orientation = new ExifInterface(photoPath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            bitmap = BitmapFactory.decodeFile(photoPath, options);
            if (bitmap == null) {
                throw new IOException("촬영한 사진을 읽을 수 없습니다.");
            }
            bitmap = rotatePhotoBitmap(bitmap, orientation);

            uploadFile = File.createTempFile("consent_upload_", ".jpg", getCacheDir());
            try (FileOutputStream output = new FileOutputStream(uploadFile)) {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output)) {
                    throw new IOException("방향을 보정한 사진을 저장하지 못했습니다.");
                }
                output.flush();
            }

            // 픽셀에 이미 보정했으므로 뷰어에서 다시 회전하지 않도록 한다.
            ExifInterface exif = new ExifInterface(uploadFile.getAbsolutePath());
            exif.setAttribute(ExifInterface.TAG_ORIENTATION,
                    String.valueOf(ExifInterface.ORIENTATION_NORMAL));
            exif.saveAttributes();
            return uploadFile;
        } catch (IOException | RuntimeException | OutOfMemoryError e) {
            if (uploadFile != null) uploadFile.delete();
            throw e;
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
        }
    }

    @Override
    public void onClickButton2(View v) {
        // 환자와 동의서, 촬영 파일을 확인하고 저장을 시작한다.
        if (mSaving || mLoadingRptList || mPendingPhotoPath.length() > 0) return;

        if (mPid.length() == 0) {
            showSimpleDialog("환자 정보가 없습니다.");
            return;
        }
        if (mRptcd.length() == 0) {
            showSimpleDialog("동의서를 선택해주세요.");
            return;
        }

        File photoFile = new File(mPhotoPath);
        if (mPhotoPath.length() == 0
                || !photoFile.isFile()
                || !photoFile.canRead()
                || photoFile.length() == 0) {
            showSimpleDialog("저장할 사진을 촬영해주세요.");
            return;
        }

        saveCameraImage();
    }

    private void saveCameraImage() {
        // 방향을 보정한 JPG를 업로드한 뒤 선택한 동의서 코드로 파일 정보를 저장한다.
        final String photoPath = mPhotoPath;

        setSaving(true);
        mDialog = ProgressDialog.show(
                this, "", getString(R.string.process_wait_message), true, false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 사진 방향을 보정하고 서버 파일 업로드 및 DB 저장을 처리한다.
                String errorMessage = null;
                File uploadPhotoFile = null;

                try {
                    uploadPhotoFile = createUploadJpg(photoPath);
                    ResultSetHelper saveInfo = getCameraSaveInfo("");
                    String sysdt = saveInfo.getString(0, "sysdt");
                    String systm = saveInfo.getString(0, "systm");
                    String applyExdt = sysdt;

                    String uploadFileName = getCameraUploadFileName(sysdt, systm);

                    // 기존 ConsentForm과 같은 일반 동의서 이미지 저장 경로를 사용한다.
                    // 방향을 보정한 업로드 파일의 형식과 확장자는 JPEG(.jpg)이다.
                    String addParam = "file_type=jpg&pre_save=&hospital_id="
                            + getHangul(getHospitalId());

                    String response = uploadPngFile(
                            uploadPhotoFile.getAbsolutePath(), uploadFileName, addParam);

                    if (response == null || !response.trim().startsWith("success")) {
                        throw new IOException(response == null
                                ? "사진 업로드 중 오류가 발생했습니다."
                                : response);
                    }

                    // 업로드하는 동안 변경되었을 수 있는 SEQ를 다시 조회한다.
                    saveInfo = getCameraSaveInfo(applyExdt);
                    int seq = saveInfo.getInt(0, "seq");

                    HashMap<String, String> param = new HashMap<String, String>();
                    param.put("hospitalid", getHospitalId());
                    param.put("userid", getUserId());
                    param.put("mode", "24");
                    param.put("pid", mPid);
                    param.put("bededt", mBededt);
                    param.put("bdiv", mBdiv);
                    param.put("rptcd", mRptcd);
                    param.put("dptcd", mDptcd);
                    param.put("drid", mDrid);
                    param.put("qfycd", mQfycd);
                    param.put("filename", getHangul(uploadFileName));
                    param.put("apply_exdt", applyExdt);
                    param.put("seq", String.valueOf(seq));
                    param.put("sysdt", sysdt);
                    param.put("systm", systm);

                    response = requestCertificate(param);
                    if (!response.startsWith("success")) {
                        throw new IOException(response);
                    }
                } catch (OutOfMemoryError e) {
                    errorMessage = "사진 방향을 보정할 메모리가 부족합니다. 다시 촬영해주세요.";
                } catch (Exception e) {
                    errorMessage = e.getMessage();
                    if (errorMessage == null || errorMessage.length() == 0) {
                        errorMessage = "촬영동의서 저장 중 오류가 발생했습니다.";
                    }
                } finally {
                    // 업로드 결과와 관계없이 보정용 임시 파일은 정리하고 원본은 유지한다.
                    if (uploadPhotoFile != null) uploadPhotoFile.delete();
                }

                final String resultMessage = errorMessage;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // 저장 결과를 안내하고 성공하면 촬영동의서 화면을 종료한다.
                        if (mDialog != null) {
                            mDialog.dismiss();
                            mDialog = null;
                        }
                        if (mDestroyed || isFinishing()) return;

                        setSaving(false);

                        if (resultMessage != null) {
                            showSimpleDialog(resultMessage);
                            return;
                        }

                        new AlertDialog.Builder(ConsentFormCamera.this)
                                .setMessage(R.string.save_success)
                                .setCancelable(false)
                                .setPositiveButton(R.string.ok,
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(
                                                    DialogInterface dialog, int which) {
                                                // 저장 완료를 알리고 이전 화면으로 돌아간다.
                                                setResult(RESULT_OK);
                                                finish();
                                            }
                                        })
                                .show();
                    }
                });
            }
        }).start();
    }

    private ResultSetHelper getCameraSaveInfo(String applyExdt) throws Exception {
        // 선택한 동의서 코드를 전달하여 서버 시각과 저장할 SEQ를 조회한다.
        HashMap<String, String> param = new HashMap<String, String>();
        param.put("hospitalid", getHospitalId());
        param.put("userid", getUserId());
        param.put("mode", "2");
        param.put("pid", mPid);
        param.put("bededt", mBededt);
        param.put("bdiv", mBdiv);
        param.put("rptcd", mRptcd);
        param.put("dptcd", mDptcd);
        param.put("drid", mDrid);
        param.put("qfycd", mQfycd);
        param.put("presave", "");
        param.put("apply_exdt", applyExdt);

        ResultSetHelper rs = new ResultSetHelper(
                requestCertificate(param),
                EmrSettingsUtil.getMaskYn(getBaseContext()));

        if (rs.getReturnCode() < 0) {
            throw new IOException(rs.getReturnDesc());
        }
        if (rs.getRecordCount() == 0) {
            throw new IOException("저장 시각과 순번을 조회하지 못했습니다.");
        }
        return rs;
    }

    private String getCameraUploadFileName(String sysdt, String systm) throws Exception {
        // 기존 파일명 구조를 따라 선택한 코드를 사용하고 파일명 중복을 확인한다.
        String inoutdiv = "1".equals(mBdiv) ? "O" : "I";
        String ipAddress = Device.getIpAddress();
        if (ipAddress == null) ipAddress = "null";
        ipAddress = ipAddress.replace(".", "_");

        String appVersion = "v" + getAppVersion().replace(".", "_");
        String prefix = sysdt.substring(0, 4) + "/"
                + sysdt + "/" + mPid + "/" + inoutdiv + "/"
                + mRptcd + "_" + sysdt + "_" + systm + "_"
                + ipAddress + "_" + appVersion + "_p";

        for (int imageSeq = 1; imageSeq <= 999; imageSeq++) {
            String fileName = prefix
                    + String.format(Locale.US, "%03d", imageSeq) + ".jpg";

            HashMap<String, String> param = new HashMap<String, String>();
            param.put("hospitalid", getHospitalId());
            param.put("userid", getUserId());
            param.put("mode", "18");
            param.put("pid", mPid);
            param.put("bededt", mBededt);
            param.put("bdiv", mBdiv);
            param.put("rptcd", mRptcd);
            param.put("dptcd", mDptcd);
            param.put("drid", mDrid);
            param.put("qfycd", mQfycd);
            param.put("file_name", getHangul(fileName));
            param.put("file_type", "jpg");
            param.put("pre_save", "");

            String response = requestCertificate(param);
            if ("N".equalsIgnoreCase(response)) return fileName;
            if (!"Y".equalsIgnoreCase(response)) {
                throw new IOException("파일명 중복 확인에 실패했습니다.");
            }
        }

        throw new IOException("저장할 파일명을 생성하지 못했습니다.");
    }

    private String requestCertificate(HashMap<String, String> param) throws IOException {
        // 동의서 서블릿을 호출하고 통신 오류나 빈 응답을 확인한다.
        String response = getXml("CertificatePaperServlet", param);

        if (getXmlError()) {
            throw new IOException(getXmlErrorMessage());
        }
        if (response == null || response.trim().length() == 0) {
            throw new IOException("서버 응답이 없습니다.");
        }
        return response.trim();
    }

    private String getAppVersion() {
        // 서버에 저장할 파일명에 사용할 앱 버전을 반환한다.
        try {
            String version = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionName;
            return version == null ? "unknown" : version;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private void setSaving(boolean saving) {
        // 저장 중에는 중복 저장과 동의서 및 사진 변경을 막는다.
        mSaving = saving;
        mRptnmTextView.setEnabled(!saving);

        int[] buttonIds = {
                R.id.button1CTB,
                R.id.button2CTB,
                R.id.button3CTB
        };
        for (int buttonId : buttonIds) {
            findViewById(buttonId).setEnabled(!saving);
        }
    }

    @Override
    public void onClickButton3(View v) {
        // 촬영 사진이 있으면 저장 파일과 화면 이미지를 삭제한다.
        if (mSaving) return;
        if (mPhotoPath.length() == 0 && mPhotoBitmap == null) return;

        if (mPhotoPath.length() > 0) {
            File photoFile = new File(mPhotoPath);
            if (photoFile.exists() && !photoFile.delete()) {
                showSimpleDialog("촬영한 사진을 지울 수 없습니다. 다시 시도해주세요.");
                return;
            }
        }

        // 삭제 전에 시작된 이미지 로딩 결과가 화면에 다시 표시되지 않도록 한다.
        mPreviewRequest++;
        mPhotoPath = "";
        mPhotoImageView.setImageDrawable(null);
        if (mPhotoBitmap != null) {
            mPhotoBitmap.recycle();
            mPhotoBitmap = null;
        }
    }
}
