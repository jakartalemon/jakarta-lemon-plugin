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
import lombok.*;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Data
@XmlAccessorType(XmlAccessType.FIELD)

public class PersistenceUnitModel {

    @XmlAttribute(name = "transaction-type")
    private String transactionType = "JTA";

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name = "jta-data-source", namespace = "https://jakarta.ee/xml/ns/persistence")
    private String jtaDataSource;


    @XmlElement(name = "properties", namespace = "https://jakarta.ee/xml/ns/persistence")
    private PropertiesModel properties;


}
