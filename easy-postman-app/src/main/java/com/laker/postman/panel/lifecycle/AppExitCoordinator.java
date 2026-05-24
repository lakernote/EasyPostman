package com.laker.postman.panel.lifecycle;

import com.laker.postman.common.UiSingletonFactory;
import com.laker.postman.common.exception.CancelException;
import com.laker.postman.frame.MainFrame;
import com.laker.postman.ioc.Component;
import com.laker.postman.panel.collections.OpenedRequestTabsSaver;
import com.laker.postman.panel.functional.FunctionalPanel;
import com.laker.postman.panel.performance.PerformancePanel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AppExitCoordinator {

    /**
     * 显示退出确认对话框，处理未保存内容。
     */
    public void exitApplication() {

        // 保存所有打开的请求（包括未保存的和已保存的）
        try {
            OpenedRequestTabsSaver.saveOpenTabsOnExit();
        } catch (CancelException e) {
            // 用户取消了保存操作，终止退出
            return;
        }

        // 保存功能测试配置
        try {
            FunctionalPanel functionalPanel = UiSingletonFactory.getInstance(FunctionalPanel.class);
            functionalPanel.save();
        } catch (Exception e) {
            log.error("Failed to save functional test config on exit", e);
        }

        // 保存性能测试配置
        try {
            PerformancePanel performancePanel = UiSingletonFactory.getInstance(PerformancePanel.class);
            performancePanel.save();
        } catch (Exception e) {
            log.error("Failed to save performance test config on exit", e);
        }

        // 没有未保存内容，或已处理完未保存内容，直接退出
        log.info("User chose to exit application");
        UiSingletonFactory.getInstance(MainFrame.class).dispose();
        System.exit(0);
    }
}
