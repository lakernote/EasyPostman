package com.laker.postman.panel.collections.right.request;

import com.laker.postman.model.HttpParam;
import com.laker.postman.panel.collections.right.request.sub.EasyRequestParamsPanel;

import javax.swing.*;
import java.util.List;

final class RequestUrlSyncHelper {
    private final JTextField urlField;
    private final EasyRequestParamsPanel paramsPanel;
    private boolean updatingFromUrl;
    private boolean updatingFromParams;

    RequestUrlSyncHelper(JTextField urlField, EasyRequestParamsPanel paramsPanel) {
        this.urlField = urlField;
        this.paramsPanel = paramsPanel;
    }

    void syncUrlToParams(boolean isLoadingData) {
        if (updatingFromParams || isLoadingData) {
            return;
        }

        updatingFromUrl = true;
        try {
            List<HttpParam> currentParams = paramsPanel.getParamsList();
            List<HttpParam> mergedParams = RequestUrlHelper.mergeUrlParamsWithDisabledParams(urlField.getText(), currentParams);
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
            String newUrl = RequestUrlHelper.rebuildUrlFromParams(currentUrl, params);
            if (!newUrl.equals(currentUrl)) {
                urlField.setText(newUrl);
                urlField.setCaretPosition(0);
            }
        } finally {
            updatingFromParams = false;
        }
    }
}
