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
package com.apuntesdejava.lemon.plugin.util;

import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.xml.bind.JAXBException;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;
import jakarta.json.JsonString;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class OpenLibertyUtil {

    private static final Path SERVER_XML_PATH = Paths.get("src", "main", "liberty", "config", "server.xml");

    private OpenLibertyUtil() {

    }

    public static void createDataSource(Log log,
            JsonObject projectModel,
            MavenProject mavenProject) {
        try {
            log.info("Updating server.xml");
            getServerModel(log, mavenProject, Collections.emptyMap())
                    .ifPresent(serverModel -> {
                        try {
                           
                            DocumentXmlUtil.createElement(serverModel, "/server", "library")
                                    .ifPresent(libraryElement -> {
                                        libraryElement.setAttribute("id", "jdbcLib");
                                        DocumentXmlUtil.createElement(serverModel, libraryElement, "fileset")
                                                .ifPresent(filesetElement -> {
                                                    filesetElement.setAttribute("dir", "jdbc");
                                                    filesetElement.setAttribute("includes", "*.jar");
                                                });
                                    });

                            String jndiName = "jdbc/" + mavenProject.getArtifactId();
                            var datasourceModel = projectModel.getJsonObject(DATASOURCE);

                            DocumentXmlUtil.createElement(serverModel, "server", "dataSource")
                                    .ifPresent(dataSourceElement -> {
                                        dataSourceElement.setAttribute("jndiName", jndiName);
                                        DocumentXmlUtil.createElement(serverModel, dataSourceElement, "jdbcDriver")
                                                .ifPresent(jdbcDriverElement -> jdbcDriverElement.setAttribute("libraryRef", "jdbcLib"));
                                        DocumentXmlUtil.createElement(serverModel, dataSourceElement, "properties")
                                                .ifPresent(propertiesElement -> {
                                                    propertiesElement.setAttribute(URL, datasourceModel.getString(URL));
                                                    propertiesElement.setAttribute(USER, datasourceModel.getString(USER));
                                                    propertiesElement.setAttribute(PASSWORD, datasourceModel.getString(PASSWORD));
                                                    var props = datasourceModel.getJsonObject(PROPERTIES);
                                                    props.keySet().forEach(key -> {
                                                        var value = props.getString(key);
                                                        propertiesElement.setAttribute(key, value);
                                                    });

                                                });
                                    });

                            log.debug("Modifing pom.xml");
                            Model model = ProjectModelUtil.getModel(mavenProject);
                            Profile profile = ProjectModelUtil.getProfile(model, "openliberty");
                            BuildBase build = ProjectModelUtil.getBuild(profile);
                            Optional<Plugin> pluginOpt = ProjectModelUtil.addPlugin(build, "io.openliberty.tools",
                                    "liberty-maven-plugin", "3.3.4");
                            if (pluginOpt.isPresent()) {
                                Plugin plugin = pluginOpt.get();
                                Xpp3Dom conf = ProjectModelUtil.getConfiguration(plugin);
                                Xpp3Dom copyDependencies = ProjectModelUtil.addChildren(conf, "copyDependencies");
                                Xpp3Dom dependencyGroup = ProjectModelUtil.addChildren(copyDependencies, "dependencyGroup");
                                Xpp3Dom location = ProjectModelUtil.addChildren(dependencyGroup, "location");
                                location.setValue("jdbc");

                                Xpp3Dom dependency = ProjectModelUtil.addChildren(dependencyGroup, "dependency");
                                ProjectModelUtil.addDependenciesDatabase(log, dependency, datasourceModel.getString(DB));

                                ProjectModelUtil.saveModel(mavenProject, model);
                            }

                            saveServerModel(mavenProject, serverModel);
                        } catch (IOException | XmlPullParserException | JAXBException | XPathExpressionException ex) {
                            log.error(ex.getMessage(), ex);
                        }
                    });
        } catch (IOException | JAXBException | ParserConfigurationException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public static Optional<Document> getServerModel(Log log,
            MavenProject mavenProject,
            Map<String, String> options) throws JAXBException, IOException, ParserConfigurationException {
        log.info("Creating server.xml file");
        Path baseDirPath = mavenProject.getBasedir().toPath();
        log.debug("baseDirPath:" + baseDirPath);
        Path serverXmlPath = baseDirPath.resolve(SERVER_XML_PATH);
        log.debug("serverXmlPath:" + serverXmlPath);
        Files.createDirectories(serverXmlPath.getParent());
        if (Files.exists(serverXmlPath)) {
            return DocumentXmlUtil.openDocument(serverXmlPath);
        }
        var document = DocumentXmlUtil.newDocument("server");
        try {
            DocumentXmlUtil.findElementsByFilter(document, "/server")
                    .stream()
                    .findFirst()
                    .ifPresent(serverElement -> {
                        try {
                            String projectName = mavenProject.getName();
                            serverElement.setAttribute("description", projectName);
                            var config = HttpClientUtil.getJson(log, LEMON_CONFIG_URL, JsonReader::readObject);
                            var features = config.getJsonObject("openliberty")
                                    .getJsonObject("server")
                                    .getJsonObject("featureManager")
                                    .getJsonArray("feature");

                            DocumentXmlUtil.createElement(document, serverElement, "featureManager")
                                    .ifPresent(featureManagerElement -> features.stream()
                                    .map(item -> ((JsonString) item).getString())
                                    .forEach(featureItem -> DocumentXmlUtil.createElement(document, featureManagerElement,
                                    "feature", featureItem)));

                            DocumentXmlUtil.createElement(document, serverElement, "applicationManager")
                                    .ifPresent(applicationManagerElement -> applicationManagerElement.setAttribute("autoExpand", "true"));
                            DocumentXmlUtil.createElement(document, serverElement, "webApplication")
                                    .ifPresent(webApplicationElement -> {
                                        webApplicationElement.setAttribute("contextRoot", "/" + projectName);
                                        webApplicationElement.setAttribute("location", projectName + ".war");
                                    });
                            if (options.containsKey(LIBERTY_VAR_DEFAULT_HTTP_PORT) || options.containsKey(LIBERTY_VAR_DEFAULT_HTTPS_PORT)) {
                                DocumentXmlUtil.createElement(document, serverElement, "httpEndpoint")
                                        .ifPresent(httpEndpointElement -> {
                                            httpEndpointElement.setAttribute("id", "defaultHttpEndpoint");
                                            if (options.containsKey(LIBERTY_VAR_DEFAULT_HTTP_PORT)) {
                                                httpEndpointElement.setAttribute("httpPort", options.get(LIBERTY_VAR_DEFAULT_HTTP_PORT));
                                            }
                                            if (options.containsKey(LIBERTY_VAR_DEFAULT_HTTPS_PORT)) {
                                                httpEndpointElement.setAttribute("httpsPort", options.get(LIBERTY_VAR_DEFAULT_HTTPS_PORT));
                                            }
                                        });
                            }
                        } catch (IOException | InterruptedException | URISyntaxException ex) {
                            log.error(ex.getMessage(), ex);
                        }
                    });
        } catch (XPathExpressionException ex) {
            log.error(ex.getMessage(), ex);
        }

        return Optional.of(document);

    }

    public static void saveServerModel(MavenProject mavenProject,
            Document serverModel) throws JAXBException {
        Path baseDirPath = mavenProject.getBasedir().toPath();
        Path serverXmlPath = baseDirPath.resolve(SERVER_XML_PATH);
        DocumentXmlUtil.saveDocument(serverXmlPath, serverModel);

    }

}
