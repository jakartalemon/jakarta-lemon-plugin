/*
 * Copyright 2021 Apuntes de Java.
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
package com.apuntesdejava.lemon.jakarta.model.types;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public enum DatasourceDefinitionStyleType {
    WEB("web.xml"),
    PAYARA_RESOURCE("payara-resources");
    private final String value;

    private DatasourceDefinitionStyleType(String value) {
        this.value = value;
    }

    public static DatasourceDefinitionStyleType findByValue(String value) {
        for (DatasourceDefinitionStyleType item : values()) {
            if (StringUtils.equalsIgnoreCase(item.value, value)) {
                return item;
            }
        }
        return null;
    }
}
