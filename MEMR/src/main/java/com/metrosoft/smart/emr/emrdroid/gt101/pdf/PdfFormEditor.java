package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.cos.COSArray;
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
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
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
            if (listener != null) listener.onError(" 101");
            if (fieldsToCreate != null) {
                for (int i = 0; i < fieldsToCreate.size(); i++) {
                    PdfFormTextFieldSpec spec = fieldsToCreate.get(i);
                    if (spec == null) continue;

                    if (listener != null) listener.onError(" 101" + ", fieldName=" + spec.fieldName + ", typeName=" + spec.typeName);
                    String typeName = spec.typeName == null ? "" : spec.typeName.trim();
                    if ("checkbox".equalsIgnoreCase(typeName)) {
                        addCheckBoxField(document, acroForm, spec, listener);
                    } else {
                        addTextField(document, acroForm, spec, listener);
                    }
                }
            }

            // 2. 기존/신규 필드 값 채우기
            if (listener != null) listener.onError(" 102");
            if (valuesToFill != null) {
                fillFieldValues(acroForm, valuesToFill, listener);
            }

            // 3. 사인 이미지 삽입
            if (listener != null) listener.onError(" 103");
            if (signatures != null) {
                for (int i = 0; i < signatures.size(); i++) {
                    drawSignature(document, signatures.get(i));
                }
            }

            // 4. flatten 처리
            if (listener != null) listener.onError(" 104");
            if (flattenAfterSave) {
                try {
                    acroForm.flatten();
                } catch (Exception ex) {
                    Log.e(TAG, "flatten error: " + ex.getMessage(), ex);
                }
            }

            // 결과 저장
            if (listener != null) listener.onError(" 105");
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

        if (listener != null) listener.onError(" a01");
        PDPage page = document.getPage(spec.pageNo);

        if (listener != null) listener.onError(" a02");
        float pageHeight = page.getMediaBox().getHeight();

        float pdfX = spec.x;
        float pdfY = pageHeight - spec.y - spec.height;

        if (listener != null) listener.onError(" a03");
        PDTextField textField = new PDTextField(acroForm);
        textField.setPartialName(spec.fieldName);
        if (spec.readOnly) {
            textField.setReadOnly(true);
        }

        String value = getSpecValue(spec);

        int fontSize = spec.fontSize <= 0 ? DEFAULT_FONT_SIZE : spec.fontSize;
        String fieldDA = "/" + FONT_RESOURCE_NAME + " " + fontSize + " Tf 0 g";

        if (listener != null) listener.onError(" a04");
        textField.setDefaultAppearance(fieldDA);
        textField.setDefaultValue(value);

        if (listener != null) listener.onError(" a05");
        PDAnnotationWidget widget = new PDAnnotationWidget();

        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(pdfX);
        rect.setLowerLeftY(pdfY);
        rect.setUpperRightX(pdfX + spec.width);
        rect.setUpperRightY(pdfY + spec.height);

        widget.setRectangle(rect);
        widget.setPage(page);
        widget.setParent(textField);

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
            widget.getCOSObject().setString(COSName.DA, fieldDA);
        } catch (Exception ignore) {
        }

        if (listener != null) listener.onError(" a06");

        // widget 연결: high-level list 조작 대신 KIDS를 raw로 설정
        COSArray kids = new COSArray();
        kids.add(widget.getCOSObject());
        textField.getCOSObject().setItem(COSName.KIDS, kids);

        if (listener != null) listener.onError(" a07");

        // field를 AcroForm에 raw 추가
        addFieldToAcroFormRaw(acroForm, textField);

        if (listener != null) listener.onError(" a08");

        // page annotation에 widget 등록
        page.getAnnotations().add(widget);

        if (listener != null) listener.onError(" a09");

        // 마지막에 값 반영
        textField.setValue(value);

        if (listener != null) listener.onError(" a10");
    }

    /**
     * 새 체크박스 필드 생성
     * - 일반 PDF 뷰어(Edge, Acrobat 등)에서도 보이도록
     *   Off / On appearance stream을 직접 만든다.
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

        // 1. field 생성
        PDCheckBox checkBox = new PDCheckBox(acroForm);
        checkBox.setPartialName(spec.fieldName);
        if (spec.readOnly) {
            checkBox.setReadOnly(true);
        }

        // 2. widget 생성
        PDAnnotationWidget widget = new PDAnnotationWidget();

        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(pdfX);
        rect.setLowerLeftY(pdfY);
        rect.setUpperRightX(pdfX + spec.width);
        rect.setUpperRightY(pdfY + spec.height);

        widget.setRectangle(rect);
        widget.setPage(page);
        widget.setParent(checkBox);

        try {
            PDAppearanceCharacteristicsDictionary appearance =
                    new PDAppearanceCharacteristicsDictionary(new COSDictionary());
            widget.setAppearanceCharacteristics(appearance);
        } catch (Exception ignore) {
        }

        try {
            PDBorderStyleDictionary border = new PDBorderStyleDictionary();
            border.setWidth(0); // checkbox 테두리를 없앤다.
            widget.setBorderStyle(border);
        } catch (Exception ignore) {
        }

        // 3. appearance stream 생성
        // 일반 뷰어가 checkbox를 표시하려면 /AP(N) 안에 Off / On appearance가 있어야 한다.
        String onValue = "Yes"; // 기본값
        PDAppearanceDictionary ap = createCheckBoxAppearance(document, rect, onValue);
        widget.setAppearance(ap);

        // 4. field <-> widget 연결
        COSArray kids = new COSArray();
        kids.add(widget.getCOSObject());
        checkBox.getCOSObject().setItem(COSName.KIDS, kids);

        addFieldToAcroFormRaw(acroForm, checkBox);
        page.getAnnotations().add(widget);

        // 5. 실제 On 이름을 다시 확인
        // getOnValue()는 normal appearance keys를 보고 결정된다.
        try {
            String actualOnValue = checkBox.getOnValue();
            if (actualOnValue != null && actualOnValue.trim().length() > 0) {
                onValue = actualOnValue;
            }
        } catch (Exception ignore) {
        }

        // 6. 값 반영
        String value = getSpecValue(spec);
        setCheckBoxValue(checkBox, widget, value, onValue);
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

    private static void addFieldToAcroFormRaw(PDAcroForm acroForm, PDField field) {
        if (acroForm == null || field == null) return;

        COSArray fieldsArray = (COSArray) acroForm.getCOSObject().getDictionaryObject(COSName.FIELDS);
        if (fieldsArray == null) {
            fieldsArray = new COSArray();
            acroForm.getCOSObject().setItem(COSName.FIELDS, fieldsArray);
        }

        fieldsArray.add(field.getCOSObject());
    }

    /**
     * checkbox의 Off / On appearance를 만든다.
     */
    private static PDAppearanceDictionary createCheckBoxAppearance(
            PDDocument document,
            PDRectangle rect,
            String onValue
    ) throws Exception {

        PDAppearanceStream offStream = createCheckBoxAppearanceStream(document, rect, false);
        PDAppearanceStream onStream  = createCheckBoxAppearanceStream(document, rect, true);

        COSDictionary normalAppearances = new COSDictionary();
        normalAppearances.setItem(COSName.Off, offStream);
        normalAppearances.setItem(COSName.getPDFName(onValue), onStream);

        PDAppearanceDictionary ap = new PDAppearanceDictionary();
        ap.getCOSObject().setItem(COSName.N, normalAppearances);

        return ap;
    }

    /**
     * checkbox 1개의 appearance stream 생성
     * checked=false : 빈 박스
     * checked=true  : 체크 표시가 있는 박스
     */
    private static PDAppearanceStream createCheckBoxAppearanceStream(
            PDDocument document,
            PDRectangle rect,
            boolean checked
    ) throws Exception {

        PDAppearanceStream stream = new PDAppearanceStream(document);
        stream.setResources(new PDResources());

        PDRectangle bbox = new PDRectangle(rect.getWidth(), rect.getHeight());
        stream.setBBox(bbox);

        PDPageContentStream cs = null;
        try {
            cs = new PDPageContentStream(document, stream);

            float w = rect.getWidth();
            float h = rect.getHeight();

            // 배경 흰색
            cs.setNonStrokingColor(255, 255, 255);
            cs.addRect(0, 0, w, h);
            cs.fill();

            // 테두리 검정 ==> 테두리를 없앰.
            //cs.setStrokingColor(0, 0, 0);
            //cs.setLineWidth(1f);
            //cs.addRect(0.5f, 0.5f, w - 1f, h - 1f);
            //cs.stroke();

            // 체크 표시
            if (checked) {
                cs.setStrokingColor(0, 0, 0);
                cs.setLineWidth(Math.max(1.5f, Math.min(w, h) * 0.12f));

                float startX = w * 0.18f;
                float startY = h * 0.55f;

                float midX = w * 0.42f;
                float midY = h * 0.22f;

                float endX = w * 0.82f;
                float endY = h * 0.78f;

                cs.moveTo(startX, startY);
                cs.lineTo(midX, midY);
                cs.lineTo(endX, endY);
                cs.stroke();
            }

        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (Exception ignore) {
                }
            }
        }

        return stream;
    }

    /**
     * checkbox 값을 field + widget appearance state에 같이 반영한다.
     */
    private static void setCheckBoxValue(
            PDCheckBox checkBox,
            PDAnnotationWidget widget,
            String value,
            String onValue
    ) throws Exception {

        if (checkBox == null || widget == null) return;

        String onName = (onValue == null || "".equals(onValue.trim())) ? "Yes" : onValue;

        if (isTrueValue(value)) {
            try {
                checkBox.setValue(onName);
            } catch (Exception e) {
                try {
                    checkBox.check();
                } catch (Exception ignore) {
                }
            }
            try {
                widget.setAppearanceState(onName);
            } catch (Exception ignore) {
            }

        } else {
            try {
                checkBox.setValue("Off");
            } catch (Exception e) {
                try {
                    checkBox.unCheck();
                } catch (Exception ignore) {
                }
            }
            try {
                widget.setAppearanceState("Off");
            } catch (Exception ignore) {
            }
        }
    }

}