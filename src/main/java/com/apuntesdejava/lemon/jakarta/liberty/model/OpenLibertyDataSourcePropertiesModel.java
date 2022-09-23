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
package com.apuntesdejava.lemon.jakarta.liberty.model;

import static jakarta.xml.bind.annotation.XmlAccessType.FIELD;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@XmlAccessorType(FIELD)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenLibertyDataSourcePropertiesModel {

    @XmlAttribute
    private String url;

    @XmlAttribute
    private String user;

    @XmlAttribute
    private String password;

    @XmlAttribute
    private String serverTimezone;

    @XmlAttribute
    private String allowPublicKeyRetrieval;

    @XmlAttribute
    private String useSSL;
}