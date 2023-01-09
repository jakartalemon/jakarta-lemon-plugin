/*
 * Copyright 2021 Apuntes de Java.
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
package com.apuntesdejava.lemon.plugin.util;

import jakarta.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class PayaraUtil {

    private PayaraUtil() {

    }

    public static void createPayaraDataSourceResources(Log log, JsonObject projectModel, MavenProject mavenProject) {
        try {

            String dataSourceName = JDBC + '/' + mavenProject.getArtifactId();
            String poolName = mavenProject.getArtifactId() + "Pool";
            var datasource = projectModel.getJsonObject(DATASOURCE);

            String driverDataSource = ProjectModelUtil.getDriver(log, datasource.getString(DB));
            var payaraResourcesXml = PayaraResourcesXmlUtil.openPayaraXml(mavenProject.getBasedir());
            DocumentXmlUtil.listElementsByFilter(payaraResourcesXml, SLASH+RESOURCES)
                .stream()
                .findFirst()
                .ifPresent(resourcesElement -> {
                    try {
                        try {
                            if (DocumentXmlUtil.listElementsByFilter(payaraResourcesXml, String.format("/resources/jdbc-resource[@jndi-name=\"%s\"]", dataSourceName))
                                .isEmpty()) {
                                DocumentXmlUtil.createElement(payaraResourcesXml, resourcesElement, "jdbc-resource")
                                    .ifPresent(jdbcResourceElement -> {
                                        jdbcResourceElement.setAttribute("jndi-name", dataSourceName);
                                        jdbcResourceElement.setAttribute("pool-name", poolName);
                                    });
                            }
                        } catch (XPathExpressionException ex) {
                            log.error(ex.getMessage(), ex);
                        }
                        if (DocumentXmlUtil.listElementsByFilter(payaraResourcesXml, String.format("/resources/jdbc-connection-pool[@name=\"%s\"]", poolName))
                            .isEmpty()) {
                            DocumentXmlUtil.createElement(payaraResourcesXml, resourcesElement, "jdbc-connection-pool")
                                .ifPresent(jdbcConnectionPoolElement -> {
                                    jdbcConnectionPoolElement.setAttribute("datasource-classname", driverDataSource);
                                    jdbcConnectionPoolElement.setAttribute(NAME, poolName);
                                    jdbcConnectionPoolElement.setAttribute("res-type", "javax.sql.DataSource");
                                    DocumentXmlUtil.createElement(payaraResourcesXml, jdbcConnectionPoolElement, PROPERTY)
                                        .ifPresent(propertyElement -> {
                                            propertyElement.setAttribute(NAME, URL);
                                            propertyElement.setAttribute(VALUE, datasource.getString(URL));
                                        });
                                    DocumentXmlUtil.createElement(payaraResourcesXml, jdbcConnectionPoolElement, PROPERTY)
                                        .ifPresent(propertyElement -> {
                                            propertyElement.setAttribute(NAME, USER);
                                            propertyElement.setAttribute(VALUE, datasource.getString(USER));
                                        });
                                    DocumentXmlUtil.createElement(payaraResourcesXml, jdbcConnectionPoolElement, PROPERTY)
                                        .ifPresent(propertyElement -> {
                                            propertyElement.setAttribute(NAME, PASSWORD);
                                            propertyElement.setAttribute(VALUE, datasource.getString(PASSWORD));
                                        });
                                    var properties = datasource.getJsonObject(PROPERTIES);
                                    properties.keySet()
                                        .forEach(key -> DocumentXmlUtil.createElement(payaraResourcesXml, jdbcConnectionPoolElement, PROPERTY)
                                            .ifPresent(propertyElement -> {
                                                propertyElement.setAttribute(NAME, key);
                                                propertyElement.setAttribute(VALUE, properties.getString(key));
                                            }));

                                });
                        }
                    } catch (XPathExpressionException ex) {
                        log.error(ex.getMessage(), ex);
                    }
                });
            PayaraResourcesXmlUtil.savePayaraXml(mavenProject.getBasedir(), payaraResourcesXml);
            log.info("To add resources into PAYARA Server, use:\n $PAYARA_HOME/bin/asadmin add-resources " + PayaraResourcesXmlUtil.getPayaraResourcesPath(mavenProject.getBasedir()));

        } catch (IOException | InterruptedException | URISyntaxException | XPathExpressionException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static String replaceChars(String str) {
        return StringUtils.replaceEach(str, new String[]{":"}, new String[]{'\\' + ":"});
    }

    public static void createPayaraMicroDataSourcePostBootFile(Log log, String fileName, JsonObject projectModel, MavenProject mavenProject) {
        try {
            log.debug("Creating datasource for PayaraMicro in " + fileName);
            var datasource = projectModel.getJsonObject(DATASOURCE);
            String driverDataSource = ProjectModelUtil.getDriver(log, datasource.getString(DB));
            String poolName = mavenProject.getArtifactId() + "Pool";
            String dataSourceName = "jdbc/" + mavenProject.getArtifactId();
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder(String.format("create-jdbc-connection-pool --ping=true --pooling=true --restype=javax.sql.DataSource --datasourceclassname=%s --property ", driverDataSource));
            line.append(String.format("%s=%s:", USER, datasource.getString(USER)));
            line.append(String.format("%s=%s:", PASSWORD, datasource.getString(PASSWORD)));
            line.append(String.format("%s=%s:", URL, replaceChars(datasource.getString(URL))));
            var properties = datasource.getJsonObject(PROPERTIES);
            properties.keySet().forEach(key -> line.append(String.format("%s=%s:", key, properties.getString(key))));
            line.setLength(line.length() - 1);//quitando último dos puntos
            line.append(' ').append(poolName);
            lines.add(line.toString());
            lines.add(String.format("create-jdbc-resource --connectionpoolid %s %s", poolName, dataSourceName));

            Files.write(Path.of(fileName), lines);
            log.debug(fileName + " created");

        } catch (IOException | InterruptedException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
