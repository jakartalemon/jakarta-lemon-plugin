package com.apuntesdejava.lemon.plugin.util;

import jakarta.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

public class JsonValuesUtil {
    public static boolean isNumberEmpty(JsonObject jsonObject,
                                        String fieldName) {
        return !jsonObject.containsKey(fieldName) ||
                jsonObject.getJsonNumber(fieldName).intValue() == 0;

    }

    public static boolean isStringEmpty(JsonObject jsonObject,
                                        String fieldName) {
        return !jsonObject.containsKey(fieldName) ||
                StringUtils.isEmpty(jsonObject.getString(fieldName));
    }

    public static boolean isFieldsEmpty(JsonObject jsonObject,
                                        String fieldName) {
        return !jsonObject.containsKey(fieldName) ||
                jsonObject.getJsonObject(fieldName).isEmpty();
    }
}
