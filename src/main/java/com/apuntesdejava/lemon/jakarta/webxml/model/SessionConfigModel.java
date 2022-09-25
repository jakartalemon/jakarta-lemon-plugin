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
package com.apuntesdejava.lemon.jakarta.webxml.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class SessionConfigModel {

    @XmlElement(name = "session-timeout", namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private String sessionTimeout;

    public SessionConfigModel() {
    }

    public SessionConfigModel(String sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public String getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(String sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

}
