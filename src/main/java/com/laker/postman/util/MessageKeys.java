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
    public static final String MENU_WORKSPACES = "menu.workspaces";

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
    public static final String GENERAL_INFO = "general.info";
    public static final String GENERAL_TIP = "general.tip";
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
    public static final String BUTTON_REFRESH = "button.refresh";

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
    public static final String TAB_SCRIPTS = "tab.scripts";
    public static final String TAB_COOKIES = "tab.cookies";
    public static final String TAB_TESTS = "tab.tests";
    public static final String TAB_NETWORK_LOG = "tab.network_log";
    public static final String TAB_REQUEST_HEADERS = "tab.request_headers";
    public static final String TAB_REQUEST_BODY = "tab.request_body";
    public static final String TAB_RESPONSE_HEADERS = "tab.response_headers";
    public static final String TAB_RESPONSE_BODY = "tab.response_body";
    public static final String TAB_CLOSE_OTHERS = "tab.close_others";
    public static final String TAB_CLOSE_ALL = "tab.close_all";
    public static final String TAB_UNSAVED_CHANGES_SAVE_CURRENT = "tab.unsaved_changes.save_current";
    public static final String TAB_UNSAVED_CHANGES_SAVE_OTHERS = "tab.unsaved_changes.save_others";
    public static final String TAB_UNSAVED_CHANGES_SAVE_ALL = "tab.unsaved_changes.save_all";
    public static final String TAB_UNSAVED_CHANGES_TITLE = "tab.unsaved_changes.title";
    public static final String TAB_CLOSE_CURRENT = "tab.close_current";

    // ============ 状态相关 ============
    public static final String STATUS_CANCELED = "status.canceled";
    public static final String STATUS_REQUESTING = "status.requesting";
    public static final String STATUS_DURATION = "status.duration";
    public static final String STATUS_RESPONSE_SIZE = "status.response_size";
    public static final String STATUS_PREFIX = "status.prefix";

    // ============ WebSocket相关 ============
    public static final String WEBSOCKET_CONNECTED = "websocket.connected";
    public static final String WEBSOCKET_SUCCESS = "websocket.success";
    public static final String WEBSOCKET_FAILED = "websocket.failed";
    public static final String WEBSOCKET_ERROR = "websocket.error";
    public static final String WEBSOCKET_NOT_CONNECTED = "websocket.not_connected";

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
    public static final String ENV_DIALOG_EXPORT_POSTMAN_TITLE = "env.dialog.export_postman.title";
    public static final String ENV_DIALOG_EXPORT_POSTMAN_SUCCESS = "env.dialog.export_postman.success";
    public static final String ENV_DIALOG_EXPORT_POSTMAN_FAIL = "env.dialog.export_postman.fail";

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

    // ============ 工作区相关 ============
    public static final String WORKSPACE_NEW = "workspace.new";
    public static final String WORKSPACE_CREATE = "workspace.create";
    public static final String WORKSPACE_NAME = "workspace.name";
    public static final String WORKSPACE_DEFAULT_NAME = "workspace.default.name";
    public static final String WORKSPACE_DEFAULT_DESCRIPTION = "workspace.default.description";
    public static final String WORKSPACE_DESCRIPTION = "workspace.description";
    public static final String WORKSPACE_TYPE = "workspace.type";
    public static final String WORKSPACE_TYPE_LOCAL = "workspace.type.local";
    public static final String WORKSPACE_TYPE_GIT = "workspace.type.git";
    public static final String WORKSPACE_PATH = "workspace.path";
    public static final String WORKSPACE_SELECT_PATH = "workspace.select.path";
    public static final String WORKSPACE_GIT_URL = "workspace.git.url";
    public static final String WORKSPACE_GIT_USERNAME = "workspace.git.username";
    public static final String WORKSPACE_GIT_PASSWORD = "workspace.git.password";
    public static final String WORKSPACE_GIT_TOKEN = "workspace.git.token";
    public static final String WORKSPACE_GIT_AUTH_TYPE = "workspace.git.auth.type";
    public static final String WORKSPACE_GIT_AUTH_NONE = "workspace.git.auth.none";
    public static final String WORKSPACE_GIT_AUTH_PASSWORD = "workspace.git.auth.password";
    public static final String WORKSPACE_GIT_AUTH_TOKEN = "workspace.git.auth.token";
    public static final String WORKSPACE_GIT_AUTH_SSH = "workspace.git.auth.ssh";
    public static final String WORKSPACE_CLONE_FROM_REMOTE = "workspace.clone.from.remote";
    public static final String WORKSPACE_INIT_LOCAL = "workspace.init.local";
    public static final String WORKSPACE_RENAME = "workspace.rename";
    public static final String WORKSPACE_DELETE = "workspace.delete";
    public static final String WORKSPACE_DELETE_CONFIRM = "workspace.delete.confirm";
    public static final String WORKSPACE_SWITCH = "workspace.switch";
    public static final String WORKSPACE_INFO = "workspace.info";
    public static final String WORKSPACE_GIT_PULL = "workspace.git.pull";
    public static final String WORKSPACE_GIT_PUSH = "workspace.git.push";
    public static final String WORKSPACE_GIT_COMMIT = "workspace.git.commit";
    public static final String WORKSPACE_VALIDATION_NAME_REQUIRED = "workspace.validation.name.required";
    public static final String WORKSPACE_VALIDATION_PATH_REQUIRED = "workspace.validation.path.required";
    public static final String WORKSPACE_VALIDATION_GIT_URL_REQUIRED = "workspace.validation.git.url.required";
    public static final String WORKSPACE_VALIDATION_AUTH_REQUIRED = "workspace.validation.auth.required";

    // ============ 性能测试相关 ============
    public static final String PERFORMANCE_TAB_TREND = "performance.tab.trend";
    public static final String PERFORMANCE_TAB_REPORT = "performance.tab.report";
    public static final String PERFORMANCE_TAB_RESULT_TREE = "performance.tab.result_tree";
    public static final String PERFORMANCE_TAB_REQUEST = "performance.tab.request";
    public static final String PERFORMANCE_TAB_RESPONSE = "performance.tab.response";
    public static final String PERFORMANCE_TAB_TESTS = "performance.tab.tests";
    public static final String PERFORMANCE_TAB_TIMING = "performance.tab.timing";
    public static final String PERFORMANCE_TAB_EVENT_INFO = "performance.tab.event_info";
    public static final String PERFORMANCE_NO_ASSERTION_RESULTS = "performance.no_assertion_results";
    public static final String PERFORMANCE_NO_TIMING_INFO = "performance.no_timing_info";
    public static final String PERFORMANCE_NO_EVENT_INFO = "performance.no_event_info";
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
    public static final String PERFORMANCE_TEST_PLAN = "performance.test_plan";
    public static final String PERFORMANCE_THREAD_GROUP = "performance.thread_group";
    public static final String PERFORMANCE_DEFAULT_REQUEST = "performance.default_request";

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

    // ============ 功能测试执行结果相关 ============
    public static final String FUNCTIONAL_EXECUTION_RESULTS = "functional.execution.results";
    public static final String FUNCTIONAL_EXECUTION_HISTORY = "functional.execution.history";
    public static final String FUNCTIONAL_EXECUTION_RESULTS_NO_DATA = "functional.execution.results.no_data";
    public static final String FUNCTIONAL_EXECUTION_RESULTS_SUMMARY = "functional.execution.results.summary";
    public static final String FUNCTIONAL_BUTTON_EXPAND_ALL = "functional.button.expand_all";
    public static final String FUNCTIONAL_BUTTON_COLLAPSE_ALL = "functional.button.collapse_all";
    public static final String FUNCTIONAL_TOOLTIP_EXPAND_ALL = "functional.tooltip.expand_all";
    public static final String FUNCTIONAL_TOOLTIP_COLLAPSE_ALL = "functional.tooltip.collapse_all";
    public static final String FUNCTIONAL_TOOLTIP_REFRESH = "functional.tooltip.refresh";
    public static final String FUNCTIONAL_DETAIL_INFO = "functional.detail.info";
    public static final String FUNCTIONAL_TAB_OVERVIEW = "functional.tab.overview";
    public static final String FUNCTIONAL_STATUS_READY = "functional.status.ready";
    public static final String FUNCTIONAL_STATUS_UPDATING = "functional.status.updating";
    public static final String FUNCTIONAL_STATUS_UPDATED = "functional.status.updated";
    public static final String FUNCTIONAL_STATUS_REFRESHING = "functional.status.refreshing";
    public static final String FUNCTIONAL_STATUS_REFRESHED = "functional.status.refreshed";
    public static final String FUNCTIONAL_STATUS_ITERATION_SELECTED = "functional.status.iteration_selected";
    public static final String FUNCTIONAL_STATUS_REQUEST_SELECTED = "functional.status.request_selected";
    public static final String FUNCTIONAL_STATUS_OVERVIEW_SELECTED = "functional.status.overview_selected";

    // ============ 功能测试详情页面相关 ============
    public static final String FUNCTIONAL_DETAIL_OVERVIEW = "functional.detail.overview";
    public static final String FUNCTIONAL_DETAIL_ITERATION = "functional.detail.iteration";
    public static final String FUNCTIONAL_DETAIL_EXECUTION_STATS = "functional.detail.execution_stats";
    public static final String FUNCTIONAL_DETAIL_ITERATION_INFO = "functional.detail.iteration_info";
    public static final String FUNCTIONAL_DETAIL_CSV_DATA = "functional.detail.csv_data";
    public static final String FUNCTIONAL_DETAIL_WELCOME_MESSAGE = "functional.detail.welcome_message";
    public static final String FUNCTIONAL_DETAIL_WELCOME_SUBTITLE = "functional.detail.welcome_subtitle";

    // ============ 功能测试统计相关 ============
    public static final String FUNCTIONAL_STATS_TOTAL_ITERATIONS = "functional.stats.total_iterations";
    public static final String FUNCTIONAL_STATS_TOTAL_REQUESTS = "functional.stats.total_requests";
    public static final String FUNCTIONAL_STATS_TOTAL_TIME = "functional.stats.total_time";
    public static final String FUNCTIONAL_STATS_SUCCESS_RATE = "functional.stats.success_rate";
    public static final String FUNCTIONAL_STATS_START_TIME = "functional.stats.start_time";
    public static final String FUNCTIONAL_STATS_END_TIME = "functional.stats.end_time";
    public static final String FUNCTIONAL_STATS_AVERAGE_TIME = "functional.stats.average_time";
    public static final String FUNCTIONAL_STATS_STATUS = "functional.stats.status";
    public static final String FUNCTIONAL_STATS_STATUS_COMPLETED = "functional.stats.status_completed";

    // ============ 功能测试表格相关 ============
    public static final String FUNCTIONAL_TABLE_ITERATION = "functional.table.iteration";
    public static final String FUNCTIONAL_TABLE_REQUEST_NAME = "functional.table.request_name";
    public static final String FUNCTIONAL_TABLE_METHOD = "functional.table.method";
    public static final String FUNCTIONAL_TABLE_STATUS = "functional.table.status";
    public static final String FUNCTIONAL_TABLE_TIME = "functional.table.time";
    public static final String FUNCTIONAL_TABLE_ASSERTION = "functional.table.assertion";
    public static final String FUNCTIONAL_TABLE_TIMESTAMP = "functional.table.timestamp";

    // ============ 功能测试迭代相关 ============
    public static final String FUNCTIONAL_ITERATION_ROUND = "functional.iteration.round";
    public static final String FUNCTIONAL_ITERATION_ROUND_FORMAT = "functional.iteration.round.format";
    public static final String FUNCTIONAL_ITERATION_START_TIME = "functional.iteration.start_time";
    public static final String FUNCTIONAL_ITERATION_EXECUTION_TIME = "functional.iteration.execution_time";
    public static final String FUNCTIONAL_ITERATION_REQUEST_COUNT = "functional.iteration.request_count";
    public static final String FUNCTIONAL_ITERATION_PASSED_FORMAT = "functional.iteration.passed_format";

    // ============ 线程组相关 ============
    // 线程组模式
    public static final String THREADGROUP_MODE_FIXED = "threadgroup.mode.fixed";
    public static final String THREADGROUP_MODE_RAMP_UP = "threadgroup.mode.ramp_up";
    public static final String THREADGROUP_MODE_SPIKE = "threadgroup.mode.spike";
    public static final String THREADGROUP_MODE_STAIRS = "threadgroup.mode.stairs";

    // 线程组界面标签
    public static final String THREADGROUP_MODE_LABEL = "threadgroup.mode.label";
    public static final String THREADGROUP_PREVIEW_TITLE = "threadgroup.preview.title";

    // 固定模式标签
    public static final String THREADGROUP_FIXED_USERS = "threadgroup.fixed.users";
    public static final String THREADGROUP_FIXED_EXECUTION_MODE = "threadgroup.fixed.execution_mode";
    public static final String THREADGROUP_FIXED_USE_TIME = "threadgroup.fixed.use_time";
    public static final String THREADGROUP_FIXED_LOOPS = "threadgroup.fixed.loops";
    public static final String THREADGROUP_FIXED_DURATION = "threadgroup.fixed.duration";

    // 递增模式标签
    public static final String THREADGROUP_RAMPUP_START_USERS = "threadgroup.rampup.start_users";
    public static final String THREADGROUP_RAMPUP_END_USERS = "threadgroup.rampup.end_users";
    public static final String THREADGROUP_RAMPUP_RAMP_TIME = "threadgroup.rampup.ramp_time";
    public static final String THREADGROUP_RAMPUP_TEST_DURATION = "threadgroup.rampup.test_duration";

    // 尖刺模式标签
    public static final String THREADGROUP_SPIKE_MIN_USERS = "threadgroup.spike.min_users";
    public static final String THREADGROUP_SPIKE_MAX_USERS = "threadgroup.spike.max_users";
    public static final String THREADGROUP_SPIKE_RAMP_UP_TIME = "threadgroup.spike.ramp_up_time";
    public static final String THREADGROUP_SPIKE_HOLD_TIME = "threadgroup.spike.hold_time";
    public static final String THREADGROUP_SPIKE_RAMP_DOWN_TIME = "threadgroup.spike.ramp_down_time";
    public static final String THREADGROUP_SPIKE_TEST_DURATION = "threadgroup.spike.test_duration";

    // 阶梯模式标签
    public static final String THREADGROUP_STAIRS_START_USERS = "threadgroup.stairs.start_users";
    public static final String THREADGROUP_STAIRS_END_USERS = "threadgroup.stairs.end_users";
    public static final String THREADGROUP_STAIRS_STEP_SIZE = "threadgroup.stairs.step_size";
    public static final String THREADGROUP_STAIRS_HOLD_TIME = "threadgroup.stairs.hold_time";
    public static final String THREADGROUP_STAIRS_TEST_DURATION = "threadgroup.stairs.test_duration";

    // 预览面板标签
    public static final String THREADGROUP_PREVIEW_TIME_SECONDS = "threadgroup.preview.time_seconds";
    public static final String THREADGROUP_PREVIEW_MODE_PREFIX = "threadgroup.preview.mode_prefix";

    // ============ Settings Dialog related ============
    // Dialog title and labels
    public static final String SETTINGS_DIALOG_TITLE = "settings.dialog.title";
    public static final String SETTINGS_DIALOG_SAVE = "settings.dialog.save";
    public static final String SETTINGS_DIALOG_CANCEL = "settings.dialog.cancel";

    // Request settings section
    public static final String SETTINGS_REQUEST_TITLE = "settings.request.title";
    public static final String SETTINGS_REQUEST_MAX_BODY_SIZE = "settings.request.max_body_size";
    public static final String SETTINGS_REQUEST_MAX_BODY_SIZE_TOOLTIP = "settings.request.max_body_size.tooltip";
    public static final String SETTINGS_REQUEST_TIMEOUT = "settings.request.timeout";
    public static final String SETTINGS_REQUEST_TIMEOUT_TOOLTIP = "settings.request.timeout.tooltip";
    public static final String SETTINGS_REQUEST_MAX_DOWNLOAD_SIZE = "settings.request.max_download_size";
    public static final String SETTINGS_REQUEST_MAX_DOWNLOAD_SIZE_TOOLTIP = "settings.request.max_download_size.tooltip";
    public static final String SETTINGS_REQUEST_FOLLOW_REDIRECTS = "settings.request.follow_redirects";
    public static final String SETTINGS_REQUEST_FOLLOW_REDIRECTS_TOOLTIP = "settings.request.follow_redirects.tooltip";
    public static final String SETTINGS_REQUEST_FOLLOW_REDIRECTS_CHECKBOX = "settings.request.follow_redirects.checkbox";

    // JMeter settings section
    public static final String SETTINGS_JMETER_TITLE = "settings.jmeter.title";
    public static final String SETTINGS_JMETER_MAX_IDLE = "settings.jmeter.max_idle";
    public static final String SETTINGS_JMETER_MAX_IDLE_TOOLTIP = "settings.jmeter.max_idle.tooltip";
    public static final String SETTINGS_JMETER_KEEP_ALIVE = "settings.jmeter.keep_alive";
    public static final String SETTINGS_JMETER_KEEP_ALIVE_TOOLTIP = "settings.jmeter.keep_alive.tooltip";

    // Download settings section
    public static final String SETTINGS_DOWNLOAD_TITLE = "settings.download.title";
    public static final String SETTINGS_DOWNLOAD_SHOW_PROGRESS = "settings.download.show_progress";
    public static final String SETTINGS_DOWNLOAD_SHOW_PROGRESS_TOOLTIP = "settings.download.show_progress.tooltip";
    public static final String SETTINGS_DOWNLOAD_THRESHOLD = "settings.download.threshold";
    public static final String SETTINGS_DOWNLOAD_THRESHOLD_TOOLTIP = "settings.download.threshold.tooltip";

    // General settings section
    public static final String SETTINGS_GENERAL_TITLE = "settings.general.title";
    public static final String SETTINGS_GENERAL_MAX_HISTORY = "settings.general.max_history";
    public static final String SETTINGS_GENERAL_MAX_HISTORY_TOOLTIP = "settings.general.max_history.tooltip";

    // Validation messages
    public static final String SETTINGS_VALIDATION_ERROR_TITLE = "settings.validation.error.title";
    public static final String SETTINGS_VALIDATION_MAX_BODY_SIZE_ERROR = "settings.validation.max_body_size.error";
    public static final String SETTINGS_VALIDATION_TIMEOUT_ERROR = "settings.validation.timeout.error";
    public static final String SETTINGS_VALIDATION_MAX_DOWNLOAD_SIZE_ERROR = "settings.validation.max_download_size.error";
    public static final String SETTINGS_VALIDATION_MAX_IDLE_ERROR = "settings.validation.max_idle.error";
    public static final String SETTINGS_VALIDATION_KEEP_ALIVE_ERROR = "settings.validation.keep_alive.error";
    public static final String SETTINGS_VALIDATION_THRESHOLD_ERROR = "settings.validation.threshold.error";
    public static final String SETTINGS_VALIDATION_MAX_HISTORY_ERROR = "settings.validation.max_history.error";
    public static final String SETTINGS_VALIDATION_INVALID_NUMBER = "settings.validation.invalid_number";

    // Success messages
    public static final String SETTINGS_SAVE_SUCCESS = "settings.save.success";
    public static final String SETTINGS_SAVE_SUCCESS_TITLE = "settings.save.success.title";

    // ============ 请求Body相关 ============
    public static final String REQUEST_BODY_TYPE = "request.body.type";
    public static final String REQUEST_BODY_FORMAT = "request.body.format";
    public static final String REQUEST_BODY_NONE = "request.body.none";
    public static final String REQUEST_BODY_SEND_MESSAGE = "request.body.send_message";
    public static final String REQUEST_BODY_FORMAT_ONLY_RAW = "request.body.format.only_raw";
    public static final String REQUEST_BODY_FORMAT_EMPTY = "request.body.format.empty";
    public static final String REQUEST_BODY_FORMAT_INVALID_JSON = "request.body.format.invalid_json";

    // ============ 响应头面板相关 ============
    public static final String RESPONSE_HEADERS_COPY_SELECTED = "response.headers.copy_selected";
    public static final String RESPONSE_HEADERS_COPY_CELL = "response.headers.copy_cell";
    public static final String RESPONSE_HEADERS_COPY_ALL = "response.headers.copy_all";
    public static final String RESPONSE_HEADERS_SELECT_ALL = "response.headers.select_all";

    // ============ CSV Data Panel related ============
    public static final String CSV_STATUS_NO_DATA = "csv.status.no_data";
    public static final String CSV_STATUS_LOADED = "csv.status.loaded";
    public static final String CSV_MANUAL_CREATED = "csv.manual_created";
    public static final String CSV_BUTTON_CLEAR_TOOLTIP = "csv.button.clear.tooltip";
    public static final String CSV_MENU_IMPORT_FILE = "csv.menu.import_file";
    public static final String CSV_MENU_MANAGE_DATA = "csv.menu.manage_data";
    public static final String CSV_MENU_CLEAR_DATA = "csv.menu.clear_data";
    public static final String CSV_DATA_CLEARED = "csv.data.cleared";
    public static final String CSV_DIALOG_MANAGEMENT_TITLE = "csv.dialog.management.title";
    public static final String CSV_DATA_DRIVEN_TEST = "csv.data_driven_test";
    public static final String CSV_DIALOG_DESCRIPTION = "csv.dialog.description";
    public static final String CSV_CURRENT_STATUS = "csv.current_status";
    public static final String CSV_OPERATIONS = "csv.operations";
    public static final String CSV_BUTTON_SELECT_FILE = "csv.button.select_file";
    public static final String CSV_BUTTON_MANAGE_DATA = "csv.button.manage_data";
    public static final String CSV_BUTTON_CLEAR_DATA = "csv.button.clear_data";
    public static final String CSV_NO_MANAGEABLE_DATA = "csv.no_manageable_data";
    public static final String CSV_DATA_MANAGEMENT = "csv.data_management";
    public static final String CSV_DATA_SOURCE_INFO = "csv.data_source_info";
    public static final String CSV_BUTTON_ADD_ROW = "csv.button.add_row";
    public static final String CSV_BUTTON_DELETE_ROW = "csv.button.delete_row";
    public static final String CSV_BUTTON_ADD_COLUMN = "csv.button.add_column";
    public static final String CSV_BUTTON_DELETE_COLUMN = "csv.button.delete_column";
    public static final String CSV_SELECT_ROWS_TO_DELETE = "csv.select_rows_to_delete";
    public static final String CSV_CONFIRM_DELETE_ROWS = "csv.confirm_delete_rows";
    public static final String CSV_CONFIRM_DELETE = "csv.confirm_delete";
    public static final String CSV_ENTER_COLUMN_NAME = "csv.enter_column_name";
    public static final String CSV_ADD_COLUMN = "csv.add_column";
    public static final String CSV_SELECT_COLUMNS_TO_DELETE = "csv.select_columns_to_delete";
    public static final String CSV_CANNOT_DELETE_ALL_COLUMNS = "csv.cannot_delete_all_columns";
    public static final String CSV_CONFIRM_DELETE_COLUMNS = "csv.confirm_delete_columns";
    public static final String CSV_USAGE_INSTRUCTIONS = "csv.usage_instructions";
    public static final String CSV_USAGE_TEXT = "csv.usage_text";
    public static final String CSV_NO_VALID_DATA_ROWS = "csv.no_valid_data_rows";
    public static final String CSV_DATA_SAVED = "csv.data_saved";
    public static final String CSV_SAVE_SUCCESS = "csv.save_success";
    public static final String CSV_SAVE_FAILED = "csv.save_failed";
    public static final String CSV_SELECT_FILE = "csv.select_file";
    public static final String CSV_FILE_FILTER = "csv.file_filter";
    public static final String CSV_FILE_VALIDATION_FAILED = "csv.file_validation_failed";
    public static final String CSV_NO_VALID_DATA = "csv.no_valid_data";
    public static final String CSV_LOAD_FAILED = "csv.load_failed";
    public static final String CSV_FILE_NOT_EXIST = "csv.file_not_exist";
    public static final String CSV_FILE_NOT_VALID = "csv.file_not_valid";
    public static final String CSV_FILE_NOT_CSV = "csv.file_not_csv";

    // ============ OkHttpResponseHandler ============
    public static final String DOWNLOAD_PROGRESS_TITLE = "download.progress.title";
    public static final String DOWNLOAD_CANCELLED = "download.cancelled";
    public static final String BINARY_TOO_LARGE = "binary.too_large";
    public static final String BINARY_TOO_LARGE_BODY = "binary.too_large.body";
    public static final String BINARY_SAVED_TEMP_FILE = "binary.saved_temp_file";
    public static final String NO_RESPONSE_BODY = "no.response.body";
    public static final String DOWNLOAD_LIMIT_TITLE = "download.limit.title";
    public static final String TEXT_TOO_LARGE = "text.too_large";
    public static final String TEXT_TOO_LARGE_BODY = "text.too_large.body";
    public static final String BODY_TOO_LARGE_SAVED = "body.too_large.saved";
    public static final String SSE_STREAM_UNSUPPORTED = "sse.stream.unsupported";

    // ============ ResponseAssertion 国际化 ============
    public static final String RESPONSE_ASSERTION_STATUS_FAILED = "response.assertion.status_failed";
    public static final String RESPONSE_ASSERTION_HEADER_NOT_FOUND = "response.assertion.header_not_found";
    public static final String RESPONSE_ASSERTION_HEADER_NOT_FOUND_WITH_NAME = "response.assertion.header_not_found_with_name";
    public static final String RESPONSE_ASSERTION_BELOW_FAILED = "response.assertion.below_failed";
    public static final String RESPONSE_ASSERTION_INVALID_JSON = "response.assertion.invalid_json";

    // ============ Expectation 国际化 ============
    public static final String EXPECTATION_INCLUDE_FAILED = "expectation.include_failed";
    public static final String EXPECTATION_EQL_FAILED = "expectation.eql_failed";
    public static final String EXPECTATION_PROPERTY_NOT_FOUND = "expectation.property_not_found";
    public static final String EXPECTATION_PROPERTY_NOT_MAP = "expectation.property_not_map";
    public static final String EXPECTATION_MATCH_REGEX_FAILED = "expectation.match_regex_failed";
    public static final String EXPECTATION_MATCH_PATTERN_FAILED = "expectation.match_pattern_failed";
    public static final String EXPECTATION_MATCH_JSREGEXP_FAILED = "expectation.match_jsregexp_failed";
    public static final String EXPECTATION_BELOW_FAILED = "expectation.below_failed";

    // ============ ScriptPanel AutoCompletion 国际化 ============
    public static final String AUTOCOMPLETE_PM = "autocomplete.pm";
    public static final String AUTOCOMPLETE_POSTMAN = "autocomplete.postman";
    public static final String AUTOCOMPLETE_REQUEST = "autocomplete.request";
    public static final String AUTOCOMPLETE_RESPONSE = "autocomplete.response";
    public static final String AUTOCOMPLETE_ENV = "autocomplete.env";
    public static final String AUTOCOMPLETE_RESPONSE_BODY = "autocomplete.response_body";
    public static final String AUTOCOMPLETE_RESPONSE_HEADERS = "autocomplete.response_headers";
    public static final String AUTOCOMPLETE_STATUS = "autocomplete.status";
    public static final String AUTOCOMPLETE_STATUS_CODE = "autocomplete.status_code";
    public static final String AUTOCOMPLETE_SET_ENV = "autocomplete.set_env";
    public static final String AUTOCOMPLETE_GET_ENV = "autocomplete.get_env";
    public static final String AUTOCOMPLETE_IF = "autocomplete.if";
    public static final String AUTOCOMPLETE_ELSE = "autocomplete.else";
    public static final String AUTOCOMPLETE_FOR = "autocomplete.for";
    public static final String AUTOCOMPLETE_WHILE = "autocomplete.while";
    public static final String AUTOCOMPLETE_FUNCTION = "autocomplete.function";
    public static final String AUTOCOMPLETE_RETURN = "autocomplete.return";
    public static final String AUTOCOMPLETE_SNIPPET_SET_ENV = "autocomplete.snippet.set_env";
    public static final String AUTOCOMPLETE_SNIPPET_GET_ENV = "autocomplete.snippet.get_env";
    public static final String AUTOCOMPLETE_SNIPPET_BTOA = "autocomplete.snippet.btoa";
    public static final String AUTOCOMPLETE_SNIPPET_ATOB = "autocomplete.snippet.atob";
    public static final String AUTOCOMPLETE_SNIPPET_ENCODE_URI = "autocomplete.snippet.encode_uri";
    public static final String AUTOCOMPLETE_SNIPPET_DECODE_URI = "autocomplete.snippet.decode_uri";
    public static final String AUTOCOMPLETE_SNIPPET_CONSOLE_LOG = "autocomplete.snippet.console_log";
    public static final String AUTOCOMPLETE_SNIPPET_JSON_PARSE = "autocomplete.snippet.json_parse";
    public static final String AUTOCOMPLETE_SNIPPET_JSON_STRINGIFY = "autocomplete.snippet.json_stringify";

}
