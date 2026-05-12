package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PDF 표시/편집용 필드 정보를 읽는 클래스.
 *
 * 중요:
 * - 이 클래스는 더 이상 AcroForm field를 읽지 않는다.
 * - PdfFormEditor가 PDF Catalog에 저장한 MS_OVERLAY_FIELDS metadata만 읽는다.
 *
 * 처리 구조:
 *
 * PdfFormEditor
 *   → text / checkbox / radio / sign / sign_image / label 정보를
 *     MS_OVERLAY_FIELDS JSON으로 PDF Catalog에 저장
 *
 * PdfFormFieldReader
 *   → MS_OVERLAY_FIELDS JSON을 읽음
 *   → PdfRenderedFormField 목록으로 변환
 *
 * PdfInkSignView
 *   → PdfRenderedFormField 목록을 화면에 그림
 *   → MODE_EDIT이면 파란색 테두리
 *   → MODE_PEN / MODE_ERASER이면 초록색 테두리
 */
public class PdfFormFieldReader {

    private static final String TAG = "PdfFormFieldReader";

    /**
     * PdfFormEditor에서 저장하는 metadata key.
     *
     * 저장 위치:
     *
     * document.getDocumentCatalog().getCOSObject().setString(
     *     MS_OVERLAY_FIELDS,
     *     metadataArray.toString()
     * );
     */
    private static final COSName MS_OVERLAY_FIELDS =
            COSName.getPDFName("MS_OVERLAY_FIELDS");

    private static final int DEFAULT_TEXT_COLOR = Color.BLACK;
    private static final float DEFAULT_FONT_SIZE = 10f;

