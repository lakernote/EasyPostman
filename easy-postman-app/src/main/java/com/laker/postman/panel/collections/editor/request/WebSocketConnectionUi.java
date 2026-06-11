package com.laker.postman.panel.collections.editor.request;

import com.laker.postman.http.runtime.model.HttpResponse;

interface WebSocketConnectionUi {
    void updateUIForResponse(HttpResponse resp);

    void resetSendButton();

    void switchSendButtonToClose();

    void setWebSocketConnected(boolean connected);

    void activateWebSocketBodyTab();
}
