package com.laker.postman.model.script;

import com.laker.postman.model.HttpFormData;
import com.laker.postman.model.HttpFormUrlencoded;
import com.laker.postman.model.HttpHeader;
import com.laker.postman.model.HttpParam;
import lombok.Getter;

import java.util.List;
import java.util.Map;

import static com.laker.postman.model.HttpFormData.TYPE_TEXT;

/**
 * JS 专用 List 包装类，支持 add 方法
 * 用于包装 List<HttpHeader>、List<HttpFormData>、List<HttpFormUrlencoded>
 */
public class JsListWrapper<T> {
    /**
     * -- GETTER --
     * 获取底层 List
     */
    @Getter
    private final List<T> list;
    private final ListType type;

    public enum ListType {
        HEADER, FORM_DATA, URLENCODED, PARAM
    }

    public JsListWrapper(List<T> list, ListType type) {
        this.list = list;
        this.type = type;
    }

    /**
     * JS 脚本调用：pm.request.headers.add({key: 'X-Custom', value: 'Value'})
     */
    public void add(Map<String, Object> obj) {
        if (obj == null) return;

        Object k = obj.get("key");
        Object v = obj.get("value");
        if (k == null || v == null) return;

        String key = String.valueOf(k);
        String value = String.valueOf(v);

        switch (type) {
            case HEADER:
                HttpHeader header = new HttpHeader();
                header.setEnabled(true);
                header.setKey(key);
                header.setValue(value);
                @SuppressWarnings("unchecked")
                List<HttpHeader> headerList = (List<HttpHeader>) list;
                headerList.add(header);
                break;

            case FORM_DATA:
                HttpFormData formData = new HttpFormData();
                formData.setEnabled(true);
                formData.setKey(key);
                formData.setValue(value);
                formData.setType(TYPE_TEXT);
                @SuppressWarnings("unchecked")
                List<HttpFormData> formDataList = (List<HttpFormData>) list;
                formDataList.add(formData);
                break;

            case URLENCODED:
                HttpFormUrlencoded urlencoded = new HttpFormUrlencoded();
                urlencoded.setEnabled(true);
                urlencoded.setKey(key);
                urlencoded.setValue(value);
                @SuppressWarnings("unchecked")
                List<HttpFormUrlencoded> urlencodedList = (List<HttpFormUrlencoded>) list;
                urlencodedList.add(urlencoded);
                break;

            case PARAM:
                HttpParam param = new HttpParam();
                param.setEnabled(true);
                param.setKey(key);
                param.setValue(value);
                @SuppressWarnings("unchecked")
                List<HttpParam> paramList = (List<HttpParam>) list;
                paramList.add(param);
                break;
        }
    }

    /**
     * JS 脚本调用：pm.request.headers.add('X-Custom', 'Value')
     */
    public void add(String key, String value) {
        if (key == null || value == null) return;

        switch (type) {
            case HEADER:
                HttpHeader header = new HttpHeader();
                header.setEnabled(true);
                header.setKey(key);
                header.setValue(value);
                @SuppressWarnings("unchecked")
                List<HttpHeader> headerList = (List<HttpHeader>) list;
                headerList.add(header);
                break;

            case FORM_DATA:
                HttpFormData formData = new HttpFormData();
                formData.setEnabled(true);
                formData.setKey(key);
                formData.setValue(value);
                formData.setType(TYPE_TEXT);
                @SuppressWarnings("unchecked")
                List<HttpFormData> formDataList = (List<HttpFormData>) list;
                formDataList.add(formData);
                break;

            case URLENCODED:
                HttpFormUrlencoded urlencoded = new HttpFormUrlencoded();
                urlencoded.setEnabled(true);
                urlencoded.setKey(key);
                urlencoded.setValue(value);
                @SuppressWarnings("unchecked")
                List<HttpFormUrlencoded> urlencodedList = (List<HttpFormUrlencoded>) list;
                urlencodedList.add(urlencoded);
                break;

            case PARAM:
                HttpParam param = new HttpParam();
                param.setEnabled(true);
                param.setKey(key);
                param.setValue(value);
                @SuppressWarnings("unchecked")
                List<HttpParam> paramList = (List<HttpParam>) list;
                paramList.add(param);
                break;
        }
    }

