package com.laker.postman.service.apipost;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.swing.tree.DefaultMutableTreeNode;

import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpFormUrlencoded;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpParam;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestGroup;
import com.laker.postman.model.SavedResponse;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BASIC;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_BEARER;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_INHERIT;
import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.AUTH_TYPE_NONE;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * ApiPost Collection解析器
 * 负责解析ApiPost导出的JSON格式
 *
 * @author L.cm
 */
@Slf4j
public class ApiPostCollectionParser {
    // 常量定义
    private static final String GROUP = "group";
    private static final String REQUEST = "request";

    /**
     * 私有构造函数，防止实例化
     */
    private ApiPostCollectionParser() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 解析完整的 Apipost Collection JSON，返回根节点
     *
     * @param json Apipost Collection JSON 字符串
     * @return 集合根节点，如果解析失败返回 null。如果返回的节点 userObject 为 null，表示是容器节点，需要展开其子节点
     */
    public static DefaultMutableTreeNode parseApiPostCollection(String json) {
        try {
            JSONObject apipostRoot = JSONUtil.parseObj(json);
            
            // 验证是否为有效的Apipost格式
            if (!apipostRoot.containsKey("apis") || !apipostRoot.containsKey("name")) {
                return null;
            }

            String projectName = apipostRoot.getStr("name", "ApiPost");
            JSONArray apis = apipostRoot.getJSONArray("apis");
            if (apis == null || apis.isEmpty()) {
                return null;
            }

            // 构建ID到树节点的映射
            java.util.Map<String, DefaultMutableTreeNode> idToNodeMap = new java.util.HashMap<>();
            
            // 第一遍：创建所有文件夹节点
            for (Object apiObj : apis) {
                JSONObject api = (JSONObject) apiObj;
                String targetType = api.getStr("target_type", "");
                if ("folder".equals(targetType)) {
                    String targetId = api.getStr("target_id");
                    String folderName = api.getStr("name", "未命名文件夹");
                    RequestGroup group = new RequestGroup(folderName);
                    
                    // 解析文件夹级别的认证
                    JSONObject request = api.getJSONObject("request");
                    if (request != null) {
                        JSONObject auth = request.getJSONObject("auth");
                        if (auth != null) {
                            parseAuthToGroup(auth, group);
                        }
                    }
                    
                    DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(new Object[]{GROUP, group});
                    idToNodeMap.put(targetId, folderNode);
                }
            }
            
            // 第二遍：构建树形结构，收集顶级节点
            List<DefaultMutableTreeNode> topLevelNodes = new ArrayList<>();
            
            for (Object apiObj : apis) {
                JSONObject api = (JSONObject) apiObj;
                String targetId = api.getStr("target_id");
                String parentId = api.getStr("parent_id", "0");
                String targetType = api.getStr("target_type", "");
                
                DefaultMutableTreeNode currentNode = null;
                
                if ("folder".equals(targetType)) {
                    currentNode = idToNodeMap.get(targetId);
                } else if ("api".equals(targetType)) {
                    HttpRequestItem req = parseApiPostApi(api);
                    if (req != null) {
                        currentNode = new DefaultMutableTreeNode(new Object[]{REQUEST, req});
                        
                        // 为响应创建子节点
                        if (req.getResponse() != null && !req.getResponse().isEmpty()) {
                            for (SavedResponse savedResp : req.getResponse()) {
                                DefaultMutableTreeNode responseNode = new DefaultMutableTreeNode(
                                        new Object[]{"response", savedResp}
                                );
                                currentNode.add(responseNode);
                            }
                        }
                    }
                }
                
                if (currentNode != null) {
                    if ("0".equals(parentId)) {
                        // 顶级节点
                        topLevelNodes.add(currentNode);
                    } else {
                        // 查找父节点
                        DefaultMutableTreeNode parentNode = idToNodeMap.get(parentId);
                        if (parentNode != null) {
                            parentNode.add(currentNode);
                        } else {
                            // 如果找不到父节点，也作为顶级节点
                            topLevelNodes.add(currentNode);
                        }
                    }
                }
            }
            
            // 检查顶级节点中是否有文件夹
            boolean hasTopLevelFolder = false;
            for (DefaultMutableTreeNode node : topLevelNodes) {
                Object[] userObj = (Object[]) node.getUserObject();
                if (userObj != null && GROUP.equals(userObj[0])) {
                    hasTopLevelFolder = true;
                    break;
                }
            }
            
            // 如果没有顶级文件夹，只有API，则创建一个以项目名为名称的文件夹
            if (!hasTopLevelFolder && !topLevelNodes.isEmpty()) {
                RequestGroup projectGroup = new RequestGroup(projectName);
                
                // 解析全局认证（如果存在）
                JSONObject global = apipostRoot.getJSONObject("global");
                if (global != null) {
                    JSONObject globalAuth = global.getJSONObject("global_param");
                    if (globalAuth != null) {
                        JSONObject auth = globalAuth.getJSONObject("auth");
                        if (auth != null) {
                            parseAuthToGroup(auth, projectGroup);
                        }
                    }
                }
                
                DefaultMutableTreeNode projectFolderNode = new DefaultMutableTreeNode(new Object[]{GROUP, projectGroup});
                for (DefaultMutableTreeNode node : topLevelNodes) {
                    projectFolderNode.add(node);
                }
                return projectFolderNode;
            }
            
            // 如果有顶级文件夹
            if (topLevelNodes.size() == 1) {
                // 只有一个顶级节点，直接返回
                return topLevelNodes.get(0);
            } else if (topLevelNodes.size() > 1) {
                // 多个顶级节点，创建容器节点（userObject 为 null，用于标识需要展开）
                DefaultMutableTreeNode containerNode = new DefaultMutableTreeNode();
                for (DefaultMutableTreeNode node : topLevelNodes) {
                    containerNode.add(node);
                }
                return containerNode;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("解析Apipost Collection失败", e);
            return null;
        }
    }

    /**
     * 解析Apipost的auth到RequestGroup
     */
    private static void parseAuthToGroup(JSONObject auth, RequestGroup group) {
        String authType = auth.getStr("type", "");
        if ("basic".equals(authType)) {
            group.setAuthType(AUTH_TYPE_BASIC);
            JSONObject basic = auth.getJSONObject("basic");
            if (basic != null) {
                group.setAuthUsername(basic.getStr("username", ""));
                group.setAuthPassword(basic.getStr("password", ""));
            }
        } else if ("bearer".equals(authType)) {
            group.setAuthType(AUTH_TYPE_BEARER);
            JSONObject bearer = auth.getJSONObject("bearer");
            if (bearer != null) {
                group.setAuthToken(bearer.getStr("key", ""));
            }
        } else if ("inherit".equals(authType)) {
            group.setAuthType(AUTH_TYPE_INHERIT);
        } else {
            group.setAuthType(AUTH_TYPE_NONE);
        }
    }

    /**
     * 解析单个Apipost API为HttpRequestItem
     */
    private static HttpRequestItem parseApiPostApi(JSONObject api) {
        try {
            HttpRequestItem req = new HttpRequestItem();
            req.setId(UUID.randomUUID().toString());
            req.setName(api.getStr("name", "未命名接口"));
            req.setMethod(api.getStr("method", "GET"));
            req.setUrl(api.getStr("url", ""));

            JSONObject request = api.getJSONObject("request");
            if (request != null) {
                // 解析认证
                JSONObject auth = request.getJSONObject("auth");
                if (auth != null) {
                    String authType = auth.getStr("type", "");
                    if ("inherit".equals(authType)) {
                        req.setAuthType(AUTH_TYPE_INHERIT);
                    } else if ("noauth".equals(authType)) {
                        req.setAuthType(AUTH_TYPE_NONE);
                    } else if ("basic".equals(authType)) {
                        req.setAuthType(AUTH_TYPE_BASIC);
                        JSONObject basic = auth.getJSONObject("basic");
                        if (basic != null) {
                            req.setAuthUsername(basic.getStr("username", ""));
                            req.setAuthPassword(basic.getStr("password", ""));
                        }
                    } else if ("bearer".equals(authType)) {
                        req.setAuthType(AUTH_TYPE_BEARER);
                        JSONObject bearer = auth.getJSONObject("bearer");
                        if (bearer != null) {
                            req.setAuthToken(bearer.getStr("key", ""));
                        }
                    } else {
                        // 其他认证类型暂不支持，设为无认证
                        req.setAuthType(AUTH_TYPE_NONE);
                    }
                }

                // 解析请求头
                JSONObject header = request.getJSONObject("header");
                if (header != null) {
                    JSONArray parameters = header.getJSONArray("parameter");
                    if (parameters != null && !parameters.isEmpty()) {
                        List<HttpHeader> headersList = new ArrayList<>();
                        for (Object paramObj : parameters) {
                            JSONObject param = (JSONObject) paramObj;
                            boolean enabled = param.getInt("is_checked", 1) == 1;
                            headersList.add(new HttpHeader(enabled, param.getStr("key", ""), param.getStr("value", "")));
                        }
                        req.setHeadersList(headersList);
                    }
                }

                // 解析查询参数
                JSONObject query = request.getJSONObject("query");
                if (query != null) {
                    JSONArray parameters = query.getJSONArray("parameter");
                    if (parameters != null && !parameters.isEmpty()) {
                        List<HttpParam> paramsList = new ArrayList<>();
                        for (Object paramObj : parameters) {
                            JSONObject param = (JSONObject) paramObj;
                            boolean enabled = param.getInt("is_checked", 1) == 1;
                            paramsList.add(new HttpParam(enabled, param.getStr("key", ""), param.getStr("value", "")));
                        }
                        req.setParamsList(paramsList);
                    }
                }

                // 解析Cookie
                JSONObject cookie = request.getJSONObject("cookie");
                if (cookie != null) {
                    JSONArray parameters = cookie.getJSONArray("parameter");
                    if (parameters != null && !parameters.isEmpty()) {
                        // Cookie参数添加到请求头中
                        if (req.getHeadersList() == null) {
                            req.setHeadersList(new ArrayList<>());
                        }
                        List<String> cookiePairs = new ArrayList<>();
                        for (Object paramObj : parameters) {
                            JSONObject param = (JSONObject) paramObj;
                            boolean enabled = param.getInt("is_checked", 1) == 1;
                            if (enabled) {
                                cookiePairs.add(param.getStr("key", "") + "=" + param.getStr("value", ""));
                            }
                        }
                        if (!cookiePairs.isEmpty()) {
                            // 检查是否已存在Cookie头
                            boolean hasCookieHeader = false;
                            for (HttpHeader h : req.getHeadersList()) {
                                if ("Cookie".equalsIgnoreCase(h.getKey())) {
                                    h.setValue(h.getValue() + "; " + String.join("; ", cookiePairs));
                                    hasCookieHeader = true;
                                    break;
                                }
                            }
                            if (!hasCookieHeader) {
                                req.getHeadersList().add(new HttpHeader(true, "Cookie", String.join("; ", cookiePairs)));
                            }
                        }
                    }
                }

                // 解析请求体
                JSONObject body = request.getJSONObject("body");
                if (body != null) {
                    String mode = body.getStr("mode", "");
                    if ("urlencoded".equals(mode)) {
                        JSONArray parameters = body.getJSONArray("parameter");
                        if (parameters != null && !parameters.isEmpty()) {
                            List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
                            for (Object paramObj : parameters) {
                                JSONObject param = (JSONObject) paramObj;
                                boolean enabled = param.getInt("is_checked", 1) == 1;
                                urlencodedList.add(new HttpFormUrlencoded(enabled, param.getStr("key", ""), param.getStr("value", "")));
                            }
                            req.setUrlencodedList(urlencodedList);
                        }
                    } else if ("formdata".equals(mode)) {
                        JSONArray parameters = body.getJSONArray("parameter");
                        if (parameters != null && !parameters.isEmpty()) {
                            List<HttpFormData> formDataList = new ArrayList<>();
                            for (Object paramObj : parameters) {
                                JSONObject param = (JSONObject) paramObj;
                                boolean enabled = param.getInt("is_checked", 1) == 1;
                                String fieldType = param.getStr("field_type", "string");
                                String key = param.getStr("key", "");
                                
                                // 判断是文件还是文本
                                if ("file".equals(fieldType) || param.containsKey("file_name")) {
                                    String fileName = param.getStr("file_name", "");
                                    formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_FILE, fileName));
                                } else {
                                    String value = param.getStr("value", "");
                                    formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_TEXT, value));
                                }
                            }
                            req.setFormDataList(formDataList);
                        }
                    } else if ("raw".equals(mode)) {
                        req.setBody(body.getStr("raw", ""));
                    } else if ("binary".equals(mode)) {
                        // Binary模式暂不支持，设为空body
                        req.setBody("");
                    }
                }