    /**
     * 현재 페이지의 overlay field 목록을 읽는다.
     *
     * 기존 이름 readAllFields는 유지한다.
     * 이유:
     * - PdfInkSignView에서 이미 PdfFormFieldReader.readAllFields(...)를 호출하고 있기 때문
     * - 호출부 수정 없이 내부 구현만 metadata 방식으로 바꾼다.
     *
     * @param context Android Context
     * @param pdfFile 읽을 PDF 파일
     * @param pageIndex 현재 페이지 index, 0-based
     * @param values 필드 값
     * @param debugTextList 디버그 메시지 출력용 리스트. null 가능
     * @return PdfInkSignView에서 그릴 PdfRenderedFormField 목록
     */
    public static List<PdfRenderedFormField> readAllFields(
            Context context,
            File pdfFile,
            int pageIndex,
            Map<String, String> values,
            List<String> debugTextList
    ) throws Exception {

        PDFBoxResourceLoader.init(context);

        List<PdfRenderedFormField> result =
                new ArrayList<PdfRenderedFormField>();

        PDDocument document = null;

        try {
            safeAddDebug(debugTextList, "readAllFields metadata mode start");

            if (pdfFile == null || !pdfFile.exists()) {
                safeAddDebug(debugTextList, "pdfFile is null or not exists");
                return result;
            }

            document = PDDocument.load(pdfFile);

            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                safeAddDebug(debugTextList, "invalid pageIndex=" + pageIndex);
                return result;
            }

            /*
             * AcroForm은 읽지 않는다.
             * 모든 field 정보는 MS_OVERLAY_FIELDS metadata에서 복원한다.
             */
            result.addAll(readOverlayMetadataFields(
                    document,
                    pageIndex,
                    values,
                    debugTextList
            ));

            safeAddDebug(debugTextList, "metadata field result count=" + result.size());

        } catch (Exception ex) {
            safeAddDebug(debugTextList, "readAllFields metadata error=" + ex.getMessage());
            Log.d(TAG, "readAllFields error=" + ex.getMessage(), ex);
            throw ex;

        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (Exception ignore) {
                }
            }
        }

        return result;
    }

    /**
     * PDF Catalog에 저장된 MS_OVERLAY_FIELDS JSON을 읽어서
     * PdfRenderedFormField 목록으로 변환한다.
     *
     * JSON 예:
     *
     * [
     *   {
     *     "pageNo": 0,
     *     "fieldName": "drnm_001",
     *     "ccfField": "drnm",
     *     "typeName": "text",
     *     "groupName": "",
     *     "value": "홍길동",
     *     "x": 100,
     *     "y": 120,
     *     "width": 80,
     *     "height": 20,
     *     "fontSize": 10,
     *     "editable": true
     *   }
     * ]
     *
     * 좌표 변환:
     * - metadata의 x/y는 기존 CCF 좌표 기준이다.
     * - CCF 좌표는 좌상단 기준이다.
     * - PDF 좌표는 좌하단 기준이다.
     *
     * 따라서:
     * - left   = x
     * - right  = x + width
     * - top    = pageHeight - y
     * - bottom = pageHeight - y - height
     */
    private static List<PdfRenderedFormField> readOverlayMetadataFields(
            PDDocument document,
            int pageIndex,
            Map<String, String> values,
            List<String> debugTextList
    ) {

        List<PdfRenderedFormField> result =
                new ArrayList<PdfRenderedFormField>();

        try {
            if (document == null || document.getDocumentCatalog() == null) {
                safeAddDebug(debugTextList, "document or catalog is null");
                return result;
            }

            String json = document.getDocumentCatalog()
                    .getCOSObject()
                    .getString(MS_OVERLAY_FIELDS);

            if (json == null || "".equals(json.trim())) {
                safeAddDebug(debugTextList, "MS_OVERLAY_FIELDS metadata not found");
                return result;
            }

            JSONArray array = new JSONArray(json);

            safeAddDebug(debugTextList,
                    "MS_OVERLAY_FIELDS raw count=" + array.length());

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);

                PdfRenderedFormField field =
                        createRenderedFieldFromMetadata(
                                document,
                                obj,
                                pageIndex,
                                values,
                                debugTextList
                        );

                if (field != null && field.isValid()) {
                    result.add(field);
                }
            }

            safeAddDebug(debugTextList,
                    "MS_OVERLAY_FIELDS page field count=" + result.size());

        } catch (Exception ex) {
            safeAddDebug(debugTextList,
                    "MS_OVERLAY_FIELDS read error=" + ex.getMessage());
            Log.d(TAG, "readOverlayMetadataFields error=" + ex.getMessage(), ex);
        }

        return result;
    }

    /**
     * metadata JSON 한 건을 PdfRenderedFormField 한 건으로 변환한다.
     */
    private static PdfRenderedFormField createRenderedFieldFromMetadata(
            PDDocument document,
            JSONObject obj,
            int targetPageIndex,
            Map<String, String> values,
            List<String> debugTextList
    ) {

        try {
            int pageNo = obj.optInt("pageNo", 0);

            /*
             * 현재 PdfInkSignView는 페이지를 열 때마다 현재 pageIndex에 해당하는
             * field만 필요하므로 다른 페이지 정보는 제외한다.
             */
            if (pageNo != targetPageIndex) {
                return null;
            }

            if (pageNo < 0 || pageNo >= document.getNumberOfPages()) {
                safeAddDebug(debugTextList,
                        "metadata invalid pageNo=" + pageNo);
                return null;
            }

            float x = (float) obj.optDouble("x", 0);
            float y = (float) obj.optDouble("y", 0);
            float width = (float) obj.optDouble("width", 0);
            float height = (float) obj.optDouble("height", 0);

            if (width <= 0 || height <= 0) {
                safeAddDebug(debugTextList,
                        "metadata invalid rect, pageNo=" + pageNo
                                + ", x=" + x
                                + ", y=" + y
                                + ", w=" + width
                                + ", h=" + height);
                return null;
            }

            PdfRenderedFormField field = new PdfRenderedFormField();

            field.pageIndex = pageNo;

            /*
             * name:
             * - PDF 내부에서 구분하기 위한 고유 이름
             * - AcroForm field명은 아니지만 기존 코드 호환을 위해 name에 넣는다.
             */
            field.name = obj.optString("fieldName", "");

            /*
             * ccfField:
             * - 서버 CCF 기준 논리 필드명
             * - drnm, dptnm, pid, drsign 등
             */
            field.ccfField = obj.optString("ccfField", "");

            /*
             * type:
             * - text
             * - label
             * - checkbox
             * - radio
             * - sign
             * - sign_image
             */
            field.type = obj.optString("typeName", "label");

            if ("".equals(safe(field.type))) {
                field.type = "label";
            }
            field.autoFit = obj.optBoolean("autoFit", false);

            field.groupName = obj.optString("groupName", "");


            /*
             * 값은 metadata가 아니라 valuesToFill에서 가져온다.
             *
             * 우선순위:
             * 1. fieldName으로 찾기
             * 2. ccfField로 찾기
             * 3. 없으면 빈 값
             */
            String value = "";
            if (values != null) {
                if (field.ccfField != null && values.containsKey(field.ccfField)) {
                    value = values.get(field.ccfField);
                }
            }
            field.value = value == null ? "" : value;

            field.fontSizePdf =
                    (float) obj.optDouble("fontSize", DEFAULT_FONT_SIZE);

            if (field.fontSizePdf <= 0) {
                field.fontSizePdf = DEFAULT_FONT_SIZE;
            }

            field.colorArgb = DEFAULT_TEXT_COLOR;

            /*
             * editable:
             * - true이면 사용자가 터치/수정할 수 있다.
             * - false이면 화면 표시만 하고 편집은 하지 않는다.
             *
             * PdfInkSignView.handleTapField()는 readOnly를 기준으로
             * 편집창을 열지 말지 결정하므로 readOnly = !editable 로 설정한다.
             */
            field.editable = obj.optBoolean("editable", false);

            /*
             * pendingSign:
             * - 저장 후 다시 열었을 때 아직 사인을 받아야 하는 sign 영역인지 표시
             * - 현재는 metadata에 값이 없을 수 있으므로 sign + editable이면 true로 보정한다.
             */
            field.pendingSign = obj.optBoolean("pendingSign", false);

            if ("sign".equalsIgnoreCase(field.type) && field.editable) {
                field.pendingSign = true;
            }

            /*
             * 좌표 변환.
             */
            PDPage page = document.getPage(pageNo);
            float pageHeight = page.getMediaBox().getHeight();

            RectF pdfRect = new RectF();

            pdfRect.left = x;
            pdfRect.right = x + width;

            /*
             * CCF 좌상단 Y → PDF 좌하단 Y 변환.
             */
            pdfRect.top = pageHeight - y;
            pdfRect.bottom = pageHeight - y - height;

            field.pdfRect = pdfRect;

            safeAddDebug(debugTextList,
                    "metadata field"
                            + ", type=" + field.type
                            + ", name=" + field.name
                            + ", ccfField=" + field.ccfField
                            + ", value=" + field.value
                            + ", editable=" + field.editable
                            + ", rect=" + rectToString(field.pdfRect));

            return field;

        } catch (Exception ex) {
            safeAddDebug(debugTextList,
                    "createRenderedFieldFromMetadata error=" + ex.getMessage());
            Log.d(TAG, "createRenderedFieldFromMetadata error=" + ex.getMessage(), ex);
        }

        return null;
    }

    /**
     * null 방어용 문자열 변환.
     */
    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * debugTextList가 null이 아닐 때만 안전하게 로그를 추가한다.
     *
     * PdfInkSignView.drawDebugText()가 현재 비활성화되어 있어도,
     * 디버깅할 때 이 리스트를 화면에 출력하도록 바꾸면 원인 추적에 도움이 된다.
     */
    private static void safeAddDebug(
            List<String> debugTextList,
            String text
    ) {
        if (debugTextList == null) return;

        try {
            debugTextList.add(text);
        } catch (Exception ignore) {
        }
    }

    /**
     * RectF 디버그 출력용.
     */
    private static String rectToString(RectF r) {
        if (r == null) return "null";

        return "("
                + r.left + ","
                + r.top + ","
                + r.right + ","
                + r.bottom
                + ")";
    }
}