    /**
     * Postman API: pm.request.headers.upsert({key: 'X-Custom', value: 'Value'})
     * 如果 key 已存在则更新，否则添加
     */
    public void upsert(Map<String, Object> obj) {
        if (obj == null) return;

        Object k = obj.get("key");
        Object v = obj.get("value");
        if (k == null || v == null) return;

        String key = String.valueOf(k);
        String value = String.valueOf(v);

        // 先尝试更新已存在的项
        boolean updated = false;
        switch (type) {
            case HEADER:
                @SuppressWarnings("unchecked")
                List<HttpHeader> headerList = (List<HttpHeader>) list;
                for (HttpHeader header : headerList) {
                    if (key.equalsIgnoreCase(header.getKey())) {
                        header.setValue(value);
                        header.setEnabled(true);
                        updated = true;
                        break;
                    }
                }
                break;

            case FORM_DATA:
                @SuppressWarnings("unchecked")
                List<HttpFormData> formDataList = (List<HttpFormData>) list;
                for (HttpFormData formData : formDataList) {
                    if (key.equals(formData.getKey())) {
                        formData.setValue(value);
                        formData.setEnabled(true);
                        updated = true;
                        break;
                    }
                }
                break;

            case URLENCODED:
                @SuppressWarnings("unchecked")
                List<HttpFormUrlencoded> urlencodedList = (List<HttpFormUrlencoded>) list;
                for (HttpFormUrlencoded urlencoded : urlencodedList) {
                    if (key.equals(urlencoded.getKey())) {
                        urlencoded.setValue(value);
                        urlencoded.setEnabled(true);
                        updated = true;
                        break;
                    }
                }
                break;

            case PARAM:
                @SuppressWarnings("unchecked")
                List<HttpParam> paramList = (List<HttpParam>) list;
                for (HttpParam param : paramList) {
                    if (key.equals(param.getKey())) {
                        param.setValue(value);
                        param.setEnabled(true);
                        updated = true;
                        break;
                    }
                }
                break;
        }

        // 如果没有找到，则添加新项
        if (!updated) {
            add(obj);
        }
    }

    /**
     * Postman API: pm.request.headers.upsert('X-Custom', 'Value')
     */
    public void upsert(String key, String value) {
        if (key == null || value == null) return;

        boolean updated = false;
        switch (type) {
            case HEADER:
                @SuppressWarnings("unchecked")
                List<HttpHeader> headerList = (List<HttpHeader>) list;
                for (HttpHeader header : headerList) {
                    if (key.equalsIgnoreCase(header.getKey())) {
                        header.setValue(value);
                        header.setEnabled(true);
                        updated = true;
                        break;
                    }
                }
                break;

            case FORM_DATA:
                @SuppressWarnings("unchecked")
                List<HttpFormData> formDataList = (List<HttpFormData>) list;
                for (HttpFormData formData : formDataList) {
                    if (key.equals(formData.getKey())) {
                        formData.setValue(value);
                        formData.setEnabled(true);
                        updated = true;
                        break;
                    }
                }
                break;

            case URLENCODED:
                @SuppressWarnings("unchecked")
                List<HttpFormUrlencoded> urlencodedList = (List<HttpFormUrlencoded>) list;
                for (HttpFormUrlencoded urlencoded : urlencodedList) {
                    if (key.equals(urlencoded.getKey())) {
                        urlencoded.setValue(value);
                        urlencoded.setEnabled(true);
                        updated = true;
                        break;
                    }
                }
                break;

            case PARAM:
                @SuppressWarnings("unchecked")
                List<HttpParam> paramList = (List<HttpParam>) list;
                for (HttpParam param : paramList) {
                    if (key.equals(param.getKey())) {
                        param.setValue(value);
                        param.setEnabled(true);
                        updated = true;
                        break;
                    }
                }
                break;
        }

        if (!updated) {
            add(key, value);
        }
    }

