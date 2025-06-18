package com.laker.postman.util;

import java.io.File;

public class SystemUtil {
    public static final String LOG_DIR = getUserHomeEasyPostmanPath() + "logs" + File.separator;
    public static final String COLLECTION_PATH = getUserHomeEasyPostmanPath() + "collections.json";
    public static final String ENV_PATH = getUserHomeEasyPostmanPath() + "environments.json";

    public static String getUserHomeEasyPostmanPath() {
        return System.getProperty("user.home") + File.separator + "EasyPostman" + File.separator;
    }
}