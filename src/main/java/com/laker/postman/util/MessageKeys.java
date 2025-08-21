package com.laker.postman.util;

/**
 * 国际化消息键常量类
 * 统一管理所有的国际化消息键，避免硬编码字符串
 */
public final class MessageKeys {

    // 私有构造函数，防止实例化
    private MessageKeys() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    // ============ 菜单相关 ============
    public static final String MENU_FILE = "menu.file";
    public static final String MENU_FILE_LOG = "menu.file.log";
    public static final String MENU_FILE_EXIT = "menu.file.exit";
    public static final String MENU_THEME = "menu.theme";
    public static final String MENU_THEME_LIGHT = "menu.theme.light";
    public static final String MENU_THEME_INTELLIJ = "menu.theme.intellij";
    public static final String MENU_THEME_MAC = "menu.theme.mac";
    public static final String MENU_LANGUAGE = "menu.language";
    public static final String MENU_SETTINGS = "menu.settings";
    public static final String MENU_SETTINGS_GLOBAL = "menu.settings.global";
    public static final String MENU_HELP = "menu.help";
    public static final String MENU_HELP_UPDATE = "menu.help.update";
    public static final String MENU_HELP_FEEDBACK = "menu.help.feedback";
    public static final String MENU_ABOUT = "menu.about";
    public static final String MENU_ABOUT_EASYPOSTMAN = "menu.about.easypostman";
    public static final String MENU_COLLECTIONS = "menu.collections";
    public static final String MENU_ENVIRONMENTS = "menu.environments";
    public static final String MENU_FUNCTIONAL = "menu.functional";
    public static final String MENU_PERFORMANCE = "menu.performance";
    public static final String MENU_HISTORY = "menu.history";

    // ============ 语言相关 ============
    public static final String LANGUAGE_CHANGED = "language.changed";

    // ============ 更新相关 ============
    public static final String UPDATE_CHECKING = "update.checking";
    public static final String UPDATE_LATEST_VERSION = "update.latest_version";
    public static final String UPDATE_NEW_VERSION_FOUND = "update.new_version_found";
    public static final String UPDATE_MANUAL_DOWNLOAD = "update.manual_download";
    public static final String UPDATE_AUTO_DOWNLOAD = "update.auto_download";
    public static final String UPDATE_CANCEL = "update.cancel";
    public static final String UPDATE_DOWNLOADING = "update.downloading";
    public static final String UPDATE_CONNECTING = "update.connecting";
    public static final String UPDATE_DOWNLOAD_PROGRESS = "update.download_progress";
    public static final String UPDATE_DOWNLOAD_SPEED = "update.download_speed";
    public static final String UPDATE_ESTIMATED_TIME = "update.estimated_time";
    public static final String UPDATE_CANCEL_DOWNLOAD = "update.cancel_download";
    public static final String UPDATE_RETRY = "update.retry";
    public static final String UPDATE_DOWNLOAD_CANCELLED = "update.download_cancelled";
    public static final String UPDATE_DOWNLOAD_FAILED = "update.download_failed";
    public static final String UPDATE_INSTALL_PROMPT = "update.install_prompt";
    public static final String UPDATE_OPEN_INSTALLER_FAILED = "update.open_installer_failed";
    public static final String UPDATE_NO_INSTALLER_FOUND = "update.no_installer_found";

    // ============ 错误消息 ============
    public static final String ERROR_NETWORK = "error.network";
    public static final String ERROR_UPDATE_FAILED = "error.update_failed";
    public static final String ERROR_NO_VERSION_INFO = "error.no_version_info";
    public static final String ERROR_OPEN_LOG_FAILED = "error.open_log_failed";
    public static final String ERROR_OPEN_LOG_MESSAGE = "error.open_log_message";
    public static final String ERROR_OPEN_LINK_FAILED = "error.open_link_failed";
    public static final String ERROR_NETWORK_TIMEOUT = "error.network_timeout";
    public static final String ERROR_SERVER_UNREACHABLE = "error.server_unreachable";
    public static final String ERROR_INVALID_DOWNLOAD_LINK = "error.invalid_download_link";
    public static final String ERROR_DISK_SPACE_INSUFFICIENT = "error.disk_space_insufficient";
    public static final String ERROR_PERMISSION_DENIED = "error.permission_denied";
    public static final String ERROR_IO_EXCEPTION = "error.io_exception";