    /**
     * Postman API: pm.request.headers.remove('X-Custom')
     * 删除指定 key 的项
     */
    public void remove(String key) {
        if (key == null) return;

        switch (type) {
            case HEADER:
                @SuppressWarnings("unchecked")
                List<HttpHeader> headerList = (List<HttpHeader>) list;
                headerList.removeIf(header -> key.equalsIgnoreCase(header.getKey()));
                break;

            case FORM_DATA:
                @SuppressWarnings("unchecked")
                List<HttpFormData> formDataList = (List<HttpFormData>) list;
                formDataList.removeIf(formData -> key.equals(formData.getKey()));
                break;

            case URLENCODED:
                @SuppressWarnings("unchecked")
                List<HttpFormUrlencoded> urlencodedList = (List<HttpFormUrlencoded>) list;
                urlencodedList.removeIf(urlencoded -> key.equals(urlencoded.getKey()));
                break;

            case PARAM:
                @SuppressWarnings("unchecked")
                List<HttpParam> paramList = (List<HttpParam>) list;
                paramList.removeIf(param -> key.equals(param.getKey()));
                break;
        }
    }

    /**
     * Postman API: pm.request.headers.has('X-Custom')
     * 检查是否存在指定 key
     */
    public boolean has(String key) {
        if (key == null) return false;

        switch (type) {
            case HEADER:
                @SuppressWarnings("unchecked")
                List<HttpHeader> headerList = (List<HttpHeader>) list;
                return headerList.stream().anyMatch(header ->
                        key.equalsIgnoreCase(header.getKey()) && header.isEnabled());

            case FORM_DATA:
                @SuppressWarnings("unchecked")
                List<HttpFormData> formDataList = (List<HttpFormData>) list;
                return formDataList.stream().anyMatch(formData ->
                        key.equals(formData.getKey()) && formData.isEnabled());

            case URLENCODED:
                @SuppressWarnings("unchecked")
                List<HttpFormUrlencoded> urlencodedList = (List<HttpFormUrlencoded>) list;
                return urlencodedList.stream().anyMatch(urlencoded ->
                        key.equals(urlencoded.getKey()) && urlencoded.isEnabled());

            case PARAM:
                @SuppressWarnings("unchecked")
                List<HttpParam> paramList = (List<HttpParam>) list;
                return paramList.stream().anyMatch(param ->
                        key.equals(param.getKey()) && param.isEnabled());
        }
        return false;
    }

    /**
     * Postman API: pm.request.headers.get('X-Custom')
     * 获取指定 key 的值
     */
    public String get(String key) {
        if (key == null) return null;

        switch (type) {
            case HEADER:
                @SuppressWarnings("unchecked")
                List<HttpHeader> headerList = (List<HttpHeader>) list;
                for (HttpHeader header : headerList) {
                    if (key.equalsIgnoreCase(header.getKey()) && header.isEnabled()) {
                        return header.getValue();
                    }
                }
                break;

            case FORM_DATA:
                @SuppressWarnings("unchecked")
                List<HttpFormData> formDataList = (List<HttpFormData>) list;
                for (HttpFormData formData : formDataList) {
                    if (key.equals(formData.getKey()) && formData.isEnabled()) {
                        return formData.getValue();
                    }
                }
                break;

            case URLENCODED:
                @SuppressWarnings("unchecked")
                List<HttpFormUrlencoded> urlencodedList = (List<HttpFormUrlencoded>) list;
                for (HttpFormUrlencoded urlencoded : urlencodedList) {
                    if (key.equals(urlencoded.getKey()) && urlencoded.isEnabled()) {
                        return urlencoded.getValue();
                    }
                }
                break;

            case PARAM:
                @SuppressWarnings("unchecked")
                List<HttpParam> paramList = (List<HttpParam>) list;
                for (HttpParam param : paramList) {
                    if (key.equals(param.getKey()) && param.isEnabled()) {
                        return param.getValue();
                    }
                }
                break;
        }
        return null;
    }

}
