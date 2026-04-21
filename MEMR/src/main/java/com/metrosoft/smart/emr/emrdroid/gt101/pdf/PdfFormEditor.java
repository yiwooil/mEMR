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
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAppearanceCharacteristicsDictionary;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
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
 * 4. 기존/신규 필드에 값 채우기
 * 5. 사인 이미지를 PDF 페이지에 삽입
 * 6. 필요 시 flatten 처리
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
     * @param fieldsToCreate 새로 생성할 텍스트 필드 목록
     * @param valuesToFill 필드명 -> 값
     * @param signatures 사인 이미지 목록
     * @param flattenAfterSave true이면 저장 전 flatten
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

        // Android용 PDFBox 초기화
        PDFBoxResourceLoader.init(context);

        PDDocument document = null;
        try {
            // 원본 PDF 로드
            document = PDDocument.load(srcPdf);

            // AcroForm 생성 또는 가져오기 + 한글 폰트 등록
            PDAcroForm acroForm = getOrCreateAcroForm(context, document);

            // 1. 새 텍스트 필드 생성
            if (fieldsToCreate != null) {
                for (int i = 0; i < fieldsToCreate.size(); i++) {
                    addTextField(document, acroForm, fieldsToCreate.get(i), listener);
                }
            }

            // 2. 기존/신규 필드 값 채우기
            if (valuesToFill != null) {
                fillFieldValues(acroForm, valuesToFill);
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
                    Log.e(TAG, "flatten error: " + ex.getMessage());
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
     *
     * 중요:
     * - 기본 리소스에 한글 폰트(F1)를 항상 등록
     * - 기본 appearance를 /F1 10 Tf 0 g 로 강제
     * - NeedAppearances 설정
     */
    private static PDAcroForm getOrCreateAcroForm(
            Context context,
            PDDocument document
    ) throws Exception {

        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();

        // AcroForm이 없으면 생성
        if (acroForm == null) {
            acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
        }

        // 기본 리소스 가져오기 또는 생성
        PDResources dr = acroForm.getDefaultResources();
        if (dr == null) {
            dr = new PDResources();
            acroForm.setDefaultResources(dr);
        }

        // 한글 폰트를 항상 F1으로 등록
        InputStream fontStream = null;
        try {
            fontStream = context.getAssets().open(FONT_ASSET_PATH);
            PDType0Font font = PDType0Font.load(document, fontStream, true);
            dr.put(COSName.getPDFName(FONT_RESOURCE_NAME), font);
        } finally {
            try {
                if (fontStream != null) {
                    fontStream.close();
                }
            } catch (Exception ignore) {
            }
        }

        // AcroForm 기본 appearance를 강제로 한글 폰트로 지정
        acroForm.setDefaultAppearance(DEFAULT_DA);

        // 일부 PDF에서 appearance 재생성에 필요
        try {
            acroForm.setNeedAppearances(true);
        } catch (Exception ignore) {
        }

        return acroForm;
    }

    /**
     * 새 텍스트 필드 생성
     *
     * @param document PDF 문서
     * @param acroForm PDF Form
     * @param spec 생성할 텍스트 필드 정보
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


        // 이미 같은 이름의 필드가 있으면 중복 생성하지 않음
        PDField existing = acroForm.getField(spec.fieldName);
        if (existing != null) {
            return;
        }

        PDPage page = document.getPage(spec.pageNo);
        float pageHeight = page.getMediaBox().getHeight();

        // PDF좌표는 좌측 하단이 (0,0)임 죄표를 변환해야힘.
        float pdfX = spec.x;
        float pdfY = pageHeight - spec.y - spec.height;

        // 새 텍스트 필드 생성
        PDTextField textField = new PDTextField(acroForm);
        textField.setPartialName(spec.fieldName);

        String defaultValue = spec.defaultValue == null ? "" : spec.defaultValue;

        // 기본값 지정
        textField.setDefaultValue(defaultValue);

        // 필드 appearance를 한글 폰트로 지정
        int fontSize = spec.fontSize <= 0 ? DEFAULT_FONT_SIZE : spec.fontSize;
        String fieldDA = "/" + FONT_RESOURCE_NAME + " " + fontSize + " Tf 0 g";
        textField.setDefaultAppearance(fieldDA);

        // 실제 필드 값 설정
        textField.setValue(defaultValue);

        // 새 widget를 만들지 말고 textField가 가진 widget를 사용
        PDAnnotationWidget widget = textField.getWidgets().get(0);
        if (widget == null) {
            // 화면에 보이는 widget 생성
            widget = new PDAnnotationWidget();
            // appearance dictionary 설정
            PDAppearanceCharacteristicsDictionary appearance =
                    new PDAppearanceCharacteristicsDictionary(new COSDictionary());
            widget.setAppearanceCharacteristics(appearance);
        }

        // PDF 좌표로 영역 지정
        PDRectangle rect = new PDRectangle();
        rect.setLowerLeftX(pdfX);
        rect.setLowerLeftY(pdfY);
        rect.setUpperRightX(pdfX + spec.width);
        rect.setUpperRightY(pdfY + spec.height);
        widget.setRectangle(rect);
        widget.setPage(page);


        // widget 자체에도 DA 설정
        try {
            widget.getCOSObject().setString(COSName.DA, fieldDA);
        } catch (Exception ignore) {
        }

        // 연결
        textField.getWidgets().add(widget);
        page.getAnnotations().add(widget);
        acroForm.getFields().add(textField);

        // 연결되었는지 검사
        /*
        try {
            PDField addedField = acroForm.getField(spec.fieldName);

            String msg = "";
            if (addedField == null) {
                msg += "addedField = null -> 필드 추가 실패";
            } else {
                msg += "fieldName = " + spec.fieldName + ",";
                msg += "pageNo = " + spec.pageNo + ",";
                msg += "pdfX = " + pdfX + ", pdfY = " + pdfY + ",";
                msg += "width = " + spec.width + ", height = " + spec.height + ",";
                msg += "page annotation count = " + page.getAnnotations().size() + ",";
                msg += "acroForm field count = " + acroForm.getFields().size() + ",";

                msg += "addedField found = " + addedField.getFullyQualifiedName()+ ",";

                if (addedField.getWidgets() != null) {
                    msg += "widget count = " + addedField.getWidgets().size() + ",";
                    if (addedField.getWidgets().size() > 0) {
                        List<PDAnnotationWidget> widgets = addedField.getWidgets();
                        for (int wi = 0; wi < widgets.size(); wi++) {
                            PDAnnotationWidget w = widgets.get(wi);
                            PDRectangle r = w.getRectangle();
                            if (r == null) {
                                msg += "widget[" + wi + "] rect = null,";
                            } else {
                                msg += "widget[" + wi + "] rect = ["
                                        + r.getLowerLeftX() + ", "
                                        + r.getLowerLeftY() + ", "
                                        + r.getUpperRightX() + ", "
                                        + r.getUpperRightY() + "],";
                            }
                        }
                    }
                } else {
                    msg += "widget list = null" + ",";
                }
            }
            if (listener != null) listener.onError(msg);

        } catch (Exception ex) {
            Log.e(TAG, "addTextField verify error: " + ex.getMessage(), ex);
        }
        */
    }

    /**
     * 기존/신규 필드에 값 채우기
     *
     * 중요:
     * - 기존 PDF 필드가 ArialUnicodeMS 같은 폰트를 물고 있을 수 있음
     * - setValue 전에 DA를 /F1 ... 로 강제로 바꿔야 한글 오류를 피할 수 있음
     */
    private static void fillFieldValues(
            PDAcroForm acroForm,
            Map<String, String> values
    ) throws Exception {

        for (Map.Entry<String, String> entry : values.entrySet()) {
            String fieldName = entry.getKey();
            String fieldValue = entry.getValue();

            PDField field = acroForm.getField(fieldName);
            if (field != null) {

                // 기존 필드의 DA를 강제로 한글 폰트로 교체
                try {
                    field.getCOSObject().setString(COSName.DA, DEFAULT_DA);
                } catch (Exception ignore) {
                }

                // 디버그 필요 시 확인용
                try {
                    String currentDA = field.getCOSObject().getString(COSName.DA);
                    Log.d(TAG, "fillFieldValues field=" + fieldName + ", DA=" + currentDA);
                } catch (Exception ignore) {
                }

                // 값 설정
                field.setValue(fieldValue == null ? "" : fieldValue);
            }
        }
    }

    /**
     * 사인 이미지를 PDF 페이지에 삽입
     *
     * 주의:
     * - 이 함수는 annotation이 아니라 페이지 본문(content stream)에 이미지를 직접 그림
     */
    private static void drawSignature(
            PDDocument document,
            PdfSignatureSpec spec
    ) throws Exception {

        if (spec == null) return;
        if (spec.bitmap == null || spec.bitmap.isRecycled()) return;
        if (spec.pageIndex < 0 || spec.pageIndex >= document.getNumberOfPages()) return;

        PDPage page = document.getPage(spec.pageIndex);

        // Bitmap -> PDF 이미지 객체
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

            // 지정 좌표에 이미지 삽입
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