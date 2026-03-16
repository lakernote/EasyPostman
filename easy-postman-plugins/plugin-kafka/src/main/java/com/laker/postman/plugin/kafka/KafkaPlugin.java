package com.laker.postman.plugin.kafka;

import com.laker.postman.plugin.api.EasyPostmanPlugin;
import com.laker.postman.plugin.api.PluginContext;
import com.laker.postman.plugin.api.SnippetDefinition;
import com.laker.postman.plugin.api.ToolboxContribution;
import com.laker.postman.util.I18nUtil;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.ShorthandCompletion;

public class KafkaPlugin implements EasyPostmanPlugin {

    private static final String TOOLBOX_GROUP_DATABASE = "toolbox.group.database";

    @Override
    public void onLoad(PluginContext context) {
        context.registerScriptApi("kafka", ScriptKafkaApi::new);
        context.registerToolboxContribution(new ToolboxContribution(
                "kafka",
                t("toolbox.kafka"),
                "icons/kafka.svg",
                TOOLBOX_GROUP_DATABASE,
                t(TOOLBOX_GROUP_DATABASE),
                KafkaPanel::new,
                KafkaPlugin.class.getClassLoader()
        ));
        context.registerScriptCompletionContributor(provider -> {
            provider.addCompletion(new BasicCompletion(provider, "pm.plugin", "pm.plugin(alias)"));
            provider.addCompletion(new BasicCompletion(provider, "pm.plugin(\"kafka\")", "Kafka plugin API"));
            provider.addCompletion(new BasicCompletion(provider, "pm.plugin(\"kafka\").listTopics",
                    "pm.plugin(\"kafka\").listTopics(options)"));
            provider.addCompletion(new BasicCompletion(provider, "pm.plugin(\"kafka\").send",
                    "pm.plugin(\"kafka\").send(options)"));
            provider.addCompletion(new BasicCompletion(provider, "pm.plugin(\"kafka\").poll",
                    "pm.plugin(\"kafka\").poll(options)"));
            provider.addCompletion(new BasicCompletion(provider, "pm.kafka", "Kafka script API"));
            provider.addCompletion(new BasicCompletion(provider, "pm.kafka.listTopics", "pm.kafka.listTopics(options)"));
            provider.addCompletion(new BasicCompletion(provider, "pm.kafka.send", "pm.kafka.send(options)"));
            provider.addCompletion(new BasicCompletion(provider, "pm.kafka.poll", "pm.kafka.poll(options)"));
            provider.addCompletion(new ShorthandCompletion(provider, "kafka.poll",
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
                    "Kafka poll + assert"));
        });
        context.registerSnippet(new SnippetDefinition(
                "EXAMPLES",
                t("snippet.exampleKafkaAssert.title"),
                t("snippet.exampleKafkaAssert.desc"),
                "// Kafka 查询 + 断言\nvar records = pm.kafka.poll({\n    bootstrapServers: pm.environment.get('kafkaBootstrap') || 'localhost:9092',\n    topic: pm.environment.get('kafkaTopic') || 'demo-topic',\n    groupId: 'easy-postman-script-' + Date.now(),\n    autoOffsetReset: 'earliest',\n    pollTimeoutMs: 1000,\n    maxMessages: 20\n});\n\npm.test('Kafka records should not be empty', function () {\n    pm.expect(records.length).to.be.above(0);\n});\n\nconsole.log('Kafka records count:', records.length);"
        ));
        context.registerSnippet(new SnippetDefinition(
                "EXAMPLES",
                t("snippet.exampleKafkaSendAssert.title"),
                t("snippet.exampleKafkaSendAssert.desc"),
                "// Kafka 发送 + 断言\nvar kafkaTopic = pm.environment.get('kafkaTopic') || 'demo-topic';\nvar kafkaResp = pm.kafka.send({\n    bootstrapServers: pm.environment.get('kafkaBootstrap') || 'localhost:9092',\n    topic: kafkaTopic,\n    key: 'order-1001',\n    value: JSON.stringify({ orderId: 1001, status: 'CREATED', source: 'easy-postman' })\n});\n\npm.test('Kafka send success', function () {\n    pm.expect(kafkaResp.topic).to.equal(kafkaTopic);\n    pm.expect(kafkaResp.offset).to.be.least(0);\n});\n\nconsole.log('Kafka send metadata:', JSON.stringify(kafkaResp));"
        ));
    }

    private static String t(String key, Object... args) {
        return I18nUtil.getMessage(key, args);
    }
}
