package com.metrosoft.smart.emr.emrdroid.gt101.pdf;

import android.content.Context;

import java.io.File;
import java.util.List;
import java.util.Map;

public class PdfFormRuntimeWriter {

    public static void fillOnly(
            Context context,
            File srcPdf,
            File outPdf,
            Map<String, String> valuesToFill,
            List<PdfSignatureSpec> signatures,
            boolean flattenAfterSave
    ) throws Exception {

        PdfFormEditor.prepareAndFillPdf(
                context,
                srcPdf,
                outPdf,
                null, // 폼 필드를 생성하지 않는다.
                valuesToFill,
                signatures,
                flattenAfterSave
        );
    }
}