package com.laker.postman.startup;

import com.laker.postman.ioc.BeanFactory;
import com.laker.postman.service.UpdateService;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.Timer;

/**
 * 主窗口可见后的启动更新检查调度。
 */
@Slf4j
@UtilityClass
class StartupUpdateScheduler {
    private static final int UPDATE_CHECK_DELAY_MS = 2000;

    void scheduleBackgroundUpdateCheck() {
        Timer delayTimer = new Timer(UPDATE_CHECK_DELAY_MS, e -> {
            try {
                BeanFactory.getBean(UpdateService.class).checkUpdateOnStartup();
                log.debug("Background update check scheduled after main window became visible");
            } catch (Exception ex) {
                log.warn("Failed to start background update check", ex);
            }
        });
        delayTimer.setRepeats(false);
        delayTimer.start();
    }
}
