package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.graphics.PointF;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;

public class PdfInkStroke {
    public final List<PointF> pointsPdf = new ArrayList<PointF>();
    public int colorArgb;
    public float strokeWidthPdf;

    public PdfInkStroke(int colorArgb, float strokeWidthPdf) {
        this.colorArgb = colorArgb;
        this.strokeWidthPdf = strokeWidthPdf;
    }

    public void addPdfPoint(float pdfX, float pdfY) {
        pointsPdf.add(new PointF(pdfX, pdfY));
    }

    public boolean isValid() {
        return pointsPdf.size() >= 2;
    }

    public RectF getPdfBounds() {
        RectF rect = new RectF();
        if (pointsPdf.isEmpty()) {
            return rect;
        }

        float minX = pointsPdf.get(0).x;
        float maxX = pointsPdf.get(0).x;
        float minY = pointsPdf.get(0).y;
        float maxY = pointsPdf.get(0).y;

        for (int i = 1; i < pointsPdf.size(); i++) {
            PointF p = pointsPdf.get(i);
            if (p.x < minX) minX = p.x;
            if (p.x > maxX) maxX = p.x;
            if (p.y < minY) minY = p.y;
            if (p.y > maxY) maxY = p.y;
        }

        rect.left = minX;
        rect.right = maxX;
        rect.top = maxY;
        rect.bottom = minY;

        return rect;
    }

    public PdfInkStroke copy() {
        PdfInkStroke copied = new PdfInkStroke(colorArgb, strokeWidthPdf);
        for (int i = 0; i < pointsPdf.size(); i++) {
            PointF p = pointsPdf.get(i);
            copied.pointsPdf.add(new PointF(p.x, p.y));
        }
        return copied;
    }
}