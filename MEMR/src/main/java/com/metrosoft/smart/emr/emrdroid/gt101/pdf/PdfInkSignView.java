package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v7.widget.AppCompatImageView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * PDF 문서를 화면에 표시하고,
 * - PDF 내부 Ink Annotation
 * - PDF 내부 AcroForm Field
 * - 앱에서 직접 그린 펜 Stroke
 * - 앱에서 올린 서명 Bitmap
 * 을 함께 표시하는 View
 *
 * 추가 기능:
 * - FingerPaintView3 방식의 두 손가락 확대/축소
 * - 두 손가락 같은 방향 이동 시 pan
 * - 확대된 상태에서 MODE_NONE일 때 한 손가락 pan
 * - 더블탭으로 원래 배율 복귀
 * - MODE_EDIT일 때 text field / checkbox 편집
 *
 * 주의:
 * - PdfRenderer는 Android 5.0(Lollipop) 이상에서만 동작한다.
 * - AcroForm field는 PdfFormFieldReader.readAllFields()에서 읽어온다.
 * - PdfRenderedFormField.type 값을 이용해 text/checkbox/radio/button/signature 등을 분기 표시한다.
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PdfInkSignView extends AppCompatImageView {

    public static final int MODE_NONE = 0;
    public static final int MODE_PEN = 1;
    public static final int MODE_ERASER = 2;
    public static final int MODE_MOVE_SIGNATURE = 3;
    public static final int MODE_EDIT = 4;

    // PDF 원본 페이지를 그릴 때 사용하는 Paint
    private final Paint pdfPaint = new Paint(Paint.DITHER_FLAG);

    // 사용자가 화면에 그리는 stroke용 Paint
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 서명 bitmap의 외곽 프레임용 Paint
    private final Paint signatureFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // PDF form field text 표시용 Paint
    private final Paint formTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap pageBitmap;
    private ParcelFileDescriptor pfd;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private int currentPageIndex = 0;

    // 현재 PDF 페이지의 실제 크기(pdf 좌표계 기준)
    private int currentPdfPageWidth;
    private int currentPdfPageHeight;

    // 현재 페이지에서 앱이 직접 그린 stroke들
    private final List<PdfInkStroke> strokes = new ArrayList<PdfInkStroke>();
    private PdfInkStroke currentStroke;

    // 현재 페이지의 서명 overlay
    private PdfSignatureOverlay signatureOverlay = new PdfSignatureOverlay();

    // 페이지별 stroke 저장
    private final HashMap<Integer, ArrayList<PdfInkStroke>> mPageStrokes =
            new HashMap<Integer, ArrayList<PdfInkStroke>>();

    // 페이지별 signature 저장
    private final HashMap<Integer, PdfSignatureOverlay> mPageSignatures =
            new HashMap<Integer, PdfSignatureOverlay>();

    // PDF 내부에 원래 저장되어 있던 Ink Annotation
    private final List<PdfRenderedInkAnnotation> mRenderedAnnotations =
            new ArrayList<PdfRenderedInkAnnotation>();

    private File mCurrentPdfFile;

    // PDF 내부 AcroForm field 표시용
    private final List<PdfRenderedFormField> mRenderedFormFields =
            new ArrayList<PdfRenderedFormField>();

    // 디버깅용 문자열
    private final List<String> mDebugTextList = new ArrayList<String>();

    private int mode = MODE_NONE;
    private int penColor = Color.RED;
    private float penWidthPx = 4f;
    private float eraserHitPx = 40f;

    private float lastTouchX;
    private float lastTouchY;

    // 더블탭 감지용
    private GestureDetector mGestureDetector;

    // 현재 배율
    private float mScaleFactor = 1.0f;

    // 최소/최대 배율
    private float mMinScaleFactor = 1.0f;
    private float mMaxScaleFactor = 2.0f;

    // 현재 화면 이동값
    private float mTranslateX = 0f;
    private float mTranslateY = 0f;

    // 한 손가락 pan 시작 좌표
    private float mLastPanX = 0f;
    private float mLastPanY = 0f;

    // 두 손가락 pan 중심점
    private float mLastMultiTouchCenterX = 0f;
    private float mLastMultiTouchCenterY = 0f;

    // 확대/축소용 이전 두 손가락 거리
    private float mPrevDistance = 0f;

    // 상태 플래그
    private boolean mIsScaling = false;
    private boolean mIsPanning = false;
    private boolean mIsTwoFingerPanning = false;

    // 드래그 시작 임계값
    private int mTouchSlop = 0;

    // 사용자가 수정한 PDF form 값 보관
    private final HashMap<String, String> mEditedFieldValues =
            new HashMap<String, String>();

    // text field 클릭 시 Activity 쪽으로 전달하기 위한 리스너
    private OnPdfFieldEditListener mOnPdfFieldEditListener;

    // 탭 판정용
    private float mDownX = 0f;
    private float mDownY = 0f;
    private boolean mSingleTapCandidate = false;

    public PdfInkSignView(Context context) {
        super(context);
        init();
    }

    /**
     * 초기 Paint 및 View 속성 설정
     */
    private void init() {
        setFocusable(true);
        setClickable(true);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        signatureFramePaint.setStyle(Paint.Style.STROKE);
        signatureFramePaint.setStrokeWidth(2f);
        signatureFramePaint.setColor(Color.argb(180, 50, 50, 50));

        formTextPaint.setColor(Color.BLACK);
        formTextPaint.setStyle(Paint.Style.FILL);
        formTextPaint.setTextAlign(Paint.Align.LEFT);
        formTextPaint.setTypeface(Typeface.DEFAULT);

        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        mGestureDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        resetZoom();
                        return true;
                    }

                    @Override
                    public boolean onDown(MotionEvent e) {
                        return true;
                    }
                });
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setPenColor(int color) {
        this.penColor = color;
    }

    public void setPenWidthPx(float widthPx) {
        this.penWidthPx = widthPx;
    }

    public void setEraserHitPx(float hitPx) {
        this.eraserHitPx = hitPx;
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public int getPdfPageCount() {
        return pdfRenderer != null ? pdfRenderer.getPageCount() : 0;
    }

    public Bitmap getPageBitmap() {
        return pageBitmap;
    }

    public int getCurrentPdfPageWidth() {
        return currentPdfPageWidth;
    }

    public int getCurrentPdfPageHeight() {
        return currentPdfPageHeight;
    }

    public List<PdfInkStroke> getCurrentPageStrokes() {
        return strokes;
    }

    public PdfSignatureOverlay getCurrentPageSignatureOverlay() {
        return signatureOverlay;
    }

    public HashMap<Integer, ArrayList<PdfInkStroke>> getAllPageStrokes() {
        saveCurrentPageOverlay();
        return mPageStrokes;
    }

    public HashMap<Integer, PdfSignatureOverlay> getAllPageSignatures() {
        saveCurrentPageOverlay();
        return mPageSignatures;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

    /**
     * 확대/축소/이동 상태를 초기화한다.
     */
    public void resetZoom() {
        mScaleFactor = 1.0f;
        mTranslateX = 0f;
        mTranslateY = 0f;

        mIsScaling = false;
        mIsPanning = false;
        mIsTwoFingerPanning = false;

        mLastPanX = 0f;
        mLastPanY = 0f;
        mLastMultiTouchCenterX = 0f;
        mLastMultiTouchCenterY = 0f;
        mPrevDistance = 0f;

        invalidate();
    }

    /**
     * 전체 overlay를 모두 삭제한다.
     */
    public void clearAllOverlays() {
        strokes.clear();
        currentStroke = null;
        signatureOverlay = new PdfSignatureOverlay();

        mPageStrokes.clear();
        mPageSignatures.clear();

        mRenderedAnnotations.clear();
        mRenderedFormFields.clear();
        mDebugTextList.clear();
        mEditedFieldValues.clear();

        invalidate();
    }

    /**
     * 현재 페이지 overlay만 삭제한다.
     */
    public void clearCurrentPageOverlay() {
        strokes.clear();
        currentStroke = null;
        signatureOverlay = new PdfSignatureOverlay();

        saveCurrentPageOverlay();
        invalidate();
    }

    /**
     * PDF를 열고 특정 페이지를 표시한다.
     */
    public void openPdf(File pdfFile, int pageIndex) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw new IOException("PdfRenderer는 Android 5.0 이상에서만 지원됩니다.");
        }

        closePdf();

        pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
        pdfRenderer = new PdfRenderer(pfd);

        if (pdfRenderer.getPageCount() <= 0) {
            throw new IOException("PDF 페이지가 없습니다.");
        }

        mDebugTextList.clear();
        mDebugTextList.add("openPdf 시작");

        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= pdfRenderer.getPageCount()) pageIndex = pdfRenderer.getPageCount() - 1;

        currentPageIndex = pageIndex;

        resetZoom();
        openRendererPage(currentPageIndex);
        restorePageOverlay(currentPageIndex);

        mDebugTextList.add("annotation 읽기 in openPdf");
        try {
            List<PdfRenderedInkAnnotation> annots =
                    PdfInkAnnotationReader.readInkAnnotations(getContext(), pdfFile, currentPageIndex);
            setRenderedAnnotations(annots);
        } catch (Exception e) {
            mRenderedAnnotations.clear();
            mDebugTextList.add("annotation 읽기 오류: " + e.getMessage());
        }

        mDebugTextList.add("form-field 읽기 in openPdf");
        try {
            List<PdfRenderedFormField> fields =
                    PdfFormFieldReader.readAllFields(getContext(), pdfFile, currentPageIndex, mDebugTextList);

            for (PdfRenderedFormField f : fields) {
                String type = "";
                try {
                    type = f.type;
                } catch (Throwable ignore) {
                }
                mDebugTextList.add("form-field = " + safe(f.name) + " / " + safe(type) + " / " + safe(f.value));
            }

            setRenderedFormFields(fields);
        } catch (Exception e) {
            mRenderedFormFields.clear();
            mDebugTextList.add("form-field 읽기 오류: " + e.getMessage());
        }

        mCurrentPdfFile = pdfFile;

        requestLayout();
        invalidate();
    }

    /**
     * 현재 열려 있는 PDF의 다른 페이지를 표시한다.
     */
    public void showPage(int pageIndex) throws IOException {
        if (pdfRenderer == null) return;

        saveCurrentPageOverlay();

        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= pdfRenderer.getPageCount()) pageIndex = pdfRenderer.getPageCount() - 1;

        currentPageIndex = pageIndex;

        resetZoom();
        openRendererPage(currentPageIndex);
        restorePageOverlay(currentPageIndex);

        try {
            if (mCurrentPdfFile != null) {
                List<PdfRenderedInkAnnotation> annots =
                        PdfInkAnnotationReader.readInkAnnotations(getContext(), mCurrentPdfFile, currentPageIndex);
                setRenderedAnnotations(annots);
            }
        } catch (Exception e) {
            mRenderedAnnotations.clear();
            mDebugTextList.add("annotation 읽기 오류(showPage): " + e.getMessage());
        }

        try {
            if (mCurrentPdfFile != null) {
                List<PdfRenderedFormField> fields =
                        PdfFormFieldReader.readAllFields(getContext(), mCurrentPdfFile, currentPageIndex, mDebugTextList);
                setRenderedFormFields(fields);
            }
        } catch (Exception e) {
            mRenderedFormFields.clear();
            mDebugTextList.add("form-field 읽기 오류(showPage): " + e.getMessage());
        }

        requestLayout();
        invalidate();
    }

    public boolean showNextPage() throws IOException {
        if (pdfRenderer == null) return false;
        if (currentPageIndex + 1 >= pdfRenderer.getPageCount()) return false;
        showPage(currentPageIndex + 1);
        return true;
    }

    public boolean showPreviousPage() throws IOException {
        if (pdfRenderer == null) return false;
        if (currentPageIndex - 1 < 0) return false;
        showPage(currentPageIndex - 1);
        return true;
    }

    /**
     * PdfRenderer로 특정 페이지를 bitmap으로 렌더링한다.
     */
    private void openRendererPage(int pageIndex) throws IOException {
        if (currentPage != null) {
            currentPage.close();
            currentPage = null;
        }

        if (pageBitmap != null && !pageBitmap.isRecycled()) {
            pageBitmap.recycle();
            pageBitmap = null;
        }

        currentPage = pdfRenderer.openPage(pageIndex);

        currentPdfPageWidth = currentPage.getWidth();
        currentPdfPageHeight = currentPage.getHeight();

        int renderW = currentPdfPageWidth * 2;
        int renderH = currentPdfPageHeight * 2;

        pageBitmap = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888);
        pageBitmap.eraseColor(Color.WHITE);
        currentPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
    }

    /**
     * 현재 페이지의 overlay(stroke / signature)를 페이지별 저장소에 저장한다.
     */
    private void saveCurrentPageOverlay() {
        ArrayList<PdfInkStroke> copiedStrokes = new ArrayList<PdfInkStroke>();
        for (int i = 0; i < strokes.size(); i++) {
            copiedStrokes.add(strokes.get(i).copy());
        }
        mPageStrokes.put(currentPageIndex, copiedStrokes);

        if (signatureOverlay != null && signatureOverlay.visible) {
            mPageSignatures.put(currentPageIndex, signatureOverlay.copyShallow());
        } else {
            mPageSignatures.remove(currentPageIndex);
        }
    }

    /**
     * 저장되어 있는 overlay를 현재 페이지 상태로 복원한다.
     */
    private void restorePageOverlay(int pageIndex) {
        strokes.clear();
        currentStroke = null;

        ArrayList<PdfInkStroke> savedStrokes = mPageStrokes.get(pageIndex);
        if (savedStrokes != null) {
            for (int i = 0; i < savedStrokes.size(); i++) {
                strokes.add(savedStrokes.get(i).copy());
            }
        }

        PdfSignatureOverlay savedSign = mPageSignatures.get(pageIndex);
        if (savedSign != null) {
            signatureOverlay = savedSign.copyShallow();
        } else {
            signatureOverlay = new PdfSignatureOverlay();
        }
    }

    /**
     * 서명 bitmap을 현재 페이지 가운데에 배치한다.
     */
    public void setSignatureBitmap(Bitmap bitmap, float desiredWidthPx, float desiredHeightPx) {
        signatureOverlay.bitmap = bitmap;
        signatureOverlay.visible = (bitmap != null);

        if (bitmap != null) {
            RectF pageRect = getPageDrawRect();

            float left = pageRect.centerX() - desiredWidthPx / 2f;
            float top = pageRect.centerY() - desiredHeightPx / 2f;
            RectF screenRect = new RectF(left, top, left + desiredWidthPx, top + desiredHeightPx);

            signatureOverlay.pdfRect = screenRectToPdfRect(screenRect);
        }

        saveCurrentPageOverlay();
        invalidate();
    }

    /**
     * 현재 페이지에서 마지막 stroke 1개 삭제
     */
    public void deleteCurrentPageLastStroke() {
        if (!strokes.isEmpty()) {
            strokes.remove(strokes.size() - 1);
            saveCurrentPageOverlay();
            invalidate();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (pageBitmap != null) {
            RectF dst = getPageDrawRect();
            canvas.drawBitmap(pageBitmap, null, dst, pdfPaint);
        }

        for (int i = 0; i < mRenderedAnnotations.size(); i++) {
            drawRenderedAnnotation(canvas, mRenderedAnnotations.get(i));
        }

        for (int i = 0; i < mRenderedFormFields.size(); i++) {
            drawRenderedFormField(canvas, mRenderedFormFields.get(i));
        }

        for (int i = 0; i < strokes.size(); i++) {
            drawStroke(canvas, strokes.get(i));
        }

        if (currentStroke != null) {
            drawStroke(canvas, currentStroke);
        }

        drawDebugText(canvas);

        if (signatureOverlay.visible && signatureOverlay.bitmap != null && !signatureOverlay.bitmap.isRecycled()) {
            RectF screenRect = pdfRectToScreenRect(signatureOverlay.pdfRect);
            canvas.drawBitmap(signatureOverlay.bitmap, null, screenRect, null);
            canvas.drawRect(screenRect, signatureFramePaint);
        }
    }

    /**
     * 사용자가 그린 stroke 1개를 화면에 그린다.
     */
    private void drawStroke(Canvas canvas, PdfInkStroke stroke) {
        if (stroke == null || !stroke.isValid()) return;

        Path path = new Path();

        PointF first = pdfToScreen(stroke.pointsPdf.get(0).x, stroke.pointsPdf.get(0).y);
        path.moveTo(first.x, first.y);

        for (int i = 1; i < stroke.pointsPdf.size(); i++) {
            PointF pdfPoint = stroke.pointsPdf.get(i);
            PointF screenPoint = pdfToScreen(pdfPoint.x, pdfPoint.y);
            path.lineTo(screenPoint.x, screenPoint.y);
        }

        strokePaint.setColor(stroke.colorArgb);
        strokePaint.setStrokeWidth(pdfWidthToScreenWidth(stroke.strokeWidthPdf));
        canvas.drawPath(path, strokePaint);
    }

    /**
     * PDF 페이지가 View 안에서 실제로 그려지는 사각형 영역을 계산한다.
     * 기본 맞춤 크기에 확대/축소 및 이동을 반영한다.
     */
    public RectF getPageDrawRect() {
        if (pageBitmap == null) {
            return new RectF(0, 0, getWidth(), getHeight());
        }

        float viewW = getWidth();
        float viewH = getHeight();
        float bmpW = pageBitmap.getWidth();
        float bmpH = pageBitmap.getHeight();

        float baseScale = Math.min(viewW / bmpW, viewH / bmpH);
        float scale = baseScale * mScaleFactor;

        float drawW = bmpW * scale;
        float drawH = bmpH * scale;

        float left = (viewW - drawW) / 2f + mTranslateX;
        float top = (viewH - drawH) / 2f + mTranslateY;

        return new RectF(left, top, left + drawW, top + drawH);
    }

    /**
     * 확대 상태에서 화면 이동값이 너무 벗어나지 않도록 제한한다.
     */
    private void clampTranslation() {
        if (pageBitmap == null) return;

        float viewW = getWidth();
        float viewH = getHeight();
        float bmpW = pageBitmap.getWidth();
        float bmpH = pageBitmap.getHeight();

        float baseScale = Math.min(viewW / bmpW, viewH / bmpH);
        float scale = baseScale * mScaleFactor;

        float drawW = bmpW * scale;
        float drawH = bmpH * scale;

        float minX;
        float maxX;
        float minY;
        float maxY;

        if (drawW <= viewW) {
            minX = maxX = 0f;
        } else {
            float overX = (drawW - viewW) / 2f;
            minX = -overX;
            maxX = overX;
        }

        if (drawH <= viewH) {
            minY = maxY = 0f;
        } else {
            float overY = (drawH - viewH) / 2f;
            minY = -overY;
            maxY = overY;
        }

        if (mTranslateX < minX) mTranslateX = minX;
        if (mTranslateX > maxX) mTranslateX = maxX;

        if (mTranslateY < minY) mTranslateY = minY;
        if (mTranslateY > maxY) mTranslateY = maxY;
    }

    /**
     * 디버깅 문자열을 화면 좌측 상단에 그린다.
     */
    private void drawDebugText(Canvas canvas) {
        if (1 == 1) return;

        formTextPaint.setColor(Color.RED);
        formTextPaint.setTextSize(28f);
        formTextPaint.setTextAlign(Paint.Align.LEFT);
        formTextPaint.setStyle(Paint.Style.FILL);

        float x = 20f;
        float y = 40f;
        float lineHeight = 35f;

        for (int i = 0; i < mDebugTextList.size(); i++) {
            String text = mDebugTextList.get(i);
            canvas.drawText(text, x, y, formTextPaint);
            y += lineHeight;

            if (y > getHeight() - 40) break;
        }
    }

    /**
     * 화면 좌표를 PDF 좌표로 변환한다.
     */
    public PointF screenToPdf(float sx, float sy) {
        RectF pageRect = getPageDrawRect();
        if (pageBitmap == null) return new PointF(0, 0);

        float clampedX = Math.max(pageRect.left, Math.min(sx, pageRect.right));
        float clampedY = Math.max(pageRect.top, Math.min(sy, pageRect.bottom));

        float normalizedX = (clampedX - pageRect.left) / pageRect.width();
        float normalizedY = (clampedY - pageRect.top) / pageRect.height();

        float pdfX = normalizedX * currentPdfPageWidth;
        float pdfYFromTop = normalizedY * currentPdfPageHeight;
        float pdfY = currentPdfPageHeight - pdfYFromTop;

        return new PointF(pdfX, pdfY);
    }

    /**
     * PDF 좌표를 화면 좌표로 변환한다.
     */
    public PointF pdfToScreen(float pdfX, float pdfY) {
        RectF pageRect = getPageDrawRect();

        float normalizedX = pdfX / currentPdfPageWidth;
        float normalizedYFromTop = (currentPdfPageHeight - pdfY) / (float) currentPdfPageHeight;

        float sx = pageRect.left + normalizedX * pageRect.width();
        float sy = pageRect.top + normalizedYFromTop * pageRect.height();

        return new PointF(sx, sy);
    }

    /**
     * 두 손가락 중심점을 구한다.
     */
    private PointF getMultiTouchCenter(MotionEvent event) {
        if (event == null || event.getPointerCount() < 2) {
            return new PointF(0f, 0f);
        }

        float x0 = event.getX(0);
        float y0 = event.getY(0);
        float x1 = event.getX(1);
        float y1 = event.getY(1);

        return new PointF((x0 + x1) / 2f, (y0 + y1) / 2f);
    }

    /**
     * 두 점 사이 거리 계산
     */
    private float distance(float x0, float x1, float y0, float y1) {
        float x = x0 - x1;
        float y = y0 - y1;
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * View 대각선 길이
     */
    private float dispDistance() {
        return (float) Math.sqrt(getWidth() * getWidth() + getHeight() * getHeight());
    }

    /**
     * 화면 사각형을 PDF 사각형으로 변환한다.
     */
    public RectF screenRectToPdfRect(RectF screenRect) {
        PointF p1 = screenToPdf(screenRect.left, screenRect.bottom);
        PointF p2 = screenToPdf(screenRect.right, screenRect.top);

        RectF pdfRect = new RectF();
        pdfRect.left = Math.min(p1.x, p2.x);
        pdfRect.right = Math.max(p1.x, p2.x);
        pdfRect.top = Math.max(p1.y, p2.y);
        pdfRect.bottom = Math.min(p1.y, p2.y);
        return pdfRect;
    }

    /**
     * PDF 사각형을 화면 사각형으로 변환한다.
     */
    public RectF pdfRectToScreenRect(RectF pdfRect) {
        PointF p1 = pdfToScreen(pdfRect.left, pdfRect.top);
        PointF p2 = pdfToScreen(pdfRect.right, pdfRect.bottom);

        RectF screenRect = new RectF();
        screenRect.left = Math.min(p1.x, p2.x);
        screenRect.right = Math.max(p1.x, p2.x);
        screenRect.top = Math.min(p1.y, p2.y);
        screenRect.bottom = Math.max(p1.y, p2.y);
        return screenRect;
    }

    /**
     * 화면의 선 두께(px)를 PDF 좌표계 두께로 환산한다.
     */
    private float screenWidthToPdfWidth(float screenWidthPx) {
        RectF pageRect = getPageDrawRect();
        if (pageRect.width() <= 0) return screenWidthPx;
        return screenWidthPx * currentPdfPageWidth / pageRect.width();
    }

    /**
     * PDF 좌표계 두께를 화면의 선 두께(px)로 환산한다.
     */
    private float pdfWidthToScreenWidth(float pdfWidth) {
        RectF pageRect = getPageDrawRect();
        if (currentPdfPageWidth <= 0) return pdfWidth;
        return pdfWidth * pageRect.width() / currentPdfPageWidth;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDetector != null) {
            mGestureDetector.onTouchEvent(event);
        }

        // 두 손가락 이상이면 확대/축소 + 2-finger pan
        if (event.getPointerCount() >= 2) {
            PointF center = getMultiTouchCenter(event);

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_POINTER_DOWN:
                case MotionEvent.ACTION_DOWN: {
                    mLastMultiTouchCenterX = center.x;
                    mLastMultiTouchCenterY = center.y;
                    mIsTwoFingerPanning = false;
                    mIsScaling = true;

                    mPrevDistance = distance(
                            event.getX(0), event.getX(1),
                            event.getY(0), event.getY(1)
                    );

                    if (currentStroke != null) {
                        currentStroke = null;
                        invalidate();
                    }
                    return true;
                }

                case MotionEvent.ACTION_MOVE: {
                    float dist = distance(
                            event.getX(0), event.getX(1),
                            event.getY(0), event.getY(1)
                    );

                    float scale = (dist - mPrevDistance) / dispDistance();
                    mPrevDistance = dist;
                    scale += 1f;
                    scale = scale * scale;

                    float oldScale = mScaleFactor;
                    float newScale = mScaleFactor * scale;

                    if (newScale < mMinScaleFactor) newScale = mMinScaleFactor;
                    if (newScale > mMaxScaleFactor) newScale = mMaxScaleFactor;

                    float focusX = center.x;
                    float focusY = center.y;

                    if (oldScale > 0f) {
                        float ratio = newScale / oldScale;
                        mTranslateX = focusX - (focusX - mTranslateX) * ratio;
                        mTranslateY = focusY - (focusY - mTranslateY) * ratio;
                    }

                    mScaleFactor = newScale;

                    float dx = center.x - mLastMultiTouchCenterX;
                    float dy = center.y - mLastMultiTouchCenterY;

                    if (Math.abs(dx) > 0.5f || Math.abs(dy) > 0.5f) {
                        mIsTwoFingerPanning = true;
                        mTranslateX += dx;
                        mTranslateY += dy;
                    }

                    mLastMultiTouchCenterX = center.x;
                    mLastMultiTouchCenterY = center.y;

                    clampTranslation();
                    invalidate();
                    return true;
                }

                case MotionEvent.ACTION_POINTER_UP:
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mIsTwoFingerPanning = false;
                    mIsScaling = false;
                    mPrevDistance = 0f;
                    return true;
            }

            return true;
        }

        float x = event.getX();
        float y = event.getY();

        // 확대 상태에서 한 손가락 pan은 MODE_NONE에서만 허용
        if (mScaleFactor > 1.0f && mode == MODE_NONE) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mLastPanX = x;
                    mLastPanY = y;
                    mIsPanning = false;
                    return true;

                case MotionEvent.ACTION_MOVE: {
                    float dx = x - mLastPanX;
                    float dy = y - mLastPanY;

                    if (!mIsPanning) {
                        if (Math.abs(dx) > mTouchSlop || Math.abs(dy) > mTouchSlop) {
                            mIsPanning = true;
                        }
                    }

                    if (mIsPanning) {
                        mTranslateX += dx;
                        mTranslateY += dy;
                        clampTranslation();
                        invalidate();
                    }

                    mLastPanX = x;
                    mLastPanY = y;
                    return true;
                }

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mIsPanning = false;
                    return true;
            }
        }

        switch (mode) {
            case MODE_PEN:
                return handlePen(event, x, y);

            case MODE_ERASER:
                return handleEraser(event, x, y);

            case MODE_MOVE_SIGNATURE:
                return handleMoveSignature(event, x, y);

            case MODE_EDIT:
                return handleEditModeTap(event, x, y);

            case MODE_NONE:
            default:
                return super.onTouchEvent(event);
        }
    }

    /**
     * 펜 모드 처리
     */
    private boolean handlePen(MotionEvent event, float x, float y) {
        PointF pdfPoint = screenToPdf(x, y);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentStroke = new PdfInkStroke(penColor, screenWidthToPdfWidth(penWidthPx));
                currentStroke.addPdfPoint(pdfPoint.x, pdfPoint.y);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentStroke != null) {
                    currentStroke.addPdfPoint(pdfPoint.x, pdfPoint.y);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentStroke != null) {
                    currentStroke.addPdfPoint(pdfPoint.x, pdfPoint.y);
                    if (currentStroke.isValid()) {
                        strokes.add(currentStroke);
                        saveCurrentPageOverlay();
                    }
                    currentStroke = null;
                    invalidate();
                }
                return true;
        }
        return false;
    }

    /**
     * 지우개 모드 처리
     */
    private boolean handleEraser(MotionEvent event, float x, float y) {
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            PointF pdfPoint = screenToPdf(x, y);
            float eraserHitPdf = screenWidthToPdfWidth(eraserHitPx);

            eraseNearestStroke(pdfPoint.x, pdfPoint.y, eraserHitPdf);

            if (signatureOverlay.visible) {
                RectF screenRect = pdfRectToScreenRect(signatureOverlay.pdfRect);
                if (isNearRect(screenRect, x, y, eraserHitPx)) {
                    signatureOverlay.visible = false;
                }
            }

            saveCurrentPageOverlay();
            invalidate();
            return true;
        }
        return false;
    }

    /**
     * 서명 이동 모드 처리
     */
    private boolean handleMoveSignature(MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (!signatureOverlay.visible) return false;

                RectF signScreenRect = pdfRectToScreenRect(signatureOverlay.pdfRect);
                if (signScreenRect.contains(x, y)) {
                    lastTouchX = x;
                    lastTouchY = y;
                    return true;
                }
                return false;
            }

            case MotionEvent.ACTION_MOVE: {
                if (!signatureOverlay.visible) return false;

                float dxScreen = x - lastTouchX;
                float dyScreen = y - lastTouchY;

                RectF pageRect = getPageDrawRect();
                float dxPdf = dxScreen * currentPdfPageWidth / pageRect.width();
                float dyPdf = -dyScreen * currentPdfPageHeight / pageRect.height();

                signatureOverlay.pdfRect.offset(dxPdf, dyPdf);

                lastTouchX = x;
                lastTouchY = y;

                saveCurrentPageOverlay();
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                saveCurrentPageOverlay();
                return true;
        }
        return false;
    }

    /**
     * 편집 모드 처리
     */
    private boolean handleEditModeTap(MotionEvent event, float x, float y) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                mDownY = y;
                mSingleTapCandidate = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (Math.abs(x - mDownX) > mTouchSlop || Math.abs(y - mDownY) > mTouchSlop) {
                    mSingleTapCandidate = false;
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (mSingleTapCandidate) {
                    if (handleTapField(x, y)) {
                        return true;
                    }
                }
                mSingleTapCandidate = false;
                return true;

            case MotionEvent.ACTION_CANCEL:
                mSingleTapCandidate = false;
                return true;
        }

        return false;
    }

    /**
     * 지우개와 가장 가까운 stroke를 찾아 삭제한다.
     */
    private void eraseNearestStroke(float pdfX, float pdfY, float eraserHitPdf) {
        int deleteIndex = -1;
        float bestDistance = Float.MAX_VALUE;

        for (int i = 0; i < strokes.size(); i++) {
            PdfInkStroke stroke = strokes.get(i);
            float d = distanceToStrokePdf(stroke, pdfX, pdfY);
            if (d < eraserHitPdf && d < bestDistance) {
                bestDistance = d;
                deleteIndex = i;
            }
        }

        if (deleteIndex >= 0) {
            strokes.remove(deleteIndex);
        }
    }

    /**
     * 한 점과 stroke 사이의 최소 거리(근사)를 계산한다.
     */
    private float distanceToStrokePdf(PdfInkStroke stroke, float pdfX, float pdfY) {
        float best = Float.MAX_VALUE;

        for (int i = 0; i < stroke.pointsPdf.size(); i++) {
            PointF p = stroke.pointsPdf.get(i);
            float dx = p.x - pdfX;
            float dy = p.y - pdfY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            if (dist < best) best = dist;
        }
        return best;
    }

    /**
     * 점이 사각형 근처에 있는지 검사
     */
    private boolean isNearRect(RectF rect, float x, float y, float padding) {
        RectF r = new RectF(
                rect.left - padding,
                rect.top - padding,
                rect.right + padding,
                rect.bottom + padding
        );
        return r.contains(x, y);
    }

    /**
     * PDF 관련 리소스를 모두 닫고 정리한다.
     */
    public void closePdf() {
        saveCurrentPageOverlay();

        try {
            if (currentPage != null) currentPage.close();
        } catch (Exception ignored) { }
        try {
            if (pdfRenderer != null) pdfRenderer.close();
        } catch (Exception ignored) { }
        try {
            if (pfd != null) pfd.close();
        } catch (Exception ignored) { }

        currentPage = null;
        pdfRenderer = null;
        pfd = null;
        currentPdfPageWidth = 0;
        currentPdfPageHeight = 0;

        if (pageBitmap != null && !pageBitmap.isRecycled()) {
            pageBitmap.recycle();
        }
        pageBitmap = null;

        mRenderedAnnotations.clear();
        mRenderedFormFields.clear();
        mDebugTextList.clear();

        mCurrentPdfFile = null;

        resetZoom();
    }

    /**
     * PDF 내부 Ink Annotation 리스트 설정
     */
    public void setRenderedAnnotations(List<PdfRenderedInkAnnotation> annotations) {
        mRenderedAnnotations.clear();
        if (annotations != null) {
            mRenderedAnnotations.addAll(annotations);
        }
        invalidate();
    }

    /**
     * PDF 내부 AcroForm field 리스트 설정
     */
    public void setRenderedFormFields(List<PdfRenderedFormField> fields) {
        mRenderedFormFields.clear();
        if (fields != null) {
            mRenderedFormFields.addAll(fields);
        }

        applyEditedValuesToRenderedFields();
        invalidate();
    }

    /**
     * PDF 내부 Ink Annotation 1개를 다시 그린다.
     */
    private void drawRenderedAnnotation(Canvas canvas, PdfRenderedInkAnnotation ann) {
        if (ann == null || !ann.isValid()) return;

        Path path = new Path();

        PointF first = pdfToScreen(ann.pointsPdf.get(0).x, ann.pointsPdf.get(0).y);
        path.moveTo(first.x, first.y);

        for (int i = 1; i < ann.pointsPdf.size(); i++) {
            PointF pdfPoint = ann.pointsPdf.get(i);
            PointF screenPoint = pdfToScreen(pdfPoint.x, pdfPoint.y);
            path.lineTo(screenPoint.x, screenPoint.y);
        }

        strokePaint.setColor(ann.colorArgb);
        strokePaint.setStrokeWidth(pdfWidthToScreenWidth(ann.strokeWidthPdf));
        canvas.drawPath(path, strokePaint);
    }

    /**
     * PDF 내부 AcroForm field 1개를 화면에 다시 그린다.
     */
    private void drawRenderedFormField(Canvas canvas, PdfRenderedFormField field) {
        if (field == null || !field.isValid()) return;

        RectF screenRect = pdfRectToScreenRect(field.pdfRect);

        float textSizePx = pdfWidthToScreenWidth(field.fontSizePdf);
        if (textSizePx < 12f) textSizePx = 12f;

        formTextPaint.setColor(field.colorArgb);
        formTextPaint.setTextSize(textSizePx);
        formTextPaint.setStyle(Paint.Style.FILL);
        formTextPaint.setTextAlign(Paint.Align.LEFT);

        String type = "text";
        try {
            if (field.type != null && !"".equals(field.type)) {
                type = field.type;
            }
        } catch (Throwable ignore) {
        }

        float x = screenRect.left + 2f;
        Paint.FontMetrics fm = formTextPaint.getFontMetrics();
        float textCenterY = screenRect.centerY();
        float y = textCenterY - ((fm.ascent + fm.descent) / 2f);

        Paint debugPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        debugPaint.setStyle(Paint.Style.STROKE);
        debugPaint.setStrokeWidth(2f);
        debugPaint.setColor(Color.GREEN);

        Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shapePaint.setColor(field.colorArgb);
        shapePaint.setStyle(Paint.Style.STROKE);
        shapePaint.setStrokeWidth(2f);

        if ("text".equalsIgnoreCase(type)
                || "combo".equalsIgnoreCase(type)
                || "listbox".equalsIgnoreCase(type)
                || "choice".equalsIgnoreCase(type)) {

            canvas.drawText(safe(field.value), x, y, formTextPaint);

        } else if ("checkbox".equalsIgnoreCase(type)) {

            canvas.drawRect(screenRect, shapePaint);

            String value = safe(field.value);
            if ("true".equalsIgnoreCase(value)
                    || "yes".equalsIgnoreCase(value)
                    || "on".equalsIgnoreCase(value)
                    || "1".equalsIgnoreCase(value)
                    || "y".equalsIgnoreCase(value)) {

                Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                checkPaint.setColor(field.colorArgb);
                checkPaint.setStyle(Paint.Style.STROKE);
                checkPaint.setStrokeWidth(3f);
                checkPaint.setStrokeCap(Paint.Cap.ROUND);
                checkPaint.setStrokeJoin(Paint.Join.ROUND);

                float w = screenRect.width();
                float h = screenRect.height();

                float startX = screenRect.left + w * 0.18f;
                float startY = screenRect.top + h * 0.55f;

                float midX = screenRect.left + w * 0.42f;
                float midY = screenRect.top + h * 0.78f;

                float endX = screenRect.left + w * 0.82f;
                float endY = screenRect.top + h * 0.22f;

                canvas.drawLine(startX, startY, midX, midY, checkPaint);
                canvas.drawLine(midX, midY, endX, endY, checkPaint);
            }

        } else if ("radio".equalsIgnoreCase(type)) {

            float cx = screenRect.centerX();
            float cy = screenRect.centerY();
            float radius = Math.min(screenRect.width(), screenRect.height()) / 2f;

            canvas.drawCircle(cx, cy, radius, shapePaint);

            if (!"".equals(safe(field.value))) {
                Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                fillPaint.setStyle(Paint.Style.FILL);
                fillPaint.setColor(field.colorArgb);
                canvas.drawCircle(cx, cy, radius / 2f, fillPaint);
            }

        } else if ("button".equalsIgnoreCase(type)) {

            canvas.drawRect(screenRect, shapePaint);

            String text = safe(field.value);
            if ("".equals(text)) {
                text = safe(field.name);
            }
            canvas.drawText(text, x, y, formTextPaint);

        } else if ("signature".equalsIgnoreCase(type)) {

            canvas.drawRect(screenRect, shapePaint);

            String text = safe(field.value);
            if ("".equals(text)) {
                text = "[SIGN]";
            }
            canvas.drawText(text, x, y, formTextPaint);

        } else {
            canvas.drawText(safe(field.value), x, y, formTextPaint);
        }

        canvas.drawRect(screenRect, debugPaint);
    }

    /**
     * null 안전 문자열 변환
     */
    private String safe(String s) {
        return s == null ? "" : s;
    }

    public interface OnPdfFieldEditListener {
        void onPdfTextFieldClick(PdfRenderedFormField field);
    }

    public void setOnPdfFieldEditListener(OnPdfFieldEditListener listener) {
        this.mOnPdfFieldEditListener = listener;
    }

    /**
     * 사용자가 수정한 필드값 전체를 반환한다.
     */
    public HashMap<String, String> getEditedFieldValues() {
        return new HashMap<String, String>(mEditedFieldValues);
    }

    /**
     * 특정 필드값을 외부에서 강제로 반영한다.
     */
    public void updateFieldValue(String fieldName, String newValue) {
        if (fieldName == null) return;

        if (newValue == null) newValue = "";
        mEditedFieldValues.put(fieldName, newValue);

        for (int i = 0; i < mRenderedFormFields.size(); i++) {
            PdfRenderedFormField field = mRenderedFormFields.get(i);
            if (field == null) continue;
            if (field.name != null && field.name.equals(fieldName)) {
                field.value = newValue;
            }
        }

        invalidate();
    }

    /**
     * 수정값 전체 삭제
     */
    public void clearEditedFieldValues() {
        mEditedFieldValues.clear();
        invalidate();
    }

    /**
     * 새로 읽어온 field 리스트에 수정값을 덮어쓴다.
     */
    private void applyEditedValuesToRenderedFields() {
        for (int i = 0; i < mRenderedFormFields.size(); i++) {
            PdfRenderedFormField field = mRenderedFormFields.get(i);
            if (field == null || field.name == null) continue;

            if (mEditedFieldValues.containsKey(field.name)) {
                field.value = mEditedFieldValues.get(field.name);
            }
        }
    }

    /**
     * 사용자가 탭한 위치에 form field가 있으면 처리한다.
     * - checkbox: 즉시 토글
     * - text: 외부 리스너로 전달
     */
    private boolean handleTapField(float x, float y) {
        for (int i = mRenderedFormFields.size() - 1; i >= 0; i--) {
            PdfRenderedFormField field = mRenderedFormFields.get(i);
            if (field == null || !field.isValid()) continue;

            RectF screenRect = pdfRectToScreenRect(field.pdfRect);
            if (!screenRect.contains(x, y)) continue;

            // readonly 필드는 편집/토글 모두 막음
            if (field.readOnly) {
                return true;   // 터치는 소비하지만 편집창은 띄우지 않음
            }

            String type = safe(field.type);

            if ("checkbox".equalsIgnoreCase(type)) {
                boolean checked = isCheckedValue(field.value);
                String newValue = checked ? "Off" : "Yes";

                field.value = newValue;
                if (field.name != null) {
                    mEditedFieldValues.put(field.name, newValue);
                }

                invalidate();
                return true;
            }

            if ("text".equalsIgnoreCase(type)
                    || "combo".equalsIgnoreCase(type)
                    || "listbox".equalsIgnoreCase(type)
                    || "choice".equalsIgnoreCase(type)
                    || "button".equalsIgnoreCase(type)
                    || "signature".equalsIgnoreCase(type)) {

                if (mOnPdfFieldEditListener != null) {
                    mOnPdfFieldEditListener.onPdfTextFieldClick(field);
                    return true;
                }
            }

            if ("".equals(type)) {
                if (mOnPdfFieldEditListener != null) {
                    mOnPdfFieldEditListener.onPdfTextFieldClick(field);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isCheckedValue(String value) {
        String v = safe(value);
        return "true".equalsIgnoreCase(v)
                || "yes".equalsIgnoreCase(v)
                || "on".equalsIgnoreCase(v)
                || "1".equalsIgnoreCase(v)
                || "y".equalsIgnoreCase(v);
    }
}