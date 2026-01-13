package com.laker.postman.service.apipost;

import com.laker.postman.model.*;
import org.testng.annotations.Test;

import javax.swing.tree.DefaultMutableTreeNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;
import static org.testng.Assert.*;

/**
 * ApiPostCollectionParser 单元测试
 * 测试完整的 ApiPost Collection 解析功能
 */
public class ApiPostCollectionParserTest {

    /**
     * 读取测试文件内容
     */
    private String readTestFile(String filename) throws IOException {
        String path = "src/test/resources/" + filename;
        return Files.readString(Paths.get(path));
    }

    @Test
    public void testParseApiPostCollection_BasicStructure() throws IOException {
        // 读取测试文件
        String json = readTestFile("apipost-test-simple.json");

        // 执行解析
        DefaultMutableTreeNode collectionNode = ApiPostCollectionParser.parseApiPostCollection(json);

        // 验证根节点存在
        assertNotNull(collectionNode, "解析后的根节点不应为null");

        // 验证根节点是一个group（类似PostmanCollectionParser）
        Object[] userObject = (Object[]) collectionNode.getUserObject();
        assertEquals(userObject[0], "group", "根节点应该是group类型");
        assertTrue(userObject[1] instanceof RequestGroup, "根节点的userObject[1]应该是RequestGroup");

        RequestGroup rootGroup = (RequestGroup) userObject[1];
        assertEquals(rootGroup.getName(), "ApiPost测试集合", "根group名称应该是项目名称");
    }

    @Test
    public void testParseApiPostCollection_MixedTopLevelItems() throws IOException {
        // 读取测试文件（包含顶级API和顶级文件夹混合的情况）
        String json = readTestFile("apipost-test-simple.json");

        // 执行解析
        DefaultMutableTreeNode collectionNode = ApiPostCollectionParser.parseApiPostCollection(json);

        // 验证结构
        assertNotNull(collectionNode);

        // 验证根节点有子节点（包括顶级API和文件夹）
        assertTrue(collectionNode.getChildCount() > 0, "根节点应该有子节点");

        // 统计顶级API和文件夹数量
        int apiCount = 0;
        int folderCount = 0;

        for (int i = 0; i < collectionNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) collectionNode.getChildAt(i);
            Object[] childUserObj = (Object[]) child.getUserObject();
            if ("request".equals(childUserObj[0])) {
                apiCount++;
            } else if ("group".equals(childUserObj[0])) {
                folderCount++;
            }
        }

