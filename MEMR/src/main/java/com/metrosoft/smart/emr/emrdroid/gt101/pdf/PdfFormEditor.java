package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * PDF Form Overlay 준비 클래스.
 *
 * 중요:
 * - 이 클래스는 AcroForm field를 만들지 않는다.
 * - 이 클래스는 PDF 본문에 text / checkbox / radio / label / sign_image / sign 값을 직접 그리지 않는다.
 * - 모든 field 정보를 PDF Catalog의 MS_OVERLAY_FIELDS metadata에 저장한다.
 *
 * 처리 흐름:
 *
 * 1. ConsentForm
 *    → PdfFormFieldSpec 목록 생성
 *    → valuesToFill 생성
 *    → PdfFormEditor.prepareAndFillPdf() 호출
 *
 * 2. PdfFormEditor
 *    → 원본 PDF를 그대로 복사하되
 *    → 모든 field 정보를 MS_OVERLAY_FIELDS metadata에 저장
 *
 * 3. PdfInkSignView
 *    → PdfFormFieldReader가 metadata를 읽음
 *    → 모든 field를 overlay로 화면에 그림
 *    → 사용자가 text / checkbox / radio / label / sign_image / sign 값을 변경 가능
 *
 * 4. PdfInkPdfSaver
 *    → 저장/임시저장 시 모든 값을 PDF 본문에 굳혀서 그림
 *    → 단, sign 필드 중 사용자가 서명하지 않은 field만 metadata에 다시 저장
 */
public class PdfFormEditor {

    /**
     * PdfFormFieldReader / PdfInkPdfSaver와 동일하게 사용하는 metadata key.
     *
     * 저장 위치:
     * document.getDocumentCatalog().getCOSObject().setString(
     *     MS_OVERLAY_FIELDS,
     *     metadataArray.toString()
     * );
     */
    public static final COSName MS_OVERLAY_FIELDS =
            COSName.getPDFName("MS_OVERLAY_FIELDS");

    private static final int DEFAULT_FONT_SIZE = 10;

