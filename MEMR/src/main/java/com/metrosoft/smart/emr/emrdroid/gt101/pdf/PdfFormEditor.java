package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.cos.COSDictionary;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * PDF Form 처리 유틸 클래스
 *
 * 기능:
 * 1. PDF에 AcroForm이 없으면 생성
 * 2. 한글 폰트(TTF)를 PDF Form 기본 폰트로 등록
 * 3. 새 텍스트 필드(TextBox) 생성
 * 4. 새 체크박스 필드(CheckBox) 생성
 * 5. 기존/신규 필드에 값 채우기
 * 6. 사인 이미지를 PDF 페이지에 삽입
 * 7. 필요 시 flatten 처리
 *
 * 주의:
 * - assets/fonts/NotoSansKR-Regular.ttf 파일이 존재해야 함
 * - PDF 좌표계는 좌측 하단이 원점임
 */
public class PdfFormEditor {

    private static final String TAG = "PdfFormEditor";

    /** PDF 내부에서 사용할 폰트 리소스 이름 */
    private static final String FONT_RESOURCE_NAME = "F1";

    /** assets 기준 폰트 경로 */
    private static final String FONT_ASSET_PATH = "fonts/NotoSansKR-Regular.ttf";

    /** 기본 폰트 크기 */
    private static final int DEFAULT_FONT_SIZE = 10;

    /** 기본 appearance 문자열 */
    private static final String DEFAULT_DA = "/" + FONT_RESOURCE_NAME + " " + DEFAULT_FONT_SIZE + " Tf 0 g";

