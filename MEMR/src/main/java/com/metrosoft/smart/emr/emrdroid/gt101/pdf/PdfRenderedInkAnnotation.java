package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.graphics.PointF;
import java.util.ArrayList;
import java.util.List;

public class PdfRenderedInkAnnotation {
    public final List<PointF> pointsPdf = new ArrayList<PointF>();
    public int colorArgb;
    public float strokeWidthPdf;

    public boolean isValid() {
        return pointsPdf.size() >= 2;
    }
}