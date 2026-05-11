package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PointF;
import android.graphics.RectF;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationMarkup;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * PDF 저장 클래스.
 *
 * 중요:
 * - AcroForm field를 전혀 사용하지 않는다.
 * - PdfInkSignView가 화면에 overlay로 표시하던 모든 값을 저장 시 PDF 본문에 직접 그린다.
 * - sign 필드 중 사용자가 서명한 것은 PDF 본문에 vector/image로 저장한다.
 * - sign 필드 중 사용자가 서명하지 않은 것은 metadata에 남겨서 다음에 다시 입력 가능하게 한다.
 *
 * 전체 흐름:
 *
 * PdfFormEditor
 *   → 모든 필드 정보를 MS_OVERLAY_FIELDS metadata에 저장
 *   → PDF 본문에는 값을 그리지 않음
 *
 * PdfInkSignView
 *   → metadata를 읽어 overlay로 표시
 *   → 사용자가 값 수정
 *
 * PdfInkPdfSaver
 *   → 저장 시 모든 overlay 값을 PDF 본문에 굳힘
 *   → 미서명 sign만 metadata에 다시 남김
 */
public class PdfInkPdfSaver {

    /**
     * PdfFormEditor / PdfFormFieldReader와 동일한 metadata key.
     *
     * 저장 후 다시 열었을 때 아직 서명하지 않은 sign field를 복원하기 위해 사용한다.
     */
    private static final COSName MS_OVERLAY_FIELDS =
            COSName.getPDFName("MS_OVERLAY_FIELDS");

    /**
     * PDF 저장 시 한글 출력을 위한 폰트.
     *
     * assets/fonts/NotoSansKR-Regular.ttf 위치에 있어야 한다.
     */
    private static final String FONT_ASSET_PATH = "fonts/NotoSansKR-Regular.ttf";

    private static final float DEFAULT_FONT_SIZE = 10f;

