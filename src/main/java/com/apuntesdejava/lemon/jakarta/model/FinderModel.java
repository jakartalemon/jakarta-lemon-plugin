/*
 * Copyright 2021 Diego Silva <diego.silva at apuntesdejava.com>.
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
package com.apuntesdejava.lemon.jakarta.model;

import jakarta.json.bind.annotation.JsonbProperty;
import java.util.Map;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class FinderModel {

    private String query;

    @JsonbProperty(value = "native")
    private boolean nativeQuery;

    private boolean unique;

    @JsonbProperty(value = "return")
    private String returnValueType;

    private Map<String, String> parameters;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isNativeQuery() {
        return nativeQuery;
    }

    public void setNativeQuery(boolean nativeQuery) {
        this.nativeQuery = nativeQuery;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public String getReturnValueType() {
        return returnValueType;
    }

    public void setReturnValueType(String returnValueType) {
        this.returnValueType = returnValueType;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }
    
    
}
