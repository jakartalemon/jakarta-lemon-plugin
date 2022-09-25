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
import java.util.ArrayList;
import java.util.List;
import static java.util.stream.Collectors.toList;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DataSourceModel {

    @XmlElement(namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private String name;

    @XmlElement(name = "class-name", namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private String className;

    @XmlElement(name = "url", namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private String url;

    @XmlElement(name = "user", namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private String user;

    @XmlElement(name = "password", namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private String password;

    @XmlElement(name = "property", namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private List<DataSourcePropertyModel> property;

    public DataSourceModel() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<DataSourcePropertyModel> getProperty() {
        return property;
    }

    public void setProperty(List<DataSourcePropertyModel> property) {
        this.property = property;
    }

    public static class DataSourceModelBuilder {

        private String name;
        private String className;
        private String url;
        private String user;
        private String password;
        private final List<List< String>> properties = new ArrayList<>();

        public DataSourceModelBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public DataSourceModelBuilder setClassName(String className) {
            this.className = className;
            return this;
        }

        public DataSourceModelBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        public DataSourceModelBuilder setUser(String user) {
            this.user = user;
            return this;
        }

        public DataSourceModelBuilder setPassword(String password) {
            this.password = password;
            return this;
        }

        public DataSourceModelBuilder addProperty(String name, String value) {
            properties.add(List.of(name, value));
            return this;
        }

        public DataSourceModel build() {
            var model = new DataSourceModel();
            model.setClassName(className);
            model.setName(name);
            model.setPassword(password);
            model.setUrl(url);
            model.setUser(user);
            model.setProperty(properties.stream()
                    .map(item -> new DataSourcePropertyModel(item.get(0), item.get(1)))
                    .collect(toList()));
            return model;
        }
    }

}
