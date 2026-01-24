package com.laker.postman.service.swagger;

import com.laker.postman.model.Environment;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.service.common.CollectionParseResult;
import com.laker.postman.service.common.TreeNodeBuilder;
import org.testng.annotations.Test;
import com.laker.postman.service.EnvironmentService;

import javax.swing.tree.DefaultMutableTreeNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BEARER;
import static org.testng.Assert.*;


/**
 * Swagger解析器测试
 */
public class SwaggerParserTest {

    @Test
    public void testParseSwagger2() {
        String swagger2Json = """
                {
                  "swagger": "2.0",
                  "info": {
                    "title": "Pet Store API",
                    "version": "1.0.0"
                  },
                  "host": "petstore.swagger.io",
                  "basePath": "/v2",
                  "schemes": ["https"],
                  "paths": {
                    "/pets": {
                      "get": {
                        "tags": ["Pets"],
                        "summary": "List all pets",
                        "operationId": "listPets",
                        "parameters": [
                          {
                            "name": "limit",
                            "in": "query",
                            "type": "integer",
                            "default": 10
                          }
                        ],
                        "responses": {
                          "200": {
                            "description": "Success"
                          }
                        }
                      },
                      "post": {
                        "tags": ["Pets"],
                        "summary": "Create a pet",
                        "operationId": "createPet",
                        "parameters": [
                          {
                            "name": "body",
                            "in": "body",
                            "schema": {
                              "type": "object",
                              "properties": {
                                "name": {
                                  "type": "string"
                                },
                                "age": {
                                  "type": "integer"
                                }
                              }
                            }
                          }
                        ],
                        "responses": {
                          "201": {
                            "description": "Created"
                          }
                        }
                      }
                    }
                  }
                }
                """;

        CollectionParseResult parseResult = SwaggerParser.parseSwagger(swagger2Json);
        assertNotNull(parseResult);

        DefaultMutableTreeNode collectionNode = TreeNodeBuilder.buildFromParseResult(parseResult);

        assertNotNull(collectionNode);
        Object[] userObject = (Object[]) collectionNode.getUserObject();
        assertEquals(userObject[0], "group");

        RequestGroup group = (RequestGroup) userObject[1];
        assertEquals(group.getName(), "Pet Store API (1.0.0)");

        // 应该有一个Pets分组
        assertEquals(collectionNode.getChildCount(), 1);
        DefaultMutableTreeNode petsGroup = (DefaultMutableTreeNode) collectionNode.getChildAt(0);
        Object[] petsUserObject = (Object[]) petsGroup.getUserObject();
        assertEquals(petsUserObject[0], "group");

        // Pets分组应该有2个请求
        assertEquals(petsGroup.getChildCount(), 2);

        // 验证第一个请求 (GET /pets)
        DefaultMutableTreeNode getRequest = (DefaultMutableTreeNode) petsGroup.getChildAt(0);
        Object[] getRequestUserObject = (Object[]) getRequest.getUserObject();
        assertEquals(getRequestUserObject[0], "request");

        HttpRequestItem getItem = (HttpRequestItem) getRequestUserObject[1];
        assertEquals(getItem.getName(), "List all pets");
        assertEquals(getItem.getMethod(), "GET");
        assertTrue(getItem.getUrl().contains("/pets"));
        assertNotNull(getItem.getParamsList());
        assertEquals(getItem.getParamsList().size(), 1);
        assertEquals(getItem.getParamsList().get(0).getKey(), "limit");

        // 验证第二个请求 (POST /pets)
        DefaultMutableTreeNode postRequest = (DefaultMutableTreeNode) petsGroup.getChildAt(1);
        Object[] postRequestUserObject = (Object[]) postRequest.getUserObject();
        assertEquals(postRequestUserObject[0], "request");

        HttpRequestItem postItem = (HttpRequestItem) postRequestUserObject[1];
        assertEquals(postItem.getName(), "Create a pet");
        assertEquals(postItem.getMethod(), "POST");
        assertTrue(postItem.getUrl().contains("/pets"));
        assertNotNull(postItem.getBody());
    }

