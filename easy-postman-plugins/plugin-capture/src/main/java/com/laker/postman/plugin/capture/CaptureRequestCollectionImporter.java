package com.laker.postman.plugin.capture;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.panel.collections.left.RequestCollectionsLeftPanel;
import com.laker.postman.panel.collections.right.RequestEditPanel;
import com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel;
import com.laker.postman.util.NotificationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.laker.postman.plugin.capture.CaptureI18n.t;

final class CaptureRequestCollectionImporter {

    void importFlows(List<CaptureFlow> flows) {
        if (flows == null || flows.isEmpty()) {
            NotificationUtil.showWarning(t(MessageKeys.TOOLBOX_CAPTURE_IMPORT_EMPTY));
            return;
        }

        RequestCollectionsLeftPanel collectionPanel = SingletonFactory.getInstance(RequestCollectionsLeftPanel.class);
        RequestEditPanel requestEditPanel = SingletonFactory.getInstance(RequestEditPanel.class);

        String defaultName = flows.size() == 1
                ? suggestedName(flows.get(0))
                : t(MessageKeys.TOOLBOX_CAPTURE_IMPORT_DEFAULT_BATCH_NAME);
        Object[] result = requestEditPanel.showGroupAndNameDialog(collectionPanel.getGroupTreeModel(), defaultName);
        if (result == null) {
            return;
        }

        Object[] groupObj = (Object[]) result[0];
        String baseName = String.valueOf(result[1]);
        HttpRequestItem lastImported = null;
        for (int i = 0; i < flows.size(); i++) {
            CaptureFlow flow = flows.get(i);
            String requestName = flows.size() == 1 ? baseName : baseName + " " + (i + 1);
            HttpRequestItem item = toHttpRequestItem(flow, requestName);
            collectionPanel.saveRequestToGroup(groupObj, item);
            lastImported = item;
        }

        if (lastImported != null) {
            collectionPanel.locateAndSelectRequest(lastImported.getId());
            requestEditPanel.showOrCreateTab(lastImported);
        }
        NotificationUtil.showSuccess(t(MessageKeys.TOOLBOX_CAPTURE_IMPORT_SUCCESS, flows.size()));
    }

    private HttpRequestItem toHttpRequestItem(CaptureFlow flow, String requestName) {
        HttpRequestItem item = new HttpRequestItem();
        item.setId(UUID.randomUUID().toString().replace("-", ""));
        item.setName(requestName);
        item.setUrl(flow.collectionRequestUrl());
        item.setMethod(flow.method());
        item.setProtocol(resolveProtocol(flow));
        item.setHeadersList(toHeaders(flow));
        item.setDescription(buildDescription(flow));
        populateBody(flow, item);
        return item;
    }

    private List<HttpHeader> toHeaders(CaptureFlow flow) {
        List<HttpHeader> headers = new ArrayList<>();
        for (Map.Entry<String, String> entry : flow.requestHeadersSnapshot().entrySet()) {
            String key = entry.getKey();
            if (shouldSkipHeader(key, flow)) {
                continue;
            }
            headers.add(new HttpHeader(true, key, entry.getValue()));
        }
        return headers;
    }

    private boolean shouldSkipHeader(String key, CaptureFlow flow) {
        if (key == null || key.isBlank()) {
            return true;
        }
        String normalized = key.toLowerCase(Locale.ROOT);
        if ("host".equals(normalized)
                || "content-length".equals(normalized)
                || "proxy-connection".equals(normalized)
                || "transfer-encoding".equals(normalized)) {
            return true;
        }
        if (flow.isWebSocketProtocol()) {
            return "connection".equals(normalized)
                    || "upgrade".equals(normalized)
                    || normalized.startsWith("sec-websocket-");
        }
        return false;
    }

    private RequestItemProtocolEnum resolveProtocol(CaptureFlow flow) {
        if (flow.isWebSocketProtocol()) {
            return RequestItemProtocolEnum.WEBSOCKET;
        }
        if (flow.isSseProtocol()) {
            return RequestItemProtocolEnum.SSE;
        }
        return RequestItemProtocolEnum.HTTP;
    }

    private void populateBody(CaptureFlow flow, HttpRequestItem item) {
        if (flow.isWebSocketProtocol()) {
            item.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
            item.setBody("");
            return;
        }
        String bodyText = flow.requestBodyImportText();
        if (!bodyText.isBlank()) {
            item.setBodyType(RequestBodyPanel.BODY_TYPE_RAW);
            item.setBody(bodyText);
        } else {
            item.setBodyType(RequestBodyPanel.BODY_TYPE_NONE);
            item.setBody("");
        }
    }

    private String buildDescription(CaptureFlow flow) {
        StringBuilder builder = new StringBuilder();
        builder.append("Imported from Capture\n");
        builder.append("Captured At: ").append(flow.startedAtText()).append('\n');
        builder.append("Source URL: ").append(flow.collectionRequestUrl()).append('\n');
        if (flow.requestBodyPartial()) {
            builder.append("Note: request body preview was truncated during capture.\n");
        }
        return builder.toString().trim();
    }

    private String suggestedName(CaptureFlow flow) {
        String path = flow.path();
        if (path == null || path.isBlank() || "/".equals(path)) {
            return flow.method() + " " + flow.host();
        }
        String compactPath = path.length() > 48 ? path.substring(0, 48) + "..." : path;
        return flow.method() + " " + compactPath;
    }
}
