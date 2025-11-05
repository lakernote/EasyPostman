package com.laker.postman.service.postman;

import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;

import static org.testng.Assert.*;

/**
 * PostmanCollectionParser 单元测试
 * 测试完整的 Postman Collection 解析功能
 */
public class PostmanCollectionParserTest {

    @Test
    public void testParsePostmanCollection_BasicCollection() {
        // 准备测试数据 - 基本的 Collection
        String json = """
                {
                    "info": {
                        "name": "Test Collection",
                        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
                    },
                    "item": [
                        {
                            "name": "Get Users",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com/users"
                            }
                        }
                    ]
                }
                """;

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果
        assertNotNull(collectionNode);
        Object[] userObject = (Object[]) collectionNode.getUserObject();
        assertEquals(userObject[0], "group");
        // userObject[1] 现在是 RequestGroup 对象
        assertTrue(userObject[1] instanceof com.laker.postman.model.RequestGroup);
        com.laker.postman.model.RequestGroup group = (com.laker.postman.model.RequestGroup) userObject[1];
        assertEquals(group.getName(), "Test Collection");
        assertEquals(collectionNode.getChildCount(), 1);
    }

    @Test
    public void testParsePostmanCollection_WithFolder() {
        // 准备测试数据 - 包含文件夹的 Collection
        String json = """
                {
                    "info": {
                        "name": "API Collection",
                        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
                    },
                    "item": [
                        {
                            "name": "User API",
                            "item": [
                                {
                                    "name": "Get User",
                                    "request": {
                                        "method": "GET",
                                        "url": "https://api.example.com/user/1"
                                    }
                                },
                                {
                                    "name": "Create User",
                                    "request": {
                                        "method": "POST",
                                        "url": "https://api.example.com/user"
                                    }
                                }
                            ]
                        }
                    ]
                }
                """;

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果
        assertNotNull(collectionNode);
        assertEquals(collectionNode.getChildCount(), 1);

        DefaultMutableTreeNode folderNode = (DefaultMutableTreeNode) collectionNode.getChildAt(0);
        Object[] folderUserObject = (Object[]) folderNode.getUserObject();
        assertEquals(folderUserObject[0], "group");
        // folderUserObject[1] 现在是 RequestGroup 对象
        assertTrue(folderUserObject[1] instanceof com.laker.postman.model.RequestGroup);
        com.laker.postman.model.RequestGroup folderGroup = (com.laker.postman.model.RequestGroup) folderUserObject[1];
        assertEquals(folderGroup.getName(), "User API");
        assertEquals(folderNode.getChildCount(), 2);
    }

    @Test
    public void testParsePostmanCollection_ComplexStructure() {
        // 准备测试数据 - 复杂的嵌套结构
        String json = """
                {
                    "info": {
                        "name": "Complex Collection"
                    },
                    "item": [
                        {
                            "name": "Auth",
                            "item": [
                                {
                                    "name": "Login",
                                    "request": {
                                        "method": "POST",
                                        "url": "https://api.example.com/login",
                                        "auth": {
                                            "type": "basic",
                                            "basic": [
                                                {"key": "username", "value": "admin"},
                                                {"key": "password", "value": "secret"}
                                            ]
                                        }
                                    }
                                }
                            ]
                        },
                        {
                            "name": "Direct Request",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com/public"
                            }
                        }
                    ]
                }
                """;

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果
        assertNotNull(collectionNode);
        assertEquals(collectionNode.getChildCount(), 2);

        // 验证第一个子节点（文件夹）
        DefaultMutableTreeNode authFolder = (DefaultMutableTreeNode) collectionNode.getChildAt(0);
        Object[] authUserObject = (Object[]) authFolder.getUserObject();
        assertEquals(authUserObject[0], "group");
        // authUserObject[1] 现在是 RequestGroup 对象
        assertTrue(authUserObject[1] instanceof com.laker.postman.model.RequestGroup);
        com.laker.postman.model.RequestGroup authGroup = (com.laker.postman.model.RequestGroup) authUserObject[1];
        assertEquals(authGroup.getName(), "Auth");
        assertEquals(authFolder.getChildCount(), 1);

        // 验证第二个子节点（请求）
        DefaultMutableTreeNode directRequest = (DefaultMutableTreeNode) collectionNode.getChildAt(1);
        Object[] requestUserObject = (Object[]) directRequest.getUserObject();
        assertEquals(requestUserObject[0], "request");
    }

    @Test
    public void testParsePostmanCollection_WithQueryParams() {
        // 准备测试数据 - 带查询参数的请求
        String json = """
                {
                    "info": {
                        "name": "Query Test"
                    },
                    "item": [
                        {
                            "name": "Search API",
                            "request": {
                                "method": "GET",
                                "url": {
                                    "raw": "https://api.example.com/search?q=test&limit=10",
                                    "query": [
                                        {"key": "q", "value": "test", "disabled": false},
                                        {"key": "limit", "value": "10", "disabled": false}
                                    ]
                                }
                            }
                        }
                    ]
                }
                """;

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果
        assertNotNull(collectionNode);
        assertEquals(collectionNode.getChildCount(), 1);

        DefaultMutableTreeNode requestNode = (DefaultMutableTreeNode) collectionNode.getChildAt(0);
        Object[] userObject = (Object[]) requestNode.getUserObject();
        assertEquals(userObject[0], "request");

        // 验证 HttpRequestItem
        com.laker.postman.model.HttpRequestItem req = (com.laker.postman.model.HttpRequestItem) userObject[1];
        assertEquals(req.getName(), "Search API");
        assertEquals(req.getMethod(), "GET");
        assertEquals(req.getUrl(), "https://api.example.com/search?q=test&limit=10");
        assertNotNull(req.getParamsList());
        assertEquals(req.getParamsList().size(), 2);
    }

