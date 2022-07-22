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
package com.apuntesdejava.lemon.jakarta.payararesources.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "resources")
public class PayaraResourcesModel {

    @XmlElement(name = "jdbc-resource")
    private JdbcResourceModel jdbcResourceModel;

    @XmlElement(name = "jdbc-connection-pool")
    private JdbcConnectionPoolModel jdbcConnectionPool;

    public PayaraResourcesModel() {
    }

    public PayaraResourcesModel(JdbcResourceModel jdbcResourceModel, JdbcConnectionPoolModel jdbcConnectionPool) {
        this.jdbcResourceModel = jdbcResourceModel;
        this.jdbcConnectionPool = jdbcConnectionPool;
    }

    public JdbcResourceModel getJdbcResourceModel() {
        return jdbcResourceModel;
    }

    public void setJdbcResourceModel(JdbcResourceModel jdbcResourceModel) {
        this.jdbcResourceModel = jdbcResourceModel;
    }

    public JdbcConnectionPoolModel getJdbcConnectionPool() {
        return jdbcConnectionPool;
    }

    public void setJdbcConnectionPool(JdbcConnectionPoolModel jdbcConnectionPool) {
        this.jdbcConnectionPool = jdbcConnectionPool;
    }

    public static PayaraResourcesModel newInstance(JdbcResourceModel jdbcResourceModel, JdbcConnectionPoolModel jdbcConnectionPool) {
        return new PayaraResourcesModel(jdbcResourceModel, jdbcConnectionPool);
    }
}