    // ============ 关于对话框 ============
    public static final String ABOUT_VERSION = "about.version";
    public static final String ABOUT_AUTHOR = "about.author";
    public static final String ABOUT_LICENSE = "about.license";
    public static final String ABOUT_WECHAT = "about.wechat";
    public static final String ABOUT_BLOG = "about.blog";
    public static final String ABOUT_GITHUB = "about.github";
    public static final String ABOUT_GITEE = "about.gitee";

    // ============ 反馈 ============
    public static final String FEEDBACK_MESSAGE = "feedback.message";
    public static final String FEEDBACK_TITLE = "feedback.title";

    // ============ 通用 ============
    public static final String GENERAL_ERROR = "general.error";
    public static final String GENERAL_ERROR_MESSAGE = "general.error.message";
    public static final String GENERAL_WARNING = "general.warning";
    public static final String GENERAL_INFO = "general.info";
    public static final String GENERAL_TIP = "general.tip";
    public static final String GENERAL_YES = "general.yes";
    public static final String GENERAL_NO = "general.no";
    public static final String GENERAL_OK = "general.ok";
    public static final String GENERAL_CANCEL = "button.cancel";
    public static final String CONSOLE_TITLE = "console.title";
    public static final String SIDEBAR_TOGGLE = "sidebar.toggle";

    // ============ 按钮 ============
    public static final String BUTTON_SEND = "button.send";
    public static final String BUTTON_SAVE = "button.save";
    public static final String BUTTON_SAVE_TOOLTIP = "button.save.tooltip";
    public static final String BUTTON_CANCEL = "button.cancel";
    public static final String BUTTON_CLOSE = "button.close";
    public static final String BUTTON_START = "button.start";
    public static final String BUTTON_STOP = "button.stop";
    public static final String BUTTON_SEARCH = "button.search";
    public static final String BUTTON_LOAD = "button.load";
    public static final String BUTTON_CLEAR = "button.clear";

    // ============ 请求相关 ============
    public static final String NEW_REQUEST = "new.request";
    public static final String CREATE_NEW_REQUEST = "create.new.request";
    public static final String SAVE_REQUEST = "save.request";
    public static final String REQUEST_NAME = "request.name";
    public static final String SELECT_GROUP = "select.group";
    public static final String PLEASE_ENTER_REQUEST_NAME = "please.enter.request.name";
    public static final String PLEASE_SELECT_GROUP = "please.select.group";
    public static final String PLEASE_SELECT_VALID_GROUP = "please.select.valid.group";
    public static final String REQUEST_SAVED = "request.saved";
    public static final String SUCCESS = "success";
    public static final String UPDATE_REQUEST = "update.request";
    public static final String UPDATE_CURRENT_REQUEST = "update.current.request";
    public static final String UPDATE_REQUEST_FAILED = "update.request.failed";
    public static final String ERROR = "error";

    // ============ 剪贴板和cURL ============
    public static final String CLIPBOARD_CURL_DETECTED = "clipboard.curl.detected";
    public static final String IMPORT_CURL = "import.curl";
    public static final String PARSE_CURL_ERROR = "parse.curl.error";
    public static final String TIP = "tip";

    // ============ 标签页 ============
    public static final String TAB_PARAMS = "tab.params";
    public static final String TAB_AUTHORIZATION = "tab.authorization";
    public static final String TAB_HEADERS = "tab.headers";
    public static final String TAB_BODY = "tab.body";
    public static final String TAB_SCRIPTS = "tab.scripts";
    public static final String TAB_COOKIES = "tab.cookies";
    public static final String TAB_TESTS = "tab.tests";
    public static final String TAB_NETWORK_LOG = "tab.network_log";
    public static final String TAB_REQUEST_HEADERS = "tab.request_headers";
    public static final String TAB_REQUEST_BODY = "tab.request_body";
    public static final String TAB_RESPONSE_HEADERS = "tab.response_headers";
    public static final String TAB_RESPONSE_BODY = "tab.response_body";

    // ============ 状态相关 ============
    public static final String STATUS_CANCELED = "status.canceled";
    public static final String STATUS_REQUESTING = "status.requesting";
    public static final String STATUS_DURATION = "status.duration";
    public static final String STATUS_RESPONSE_SIZE = "status.response_size";
    public static final String STATUS_PREFIX = "status.prefix";
    public static final String STATUS_UNKNOWN = "status.unknown";

