package com.laker.postman.service;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.service.collections.OpenedRequestService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExitService {
    private ExitService() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 显示退出确认对话框，处理未保存内容。
     */
    public static void beforeExit() {

        // 保存所有打开的请求（包括未保存的和已保存的）
        OpenedRequestService.save();

        // 没有未保存内容，或已处理完未保存内容，直接退出
        log.info("User chose to exit application");
        SingletonFactory.getInstance(MainFrame.class).dispose();
        System.exit(0);
    }
}