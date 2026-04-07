package com.metrosoft.smart.emr.emrdroid.gt101.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.metrosoft.smart.emr.emrdroid.gt101.data.PointDilatation;

import java.util.ArrayList;
import java.util.List;

public class LaborRecordGraphView extends View {
    private Paint linePaint, pointPaint, textPaint, gridPaint, dateLinePaint;
    private List<PointDilatation> dataPoints;
    private int width, height;
    private int padding_left, padding_right, padding_top, padding_bottom;
    private int top_y, bottom_y, title_y;
    private int max_x, max_y;
    private float xScale, yScale;
    private String method_list = "";

    public LaborRecordGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(Color.BLUE);
        linePaint.setStrokeWidth(5f);
        linePaint.setStyle(Paint.Style.STROKE);

        pointPaint = new Paint();
        pointPaint.setColor(Color.RED);
        pointPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(30f);

        gridPaint = new Paint();
        gridPaint.setColor(Color.BLACK);
        gridPaint.setColor(Color.DKGRAY);
        gridPaint.setStrokeWidth(2f);
        gridPaint.setStyle(Paint.Style.STROKE);

        dateLinePaint = new Paint();
        dateLinePaint.setColor(Color.RED);
        dateLinePaint.setStrokeWidth(3f);
        dateLinePaint.setStyle(Paint.Style.STROKE);

        dataPoints = new ArrayList<>();
        padding_left = 120;
        padding_right = 50;
        padding_top = 150;
        padding_bottom = 150;
        title_y = 100;

        xScale = 0;
        yScale = 0;
    }

    public void setDataPoints(List<PointDilatation> poins) {
        method_list += "setDataPoints ";
        dataPoints.clear();
        for(int i = 0; i < poins.size(); i++) {
            PointDilatation p = poins.get(i);
            dataPoints.add(new PointDilatation(p.getIdx(), p.getExdt(), p.getExtm(), p.getValue()));
        }
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        method_list += "onSizeChanged ";
        super.onSizeChanged(w, h, oldw, oldh);
        width = w;
        height = h;
    }

    private void updateScales() {
        method_list += "updateScales ";
        if (dataPoints.isEmpty()) return;

        top_y = padding_top;
        bottom_y = height - padding_bottom;

        max_x = dataPoints.size();
        max_y = 10;

        xScale = (float)(width - padding_left - padding_right) / max_x;
        yScale = (float)(bottom_y - top_y) / max_y;
    }

    private float getPosX(int value_x) {
        return padding_left + value_x * xScale;
    }

    private float getPosY(float value_y) {
        return (float)top_y + ((float)max_y - value_y) * yScale;
    }

    private String getLabelX(int idx) {
        return dataPoints.get(idx).getExtm();
    }

    private String getExdt(int idx) {
        return dataPoints.get(idx).getExdt();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        method_list += "onDraw ";
        super.onDraw(canvas);

        if (dataPoints.isEmpty()) return;

        updateScales();

        // 제목
        String title = "Dilatation (Cm)";
        Rect bonds = new Rect();
        textPaint.getTextBounds(title, 0, title.length(), bonds);
        canvas.drawText(title, (width - bonds.width()) / 2, title_y, textPaint); // 레이블

        // 가로줄
        float pos_x1 = getPosX(0);
        float pos_x2 = getPosX(max_x); // 최소 10개는 있게...
        for (int i = 0; i <= max_y; i++) {
            float pos_y = getPosY(i);
            canvas.drawLine(pos_x1 - 15, pos_y, pos_x2, pos_y, gridPaint);
            float x = pos_x1 - 60;
            if (i >= 10) x = pos_x1 - 80;
            canvas.drawText(String.valueOf(i), x, pos_y + 10, textPaint); // 레이블(0 ~ 10)
        }

        // 세로줄
        String bkExdt = "";
        float pos_y1 = getPosY(0);
        float pos_y2 = getPosY(max_y);
        for (int i = 0; i < max_x ; i++) {
            float pos_x = getPosX(i);
            canvas.drawLine(pos_x, pos_y1, pos_x, pos_y2, gridPaint);
            // 날짜가 바뀌면 세로줄을 빨간색으로 그린다.
            String exdt = getExdt(i);
            if (!exdt.equalsIgnoreCase(bkExdt)) {
                canvas.drawLine(pos_x, pos_y1, pos_x, pos_y2, dateLinePaint);
                // 일자를 표신한다.
                for (int j = 0; j < 8 ; j++) {
                    float x = pos_x - 10;
                    float y = pos_y1 - 30 - 20 * j;
                    canvas.rotate(-90, x, y);
                    canvas.drawText(exdt.substring(j, j + 1), x, y, textPaint);
                    canvas.rotate(90, x, y);
                }
            }
            bkExdt = exdt;
            // 레이블
            String label = getLabelX(i);
            for (int j = 0; j < 4; j++) {
                float x = pos_x + 8;
                float y = pos_y1 + 30 + 20 * (4 - j);
                canvas.rotate(-90, x, y);
                canvas.drawText(label.substring(j, j + 1), x, y, textPaint);
                canvas.rotate(90, x, y);
            }
        }

        // 값 사이를 연결
        for (int i = 1; i < dataPoints.size(); i++) {
            PointDilatation start = dataPoints.get(i - 1);
            PointDilatation end = dataPoints.get(i);

            float startX = getPosX(start.getIdx());
            float startY = getPosY(start.getValue());
            float endX = getPosX(end.getIdx());
            float endY = getPosY(end.getValue());

            canvas.drawLine(startX, startY, endX, endY, linePaint);
        }

        // 값을 사각형으로 표시
        for (int i = 0; i < dataPoints.size(); i++) {
            PointDilatation p = dataPoints.get(i);

            float startX = getPosX(p.getIdx());
            float startY = getPosY(p.getValue());

            canvas.drawCircle(startX, startY, 10, pointPaint);
        }
    }}
