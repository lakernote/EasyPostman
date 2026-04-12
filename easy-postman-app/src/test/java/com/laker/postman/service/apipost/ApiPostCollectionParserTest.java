package com.laker.postman.service.apipost;

import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.service.common.CollectionParseResult;
import org.testng.annotations.Test;

import static com.laker.postman.panel.collections.right.request.sub.RequestBodyPanel.BODY_TYPE_RAW;
import static org.testng.Assert.*;

/**
 * ApiPostCollectionParser 单元测试
 * 测试 ApiPost Collection 解析功能
 */
public class ApiPostCollectionParserTest {

    @Test
    public void testParseApiPostCollection_BasicStructure() {
        String json = """
                {
                    "name": "Test API",
                    "apis": [
                        {
                            "target_id": "req1",
                            "target_type": "api",
                            "parent_id": "0",
                            "name": "Get User",
                            "method": "GET",
                            "url": "https://api.example.com/user"
                        }
                    ]
                }
                """;

        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);

        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        var requestNode = result.getChildren().get(0);
        assertTrue(requestNode.isRequest());
        assertEquals(requestNode.asRequest().getName(), "Get User");
        assertEquals(requestNode.asRequest().getMethod(), "GET");
    }

    @Test
    public void testParseApiPostCollection_WithFolder() {
        String json = """
                {
                    "name": "Test Collection",
                    "apis": [
                        {
                            "target_id": "folder1",
                            "target_type": "folder",
                            "parent_id": "0",
                            "name": "User API"
                        },
                        {
                            "target_id": "req1",
                            "target_type": "api",
                            "parent_id": "folder1",
                            "name": "Get User",
                            "method": "GET",
                            "url": "https://api.example.com/user"
                        }
                    ]
                }
                """;

        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);

        assertNotNull(result);
        assertEquals(result.getChildren().size(), 1);

        var folderNode = result.getChildren().get(0);
        assertTrue(folderNode.isGroup());
        assertEquals(folderNode.asGroup().getName(), "User API");
        assertEquals(folderNode.getChildren().size(), 1);
    }

    @Test
    public void testParseApiPostCollection_InvalidJson() {
        String json = "invalid json";
        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);
        assertNull(result);
    }

    @Test
    public void testParseApiPostCollection_MissingApis() {
        String json = """
                {
                    "name": "Test"
                }
                """;
        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);
        assertNull(result);
    }

    @Test
    public void testParseApiPostCollection_EmptyApis() {
        String json = """
                {
                    "name": "Empty Collection",
                    "apis": []
                }
                """;
        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);
        assertNull(result);
    }

    @Test
    public void testParseApiPostCollection_WithScriptsAndEnvironments() {
        String json = """
                {
                  "name": "ApiPost Demo",
                  "global": {
                    "envs": [
                      {
                        "name": "开发环境",
                        "server_list": [
                          {
                            "uri": "https://dev.example.com"
                          }
                        ],
                        "env_var_list": {
                          "token": "abc"
                        }
                      }
                    ]
                  },
                  "apis": [
                    {
                      "target_id": "req1",
                      "target_type": "api",
                      "parent_id": "0",
                      "name": "Create User",
                      "method": "POST",
                      "url": "https://api.example.com/users",
                      "description": "create user desc",
                      "request": {
                        "pre_tasks": [
                          {
                            "script": "pm.environment.set('token', 'abc');"
                          }
                        ],
                        "post_tasks": [
                          {
                            "code": "pm.test('ok', function () {});"
                          }
                        ],
                        "body": {
                          "mode": "raw",
                          "raw": "{\\"name\\":\\"Alice\\"}"
                        },
                        "header": {
                          "parameter": []
                        },
                        "query": {
                          "parameter": []
                        },
                        "cookie": {
                          "parameter": []
                        }
                      }
                    }
                  ]
                }
                """;

        CollectionParseResult result = ApiPostCollectionParser.parseApiPostCollection(json);
        assertNotNull(result);
        assertEquals(result.getEnvironments().size(), 1);

        Environment env = result.getEnvironments().get(0);
        assertEquals(env.getName(), "开发环境");
        assertEquals(env.getVariable("baseUrl"), "https://dev.example.com");
        assertEquals(env.getVariable("token"), "abc");

        HttpRequestItem request = result.getChildren().get(0).asRequest();
        assertEquals(request.getDescription(), "create user desc");
        assertEquals(request.getBodyType(), BODY_TYPE_RAW);
        assertEquals(request.getBody(), "{\"name\":\"Alice\"}");
        assertEquals(request.getPrescript(), "pm.environment.set('token', 'abc');");
        assertEquals(request.getPostscript(), "pm.test('ok', function () {});");
    }
}