    /**
     * PDF 전체 처리 메인 함수
     *
     * @param context Android Context
     * @param srcPdf 원본 PDF
     * @param outPdf 결과 PDF
     * @param fieldsToCreate 새로 생성할 필드 목록
     * @param valuesToFill 필드명 -> 값
     * @param signatures 사인 이미지 목록
     * @param flattenAfterSave true이면 저장 전 flatten
     * @param listener 오류/디버그 메시지 수신
     */
    public static void prepareAndFillPdf(
            Context context,
            File srcPdf,
            File outPdf,
            List<PdfFormTextFieldSpec> fieldsToCreate,
            Map<String, String> valuesToFill,
            List<PdfSignatureSpec> signatures,
            boolean flattenAfterSave,
            PdfErrorListener listener
    ) throws Exception {

        PDFBoxResourceLoader.init(context);

        PDDocument document = null;
        try {
            document = PDDocument.load(srcPdf);

            PDAcroForm acroForm = getOrCreateAcroForm(context, document);

            // 1. 새 필드 생성
            if (fieldsToCreate != null) {
                for (int i = 0; i < fieldsToCreate.size(); i++) {
                    PdfFormTextFieldSpec spec = fieldsToCreate.get(i);
                    if (spec == null) continue;

                    String typeName = spec.typeName == null ? "" : spec.typeName.trim();
                    if ("checkbox".equalsIgnoreCase(typeName)) {
                        addCheckBoxField(document, acroForm, spec, listener);
                    } else {
                        addTextField(document, acroForm, spec, listener);
                    }
                }
            }

            // 2. 기존/신규 필드 값 채우기
            if (valuesToFill != null) {
                fillFieldValues(acroForm, valuesToFill, listener);
            }

            // 3. 사인 이미지 삽입
            if (signatures != null) {
                for (int i = 0; i < signatures.size(); i++) {
                    drawSignature(document, signatures.get(i));
                }
            }

            // 4. flatten 처리
            if (flattenAfterSave) {
                try {
                    acroForm.flatten();
                } catch (Exception ex) {
                    Log.e(TAG, "flatten error: " + ex.getMessage(), ex);
                }
            }

            // 결과 저장
            document.save(outPdf);

        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * AcroForm 가져오기 또는 생성
     */
    private static PDAcroForm getOrCreateAcroForm(
            Context context,
            PDDocument document
    ) throws Exception {

        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
        }

        PDResources dr = acroForm.getDefaultResources();
        if (dr == null) {
            dr = new PDResources();
            acroForm.setDefaultResources(dr);
        }

        InputStream fontStream = null;
        try {
            fontStream = context.getAssets().open(FONT_ASSET_PATH);
            PDType0Font font = PDType0Font.load(document, fontStream, true);
            dr.put(COSName.getPDFName(FONT_RESOURCE_NAME), font);
        } finally {
            try {
                if (fontStream != null) fontStream.close();
            } catch (Exception ignore) {
            }
        }

        acroForm.setDefaultAppearance(DEFAULT_DA);

        try {
            acroForm.setNeedAppearances(true);
        } catch (Exception ignore) {
        }

        return acroForm;
    }

    /**
     * 새 텍스트 필드 생성
     */
    private static void addTextField(
            PDDocument document,
            PDAcroForm acroForm,
            PdfFormTextFieldSpec spec,
            PdfErrorListener listener
    ) throws Exception {

        if (spec == null) return;
        if (spec.fieldName == null || "".equals(spec.fieldName.trim())) return;
        if (spec.pageNo < 0 || spec.pageNo >= document.getNumberOfPages()) return;

        PDField existing = acroForm.getField(spec.fieldName);
        if (existing != null) {
            return;
        }

        PDPage page = document.getPage(spec.pageNo);
        float pageHeight = page.getMediaBox().getHeight();

        // PDF 좌표는 좌측 하단 기준
        float pdfX = spec.x;
        float pdfY = pageHeight - spec.y - spec.height;

        PDTextField textField = new PDTextField(acroForm);
        textField.setPartialName(spec.fieldName);

        String value = getSpecValue(spec);

        int fontSize = spec.fontSize <= 0 ? DEFAULT_FONT_SIZE : spec.fontSize;
        String fieldDA = "/" + FONT_RESOURCE_NAME + " " + fontSize + " Tf 0 g";

        textField.setDefaultAppearance(fieldDA);
        textField.setDefaultValue(value);
        textField.setValue(value);

        PDAnnotationWidget widget;
        if (textField.getWidgets() != null && textField.getWidgets().size() > 0) {
            widget = textField.getWidgets().get(0);
        } else {
            widget = new PDAnnotationWidget();
            textField.getWidgets().add(widget);
        }

        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(pdfX);
        rect.setLowerLeftY(pdfY);
        rect.setUpperRightX(pdfX + spec.width);
        rect.setUpperRightY(pdfY + spec.height);
        widget.setRectangle(rect);
        widget.setPage(page);

        // 최소한의 appearance dictionary
        try {
            PDAppearanceCharacteristicsDictionary appearance =
                    new PDAppearanceCharacteristicsDictionary(new COSDictionary());
            widget.setAppearanceCharacteristics(appearance);
        } catch (Exception ignore) {
        }

        // 테두리
        try {
            PDBorderStyleDictionary border = new PDBorderStyleDictionary();
            border.setWidth(1);
            widget.setBorderStyle(border);
        } catch (Exception ignore) {
        }

        try {
            widget.getCOSObject().setString(COSName.DA, fieldDA);
        } catch (Exception ignore) {
        }

        page.getAnnotations().add(widget);
        acroForm.getFields().add(textField);

        /*
        if (listener != null) {
            listener.onError("text field added"
                    + ", fieldName=" + spec.fieldName
                    + ", pageNo=" + spec.pageNo
                    + ", pdfX=" + pdfX
                    + ", pdfY=" + pdfY
                    + ", width=" + spec.width
                    + ", height=" + spec.height
                    + ", value=" + value);
        }
        */
    }

    /**
     * 새 체크박스 필드 생성
     */
    private static void addCheckBoxField(
            PDDocument document,
            PDAcroForm acroForm,
            PdfFormTextFieldSpec spec,
            PdfErrorListener listener
    ) throws Exception {

        if (spec == null) return;
        if (spec.fieldName == null || "".equals(spec.fieldName.trim())) return;
        if (spec.pageNo < 0 || spec.pageNo >= document.getNumberOfPages()) return;

        PDField existing = acroForm.getField(spec.fieldName);
        if (existing != null) {
            return;
        }

        PDPage page = document.getPage(spec.pageNo);
        float pageHeight = page.getMediaBox().getHeight();

        float pdfX = spec.x;
        float pdfY = pageHeight - spec.y - spec.height;

        PDCheckBox checkBox = new PDCheckBox(acroForm);
        checkBox.setPartialName(spec.fieldName);

        PDAnnotationWidget widget;
        if (checkBox.getWidgets() != null && checkBox.getWidgets().size() > 0) {
            widget = checkBox.getWidgets().get(0);
        } else {
            widget = new PDAnnotationWidget();
            checkBox.getWidgets().add(widget);
        }

        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(pdfX);
        rect.setLowerLeftY(pdfY);
        rect.setUpperRightX(pdfX + spec.width);
        rect.setUpperRightY(pdfY + spec.height);
        widget.setRectangle(rect);
        widget.setPage(page);

        try {
            PDAppearanceCharacteristicsDictionary appearance =
                    new PDAppearanceCharacteristicsDictionary(new COSDictionary());
            widget.setAppearanceCharacteristics(appearance);
        } catch (Exception ignore) {
        }

        try {
            PDBorderStyleDictionary border = new PDBorderStyleDictionary();
            border.setWidth(1);
            widget.setBorderStyle(border);
        } catch (Exception ignore) {
        }

        try {
            widget.getCOSObject().setString(COSName.DA, DEFAULT_DA);
        } catch (Exception ignore) {
        }

        page.getAnnotations().add(widget);
        acroForm.getFields().add(checkBox);

        String value = getSpecValue(spec);
        setCheckBoxValue(checkBox, value);

        /*
        if (listener != null) {
            listener.onError("checkbox field added"
                    + ", fieldName=" + spec.fieldName
                    + ", pageNo=" + spec.pageNo
                    + ", pdfX=" + pdfX
                    + ", pdfY=" + pdfY
                    + ", width=" + spec.width
                    + ", height=" + spec.height
                    + ", value=" + value);
        }
        */
    }

    /**
     * 기존/신규 필드에 값 채우기
     */
    private static void fillFieldValues(
            PDAcroForm acroForm,
            Map<String, String> values,
            PdfErrorListener listener
    ) throws Exception {

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();

            PDField field = acroForm.getField(fieldName);
            if (field == null) {
                continue;
            }

            try {
                field.getCOSObject().setString(COSName.DA, DEFAULT_DA);
            } catch (Exception ignore) {
            }

            try {
                String currentDA = field.getCOSObject().getString(COSName.DA);
                Log.d(TAG, "fillFieldValues field=" + fieldName + ", DA=" + currentDA);
            } catch (Exception ignore) {
            }

            if (field instanceof PDCheckBox) {
                setCheckBoxValue((PDCheckBox) field, fieldValue);
            } else {
                field.setValue(fieldValue == null ? "" : fieldValue);
            }

            /*
            if (listener != null) {
                listener.onError("field filled"
                        + ", fieldName=" + fieldName
                        + ", value=" + (fieldValue == null ? "" : fieldValue)
                        + ", type=" + field.getClass().getSimpleName());
            }
            */
        }
    }

