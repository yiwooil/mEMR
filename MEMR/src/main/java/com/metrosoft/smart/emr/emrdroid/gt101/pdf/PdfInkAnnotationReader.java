package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.cos.COSArray;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PdfInkAnnotationReader {

    public static List<PdfRenderedInkAnnotation> readInkAnnotations(
            Context context,
            File pdfFile,
            int pageIndex
    ) throws Exception {

        PDFBoxResourceLoader.init(context);

        List<PdfRenderedInkAnnotation> result = new ArrayList<PdfRenderedInkAnnotation>();
        PDDocument document = null;

        try {
            document = PDDocument.load(pdfFile);

            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                return result;
            }

            PDPage page = document.getPage(pageIndex);
            List<PDAnnotation> annots = page.getAnnotations();

            for (int i = 0; i < annots.size(); i++) {
                PDAnnotation ann = annots.get(i);

                String subtype = ann.getSubtype();
                if (!"Ink".equalsIgnoreCase(subtype)) {
                    continue;
                }

                if (!(ann instanceof PDAnnotationMarkup)) {
                    continue;
                }

                PDAnnotationMarkup markup = (PDAnnotationMarkup) ann;
                float[][] inkList = markup.getInkList();
                if (inkList == null || inkList.length == 0) {
                    continue;
                }

                int color = Color.BLUE;
                PDColor pdColor = markup.getColor();
                if (pdColor != null && pdColor.getComponents() != null) {
                    float[] c = pdColor.getComponents();
                    if (c.length >= 3) {
                        int r = Math.min(255, Math.max(0, (int) (c[0] * 255f)));
                        int g = Math.min(255, Math.max(0, (int) (c[1] * 255f)));
                        int b = Math.min(255, Math.max(0, (int) (c[2] * 255f)));
                        color = Color.rgb(r, g, b);
                    }
                }

                float width = 2f;
                PDBorderStyleDictionary border = markup.getBorderStyle();
                if (border != null) {
                    width = border.getWidth();
                }

                for (int s = 0; s < inkList.length; s++) {
                    float[] path = inkList[s];
                    if (path == null || path.length < 4) {
                        continue;
                    }

                    PdfRenderedInkAnnotation item = new PdfRenderedInkAnnotation();
                    item.colorArgb = color;
                    item.strokeWidthPdf = width;

                    for (int p = 0; p + 1 < path.length; p += 2) {
                        float x = path[p];
                        float y = path[p + 1];
                        item.pointsPdf.add(new PointF(x, y));
                    }

                    if (item.isValid()) {
                        result.add(item);
                    }
                }
            }

        } finally {
            if (document != null) {
                document.close();
            }
        }

        return result;
    }
}