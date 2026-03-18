package com.laker.postman.plugin.kafka;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContributionSupport;
import com.laker.postman.plugin.api.PluginContext;

import static com.laker.postman.plugin.kafka.KafkaI18n.t;

public class KafkaPlugin implements EasyPostmanPlugin {

    @Override
    public void onLoad(PluginContext context) {
        // 插件入口类本身不做复杂业务，它的核心任务是把“Kafka 能力清单”注册给宿主。
        // 宿主后面会分别把这些能力接到脚本、Toolbox、补全和 Snippet 等不同位置。
        context.registerScriptApi("kafka", ScriptKafkaApi::new);
        PluginContributionSupport.registerToolbox(
                context,
                "kafka",
                t(MessageKeys.TOOLBOX_KAFKA),
                "icons/kafka.svg",
                MessageKeys.TOOLBOX_GROUP_DATABASE,
                t(MessageKeys.TOOLBOX_GROUP_DATABASE),
                KafkaPanel::new,
                KafkaPlugin.class
        );
        context.registerScriptCompletionContributor(provider -> {
            PluginContributionSupport.addScriptApiCompletions(
                    provider,
                    "kafka",
                    "Kafka script API",
                    "listTopics",
                    "send",
                    "poll"
            );
            PluginContributionSupport.addShorthandCompletion(
                    provider,
                    "kafka.poll",
                    """
                    const records = pm.kafka.poll({
                      bootstrapServers: "localhost:9092",
                      topic: "demo-topic",
                      groupId: "easy-postman-script",
                      autoOffsetReset: "earliest",
                      pollTimeoutMs: 1000
                    });
                    pm.test("Kafka has records", function () {
                      pm.expect(records.length).to.be.above(0);
                    });""",
                    "Kafka poll + assert"
            );
        });
        PluginContributionSupport.registerExampleSnippet(
                context,
                t(MessageKeys.SNIPPET_EXAMPLE_KAFKA_ASSERT_TITLE),
                t(MessageKeys.SNIPPET_EXAMPLE_KAFKA_ASSERT_DESC),
                "// Kafka 查询 + 断言\nvar records = pm.kafka.poll({\n    bootstrapServers: pm.environment.get('kafkaBootstrap') || 'localhost:9092',\n    topic: pm.environment.get('kafkaTopic') || 'demo-topic',\n    groupId: 'easy-postman-script-' + Date.now(),\n    autoOffsetReset: 'earliest',\n    pollTimeoutMs: 1000,\n    maxMessages: 20\n});\n\npm.test('Kafka records should not be empty', function () {\n    pm.expect(records.length).to.be.above(0);\n});\n\nconsole.log('Kafka records count:', records.length);"
        );
        PluginContributionSupport.registerExampleSnippet(
                context,
                t(MessageKeys.SNIPPET_EXAMPLE_KAFKA_SEND_ASSERT_TITLE),
                t(MessageKeys.SNIPPET_EXAMPLE_KAFKA_SEND_ASSERT_DESC),
                "// Kafka 发送 + 断言\nvar kafkaTopic = pm.environment.get('kafkaTopic') || 'demo-topic';\nvar kafkaResp = pm.kafka.send({\n    bootstrapServers: pm.environment.get('kafkaBootstrap') || 'localhost:9092',\n    topic: kafkaTopic,\n    key: 'order-1001',\n    value: JSON.stringify({ orderId: 1001, status: 'CREATED', source: 'easy-postman' })\n});\n\npm.test('Kafka send success', function () {\n    pm.expect(kafkaResp.topic).to.equal(kafkaTopic);\n    pm.expect(kafkaResp.offset).to.be.least(0);\n});\n\nconsole.log('Kafka send metadata:', JSON.stringify(kafkaResp));"
        );
    }
}
