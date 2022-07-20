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
package com.apuntesdejava.lemon.jakarta.persistence.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.List;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertiesModel {
    @XmlElement(name = "property", namespace = "https://jakarta.ee/xml/ns/persistence")
    private List<PropertyModel> property;

    public PropertiesModel() {
    }

    public PropertiesModel(List<PropertyModel> property) {
        this.property = property;
    }

    public List<PropertyModel> getProperty() {
        return property;
    }

    public void setProperty(List<PropertyModel> property) {
        this.property = property;
    }
}
