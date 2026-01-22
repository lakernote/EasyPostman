package com.laker.postman.service.apipost;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.laker.postman.model.*;
import com.laker.postman.service.common.AuthParserUtil;
import com.laker.postman.service.common.CollectionNode;
import com.laker.postman.service.common.CollectionParseResult;
import com.laker.postman.service.common.NodeType;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

import static com.laker.postman.panel.collections.right.request.sub.AuthTabPanel.*;

/**
 * ApiPost Collection解析器
 * 负责解析ApiPost导出的JSON格式
 * 此类只负责解析，不涉及 UI 层的 TreeNode 组装
 *
 * @author L.cm
 */
@Slf4j
@UtilityClass
public class ApiPostCollectionParser {

    // 常量定义
    private static final String KEY_PARAMETER = "parameter";
    private static final String KEY_IS_CHECKED = "is_checked";
    private static final String KEY_VALUE = "value";

    /**
     * 解析完整的 Apipost Collection JSON，返回解析结果
     *
     * @param json Apipost Collection JSON 字符串
     * @return 解析结果，如果解析失败返回 null
     */
    public static CollectionParseResult parseApiPostCollection(String json) {
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

            // 构建ID到节点的映射
            Map<String, CollectionNode> idToNodeMap = new HashMap<>();

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
                            AuthParserUtil.parseApiPostAuthToGroup(auth, group);
                        }
                    }

                    CollectionNode folderNode = new CollectionNode(NodeType.GROUP, group);
                    idToNodeMap.put(targetId, folderNode);
                }
            }

            // 第二遍：构建树形结构，收集顶级节点
            List<CollectionNode> topLevelNodes = new ArrayList<>();

            for (Object apiObj : apis) {
                JSONObject api = (JSONObject) apiObj;
                String targetId = api.getStr("target_id");
                String parentId = api.getStr("parent_id", "0");
                String targetType = api.getStr("target_type", "");

                CollectionNode currentNode = null;

                if ("folder".equals(targetType)) {
                    currentNode = idToNodeMap.get(targetId);
                } else if ("api".equals(targetType)) {
                    HttpRequestItem req = parseApiPostApi(api);
                    if (req != null) {
                        currentNode = new CollectionNode(NodeType.REQUEST, req);
                    }
                }

                if (currentNode != null) {
                    if ("0".equals(parentId)) {
                        // 顶级节点
                        topLevelNodes.add(currentNode);
                    } else {
                        // 查找父节点
                        CollectionNode parentNode = idToNodeMap.get(parentId);
                        if (parentNode != null) {
                            parentNode.addChild(currentNode);
                        } else {
                            // 如果找不到父节点，也作为顶级节点
                            topLevelNodes.add(currentNode);
                        }
                    }
                }
            }

            // 创建一个以项目名为名称的根文件夹
            RequestGroup collectionGroup = new RequestGroup(projectName);

            // 解析全局认证（如果存在）
            JSONObject global = apipostRoot.getJSONObject("global");
            if (global != null) {
                JSONObject globalAuth = global.getJSONObject("global_param");
                if (globalAuth != null) {
                    JSONObject auth = globalAuth.getJSONObject("auth");
                    if (auth != null) {
                        AuthParserUtil.parseApiPostAuthToGroup(auth, collectionGroup);
                    }
                }
            }

            CollectionParseResult result = new CollectionParseResult(collectionGroup);

            // 将所有顶级节点添加到根节点下
            for (CollectionNode node : topLevelNodes) {
                result.addChild(node);
            }

            return result;
        } catch (Exception e) {
            log.error("解析Apipost Collection失败", e);
            return null;
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
                    JSONArray parameters = header.getJSONArray(KEY_PARAMETER);
                    if (parameters != null && !parameters.isEmpty()) {
                        List<HttpHeader> headersList = new ArrayList<>();
                        for (Object paramObj : parameters) {
                            JSONObject param = (JSONObject) paramObj;
                            boolean enabled = param.getInt(KEY_IS_CHECKED, 1) == 1;
                            headersList.add(new HttpHeader(enabled, param.getStr("key", ""), param.getStr(KEY_VALUE, "")));
                        }
                        req.setHeadersList(headersList);
                    }
                }

                // 解析查询参数
                JSONObject query = request.getJSONObject("query");
                if (query != null) {
                    JSONArray parameters = query.getJSONArray(KEY_PARAMETER);
                    if (parameters != null && !parameters.isEmpty()) {
                        List<HttpParam> paramsList = new ArrayList<>();
                        for (Object paramObj : parameters) {
                            JSONObject param = (JSONObject) paramObj;
                            boolean enabled = param.getInt(KEY_IS_CHECKED, 1) == 1;
                            paramsList.add(new HttpParam(enabled, param.getStr("key", ""), param.getStr(KEY_VALUE, "")));
                        }
                        req.setParamsList(paramsList);
                    }
                }

                // 解析Cookie
                JSONObject cookie = request.getJSONObject("cookie");
                if (cookie != null) {
                    JSONArray parameters = cookie.getJSONArray(KEY_PARAMETER);
                    if (parameters != null && !parameters.isEmpty()) {
                        List<String> cookiePairs = new ArrayList<>();
                        for (Object paramObj : parameters) {
                            JSONObject param = (JSONObject) paramObj;
                            boolean enabled = param.getInt(KEY_IS_CHECKED, 1) == 1;
                            if (enabled) {
                                cookiePairs.add(param.getStr("key", "") + "=" + param.getStr(KEY_VALUE, ""));
                            }
                        }

                        if (!cookiePairs.isEmpty()) {
                            // 确保headersList已初始化
                            if (req.getHeadersList() == null) {
                                req.setHeadersList(new ArrayList<>());
                            }

                            // 检查是否已存在Cookie头
                            boolean hasCookieHeader = false;
                            for (HttpHeader h : req.getHeadersList()) {
                                if ("Cookie".equalsIgnoreCase(h.getKey())) {
                                    String currentValue = h.getValue();
                                    String newValue = currentValue.isEmpty()
                                            ? String.join("; ", cookiePairs)
                                            : currentValue + "; " + String.join("; ", cookiePairs);
                                    h.setValue(newValue);
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
                        JSONArray parameters = body.getJSONArray(KEY_PARAMETER);
                        if (parameters != null && !parameters.isEmpty()) {
                            List<HttpFormUrlencoded> urlencodedList = new ArrayList<>();
                            for (Object paramObj : parameters) {
                                JSONObject param = (JSONObject) paramObj;
                                boolean enabled = param.getInt(KEY_IS_CHECKED, 1) == 1;
                                urlencodedList.add(new HttpFormUrlencoded(enabled, param.getStr("key", ""), param.getStr(KEY_VALUE, "")));
                            }
                            req.setUrlencodedList(urlencodedList);
                        }
                    } else if ("formdata".equals(mode)) {
                        JSONArray parameters = body.getJSONArray(KEY_PARAMETER);
                        if (parameters != null && !parameters.isEmpty()) {
                            List<HttpFormData> formDataList = new ArrayList<>();
                            for (Object paramObj : parameters) {
                                JSONObject param = (JSONObject) paramObj;
                                boolean enabled = param.getInt(KEY_IS_CHECKED, 1) == 1;
                                String fieldType = param.getStr("field_type", "string");
                                String key = param.getStr("key", "");

                                // 判断是文件还是文本
                                if ("file".equals(fieldType) || param.containsKey("file_name")) {
                                    String fileName = param.getStr("file_name", "");
                                    formDataList.add(new HttpFormData(enabled, key, HttpFormData.TYPE_FILE, fileName));
                                } else {
                                    String value = param.getStr(KEY_VALUE, "");
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
                parseApiPostScripts(request, req);
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
     * 解析Apipost的脚本（前置和后置）
     * 注意：Apipost的脚本格式可能与Postman不同，这里预留扩展点
     */
    private static void parseApiPostScripts(JSONObject request, HttpRequestItem req) {
        JSONArray preTasks = request.getJSONArray("pre_tasks");
        if (preTasks != null && !preTasks.isEmpty()) {
            // TODO: 实现Apipost的前置脚本格式解析
            log.debug("Apipost pre_tasks parsing not yet implemented");
        }

        JSONArray postTasks = request.getJSONArray("post_tasks");
        if (postTasks != null && !postTasks.isEmpty()) {
            // TODO: 实现Apipost的后置脚本格式解析
            log.debug("Apipost post_tasks parsing not yet implemented");
        }
    }

    /**
     * 安全解析整数状态码
     */
    private static int parseStatusCode(String codeStr) {
        try {
            return Integer.parseInt(codeStr);
        } catch (NumberFormatException e) {
            log.warn("Invalid status code: {}, using 200 as default", codeStr);
            return 200;
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
                savedResponse.setCode(parseStatusCode(codeStr));
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
                    headersList.add(new HttpHeader(true, hObj.getStr("key", ""), hObj.getStr(KEY_VALUE, "")));
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
