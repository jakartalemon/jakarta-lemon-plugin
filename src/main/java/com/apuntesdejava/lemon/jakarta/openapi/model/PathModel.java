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
package com.apuntesdejava.lemon.jakarta.openapi.model;

import jakarta.json.bind.annotation.JsonbProperty;
import lombok.Data;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Data
public class PathModel {

    @JsonbProperty(value = "$ref")
    private String ref;
    private String summary;
    private String description;

    private OperationModel get;
    private OperationModel put;
    private OperationModel post;
    private OperationModel delete;
    private OperationModel options;
    private OperationModel patch;
    private OperationModel trace;
}
