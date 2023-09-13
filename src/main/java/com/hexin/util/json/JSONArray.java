package com.hexin.util.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author 浦希成  2019/10/14 13:31
 */
public class JSONArray {
    private static ObjectMapper mapper;
    private List<Object> list = new ArrayList<>();

    public void setList(List<Object> list) {
        this.list = list;
    }

    public List<Object> getList() {
        return list;
    }

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

    public static JSONArray parseArray2(String value) {
        List<Object> res = null;
        try {
            Object[] o = mapper.readValue(value, Object[].class);
            res = new ArrayList<>(Arrays.asList(o));
        } catch (IOException e) {
            e.printStackTrace();
        }
        JSONArray jsonArray = new JSONArray();
        jsonArray.setList(res);
        return jsonArray;
    }

    public static JSONArray parseArray(String value) {
        List<Map> lists = toJavaObjectList(value, Map.class);
        if (lists == null) {
            throw new ClassCastException();
        }
        List<Object> result = new ArrayList<>(lists.size());
        for (Map map : lists) {
            JSONObject jsonObject1 = new JSONObject(map);
            result.add(jsonObject1.toJSONString());
        }
        JSONArray jsonArray = new JSONArray();
        jsonArray.setList(result);
        return jsonArray;
    }

    public static <T> List<T> parseArray(String value, Class<T> tClass) {
        return toJavaObjectList(value, tClass);
    }

    private static <T> List<T> toJavaObjectList(String value, Class<T> tClass, Supplier<List<T>> defaultSupplier) {
        try {
            if (StringUtils.isBlank(value)) {
                return defaultSupplier.get();
            }
            JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, tClass);
            return mapper.readValue(value, javaType);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return defaultSupplier.get();
    }

    private static <T> List<T> toJavaObjectList(String value, Class<T> tClass) {
        return StringUtils.isNotBlank(value) ? toJavaObjectList(value, tClass, () -> null) : null;
    }

    public static String toJSONString(List<String> list) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("[");
        for (String s : list) {
            buffer.append("\"").append(s).append("\"").append(",");
        }

        return buffer.delete(buffer.length() - 1, buffer.length()).toString() + "]";
    }

    public static String toJSONString(Object obj, Supplier<String> defaultSupplier) {
        try {
            return obj != null ? mapper.writeValueAsString(obj) : defaultSupplier.get();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return defaultSupplier.get();
    }

    public int size() {
        /**
         * 解决空指针bug
         */
        if (list == null) {
            return 0;
        }
        return list.size();
    }

    public Object get(int i) {
        return list.get(i);
    }

    public JSONArray getJSONArray(int i) {
        JSONArray jsonArray = new JSONArray();
        Object obj = get(i);
        if (obj instanceof String) {
            jsonArray = JSONArray.parseArray2((String) obj);
        } else {
            jsonArray.setList((List<Object>) get(i));
        }
        return jsonArray;
    }

    public void addAll(List<Map<String, Object>> defaultList) {
        for (Map<String, Object> map : defaultList) {
            JSONObject jsonObject = new JSONObject(map);
            list.add(jsonObject);
        }
    }

    @Override
    public String toString() {
        String res = null;
        try {
            res = mapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return res;
    }

    public void add(Object object) {
        list.add(object);
    }

    public void add(int index, Object object) {
        list.add(index, object);
    }

    public void clear() {
        list.clear();
    }

    public JSONObject getJSONObject(int i) {
        Object obj = list.get(i);
        if (obj instanceof String) {
            return JSONObject.toJsonObject((String) obj);
        } else if (obj instanceof Map) {
            return JSONObject.toJsonObject(JSONObject.toJSONString(obj));
        } else {
            return (JSONObject) obj;
        }
    }

    public static void main(String[] args) {

    }
}
