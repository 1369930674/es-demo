package com.hexin.util.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author jgd
 */
@JsonInclude
public class JSONObject {

    private static ObjectMapper mapper;

    private Map<String, Object> map;

    static {
        mapper = new ObjectMapper();
        // 如果json中有新增的字段并且是实体类类中不存在的，不报错
        mapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        // 如果存在未知属性，则忽略不报错
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 允许key没有双引号
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 允许key有单引号
        mapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        // 允许整数以0开头
        mapper.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        // 允许字符串中存在回车换行控制符
        mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    }

    public JSONObject() {
    }

    public JSONObject(Map<String, Object> map) {
        this.map = map;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    public void setMap(Map<String, Object> map) {
        this.map = map;
    }

    public static String toJSONString(Object obj) {
        return obj != null ? toJSONString(obj, () -> "") : null;
    }

    public String toJSONString() {
        return toJSONString(map);
    }

    public static String toJSONString(Object obj, Supplier<String> defaultSupplier) {
        try {
            return obj != null ? mapper.writeValueAsString(obj) : defaultSupplier.get();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return defaultSupplier.get();
    }
    public void put(String key, Object value) {
        if (map == null) {
            map = new HashMap<>();
        }
        if (value instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) value;
            this.map.put(key, jsonObject.getMap());
        } else {
            map.put(key, value);
        }
    }

    public static <T> T toJavaObject(String value, Class<T> tClass) {
        return StringUtils.isNotBlank(value) ? toJavaObject(value, tClass, () -> null) : null;
    }
    public JSONArray getJSONArray2(String key) {
        String data = getString(key);
        JSONArray jsonArray = JSONArray.parseArray2(data);
        return jsonArray;
    }
    public static <T> T toJavaObject(String value, Class<T> tClass, Supplier<T> defaultSupplier) {
        try {
            if (StringUtils.isBlank(value)) {
                return defaultSupplier.get();
            }
            return mapper.readValue(value, tClass);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return defaultSupplier.get();
    }

    public static JSONObject toJsonObject(String value) {
        Map<String, Object> map = StringUtils.isNotBlank(value) ? toMap(value, () -> null) : null;
        if (map == null) {
            throw new RuntimeException("parse jsonObject error, value=" + value);
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.setMap(map);
        return jsonObject;
    }


    public static Map<String, Object> toMap(String value, Supplier<Map<String, Object>> defaultSupplier) {
        if (StringUtils.isBlank(value)) {
            return defaultSupplier.get();
        }
        try {
            return toJavaObject(value, LinkedHashMap.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultSupplier.get();
    }

    public Integer size() {
        return map.size();
    }


    public Object get(String key) {
        return map.get(key);
    }
    public Integer getInteger(String key) {
        Object obj = map.get(key);
        return TypeUtils.castToInt(obj);
    }
    public String getString(String key) {
        String result;
        Object o = map.get(key);
        if (!(o instanceof String)) {
            result = toJSONString(o);
        } else {
            result = String.valueOf(map.get(key));
        }
        return result;
    }

    @Override
    public String toString() {
        return toJSONString(map);
    }

}