    /**
     * PDF 준비 메인 함수.
     *
     * 기존 AcroForm 방식과 달리:
     * - AcroForm field를 만들지 않음
     * - PDF page content에 값을 직접 그리지 않음
     * - 모든 field 정보를 metadata에만 저장
     *
     * @param context Android Context
     * @param srcPdf 원본 PDF
     * @param outPdf metadata가 추가된 출력 PDF
     * @param fieldsToCreate CCF 기반 field 목록
     * @param valuesToFill 초기값 map
     * @param listener 디버그 콜백
     */
    public static void prepareAndFillPdf(
            Context context,
            File srcPdf,
            File outPdf,
            List<PdfFormFieldSpec> fieldsToCreate,
            Map<String, String> valuesToFill,
            PdfDebugListener listener
    ) throws Exception {

        PDFBoxResourceLoader.init(context);

        PDDocument document = null;

        try {
            document = PDDocument.load(srcPdf);

            if (listener != null) {
                listener.onError("metadata-mode-101");
            }

            JSONArray metadataArray = new JSONArray();

            if (fieldsToCreate != null) {
                for (int i = 0; i < fieldsToCreate.size(); i++) {
                    PdfFormFieldSpec spec = fieldsToCreate.get(i);
                    if (spec == null) continue;

                    if (spec.pageNo < 0 || spec.pageNo >= document.getNumberOfPages()) {
                        if (listener != null) {
                            listener.onError("metadata-mode skip invalid page"
                                    + ", fieldName=" + nvl(spec.fieldName)
                                    + ", pageNo=" + spec.pageNo);
                        }
                        continue;
                    }

                    /*
                     * field 초기값 결정.
                     *
                     * 우선순위:
                     * 1. valuesToFill[fieldName]
                     * 2. valuesToFill[ccfField]
                     * 3. spec.value
                     * 4. spec.defaultValue
                     * 5. ""
                     */
                    String value = getSpecValue(spec);

                    if (valuesToFill != null) {
                        if (spec.fieldName != null && valuesToFill.containsKey(spec.fieldName)) {
                            value = valuesToFill.get(spec.fieldName);
                        } else if (spec.ccfField != null && valuesToFill.containsKey(spec.ccfField)) {
                            value = valuesToFill.get(spec.ccfField);
                        }
                    }

                    String typeName = nvl(spec.typeName).trim();
                    if ("".equals(typeName)) {
                        typeName = "label";
                    }

                    /*
                     * 모든 field를 metadata에 저장한다.
                     *
                     * 이 단계에서는 PDF 본문에 값을 그리지 않는다.
                     * 그래야 PdfInkSignView에서 overlay로 그릴 때 이중 출력되지 않는다.
                     */
                    metadataArray.put(toMetadataJson(spec, typeName, value, true));

                    if (listener != null) {
                        listener.onError("metadata-mode field"
                                + ", name=" + nvl(spec.fieldName)
                                + ", ccfField=" + nvl(spec.ccfField)
                                + ", type=" + typeName
                                + ", value=" + nvl(value));
                    }
                }
            }

            /*
             * metadata 저장.
             *
             * 이 PDF를 PdfInkSignView가 열면
             * PdfFormFieldReader가 MS_OVERLAY_FIELDS를 읽어서
             * PdfRenderedFormField 목록을 만든다.
             */
            document.getDocumentCatalog().getCOSObject().setString(
                    MS_OVERLAY_FIELDS,
                    metadataArray.toString()
            );

            if (listener != null) {
                listener.onError("metadata-mode-102 count=" + metadataArray.length());
            }

            /*
             * signs, flattenAfterSave는 현재 방식에서 사용하지 않는다.
             *
             * signs:
             * - 기존 이미지 직접 삽입 방식의 호환 파라미터였음
             * - 지금은 sign/sign_image 모두 metadata로 관리하고
             *   PdfInkSignView에서 overlay 표시 후 PdfInkPdfSaver에서 최종 저장한다.
             *
             * flattenAfterSave:
             * - AcroForm 전용 개념
             * - 현재 AcroForm을 만들지 않으므로 사용하지 않는다.
             */

            document.save(outPdf);

            if (listener != null) {
                listener.onError("metadata-mode-103 saved");
            }

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
     * PdfFormFieldSpec 한 건을 metadata JSON으로 변환한다.
     *
     * 저장 좌표:
     * - x/y/width/height는 PdfFormFieldSpec 기준 좌표 그대로 저장한다.
     * - 현재 구조에서 spec.x/spec.y는 CCF 기준 좌상단 좌표이다.
     *
     * 읽기:
     * - PdfFormFieldReader가 pageHeight를 이용해 PDF 좌표계 RectF로 변환한다.
     */
    private static JSONObject toMetadataJson(
            PdfFormFieldSpec spec,
            String typeName,
            String value,
            boolean editable
    ) throws Exception {

        JSONObject obj = new JSONObject();

        obj.put("pageNo", spec.pageNo);

        /*
         * fieldName:
         * - PDF 내부에서 field를 고유하게 식별하기 위한 이름
         * - AcroForm field명은 아니지만 기존 코드 호환을 위해 유지한다.
         */
        obj.put("fieldName", nvl(spec.fieldName));

        /*
         * ccfField:
         * - 서버/CCF 기준 논리 필드명
         * - 값 매핑, mApplyDrnm/mApplyDptnm, injectCcfValue 계열에서 사용
         */
        obj.put("ccfField", nvl(spec.ccfField));

        /*
         * typeName:
         * - text
         * - label
         * - checkbox
         * - radio
         * - sign
         * - sign_image
         */
        obj.put("typeName", nvl(typeName));

        /*
         * autoFit:
         * - 문자열 출력시 자동 줄바꿉할지 여부
         */
        obj.put("autoFit", spec.autoFit);

        /*
         * radio 그룹 처리용.
         */
        obj.put("groupName", nvl(spec.groupName));

        /*
         * 현재 field 값.
         * PdfInkSignView에서 overlay로 표시하고,
         * 사용자가 수정하면 field.value가 변경된다.
         */
        obj.put("value", nvl(value));

        /*
         * 좌상단 기준 field 위치.
         */
        obj.put("x", spec.x);
        obj.put("y", spec.y);
        obj.put("width", spec.width);
        obj.put("height", spec.height);

        obj.put("fontSize", spec.fontSize <= 0 ? DEFAULT_FONT_SIZE : spec.fontSize);

        /*
         * 모든 field가 수정 가능해야 한다는 정책이므로 true.
         *
         * 단, 나중에 특정 field만 잠그고 싶으면 spec.editable 값을 사용하도록
         * 이 부분을 바꿀 수 있다.
         */
        obj.put("editable", editable);

        /*
         * sign field 중 아직 서명이 필요한 field인지 여부.
         *
         * 최초 작성 시점에서는 sign field는 모두 pendingSign=true로 둔다.
         * 저장 시 PdfInkPdfSaver가 서명 완료된 sign은 metadata에서 제거하고,
         * 미서명 sign만 metadata에 다시 남긴다.
         */
        obj.put("pendingSign", "sign".equalsIgnoreCase(nvl(typeName)));

        return obj;
    }

    /**
     * spec의 기본값을 안전하게 가져온다.
     *
     * 기존 코드와 호환하기 위해 defaultValue와 value를 모두 고려한다.
     */
    private static String getSpecValue(PdfFormFieldSpec spec) {
        if (spec == null) return "";

        if (spec.value != null) {
            return spec.value;
        }

        if (spec.defaultValue != null) {
            return spec.defaultValue;
        }

        return "";
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }
}