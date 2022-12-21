package com.apuntesdejava.lemon.plugin.util;

import jakarta.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

public class JsonValuesUtil {
    public static boolean isNumberEmpty(JsonObject jsonObject, String fieldName) {
        return !jsonObject.containsKey(fieldName) || jsonObject.getJsonNumber(fieldName).intValue() == 0;

    }

    public static boolean isStringNotEmpty(JsonObject jsonObject, String fieldName) {
        return jsonObject.containsKey(fieldName) && StringUtils.isNotBlank(jsonObject.getString(fieldName));
    }


    public static boolean isFieldsNotEmpty(JsonObject jsonObject, String fieldName) {
        return jsonObject.containsKey(fieldName) && !jsonObject.getJsonObject(fieldName).isEmpty();
    }
}
