package com.laker.postman.panel.performance.report;


import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PerformanceReportTableSchema {

    public static final int FAIL_COLUMN_INDEX = 3;
    public static final int SUCCESS_RATE_COLUMN_INDEX = 4;
    public static final int API_NAME_MIN_WIDTH = 110;
    public static final int API_NAME_PREFERRED_WIDTH = 160;

    public String[] httpColumns() {
        return new String[]{
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_API_NAME),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_TOTAL),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_FAIL),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_QPS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SENT_KB_PER_SEC),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_RECEIVED_KB_PER_SEC),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_BYTES),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MIN),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MAX),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P90),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P99)
        };
    }

    public String[] webSocketColumns() {
        return new String[]{
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_API_NAME),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SESSIONS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_FAIL),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SENT),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_RECEIVED),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MATCHED),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SEND_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_RECEIVE_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_FIRST_MESSAGE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_SESSION),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_SESSION)
        };
    }

    public String[] sseColumns() {
        return new String[]{
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_API_NAME),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_STREAMS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_FAIL),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_SUCCESS_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_EVENTS),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MATCHED),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_EVENT_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_MATCHED_RATE),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_FIRST_EVENT),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P90_FIRST_EVENT),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_FIRST_EVENT),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P99_FIRST_EVENT),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_AVG_STREAM),
                I18nUtil.getMessage(MessageKeys.PERFORMANCE_REPORT_COLUMN_P95_STREAM)
        };
    }

    public int[] webSocketColumnWidths() {
        return new int[]{180, 72, 70, 60, 90, 70, 70, 70, 104, 104, 112, 118, 118};
    }

    public int[] sseColumnWidths() {
        return new int[]{180, 72, 70, 60, 90, 70, 70, 92, 104, 112, 112, 112, 112, 118, 118};
    }
}
