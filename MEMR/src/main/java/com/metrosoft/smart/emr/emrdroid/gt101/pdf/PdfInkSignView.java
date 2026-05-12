package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
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
import java.util.Map;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PdfInkSignView extends AppCompatImageView {
    /**
     * PdfInkPdfSaver에서 PDF 저장 시 사용하는 폰트와 동일하게 맞춘다.
     * 화면 overlay와 저장 PDF의 글자 모양/높이/baseline 차이를 줄이기 위한 목적이다.
     */
    private static final String FONT_ASSET_PATH = "fonts/NotoSansKR-Regular.ttf";
    private static final float DEFAULT_FONT_SIZE = 10f;

    public static final int MODE_NONE = 0;
    public static final int MODE_PEN = 1;
    public static final int MODE_ERASER = 2;
    public static final int MODE_MOVE_SIGN = 3;
    public static final int MODE_EDIT = 4;

    private final Paint pdfPaint = new Paint(Paint.DITHER_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint signFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint formTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String mTextFont = ""; // 디버깅용

    private String mUserid;
    private Bitmap pageBitmap;
    private ParcelFileDescriptor pfd;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private int currentPageIndex = 0;

    private int currentPdfPageWidth;
    private int currentPdfPageHeight;

    private final List<PdfInkStroke> strokes = new ArrayList<PdfInkStroke>();
    private PdfInkStroke currentStroke;

    // 현재 페이지 sign overlay. key = fieldName 또는 rect 기반 key
    private final HashMap<String, PdfSignOverlay> mSignOverlays =
            new HashMap<String, PdfSignOverlay>();

    // 전체 페이지 sign overlay
    private final HashMap<Integer, HashMap<String, PdfSignOverlay>> mPageSigns =
            new HashMap<Integer, HashMap<String, PdfSignOverlay>>();

    /**
     * 현재 페이지 sign_image overlay.
     *
     * sign_image는 사용자가 직접 그린 sign이 아니므로
     * mSignOverlays에 넣으면 안 된다.
     *
     * key = fieldName 또는 rect 기반 key
     */
    private final HashMap<String, PdfSignImageOverlay> mSignImageOverlays =
            new HashMap<String, PdfSignImageOverlay>();

    /**
     * 전체 페이지 sign_image overlay.
     *
     * 페이지 이동 시 sign_image bitmap cache를 유지하려는 용도이다.
     * 단, 저장은 PdfRenderedFormField.value 기준으로 처리하므로
     * 이 map은 화면 표시/cache 용도로만 생각하는 것이 안전하다.
     */
    private final HashMap<Integer, HashMap<String, PdfSignImageOverlay>> mPageSignImages =
            new HashMap<Integer, HashMap<String, PdfSignImageOverlay>>();

    private final HashMap<Integer, ArrayList<PdfInkStroke>> mPageStrokes =
            new HashMap<Integer, ArrayList<PdfInkStroke>>();

    private final List<PdfRenderedInkAnnotation> mRenderedAnnotations =
            new ArrayList<PdfRenderedInkAnnotation>();

    private final List<PdfRenderedFormField> mRenderedFormFields =
            new ArrayList<PdfRenderedFormField>();

    private final List<String> mDebugTextList = new ArrayList<String>();

    private File mCurrentPdfFile;

    private int mode = MODE_NONE;
    private int penColor = Color.RED;
    private float penWidthPx = 4f;
    private float eraserHitPx = 40f;

    private GestureDetector mGestureDetector;

    private float mScaleFactor = 1.0f;
    private float mMinScaleFactor = 1.0f;
    private float mMaxScaleFactor = 2.0f;

    private float mTranslateX = 0f;
    private float mTranslateY = 0f;

    private float mLastPanX = 0f;
    private float mLastPanY = 0f;
    private float mLastMultiTouchCenterX = 0f;
    private float mLastMultiTouchCenterY = 0f;
    private float mPrevDistance = 0f;

    private boolean mIsScaling = false;
    private boolean mIsPanning = false;
    private boolean mIsTwoFingerPanning = false;

    private int mTouchSlop = 0;

    private float mDownX = 0f;
    private float mDownY = 0f;
    private boolean mSingleTapCandidate = false;

    private Map<String, String> mValues;

    // 사용자가 수정한 값. 저장은 실제 PDF fieldName 기준으로 한다.
    private final HashMap<String, String> mEditedFieldValues =
            new HashMap<String, String>();

    public interface OnPdfFieldEditListener {
        void onPdfTextFieldClick(PdfRenderedFormField field);
    }

    private OnPdfFieldEditListener mOnPdfFieldEditListener;

    public void setOnPdfFieldEditListener(OnPdfFieldEditListener listener) {
        this.mOnPdfFieldEditListener = listener;
    }

    public interface OnPdfSignFieldClickListener {
        void onPdfSignFieldClick(PdfRenderedFormField field, ArrayList<Path> paths);
    }

    private OnPdfSignFieldClickListener mOnPdfSignFieldClickListener;

    public void setOnPdfSignFieldClickListener(OnPdfSignFieldClickListener listener) {
        this.mOnPdfSignFieldClickListener = listener;
    }

    public interface OnPdfFieldValueChangedListener {
        void onPdfFieldValueChanged(int index, PdfRenderedFormField field);
    }

    private OnPdfFieldValueChangedListener mOnPdfFieldValueChangedListener;

    public void setOnPdfFieldValueChangedListener(OnPdfFieldValueChangedListener listener) {
        this.mOnPdfFieldValueChangedListener = listener;
    }

    public PdfInkSignView(Context context) {
        super(context);
        init();
    }

    private void init() {
        setFocusable(true);
        setClickable(true);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        signFramePaint.setStyle(Paint.Style.STROKE);
        signFramePaint.setStrokeWidth(2f);
        signFramePaint.setColor(Color.argb(180, 50, 50, 50));

        formTextPaint.setColor(Color.BLACK);
        formTextPaint.setStyle(Paint.Style.FILL);
        formTextPaint.setTextAlign(Paint.Align.LEFT);

        /*
         * PDF 저장 시 PdfInkPdfSaver가 사용하는 폰트와 동일한 폰트를 사용한다.
         * 그래야 저장 전 화면 overlay와 저장 후 PDF의 글자 폭/높이/baseline 차이가 줄어든다.
         */
        try {
            Typeface tf = Typeface.createFromAsset(getContext().getAssets(), FONT_ASSET_PATH);
            formTextPaint.setTypeface(tf);
            mTextFont = "custom font";
        } catch (Exception ex) {
            /*
             * 폰트 파일이 없거나 로딩 실패 시 기존 기본 폰트 사용.
             * 단, 이 경우 저장 전/후 모양 차이가 다시 발생할 수 있다.
             */
            formTextPaint.setTypeface(Typeface.DEFAULT);
            mTextFont = "default font";
        }

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

    public int getPdfPageCount() {
        return pdfRenderer != null ? pdfRenderer.getPageCount() : 0;
    }

    public int getCurrentPdfPageWidth() {
        return currentPdfPageWidth;
    }

    public int getCurrentPdfPageHeight() {
        return currentPdfPageHeight;
    }

    public Bitmap getPageBitmap() {
        return pageBitmap;
    }

    public List<PdfInkStroke> getCurrentPageStrokes() {
        return strokes;
    }

    public HashMap<Integer, ArrayList<PdfInkStroke>> getAllPageStrokes() {
        saveCurrentPageOverlay();
        return mPageStrokes;
    }

    public HashMap<Integer, HashMap<String, PdfSignOverlay>> getAllPageSignOverlays() {
        saveCurrentPageOverlay();
        return mPageSigns;
    }

    public HashMap<String, PdfSignOverlay> getCurrentPageSignOverlays() {
        saveCurrentPageOverlay();

        HashMap<String, PdfSignOverlay> result =
                new HashMap<String, PdfSignOverlay>();

        for (String key : mSignOverlays.keySet()) {
            PdfSignOverlay overlay = mSignOverlays.get(key);
            if (overlay == null || !overlay.visible) continue;
            result.put(key, overlay.copyShallow());
        }

        return result;
    }

    public float getScaleFactor() {
        return mScaleFactor;
    }

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

    public void clearAllOverlays() {
        strokes.clear();
        currentStroke = null;
        mSignOverlays.clear();
        mSignImageOverlays.clear();

        mPageStrokes.clear();
        mPageSigns.clear();
        mPageSignImages.clear();

        mRenderedAnnotations.clear();
        mRenderedFormFields.clear();
        mDebugTextList.clear();
        mEditedFieldValues.clear();

        invalidate();
    }

    public void clearCurrentPageOverlay() {
        strokes.clear();
        currentStroke = null;
        mSignOverlays.clear();
        mSignImageOverlays.clear();

        saveCurrentPageOverlay();
        invalidate();
    }

    public void openPdf(File pdfFile, int pageIndex, Map<String, String> values, String userid) throws IOException {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            throw new IOException("PdfRenderer는 Android 5.0 이상에서만 지원됩니다.");
        }

        closePdf();
        clearAllOverlays();

        mUserid = userid;
        mValues = values;

        pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
        pdfRenderer = new PdfRenderer(pfd);

        if (pdfRenderer.getPageCount() <= 0) {
            throw new IOException("PDF 페이지가 없습니다.");
        }

        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= pdfRenderer.getPageCount()) {
            pageIndex = pdfRenderer.getPageCount() - 1;
        }

        currentPageIndex = pageIndex;

        resetZoom();
        openRendererPage(currentPageIndex);
        restorePageOverlay(currentPageIndex);

        mCurrentPdfFile = pdfFile;

        readCurrentPageAnnotationsAndFields();

        requestLayout();
        invalidate();
    }

    public void showPage(int pageIndex) throws IOException {
        if (pdfRenderer == null) return;

        saveCurrentPageOverlay();

        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= pdfRenderer.getPageCount()) {
            pageIndex = pdfRenderer.getPageCount() - 1;
        }

        currentPageIndex = pageIndex;

        resetZoom();
        openRendererPage(currentPageIndex);
        restorePageOverlay(currentPageIndex);

        readCurrentPageAnnotationsAndFields();

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

    private void readCurrentPageAnnotationsAndFields() {
        mDebugTextList.clear();

        try {
            if (mCurrentPdfFile != null) {
                List<PdfRenderedInkAnnotation> annots =
                        PdfInkAnnotationReader.readInkAnnotations(
                                getContext(),
                                mCurrentPdfFile,
                                currentPageIndex
                        );
                setRenderedAnnotations(annots);
            }
        } catch (Exception e) {
            mRenderedAnnotations.clear();
            mDebugTextList.add("annotation 읽기 오류: " + e.getMessage());
        }

        try {
            if (mCurrentPdfFile != null) {
                List<PdfRenderedFormField> fields =
                        PdfFormFieldReader.readAllFields(
                                getContext(),
                                mCurrentPdfFile,
                                currentPageIndex,
                                mValues,
                                mDebugTextList
                        );
                setRenderedFormFields(fields);
            }
        } catch (Exception e) {
            mRenderedFormFields.clear();
            mDebugTextList.add("form-field 읽기 오류: " + e.getMessage());
        }

        mDebugTextList.add(mTextFont);
    }

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

    private void saveCurrentPageOverlay() {
        ArrayList<PdfInkStroke> copiedStrokes = new ArrayList<PdfInkStroke>();

        for (int i = 0; i < strokes.size(); i++) {
            copiedStrokes.add(strokes.get(i).copy());
        }

        mPageStrokes.put(currentPageIndex, copiedStrokes);

        /*
         * sign overlay 저장.
         * PdfSignOverlay는 사용자가 직접 입력한 sign 전용이다.
         */
        HashMap<String, PdfSignOverlay> copiedSigns =
                new HashMap<String, PdfSignOverlay>();

        for (String key : mSignOverlays.keySet()) {
            PdfSignOverlay overlay = mSignOverlays.get(key);
            if (overlay == null || !overlay.visible) continue;

            copiedSigns.put(key, overlay.copyShallow());
        }

        if (copiedSigns.size() > 0) {
            mPageSigns.put(currentPageIndex, copiedSigns);
        } else {
            mPageSigns.remove(currentPageIndex);
        }

        /*
         * sign_image overlay 저장.
         * PdfSignImageOverlay는 sign_image 전용이다.
         */
        HashMap<String, PdfSignImageOverlay> copiedSignImages =
                new HashMap<String, PdfSignImageOverlay>();

        for (String key : mSignImageOverlays.keySet()) {
            PdfSignImageOverlay overlay = mSignImageOverlays.get(key);
            if (overlay == null || !overlay.visible) continue;

            copiedSignImages.put(key, overlay.copyShallow());
        }

        if (copiedSignImages.size() > 0) {
            mPageSignImages.put(currentPageIndex, copiedSignImages);
        } else {
            mPageSignImages.remove(currentPageIndex);
        }
    }


    private void restorePageOverlay(int pageIndex) {
        strokes.clear();
        currentStroke = null;

        ArrayList<PdfInkStroke> savedStrokes = mPageStrokes.get(pageIndex);
        if (savedStrokes != null) {
            for (int i = 0; i < savedStrokes.size(); i++) {
                strokes.add(savedStrokes.get(i).copy());
            }
        }

        /*
         * sign 복원.
         */
        mSignOverlays.clear();

        HashMap<String, PdfSignOverlay> savedSigns = mPageSigns.get(pageIndex);
        if (savedSigns != null) {
            for (String key : savedSigns.keySet()) {
                PdfSignOverlay overlay = savedSigns.get(key);
                if (overlay == null) continue;

                mSignOverlays.put(key, overlay.copyShallow());
            }
        }

        /*
         * sign_image 복원.
         */
        mSignImageOverlays.clear();

        HashMap<String, PdfSignImageOverlay> savedSignImages = mPageSignImages.get(pageIndex);
        if (savedSignImages != null) {
            for (String key : savedSignImages.keySet()) {
                PdfSignImageOverlay overlay = savedSignImages.get(key);
                if (overlay == null) continue;

                mSignImageOverlays.put(key, overlay.copyShallow());
            }
        }
    }


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
            drawRenderedFormField(canvas, mRenderedFormFields.get(i), i);
        }

        for (int i = 0; i < strokes.size(); i++) {
            drawStroke(canvas, strokes.get(i));
        }

        if (currentStroke != null) {
            drawStroke(canvas, currentStroke);
        }

        // sign overlay는 field 위에 표시한다.
        for (String key : mSignOverlays.keySet()) {
            drawSignOverlay(canvas, mSignOverlays.get(key));
        }

        drawDebugText(canvas);
    }

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
     * sign overlay를 화면에 표시한다.
     */
    private void drawSignOverlay(Canvas canvas, PdfSignOverlay overlay) {
        if (canvas == null || overlay == null) return;
        if (!overlay.visible) return;
        if (overlay.pdfRect == null) return;

        RectF screenRect = pdfRectToScreenRect(overlay.pdfRect);

        if (overlay.paths == null || overlay.paths.size() <= 0) {
            return;
        }

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(overlay.strokeColor);
        p.setStrokeWidth(pdfWidthToScreenWidth(overlay.strokeWidth));
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);

        for (int i = 0; i < overlay.paths.size(); i++) {
            Path src = overlay.paths.get(i);
            if (src == null) continue;

            Path dst = new Path(src);
            transformPathPdfToScreen(dst);
            canvas.drawPath(dst, p);
        }
    }

    /**
     * PDF 좌표계 Path를 화면 좌표계 Path로 변환한다.
     */
    private void transformPathPdfToScreen(Path path) {
        if (path == null) return;

        RectF pageRect = getPageDrawRect();

        if (currentPdfPageWidth <= 0 || currentPdfPageHeight <= 0) return;

        float scaleX = pageRect.width() / currentPdfPageWidth;
        float scaleY = pageRect.height() / currentPdfPageHeight;

        Matrix matrix = new Matrix();

        // PDF 좌표계는 좌하단 원점이고, Android 화면은 좌상단 원점이다.
        matrix.postScale(scaleX, -scaleY);
        matrix.postTranslate(pageRect.left, pageRect.top + pageRect.height());

        path.transform(matrix);
    }

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

    private void drawDebugText(Canvas canvas) {
        if (!"mmsdev".equalsIgnoreCase(mUserid)) return;

        formTextPaint.setColor(Color.RED);
        formTextPaint.setTextSize(28f);
        formTextPaint.setTextAlign(Paint.Align.LEFT);
        formTextPaint.setStyle(Paint.Style.FILL);

        float x = 20f;
        float y = 40f;
        float lineHeight = 35f;

        for (int i = 0; i < mDebugTextList.size(); i++) {
            canvas.drawText(mDebugTextList.get(i), x, y, formTextPaint);
            y += lineHeight;
            if (y > getHeight() - 40) break;
        }
    }

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
     * PDF 좌표를 현재 화면(View) 좌표로 변환한다.
     *
     * PDF 좌표계:
     * - 원점이 페이지의 좌하단이다.
     * - X는 오른쪽으로 증가한다.
     * - Y는 위쪽으로 증가한다.
     *
     * Android 화면 좌표계:
     * - 원점이 View의 좌상단이다.
     * - X는 오른쪽으로 증가한다.
     * - Y는 아래쪽으로 증가한다.
     *
     * 따라서 Y 좌표는 변환할 때 위/아래 방향을 뒤집어야 한다.
     */
    public PointF pdfToScreen(float pdfX, float pdfY) {
        /*
         * 현재 PDF 페이지가 View 안에서 실제로 그려지는 사각형 영역이다.
         *
         * 확대/축소, 이동, 가운데 정렬이 모두 반영된 화면 좌표 영역이다.
         */
        RectF pageRect = getPageDrawRect();

        /*
         * PDF 페이지 안에서 X 위치를 0.0 ~ 1.0 비율로 변환한다.
         *
         * 예:
         * - pdfX == 0                  → 페이지의 가장 왼쪽
         * - pdfX == currentPdfPageWidth → 페이지의 가장 오른쪽
         */
        float normalizedX = pdfX / currentPdfPageWidth;

        /*
         * PDF의 Y 좌표는 아래에서 위로 증가한다.
         * Android 화면의 Y 좌표는 위에서 아래로 증가한다.
         *
         * 그래서 PDF의 pdfY를 화면 기준 "위에서부터의 거리"로 바꾼다.
         *
         * 예:
         * - pdfY == currentPdfPageHeight → PDF 페이지 맨 위
         *   → normalizedYFromTop == 0
         *
         * - pdfY == 0 → PDF 페이지 맨 아래
         *   → normalizedYFromTop == 1
         */
        float normalizedYFromTop = (currentPdfPageHeight - pdfY) / (float) currentPdfPageHeight;

        /*
         * X 비율을 실제 화면 좌표로 변환한다.
         *
         * pageRect.left는 화면에서 PDF 페이지가 시작되는 X 좌표이고,
         * pageRect.width()는 현재 확대/축소가 반영된 PDF 페이지의 화면 폭이다.
         */
        float sx = pageRect.left + normalizedX * pageRect.width();

        /*
         * Y 비율을 실제 화면 좌표로 변환한다.
         *
         * pageRect.top은 화면에서 PDF 페이지가 시작되는 Y 좌표이고,
         * pageRect.height()는 현재 확대/축소가 반영된 PDF 페이지의 화면 높이다.
         */
        float sy = pageRect.top + normalizedYFromTop * pageRect.height();

        /*
         * 변환된 Android 화면 좌표를 반환한다.
         */
        return new PointF(sx, sy);
    }

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

    private float distance(float x0, float x1, float y0, float y1) {
        float x = x0 - x1;
        float y = y0 - y1;
        return (float) Math.sqrt(x * x + y * y);
    }

    private float dispDistance() {
        return (float) Math.sqrt(getWidth() * getWidth() + getHeight() * getHeight());
    }

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

    private float screenWidthToPdfWidth(float screenWidthPx) {
        RectF pageRect = getPageDrawRect();
        if (pageRect.width() <= 0) return screenWidthPx;
        return screenWidthPx * currentPdfPageWidth / pageRect.width();
    }

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

        if (event.getPointerCount() >= 2) {
            return handleMultiTouch(event);
        }

        float x = event.getX();
        float y = event.getY();

        if (mScaleFactor > 1.0f && mode == MODE_NONE) {
            return handleOneFingerPan(event, x, y);
        }

        switch (mode) {
            case MODE_PEN:
                return handlePen(event, x, y);

            case MODE_ERASER:
                return handleEraser(event, x, y);

            case MODE_MOVE_SIGN:
                return handleMoveSign(event, x, y);

            case MODE_EDIT:
                return handleEditModeTap(event, x, y);

            case MODE_NONE:
            default:
                return super.onTouchEvent(event);
        }
    }

    private boolean handleMultiTouch(MotionEvent event) {
        PointF center = getMultiTouchCenter(event);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_DOWN:
                mLastMultiTouchCenterX = center.x;
                mLastMultiTouchCenterY = center.y;
                mIsTwoFingerPanning = false;
                mIsScaling = true;

                mPrevDistance = distance(
                        event.getX(0), event.getX(1),
                        event.getY(0), event.getY(1)
                );

                currentStroke = null;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
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

    private boolean handleOneFingerPan(MotionEvent event, float x, float y) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mLastPanX = x;
                mLastPanY = y;
                mIsPanning = false;
                return true;

            case MotionEvent.ACTION_MOVE:
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

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mIsPanning = false;
                return true;
        }

        return false;
    }

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

    private boolean handleEraser(MotionEvent event, float x, float y) {
        if (event.getAction() == MotionEvent.ACTION_DOWN
                || event.getAction() == MotionEvent.ACTION_MOVE) {

            PointF pdfPoint = screenToPdf(x, y);
            float eraserHitPdf = screenWidthToPdfWidth(eraserHitPx);

            eraseNearestStroke(pdfPoint.x, pdfPoint.y, eraserHitPdf);

            // sign은 지우개로 삭제하지 않는다.
            saveCurrentPageOverlay();
            invalidate();
            return true;
        }

        return false;
    }

    private boolean handleMoveSign(MotionEvent event, float x, float y) {
        return false;
    }

    private boolean handleEditModeTap(MotionEvent event, float x, float y) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = x;
                mDownY = y;
                mSingleTapCandidate = true;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (Math.abs(x - mDownX) > mTouchSlop
                        || Math.abs(y - mDownY) > mTouchSlop) {
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

    public void closePdf() {
        //saveCurrentPageOverlay();

        try {
            if (currentPage != null) currentPage.close();
        } catch (Exception ignored) {
        }

        try {
            if (pdfRenderer != null) pdfRenderer.close();
        } catch (Exception ignored) {
        }

        try {
            if (pfd != null) pfd.close();
        } catch (Exception ignored) {
        }

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

    public void setRenderedAnnotations(List<PdfRenderedInkAnnotation> annotations) {
        mRenderedAnnotations.clear();
        if (annotations != null) {
            mRenderedAnnotations.addAll(annotations);
        }
        invalidate();
    }

    public void setRenderedFormFields(List<PdfRenderedFormField> fields) {
        mRenderedFormFields.clear();
        if (fields != null) {
            mRenderedFormFields.addAll(fields);
        }

        applyEditedValuesToRenderedFields();
        invalidate();
    }

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

    private void drawRenderedFormField(Canvas canvas, PdfRenderedFormField field, int index) {
        if (field == null || !field.isValid()) return;

        if (mOnPdfFieldValueChangedListener != null) {
            mOnPdfFieldValueChangedListener.onPdfFieldValueChanged(index, field);
        }

        RectF screenRect = pdfRectToScreenRect(field.pdfRect);

        float textSizePx = pdfWidthToScreenWidth(field.fontSizePdf);

        /*
         * 저장 시 PdfInkPdfSaver는 field.fontSizePdf를 그대로 사용한다.
         * 화면에서만 12px로 강제하면 저장 전/후 글자 크기가 달라진다.
         */
        if (textSizePx <= 0f) {
            textSizePx = DEFAULT_FONT_SIZE;
        }

        formTextPaint.setColor(field.colorArgb);
        formTextPaint.setTextSize(textSizePx);
        formTextPaint.setStyle(Paint.Style.FILL);
        formTextPaint.setTextAlign(Paint.Align.LEFT);

        String type = safe(field.type);
        if ("".equals(type)) type = "label";

        float x = screenRect.left;// + 2f;
        Paint.FontMetrics fm = formTextPaint.getFontMetrics();
        float y = screenRect.centerY() - ((fm.ascent + fm.descent) / 2f);

        Paint editablePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        editablePaint.setStyle(Paint.Style.STROKE);
        editablePaint.setStrokeWidth(2f);
        editablePaint.setColor(Color.GREEN);

        Paint editingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        editingPaint.setStyle(Paint.Style.STROKE);
        editingPaint.setStrokeWidth(2f);
        editingPaint.setColor(Color.BLUE);

        Paint shapePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shapePaint.setColor(field.colorArgb);
        shapePaint.setStyle(Paint.Style.STROKE);
        shapePaint.setStrokeWidth(2f);

        if ("text".equalsIgnoreCase(type)
                || "combo".equalsIgnoreCase(type)
                || "listbox".equalsIgnoreCase(type)
                || "choice".equalsIgnoreCase(type)
                || "label".equalsIgnoreCase(type)) {

            /*
             * text/label 계열 field 출력.
             *
             * autoFit=true이면:
             * - field 박스 오른쪽을 벗어나지 않도록 자동 줄바꿈
             * - 값 안에 엔터(\n, \r\n)가 있으면 강제 줄바꿈
             * - 줄바꿈 후 아래쪽이 field 박스를 벗어나면 출력 중단
             *
             * autoFit=false이면 기존처럼 한 줄 출력한다.
             */
            if (field.autoFit) {
                drawAutoFitTextInBox(
                        canvas,
                        safe(field.value),
                        screenRect,
                        formTextPaint
                );
            } else {
                canvas.drawText(safe(field.value), x, y, formTextPaint);
            }


        } else if ("checkbox".equalsIgnoreCase(type)) {

            if (isCheckedValue(field.value)) {
                Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                checkPaint.setColor(field.colorArgb);
                checkPaint.setStyle(Paint.Style.STROKE);
                checkPaint.setStrokeWidth(3f);
                checkPaint.setStrokeCap(Paint.Cap.ROUND);
                checkPaint.setStrokeJoin(Paint.Join.ROUND);

                float w = screenRect.width();
                float h = screenRect.height();

                // checkbox를 왼쪽(상단)에 붙이기 위한 용도
                if (w < h) h = w;
                if (h < w) w = h;

                canvas.drawLine(
                        screenRect.left + w * 0.18f,
                        screenRect.top + h * 0.55f,
                        screenRect.left + w * 0.42f,
                        screenRect.top + h * 0.78f,
                        checkPaint
                );

                canvas.drawLine(
                        screenRect.left + w * 0.42f,
                        screenRect.top + h * 0.78f,
                        screenRect.left + w * 0.82f,
                        screenRect.top + h * 0.22f,
                        checkPaint
                );
            }

        } else if ("radio".equalsIgnoreCase(type)) {

            // radio 버튼을 왼쪽(상단)에 붙이기 위한 용도
            RectF rf = new RectF(0, 0,
                    Math.min(screenRect.width(), screenRect.height()),
                    Math.min(screenRect.width(), screenRect.height()));

            float cx = screenRect.centerX();
            float cy = screenRect.centerY();

            cx = screenRect.left + rf.centerX();
            cy = screenRect.top + rf.centerY();

            float radius = Math.min(screenRect.width(), screenRect.height()) / 2f;

            //바깥 테두리는 그리지 말자..
            //canvas.drawCircle(cx, cy, radius, shapePaint);

            if (isRadioSelectedValue(field.value)) {
                Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                fillPaint.setStyle(Paint.Style.FILL);
                fillPaint.setColor(field.colorArgb);
                canvas.drawCircle(cx, cy, radius / 2f, fillPaint);
            }

        } else if ("button".equalsIgnoreCase(type)) {

            canvas.drawRect(screenRect, shapePaint);

            String text = safe(field.value);
            if ("".equals(text)) text = safe(field.name);
            canvas.drawText(text, x, y, formTextPaint);

        } else if ("sign".equalsIgnoreCase(type)) {

            /*
             * sign 필드 배경색.
             *
             * 사용자가 서명해야 하는 영역임을 약하게 표시하기 위한 색상이다.
             * 너무 진하면 원본 PDF 내용이나 서명선이 가려질 수 있으므로
             * alpha 값을 낮게 준다.
             *
             * Color.argb(alpha, red, green, blue)
             * - alpha 0   : 완전 투명
             * - alpha 255 : 완전 불투명
             *
             * 현재 값은 "노란색이 있구나" 정도로만 보이는 연한 노란색이다.
             */
            Paint signBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            signBgPaint.setStyle(Paint.Style.FILL);
            signBgPaint.setColor(Color.argb(45, 255, 235, 80));
            canvas.drawRect(screenRect, signBgPaint);

            /*
             * sign field 외곽선.
             *
             * 배경을 먼저 채운 뒤 테두리를 그려야
             * 테두리가 배경에 가려지지 않는다.
             */
            // 배경색을 노란색으로 하여 영영을 표시하였음로 테두리를 그리지 안흔다.
            //Paint signBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            //signBorderPaint.setStyle(Paint.Style.STROKE);
            //signBorderPaint.setColor(Color.BLACK);
            //signBorderPaint.setStrokeWidth(2f);
            //canvas.drawRect(screenRect, signBorderPaint);

        } else if ("sign_image".equalsIgnoreCase(type)) {

            drawDoctorSignValue(canvas, field, screenRect, safe(field.value));

        } else {

            canvas.drawText(safe(field.value), x, y, formTextPaint);

        }

        /*
         * 편집 가능 영역 표시
         *
         * 기존에는 !field.readOnly 일 때만 테두리를 그렸기 때문에
         * metadata 방식 또는 저장된 PDF에서 readOnly=true로 읽힌 경우
         * text / checkbox / radio / sign 영역의 안내 테두리가 보이지 않았다.
         *
         * 요구사항:
         * - MODE_EDIT     : 파란색 테두리
         * - MODE_PEN/ERASER : 초록색 테두리
         * - MODE_NONE     : 테두리 표시 안 함
         *
         * 단, sign_image / label 등은 표시 대상에서 제외한다.
         */
        if (shouldDrawEditableBorder(field)) {
            if (mode == MODE_EDIT) {
                canvas.drawRect(screenRect, editingPaint);
            } else if (mode == MODE_PEN || mode == MODE_ERASER || mode == MODE_MOVE_SIGN) {
                canvas.drawRect(screenRect, editablePaint);
            }
        }

    }

    private boolean drawDoctorSignValue(Canvas canvas,
                                        PdfRenderedFormField field,
                                        RectF screenRect,
                                        String value) {
        if (canvas == null || field == null || screenRect == null) return false;

        String drid = getSignDridFromValue(value);
        if ("".equals(drid)) return false;

        /*
         * sign_image 전용 overlay cache에서 먼저 찾는다.
         */
        PdfSignImageOverlay cached = getSignImageOverlayForField(field, value);
        if (cached != null
                && cached.visible
                && cached.bitmap != null
                && !cached.bitmap.isRecycled()) {

            canvas.drawBitmap(cached.bitmap, null, screenRect, null);
            return true;
        }

        /*
         * cache가 없으면 파일에서 로딩한다.
         */
        Bitmap signBitmap = loadDoctorSignBitmap(drid);
        if (signBitmap == null || signBitmap.isRecycled()) {
            return true;
        }

        canvas.drawBitmap(signBitmap, null, screenRect, null);

        /*
         * sign_image는 PdfSignOverlay가 아니라
         * PdfSignImageOverlay에만 등록한다.
         */
        registerSignImageOverlay(field, value, signBitmap);

        return true;
    }

    private String getSignDridFromValue(String value) {
        if (value == null) return "";

        String v = value.trim();

        if (v.startsWith("sign_")) return v.substring(5);
        if (v.startsWith("logindrsign_")) return v.substring(12);
        if (v.startsWith("login_sign_")) return v.substring(11);

        return "";
    }

    private Bitmap loadDoctorSignBitmap(String drid) {
        if (drid == null || "".equals(drid.trim())) return null;

        String dstDir = getContext().getFilesDir().getAbsolutePath();
        String pathName = dstDir + File.separator + "Sign" + File.separator + drid;

        Bitmap bm = BitmapFactory.decodeFile(pathName);
        if (bm == null) return null;

        Bitmap processed = bm.copy(Bitmap.Config.ARGB_8888, true);
        bm.recycle();

        processed = makeTransparent(processed);
        processed = enhanceBitmap(processed);
        processed = expandStroke(processed, 1);

        return processed;
    }

    private Bitmap makeTransparent(Bitmap bm) {
        if (bm == null) return null;

        int width = bm.getWidth();
        int height = bm.getHeight();

        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];

        bm.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == Color.WHITE) {
                pixels[i] = Color.TRANSPARENT;
            }
        }

        out.setPixels(pixels, 0, width, 0, 0, width, height);
        return out;
    }

    private Bitmap enhanceBitmap(Bitmap src) {
        if (src == null) return null;

        int width = src.getWidth();
        int height = src.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];

        src.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int color = pixels[i];

            int alpha = Color.alpha(color);
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);

            alpha = Math.min(255, (int) (alpha * 1.5));
            pixels[i] = Color.argb(alpha, red, green, blue);
        }

        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    private Bitmap expandStroke(Bitmap src, int radius) {
        if (src == null) return null;

        int width = src.getWidth();
        int height = src.getHeight();

        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int[] srcPixels = new int[width * height];
        int[] outPixels = new int[width * height];

        src.getPixels(srcPixels, 0, width, 0, 0, width, height);
        System.arraycopy(srcPixels, 0, outPixels, 0, srcPixels.length);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                int color = srcPixels[idx];
                int alpha = Color.alpha(color);

                if (alpha > 20) {
                    int red = Color.red(color);
                    int green = Color.green(color);
                    int blue = Color.blue(color);

                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dx = -radius; dx <= radius; dx++) {
                            int nx = x + dx;
                            int ny = y + dy;

                            if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                            if (dx * dx + dy * dy > radius * radius) continue;

                            int nIdx = ny * width + nx;
                            int oldAlpha = Color.alpha(outPixels[nIdx]);

                            if (oldAlpha < alpha) {
                                outPixels[nIdx] = Color.argb(alpha, red, green, blue);
                            }
                        }
                    }
                }
            }
        }

        out.setPixels(outPixels, 0, width, 0, 0, width, height);
        return out;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String getSignFieldKey(PdfRenderedFormField field) {
        if (field == null) return "";

        String name = safe(field.name).trim();
        if (!"".equals(name)) return name;

        if (field.pdfRect == null) return "";

        return "rect_"
                + Math.round(field.pdfRect.left) + "_"
                + Math.round(field.pdfRect.top) + "_"
                + Math.round(field.pdfRect.right) + "_"
                + Math.round(field.pdfRect.bottom);
    }

    public HashMap<String, String> getEditedFieldValues() {
        return new HashMap<String, String>(mEditedFieldValues);
    }

    public void updateFieldValue(String fieldName, String newValue) {
        if (fieldName == null) return;
        if (newValue == null) newValue = "";

        mEditedFieldValues.put(fieldName, newValue);

        for (int i = 0; i < mRenderedFormFields.size(); i++) {
            PdfRenderedFormField field = mRenderedFormFields.get(i);
            if (field == null) continue;

            String name = safe(field.name);
            String ccfField = safe(field.ccfField);

            if (fieldName.equalsIgnoreCase(name)
                    || fieldName.equalsIgnoreCase(ccfField)) {

                field.value = newValue;

                if (!"".equals(name)) {
                    mEditedFieldValues.put(name, newValue);
                }

                if (!"".equals(ccfField)) {
                    mEditedFieldValues.put(ccfField, newValue);
                }

                /*
                 * sign_image 값이 변경되면 기존 bitmap cache 제거.
                 */
                if ("sign_image".equalsIgnoreCase(safe(field.type))) {
                    removeSignImageOverlayForField(field);
                }
            }
        }

        invalidate();
    }

    public boolean injectCcfValue4Doctor(
            String drid,
            String drnm,
            String drnmEng,
            String gdrlcid,
            String sdrlcid
    ) {
        boolean changed = false;

        changed |= updateFieldValueIfExists("drid", drid);
        changed |= updateFieldValueIfExists("drnm", drnm);
        changed |= updateFieldValueIfExists("drnm_eng", drnmEng);
        changed |= updateFieldValueIfExists("gdrlcid", gdrlcid);
        changed |= updateFieldValueIfExists("sdrlcid", sdrlcid);

        if (changed) invalidate();
        return changed;
    }

    public boolean injectCcfValue4Doctor(
            String drid,
            String drnm,
            String drnmEng,
            String gdrlcid,
            String sdrlcid,
            String dptcd,
            String dptnm
    ) {
        boolean changed = false;

        changed |= injectCcfValue4Doctor(drid, drnm, drnmEng, gdrlcid, sdrlcid);
        changed |= updateFieldValueIfExists("dptcd", dptcd);
        changed |= updateFieldValueIfExists("dptnm", dptnm);

        if (changed) invalidate();
        return changed;
    }

    public boolean injectCcfValue4DrSign(String drsign) {
        boolean changed = false;
        if (drsign == null) drsign = "";

        for (int i = 0; i < mRenderedFormFields.size(); i++) {
            PdfRenderedFormField field = mRenderedFormFields.get(i);
            if (field == null) continue;

            String logicalName = safe(field.ccfField);
            if ("".equals(logicalName)) logicalName = safe(field.name);

            if ("drsign".equalsIgnoreCase(logicalName)) {
                String oldValue = safe(field.value);

                if (!drsign.equalsIgnoreCase(oldValue)) {
                    field.value = drsign;
                    changed = true;

                    if (field.name != null && !"".equals(field.name)) {
                        mEditedFieldValues.put(field.name, drsign);
                    }

                    removeSignImageOverlayForField(field);
                }
            }
        }

        if (changed) invalidate();
        return changed;
    }


    public boolean injectCcfValue4Dept(String dptcd, String dptnm) {
        boolean changed = false;

        changed |= updateFieldValueIfExists("dptcd", dptcd);
        changed |= updateFieldValueIfExists("dptnm", dptnm);

        if (changed) invalidate();
        return changed;
    }

    private boolean updateFieldValueIfExists(String ccfField, String newValue) {
        if (ccfField == null) return false;
        if (newValue == null) newValue = "";

        //boolean exists = false;
        boolean changed = false;

        for (int i = 0; i < mRenderedFormFields.size(); i++) {
            PdfRenderedFormField field = mRenderedFormFields.get(i);
            if (field == null) continue;
            if (field.name == null) continue;
            if ("".equalsIgnoreCase(field.name)) continue;

            String logicalCcfField = safe(field.ccfField);

            if (ccfField.equalsIgnoreCase(logicalCcfField)) {
                //exists = true;

                String oldValue = safe(field.value);
                if (!oldValue.equals(newValue)) {
                    field.value = newValue;
                    changed = true;
                }

                mEditedFieldValues.put(field.name, newValue);
            }
        }

        //if (exists) mEditedFieldValues.put(fieldName, newValue);

        return changed;
    }

    public void clearEditedFieldValues() {
        mEditedFieldValues.clear();
        invalidate();
    }

    private void applyEditedValuesToRenderedFields() {
        for (int i = 0; i < mRenderedFormFields.size(); i++) {
            PdfRenderedFormField field = mRenderedFormFields.get(i);
            if (field == null || field.name == null) continue;

            if (mEditedFieldValues.containsKey(field.name)) {
                field.value = mEditedFieldValues.get(field.name);
            }
        }
    }

    private boolean handleTapField(float x, float y) {
        for (int i = mRenderedFormFields.size() - 1; i >= 0; i--) {
            PdfRenderedFormField field = mRenderedFormFields.get(i);
            if (field == null || !field.isValid()) continue;

            RectF screenRect = pdfRectToScreenRect(field.pdfRect);
            if (!screenRect.contains(x, y)) continue;

            String type = safe(field.type);

            if ("checkbox".equalsIgnoreCase(type)) {
                String newValue = isCheckedValue(field.value) ? "Off" : "Yes";

                field.value = newValue;
                if (field.name != null) {
                    mEditedFieldValues.put(field.name, newValue);
                }

                invalidate();
                return true;
            }

            if ("radio".equalsIgnoreCase(type)) {
                selectRadioField(field);
                invalidate();
                return true;
            }

            if ("sign".equalsIgnoreCase(type)) {
                if (mOnPdfSignFieldClickListener != null) {
                    /*
                     * 현재 sign field에 이미 입력된 vector sign path를 가져온다.
                     *
                     * 반환되는 paths는 PDF 좌표계 기준이다.
                     * PdfSignInputView에 바로 표시하려면 입력창 크기에 맞춰
                     * PDF 좌표계 → PdfSignInputView 좌표계로 다시 변환해야 한다.
                     */
                    ArrayList<Path> paths = getSignPathsForField(field);

                    mOnPdfSignFieldClickListener.onPdfSignFieldClick(field, paths);
                }
                return true;
            }

            if ("text".equalsIgnoreCase(type)
                    || "combo".equalsIgnoreCase(type)
                    || "listbox".equalsIgnoreCase(type)
                    || "choice".equalsIgnoreCase(type)
                    || "button".equalsIgnoreCase(type)
                    || "".equals(type)) {

                if (mOnPdfFieldEditListener != null) {
                    mOnPdfFieldEditListener.onPdfTextFieldClick(field);
                }
                return true;
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

    private void selectRadioField(PdfRenderedFormField selectedField) {
        if (selectedField == null) return;

        String selectedGroup = getRadioGroupKey(selectedField);
        //if ("".equals(selectedGroup)) return; // group name이 빈 값이어도 유효함.

        for (int i = 0; i < mRenderedFormFields.size(); i++) {
            PdfRenderedFormField field = mRenderedFormFields.get(i);
            if (field == null) continue;
            if (!"radio".equalsIgnoreCase(safe(field.type))) continue;

            String fieldGroup = getRadioGroupKey(field);
            if (!selectedGroup.equalsIgnoreCase(fieldGroup)) continue;

            String newValue = field == selectedField ? "selected" : "";
            field.value = newValue;

            if (field.name != null && !"".equals(field.name)) {
                mEditedFieldValues.put(field.name, newValue);
            }
        }
    }

    private String getRadioGroupKey(PdfRenderedFormField field) {
        if (field == null) return "nofield";

        String groupName = safe(field.groupName).trim();
        return groupName; // group name이 빈 값이어도 유효함.
        //if (!"".equals(groupName)) return groupName;

        //String ccfField = safe(field.ccfField).trim();
        //if (!"".equals(ccfField)) return ccfField;

        //return safe(field.name).trim();
    }

    private boolean isRadioSelectedValue(String value) {
        String v = safe(value).trim();

        return "true".equalsIgnoreCase(v)
                || "yes".equalsIgnoreCase(v)
                || "on".equalsIgnoreCase(v)
                || "1".equalsIgnoreCase(v)
                || "y".equalsIgnoreCase(v)
                || "selected".equalsIgnoreCase(v)
                || "checked".equalsIgnoreCase(v);
    }

    /**
     * 화면에서 편집 가능 영역 표시 테두리를 그릴 타입인지 확인한다.
     *
     * text / checkbox / radio / sign 은 사용자가 터치하거나 확인해야 하는 영역이므로
     * MODE_EDIT 에서는 파란색,
     * MODE_PEN / MODE_ERASER 에서는 초록색 테두리를 그린다.
     */
    private boolean shouldDrawEditableBorder(PdfRenderedFormField field) {
        if (field == null) return false;

        String type = safe(field.type).trim();

        return "text".equalsIgnoreCase(type)
                || "checkbox".equalsIgnoreCase(type)
                || "radio".equalsIgnoreCase(type)
                || "sign".equalsIgnoreCase(type);
    }

    /**
     * PdfSignInputView에서 입력한 vector sign을 sign field에 저장한다.
     *
     * inputWidth/inputHeight:
     * - PdfSignInputView의 실제 폭/높이
     * - Path 좌표가 이 크기 기준으로 만들어졌기 때문에 반드시 필요하다.
     */
    public void setSignToField(PdfRenderedFormField field,
                               List<Path> paths,
                               int inputWidth,
                               int inputHeight,
                               float strokeWidth,
                               int color) {

        if (field == null || field.pdfRect == null) return;

        String key = getSignFieldKey(field);
        if ("".equals(key)) return;

        PdfSignOverlay overlay = new PdfSignOverlay();
        overlay.paths.clear();

        if (paths != null) {
            for (int i = 0; i < paths.size(); i++) {
                Path viewPath = paths.get(i);
                if (viewPath == null) continue;

                /*
                 * PdfSignInputView 좌표계 Path를 PDF 좌표계 Path로 변환한다.
                 */
                Path pdfPath = new Path(viewPath);
                transformSignInputPathToPdf(
                        pdfPath,
                        inputWidth,
                        inputHeight,
                        field.pdfRect
                );

                overlay.paths.add(pdfPath);
            }
        }

        /*
         * strokeWidth도 PdfSignInputView 기준 px이므로
         * sign field의 PDF 폭 기준으로 변환한다.
         */
        overlay.strokeWidth = signInputStrokeWidthToPdfWidth(
                strokeWidth,
                inputWidth,
                field.pdfRect
        );

        overlay.strokeColor = color;
        overlay.visible = true;
        overlay.pdfRect = new RectF(field.pdfRect);

        overlay.fieldName = field.name;
        overlay.ccfField = field.ccfField;
        overlay.groupName = field.groupName;

        mSignOverlays.put(key, overlay);

        saveCurrentPageOverlay();
        invalidate();
    }


    /**
     * PdfSignInputView의 좌상단 기준 Path를 PDF sign field 영역으로 변환한다.
     *
     * PdfSignInputView 좌표계:
     * - 원점: 입력 View 좌상단
     * - X: 오른쪽 증가
     * - Y: 아래쪽 증가
     *
     * PDF 좌표계:
     * - 원점: PDF 페이지 좌하단
     * - X: 오른쪽 증가
     * - Y: 위쪽 증가
     *
     * 변환:
     * - X는 그대로 scale
     * - Y는 위아래를 뒤집어서 scale
     */
    private void transformSignInputPathToPdf(Path path,
                                             int inputWidth,
                                             int inputHeight,
                                             RectF fieldPdfRect) {
        if (path == null || fieldPdfRect == null) return;

        Matrix matrix = createSignInputToPdfMatrix(
                inputWidth,
                inputHeight,
                fieldPdfRect
        );

        path.transform(matrix);
    }


    /**
     * PdfSignInputView의 strokeWidth(px)를 PDF sign field 기준 strokeWidth로 변환한다.
     */
    private float signInputStrokeWidthToPdfWidth(float inputStrokeWidth,
                                                 int inputWidth,
                                                 RectF fieldPdfRect) {
        if (fieldPdfRect == null) return inputStrokeWidth;
        if (inputWidth <= 0) return inputStrokeWidth;

        float fieldW = Math.abs(fieldPdfRect.right - fieldPdfRect.left);

        return inputStrokeWidth * fieldW / (float) inputWidth;
    }

    /**
     * sign field에 해당하는 PdfSignOverlay를 찾는다.
     *
     * 찾는 순서:
     * 1. 현재 페이지의 mSignOverlays
     * 2. 전체 페이지 저장소인 mPageSigns[currentPageIndex]
     *
     * 주의:
     * - mSignOverlays는 현재 화면에 표시 중인 sign overlay이다.
     * - mPageSigns는 페이지 이동 시 저장해 둔 sign overlay이다.
     */
    private PdfSignOverlay getSignOverlayForField(PdfRenderedFormField field) {
        if (field == null) return null;

        String key = getSignFieldKey(field);
        if ("".equals(key)) return null;

        /*
         * 1. 현재 페이지 overlay에서 먼저 찾는다.
         */
        PdfSignOverlay overlay = mSignOverlays.get(key);

        if (overlay != null && overlay.visible) {
            return overlay;
        }

        /*
         * 2. 현재 페이지 저장소에서 다시 찾는다.
         */
        HashMap<String, PdfSignOverlay> pageMap = mPageSigns.get(currentPageIndex);
        if (pageMap != null) {
            overlay = pageMap.get(key);

            if (overlay != null && overlay.visible) {
                return overlay;
            }
        }

        return null;
    }

    /**
     * sign field에 이미 입력된 vector sign path 목록을 가져온다.
     *
     * 반환값:
     * - null 아님
     * - 기존 sign이 없으면 빈 ArrayList 반환
     *
     * 좌표계:
     * - 반환되는 Path는 PDF 좌표계 기준이다.
     * - 즉, PdfSignOverlay.paths에 저장된 값을 그대로 복사한 것이다.
     *
     * PdfSignInputView에 표시하려면:
     * - PdfSignInputView의 width/height가 정해진 뒤
     * - PDF 좌표계 Path를 PdfSignInputView 좌표계로 변환해야 한다.
     */
    public ArrayList<Path> getSignPathsForField(PdfRenderedFormField field) {
        ArrayList<Path> result = new ArrayList<Path>();

        PdfSignOverlay overlay = getSignOverlayForField(field);
        if (overlay == null) return result;

        if (overlay.paths == null || overlay.paths.size() <= 0) {
            return result;
        }

        for (int i = 0; i < overlay.paths.size(); i++) {
            Path p = overlay.paths.get(i);
            if (p == null) continue;

            /*
             * 원본 Path를 그대로 넘기면 PdfSignInputView 쪽에서 변형할 때
             * 기존 overlay Path가 같이 변형될 수 있다.
             * 반드시 복사해서 반환한다.
             */
            result.add(new Path(p));
        }

        return result;
    }

    /**
     * sign field에 기존 sign이 있는지 확인한다.
     */
    public boolean hasSignPathsForField(PdfRenderedFormField field) {
        PdfSignOverlay overlay = getSignOverlayForField(field);
        if (overlay == null) return false;

        return overlay.paths != null && overlay.paths.size() > 0;
    }

    /**
     * 현재 화면에 표시 중인 PDF overlay field 목록을 저장용으로 복사해서 반환한다.
     *
     * PdfInkPdfSaver는 AcroForm을 사용하지 않으므로
     * 저장 시점에 이 목록을 기준으로 text / label / checkbox / radio / sign_image 값을
     * PDF page content에 직접 그린다.
     */
    public List<PdfRenderedFormField> getRenderedFormFieldsSnapshot() {
        ArrayList<PdfRenderedFormField> copied = new ArrayList<PdfRenderedFormField>();

        for (int i = 0; i < mRenderedFormFields.size(); i++) {
            PdfRenderedFormField src = mRenderedFormFields.get(i);
            if (src == null) continue;

            PdfRenderedFormField dst = new PdfRenderedFormField();

            dst.pageIndex = src.pageIndex;
            dst.name = src.name;
            dst.ccfField = src.ccfField;
            dst.value = src.value;
            dst.type = src.type;
            dst.autoFit = src.autoFit;

            dst.fontSizePdf = src.fontSizePdf;
            dst.colorArgb = src.colorArgb;

            dst.groupName = src.groupName;

            dst.pendingSign = src.pendingSign;
            dst.editable = src.editable;

            if (src.pdfRect != null) {
                dst.pdfRect = new RectF(src.pdfRect);
            }

            copied.add(dst);
        }

        return copied;
    }

    /**
     * PDF 좌표계 sign path를 PdfSignInputView 좌표계 path로 변환해서 반환한다.
     *
     * 사용 시점:
     * - ConsentForm에서 PdfSignInputView를 생성한 뒤
     * - signInputView.post(...) 안에서 width/height가 정해진 후 호출
     *
     * inputWidth/inputHeight:
     * - PdfSignInputView의 실제 width/height
     */
    public ArrayList<Path> getSignInputPathsForField(
            PdfRenderedFormField field,
            int inputWidth,
            int inputHeight
    ) {
        ArrayList<Path> result = new ArrayList<Path>();

        if (field == null || field.pdfRect == null) return result;
        if (inputWidth <= 0 || inputHeight <= 0) return result;

        ArrayList<Path> pdfPaths = getSignPathsForField(field);
        if (pdfPaths == null || pdfPaths.size() <= 0) return result;

        Matrix inputToPdfMatrix = createSignInputToPdfMatrix(
                inputWidth,
                inputHeight,
                field.pdfRect
        );

        Matrix pdfToInputMatrix = new Matrix();

        /*
         * input → PDF 변환 Matrix를 역변환하면
         * PDF → input 변환 Matrix가 된다.
         */
        if (!inputToPdfMatrix.invert(pdfToInputMatrix)) {
            return result;
        }

        for (int i = 0; i < pdfPaths.size(); i++) {
            Path pdfPath = pdfPaths.get(i);
            if (pdfPath == null) continue;

            Path inputPath = new Path(pdfPath);
            inputPath.transform(pdfToInputMatrix);

            result.add(inputPath);
        }

        return result;
    }

    /**
     * PdfSignInputView 좌표계 → PDF sign field 좌표계 변환 Matrix를 만든다.
     *
     * PdfSignInputView 좌표계:
     * - 원점: 입력창 좌상단
     * - X: 오른쪽 증가
     * - Y: 아래쪽 증가
     *
     * PDF 좌표계:
     * - 원점: PDF 페이지 좌하단
     * - X: 오른쪽 증가
     * - Y: 위쪽 증가
     *
     * 변환 방식:
     * - 입력창 전체 영역(inputWidth x inputHeight)을
     *   PDF sign field 영역(fieldPdfRect)에 맞춘다.
     * - Y 방향은 서로 반대이므로 -scale을 적용한다.
     */
    private Matrix createSignInputToPdfMatrix(
            int inputWidth,
            int inputHeight,
            RectF fieldPdfRect
    ) {
        Matrix matrix = new Matrix();

        if (fieldPdfRect == null) {
            return matrix;
        }

        if (inputWidth <= 0) inputWidth = 1;
        if (inputHeight <= 0) inputHeight = 1;

        /*
         * fieldPdfRect는 PDF 좌표계 RectF이다.
         * top/bottom이 항상 정렬되어 있다고 가정하지 않고 안전하게 정규화한다.
         */
        float fieldLeft = Math.min(fieldPdfRect.left, fieldPdfRect.right);
        float fieldRight = Math.max(fieldPdfRect.left, fieldPdfRect.right);
        float fieldBottom = Math.min(fieldPdfRect.top, fieldPdfRect.bottom);
        float fieldTop = Math.max(fieldPdfRect.top, fieldPdfRect.bottom);

        float fieldW = fieldRight - fieldLeft;
        float fieldH = fieldTop - fieldBottom;

        /*
         * 입력창 좌표를 PDF field 크기로 scale 한다.
         *
         * X:
         *   0 ~ inputWidth
         *   → fieldLeft ~ fieldRight
         *
         * Y:
         *   0 ~ inputHeight, 아래로 증가
         *   → fieldTop ~ fieldBottom
         *
         * PDF는 Y가 위로 증가하므로 Y scale은 음수이다.
         */
        matrix.postScale(
                fieldW / (float) inputWidth,
                -fieldH / (float) inputHeight
        );

        /*
         * scale 후 입력창의 (0, 0)을 PDF field의 좌상단 위치로 이동한다.
         * Y scale이 음수이므로 translateY는 fieldTop이 된다.
         */
        matrix.postTranslate(fieldLeft, fieldTop);

        return matrix;
    }


    /**
     * PDF 좌표계의 sign Path 목록을 PdfSignInputView 좌표계 Path 목록으로 변환한다.
     *
     * 사용 목적:
     * - 사용자가 이미 sign 필드에 서명한 뒤
     * - 다시 같은 sign 필드를 터치했을 때
     * - 기존 서명을 PdfSignInputView 입력창에 다시 표시하기 위함.
     *
     * 입력값:
     * - pdfPaths:
     *   PdfInkSignView 내부 PdfSignOverlay.paths에 저장된 Path 목록.
     *   이 Path들은 PDF 좌표계 기준이다.
     *
     * - field:
     *   현재 터치한 sign 필드.
     *   field.pdfRect는 PDF 좌표계 기준의 sign 영역이다.
     *
     * - inputWidth / inputHeight:
     *   PdfSignInputView의 실제 width / height.
     *   반드시 dialog.show() 이후 또는 signView.post() 안에서 넘겨야 한다.
     *
     * 반환값:
     * - PdfSignInputView 좌표계로 변환된 Path 목록.
     *
     * 좌표계 차이:
     * - PDF 좌표계:
     *   원점은 페이지 좌하단, Y는 위로 증가.
     *
     * - PdfSignInputView 좌표계:
     *   원점은 입력창 좌상단, Y는 아래로 증가.
     */
    public ArrayList<Path> convertPdfSignPathsToInputPaths(
            ArrayList<Path> pdfPaths,
            PdfRenderedFormField field,
            int inputWidth,
            int inputHeight
    ) {
        ArrayList<Path> result = new ArrayList<Path>();

        if (pdfPaths == null || pdfPaths.size() <= 0) {
            return result;
        }

        if (field == null || field.pdfRect == null) {
            return result;
        }

        if (inputWidth <= 0 || inputHeight <= 0) {
            return result;
        }

        /*
         * PdfSignInputView 좌표계 → PDF 좌표계 변환 Matrix를 만든다.
         * 이후 invert() 해서 PDF 좌표계 → PdfSignInputView 좌표계 Matrix로 바꾼다.
         */
        Matrix inputToPdfMatrix = createSignInputToPdfMatrix(
                inputWidth,
                inputHeight,
                field.pdfRect
        );

        Matrix pdfToInputMatrix = new Matrix();

        /*
         * 역변환 Matrix를 만들 수 없으면 변환 불가.
         */
        if (!inputToPdfMatrix.invert(pdfToInputMatrix)) {
            return result;
        }

        for (int i = 0; i < pdfPaths.size(); i++) {
            Path pdfPath = pdfPaths.get(i);
            if (pdfPath == null) continue;

            /*
             * 원본 Path를 직접 변환하면 기존 overlay Path까지 변형될 수 있다.
             * 반드시 복사본을 만들어 변환한다.
             */
            Path inputPath = new Path(pdfPath);
            inputPath.transform(pdfToInputMatrix);

            result.add(inputPath);
        }

        return result;
    }

    /**
     * sign_image field key를 만든다.
     *
     * sign과 같은 getSignFieldKey 규칙을 써도 되지만,
     * sign_image 전용 map에 들어가므로 충돌 가능성이 줄어든다.
     */
    private String getSignImageFieldKey(PdfRenderedFormField field) {
        if (field == null) return "";

        String name = safe(field.name).trim();
        if (!"".equals(name)) return name;

        String ccfField = safe(field.ccfField).trim();
        if (!"".equals(ccfField) && field.pdfRect != null) {
            return ccfField + "_"
                    + Math.round(field.pdfRect.left) + "_"
                    + Math.round(field.pdfRect.top) + "_"
                    + Math.round(field.pdfRect.right) + "_"
                    + Math.round(field.pdfRect.bottom);
        }

        if (field.pdfRect == null) return "";

        return "sign_image_rect_"
                + Math.round(field.pdfRect.left) + "_"
                + Math.round(field.pdfRect.top) + "_"
                + Math.round(field.pdfRect.right) + "_"
                + Math.round(field.pdfRect.bottom);
    }

    /**
     * sign_image overlay cache를 가져온다.
     */
    private PdfSignImageOverlay getSignImageOverlayForField(PdfRenderedFormField field,
                                                            String value) {
        if (field == null) return null;

        String key = getSignImageFieldKey(field);
        if ("".equals(key)) return null;

        PdfSignImageOverlay overlay = mSignImageOverlays.get(key);

        if (overlay == null) {
            HashMap<String, PdfSignImageOverlay> pageMap = mPageSignImages.get(currentPageIndex);
            if (pageMap != null) {
                overlay = pageMap.get(key);
            }
        }

        if (overlay == null) return null;
        if (!overlay.visible) return null;

        /*
         * field.value가 바뀐 경우 기존 cache를 사용하면 안 된다.
         */
        if (!safe(value).equalsIgnoreCase(safe(overlay.value))) {
            return null;
        }

        return overlay;
    }

    /**
     * sign_image 전용 overlay를 등록한다.
     *
     * 주의:
     * - 이 함수는 mSignOverlays를 절대 건드리지 않는다.
     * - sign_image는 PdfSignOverlay가 아니라 PdfSignImageOverlay로만 관리한다.
     */
    private void registerSignImageOverlay(PdfRenderedFormField field,
                                          String value,
                                          Bitmap bitmap) {
        if (field == null || bitmap == null || bitmap.isRecycled()) return;
        if (field.pdfRect == null) return;

        String key = getSignImageFieldKey(field);
        if ("".equals(key)) return;

        PdfSignImageOverlay overlay = new PdfSignImageOverlay();

        overlay.bitmap = bitmap;
        overlay.visible = true;
        overlay.pdfRect = new RectF(field.pdfRect);

        overlay.fieldName = field.name;
        overlay.ccfField = field.ccfField;
        overlay.groupName = field.groupName;
        overlay.value = value;

        mSignImageOverlays.put(key, overlay);
        saveCurrentPageOverlay();
    }

    /**
     * sign_image 값이 바뀌었을 때 기존 cache를 제거한다.
     *
     * 예:
     * - 의사를 변경해서 drsign 값이 바뀐 경우
     * - sign_image 필드를 직접 편집해서 value가 바뀐 경우
     */
    private void removeSignImageOverlayForField(PdfRenderedFormField field) {
        if (field == null) return;

        String key = getSignImageFieldKey(field);
        if ("".equals(key)) return;

        mSignImageOverlays.remove(key);

        HashMap<String, PdfSignImageOverlay> pageMap = mPageSignImages.get(currentPageIndex);
        if (pageMap != null) {
            pageMap.remove(key);

            if (pageMap.size() <= 0) {
                mPageSignImages.remove(currentPageIndex);
            }
        }
    }

    /**
     * text/label 값을 field 박스 안에 자동 줄바꿈하여 출력한다.
     *
     * 처리 규칙:
     * 1. 엔터 문자는 강제 줄바꿈으로 처리한다.
     * 2. 한 줄이 field 박스 오른쪽을 벗어나면 자동 줄바꿈한다.
     * 3. 다음 줄이 field 박스 아래쪽을 벗어나면 더 이상 출력하지 않는다.
     *
     * 주의:
     * - Android Canvas.drawText(x, y, paint)의 y는 baseline이다.
     * - RectF는 화면 좌표계이므로 top < bottom 이다.
     */
    private void drawAutoFitTextInBox(Canvas canvas,
                                      String text,
                                      RectF box,
                                      Paint paint) {
        if (canvas == null || box == null || paint == null) return;
        if (text == null) text = "";

        /*
         * 좌우/상하 여백.
         * 글자가 박스 테두리에 너무 붙지 않게 한다.
         */
        float paddingX = 2f;
        float paddingY = 0f;

        float maxWidth = box.width() - (paddingX * 2f);
        float maxHeight = box.height() - (paddingY * 2f);

        if (maxWidth <= 0f || maxHeight <= 0f) {
            return;
        }

        Paint.FontMetrics fm = paint.getFontMetrics();

        /*
         * lineHeight:
         * - descent - ascent가 실제 글자 높이에 가깝다.
         * - 줄 간격을 조금 주기 위해 1.05f를 곱한다.
         */
        float lineHeight = (fm.descent - fm.ascent) * 1.05f;
        if (lineHeight <= 0f) {
            lineHeight = paint.getTextSize() * 1.2f;
        }

        /*
         * 첫 줄 baseline.
         *
         * box.top + paddingY는 글자 영역의 위쪽이다.
         * Canvas.drawText는 baseline 기준이므로 -fm.ascent를 더한다.
         */
        float startX = box.left + paddingX;
        float baselineY = box.top + paddingY - fm.ascent;

        /*
         * 마지막으로 허용되는 baseline.
         *
         * baseline + descent가 box.bottom - paddingY를 넘으면
         * 실제 글자가 박스를 벗어난다.
         */
        float maxBaselineY = box.bottom - paddingY - fm.descent;

        /*
         * \r\n, \r 을 모두 \n으로 통일한다.
         * split(..., -1)을 써야 마지막 빈 줄도 유지된다.
         */
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n", -1);

        for (int p = 0; p < paragraphs.length; p++) {
            String paragraph = paragraphs[p];

            /*
             * 빈 줄도 한 줄 높이를 차지하게 처리한다.
             */
            if ("".equals(paragraph)) {
                if (baselineY > maxBaselineY) {
                    return;
                }

                /*
                 * 빈 줄은 실제로 그릴 문자는 없고 다음 줄로 이동한다.
                 */
                baselineY += lineHeight;
                continue;
            }

            ArrayList<String> lines = buildWrappedLines(paragraph, paint, maxWidth);

            for (int i = 0; i < lines.size(); i++) {
                if (baselineY > maxBaselineY) {
                    return;
                }

                canvas.drawText(lines.get(i), startX, baselineY, paint);
                baselineY += lineHeight;
            }
        }
    }

    /**
     * 한 문단을 maxWidth 안에 들어가도록 여러 줄로 분리한다.
     *
     * 한글/영문 혼합을 단순하고 안전하게 처리하기 위해
     * 문자 단위로 폭을 누적한다.
     *
     * - 공백 단어 기준 줄바꿈보다 예쁘지는 않지만,
     *   한글 의료 문서처럼 공백이 적은 문자열에서 안정적이다.
     * - 한 글자 자체가 maxWidth보다 커도 최소 1글자는 한 줄에 출력한다.
     */
    private ArrayList<String> buildWrappedLines(String text,
                                                Paint paint,
                                                float maxWidth) {
        ArrayList<String> lines = new ArrayList<String>();

        if (text == null) {
            lines.add("");
            return lines;
        }

        if ("".equals(text)) {
            lines.add("");
            return lines;
        }

        StringBuilder line = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            String candidate = line.toString() + ch;

            if (paint.measureText(candidate) <= maxWidth) {
                line.append(ch);
                continue;
            }

            /*
             * candidate가 maxWidth를 넘었다.
             * 기존 line이 있으면 먼저 한 줄로 확정한다.
             */
            if (line.length() > 0) {
                lines.add(line.toString());
                line.setLength(0);
            }

            /*
             * 현재 문자 하나도 maxWidth보다 클 수 있다.
             * 그래도 무한 루프를 막기 위해 한 글자는 한 줄로 넣는다.
             */
            String single = String.valueOf(ch);
            if (paint.measureText(single) > maxWidth) {
                lines.add(single);
            } else {
                line.append(ch);
            }
        }

        if (line.length() > 0) {
            lines.add(line.toString());
        }

        return lines;
    }

    /**
     * autoFit field 입력창에 필요한 정보를 계산한다.
     *
     * 계산 내용:
     * - field 박스 폭 기준 한 줄 예상 최대 글자 수
     * - field 박스 높이 기준 최대 줄 수
     * - field 박스 전체 예상 최대 글자 수
     * - 현재 value가 field 박스 폭 기준 몇 줄 필요한지
     * - EditText에 적용할 추천 줄 수
     *
     * 주의:
     * - maxCharsPerLine / maxCharsTotal은 "한글 기준 예상값"이다.
     * - 영문, 숫자, 공백은 한글보다 폭이 좁아 더 많이 들어갈 수 있다.
     */
    public PdfAutoFitInputInfo getAutoFitInputInfo(PdfRenderedFormField field, String value) {
        PdfAutoFitInputInfo info = new PdfAutoFitInputInfo();

        /*
         * 계산 실패 시 기본값.
         */
        info.maxCharsPerLine = 10;
        info.maxLines = 2;
        info.maxCharsTotal = 20;
        info.requiredLines = 2;
        info.editLines = 2;

        if (field == null || field.pdfRect == null) {
            return info;
        }

        if (value == null) value = "";

        /*
         * field.pdfRect는 PDF 좌표계이다.
         * 실제 화면에 그리는 기준과 동일하게 screenRect로 변환한다.
         */
        RectF screenRect = pdfRectToScreenRect(field.pdfRect);

        if (screenRect.width() <= 0f || screenRect.height() <= 0f) {
            return info;
        }

        /*
         * drawRenderedFormField()와 동일하게 fontSizePdf를 화면 px로 변환한다.
         */
        float textSizePx = pdfWidthToScreenWidth(field.fontSizePdf);

        if (textSizePx <= 0f) {
            textSizePx = 10f;
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(field.colorArgb);
        paint.setTextSize(textSizePx);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);

        /*
         * 화면 출력과 같은 폰트 사용.
         */
        paint.setTypeface(formTextPaint.getTypeface());

        /*
         * drawAutoFitTextInBox()와 같은 padding 기준.
         */
        float paddingX = 2f;
        float paddingY = 2f;

        float maxTextWidth = screenRect.width() - (paddingX * 2f);
        float maxTextHeight = screenRect.height() - (paddingY * 2f);

        if (maxTextWidth <= 0f || maxTextHeight <= 0f) {
            return info;
        }

        Paint.FontMetrics fm = paint.getFontMetrics();

        /*
         * drawAutoFitTextInBox()와 같은 줄 높이 기준.
         */
        float lineHeight = (fm.descent - fm.ascent) * 1.05f;

        if (lineHeight <= 0f) {
            lineHeight = paint.getTextSize() * 1.2f;
        }

        /*
         * field 박스 높이에 들어갈 수 있는 최대 줄 수.
         */
        int maxLines = (int) (maxTextHeight / lineHeight);
        if (maxLines < 1) maxLines = 1;

        /*
         * 한 줄에 들어갈 예상 최대 글자 수.
         *
         * 한글 기준으로 계산한다.
         * "가"는 일반적인 한글 한 글자 폭을 대표값으로 사용한다.
         */
        float avgCharWidth = paint.measureText("가");

        if (avgCharWidth <= 0f) {
            avgCharWidth = paint.getTextSize();
        }

        int maxCharsPerLine = (int) (maxTextWidth / avgCharWidth);
        if (maxCharsPerLine < 1) maxCharsPerLine = 1;

        /*
         * field 전체에 들어갈 예상 최대 글자 수.
         */
        int maxCharsTotal = maxCharsPerLine * maxLines;
        if (maxCharsTotal < 1) maxCharsTotal = 1;

        /*
         * 현재 value가 field 박스 폭 기준으로 몇 줄 필요한지 계산한다.
         */
        int requiredLines = estimateAutoFitRequiredLines(value, paint, maxTextWidth);
        if (requiredLines < 1) requiredLines = 1;

        /*
         * EditText 추천 줄 수.
         *
         * 현재 입력값이 필요한 줄 수를 기준으로 하되,
         * 실제 field 박스에 들어갈 수 있는 줄 수를 넘지 않게 한다.
         */
        int editLines = requiredLines;

        if (editLines > maxLines) editLines = maxLines;
        if (editLines < 2) editLines = 2;
        if (editLines > 10) editLines = 10;

        info.maxCharsPerLine = maxCharsPerLine;
        info.maxLines = maxLines;
        info.maxCharsTotal = maxCharsTotal;
        info.requiredLines = requiredLines;
        info.editLines = editLines;

        return info;
    }


    /**
     * autoFit text가 field 박스 폭 기준으로 몇 줄이 필요한지 계산한다.
     *
     * drawAutoFitTextInBox()와 같은 방식으로:
     * - 엔터는 강제 줄바꿈
     * - 오른쪽 폭을 넘으면 문자 단위 자동 줄바꿈
     */
    private int estimateAutoFitRequiredLines(String text, Paint paint, float maxWidth) {
        if (text == null) text = "";
        if (paint == null || maxWidth <= 0f) return 1;

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n", -1);

        int totalLines = 0;

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i];

            if ("".equals(paragraph)) {
                totalLines++;
                continue;
            }

            totalLines += estimateWrappedLineCount(paragraph, paint, maxWidth);
        }

        if (totalLines <= 0) totalLines = 1;

        return totalLines;
    }

    /**
     * 한 문단이 maxWidth 안에서 몇 줄로 나뉘는지 계산한다.
     *
     * 한글은 공백 없이 길어지는 경우가 많으므로 문자 단위로 계산한다.
     */
    private int estimateWrappedLineCount(String text, Paint paint, float maxWidth) {
        if (text == null || "".equals(text)) return 1;
        if (paint == null || maxWidth <= 0f) return 1;

        int lineCount = 1;
        StringBuilder line = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            String candidate = line.toString() + ch;

            if (paint.measureText(candidate) <= maxWidth) {
                line.append(ch);
                continue;
            }

            /*
             * 현재 문자를 붙이면 field 박스 폭을 넘으므로 다음 줄로 이동.
             */
            lineCount++;
            line.setLength(0);

            /*
             * 현재 문자 하나가 maxWidth보다 큰 경우에도
             * 무한 루프를 피하기 위해 새 줄에 넣고 계속 진행한다.
             */
            line.append(ch);
        }

        return lineCount;
    }

    /**
     * autoFit text field의 입력 가능 정보.
     */
    public static class PdfAutoFitInputInfo {
        /**
         * field 박스 폭 기준으로 한 줄에 들어갈 수 있는 예상 글자 수.
         * 한글 "가" 기준으로 계산한다.
         */
        public int maxCharsPerLine;

        /**
         * field 박스 높이 기준으로 들어갈 수 있는 최대 줄 수.
         */
        public int maxLines;

        /**
         * field 박스 전체에 들어갈 수 있는 예상 최대 글자 수.
         *
         * 계산식:
         * maxCharsPerLine * maxLines
         */
        public int maxCharsTotal;

        /**
         * 현재 value가 field 박스 폭 기준으로 몇 줄 필요한지.
         */
        public int requiredLines;

        /**
         * EditText에 적용할 추천 줄 수.
         */
        public int editLines;
    }



}