package com.laker.postman.service.update;

import com.laker.postman.platform.update.UpdateCenter;
import lombok.experimental.UtilityClass;

/**
 * Shared app-side update center.
 */
@UtilityClass
public class AppUpdateCenter {

    private static final UpdateCenter INSTANCE = new UpdateCenter(new AppUpdateStateStore());

    public static UpdateCenter get() {
        return INSTANCE;
    }
}
