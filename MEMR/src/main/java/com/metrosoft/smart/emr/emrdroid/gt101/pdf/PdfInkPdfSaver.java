package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.RectF;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDNonTerminalField;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdfInkPdfSaver {

    private static final COSName MS_FIELD_TYPE = COSName.getPDFName("MS_FIELD_TYPE");

    public static void saveAllPages(Context context, File srcPdf, File outPdf, PdfInkSignView view, PdfDebugListener listener) throws IOException {

        PDFBoxResourceLoader.init(context);

        PDDocument document = null;
        try {
            document = PDDocument.load(srcPdf);

            // 1. 페이지별 펜 stroke / 서명 저장
            HashMap<Integer, ArrayList<PdfInkStroke>> allPageStrokes = view.getAllPageStrokes();
            HashMap<Integer, HashMap<String, PdfSignOverlay>> allPageSigns =
                    view.getAllPageSignOverlays();

            // 2. 사용자가 수정한 form field 값(text / checkbox)을 먼저 반영
            saveEditedFormFields(document, view, allPageSigns, listener);


            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                PDPage page = document.getPage(pageIndex);

                ArrayList<PdfInkStroke> strokes = allPageStrokes.get(pageIndex);
                if (strokes != null && !strokes.isEmpty()) {
                    savePageInkAnnotations(document, page, strokes);
                }

                HashMap<String, PdfSignOverlay> signMap = allPageSigns.get(pageIndex);
                if (signMap != null && signMap.size() > 0) {
                    for (String key : signMap.keySet()) {
                        PdfSignOverlay sign = signMap.get(key);

                        if (sign == null) continue;
                        if (!sign.visible) continue;
                        if (sign.bitmap == null || sign.bitmap.isRecycled()) continue;
                        if (sign.pdfRect == null) continue;

                        savePageSignImage(document, page, sign);
                    }
                }
            }

            document.save(outPdf);
        } finally {
            if (document != null) {
                document.close();
            }
        }
    }

    /**
     * 사용자가 수정한 AcroForm field 값을 PDF에 반영한다.
     * - checkbox : Yes / Off 계열로 저장
     * - text     : 문자열 그대로 저장
     */
    private static void saveEditedFormFields(
            PDDocument document,
            PdfInkSignView view,
            HashMap<Integer, HashMap<String, PdfSignOverlay>> allPageSigns,
            PdfDebugListener listener
    ) throws IOException {

        if (document == null || view == null) return;

        if (listener != null) listener.onError(" save - 101");

        PDAcroForm acroForm = null;
        try {
            if (document.getDocumentCatalog() != null) {
                acroForm = document.getDocumentCatalog().getAcroForm();
            }
        } catch (Exception ignore) {
        }

        if (acroForm == null) return;

        if (listener != null) listener.onError(" save - 102");

        Map<String, String> editedValues = view.getEditedFieldValues();

        if (listener != null) listener.onError(" save - 103");

        List<PDField> fields = acroForm.getFields();

        for (int i = 0; i < fields.size(); i++) {
            PDField field = fields.get(i);
            if (field == null) continue;

            String fieldName = field.getFullyQualifiedName();
            if (fieldName == null || "".equals(fieldName)) continue;

            String fieldValue = null;

            // 1. 사용자가 수정한 값 우선
            if (editedValues != null && editedValues.containsKey(fieldName)) {
                fieldValue = editedValues.get(fieldName);
            } else {
                // 2. 기존 PDF 값 사용 (기본값)
                try {
                    fieldValue = field.getValueAsString();
                } catch (Exception ignore) {
                }
            }

            try {
                if (field instanceof PDCheckBox) {
                    setCheckBoxValue((PDCheckBox) field, fieldValue);

                } else if (isCustomType(field, "sign") || isCustomType(field, "sign_image")) {
                    // sign_image는 value 대신 custom에 저장
                    field.getCOSObject().setString(
                            COSName.getPDFName("MS_SIGN_IMAGE_VALUE"),
                            fieldValue == null ? "" : fieldValue
                    );

                } else if ("signed".equalsIgnoreCase(fieldValue)) {
                    // skip

                } else {
                    field.setValue(fieldValue == null ? "" : fieldValue);
                }

            } catch (Exception ignore) {
            }
        }

        if (listener != null) listener.onError(" save - 104");
        try {
            // 다른 뷰어e가 자체적으로 다시 그리지 않고, 우리가 만든 appearance를 사용하게 한다.
            acroForm.setNeedAppearances(false);
        } catch (Exception ignore) {
        }

        if (listener != null) listener.onError(" save - 105");

        // 문서의 모든 form field를 readonly 처리
        setAllFieldsReadOnly(acroForm, allPageSigns);

        if (listener != null) listener.onError(" save - 106");
    }

    private static void savePageInkAnnotations(PDDocument document, PDPage page, List<PdfInkStroke> strokes) throws IOException {

        for (int i = 0; i < strokes.size(); i++) {
            PdfInkStroke stroke = strokes.get(i);
            if (stroke == null || !stroke.isValid()) continue;

            PDAnnotationMarkup annot = new PDAnnotationMarkup();
            annot.getCOSObject().setName(COSName.SUBTYPE, PDAnnotationMarkup.SUB_TYPE_INK);
            annot.setPrinted(true);
            annot.setConstantOpacity(1.0f);

            PDBorderStyleDictionary border = new PDBorderStyleDictionary();
            border.setWidth(stroke.strokeWidthPdf);
            annot.setBorderStyle(border);
            annot.setColor(toPdColor(stroke.colorArgb));

            RectF pdfBounds = stroke.getPdfBounds();

            PDRectangle rect = new PDRectangle();
            rect.setLowerLeftX(pdfBounds.left);
            rect.setLowerLeftY(pdfBounds.bottom);
            rect.setUpperRightX(pdfBounds.right);
            rect.setUpperRightY(pdfBounds.top);
            annot.setRectangle(rect);

            float[][] inkList = new float[1][];
            inkList[0] = toPdfInkPath(stroke);
            annot.setInkList(inkList);

            annot.constructAppearances(document);
            page.getAnnotations().add(annot);
        }
    }

    private static void savePageSignImage(PDDocument document, PDPage page, PdfSignOverlay sign) throws IOException {

        RectF pdfRect = sign.pdfRect;
        PDImageXObject imageXObject = LosslessFactory.createFromImage(document, sign.bitmap);

        // PdfInkSignView의 pdfRect는 top > bottom 형태로 관리됨.
        // PDFBox drawImage()는 left, lowerY, width, height 기준이므로
        // 반드시 bottom을 Y 좌표로 사용하고 높이는 top-bottom으로 계산한다.
        float x = Math.min(pdfRect.left, pdfRect.right);
        float y = Math.min(pdfRect.top, pdfRect.bottom);
        float w = Math.abs(pdfRect.right - pdfRect.left);
        float h = Math.abs(pdfRect.top - pdfRect.bottom);

        PDPageContentStream cs = null;
        try {
            cs = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            );

            cs.drawImage(
                    imageXObject,
                    x,
                    y,
                    w,
                    h
            );
        } finally {
            if (cs != null) cs.close();
        }
    }

    private static float[] toPdfInkPath(PdfInkStroke stroke) {
        float[] arr = new float[stroke.pointsPdf.size() * 2];

        for (int i = 0; i < stroke.pointsPdf.size(); i++) {
            PointF p = stroke.pointsPdf.get(i);
            arr[i * 2] = p.x;
            arr[i * 2 + 1] = p.y;
        }

        return arr;
    }

    /**
     * checkbox 값을 PDF에 반영한다.
     * true 계열이면 check(), 아니면 unCheck() 시도.
     * 실패 시 setValue("Yes"/"Off")로 한 번 더 시도.
     */
    private static void setCheckBoxValue(PDCheckBox checkBox, String value) throws IOException {
        if (checkBox == null) return;

        if (isTrueValue(value)) {
            try {
                checkBox.check();
            } catch (Exception e) {
                try {
                    checkBox.setValue("Yes");
                } catch (Exception ignore) {
                }
            }
        } else {
            try {
                checkBox.unCheck();
            } catch (Exception e) {
                try {
                    checkBox.setValue("Off");
                } catch (Exception ignore) {
                }
            }
        }
    }

    private static boolean isTrueValue(String value) {
        if (value == null) return false;

        String v = value.trim();
        return "true".equalsIgnoreCase(v)
                || "yes".equalsIgnoreCase(v)
                || "on".equalsIgnoreCase(v)
                || "1".equalsIgnoreCase(v)
                || "y".equalsIgnoreCase(v);
    }

    private static PDColor toPdColor(int colorArgb) {
        float[] rgb = new float[] {
                Color.red(colorArgb) / 255f,
                Color.green(colorArgb) / 255f,
                Color.blue(colorArgb) / 255f
        };
        return new PDColor(rgb, PDDeviceRGB.INSTANCE);
    }

    private static void setAllFieldsReadOnly(
            PDAcroForm acroForm,
            HashMap<Integer, HashMap<String, PdfSignOverlay>> allPageSigns
    ) {
        if (acroForm == null) return;

        try {
            List<PDField> fields = acroForm.getFields();
            if (fields == null) return;

            for (int i = 0; i < fields.size(); i++) {
                setFieldAndChildrenReadOnly(fields.get(i), true, allPageSigns);
            }
        } catch (Exception ignore) {
        }
    }

    private static void setFieldAndChildrenReadOnly(
            PDField field,
            boolean readOnly,
            HashMap<Integer, HashMap<String, PdfSignOverlay>> allPageSigns
    ) {
        if (field == null) return;

        boolean applyReadOnly = readOnly;

        if (readOnly && isCustomType(field, "sign")) {
            if (hasSignOverlayForField(field, allPageSigns)) {
                // 이번 저장에서 실제 싸인이 들어간 sign 필드
                applyReadOnly = true;
            } else {
                // 이번 저장 overlay에는 없지만, 기존 저장 PDF에서 이미 readonly였을 수 있음
                // 이 경우 readonly를 풀면 안 됨
                applyReadOnly = isFieldReadOnly(field);
            }
        }
        try {
            field.setReadOnly(applyReadOnly);
        } catch (Exception ignore) {
        }

        if (field instanceof PDNonTerminalField) {
            try {
                List<PDField> children = ((PDNonTerminalField) field).getChildren();
                if (children != null) {
                    for (int i = 0; i < children.size(); i++) {
                        setFieldAndChildrenReadOnly(children.get(i), readOnly, allPageSigns);
                    }
                }
            } catch (Exception ignore) {
            }
        }
    }


    private static boolean isCustomType(PDField field, String typeName) {
        if (field == null || typeName == null) return false;

        try {
            String type = field.getCOSObject().getNameAsString(MS_FIELD_TYPE);
            return typeName.equalsIgnoreCase(type);
        } catch (Exception ignore) {
        }

        return false;
    }

    private static boolean hasSignOverlayForField(
            PDField field,
            HashMap<Integer, HashMap<String, PdfSignOverlay>> allPageSigns
    ) {
        if (field == null || allPageSigns == null) return false;

        String fieldName = "";
        try {
            fieldName = field.getFullyQualifiedName();
        } catch (Exception ignore) {
        }

        if (fieldName == null || "".equals(fieldName.trim())) {
            try {
                fieldName = field.getPartialName();
            } catch (Exception ignore) {
            }
        }

        if (fieldName == null || "".equals(fieldName.trim())) return false;

        for (Integer pageIndex : allPageSigns.keySet()) {
            HashMap<String, PdfSignOverlay> signMap = allPageSigns.get(pageIndex);
            if (signMap == null) continue;

            PdfSignOverlay overlay = signMap.get(fieldName);
            if (overlay == null) continue;
            if (!overlay.visible) continue;
            if (overlay.bitmap == null || overlay.bitmap.isRecycled()) continue;

            return true;
        }

        return false;
    }

    private static boolean isFieldReadOnly(PDField field) {
        if (field == null) return false;

        try {
            if (field.isReadOnly()) return true;
        } catch (Exception ignore) {
        }

        try {
            int ff = field.getCOSObject().getInt(COSName.FF, 0);
            return (ff & 1) != 0;
        } catch (Exception ignore) {
        }

        return false;
    }

}