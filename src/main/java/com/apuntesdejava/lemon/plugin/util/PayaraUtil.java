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

import com.apuntesdejava.lemon.jakarta.jpa.model.DataSourceModel;
import com.apuntesdejava.lemon.jakarta.jpa.model.ProjectModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class PayaraUtil {

    private static final String PAYARA_RESOURCE_DOCTYPE_PUBLIC = "-//Payara.fish//DTD Payara Server 4 Resource Definitions//EN";
    private static final String PAYARA_RESOURCE_DOCTYPE_SYSTEM = "https://raw.githubusercontent.com/payara/Payara-Community-Documentation/master/docs/modules/ROOT/pages/schemas/payara-resources_1_6.dtd";

    private PayaraUtil() {

    }

    public static void createPayaraDataSourceResources(
            Log log, ProjectModel projectModel, MavenProject mavenProject) {
        try {
            String driverDataSource = projectModel.getDriver();

            Path resourceXml = Paths.get(mavenProject.getBasedir().toString(), "src", "main", "setup", "payara-resources.xml").normalize();
            log.debug("Creating DataSource at " + resourceXml);
            Files.createDirectories(resourceXml.getParent());
            String dataSourceName = "jdbc/" + mavenProject.getArtifactId();
            String poolName = mavenProject.getArtifactId() + "Pool";
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = dbf.newDocumentBuilder();
            Document doc = builder.newDocument();
            Element resourcesElem = doc.createElement("resources");
            Element jdbcResourceElem = doc.createElement("jdbc-resource");
            jdbcResourceElem.setAttribute("jndi-name", dataSourceName);
            jdbcResourceElem.setAttribute("pool-name", poolName);
            resourcesElem.appendChild(jdbcResourceElem);

            doc.appendChild(resourcesElem);

            Map<String, String> attrs = new LinkedHashMap<>(Map.of(
                    "url", projectModel.getDatasource().getUrl(),
                    "user", projectModel.getDatasource().getUser(),
                    "password", projectModel.getDatasource().getPassword()
            ));
            attrs.putAll(projectModel.getDatasource().getProperties());

            Element jdbcConnectionPoolElem = XmlUtil.createElement(doc, "jdbc-connection-pool", attrs);

            jdbcConnectionPoolElem.setAttribute("datasource-classname", driverDataSource);
            jdbcConnectionPoolElem.setAttribute("name", poolName);
            jdbcConnectionPoolElem.setAttribute("res-type", "javax.sql.DataSource");
            resourcesElem.appendChild(jdbcConnectionPoolElem);

            XmlUtil.writeXml(
                    doc,
                    PAYARA_RESOURCE_DOCTYPE_PUBLIC,
                    PAYARA_RESOURCE_DOCTYPE_SYSTEM,
                    resourceXml
            );

            log.info("To add resources into PAYARA Server, use:\n $PAYARA_HOME/bin/asadmin add-resources " + resourceXml);

        } catch (IOException | ParserConfigurationException | TransformerException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static String replaceChars(String str) {
        return StringUtils.replaceEach(str,
                new String[]{":"},
                new String[]{'\\' + ":"}
        );
    }

    public static void createPayaraMicroDataSourcePostBootFile(Log log, String fileName, ProjectModel projectModel, MavenProject mavenProject) {
        try {
            log.debug("Creating datasource for PayaraMicro in " + fileName);
            String driverDataSource = projectModel.getDriver();
            DataSourceModel dataSource = projectModel.getDatasource();
            String poolName = mavenProject.getArtifactId() + "Pool";
            String dataSourceName = "jdbc/" + mavenProject.getArtifactId();
            List<String> lines = new ArrayList<>();
            StringBuilder line = new StringBuilder(String.format("create-jdbc-connection-pool --ping=true --pooling=true --restype=javax.sql.DataSource --datasourceclassname=%s --property ", driverDataSource));
            line.append(String.format("user=%s:", dataSource.getUser()));
            line.append(String.format("password=%s:", dataSource.getPassword()));
            line.append(String.format("url=%s:", replaceChars(dataSource.getUrl())));
            dataSource.getProperties().entrySet().forEach(entry -> line.append(String.format("%s=%s:", entry.getKey(), entry.getValue())));
            line.setLength(line.length() - 1);//quitando último dos puntos
            line.append(' ').append(poolName);
            lines.add(line.toString());
            lines.add(String.format("create-jdbc-resource --connectionpoolid %s %s", poolName, dataSourceName));

            Files.write(
                    Path.of(fileName),
                    lines
            );
            log.debug(fileName + " created");

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }
}