    @Test
    public void testParsePostmanCollection_WithHeaders() {
        // 准备测试数据 - 带请求头的请求
        String json = """
                {
                    "info": {
                        "name": "Header Test"
                    },
                    "item": [
                        {
                            "name": "API Request",
                            "request": {
                                "method": "POST",
                                "url": "https://api.example.com/data",
                                "header": [
                                    {"key": "Content-Type", "value": "application/json", "disabled": false},
                                    {"key": "Authorization", "value": "Bearer token", "disabled": false}
                                ]
                            }
                        }
                    ]
                }
                """;

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果
        assertNotNull(collectionNode);
        DefaultMutableTreeNode requestNode = (DefaultMutableTreeNode) collectionNode.getChildAt(0);
        Object[] userObject = (Object[]) requestNode.getUserObject();
        com.laker.postman.model.HttpRequestItem req = (com.laker.postman.model.HttpRequestItem) userObject[1];

        assertNotNull(req.getHeadersList());
        assertEquals(req.getHeadersList().size(), 2);
        assertEquals(req.getHeadersList().get(0).getKey(), "Content-Type");
        assertEquals(req.getHeadersList().get(0).getValue(), "application/json");
    }

    @Test
    public void testParsePostmanCollection_WithScripts() {
        // 准备测试数据 - 带脚本的请求
        String json = """
                {
                    "info": {
                        "name": "Script Test"
                    },
                    "item": [
                        {
                            "name": "API With Scripts",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com/data"
                            },
                            "event": [
                                {
                                    "listen": "prerequest",
                                    "script": {
                                        "exec": ["console.log('before');"]
                                    }
                                },
                                {
                                    "listen": "test",
                                    "script": {
                                        "exec": ["pm.test('Status is 200', function() {", "    pm.response.to.have.status(200);", "});"]
                                    }
                                }
                            ]
                        }
                    ]
                }
                """;

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果
        assertNotNull(collectionNode);
        DefaultMutableTreeNode requestNode = (DefaultMutableTreeNode) collectionNode.getChildAt(0);
        Object[] userObject = (Object[]) requestNode.getUserObject();
        com.laker.postman.model.HttpRequestItem req = (com.laker.postman.model.HttpRequestItem) userObject[1];

        assertNotNull(req.getPrescript());
        assertTrue(req.getPrescript().contains("console.log('before')"));

        assertNotNull(req.getPostscript());
        assertTrue(req.getPostscript().contains("Status is 200"));
    }

    @Test
    public void testParsePostmanCollection_InvalidJson() {
        // 准备测试数据 - 无效的 JSON
        String json = "invalid json";

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果 - 应该返回 null
        assertNull(collectionNode);
    }

    @Test
    public void testParsePostmanCollection_MissingInfo() {
        // 准备测试数据 - 缺少 info 字段
        String json = """
                {
                    "item": [
                        {
                            "name": "Test",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com"
                            }
                        }
                    ]
                }
                """;

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果 - 应该返回 null
        assertNull(collectionNode);
    }

    @Test
    public void testParsePostmanCollection_MissingItem() {
        // 准备测试数据 - 缺少 item 字段
        String json = """
                {
                    "info": {
                        "name": "Test Collection"
                    }
                }
                """;

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果 - 应该返回 null
        assertNull(collectionNode);
    }

    @Test
    public void testParsePostmanCollection_EmptyCollection() {
        // 准备测试数据 - 空的 Collection
        String json = """
                {
                    "info": {
                        "name": "Empty Collection"
                    },
                    "item": []
                }
                """;

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果
        assertNotNull(collectionNode);
        Object[] userObject = (Object[]) collectionNode.getUserObject();
        // userObject[1] 现在是 RequestGroup 对象
        assertTrue(userObject[1] instanceof com.laker.postman.model.RequestGroup);
        com.laker.postman.model.RequestGroup group = (com.laker.postman.model.RequestGroup) userObject[1];
        assertEquals(group.getName(), "Empty Collection");
        assertEquals(collectionNode.getChildCount(), 0);
    }

    @Test
    public void testParsePostmanCollection_NoCollectionName() {
        // 准备测试数据 - 无 collection 名称
        String json = """
                {
                    "info": {
                        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
                    },
                    "item": [
                        {
                            "name": "Test",
                            "request": {
                                "method": "GET",
                                "url": "https://api.example.com"
                            }
                        }
                    ]
                }
                """;

        // 执行解析
        DefaultMutableTreeNode collectionNode = PostmanCollectionParser.parsePostmanCollection(json);

        // 验证结果 - 应该使用默认名称
        assertNotNull(collectionNode);
        Object[] userObject = (Object[]) collectionNode.getUserObject();
        // userObject[1] 现在是 RequestGroup 对象
        assertTrue(userObject[1] instanceof com.laker.postman.model.RequestGroup);
        com.laker.postman.model.RequestGroup group = (com.laker.postman.model.RequestGroup) userObject[1];
        assertEquals(group.getName(), "Postman");
    }
}

