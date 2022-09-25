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
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "web-app", namespace = "https://jakarta.ee/xml/ns/jakartaee")
public class WebAppModel {

    @XmlAttribute
    private final String version = "5.0";

    @XmlAttribute(name = "xsi:schemaLocation")
    private final String schemaLocation = "https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd";

    @XmlElement(name = "servlet", namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private List<ServletModel> servlet;

    @XmlElement(name = "servlet-mapping", namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private List<ServletMappingModel> servletMapping;

    @XmlElement(name = "data-source", namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private DataSourceModel dataSource;

    @XmlElement(name = "session-config", namespace = "https://jakarta.ee/xml/ns/jakartaee")
    private SessionConfigModel sessionConfig;

    public List<ServletModel> getServlet() {
        return servlet;
    }

    public void setServlet(List<ServletModel> servlet) {
        this.servlet = servlet;
    }

    public List<ServletMappingModel> getServletMapping() {
        return servletMapping;
    }

    public void setServletMapping(List<ServletMappingModel> servletMapping) {
        this.servletMapping = servletMapping;
    }

    public DataSourceModel getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSourceModel dataSource) {
        this.dataSource = dataSource;
    }

    public String getVersion() {
        return version;
    }

    public SessionConfigModel getSessionConfig() {
        return sessionConfig;
    }

    public void setSessionConfig(SessionConfigModel sessionConfig) {
        this.sessionConfig = sessionConfig;
    }

    public String getSchemaLocation() {
        return schemaLocation;
    }

}
