package com.laker.postman.util;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * JsonUtilTest
 **/
public class JsonUtilTest {

    @Test
    public void testIsTypeJSON() {
        String json = "123123{}[]";
        boolean isJson = JsonUtil.isTypeJSON(json);
        Assert.assertFalse(isJson);
    }

    @Test
    public void testCleanJsonComments() {
        String json = "{\n" +
                "    \"fileIds\": \"\",\n" +
                "    \"name\": \"测试新增20251023\",  // 测试任务\n" +
                "    \"complexType\": \"1\",\n" +
                "    \"taskPriority\": \"medium\",\n" +
                "    \"taskType\": \"2\",\n" +
                "    \"workerCode\": \"PER009574\",\n" +
                "    \"note\": \"\",\n" +
                "    \"deadLine\": \"2025-09-30\",\n" +
                "    \"startTime\": \"2025-09-01\",\n" +
                "    \"link\": \"http://www.baidu.com\",\n" +
                "    \"urgencyLevel\": \"critical\"\n" +
                "}";
        String cleanedJsonComments = JsonUtil.cleanJsonComments(json);
        System.out.println(cleanedJsonComments);
    }

}