    // ============ WebSocket相关 ============
    public static final String WEBSOCKET_CONNECTED = "websocket.connected";
    public static final String WEBSOCKET_SUCCESS = "websocket.success";
    public static final String WEBSOCKET_FAILED = "websocket.failed";
    public static final String WEBSOCKET_CLOSING = "websocket.closing";
    public static final String WEBSOCKET_CLOSED = "websocket.closed";
    public static final String WEBSOCKET_ERROR = "websocket.error";

    // ============ SSE相关 ============
    public static final String SSE_SWITCH_TIP = "sse.switch.tip";
    public static final String SSE_SWITCH_TITLE = "sse.switch.title";
    public static final String SSE_HEADER_ADDED = "sse.header.added";
    public static final String OPERATION_TIP = "operation.tip";
    public static final String SSE_FAILED = "sse.failed";
    public static final String SSE_ERROR = "sse.error";

    // ============ WebSocket图标 ============
    public static final String WS_ICON_CONNECTED = "ws.icon.connected";
    public static final String WS_ICON_RECEIVED = "ws.icon.received";
    public static final String WS_ICON_BINARY = "ws.icon.binary";
    public static final String WS_ICON_SENT = "ws.icon.sent";
    public static final String WS_ICON_CLOSED = "ws.icon.closed";
    public static final String WS_ICON_WARNING = "ws.icon.warning";
    public static final String WS_ICON_INFO = "ws.icon.info";

    // ============ 脚本相关 ============
    public static final String SCRIPT_TAB_PRESCRIPT = "script.tab.prescript";
    public static final String SCRIPT_TAB_POSTSCRIPT = "script.tab.postscript";
    public static final String SCRIPT_TAB_HELP = "script.tab.help";
    public static final String SCRIPT_BUTTON_SNIPPETS = "script.button.snippets";
    public static final String SCRIPT_HELP_TEXT = "script.help.text";

    // ============ 认证相关 ============
    public static final String AUTH_TYPE_LABEL = "auth.type.label";
    public static final String AUTH_TYPE_NONE_DESC = "auth.type.none.desc";
    public static final String AUTH_USERNAME = "auth.username";
    public static final String AUTH_PASSWORD = "auth.password";
    public static final String AUTH_TOKEN = "auth.token";

    // ============ Cookie相关 ============
    public static final String COOKIE_BUTTON_DELETE = "cookie.button.delete";
    public static final String COOKIE_BUTTON_CLEAR = "cookie.button.clear";
    public static final String COOKIE_BUTTON_ADD = "cookie.button.add";
    public static final String COOKIE_BUTTON_REFRESH = "cookie.button.refresh";
    public static final String COOKIE_TOOLTIP_ADD = "cookie.tooltip.add";
    public static final String COOKIE_TOOLTIP_DELETE = "cookie.tooltip.delete";
    public static final String COOKIE_TOOLTIP_CLEAR = "cookie.tooltip.clear";
    public static final String COOKIE_TOOLTIP_REFRESH = "cookie.tooltip.refresh";
    public static final String COOKIE_DIALOG_CLEAR_CONFIRM = "cookie.dialog.clear_confirm";
    public static final String COOKIE_DIALOG_CLEAR_CONFIRM_TITLE = "cookie.dialog.clear_confirm_title";
    public static final String COOKIE_DIALOG_ADD_TITLE = "cookie.dialog.add_title";
    public static final String COOKIE_DIALOG_ERROR_EMPTY = "cookie.dialog.error.empty";
    public static final String COOKIE_DIALOG_ERROR_TITLE = "cookie.dialog.error.title";