                // 解析前置脚本和后置脚本
                // 注意：Apipost的脚本格式可能与Postman不同，这里暂时跳过解析
                // 如果后续需要支持，可以根据Apipost的实际脚本格式进行解析
                JSONArray preTasks = request.getJSONArray("pre_tasks");
                if (preTasks != null && !preTasks.isEmpty()) {
                    // TODO: 解析Apipost的前置脚本格式
                }

                JSONArray postTasks = request.getJSONArray("post_tasks");
                if (postTasks != null && !postTasks.isEmpty()) {
                    // TODO: 解析Apipost的后置脚本格式
                }
            }

            // 解析响应示例
            JSONObject response = api.getJSONObject("response");
            if (response != null) {
                JSONArray examples = response.getJSONArray("example");
                if (examples != null && !examples.isEmpty()) {
                    List<SavedResponse> savedResponsesList = new ArrayList<>();
                    for (Object exampleObj : examples) {
                        JSONObject example = (JSONObject) exampleObj;
                        SavedResponse savedResponse = parseApiPostResponse(example, req);
                        if (savedResponse != null) {
                            savedResponsesList.add(savedResponse);
                        }
                    }
                    req.setResponse(savedResponsesList);
                }
            }

            return req;
        } catch (Exception e) {
            log.error("解析Apipost API失败", e);
            return null;
        }
    }

    /**
     * 解析 Apipost 的单个响应示例
     */
    private static SavedResponse parseApiPostResponse(JSONObject exampleJson, HttpRequestItem request) {
        try {
            SavedResponse savedResponse = new SavedResponse();
            savedResponse.setId(UUID.randomUUID().toString());

            JSONObject expect = exampleJson.getJSONObject("expect");
            if (expect != null) {
                savedResponse.setName(expect.getStr("name", "Response"));
                String codeStr = expect.getStr("code", "200");
                try {
                    savedResponse.setCode(Integer.parseInt(codeStr));
                } catch (NumberFormatException e) {
                    savedResponse.setCode(200);
                }
                savedResponse.setStatus("OK");
                savedResponse.setPreviewLanguage(expect.getStr("content_type", "json"));
            } else {
                savedResponse.setName("Response");
                savedResponse.setCode(200);
                savedResponse.setStatus("OK");
                savedResponse.setPreviewLanguage("json");
            }

            savedResponse.setTimestamp(System.currentTimeMillis());

            // 解析响应头
            JSONArray headers = exampleJson.getJSONArray("headers");
            if (headers != null && !headers.isEmpty()) {
                List<HttpHeader> headersList = new ArrayList<>();
                for (Object h : headers) {
                    JSONObject hObj = (JSONObject) h;
                    headersList.add(new HttpHeader(true, hObj.getStr("key", ""), hObj.getStr("value", "")));
                }
                savedResponse.setHeaders(headersList);
            }

            // 解析响应体
            savedResponse.setBody(exampleJson.getStr("raw", ""));

            // 保存原始请求信息
            SavedResponse.OriginalRequest origReq = new SavedResponse.OriginalRequest();
            origReq.setMethod(request.getMethod());
            origReq.setUrl(request.getUrl());
            origReq.setHeaders(request.getHeadersList() != null ? new ArrayList<>(request.getHeadersList()) : new ArrayList<>());
            origReq.setParams(request.getParamsList() != null ? new ArrayList<>(request.getParamsList()) : new ArrayList<>());
            origReq.setBodyType(request.getBodyType());
            origReq.setBody(request.getBody());
            origReq.setFormDataList(request.getFormDataList() != null ? new ArrayList<>(request.getFormDataList()) : new ArrayList<>());
            origReq.setUrlencodedList(request.getUrlencodedList() != null ? new ArrayList<>(request.getUrlencodedList()) : new ArrayList<>());
            savedResponse.setOriginalRequest(origReq);

            return savedResponse;
        } catch (Exception e) {
            log.error("解析Apipost响应失败", e);
            return null;
        }
    }
}
