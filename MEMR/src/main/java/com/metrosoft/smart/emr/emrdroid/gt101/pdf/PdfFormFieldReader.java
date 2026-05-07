package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;
import android.graphics.Color;
import android.graphics.RectF;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.cos.COSBase;
import com.tom_roush.pdfbox.cos.COSName;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDAcroForm;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDCheckBox;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDChoice;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDComboBox;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDField;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDListBox;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDNonTerminalField;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDPushButton;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDRadioButton;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDSignatureField;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTerminalField;
import com.tom_roush.pdfbox.pdmodel.interactive.form.PDTextField;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 안의 AcroForm field를 읽어서 화면 표시용 모델(PdfRenderedFormField)로 변환한다.
 *
 * 지원 대상:
 * - Text Field
 * - Check Box
 * - Radio Button
 * - Combo Box
 * - List Box
 * - Push Button
 * - Signature Field
 *
 * 주의:
 * - PDFBox Android 버전에 따라 일부 field 타입의 값 표현 방식이 조금 다를 수 있다.
 * - widget.getPage()가 null인 PDF도 있어서, page 판별은 최대한 안전하게 처리한다.
 */
public class PdfFormFieldReader {

    private static final String TAG = "PdfFormFieldReader";

    private static final COSName MS_FIELD_TYPE = COSName.getPDFName("MS_FIELD_TYPE");
    private static final COSName MS_CCF_FIELD = COSName.getPDFName("MS_CCF_FIELD");
    private static final COSName MS_SIGN_IMAGE_VALUE = COSName.getPDFName("MS_SIGN_IMAGE_VALUE");
    private static final COSName MS_GROUP_NAME = COSName.getPDFName("MS_GROUP_NAME");
    /**
     * 모든 AcroForm field를 읽는다.
     */
    public static List<PdfRenderedFormField> readAllFields(
            Context context,
            File pdfFile,
            int pageIndex,
            List<String> debugTextList
    ) throws Exception {

        PDFBoxResourceLoader.init(context);

        List<PdfRenderedFormField> result = new ArrayList<PdfRenderedFormField>();
        PDDocument document = null;

        try {
            safeAddDebug(debugTextList, "readAllFields start");
            document = PDDocument.load(pdfFile);

            if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
                safeAddDebug(debugTextList, "invalid pageIndex=" + pageIndex);
                return result;
            }

            PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
            if (acroForm == null) {
                safeAddDebug(debugTextList, "acroForm is null");
                return result;
            }

            List<PDField> fields = acroForm.getFields();
            if (fields == null || fields.size() <= 0) {
                safeAddDebug(debugTextList, "acroForm fields empty");
                return result;
            }

            safeAddDebug(debugTextList, "top field count=" + fields.size());

            for (int i = 0; i < fields.size(); i++) {
                collectField(result, document, fields.get(i), pageIndex, debugTextList);
            }

            safeAddDebug(debugTextList, "result count=" + result.size());

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
     * 트리 구조 field를 재귀적으로 순회한다.
     *
     * parent(PDNonTerminalField)는 자식으로 내려가고,
     * terminal field는 widget/rectangle/page 정보를 이용해 화면 모델로 변환한다.
     */
    private static void collectField(
            List<PdfRenderedFormField> result,
            PDDocument document,
            PDField field,
            int targetPageIndex,
            List<String> debugTextList
    ) {
        if (field == null) return;

        String partialName = "";
        String fullName = "";

        try {
            partialName = field.getPartialName();
        } catch (Exception ignore) {
        }

        try {
            fullName = field.getFullyQualifiedName();
        } catch (Exception ignore) {
        }

        safeAddDebug(debugTextList,
                "collectField start partialName=" + partialName + ", fullName=" + fullName);

        // 부모 field이면 자식들을 재귀적으로 순회
        if (field instanceof PDNonTerminalField) {
            PDNonTerminalField parent = (PDNonTerminalField) field;
            List<PDField> children = parent.getChildren();

            if (children != null && children.size() > 0) {
                safeAddDebug(debugTextList, "non-terminal child count=" + children.size());
                for (int i = 0; i < children.size(); i++) {
                    collectField(result, document, children.get(i), targetPageIndex, debugTextList);
                }
            }
            return;
        }

        // terminal field만 실제 화면용 모델 생성 가능
        if (!(field instanceof PDTerminalField)) {
            safeAddDebug(debugTextList, "not terminal field, skip");
            return;
        }

        PDTerminalField terminalField = (PDTerminalField) field;

        String name = fullName;
        if (name == null || "".equals(name)) {
            name = partialName;
        }
        if (name == null) {
            name = "";
        }

        String fieldType = getFieldTypeName(field);
        String value = getFieldValueAsString(field);
        float fontSizePdf = extractFontSize(field);
        int colorArgb = extractTextColor(field);

        safeAddDebug(debugTextList,
                "terminal field type=" + fieldType + ", name=" + name + ", value=" + value);

        List<PDAnnotationWidget> widgets = terminalField.getWidgets();
        if (widgets == null || widgets.size() <= 0) {
            safeAddDebug(debugTextList, "widgets empty, skip");
            return;
        }

        for (int i = 0; i < widgets.size(); i++) {
            PDAnnotationWidget widget = widgets.get(i);
            if (widget == null) continue;

            // widget가 targetPageIndex 페이지에 속하는지 확인
            int widgetPageIndex = findWidgetPageIndex(document, widget, targetPageIndex);
            if (widgetPageIndex != targetPageIndex) {
                continue;
            }

            PDRectangle r = null;
            try {
                r = widget.getRectangle();
            } catch (Exception ignore) {
            }

            if (r == null) {
                safeAddDebug(debugTextList, "widget rectangle null, skip");
                continue;
            }


            PdfRenderedFormField item = new PdfRenderedFormField();

            item.type = getFieldTypeCustom(field, widget);

            item.pageIndex = targetPageIndex;

            // PDF 실제 AcroForm 필드명
            // 예: drnm_001, drnm_002
            item.name = name;

            // 값 매핑용 논리 필드명
            // 예: drnm, dptnm, yyyymmdd
            item.ccfField = getCcfField(field, widget);
            if ("".equals(safe(item.ccfField))) {
                item.ccfField = item.name;
            }

            // radio 그룹명
            item.groupName = getGroupName(field, widget);

            item.fontSizePdf = fontSizePdf;
            item.colorArgb = colorArgb;
            item.readOnly = isFieldReadOnly(field);

            if ("sign_image".equalsIgnoreCase(item.type)) {
                String signValue = "";

                try {
                    signValue = field.getCOSObject().getString(MS_SIGN_IMAGE_VALUE);
                } catch (Exception ignore) {
                }

                if ((signValue == null || "".equals(signValue)) && widget != null) {
                    try {
                        signValue = widget.getCOSObject().getString(MS_SIGN_IMAGE_VALUE);
                    } catch (Exception ignore) {
                    }
                }

                item.value = signValue == null ? "" : signValue;
            } else {
                item.value = value;
            }

            RectF pdfRect = new RectF();
            pdfRect.left = r.getLowerLeftX();
            pdfRect.bottom = r.getLowerLeftY();
            pdfRect.right = r.getUpperRightX();
            pdfRect.top = r.getUpperRightY();
            item.pdfRect = pdfRect;

            safeAddDebug(debugTextList,
                    "-> terminal field type=" + item.type + ", name=" + name + ", value=" + value);

            if (item.isValid()) {
                result.add(item);
            }
        }
    }

    /**
     * field 타입명을 문자열로 반환한다.
     * 화면 디버그나 후처리에 사용하기 쉽도록 단순한 이름으로 통일한다.
     */
    private static String getFieldTypeName(PDField field) {
        if (field instanceof PDTextField) return "text";
        if (field instanceof PDCheckBox) return "checkbox";
        if (field instanceof PDRadioButton) return "radio";
        if (field instanceof PDComboBox) return "combo";
        if (field instanceof PDListBox) return "listbox";
        if (field instanceof PDPushButton) return "button";
        if (field instanceof PDSignatureField) return "signature";
        if (field instanceof PDChoice) return "choice";
        return field.getClass().getSimpleName();
    }

    /**
     * field 값을 문자열로 최대한 읽는다.
     *
     * - text: 입력값
     * - checkbox: true / false
     * - radio: 선택값
     * - combo/listbox: 선택값
     * - button: 일반적으로 표시할 값이 없으므로 빈 문자열
     * - signature: 서명 유무만 단순 표시
     */
    private static String getFieldValueAsString(PDField field) {
        try {
            if (field instanceof PDTextField) {
                String v = ((PDTextField) field).getValueAsString();
                return v == null ? "" : v;
            }

            if (field instanceof PDCheckBox) {
                return ((PDCheckBox) field).isChecked() ? "true" : "false";
            }

            if (field instanceof PDRadioButton) {
                String v = ((PDRadioButton) field).getValue();
                return v == null ? "" : v;
            }

            if (field instanceof PDComboBox) {
                String v = ((PDComboBox) field).getValueAsString();
                return v == null ? "" : v;
            }

            if (field instanceof PDListBox) {
                String v = ((PDListBox) field).getValueAsString();
                return v == null ? "" : v;
            }

            if (field instanceof PDPushButton) {
                return "";
            }

            String v = field.getValueAsString();
            return v == null ? "" : v;

        } catch (Exception ex) {
            Log.d(TAG, "getFieldValueAsString error=" + ex.getMessage());
        }

        return "";
    }

    /**
     * field의 DA(Default Appearance) 문자열에서 폰트 크기를 대략 추출한다.
     * 예: "/F1 10 Tf 0 g"
     *
     * text field 외에도 field 자체 또는 widget 쪽 DA를 볼 수 있도록 최대한 일반화하였다.
     */
    private static float extractFontSize(PDField field) {
        try {
            String da = null;

            // 1순위: field의 DA
            try {
                COSBase daBase = field.getCOSObject().getDictionaryObject(COSName.DA);
                if (daBase != null) {
                    da = field.getCOSObject().getString(COSName.DA);
                }
            } catch (Exception ignore) {
            }

            // 2순위: text field라면 getDefaultAppearance()
            if ((da == null || "".equals(da)) && field instanceof PDTextField) {
                try {
                    da = ((PDTextField) field).getDefaultAppearance();
                } catch (Exception ignore) {
                }
            }

            if (da == null || "".equals(da.trim())) {
                return 10f;
            }

            String[] arr = da.trim().split("\\s+");
            for (int i = 0; i < arr.length - 1; i++) {
                if ("Tf".equals(arr[i + 1])) {
                    try {
                        return Float.parseFloat(arr[i]);
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "extractFontSize error=" + ex.getMessage());
        }

        return 10f;
    }

    /**
     * DA 문자열의 마지막 색상 지시를 단순 해석해서 검정/회색 정도만 처리한다.
     * 복잡한 색상 연산은 생략하고, 실패 시 기본 검정색을 반환한다.
     */
    private static int extractTextColor(PDField field) {
        try {
            String da = null;

            try {
                da = field.getCOSObject().getString(COSName.DA);
            } catch (Exception ignore) {
            }

            if ((da == null || "".equals(da)) && field instanceof PDTextField) {
                try {
                    da = ((PDTextField) field).getDefaultAppearance();
                } catch (Exception ignore) {
                }
            }

            if (da == null) {
                return Color.BLACK;
            }

            // 아주 단순한 처리:
            // "0 g" -> 검정
            // "1 g" -> 흰색
            // 그 외는 일단 검정
            String[] arr = da.trim().split("\\s+");
            for (int i = 0; i < arr.length - 1; i++) {
                if ("g".equals(arr[i + 1])) {
                    try {
                        float gray = Float.parseFloat(arr[i]);
                        int c = Math.max(0, Math.min(255, (int) (gray * 255f)));
                        return Color.rgb(c, c, c);
                    } catch (Exception ignore) {
                    }
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, "extractTextColor error=" + ex.getMessage());
        }

        return Color.BLACK;
    }

    /**
     * widget가 어느 페이지에 있는지 찾는다.
     *
     * 우선순위:
     * 1) widget.getPage() 직접 비교
     * 2) 실패하면 targetPageIndex를 fallback으로 사용
     *
     * 일부 PDF는 widget.getPage()가 null이거나 직접 비교가 잘 안 되는 경우가 있어서
     * 완전히 실패하지 않도록 안전하게 작성했다.
     */
    private static int findWidgetPageIndex(
            PDDocument document,
            PDAnnotationWidget widget,
            int fallbackPageIndex
    ) {
        try {
            PDPage widgetPage = widget.getPage();
            if (widgetPage == null) {
                return fallbackPageIndex;
            }

            int pageCount = document.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);

                // 객체 동일성 비교
                if (page == widgetPage) {
                    return i;
                }

                // COSObject 동일성 비교
                try {
                    if (page.getCOSObject() == widgetPage.getCOSObject()) {
                        return i;
                    }
                } catch (Exception ignore) {
                }
            }
        } catch (Exception ignore) {
        }

        return fallbackPageIndex;
    }

    /**
     * debugTextList가 null이 아닐 때만 안전하게 로그 문자열을 추가한다.
     */
    private static void safeAddDebug(List<String> debugTextList, String text) {
        if (debugTextList == null) return;
        try {
            debugTextList.add(text);
        } catch (Exception ignore) {
        }
    }

    private static String getFieldTypeCustom(PDField field, PDAnnotationWidget widget) {
        String customType = "";

        try {
            customType = field.getCOSObject().getNameAsString(MS_FIELD_TYPE);
            if (customType != null && customType.trim().length() > 0) {
                return customType.trim();
            }
        } catch (Exception ignore) {
        }

        try {
            if (widget != null) {
                customType = widget.getCOSObject().getNameAsString(MS_FIELD_TYPE);
                if (customType != null && customType.trim().length() > 0) {
                    return customType.trim();
                }
            }
        } catch (Exception ignore) {
        }

        if (field instanceof PDTextField) {
            return "text";
        }

        return "";
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String getCcfField(PDField field, PDAnnotationWidget widget) {
        String ccfField = "";

        try {
            ccfField = field.getCOSObject().getString(MS_CCF_FIELD);
        } catch (Exception ignore) {
        }

        if ("".equals(safe(ccfField)) && widget != null) {
            try {
                ccfField = widget.getCOSObject().getString(MS_CCF_FIELD);
            } catch (Exception ignore) {
            }
        }

        return safe(ccfField).trim();
    }

    private static boolean isFieldReadOnly(PDField field) {
        if (field == null) return false;

        try {
            if (field.isReadOnly()) return true;
        } catch (Exception ignore) {
        }

        try {
            int ff = field.getCOSObject().getInt(COSName.FF, 0);
            return (ff & 1) != 0; // bit 1 = ReadOnly
        } catch (Exception ignore) {
        }

        return false;
    }

    private static String getGroupName(PDField field, PDAnnotationWidget widget) {
        String groupName = "";

        try {
            groupName = field.getCOSObject().getString(MS_GROUP_NAME);
        } catch (Exception ignore) {
        }

        if ("".equals(safe(groupName)) && widget != null) {
            try {
                groupName = widget.getCOSObject().getString(MS_GROUP_NAME);
            } catch (Exception ignore) {
            }
        }

        return safe(groupName).trim();
    }

}