    // ============ 环境变量相关 ============
    public static final String ENV_BUTTON_IMPORT = "env.button.import";
    public static final String ENV_BUTTON_EXPORT = "env.button.export";
    public static final String ENV_BUTTON_SAVE = "env.button.save";
    public static final String ENV_BUTTON_ADD = "env.button.add";
    public static final String ENV_BUTTON_RENAME = "env.button.rename";
    public static final String ENV_BUTTON_DUPLICATE = "env.button.duplicate";
    public static final String ENV_BUTTON_DELETE = "env.button.delete";
    public static final String ENV_BUTTON_EXPORT_POSTMAN = "env.button.export_postman";
    public static final String ENV_MENU_IMPORT_EASY = "env.menu.import_easy";
    public static final String ENV_MENU_IMPORT_POSTMAN = "env.menu.import_postman";
    public static final String ENV_DIALOG_SAVE_CHANGES = "env.dialog.save_changes";
    public static final String ENV_DIALOG_SAVE_CHANGES_TITLE = "env.dialog.save_changes.title";
    public static final String ENV_DIALOG_SAVE_SUCCESS = "env.dialog.save_success";
    public static final String ENV_DIALOG_SAVE_SUCCESS_TITLE = "env.dialog.save_success.title";
    public static final String ENV_DIALOG_EXPORT_TITLE = "env.dialog.export.title";
    public static final String ENV_DIALOG_EXPORT_SUCCESS = "env.dialog.export.success";
    public static final String ENV_DIALOG_EXPORT_FAIL = "env.dialog.export.fail";
    public static final String ENV_DIALOG_IMPORT_EASY_TITLE = "env.dialog.import_easy.title";
    public static final String ENV_DIALOG_IMPORT_EASY_SUCCESS = "env.dialog.import_easy.success";
    public static final String ENV_DIALOG_IMPORT_EASY_FAIL = "env.dialog.import_easy.fail";
    public static final String ENV_DIALOG_IMPORT_POSTMAN_TITLE = "env.dialog.import_postman.title";
    public static final String ENV_DIALOG_IMPORT_POSTMAN_FAIL = "env.dialog.import_postman.fail";
    public static final String ENV_DIALOG_IMPORT_POSTMAN_INVALID = "env.dialog.import_postman.invalid";
    public static final String ENV_DIALOG_ADD_TITLE = "env.dialog.add.title";
    public static final String ENV_DIALOG_ADD_PROMPT = "env.dialog.add.prompt";
    public static final String ENV_DIALOG_RENAME_TITLE = "env.dialog.rename.title";
    public static final String ENV_DIALOG_RENAME_PROMPT = "env.dialog.rename.prompt";
    public static final String ENV_DIALOG_RENAME_SUCCESS = "env.dialog.rename.success";
    public static final String ENV_DIALOG_RENAME_FAIL = "env.dialog.rename.fail";
    public static final String ENV_DIALOG_DELETE_TITLE = "env.dialog.delete.title";
    public static final String ENV_DIALOG_DELETE_PROMPT = "env.dialog.delete.prompt";
    public static final String ENV_DIALOG_COPY_FAIL = "env.dialog.copy.fail";
    public static final String ENV_NAME_COPY_SUFFIX = "env.name.copy_suffix";

    // ============ 功能测试相关 ============
    public static final String FUNCTIONAL_TAB_REQUEST_CONFIG = "functional.tab.request_config";
    public static final String FUNCTIONAL_TAB_EXECUTION_RESULTS = "functional.tab.execution_results";
    public static final String FUNCTIONAL_MSG_NO_RUNNABLE_REQUEST = "functional.msg.no_runnable_request";
    public static final String FUNCTIONAL_MSG_CSV_DETECTED = "functional.msg.csv_detected";
    public static final String FUNCTIONAL_MSG_CSV_TITLE = "functional.msg.csv_title";
    public static final String FUNCTIONAL_STATUS_NOT_EXECUTED = "functional.status.not_executed";
    public static final String FUNCTIONAL_STATUS_PRE_SCRIPT_FAILED = "functional.status.pre_script_failed";
    public static final String FUNCTIONAL_STATUS_SSE_BATCH_NOT_SUPPORTED = "functional.status.sse_batch_not_supported";
    public static final String FUNCTIONAL_STATUS_WS_BATCH_NOT_SUPPORTED = "functional.status.ws_batch_not_supported";

