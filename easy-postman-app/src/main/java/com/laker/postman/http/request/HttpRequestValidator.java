package com.laker.postman.http.request;

import com.laker.postman.model.PreparedRequest;
import com.laker.postman.request.model.HttpRequestItem;
import com.laker.postman.service.EnvironmentService;
import com.laker.postman.util.I18nUtil;
import com.laker.postman.util.MessageKeys;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@UtilityClass
public class HttpRequestValidator {
    private static final Pattern UNRESOLVED_VARIABLE_PATTERN = Pattern.compile("\\{\\{(.+?)}}");

    public static HttpRequestValidationResult validate(PreparedRequest req, HttpRequestItem item) {
        if (req.url.isEmpty()) {
            return HttpRequestValidationResult.error(I18nUtil.getMessage(MessageKeys.REQUEST_VALIDATION_URL_REQUIRED), true);
        }
        if (req.method == null || req.method.isEmpty()) {
            return HttpRequestValidationResult.error(I18nUtil.getMessage(MessageKeys.REQUEST_VALIDATION_METHOD_REQUIRED), false);
        }

        List<String> unresolved = findUnresolvedVariables(req.url);
        if (!unresolved.isEmpty()) {
            String activeEnvName = EnvironmentService.getActiveEnvironment() != null
                    ? EnvironmentService.getActiveEnvironment().getName()
                    : I18nUtil.getMessage(MessageKeys.REQUEST_VALIDATION_NO_ACTIVE_ENVIRONMENT);
            int queryIndex = req.url.indexOf('?');
            String targetPart = queryIndex >= 0 ? req.url.substring(0, queryIndex) : req.url;
            List<String> unresolvedInTarget = findUnresolvedVariables(targetPart);
            if (!unresolvedInTarget.isEmpty()) {
                log.warn("URL 目标地址中存在未解析的变量占位符，将阻止发送。未解析变量={}, 当前激活环境=[{}], URL={}",
                        unresolvedInTarget, activeEnvName, req.url);
                return HttpRequestValidationResult.error(I18nUtil.getMessage(
                        MessageKeys.REQUEST_VALIDATION_UNRESOLVED_URL_VARIABLES,
                        String.join(", ", unresolvedInTarget),
                        activeEnvName
                ), true);
            }

            log.warn("URL 查询参数中存在未解析的变量占位符，将继续发送请求。未解析变量={}, 当前激活环境=[{}], URL={}",
                    unresolved, activeEnvName, req.url);
            return HttpRequestValidationResult.okWithWarning(I18nUtil.getMessage(
                    MessageKeys.REQUEST_VALIDATION_UNRESOLVED_QUERY_VARIABLES,
                    String.join(", ", unresolved),
                    activeEnvName
            ));
        }

        if (item != null
                && item.getProtocol() != null
                && item.getProtocol().isHttpProtocol()
                && req.body != null
                && "GET".equalsIgnoreCase(req.method)
                && item.getBody() != null && !item.getBody().isEmpty()) {
            return HttpRequestValidationResult.requiresConfirmation(
                    I18nUtil.getMessage(MessageKeys.REQUEST_VALIDATION_GET_BODY_CONFIRM),
                    I18nUtil.getMessage(MessageKeys.REQUEST_VALIDATION_GET_BODY_CONFIRM_TITLE)
            );
        }
        return HttpRequestValidationResult.ok();
    }

    public static List<String> findUnresolvedVariables(String text) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return result;
        }
        Matcher matcher = UNRESOLVED_VARIABLE_PATTERN.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
