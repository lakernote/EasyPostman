package com.laker.postman.plugin.redis;

final class MessageKeys {

    private MessageKeys() {
    }

    static final String SNIPPET_EXAMPLE_REDIS_ASSERT_TITLE = "snippet.exampleRedisAssert.title";
    static final String SNIPPET_EXAMPLE_REDIS_ASSERT_DESC = "snippet.exampleRedisAssert.desc";
    static final String SNIPPET_EXAMPLE_REDIS_WRITE_ASSERT_TITLE = "snippet.exampleRedisWriteAssert.title";
    static final String SNIPPET_EXAMPLE_REDIS_WRITE_ASSERT_DESC = "snippet.exampleRedisWriteAssert.desc";

    static final String TOOLBOX_REDIS = "toolbox.redis";
    static final String TOOLBOX_REDIS_HOST = "toolbox.redis.host";
    static final String TOOLBOX_REDIS_HOST_PLACEHOLDER = "toolbox.redis.host.placeholder";
    static final String TOOLBOX_REDIS_PORT = "toolbox.redis.port";
    static final String TOOLBOX_REDIS_DB = "toolbox.redis.db";
    static final String TOOLBOX_REDIS_USER = "toolbox.redis.user";
    static final String TOOLBOX_REDIS_USER_PLACEHOLDER = "toolbox.redis.user.placeholder";
    static final String TOOLBOX_REDIS_PASS = "toolbox.redis.pass";
    static final String TOOLBOX_REDIS_PASS_PLACEHOLDER = "toolbox.redis.pass.placeholder";
    static final String TOOLBOX_REDIS_CONNECT = "toolbox.redis.connect";
    static final String TOOLBOX_REDIS_DISCONNECT = "toolbox.redis.disconnect";
    static final String TOOLBOX_REDIS_CONNECT_SUCCESS = "toolbox.redis.connect.success";
    static final String TOOLBOX_REDIS_DISCONNECT_SUCCESS = "toolbox.redis.disconnect.success";
    static final String TOOLBOX_REDIS_STATUS_NOT_CONNECTED = "toolbox.redis.status.not_connected";
    static final String TOOLBOX_REDIS_STATUS_CONNECTED = "toolbox.redis.status.connected";
    static final String TOOLBOX_REDIS_STATUS_CONNECTED_SIMPLE = "toolbox.redis.status.connected.simple";
    static final String TOOLBOX_REDIS_STATUS_CONNECTING = "toolbox.redis.status.connecting";
    static final String TOOLBOX_REDIS_STATUS_CONNECT_FAILED = "toolbox.redis.status.connect_failed";
    static final String TOOLBOX_REDIS_STATUS_LOADING_KEYS = "toolbox.redis.status.loading_keys";
    static final String TOOLBOX_REDIS_STATUS_KEYS_LOADED = "toolbox.redis.status.keys_loaded";
    static final String TOOLBOX_REDIS_STATUS_EXECUTING = "toolbox.redis.status.executing";
    static final String TOOLBOX_REDIS_STATUS_OK = "toolbox.redis.status.ok";
    static final String TOOLBOX_REDIS_STATUS_ERROR = "toolbox.redis.status.error";
    static final String TOOLBOX_REDIS_KEYS_MANAGEMENT = "toolbox.redis.keys.management";
    static final String TOOLBOX_REDIS_KEYS_SEARCH_PLACEHOLDER = "toolbox.redis.keys.search.placeholder";
    static final String TOOLBOX_REDIS_KEYS_REFRESH = "toolbox.redis.keys.refresh";
    static final String TOOLBOX_REDIS_KEY_COPY = "toolbox.redis.key.copy";
    static final String TOOLBOX_REDIS_KEY_QUICK_READ = "toolbox.redis.key.quick_read";
    static final String TOOLBOX_REDIS_KEY_DELETE = "toolbox.redis.key.delete";
    static final String TOOLBOX_REDIS_KEY_DELETE_CONFIRM = "toolbox.redis.key.delete.confirm";
    static final String TOOLBOX_REDIS_KEY_DELETE_BATCH_CONFIRM = "toolbox.redis.key.delete.batch.confirm";
    static final String TOOLBOX_REDIS_KEY_DELETE_CONFIRM_TITLE = "toolbox.redis.key.delete.confirm.title";
    static final String TOOLBOX_REDIS_KEY_DELETE_SUCCESS = "toolbox.redis.key.delete.success";
    static final String TOOLBOX_REDIS_HISTORY = "toolbox.redis.history";
    static final String TOOLBOX_REDIS_HISTORY_CLEAR = "toolbox.redis.history.clear";
    static final String TOOLBOX_REDIS_HISTORY_EMPTY = "toolbox.redis.history.empty";
    static final String TOOLBOX_REDIS_LOAD_TEMPLATE = "toolbox.redis.load_template";
    static final String TOOLBOX_REDIS_EXECUTE = "toolbox.redis.execute";
    static final String TOOLBOX_REDIS_COMMAND = "toolbox.redis.command";
    static final String TOOLBOX_REDIS_KEY = "toolbox.redis.key";
    static final String TOOLBOX_REDIS_ARGS = "toolbox.redis.args";
    static final String TOOLBOX_REDIS_KEY_PLACEHOLDER = "toolbox.redis.key.placeholder";
    static final String TOOLBOX_REDIS_ARGS_PLACEHOLDER = "toolbox.redis.args.placeholder";
    static final String TOOLBOX_REDIS_VALUE_TITLE = "toolbox.redis.value.title";
    static final String TOOLBOX_REDIS_RESPONSE_TITLE = "toolbox.redis.response.title";
    static final String TOOLBOX_REDIS_FORMAT_JSON = "toolbox.redis.format_json";
    static final String TOOLBOX_REDIS_ERR_HOST_REQUIRED = "toolbox.redis.err.host_required";
    static final String TOOLBOX_REDIS_ERR_KEY_REQUIRED = "toolbox.redis.err.key_required";
    static final String TOOLBOX_REDIS_ERR_NOT_CONNECTED = "toolbox.redis.err.not_connected";
    static final String TOOLBOX_REDIS_ERR_CONNECT_FAILED = "toolbox.redis.err.connect_failed";
    static final String TOOLBOX_REDIS_ERR_EXECUTE_FAILED = "toolbox.redis.err.execute_failed";
    static final String TOOLBOX_REDIS_ERR_SET_VALUE_REQUIRED = "toolbox.redis.err.set_value_required";
    static final String TOOLBOX_REDIS_ERR_ARG_REQUIRED = "toolbox.redis.err.arg_required";
    static final String TOOLBOX_REDIS_ERR_INVALID_JSON = "toolbox.redis.err.invalid_json";
    static final String TOOLBOX_REDIS_ERR_UNSUPPORTED_COMMAND = "toolbox.redis.err.unsupported_command";
    static final String TOOLBOX_REDIS_TPL_GET = "toolbox.redis.tpl.get";
    static final String TOOLBOX_REDIS_TPL_SET_JSON = "toolbox.redis.tpl.set_json";
    static final String TOOLBOX_REDIS_TPL_HGETALL = "toolbox.redis.tpl.hgetall";
    static final String TOOLBOX_REDIS_TPL_LRANGE = "toolbox.redis.tpl.lrange";
    static final String TOOLBOX_REDIS_TPL_SMEMBERS = "toolbox.redis.tpl.smembers";
    static final String TOOLBOX_REDIS_TPL_ZRANGE = "toolbox.redis.tpl.zrange";
    static final String TOOLBOX_REDIS_TPL_TTL = "toolbox.redis.tpl.ttl";
    static final String TOOLBOX_REDIS_TPL_DEL = "toolbox.redis.tpl.del";
    static final String TOOLBOX_REDIS_TTL_PERMANENT = "toolbox.redis.ttl.permanent";
    static final String TOOLBOX_REDIS_TTL_EXPIRED = "toolbox.redis.ttl.expired";
}
