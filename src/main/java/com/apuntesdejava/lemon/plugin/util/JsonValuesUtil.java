/*
 * Copyright 2022 Apuntes de Java.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apuntesdejava.lemon.plugin.util;

import jakarta.json.JsonObject;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for handling JSON fields
 *
 * @author Diego Silva diego.silva at apuntesdejava.com
 */
public class JsonValuesUtil {
    /**
     * Evaluates whether the numeric field does not exist, is empty, or is zero.
     *
     * @param jsonObject The json object to evaluate
     * @param fieldName  The name of the numeric field
     * @return {@code  true } if it does not exist or is zero
     */
    public static boolean isNumberEmpty(JsonObject jsonObject, String fieldName) {
        return !jsonObject.containsKey(fieldName) || jsonObject.getJsonNumber(fieldName).intValue() == 0;

    }

    /**
     * Evaluates whether the string field does not exist or is empty.
     *
     * @param jsonObject The json object to evaluate
     * @param fieldName  The name of the string field
     * @return {@code  true } if it does not exist or is empty
     */
    public static boolean isStringNotEmpty(JsonObject jsonObject, String fieldName) {
        return jsonObject.containsKey(fieldName) && StringUtils.isNotBlank(jsonObject.getString(fieldName));
    }


    /**
     * Evaluates whether the indicated field exists and whether it is an object with fields.
     *
     * @param jsonObject The json object to evaluate
     * @param fieldName  The name of the field to evaluate
     * @return {@code true} If the field exists, and whether it is an object with fields
     */
    public static boolean isFieldsNotEmpty(JsonObject jsonObject, String fieldName) {
        return jsonObject.containsKey(fieldName) && !jsonObject.getJsonObject(fieldName).isEmpty();
    }
}