        // 根据测试数据：1个顶级API（获取用户列表）+ 2个顶级文件夹（用户管理、订单管理）
        assertEquals(apiCount, 1, "应该有1个顶级API");
        assertEquals(folderCount, 2, "应该有2个顶级文件夹");
    }

    @Test
    public void testParseApiPostCollection_TopLevelRequest() throws IOException {
        // 读取测试文件
        String json = readTestFile("apipost-test-simple.json");

        // 执行解析
        DefaultMutableTreeNode collectionNode = ApiPostCollectionParser.parseApiPostCollection(json);

        // 查找顶级请求（获取用户列表）
        DefaultMutableTreeNode topLevelRequest = null;
        for (int i = 0; i < collectionNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) collectionNode.getChildAt(i);
            Object[] userObj = (Object[]) child.getUserObject();
            if ("request".equals(userObj[0])) {
                HttpRequestItem req = (HttpRequestItem) userObj[1];
                if ("获取用户列表".equals(req.getName())) {
                    topLevelRequest = child;
                    break;
                }
            }
        }

        // 验证找到了顶级请求
        assertNotNull(topLevelRequest, "应该找到顶级请求");

        // 验证请求详情
        Object[] userObj = (Object[]) topLevelRequest.getUserObject();
        HttpRequestItem req = (HttpRequestItem) userObj[1];

        assertEquals(req.getName(), "获取用户列表");
        assertEquals(req.getMethod(), "GET");
        assertEquals(req.getUrl(), "https://api.example.com/users");

        // 验证认证
        assertEquals(req.getAuthType(), AUTH_TYPE_BASIC);
        assertEquals(req.getAuthUsername(), "admin");
        assertEquals(req.getAuthPassword(), "secret");

        // 验证请求头
        assertNotNull(req.getHeadersList());
        assertEquals(req.getHeadersList().size(), 1);
        assertEquals(req.getHeadersList().get(0).getKey(), "Content-Type");
        assertEquals(req.getHeadersList().get(0).getValue(), "application/json");

        // 验证查询参数
        assertNotNull(req.getParamsList());
        assertEquals(req.getParamsList().size(), 2);
        assertEquals(req.getParamsList().get(0).getKey(), "page");
        assertEquals(req.getParamsList().get(0).getValue(), "1");
    }

    @Test
    public void testParseApiPostCollection_FolderStructure() throws IOException {
        // 读取测试文件
        String json = readTestFile("apipost-test-simple.json");

        // 执行解析
        DefaultMutableTreeNode collectionNode = ApiPostCollectionParser.parseApiPostCollection(json);

        // 查找"用户管理"文件夹
        DefaultMutableTreeNode userFolder = null;
        for (int i = 0; i < collectionNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) collectionNode.getChildAt(i);
            Object[] userObj = (Object[]) child.getUserObject();
            if ("group".equals(userObj[0])) {
                RequestGroup group = (RequestGroup) userObj[1];
                if ("用户管理".equals(group.getName())) {
                    userFolder = child;
                    break;
                }
            }
        }

        // 验证找到了文件夹
        assertNotNull(userFolder, "应该找到'用户管理'文件夹");

        // 验证文件夹有子节点
        assertEquals(userFolder.getChildCount(), 2, "用户管理文件夹应该有2个子请求");

        // 验证文件夹认证类型
        Object[] folderUserObj = (Object[]) userFolder.getUserObject();
        RequestGroup userGroup = (RequestGroup) folderUserObj[1];
        assertEquals(userGroup.getAuthType(), AUTH_TYPE_INHERIT);
    }

    @Test
    public void testParseApiPostCollection_NestedRequest() throws IOException {
        // 读取测试文件
        String json = readTestFile("apipost-test-simple.json");

        // 执行解析
        DefaultMutableTreeNode collectionNode = ApiPostCollectionParser.parseApiPostCollection(json);

        // 查找"用户管理"文件夹
        DefaultMutableTreeNode userFolder = null;
        for (int i = 0; i < collectionNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) collectionNode.getChildAt(i);
            Object[] userObj = (Object[]) child.getUserObject();
            if ("group".equals(userObj[0])) {
                RequestGroup group = (RequestGroup) userObj[1];
                if ("用户管理".equals(group.getName())) {
                    userFolder = child;
                    break;
                }
            }
        }

        assertNotNull(userFolder);

        // 查找"创建用户"请求
        DefaultMutableTreeNode createUserNode = (DefaultMutableTreeNode) userFolder.getChildAt(0);
        Object[] userObj = (Object[]) createUserNode.getUserObject();
        assertEquals(userObj[0], "request");

        HttpRequestItem createUserReq = (HttpRequestItem) userObj[1];
        assertEquals(createUserReq.getName(), "创建用户");
        assertEquals(createUserReq.getMethod(), "POST");
        assertEquals(createUserReq.getUrl(), "https://api.example.com/users");

        // 验证请求体
        assertEquals(createUserReq.getBody(), "{\"name\":\"John\",\"email\":\"john@example.com\"}");

        // 验证认证继承
        assertEquals(createUserReq.getAuthType(), AUTH_TYPE_INHERIT);

        // 验证响应示例
        assertNotNull(createUserReq.getResponse());
        assertEquals(createUserReq.getResponse().size(), 1);
        SavedResponse savedResponse = createUserReq.getResponse().get(0);
        assertEquals(savedResponse.getName(), "成功响应");
        assertEquals(savedResponse.getCode(), 200);
        assertEquals(savedResponse.getBody(), "{\"id\":123,\"name\":\"John\",\"email\":\"john@example.com\"}");

        // 验证响应有一个子节点
        assertEquals(createUserNode.getChildCount(), 1, "请求节点应该有1个响应子节点");
        DefaultMutableTreeNode responseNode = (DefaultMutableTreeNode) createUserNode.getChildAt(0);
        Object[] responseUserObj = (Object[]) responseNode.getUserObject();
        assertEquals(responseUserObj[0], "response");
    }

    @Test
    public void testParseApiPostCollection_UrlencodedBody() throws IOException {
        // 读取测试文件
        String json = readTestFile("apipost-test-simple.json");

        // 执行解析
        DefaultMutableTreeNode collectionNode = ApiPostCollectionParser.parseApiPostCollection(json);

        // 查找"用户管理"文件夹中的"更新用户"请求
        DefaultMutableTreeNode userFolder = null;
        for (int i = 0; i < collectionNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) collectionNode.getChildAt(i);
            Object[] userObj = (Object[]) child.getUserObject();
            if ("group".equals(userObj[0])) {
                RequestGroup group = (RequestGroup) userObj[1];
                if ("用户管理".equals(group.getName())) {
                    userFolder = child;
                    break;
                }
            }
        }

        assertNotNull(userFolder);

        // 获取第二个子节点（更新用户）
        DefaultMutableTreeNode updateUserNode = (DefaultMutableTreeNode) userFolder.getChildAt(1);
        Object[] userObj = (Object[]) updateUserNode.getUserObject();
        HttpRequestItem updateUserReq = (HttpRequestItem) userObj[1];

        assertEquals(updateUserReq.getName(), "更新用户");
        assertEquals(updateUserReq.getMethod(), "PUT");

        // 验证Bearer认证
        assertEquals(updateUserReq.getAuthType(), AUTH_TYPE_BEARER);
        assertEquals(updateUserReq.getAuthToken(), "{{token}}");

        // 验证urlencoded请求体
        assertNotNull(updateUserReq.getUrlencodedList());
        assertEquals(updateUserReq.getUrlencodedList().size(), 2);

        HttpFormUrlencoded field1 = updateUserReq.getUrlencodedList().get(0);
        assertTrue(field1.isEnabled());
        assertEquals(field1.getKey(), "name");
        assertEquals(field1.getValue(), "Jane");

        HttpFormUrlencoded field2 = updateUserReq.getUrlencodedList().get(1);
        assertEquals(field2.getKey(), "email");
        assertEquals(field2.getValue(), "jane@example.com");
    }

    @Test
    public void testParseApiPostCollection_FolderAuth() throws IOException {
        // 读取测试文件
        String json = readTestFile("apipost-test-simple.json");

        // 执行解析
        DefaultMutableTreeNode collectionNode = ApiPostCollectionParser.parseApiPostCollection(json);

        // 查找"订单管理"文件夹
        DefaultMutableTreeNode orderFolder = null;
        for (int i = 0; i < collectionNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) collectionNode.getChildAt(i);
            Object[] userObj = (Object[]) child.getUserObject();
            if ("group".equals(userObj[0])) {
                RequestGroup group = (RequestGroup) userObj[1];
                if ("订单管理".equals(group.getName())) {
                    orderFolder = child;
                    break;
                }
            }
        }

        assertNotNull(orderFolder, "应该找到'订单管理'文件夹");

        // 验证文件夹级别的Bearer认证
        Object[] folderUserObj = (Object[]) orderFolder.getUserObject();
        RequestGroup orderGroup = (RequestGroup) folderUserObj[1];
        assertEquals(orderGroup.getAuthType(), AUTH_TYPE_BEARER);
        assertEquals(orderGroup.getAuthToken(), "test-token-123");

        // 验证子请求
        assertEquals(orderFolder.getChildCount(), 1);
        DefaultMutableTreeNode queryOrderNode = (DefaultMutableTreeNode) orderFolder.getChildAt(0);
        Object[] requestUserObj = (Object[]) queryOrderNode.getUserObject();
        HttpRequestItem queryOrderReq = (HttpRequestItem) requestUserObj[1];

        assertEquals(queryOrderReq.getName(), "查询订单");
        assertEquals(queryOrderReq.getMethod(), "GET");

        // 验证继承父级认证
        assertEquals(queryOrderReq.getAuthType(), AUTH_TYPE_INHERIT);
    }

    @Test
    public void testParseApiPostCollection_CookieParameter() throws IOException {
        // 读取测试文件
        String json = readTestFile("apipost-test-simple.json");

        // 执行解析
        DefaultMutableTreeNode collectionNode = ApiPostCollectionParser.parseApiPostCollection(json);

        // 查找"订单管理"文件夹中的"查询订单"请求
        DefaultMutableTreeNode orderFolder = null;
        for (int i = 0; i < collectionNode.getChildCount(); i++) {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) collectionNode.getChildAt(i);
            Object[] userObj = (Object[]) child.getUserObject();
            if ("group".equals(userObj[0])) {
                RequestGroup group = (RequestGroup) userObj[1];
                if ("订单管理".equals(group.getName())) {
                    orderFolder = child;
                    break;
                }
            }
        }

        assertNotNull(orderFolder);

        DefaultMutableTreeNode queryOrderNode = (DefaultMutableTreeNode) orderFolder.getChildAt(0);
        Object[] userObj = (Object[]) queryOrderNode.getUserObject();
        HttpRequestItem queryOrderReq = (HttpRequestItem) userObj[1];

        // 验证Cookie已添加到请求头
        assertNotNull(queryOrderReq.getHeadersList());
        boolean hasCookieHeader = false;
        for (HttpHeader header : queryOrderReq.getHeadersList()) {
            if ("Cookie".equalsIgnoreCase(header.getKey())) {
                hasCookieHeader = true;
                assertEquals(header.getValue(), "sessionId=abc123");
                break;
            }
        }
        assertTrue(hasCookieHeader, "应该有Cookie请求头");
    }

    @Test
    public void testParseApiPostCollection_InvalidJson() {
        // 测试无效JSON
        String invalidJson = "{invalid json}";
        DefaultMutableTreeNode result = ApiPostCollectionParser.parseApiPostCollection(invalidJson);
        assertNull(result, "无效JSON应该返回null");
    }

    @Test
    public void testParseApiPostCollection_MissingRequiredFields() {
        // 测试缺少必需字段的JSON
        String json = """
                {
                    "project_id": "test"
                }
                """;
        DefaultMutableTreeNode result = ApiPostCollectionParser.parseApiPostCollection(json);
        assertNull(result, "缺少必需字段的JSON应该返回null");
    }

    @Test
    public void testParseApiPostCollection_EmptyApis() {
        // 测试空apis数组
        String json = """
                {
                    "project_id": "test",
                    "name": "Empty Collection",
                    "apis": []
                }
                """;
        DefaultMutableTreeNode result = ApiPostCollectionParser.parseApiPostCollection(json);
        assertNull(result, "空apis数组应该返回null");
    }
}

