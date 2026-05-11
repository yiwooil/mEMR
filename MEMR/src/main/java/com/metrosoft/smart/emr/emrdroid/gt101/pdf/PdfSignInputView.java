package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PdfSignInputView extends View {

    private Paint paint;
    private Path currentPath;
    private ArrayList<Path> paths = new ArrayList<Path>();

    public PdfSignInputView(Context context) {
        super(context);

        currentPath = null;

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
        paths.clear();
        currentPath = null;

        invalidate();
    }

    public List<Path> getSignPaths() {
        ArrayList<Path> copied = new ArrayList<Path>();

        for (int i = 0; i < paths.size(); i++) {
            copied.add(new Path(paths.get(i)));
        }

        if (currentPath != null) {
            copied.add(new Path(currentPath));
        }

        return copied;
    }

    public boolean hasVectorSign() {
        return paths.size() > 0 || currentPath != null;
    }

    public float getSignStrokeWidth() {
        return paint.getStrokeWidth();
    }

    public int getSignStrokeColor() {
        return paint.getColor();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (int i = 0; i < paths.size(); i++) {
            canvas.drawPath(paths.get(i), paint);
        }

        if (currentPath != null) {
            canvas.drawPath(currentPath, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentPath = new Path();
                currentPath.moveTo(x, y);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentPath != null) {
                    paths.add(new Path(currentPath));
                    currentPath = null;
                }
                invalidate();
                return true;
        }

        return true;
    }

    public void setSignPaths(List<Path> inputPaths) {
        paths.clear();
        currentPath = null;

        if (inputPaths != null) {
            for (int i = 0; i < inputPaths.size(); i++) {
                Path p = inputPaths.get(i);
                if (p == null) continue;
                paths.add(new Path(p));
            }
        }

        invalidate();
    }


}