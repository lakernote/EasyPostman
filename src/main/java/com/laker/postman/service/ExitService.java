package com.laker.postman.service;

import com.laker.postman.common.SingletonFactory;
import com.laker.postman.common.exception.CancelException;
import com.laker.postman.common.frame.MainFrame;
import com.laker.postman.service.collections.OpenedRequestsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExitService {
    private ExitService() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 显示退出确认对话框，处理未保存内容。
     */
    public static void exit() {

        // 保存所有打开的请求（包括未保存的和已保存的）
        try {
            OpenedRequestsService.save();
        } catch (CancelException e) {
            // 用户取消了保存操作，终止退出
            return;
        }

        // 没有未保存内容，或已处理完未保存内容，直接退出
        log.info("User chose to exit application");
        SingletonFactory.getInstance(MainFrame.class).dispose();
        System.exit(0);
    }
}