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

import java.util.Map;
import lombok.Data;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Data
public class OpenApiModel {

    private String openapi;
    private InfoModel info;

    private ServerModel[] servers;

    private Map<String, PathModel> paths;
    private Map<String, Object> components;
}
