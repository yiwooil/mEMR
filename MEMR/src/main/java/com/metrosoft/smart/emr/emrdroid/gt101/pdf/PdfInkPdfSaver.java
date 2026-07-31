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
import com.tom_roush.pdfbox.pdmodel.PDResources;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDType0Font;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDColor;
import com.tom_roush.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
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
    //private static final float PDF_TEXT_FONT_SCALE = 0.95f;
    private static final float PDF_TEXT_FONT_SCALE = 1.0f;

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
                         *
                         * sign 영역을 먼저 아주 연한 노란색으로 채운 뒤,
                         * 그 위에 실제 서명 path를 저장한다.
                         *
                         * 주의:
                         * - sign 배경은 서명된 sign에 대해서만 굳혀서 저장한다.
                         * - 미서명 sign은 metadata로 다시 남기므로,
                         *   다음 조회 시 PdfInkSignView가 화면에서 배경을 다시 그리게 한다.
                         * - 미서명 sign까지 PDF 본문에 배경을 계속 저장하면
                         *   임시저장을 반복할 때 노란색이 누적되어 진해질 수 있다.
                         */
                        if (hasVectorSign(sign)) {
                            drawSignFieldBackground(document, page, sign.pdfRect);
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
     * autoFit=false:
     * - 기존처럼 한 줄로 출력한다.
     *
     * autoFit=true:
     * - field 박스 오른쪽을 벗어나면 자동 줄바꿈한다.
     * - \n, \r\n, \r 문자는 강제 줄바꿈으로 처리한다.
     * - 다음 줄이 field 박스 아래쪽을 벗어나면 출력을 중단한다.
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

        /*
         * normalizeRect()는 PDF 좌표계 기준으로
         * left <= right, top >= bottom 형태를 만든다.
         *
         * Android RectF.height()는 bottom - top이라서 음수가 될 수 있으므로
         * 여기서는 top/bottom/width/height를 직접 계산해서 사용한다.
         */
        RectF r = normalizeRect(field.pdfRect);

        float left = Math.min(r.left, r.right);
        float right = Math.max(r.left, r.right);
        float top = Math.max(r.top, r.bottom);
        float bottom = Math.min(r.top, r.bottom);

        float boxWidth = right - left;
        float boxHeight = top - bottom;

        if (boxWidth <= 0f || boxHeight <= 0f) return;

        float fontSize = field.fontSizePdf <= 0 ? DEFAULT_FONT_SIZE : field.fontSizePdf;

        /*
         * Android 화면 표시보다 PDF 저장 글자가 약간 크고 진하게 보이는 현상을 줄이기 위한 보정값.
         *
         * PDFBox + PDF Viewer 렌더링은 Android Canvas보다 글자가 조금 크게/진하게 보일 수 있다.
         * 실제 양식 기준으로 0.90f ~ 0.95f 사이에서 조정한다.
         */
        fontSize *= PDF_TEXT_FONT_SCALE;

        if (field.autoFit) {
            drawAutoFitTextValue(
                    document,
                    page,
                    left,
                    right,
                    top,
                    bottom,
                    safe(value),
                    font,
                    fontSize
            );
            return;
        }

        /*
         * 기존 한 줄 출력.
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

        float centerY = bottom + (boxHeight / 2f);
        float baselineY = centerY - ((ascent + descent) / 2f);

        /*
         * 현재 양식에서 화면 overlay와 저장 PDF의 텍스트 baseline 차이를 맞추기 위한 보정값.
         */
        float yAdjust = 2f;
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
            cs.newLineAtOffset(left + 2f, baselineY);
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
     * autoFit=true인 text/label 값을 PDF field 박스 안에 여러 줄로 저장한다.
     *
     * PDF 좌표계:
     * - 원점: 페이지 좌하단
     * - Y: 위쪽 증가
     *
     * 입력된 top/bottom:
     * - top은 field의 위쪽 Y
     * - bottom은 field의 아래쪽 Y
     */
    private static void drawAutoFitTextValue(
            PDDocument document,
            PDPage page,
            float left,
            float right,
            float top,
            float bottom,
            String value,
            PDType0Font font,
            float fontSize
    ) throws IOException {

        if (document == null || page == null || font == null) return;
        if (value == null) value = "";

        /*
         * field 내부 여백.
         */
        float paddingX = 2f;
        float paddingTop = 0f;
        float paddingBottom = 0f;

        float maxWidth = (right - left) - (paddingX * 2f);
        float maxHeight = (top - bottom) - paddingTop - paddingBottom;

        if (maxWidth <= 0f || maxHeight <= 0f) {
            return;
        }

        float ascent;
        float descent;

        try {
            ascent = font.getFontDescriptor().getAscent() / 1000f * fontSize;
            descent = font.getFontDescriptor().getDescent() / 1000f * fontSize;
        } catch (Exception ignore) {
            ascent = fontSize * 0.8f;
            descent = -fontSize * 0.2f;
        }

        /*
         * autoFit은 여러 줄 출력이므로 기존 중앙 정렬 baseline이 아니라
         * field 상단부터 아래 방향으로 출력한다.
         *
         * PDF에서 baseline은 글자의 top이 아니다.
         * 첫 줄 baseline = field top - paddingTop - ascent.
         */
        float yAdjust = 2f;
        float baselineY = top - paddingTop - ascent + yAdjust;

        /*
         * PDF 좌표계에서는 아래쪽으로 내려갈수록 Y가 작아진다.
         * 글자의 아래쪽은 baseline + descent이다.
         * descent는 보통 음수이므로, 이 값이 bottom + paddingBottom보다 작아지면 박스를 벗어난다.
         */
        float minBaselineY = bottom + paddingBottom - descent;

        /*
         * 줄 간격.
         * PDF 좌표에서는 다음 줄로 내려갈 때 baselineY에서 lineHeight를 뺀다.
         */
        float lineHeight = (ascent - descent) * 1.05f;
        if (lineHeight <= 0f) {
            lineHeight = fontSize * 1.2f;
        }

        ArrayList<String> lines = buildAutoFitLines(value, font, fontSize, maxWidth);

        PDPageContentStream cs = null;

        try {
            cs = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            );

            cs.setFont(font, fontSize);
            cs.setNonStrokingColor(0, 0, 0);

            for (int i = 0; i < lines.size(); i++) {
                /*
                 * baseline이 허용 범위보다 아래로 내려가면 출력 중단.
                 */
                if (baselineY < minBaselineY) {
                    break;
                }

                String line = lines.get(i);

                /*
                 * 빈 줄은 줄 높이만 차지하고 출력하지 않는다.
                 */
                if (!"".equals(line)) {
                    cs.beginText();
                    cs.newLineAtOffset(left + paddingX, baselineY);
                    cs.showText(line);
                    cs.endText();
                }

                baselineY -= lineHeight;
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
     * autoFit용 줄 목록을 만든다.
     *
     * 처리 규칙:
     * - \r\n, \r은 \n으로 통일한다.
     * - \n은 강제 줄바꿈이다.
     * - 한 문단이 maxWidth를 넘으면 문자 단위로 자동 줄바꿈한다.
     *
     * 문자 단위 줄바꿈을 사용하는 이유:
     * - 한글 의료 문서는 공백이 적은 문자열이 많다.
     * - 단어 단위 줄바꿈만 사용하면 박스를 벗어나는 경우가 생긴다.
     */
    private static ArrayList<String> buildAutoFitLines(
            String text,
            PDType0Font font,
            float fontSize,
            float maxWidth
    ) throws IOException {

        ArrayList<String> result = new ArrayList<String>();

        if (text == null) {
            result.add("");
            return result;
        }

        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n", -1);

        for (int p = 0; p < paragraphs.length; p++) {
            String paragraph = paragraphs[p];

            /*
             * 엔터로 생긴 빈 줄도 한 줄로 유지한다.
             */
            if ("".equals(paragraph)) {
                result.add("");
                continue;
            }

            ArrayList<String> wrapped = wrapParagraphByWidth(
                    paragraph,
                    font,
                    fontSize,
                    maxWidth
            );

            result.addAll(wrapped);
        }

        return result;
    }

    /**
     * 한 문단을 maxWidth 안에 들어가도록 문자 단위로 나눈다.
     */
    private static ArrayList<String> wrapParagraphByWidth(
            String paragraph,
            PDType0Font font,
            float fontSize,
            float maxWidth
    ) throws IOException {

        ArrayList<String> result = new ArrayList<String>();

        if (paragraph == null || "".equals(paragraph)) {
            result.add("");
            return result;
        }

        StringBuilder line = new StringBuilder();

        for (int i = 0; i < paragraph.length(); i++) {
            char ch = paragraph.charAt(i);

            String candidate = line.toString() + ch;

            if (getTextWidth(font, fontSize, candidate) <= maxWidth) {
                line.append(ch);
                continue;
            }

            /*
             * 현재 글자를 붙이면 maxWidth를 넘는다.
             * 기존 line을 먼저 확정한다.
             */
            if (line.length() > 0) {
                result.add(line.toString());
                line.setLength(0);
            }

            /*
             * 한 글자 자체가 maxWidth보다 큰 경우도 무한루프 방지를 위해 한 줄로 넣는다.
             */
            String single = String.valueOf(ch);
            if (getTextWidth(font, fontSize, single) > maxWidth) {
                result.add(single);
            } else {
                line.append(ch);
            }
        }

        if (line.length() > 0) {
            result.add(line.toString());
        }

        return result;
    }

    /**
     * PDFBox font 기준 문자열 폭을 PDF 좌표 단위로 계산한다.
     */
    private static float getTextWidth(
            PDType0Font font,
            float fontSize,
            String text
    ) throws IOException {

        if (font == null || text == null || "".equals(text)) {
            return 0f;
        }

        /*
         * PDFBox getStringWidth()는 1000 unit 기준 폭을 반환한다.
         */
        return font.getStringWidth(text) / 1000f * fontSize;
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

        RectF nr = normalizeRect(field.pdfRect);

        // checkbox 버튼을 왼쪽(상단)에 붙이기 위한 용도
        RectF r = new RectF();
        r.left = nr.left;
        r.right = nr.left + Math.min(Math.abs(nr.width()), Math.abs(nr.height()));
        r.top = nr.bottom + Math.min(Math.abs(nr.width()), Math.abs(nr.height()));
        r.bottom = nr.bottom;

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

            float w = r.width();
            float h = r.height();

            /*
             * 체크표시 V 모양:
             * - start : 왼쪽 중간쯤
             * - mid   : 가장 아래
             * - end   : 오른쪽 위
             *
             * PDF 좌표계는 Y가 위로 증가하므로
             * midY가 startY, endY보다 작아야 V 모양이 된다.
             */
            float startX = r.left + w * 0.18f;
            float startY = r.top + h * 0.55f;

            float midX = r.left + w * 0.42f;
            float midY = r.top + h * 0.78f;

            float endX = r.left + w * 0.82f;
            float endY = r.top + h * 0.22f;

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

        RectF nr = normalizeRect(field.pdfRect);

        // radio 버튼을 왼쪽(상단)에 붙이기 위한 용도
        RectF r = new RectF();
        r.left = nr.left;
        r.right = nr.left + Math.min(Math.abs(nr.width()), Math.abs(nr.height()));
        r.top = nr.bottom + Math.min(Math.abs(nr.width()), Math.abs(nr.height()));
        r.bottom = nr.bottom;

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

            /* 바깥 테두리는 그리지 말자..
            cs.setStrokingColor(0, 0, 0);
            cs.setLineWidth(1f);

            drawCircle(cs, cx, cy, radius);
            cs.stroke();
             */

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
     * sign 필드 영역을 아주 연한 노란색으로 채운다.
     *
     * 목적:
     * - 저장된 PDF에서도 이 영역이 서명 영역이라는 것을 약하게 표시한다.
     *
     * 색상 정책:
     * - 노란색이 있다는 정도만 보이도록 alpha를 낮게 준다.
     * - 기존 PDF 양식의 선/글자를 완전히 가리지 않도록 투명도를 적용한다.
     *
     * 주의:
     * - 이 함수는 PDF page content에 직접 그린다.
     * - 같은 위치에 여러 번 저장하면 색이 누적될 수 있으므로,
     *   보통 "서명된 sign" 저장 직전에만 호출하는 것이 안전하다.
     */
    private static void drawSignFieldBackground(
            PDDocument document,
            PDPage page,
            RectF pdfRect
    ) throws IOException {

        if (document == null || page == null || pdfRect == null) return;

        RectF r = normalizeRect(pdfRect);

        /*
         * normalizeRect() 결과는 PDF 좌표계 기준으로
         * top >= bottom 형태가 될 수 있다.
         *
         * Android RectF.height()는 bottom - top이므로 사용하지 않고
         * 직접 width / height를 계산한다.
         */
        float left = Math.min(r.left, r.right);
        float right = Math.max(r.left, r.right);
        float top = Math.max(r.top, r.bottom);
        float bottom = Math.min(r.top, r.bottom);

        float width = right - left;
        float height = top - bottom;

        if (width <= 0f || height <= 0f) return;

        PDPageContentStream cs = null;

        try {
            cs = new PDPageContentStream(
                    document,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            );

            /*
             * 기존 PDF 내용이 완전히 가려지지 않도록 투명도 적용.
             */
            PDResources resources = page.getResources();
            if (resources == null) {
                resources = new PDResources();
                page.setResources(resources);
            }

            PDExtendedGraphicsState gs = new PDExtendedGraphicsState();

            /*
             * 0.0f = 완전 투명
             * 1.0f = 완전 불투명
             *
             * 0.12f ~ 0.18f 정도가 "노란색이 있구나" 수준이다.
             */
            gs.setNonStrokingAlphaConstant(0.15f);

            cs.saveGraphicsState();
            cs.setGraphicsStateParameters(gs);

            /*
             * 연한 노란색.
             * alpha는 위의 ExtGState에서 처리한다.
             */
            cs.setNonStrokingColor(255, 235, 80);

            cs.addRect(left, bottom, width, height);
            cs.fill();

            cs.restoreGraphicsState();

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
        obj.put("autoFit", field.autoFit);

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