    @Test
    void testParseOpenAPI3() {
        String openapi3Json = """
                {
                  "openapi": "3.0.0",
                  "info": {
                    "title": "User API",
                    "version": "2.0.0"
                  },
                  "servers": [
                    {
                      "url": "https://api.example.com/v1"
                    }
                  ],
                  "paths": {
                    "/users": {
                      "get": {
                        "tags": ["Users"],
                        "summary": "Get all users",
                        "parameters": [
                          {
                            "name": "page",
                            "in": "query",
                            "schema": {
                              "type": "integer",
                              "default": 1
                            }
                          }
                        ],
                        "responses": {
                          "200": {
                            "description": "Success"
                          }
                        }
                      },
                      "post": {
                        "tags": ["Users"],
                        "summary": "Create user",
                        "requestBody": {
                          "content": {
                            "application/json": {
                              "schema": {
                                "type": "object",
                                "properties": {
                                  "username": {
                                    "type": "string"
                                  },
                                  "email": {
                                    "type": "string",
                                    "format": "email"
                                  }
                                }
                              }
                            }
                          }
                        },
                        "responses": {
                          "201": {
                            "description": "Created"
                          }
                        }
                      }
                    }
                  }
                }
                """;

        CollectionParseResult parseResult = SwaggerParser.parseSwagger(openapi3Json);
        assertNotNull(parseResult);

        DefaultMutableTreeNode collectionNode = TreeNodeBuilder.buildFromParseResult(parseResult);

        assertNotNull(collectionNode);
        Object[] userObject = (Object[]) collectionNode.getUserObject();
        assertEquals(userObject[0], "group");

        RequestGroup group = (RequestGroup) userObject[1];
        assertEquals(group.getName(), "User API (2.0.0)");

        // 应该有一个Users分组
        assertEquals(collectionNode.getChildCount(), 1);
        DefaultMutableTreeNode usersGroup = (DefaultMutableTreeNode) collectionNode.getChildAt(0);

        // Users分组应该有2个请求
        assertEquals(usersGroup.getChildCount(), 2);

        // 验证第一个请求 (GET /users)
        DefaultMutableTreeNode getRequest = (DefaultMutableTreeNode) usersGroup.getChildAt(0);
        Object[] getRequestUserObject = (Object[]) getRequest.getUserObject();
        HttpRequestItem getItem = (HttpRequestItem) getRequestUserObject[1];
        assertEquals(getItem.getName(), "Get all users");
        assertEquals(getItem.getMethod(), "GET");
        // 验证 URL 使用了 {{baseUrl}} 变量引用
        assertTrue(getItem.getUrl().contains("{{baseUrl}}"), "URL 应该使用 {{baseUrl}} 变量");
        assertTrue(getItem.getUrl().contains("/users"));

        // 验证第二个请求 (POST /users)
        DefaultMutableTreeNode postRequest = (DefaultMutableTreeNode) usersGroup.getChildAt(1);
        Object[] postRequestUserObject = (Object[]) postRequest.getUserObject();
        HttpRequestItem postItem = (HttpRequestItem) postRequestUserObject[1];
        assertEquals(postItem.getName(), "Create user");
        assertEquals(postItem.getMethod(), "POST");
        assertNotNull(postItem.getBody());
    }

    @Test
    void testParseInvalidSwagger() {
        String invalidJson = """
                {
                  "invalid": "format"
                }
                """;

        CollectionParseResult parseResult = SwaggerParser.parseSwagger(invalidJson);
        assertNull(parseResult);
    }

    @Test
    void testParseSwagger2WithAuth() {
        String swagger2Json = """
                {
                  "swagger": "2.0",
                  "info": {
                    "title": "Secure API",
                    "version": "1.0.0"
                  },
                  "host": "api.example.com",
                  "basePath": "/v1",
                  "securityDefinitions": {
                    "bearerAuth": {
                      "type": "apiKey",
                      "name": "Authorization",
                      "in": "header"
                    }
                  },
                  "paths": {
                    "/protected": {
                      "get": {
                        "tags": ["Secure"],
                        "summary": "Protected endpoint",
                        "security": [
                          {
                            "bearerAuth": []
                          }
                        ],
                        "responses": {
                          "200": {
                            "description": "Success"
                          }
                        }
                      }
                    }
                  }
                }
                """;

        CollectionParseResult parseResult = SwaggerParser.parseSwagger(swagger2Json);
        assertNotNull(parseResult);

        DefaultMutableTreeNode collectionNode = TreeNodeBuilder.buildFromParseResult(parseResult);
        assertNotNull(collectionNode);

        DefaultMutableTreeNode secureGroup = (DefaultMutableTreeNode) collectionNode.getChildAt(0);
        DefaultMutableTreeNode getRequest = (DefaultMutableTreeNode) secureGroup.getChildAt(0);
        Object[] requestUserObject = (Object[]) getRequest.getUserObject();
        HttpRequestItem item = (HttpRequestItem) requestUserObject[1];

        // 应该设置了Bearer认证
        assertEquals(item.getAuthType(), AUTH_TYPE_BEARER);
    }

