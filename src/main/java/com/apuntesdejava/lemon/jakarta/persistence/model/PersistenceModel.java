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

import jakarta.xml.bind.annotation.*;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "persistence", namespace = "https://jakarta.ee/xml/ns/persistence")
public class PersistenceModel {

    @XmlAttribute
    private final String version = "3.0";

    @XmlElement(name = "persistence-unit", namespace = "https://jakarta.ee/xml/ns/persistence")
    private PersistenceUnitModel persistenceUnit;

    public String getVersion() {
        return version;
    }

    public PersistenceUnitModel getPersistenceUnit() {
        return persistenceUnit;
    }

    public void setPersistenceUnit(PersistenceUnitModel persistenceUnit) {
        this.persistenceUnit = persistenceUnit;
    }
}
