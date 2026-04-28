package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

public class PdfSignatureInputView extends View {

    private Paint paint;
    private Path path;

    public PdfSignatureInputView(Context context) {
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
        invalidate();
    }

    public Bitmap getSignatureBitmap() {
        if (getWidth() <= 0 || getHeight() <= 0) return null;

        Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(Color.TRANSPARENT);
        canvas.drawPath(path, paint);

        return bitmap;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
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
}