    // ============ 性能测试相关 ============
    public static final String PERFORMANCE_TAB_TREND = "performance.tab.trend";
    public static final String PERFORMANCE_TAB_REPORT = "performance.tab.report";
    public static final String PERFORMANCE_TAB_RESULT_TREE = "performance.tab.result_tree";
    public static final String PERFORMANCE_PROPERTY_SELECT_NODE = "performance.property.select_node";
    public static final String PERFORMANCE_EFFICIENT_MODE = "performance.efficient_mode";
    public static final String PERFORMANCE_EFFICIENT_MODE_TOOLTIP = "performance.efficient_mode.tooltip";
    public static final String PERFORMANCE_EFFICIENT_MODE_HELP = "performance.efficient_mode.help";
    public static final String PERFORMANCE_EFFICIENT_MODE_DESC = "performance.efficient_mode.desc";
    public static final String PERFORMANCE_EFFICIENT_MODE_HELP_TITLE = "performance.efficient_mode.help_title";
    public static final String PERFORMANCE_PROGRESS_TOOLTIP = "performance.progress.tooltip";
    public static final String PERFORMANCE_MENU_ADD_THREAD_GROUP = "performance.menu.add_thread_group";
    public static final String PERFORMANCE_MENU_ADD_REQUEST = "performance.menu.add_request";
    public static final String PERFORMANCE_MENU_ADD_ASSERTION = "performance.menu.add_assertion";
    public static final String PERFORMANCE_MENU_ADD_TIMER = "performance.menu.add_timer";
    public static final String PERFORMANCE_MENU_RENAME = "performance.menu.rename";
    public static final String PERFORMANCE_MENU_DELETE = "performance.menu.delete";
    public static final String PERFORMANCE_MSG_SELECT_THREAD_GROUP = "performance.msg.select_thread_group";
    public static final String PERFORMANCE_MSG_RENAME_NODE = "performance.msg.rename_node";
    public static final String PERFORMANCE_MSG_EXECUTION_INTERRUPTED = "performance.msg.execution_interrupted";
    public static final String PERFORMANCE_MSG_PRE_SCRIPT_FAILED = "performance.msg.pre_script_failed";
    public static final String PERFORMANCE_MSG_REQUEST_FAILED = "performance.msg.request_failed";
    public static final String PERFORMANCE_MSG_ASSERTION_FAILED = "performance.msg.assertion_failed";

    // ============ 性能趋势相关 ============
    public static final String PERFORMANCE_TREND_THREADS = "performance.trend.threads";
    public static final String PERFORMANCE_TREND_RESPONSE_TIME_MS = "performance.trend.response_time_ms";
    public static final String PERFORMANCE_TREND_RESPONSE_TIME = "performance.trend.response_time";
    public static final String PERFORMANCE_TREND_QPS = "performance.trend.qps";
    public static final String PERFORMANCE_TREND_ERROR_RATE_PERCENT = "performance.trend.error_rate_percent";
    public static final String PERFORMANCE_TREND_ERROR_RATE = "performance.trend.error_rate";
    public static final String PERFORMANCE_TREND_CHART_TITLE = "performance.trend.chart_title";
    public static final String PERFORMANCE_TREND_TIME = "performance.trend.time";
    public static final String PERFORMANCE_TREND_METRIC_VALUE = "performance.trend.metric_value";
    public static final String PERFORMANCE_TREND_METRICS = "performance.trend.metrics";
    public static final String PERFORMANCE_TREND_NO_METRIC_SELECTED = "performance.trend.no_metric_selected";

    // ============ 历史记录相关 ============
    public static final String HISTORY_EMPTY_BODY = "history.empty_body";
    public static final String HISTORY_TODAY = "history.today";
    public static final String HISTORY_YESTERDAY = "history.yesterday";
    public static final String HISTORY_REQUEST_TIME = "history.request_time";

    // ============ 应用相关 ============
    public static final String APP_NAME = "app.name";
    public static final String SPLASH_STATUS_STARTING = "splash.status.starting";
    public static final String SPLASH_STATUS_LOADING_MAIN = "splash.status.loading_main";
    public static final String SPLASH_STATUS_INITIALIZING = "splash.status.initializing";
    public static final String SPLASH_STATUS_READY = "splash.status.ready";
    public static final String SPLASH_STATUS_DONE = "splash.status.done";
    public static final String SPLASH_ERROR_LOAD_MAIN = "splash.error.load_main";

    // ============ 退出相关 ============
    public static final String EXIT_UNSAVED_CHANGES = "exit.unsaved_changes";
    public static final String EXIT_UNSAVED_CHANGES_TITLE = "exit.unsaved_changes.title";
    public static final String EXIT_CONFIRM = "exit.confirm";
    public static final String EXIT_TITLE = "exit.title";

    // ============ 集合相关 ============
    public static final String COLLECTIONS_EXPORT_TOOLTIP = "collections.export.tooltip";
    public static final String COLLECTIONS_IMPORT_TOOLTIP = "collections.import.tooltip";
    public static final String COLLECTIONS_IMPORT_CURL_DETECTED = "collections.import.curl.detected";
    public static final String COLLECTIONS_IMPORT_CURL_TITLE = "collections.import.curl.title";
    public static final String COLLECTIONS_IMPORT_EASY = "collections.import.easy";
    public static final String COLLECTIONS_IMPORT_EASY_TOOLTIP = "collections.import.easy.tooltip";
    public static final String COLLECTIONS_IMPORT_POSTMAN = "collections.import.postman";
    public static final String COLLECTIONS_IMPORT_POSTMAN_TOOLTIP = "collections.import.postman.tooltip";
    public static final String COLLECTIONS_IMPORT_CURL = "collections.import.curl";
    public static final String COLLECTIONS_IMPORT_CURL_TOOLTIP = "collections.import.curl.tooltip";

