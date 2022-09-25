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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Model for the Pool of Connections. 
 * This model is obtained from the configuration file.
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class JdbcConnectionPoolModel {

    @XmlAttribute(name = "datasource-classname")
    private String dataSourceClassName;

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "resType")
    private String resType;

    @XmlElement(name = "property")
    private List<JdbcConnectionPoolPropertyModel> property;

    /**
     * Basic constructor
     */
    public JdbcConnectionPoolModel() {
    }

    /**
     * Get classname for DataSource
     *
     * @return Classname for DataSoure
     */
    public String getDataSourceClassName() {
        return dataSourceClassName;
    }

    public void setDataSourceClassName(String dataSourceClassName) {
        this.dataSourceClassName = dataSourceClassName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getResType() {
        return resType;
    }

    public void setResType(String resType) {
        this.resType = resType;
    }

    public List<JdbcConnectionPoolPropertyModel> getProperty() {
        return property;
    }

    public void setProperty(List<JdbcConnectionPoolPropertyModel> property) {
        this.property = property;
    }

    public static class JdbcConnectionPoolModelBuilder {

        private String dataSourceClassName;
        private String name;
        private String resType;
        private List<JdbcConnectionPoolPropertyModel> property;

        private JdbcConnectionPoolModelBuilder() {
        }

        public JdbcConnectionPoolModelBuilder setDataSourceClassName(String dataSourceClassName) {
            this.dataSourceClassName = dataSourceClassName;
            return this;
        }

        public JdbcConnectionPoolModelBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public JdbcConnectionPoolModelBuilder setResType(String resType) {
            this.resType = resType;
            return this;
        }

        public JdbcConnectionPoolModelBuilder addProperty(JdbcConnectionPoolPropertyModel property) {
            if (this.property == null) {
                this.property = new ArrayList<>();
            }
            this.property.add(property);
            return this;
        }

        public static JdbcConnectionPoolModelBuilder newBuilder() {
            return new JdbcConnectionPoolModelBuilder();
        }

        public JdbcConnectionPoolModel build() {
            var model = new JdbcConnectionPoolModel();
            model.dataSourceClassName = dataSourceClassName;
            model.name = name;
            model.resType = resType;
            model.property = property;
            return model;
        }
    }
}
