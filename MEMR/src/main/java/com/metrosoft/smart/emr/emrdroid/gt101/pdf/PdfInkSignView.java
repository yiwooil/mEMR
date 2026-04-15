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
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.support.v7.widget.AppCompatImageView;
import android.view.MotionEvent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class PdfInkSignView extends AppCompatImageView {

    public static final int MODE_NONE = 0;
    public static final int MODE_PEN = 1;
    public static final int MODE_ERASER = 2;
    public static final int MODE_MOVE_SIGNATURE = 3;

    private final Paint pdfPaint = new Paint(Paint.DITHER_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint signatureFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Bitmap pageBitmap;
    private ParcelFileDescriptor pfd;
    private PdfRenderer pdfRenderer;
    private PdfRenderer.Page currentPage;
    private int currentPageIndex = 0;

    private int currentPdfPageWidth;
    private int currentPdfPageHeight;

    // 현재 페이지 표시용
    private final List<PdfInkStroke> strokes = new ArrayList<PdfInkStroke>();
    private PdfInkStroke currentStroke;
    private PdfSignatureOverlay signatureOverlay = new PdfSignatureOverlay();

    // 전체 페이지 저장용
    private final HashMap<Integer, ArrayList<PdfInkStroke>> mPageStrokes =
            new HashMap<Integer, ArrayList<PdfInkStroke>>();
    private final HashMap<Integer, PdfSignatureOverlay> mPageSignatures =
            new HashMap<Integer, PdfSignatureOverlay>();

    // 2026.04.15 WOOIL - Annotation 추가용
    private final List<PdfRenderedInkAnnotation> mRenderedAnnotations =
            new ArrayList<PdfRenderedInkAnnotation>();
    private File mCurrentPdfFile;

    private int mode = MODE_NONE;
    private int penColor = Color.RED;
    private float penWidthPx = 4f;
    private float eraserHitPx = 40f;

    private float lastTouchX;
    private float lastTouchY;


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

        signatureFramePaint.setStyle(Paint.Style.STROKE);
        signatureFramePaint.setStrokeWidth(2f);
        signatureFramePaint.setColor(Color.argb(180, 50, 50, 50));
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

    public void clearAllOverlays() {
        strokes.clear();
        currentStroke = null;
        signatureOverlay = new PdfSignatureOverlay();

        mPageStrokes.clear();
        mPageSignatures.clear();

        invalidate();
    }

    public void clearCurrentPageOverlay() {
        strokes.clear();
        currentStroke = null;
        signatureOverlay = new PdfSignatureOverlay();

        saveCurrentPageOverlay();
        invalidate();
    }

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

        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= pdfRenderer.getPageCount()) pageIndex = pdfRenderer.getPageCount() - 1;

        currentPageIndex = pageIndex;
        openRendererPage(currentPageIndex);
        restorePageOverlay(currentPageIndex);

        // 추가: 기존 PDF 안의 Ink annotation 읽기
        try {
            List<PdfRenderedInkAnnotation> annots =
                    PdfInkAnnotationReader.readInkAnnotations(getContext(), pdfFile, currentPageIndex);
            setRenderedAnnotations(annots);
        } catch (Exception e) {
            mRenderedAnnotations.clear();
        }
        mCurrentPdfFile = pdfFile;

        requestLayout();
        invalidate();
    }

    public void showPage(int pageIndex) throws IOException {
        if (pdfRenderer == null) return;

        saveCurrentPageOverlay();

        if (pageIndex < 0) pageIndex = 0;
        if (pageIndex >= pdfRenderer.getPageCount()) pageIndex = pdfRenderer.getPageCount() - 1;

        currentPageIndex = pageIndex;
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

        if (signatureOverlay != null && signatureOverlay.visible) {
            mPageSignatures.put(currentPageIndex, signatureOverlay.copyShallow());
        } else {
            mPageSignatures.remove(currentPageIndex);
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

        PdfSignatureOverlay savedSign = mPageSignatures.get(pageIndex);
        if (savedSign != null) {
            signatureOverlay = savedSign.copyShallow();
        } else {
            signatureOverlay = new PdfSignatureOverlay();
        }
    }

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

        // PDF에 저장되어 있는 Ink annotation 다시 그리기
        for (int i = 0; i < mRenderedAnnotations.size(); i++) {
            drawRenderedAnnotation(canvas, mRenderedAnnotations.get(i));
        }

        // 현재 앱에서 그린 stroke
        for (int i = 0; i < strokes.size(); i++) {
            drawStroke(canvas, strokes.get(i));
        }

        if (currentStroke != null) {
            drawStroke(canvas, currentStroke);
        }

        if (signatureOverlay.visible && signatureOverlay.bitmap != null && !signatureOverlay.bitmap.isRecycled()) {
            RectF screenRect = pdfRectToScreenRect(signatureOverlay.pdfRect);
            canvas.drawBitmap(signatureOverlay.bitmap, null, screenRect, null);
            canvas.drawRect(screenRect, signatureFramePaint);
        }
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

    public RectF getPageDrawRect() {
        if (pageBitmap == null) {
            return new RectF(0, 0, getWidth(), getHeight());
        }

        float viewW = getWidth();
        float viewH = getHeight();
        float bmpW = pageBitmap.getWidth();
        float bmpH = pageBitmap.getHeight();

        float scale = Math.min(viewW / bmpW, viewH / bmpH);
        float drawW = bmpW * scale;
        float drawH = bmpH * scale;
        float left = (viewW - drawW) / 2f;
        float top = (viewH - drawH) / 2f;

        return new RectF(left, top, left + drawW, top + drawH);
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

    public PointF pdfToScreen(float pdfX, float pdfY) {
        RectF pageRect = getPageDrawRect();

        float normalizedX = pdfX / currentPdfPageWidth;
        float normalizedYFromTop = (currentPdfPageHeight - pdfY) / (float) currentPdfPageHeight;

        float sx = pageRect.left + normalizedX * pageRect.width();
        float sy = pageRect.top + normalizedYFromTop * pageRect.height();

        return new PointF(sx, sy);
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
        float x = event.getX();
        float y = event.getY();

        switch (mode) {
            case MODE_PEN:
                return handlePen(event, x, y);
            case MODE_ERASER:
                return handleEraser(event, x, y);
            case MODE_MOVE_SIGNATURE:
                return handleMoveSignature(event, x, y);
            default:
                return super.onTouchEvent(event);
        }
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

    private boolean isNearRect(RectF rect, float x, float y, float padding) {
        RectF r = new RectF(
                rect.left - padding,
                rect.top - padding,
                rect.right + padding,
                rect.bottom + padding
        );
        return r.contains(x, y);
    }

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
    }

    // 2026.04.15 WOOIL
    public void setRenderedAnnotations(List<PdfRenderedInkAnnotation> annotations) {
        mRenderedAnnotations.clear();
        if (annotations != null) {
            mRenderedAnnotations.addAll(annotations);
        }
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
}