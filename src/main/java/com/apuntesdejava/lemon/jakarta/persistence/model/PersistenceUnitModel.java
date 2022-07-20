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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@XmlAccessorType(XmlAccessType.FIELD)

public class PersistenceUnitModel {

    @XmlAttribute(name = "transaction-type")
    private String transactionType = "JTA";

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "jta-data-source", namespace = "https://jakarta.ee/xml/ns/persistence")
    private String jtaDataSource;

    /*@XmlElement(name = "shared-cache-mode", namespace = "https://jakarta.ee/xml/ns/persistence")
    private String sharedCacheMode = "ENABLE_SELECTIVE"; */

    @XmlElement(name = "properties", namespace = "https://jakarta.ee/xml/ns/persistence")
    private PropertiesModel properties;

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJtaDataSource() {
        return jtaDataSource;
    }

    public void setJtaDataSource(String jtaDataSource) {
        this.jtaDataSource = jtaDataSource;
    }
/*
    public String getSharedCacheMode() {
        return sharedCacheMode;
    }

    public void setSharedCacheMode(String sharedCacheMode) {
        this.sharedCacheMode = sharedCacheMode;
    }*/

    public PropertiesModel getProperties() {
        return properties;
    }

    public void setProperties(PropertiesModel properties) {
        this.properties = properties;
    }
}