    /**
     * 전체 페이지 저장.
     *
     * 처리 순서:
     * 1. 현재 PdfInkSignView 상태 가져오기
     * 2. sign이 아닌 모든 field를 PDF 본문에 직접 그림
     * 3. 펜 stroke를 PDF Ink Annotation으로 저장
     * 4. sign overlay를 PDF 본문에 vector/image로 저장
     * 5. 미서명 sign field만 metadata에 다시 저장
     * 6. PDF 저장
     */
    public static void saveAllPages(
            Context context,
            File srcPdf,
            File outPdf,
            PdfInkSignView view,
            PdfDebugListener listener
    ) throws IOException {

        PDFBoxResourceLoader.init(context);

        PDDocument document = null;
        InputStream fontStream = null;

        try {
            document = PDDocument.load(srcPdf);

            PDType0Font font = null;

            try {
                fontStream = context.getAssets().open(FONT_ASSET_PATH);
                font = PDType0Font.load(document, fontStream, true);
            } catch (Exception ex) {
                if (listener != null) {
                    listener.onError("font load error=" + ex.getMessage());
                }
            }

            if (listener != null) listener.onError("save fixed overlay - 101");

            /*
             * 현재 View 상태를 가져온다.
             *
             * getAllPageStrokes(), getAllPageSignOverlays() 내부에서
             * 현재 페이지 overlay도 saveCurrentPageOverlay()를 통해 반영된다.
             */
            HashMap<Integer, ArrayList<PdfInkStroke>> allPageStrokes =
                    view.getAllPageStrokes();

            HashMap<Integer, HashMap<String, PdfSignOverlay>> allPageSigns =
                    view.getAllPageSignOverlays();

            /*
             * PdfInkSignView에 아래 함수가 있어야 한다.
             *
             * public List<PdfRenderedFormField> getRenderedFormFieldsSnapshot()
             */
            List<PdfRenderedFormField> renderedFields =
                    view.getRenderedFormFieldsSnapshot();

            if (listener != null) listener.onError("save fixed overlay - 102");

            /*
             * 1. sign이 아닌 모든 field 값을 PDF에 굳혀서 그림.
             *
             * text, label, checkbox, radio, sign_image 등은 여기서 저장한다.
             * sign은 savePageSignVector()/savePageSignImage() 단계에서 저장한다.
             */
            bakeAllNonSignFields(
                    context,
                    document,
                    renderedFields,
                    font,
                    listener
            );

            if (listener != null) listener.onError("save fixed overlay - 103");

            /*
             * 2. page별 펜 stroke와 sign overlay 저장.
             */
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                PDPage page = document.getPage(pageIndex);

                /*
                 * 펜 stroke 저장.
                 */
                ArrayList<PdfInkStroke> strokes = allPageStrokes.get(pageIndex);
                if (strokes != null && !strokes.isEmpty()) {
                    savePageInkAnnotations(document, page, strokes);
                }

                /*
                 * sign overlay 저장.
                 */
                HashMap<String, PdfSignOverlay> signMap = allPageSigns.get(pageIndex);
                if (signMap != null && signMap.size() > 0) {
                    for (String key : signMap.keySet()) {
                        PdfSignOverlay sign = signMap.get(key);

                        if (sign == null) continue;
                        if (!sign.visible) continue;
                        if (sign.pdfRect == null) continue;


                        /*
                         * 사용자가 손으로 입력한 sign 저장.
                         */
                        if (hasVectorSign(sign)) {
                            savePageSignVector(document, page, sign);
                        }
                    }
                }

            }

            if (listener != null) listener.onError("save fixed overlay - 104");

            /*
             * 3. 미서명 sign 필드만 metadata에 다시 저장.
             *
             * 저장 후 다시 동의서를 열 때는 이 metadata에 남은 sign만 다시 편집 가능하다.
             */
            updatePendingSignMetadata(
                    document,
                    renderedFields,
                    allPageSigns,
                    listener
            );

            if (listener != null) listener.onError("save fixed overlay - 105");

            document.save(outPdf);

            if (listener != null) listener.onError("save fixed overlay - 106");

        } finally {
            if (fontStream != null) {
                try {
                    fontStream.close();
                } catch (Exception ignore) {
                }
            }

            if (document != null) {
                try {
                    document.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * sign이 아닌 모든 field 값을 PDF page content에 직접 그린다.
     *
     * 저장 대상:
     * - text
     * - label
     * - checkbox
     * - radio
     * - sign_image
     * - combo/listbox/choice/button 등 기타 text 계열
     *
     * 제외:
     * - sign
     *
     * 주의:
     * - PdfFormEditor가 PDF 본문에 값을 미리 그리지 않아야 이중 출력이 발생하지 않는다.
     */
    private static void bakeAllNonSignFields(
            Context context,
            PDDocument document,
            List<PdfRenderedFormField> fields,
            PDType0Font font,
            PdfDebugListener listener
    ) throws IOException {

        if (document == null || fields == null) return;

        for (int i = 0; i < fields.size(); i++) {
            PdfRenderedFormField field = fields.get(i);
            if (field == null || field.pdfRect == null) continue;

            if (field.pageIndex < 0 || field.pageIndex >= document.getNumberOfPages()) {
                continue;
            }

            String type = safe(field.type);
            if ("".equals(type)) type = "label";

            /*
             * sign은 별도 저장 단계에서 처리한다.
             * 서명하지 않은 sign은 metadata에 남기고, 서명한 sign은 vector/image로 저장한다.
             */
            if ("sign".equalsIgnoreCase(type)) {
                continue;
            }

            PDPage page = document.getPage(field.pageIndex);

            if ("checkbox".equalsIgnoreCase(type)) {
                drawCheckBoxValue(document, page, field, field.value);

            } else if ("radio".equalsIgnoreCase(type)) {
                drawRadioValue(document, page, field, field.value);

            } else if ("sign_image".equalsIgnoreCase(type)) {
                drawSignImageValue(context, document, page, field, field.value);

            } else {
                /*
                 * text / label / combo / listbox / choice / button / 기타는 text로 저장한다.
                 */
                drawTextValue(document, page, field, field.value, font);
            }

            if (listener != null) {
                listener.onError("baked field"
                        + ", type=" + type
                        + ", name=" + safe(field.name)
                        + ", ccfField=" + safe(field.ccfField)
                        + ", value=" + safe(field.value));
            }
        }
    }

    /**
     * text/label 값을 PDF에 직접 그린다.
     *
     * PdfFormEditor가 값을 PDF에 미리 그리지 않는 구조이므로,
     * 여기서는 흰색으로 지우는 처리를 하지 않는다.
     */
    private static void drawTextValue(
            PDDocument document,
            PDPage page,
            PdfRenderedFormField field,
            String value,
            PDType0Font font
    ) throws IOException {

        if (document == null || page == null || field == null || field.pdfRect == null) return;
        if (font == null) return;

        RectF r = normalizeRect(field.pdfRect);

        float fontSize = field.fontSizePdf <= 0 ? DEFAULT_FONT_SIZE : field.fontSizePdf;

        /*
         * PDFBox의 text y 좌표는 baseline이다.
         * field 영역 중앙에 글자가 오도록 ascent/descent 기준으로 baseline을 계산한다.
         */
        float ascent;
        float descent;

        try {
            ascent = font.getFontDescriptor().getAscent() / 1000f * fontSize;
            descent = font.getFontDescriptor().getDescent() / 1000f * fontSize;
        } catch (Exception ignore) {
            ascent = fontSize * 0.8f;
            descent = -fontSize * 0.2f;
        }

        float centerY = r.bottom + (r.height() / 2f);
        float baselineY = centerY - ((ascent + descent) / 2f);

        /*
         * 화면 표시(PdfInkSignView)와 PDF 저장(PdfInkPdfSaver)의 텍스트 위치 보정값.
         *
         * PdfInkSignView는 Android Canvas + Android Paint.FontMetrics 기준으로 그린다.
         * PdfInkPdfSaver는 PDFBox + PDType0Font font descriptor 기준으로 그린다.
         *
         * 두 클래스가 같은 NotoSansKR-Regular.ttf를 사용하더라도
         * Android 렌더링과 PDFBox 렌더링의 baseline/metric 차이가 있어서
         * 실제 저장 PDF에서 위치가 다르게 보일 수 있다.
         *
         * 테스트 결과 현재 양식에서는 16f 보정이 화면 overlay 위치와 가장 잘 맞는다.
         *
         * 주의:
         * - 이 값은 PDF 좌표 단위이다.
         * - 값이 커질수록 저장된 PDF의 글자가 위로 올라간다.
         * - 값이 작아질수록 저장된 PDF의 글자가 아래로 내려간다.
         */
        float yAdjust = 16f;
        baselineY += yAdjust;

        PDPageContentStream cs = null;

        try {
            cs = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            );

            cs.beginText();
            cs.setFont(font, fontSize);
            cs.setNonStrokingColor(0, 0, 0);
            cs.newLineAtOffset(r.left + 2f, baselineY);
            cs.showText(safe(value));
            cs.endText();

        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * checkbox 값을 PDF에 직접 그린다.
     *
     * 현재 정책:
     * - 체크된 경우 체크 모양만 그림
     * - 체크되지 않은 경우 아무것도 그리지 않음
     */
    private static void drawCheckBoxValue(
            PDDocument document,
            PDPage page,
            PdfRenderedFormField field,
            String value
    ) throws IOException {

        if (document == null || page == null || field == null || field.pdfRect == null) return;

        if (!isTrueValue(value)) {
            return;
        }

        RectF r = normalizeRect(field.pdfRect);

        PDPageContentStream cs = null;

        try {
            cs = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            );

            cs.setStrokingColor(0, 0, 0);
            cs.setLineWidth(Math.max(1.2f, Math.min(r.width(), r.height()) * 0.12f));

            /*
             * 체크표시 V 모양:
             * - start : 왼쪽 중간쯤
             * - mid   : 가장 아래
             * - end   : 오른쪽 위
             *
             * PDF 좌표계는 Y가 위로 증가하므로
             * midY가 startY, endY보다 작아야 V 모양이 된다.
             */
            float startX = r.left + r.width() * 0.18f;
            float startY = r.top + r.height() * 0.55f;

            float midX = r.left + r.width() * 0.42f;
            float midY = r.top + r.height() * 0.78f;

            float endX = r.left + r.width() * 0.82f;
            float endY = r.top + r.height() * 0.22f;

            cs.moveTo(startX, startY);
            cs.lineTo(midX, midY);
            cs.lineTo(endX, endY);
            cs.stroke();

        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (Exception ignore) {
                }
            }
        }
    }


    /**
     * radio 값을 PDF에 직접 그린다.
     *
     * 정책:
     * - 미선택이어도 빈 동그라미를 그림
     * - 선택이면 속을 채움
     */
    private static void drawRadioValue(
            PDDocument document,
            PDPage page,
            PdfRenderedFormField field,
            String value
    ) throws IOException {

        if (document == null || page == null || field == null || field.pdfRect == null) return;

        RectF r = normalizeRect(field.pdfRect);

        float size = Math.min(r.width(), r.height());
        float cx = r.left + r.width() / 2f;
        float cy = r.top + r.height() / 2f;
        float radius = size / 2f - 1f;
        if (radius < 1f) radius = size / 2f;

        PDPageContentStream cs = null;

        try {
            cs = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            );

            cs.setStrokingColor(0, 0, 0);
            cs.setLineWidth(1f);

            drawCircle(cs, cx, cy, radius);
            cs.stroke();

            if (isTrueValue(value)) {
                cs.setNonStrokingColor(0, 0, 0);
                drawCircle(cs, cx, cy, radius / 2f);
                cs.fill();
            }

        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * sign_image 값을 PDF에 이미지로 직접 그린다.
     *
     * field.value 예:
     * - sign_AA10011
     * - logindrsign_AA10011
     * - login_sign_AA10011
     *
     * 위 값에서 의사 ID를 추출한 뒤:
     * /data/data/.../files/Sign/{의사ID}
     * 파일을 읽어 PDF에 삽입한다.
     */
    private static void drawSignImageValue(
            Context context,
            PDDocument document,
            PDPage page,
            PdfRenderedFormField field,
            String value
    ) throws IOException {

        if (context == null || document == null || page == null || field == null) return;
        if (field.pdfRect == null) return;

        String drid = getSignDridFromValue(value);
        if ("".equals(drid)) return;

        String dstDir = context.getFilesDir().getAbsolutePath();
        String pathName = dstDir + File.separator + "Sign" + File.separator + drid;

        Bitmap bitmap = BitmapFactory.decodeFile(pathName);
        if (bitmap == null || bitmap.isRecycled()) return;

        try {
            Bitmap transparent = makeTransparent(bitmap);
            bitmap = null;

            if (transparent == null || transparent.isRecycled()) return;

            RectF r = normalizeRect(field.pdfRect);

            PDImageXObject imageXObject =
                    LosslessFactory.createFromImage(document, transparent);

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
                        r.left,
                        r.top,
                        r.width(),
                        r.height()
                );

            } finally {
                if (cs != null) {
                    try {
                        cs.close();
                    } catch (Exception ignore) {
                    }
                }

                if (!transparent.isRecycled()) {
                    transparent.recycle();
                }
            }

        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    /**
     * 펜 stroke를 PDF Ink Annotation으로 저장한다.
     *
     * 기존 펜 주석 저장 방식은 유지한다.
     */
    private static void savePageInkAnnotations(
            PDDocument document,
            PDPage page,
            List<PdfInkStroke> strokes
    ) throws IOException {

        if (document == null || page == null || strokes == null) return;

        for (int i = 0; i < strokes.size(); i++) {
            PdfInkStroke stroke = strokes.get(i);
            if (stroke == null || !stroke.isValid()) continue;

            PDAnnotationMarkup annot = new PDAnnotationMarkup();

            /*
             * PDFBox Android 버전에 따라 상수가 다를 수 있으므로 COSName으로 직접 설정한다.
             */
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

            try {
                annot.constructAppearances(document);
            } catch (Exception ignore) {
                /*
                 * 일부 PDFBox Android 버전에서는 appearance 생성이 실패할 수 있다.
                 * annotation 자체는 저장되므로 무시한다.
                 */
            }

            page.getAnnotations().add(annot);
        }
    }

    /**
     * vector sign 저장.
     *
     * 전제:
     * - PdfInkSignView.setSignToField()에서
     *   sign.paths는 이미 PDF 좌표계로 변환되어 있어야 한다.
     */
    private static void savePageSignVector(
            PDDocument document,
            PDPage page,
            PdfSignOverlay sign
    ) throws IOException {

        if (document == null || page == null || sign == null) return;
        if (!hasVectorSign(sign)) return;

        PDPageContentStream cs = null;

        try {
            cs = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            );

            cs.setStrokingColor(
                    Color.red(sign.strokeColor),
                    Color.green(sign.strokeColor),
                    Color.blue(sign.strokeColor)
            );

            cs.setLineWidth(sign.strokeWidth);

            for (int i = 0; i < sign.paths.size(); i++) {
                Path path = sign.paths.get(i);
                if (path == null) continue;

                drawAndroidPathAsPdfPath(cs, path);
            }

            cs.stroke();

        } finally {
            if (cs != null) {
                try {
                    cs.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Android Path를 PDF path 명령으로 변환한다.
     *
     * Android Path를 직접 PDF에 넣는 API가 없으므로 PathMeasure로 샘플링한다.
     */
    private static void drawAndroidPathAsPdfPath(
            PDPageContentStream cs,
            Path path
    ) throws IOException {

        if (cs == null || path == null) return;

        PathMeasure measure = new PathMeasure(path, false);
        float[] pos = new float[2];

        do {
            float length = measure.getLength();

            if (length <= 0f) {
                continue;
            }

            /*
             * 샘플링 간격.
             * 작을수록 원본과 비슷하지만 PDF 용량이 증가한다.
             */
            float sampleStep = 2.0f;
            boolean firstPointInContour = true;

            for (float distance = 0f; distance <= length; distance += sampleStep) {
                if (!measure.getPosTan(distance, pos, null)) continue;

                float x = pos[0];
                float y = pos[1];

                if (firstPointInContour) {
                    cs.moveTo(x, y);
                    firstPointInContour = false;
                } else {
                    cs.lineTo(x, y);
                }
            }

            /*
             * 마지막 점 보정.
             */
            if (measure.getPosTan(length, pos, null)) {
                if (firstPointInContour) {
                    cs.moveTo(pos[0], pos[1]);
                } else {
                    cs.lineTo(pos[0], pos[1]);
                }
            }

        } while (measure.nextContour());
    }


    /**
     * 사용자가 서명하지 않은 sign field만 metadata에 다시 저장한다.
     *
     * 저장 후 다시 PDF를 열면:
     * - metadata에 남은 sign field만 PdfFormFieldReader가 읽음
     * - PdfInkSignView에서 다시 서명 가능
     */
    private static void updatePendingSignMetadata(
            PDDocument document,
            List<PdfRenderedFormField> renderedFields,
            HashMap<Integer, HashMap<String, PdfSignOverlay>> allPageSigns,
            PdfDebugListener listener
    ) {

        if (document == null || renderedFields == null) return;

        JSONArray pendingArray = new JSONArray();

        try {
            for (int i = 0; i < renderedFields.size(); i++) {
                PdfRenderedFormField field = renderedFields.get(i);
                if (field == null || field.pdfRect == null) continue;

                /*
                 * 저장 후 다시 입력 가능해야 하는 것은 미서명 sign뿐이다.
                 */
                if (!"sign".equalsIgnoreCase(safe(field.type))) {
                    continue;
                }

                /*
                 * 이번 저장에서 sign overlay가 있으면 이미 PDF에 그렸으므로
                 * metadata에서 제거한다.
                 */
                if (hasSignOverlayForField(field, allPageSigns)) {
                    continue;
                }

                /*
                 * 아직 서명하지 않은 sign만 metadata에 유지한다.
                 */
                JSONObject obj = toMetadataJson(document, field, true);
                pendingArray.put(obj);
            }

            document.getDocumentCatalog().getCOSObject().setString(
                    MS_OVERLAY_FIELDS,
                    pendingArray.toString()
            );

            if (listener != null) {
                listener.onError("pending sign metadata count=" + pendingArray.length());
            }

        } catch (Exception ex) {
            if (listener != null) {
                listener.onError("pending sign metadata error=" + ex.getMessage());
            }
        }
    }

    /**
     * PdfRenderedFormField를 MS_OVERLAY_FIELDS JSON 한 건으로 변환한다.
     *
     * PdfRenderedFormField.pdfRect는 PDF 좌표계다.
     * metadata의 x/y는 PdfFormFieldSpec과 동일하게 좌상단 기준으로 저장한다.
     */
    private static JSONObject toMetadataJson(
            PDDocument document,
            PdfRenderedFormField field,
            boolean editable
    ) throws Exception {

        JSONObject obj = new JSONObject();

        RectF r = normalizeRect(field.pdfRect);

        PDPage page = document.getPage(field.pageIndex);
        float pageHeight = page.getMediaBox().getHeight();

        /*
         * PDF 좌표계 RectF → CCF 좌상단 기준 좌표로 변환.
         *
         * 주의:
         * - PdfInkSignView/PdfInkPdfSaver에서 PDF rect는 관례적으로
         *   top > bottom 형태로 관리한다.
         *
         * - Android RectF.height()는 bottom - top을 반환한다.
         *   따라서 top > bottom인 PDF rect에 대해 r.height()를 쓰면
         *   음수 height가 나온다.
         *
         * - metadata에는 CCF 원래 값과 동일하게 width/height를 양수로 저장해야 한다.
         */
        float left = Math.min(r.left, r.right);
        float right = Math.max(r.left, r.right);
        float top = Math.max(r.top, r.bottom);
        float bottom = Math.min(r.top, r.bottom);

        float x = left;
        float y = pageHeight - top;
        float w = right - left;
        float h = top - bottom;

        obj.put("pageNo", field.pageIndex);
        obj.put("fieldName", safe(field.name));
        obj.put("ccfField", safe(field.ccfField));
        obj.put("typeName", safe(field.type));
        obj.put("groupName", safe(field.groupName));
        obj.put("value", "");

        obj.put("x", x);
        obj.put("y", y);
        obj.put("width", w);
        obj.put("height", h);
        obj.put("fontSize", field.fontSizePdf <= 0 ? DEFAULT_FONT_SIZE : field.fontSizePdf);

        obj.put("editable", editable);
        obj.put("pendingSign", editable && "sign".equalsIgnoreCase(safe(field.type)));

        return obj;
    }

    /**
     * 특정 sign field에 대해 이번 저장 시 실제 sign overlay가 있는지 확인한다.
     */
    private static boolean hasSignOverlayForField(
            PdfRenderedFormField field,
            HashMap<Integer, HashMap<String, PdfSignOverlay>> allPageSigns
    ) {

        if (field == null || allPageSigns == null) return false;

        String key = getSignFieldKey(field);
        if ("".equals(key)) return false;

        HashMap<String, PdfSignOverlay> signMap = allPageSigns.get(field.pageIndex);
        if (signMap == null) return false;

        PdfSignOverlay overlay = signMap.get(key);
        if (overlay == null) return false;
        if (!overlay.visible) return false;

        return hasVectorSign(overlay);
    }

    /**
     * PdfInkSignView.getSignFieldKey()와 동일한 규칙을 사용해야 한다.
     */
    private static String getSignFieldKey(PdfRenderedFormField field) {
        if (field == null) return "";

        String name = safe(field.name).trim();
        if (!"".equals(name)) return name;

        if (field.pdfRect == null) return "";

        return "rect_"
                + Math.round(field.pdfRect.left) + "_"
                + Math.round(field.pdfRect.top) + "_"
                + Math.round(field.pdfRect.right) + "_"
                + Math.round(field.pdfRect.bottom);
    }

    private static String getSignDridFromValue(String value) {
        if (value == null) return "";

        String v = value.trim();

        if (v.startsWith("sign_")) {
            return v.substring(5);
        }

        if (v.startsWith("logindrsign_")) {
            return v.substring(12);
        }

        if (v.startsWith("login_sign_")) {
            return v.substring(11);
        }

        return "";
    }

    /**
     * 흰색 배경을 투명하게 만든다.
     */
    private static Bitmap makeTransparent(Bitmap bm) {
        if (bm == null) return null;

        int width = bm.getWidth();
        int height = bm.getHeight();

        Bitmap out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];

        bm.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] == Color.WHITE) {
                pixels[i] = Color.TRANSPARENT;
            }
        }

        out.setPixels(pixels, 0, width, 0, 0, width, height);

        if (!bm.isRecycled()) {
            bm.recycle();
        }

        return out;
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

    private static PDColor toPdColor(int colorArgb) {
        float[] rgb = new float[]{
                Color.red(colorArgb) / 255f,
                Color.green(colorArgb) / 255f,
                Color.blue(colorArgb) / 255f
        };

        return new PDColor(rgb, PDDeviceRGB.INSTANCE);
    }

    private static boolean hasVectorSign(PdfSignOverlay sign) {
        return sign != null
                && sign.paths != null
                && sign.paths.size() > 0;
    }


    private static boolean isTrueValue(String value) {
        if (value == null) return false;

        String v = value.trim();

        return "true".equalsIgnoreCase(v)
                || "yes".equalsIgnoreCase(v)
                || "on".equalsIgnoreCase(v)
                || "1".equalsIgnoreCase(v)
                || "y".equalsIgnoreCase(v)
                || "selected".equalsIgnoreCase(v)
                || "checked".equalsIgnoreCase(v);
    }

    /**
     * RectF를 PDF 저장에 쓰기 좋게 정규화한다.
     *
     * 결과:
     * - left <= right
     * - bottom <= top
     */
    private static RectF normalizeRect(RectF src) {
        RectF r = new RectF();

        if (src == null) return r;

        r.left = Math.min(src.left, src.right);
        r.right = Math.max(src.left, src.right);
        r.bottom = Math.min(src.top, src.bottom);
        r.top = Math.max(src.top, src.bottom);

        return r;
    }

    /**
     * PDF content stream에 원을 그린다.
     *
     * PDFBox에는 Android Canvas처럼 drawCircle이 없으므로
     * Bezier curve 네 개로 원을 근사한다.
     */
    private static void drawCircle(
            PDPageContentStream cs,
            float cx,
            float cy,
            float r
    ) throws IOException {

        float c = 0.552284749831f;
        float ox = r * c;
        float oy = r * c;

        cs.moveTo(cx + r, cy);
        cs.curveTo(cx + r, cy + oy, cx + ox, cy + r, cx, cy + r);
        cs.curveTo(cx - ox, cy + r, cx - r, cy + oy, cx - r, cy);
        cs.curveTo(cx - r, cy - oy, cx - ox, cy - r, cx, cy - r);
        cs.curveTo(cx + ox, cy - r, cx + r, cy - oy, cx + r, cy);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}