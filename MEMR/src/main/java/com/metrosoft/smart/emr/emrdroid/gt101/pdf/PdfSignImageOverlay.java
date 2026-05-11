package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.graphics.Bitmap;
import android.graphics.RectF;

/**
 * sign_image 필드 전용 overlay 모델.
 *
 * 주의:
 * - 사용자가 직접 입력하는 sign 필드와 분리하기 위한 클래스이다.
 * - PdfSignOverlay는 sign 필드 전용으로 사용한다.
 * - PdfSignImageOverlay는 sign_image 필드 전용으로 사용한다.
 *
 * 사용 목적:
 * - 화면에서 sign_image를 표시할 때 로딩된 Bitmap을 보관한다.
 * - 동일한 sign_image를 반복해서 decode하지 않도록 캐시 역할을 한다.
 * - PdfInkPdfSaver에서 sign_image 저장 여부 판단에는 사용하지 않아도 된다.
 *   저장은 PdfRenderedFormField.value 기준으로 처리하는 것이 더 안전하다.
 */
public class PdfSignImageOverlay {

    /**
     * PDF field 이름.
     *
     * 예:
     * - drsign0
     * - sign_image_1
     */
    public String fieldName;

    /**
     * 원래 CCF field 이름.
     *
     * 예:
     * - drsign
     */
    public String ccfField;

    /**
     * sign_image 값.
     *
     * 예:
     * - sign_AA10011
     * - login_sign_AA10011
     * - logindrsign_AA10011
     */
    public String value;

    /**
     * sign_image가 속한 radio/group 용도 값.
     * 일반적으로 sign_image에서는 사용하지 않지만,
     * PdfRenderedFormField와 맞추기 위해 둔다.
     */
    public String groupName;

    /**
     * sign_image가 그려질 PDF 좌표계 사각형.
     *
     * PDF 좌표계:
     * - 원점: 좌하단
     * - Y: 위쪽 증가
     */
    public RectF pdfRect = new RectF();

    /**
     * 화면 표시용 Bitmap.
     *
     * 이 bitmap은 sign_image 전용이다.
     * 사용자가 직접 입력한 sign bitmap과 섞으면 안 된다.
     */
    public Bitmap bitmap;

    /**
     * 화면 표시 여부.
     */
    public boolean visible = false;

    /**
     * 얕은 복사.
     *
     * Bitmap은 복사하지 않고 같은 참조를 공유한다.
     * RectF는 새 객체로 복사한다.
     */
    public PdfSignImageOverlay copyShallow() {
        PdfSignImageOverlay copied = new PdfSignImageOverlay();

        copied.fieldName = this.fieldName;
        copied.ccfField = this.ccfField;
        copied.value = this.value;
        copied.groupName = this.groupName;

        if (this.pdfRect != null) {
            copied.pdfRect = new RectF(this.pdfRect);
        }

        copied.bitmap = this.bitmap;
        copied.visible = this.visible;

        return copied;
    }
}