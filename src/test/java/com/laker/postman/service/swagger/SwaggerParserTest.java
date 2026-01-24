package com.laker.postman.service.swagger;

import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.service.common.CollectionParseResult;
import com.laker.postman.service.common.TreeNodeBuilder;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;

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
}

