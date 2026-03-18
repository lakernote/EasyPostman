package com.laker.postman.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.laker.postman.model.CurlRequest;
import com.laker.postman.model.HttpRequestItem;
import com.laker.postman.model.RequestItemProtocolEnum;
import com.laker.postman.service.curl.CurlParser;
import com.laker.postman.service.http.HttpUtil;
import lombok.experimental.UtilityClass;

/**
 * cURL 导入工具类
 * 提供 cURL 命令解析和转换为 HttpRequestItem 的功能
 */
@UtilityClass
public class CurlImportUtil {

    /**
     * 解析 cURL 命令并转换为 HttpRequestItem
     *
     * @param curlText cURL 命令文本（可以是 cURL 命令字符串或已解析的 CurlRequest 对象）
     * @return 解析后的 HttpRequestItem，如果解析失败返回 null
     */
    public static HttpRequestItem fromCurl(String curlText) {
        if (CharSequenceUtil.isBlank(curlText)) {
            return null;
        }

        try {
            CurlRequest curlRequest = CurlParser.parse(curlText);
            return fromCurlRequest(curlRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse cURL command", e);
        }
    }

    /**
     * 将 CurlRequest 转换为 HttpRequestItem
     *
     * @param curlRequest 已解析的 cURL 请求对象
     * @return HttpRequestItem 对象，如果 curlRequest 无效返回 null
     */
    public static HttpRequestItem fromCurlRequest(CurlRequest curlRequest) {
        if (curlRequest == null || curlRequest.url == null) {
            return null;
        }

        HttpRequestItem item = new HttpRequestItem();
        item.setName(null);
        item.setUrl(curlRequest.url);
        item.setMethod(curlRequest.method);

        if (CollUtil.isNotEmpty(curlRequest.headersList)) {
            item.setHeadersList(curlRequest.headersList);
        }

        item.setBody(curlRequest.body);

        if (CollUtil.isNotEmpty(curlRequest.paramsList)) {
            item.setParamsList(curlRequest.paramsList);
        }

        if (CollUtil.isNotEmpty(curlRequest.formDataList)) {
            item.setFormDataList(curlRequest.formDataList);
        }

        if (CollUtil.isNotEmpty(curlRequest.urlencodedList)) {
            item.setUrlencodedList(curlRequest.urlencodedList);
        }

        // 智能判断协议类型
        if (HttpUtil.isSSERequest(item)) {
            item.setProtocol(RequestItemProtocolEnum.SSE);
        } else if (HttpUtil.isWebSocketRequest(item.getUrl())) {
            item.setProtocol(RequestItemProtocolEnum.WEBSOCKET);
        } else {
            item.setProtocol(RequestItemProtocolEnum.HTTP);
        }

        return item;
    }
}
