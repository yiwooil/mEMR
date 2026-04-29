package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;

public class PdfSignInputView extends View {

    private Paint paint;
    private Path path;
    private Bitmap baseBitmap;

    public PdfSignInputView(Context context) {
        super(context);

        path = new Path();

        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(5f);

        setBackgroundColor(Color.WHITE);
    }

    public void clear() {
        path.reset();

        if (baseBitmap != null && !baseBitmap.isRecycled()) {
            baseBitmap.recycle();
        }
        baseBitmap = null;

        invalidate();
    }

    public Bitmap getSignBitmap() {
        if (getWidth() <= 0 || getHeight() <= 0) return null;

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(Color.TRANSPARENT);

        if (baseBitmap != null && !baseBitmap.isRecycled()) {
            canvas.drawBitmap(baseBitmap, null,
                    new android.graphics.RectF(0, 0, getWidth(), getHeight()), null);
        }

        canvas.drawPath(path, paint);

        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (baseBitmap != null && !baseBitmap.isRecycled()) {
            canvas.drawBitmap(baseBitmap, null,
                    new android.graphics.RectF(0, 0, getWidth(), getHeight()), null);
        }

        canvas.drawPath(path, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                invalidate();
                return true;
        }

        return true;
    }

    public void setSignBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return;

        baseBitmap = Bitmap.createScaledBitmap(
                bitmap,
                getWidth() > 0 ? getWidth() : bitmap.getWidth(),
                getHeight() > 0 ? getHeight() : bitmap.getHeight(),
                true
        );

        path.reset();
        invalidate();
    }

}