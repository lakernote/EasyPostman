package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.request.model.HttpParam;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestParamsPanel;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class RequestUrlParamsSynchronizer {
    private final JTextField urlField;
    private final EasyRequestParamsPanel paramsPanel;
    private boolean updatingFromUrl;
    private boolean updatingFromParams;

    void syncUrlToParams(boolean isLoadingData) {
        if (updatingFromParams || isLoadingData) {
            return;
        }

        updatingFromUrl = true;
        try {
            List<HttpParam> currentParams = paramsPanel.getParamsList();
            List<HttpParam> mergedParams = RequestUrlEditorSupport.mergeUrlParamsWithDisabledParams(urlField.getText(), currentParams);
            if (mergedParams != currentParams) {
                paramsPanel.setParamsList(mergedParams);
            }
        } finally {
            updatingFromUrl = false;
        }
    }

    void syncParamsToUrl(boolean isLoadingData) {
        if (updatingFromUrl || isLoadingData) {
            return;
        }

        updatingFromParams = true;
        try {
            String currentUrl = urlField.getText().trim();
            List<HttpParam> params = paramsPanel.getParamsList();
            String newUrl = RequestUrlEditorSupport.rebuildUrlFromParams(currentUrl, params);
            if (!newUrl.equals(currentUrl)) {
                urlField.setText(newUrl);
                urlField.setCaretPosition(0);
            }
        } finally {
            updatingFromParams = false;
        }
    }
}