    // ============ 集合菜单相关 ============
    public static final String COLLECTIONS_MENU_ADD_GROUP = "collections.menu.add_group";
    public static final String COLLECTIONS_MENU_ADD_REQUEST = "collections.menu.add_request";
    public static final String COLLECTIONS_MENU_DUPLICATE = "collections.menu.duplicate";
    public static final String COLLECTIONS_MENU_EXPORT_POSTMAN = "collections.menu.export_postman";
    public static final String COLLECTIONS_MENU_COPY_CURL = "collections.menu.copy_curl";
    public static final String COLLECTIONS_MENU_RENAME = "collections.menu.rename";
    public static final String COLLECTIONS_MENU_DELETE = "collections.menu.delete";
    public static final String COLLECTIONS_MENU_COPY_SUFFIX = "collections.menu.copy_suffix";
    public static final String COLLECTIONS_MENU_COPY_CURL_SUCCESS = "collections.menu.copy_curl.success";
    public static final String COLLECTIONS_MENU_COPY_CURL_FAIL = "collections.menu.copy_curl.fail";
    public static final String COLLECTIONS_MENU_EXPORT_POSTMAN_SELECT_GROUP = "collections.menu.export_postman.select_group";
    public static final String COLLECTIONS_MENU_EXPORT_POSTMAN_DIALOG_TITLE = "collections.menu.export_postman.dialog_title";

    // ============ 集合导出导入相关 ============
    public static final String COLLECTIONS_EXPORT_DIALOG_TITLE = "collections.export.dialog_title";
    public static final String COLLECTIONS_EXPORT_SUCCESS = "collections.export.success";
    public static final String COLLECTIONS_EXPORT_FAIL = "collections.export.fail";
    public static final String COLLECTIONS_IMPORT_DIALOG_TITLE = "collections.import.dialog_title";
    public static final String COLLECTIONS_IMPORT_SUCCESS = "collections.import.success";
    public static final String COLLECTIONS_IMPORT_FAIL = "collections.import.fail";
    public static final String COLLECTIONS_IMPORT_POSTMAN_DIALOG_TITLE = "collections.import.postman.dialog_title";
    public static final String COLLECTIONS_IMPORT_POSTMAN_INVALID = "collections.import.postman.invalid";
    public static final String COLLECTIONS_IMPORT_CURL_DIALOG_TITLE = "collections.import.curl.dialog_title";
    public static final String COLLECTIONS_IMPORT_CURL_DIALOG_PROMPT = "collections.import.curl.dialog_prompt";
    public static final String COLLECTIONS_IMPORT_CURL_PARSE_FAIL = "collections.import.curl.parse_fail";
    public static final String COLLECTIONS_IMPORT_CURL_PARSE_ERROR = "collections.import.curl.parse_error";

    // ============ 集合对话框相关 ============
    public static final String COLLECTIONS_DIALOG_ADD_GROUP_PROMPT = "collections.dialog.add_group.prompt";
    public static final String COLLECTIONS_DIALOG_RENAME_GROUP_PROMPT = "collections.dialog.rename_group.prompt";
    public static final String COLLECTIONS_DIALOG_RENAME_GROUP_EMPTY = "collections.dialog.rename_group.empty";
    public static final String COLLECTIONS_DIALOG_RENAME_REQUEST_PROMPT = "collections.dialog.rename_request.prompt";
    public static final String COLLECTIONS_DIALOG_RENAME_REQUEST_EMPTY = "collections.dialog.rename_request.empty";
    public static final String COLLECTIONS_DIALOG_MULTI_SELECT_TITLE = "collections.dialog.multi_select.title";
    public static final String COLLECTIONS_DIALOG_MULTI_SELECT_EMPTY = "collections.dialog.multi_select.empty";
    public static final String COLLECTIONS_DELETE_CONFIRM = "collections.delete.confirm";
    public static final String COLLECTIONS_DELETE_CONFIRM_TITLE = "collections.delete.confirm_title";
}