    /**
     * 测试解析服务并保存到环境
     */
    @Test
    void testParseServersOpenAPI3() {
        // 从resources目录读取zfile.openapi.json文件
        String openapiJson;
        try (var stream = getClass().getResourceAsStream("/zfile.openapi.json")) {
            if (stream == null) {
                throw new RuntimeException("无法找到资源文件: zfile.openapi.json");
            }
            openapiJson = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取zfile.openapi.json文件时发生错误", e);
        }

        System.out.println("开始解析OpenAPI文件...");
        CollectionParseResult parseResult = SwaggerParser.parseSwagger(openapiJson);
        assertNotNull(parseResult);

        DefaultMutableTreeNode collectionNode = TreeNodeBuilder.buildFromParseResult(parseResult);

        if (collectionNode != null) {
            System.out.println("OpenAPI解析成功！");
        } else {
            System.out.println("OpenAPI解析失败！");
            return;
        }

        // 检查环境变量是否被创建
        System.out.println("\n检查创建的环境变量：");
        List<Environment> environments = EnvironmentService.getAllEnvironments();
        for (Environment env : environments) {
            System.out.println("\n环境名称：" + env.getName());
            System.out.println("环境ID：" + env.getId());
            System.out.println("是否激活：" + env.isActive());
            System.out.println("变量列表：");
            for (var var : env.getVariables().entrySet()) {
                System.out.println("  " + var.getKey() + " = " + var.getValue());
            }
        }
    }

    @Test
    void testOpenAPI3WithEnvironments() {
        String openapi3Json = """
                {
                  "openapi": "3.0.1",
                  "info": {
                    "title": "Test API",
                    "version": "1.0.0"
                  },
                  "servers": [
                    {
                      "url": "http://localhost:8080",
                      "description": "本地环境"
                    },
                    {
                      "url": "http://dev.example.com",
                      "description": "开发环境"
                    },
                    {
                      "url": "http://prod.example.com",
                      "description": "生产环境"
                    }
                  ],
                  "paths": {
                    "/users": {
                      "get": {
                        "tags": ["Users"],
                        "summary": "Get users",
                        "responses": {
                          "200": {
                            "description": "Success"
                          }
                        }
                      }
                    }
                  }
                }
                """;

        CollectionParseResult parseResult = SwaggerParser.parseSwagger(openapi3Json);
        assertNotNull(parseResult);

        // 验证解析出了3个环境
        assertEquals(parseResult.getEnvironments().size(), 3);

        // 验证第一个环境
        Environment env1 = parseResult.getEnvironments().get(0);
        assertEquals(env1.getName(), "Test API - 本地环境");
        assertEquals(env1.getVariableList().size(), 1);
        assertEquals(env1.getVariableList().get(0).getKey(), "baseUrl");
        assertEquals(env1.getVariableList().get(0).getValue(), "http://localhost:8080");

        // 验证第二个环境
        Environment env2 = parseResult.getEnvironments().get(1);
        assertEquals(env2.getName(), "Test API - 开发环境");
        assertEquals(env2.getVariableList().get(0).getValue(), "http://dev.example.com");

        // 验证第三个环境
        Environment env3 = parseResult.getEnvironments().get(2);
        assertEquals(env3.getName(), "Test API - 生产环境");
        assertEquals(env3.getVariableList().get(0).getValue(), "http://prod.example.com");

        // 验证请求 URL 使用了 {{baseUrl}} 变量引用
        DefaultMutableTreeNode collectionNode = TreeNodeBuilder.buildFromParseResult(parseResult);
        DefaultMutableTreeNode usersGroup = (DefaultMutableTreeNode) collectionNode.getChildAt(0);
        DefaultMutableTreeNode getUserRequest = (DefaultMutableTreeNode) usersGroup.getChildAt(0);
        Object[] requestUserObject = (Object[]) getUserRequest.getUserObject();
        HttpRequestItem requestItem = (HttpRequestItem) requestUserObject[1];

        // 验证 URL 格式为 {{baseUrl}}/users
        assertEquals(requestItem.getUrl(), "{{baseUrl}}/users");
        System.out.println("✅ URL 使用变量引用: " + requestItem.getUrl());
    }
}