    /**
     * 체크박스 값 설정
     */
    private static void setCheckBoxValue(PDCheckBox checkBox, String value) throws Exception {
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

    /**
     * true 계열 문자열 판별
     */
    private static boolean isTrueValue(String value) {
        if (value == null) return false;

        String v = value.trim();
        return "true".equalsIgnoreCase(v)
                || "yes".equalsIgnoreCase(v)
                || "on".equalsIgnoreCase(v)
                || "1".equalsIgnoreCase(v)
                || "y".equalsIgnoreCase(v);
    }

    /**
     * spec에서 실제 입력값을 구한다.
     * defaultValue 우선, 없으면 value 사용
     */
    private static String getSpecValue(PdfFormTextFieldSpec spec) {
        if (spec == null) return "";
        if (spec.defaultValue != null) return spec.defaultValue;
        if (spec.value != null) return spec.value;
        return "";
    }

    /**
     * 사인 이미지를 PDF 페이지에 삽입
     */
    private static void drawSignature(
            PDDocument document,
            PdfSignatureSpec spec
    ) throws Exception {

        if (spec == null) return;
        if (spec.bitmap == null || spec.bitmap.isRecycled()) return;
        if (spec.pageIndex < 0 || spec.pageIndex >= document.getNumberOfPages()) return;

        PDPage page = document.getPage(spec.pageIndex);

        PDImageXObject imageXObject = LosslessFactory.createFromImage(document, spec.bitmap);

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
                    spec.x,
                    spec.y,
                    spec.width,
                    spec.height
            );

        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (Exception ignore) {
                }
            }
        }
    }
}