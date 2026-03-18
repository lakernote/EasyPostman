package com.laker.postman.plugin.redis;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContributionSupport;
import com.laker.postman.plugin.api.PluginContext;

import static com.laker.postman.plugin.redis.RedisI18n.t;

/**
 * Redis 官方插件。
 */
public class RedisPlugin implements EasyPostmanPlugin {

    private static final String TOOLBOX_GROUP_DATABASE = "toolbox.group.database";

    @Override
    public void onLoad(PluginContext context) {
        context.registerScriptApi("redis", ScriptRedisApi::new);
        PluginContributionSupport.registerToolbox(
                context,
                "redis",
                t(MessageKeys.TOOLBOX_REDIS),
                "icons/redis.svg",
                TOOLBOX_GROUP_DATABASE,
                t(TOOLBOX_GROUP_DATABASE),
                RedisPanel::new,
                RedisPlugin.class
        );
        context.registerScriptCompletionContributor(provider -> {
            PluginContributionSupport.addScriptApiCompletions(
                    provider,
                    "redis",
                    "Redis plugin API",
                    "execute",
                    "query"
            );
            PluginContributionSupport.addShorthandCompletion(
                    provider,
                    "redis.get",
                    """
                            const redis = pm.plugin("redis");
                            const value = redis.execute({
                              host: "localhost",
                              port: 6379,
                              db: 0,
                              command: "GET",
                              key: "user:1"
                            });
                            pm.test("Redis key exists", function () {
                              pm.expect(value).to.exist();
                            });""",
                    "Redis query + assert"
            );
        });
        PluginContributionSupport.registerExampleSnippet(
                context,
                t(MessageKeys.SNIPPET_EXAMPLE_REDIS_ASSERT_TITLE),
                t(MessageKeys.SNIPPET_EXAMPLE_REDIS_ASSERT_DESC),
                "// Redis 查询 + 断言\nvar redis = pm.plugin('redis');\nvar redisResult = redis.execute({\n    host: pm.environment.get('redisHost') || 'localhost',\n    port: parseInt(pm.environment.get('redisPort') || '6379', 10),\n    db: parseInt(pm.environment.get('redisDb') || '0', 10),\n    password: pm.environment.get('redisPassword') || '',\n    command: 'GET',\n    key: pm.environment.get('redisKey') || 'user:1'\n});\n\npm.test('Redis key exists', function () {\n    pm.expect(redisResult).to.exist();\n});\n\nconsole.log('Redis query result:', redisResult);"
        );
        PluginContributionSupport.registerExampleSnippet(
                context,
                t(MessageKeys.SNIPPET_EXAMPLE_REDIS_WRITE_ASSERT_TITLE),
                t(MessageKeys.SNIPPET_EXAMPLE_REDIS_WRITE_ASSERT_DESC),
                "// Redis 写入 + 断言\nvar redis = pm.plugin('redis');\nvar redisKey = pm.environment.get('redisKey') || 'order:1001';\nvar redisWriteResp = redis.execute({\n    host: pm.environment.get('redisHost') || 'localhost',\n    port: parseInt(pm.environment.get('redisPort') || '6379', 10),\n    db: parseInt(pm.environment.get('redisDb') || '0', 10),\n    password: pm.environment.get('redisPassword') || '',\n    command: 'SET',\n    key: redisKey,\n    value: JSON.stringify({ id: 1001, status: 'CREATED', source: 'easy-postman' })\n});\n\nvar redisValue = redis.query({\n    host: pm.environment.get('redisHost') || 'localhost',\n    port: parseInt(pm.environment.get('redisPort') || '6379', 10),\n    db: parseInt(pm.environment.get('redisDb') || '0', 10),\n    password: pm.environment.get('redisPassword') || '',\n    command: 'GET',\n    key: redisKey\n});\n\npm.test('Redis write success', function () {\n    pm.expect(redisWriteResp).to.equal('OK');\n    pm.expect(redisValue).to.include('CREATED');\n});\n\nconsole.log('Redis write result:', redisWriteResp);\nconsole.log('Redis read back value:', redisValue);"
        );
    }
}
