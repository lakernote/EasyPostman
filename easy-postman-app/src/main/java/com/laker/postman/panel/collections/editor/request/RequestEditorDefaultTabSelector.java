package com.laker.postman.panel.collections.editor.request;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.panel.collections.editor.request.sub.EasyRequestParamsPanel;
import com.laker.postman.panel.collections.editor.request.sub.RequestBodyPanel;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.request.model.RequestItemProtocolEnum;
import com.laker.postman.request.model.SavedResponse;

import javax.swing.*;

final class RequestEditorDefaultTabSelector {
    private final JTabbedPane reqTabs;
    private final RequestBodyPanel requestBodyPanel;
    private final EasyRequestParamsPanel paramsPanel;

    RequestEditorDefaultTabSelector(JTabbedPane reqTabs,
                                    RequestBodyPanel requestBodyPanel,
                                    EasyRequestParamsPanel paramsPanel) {
        this.reqTabs = reqTabs;
        this.requestBodyPanel = requestBodyPanel;
        this.paramsPanel = paramsPanel;
    }

    void selectByRequestType(RequestItemProtocolEnum effectiveProtocol, HttpRequestItem item) {
        if (item == null) {
            return;
        }

        selectDefaultTab(
                effectiveProtocol,
                item.getMethod(),
                item.getBodyType(),
                item.getBody(),
                CollUtil.isNotEmpty(item.getFormDataList()) || CollUtil.isNotEmpty(item.getUrlencodedList()),
                CollUtil.isNotEmpty(item.getParamsList())
        );
    }

    void selectBySavedResponse(RequestItemProtocolEnum effectiveProtocol,
                               SavedResponse.OriginalRequest originalRequest) {
        if (originalRequest == null) {
            return;
        }

        selectDefaultTab(
                effectiveProtocol,
                originalRequest.getMethod(),
                originalRequest.getBodyType(),
                originalRequest.getBody(),
                CollUtil.isNotEmpty(originalRequest.getFormDataList())
                        || CollUtil.isNotEmpty(originalRequest.getUrlencodedList()),
                CollUtil.isNotEmpty(originalRequest.getParams())
        );
    }

    private void selectDefaultTab(RequestItemProtocolEnum effectiveProtocol,
                                  String method,
                                  String bodyType,
                                  String body,
                                  boolean hasFormData,
                                  boolean hasParams) {
        if (effectiveProtocol != null && effectiveProtocol.isWebSocketProtocol()) {
            RequestTabSelectionHelper.selectFirstVisible(reqTabs, requestBodyPanel, paramsPanel);
            return;
        }
        if (effectiveProtocol != null && effectiveProtocol.isSseProtocol()) {
            RequestTabSelectionHelper.selectFirstVisible(reqTabs, paramsPanel, requestBodyPanel);
            return;
        }
        if (CharSequenceUtil.isNotBlank(body) && !RequestBodyPanel.BODY_TYPE_NONE.equals(bodyType)) {
            RequestTabSelectionHelper.selectFirstVisible(reqTabs, requestBodyPanel, paramsPanel);
            return;
        }
        if (hasFormData) {
            RequestTabSelectionHelper.selectFirstVisible(reqTabs, requestBodyPanel, paramsPanel);
            return;
        }
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            if (hasParams) {
                RequestTabSelectionHelper.selectFirstVisible(reqTabs, paramsPanel, requestBodyPanel);
            } else {
                RequestTabSelectionHelper.selectFirstVisible(reqTabs, requestBodyPanel, paramsPanel);
            }
            return;
        }
        RequestTabSelectionHelper.selectFirstVisible(reqTabs, paramsPanel, requestBodyPanel);
    }
}
