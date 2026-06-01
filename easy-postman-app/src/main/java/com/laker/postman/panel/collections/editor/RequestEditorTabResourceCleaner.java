package com.laker.postman.panel.collections.editor;

import com.laker.postman.panel.collections.editor.request.RequestEditSubPanel;
import lombok.experimental.UtilityClass;

import java.awt.*;

/**
 * 请求编辑器 Tab 资源清理工具。
 * <p>
 * RequestEditSubPanel 持有网络连接、后台任务等运行资源，Tab 被替换或关闭时必须统一释放。
 */
@UtilityClass
class RequestEditorTabResourceCleaner {
    void cleanup(Component component) {
        if (component instanceof RequestEditSubPanel requestEditSubPanel) {
            requestEditSubPanel.disposeResources();
        }
    }
}
