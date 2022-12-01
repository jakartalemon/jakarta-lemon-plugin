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

import com.apuntesdejava.lemon.jakarta.liberty.model.*;
import jakarta.json.JsonObject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class OpenLibertyUtil {

    private static final Path SERVER_XML_PATH = Paths.get("src", "main", "liberty", "config", "server.xml");

    public static void createDataSource(Log log, JsonObject projectModel, MavenProject mavenProject) {
        try {
            log.debug("Updating server.xml");
            ServerModel serverModel = getServerModel(log, mavenProject);
            serverModel.getFeatureManager().getFeature().add("jdbc-4.3");
            serverModel.setLibrary(
                    new OpenLibertyLibraryModel(
                            "jdbcLib", new FilesetModel(
                            "jdbc",
                            "*.jar"
                    )
                    )
            );
            String jndiName = "jdbc/" + mavenProject.getArtifactId();
            OpenLibertyDataSourcePropertiesModel properties = new OpenLibertyDataSourcePropertiesModel();
            var datasourceModel = projectModel.getJsonObject(DATASOURCE);
            properties.setUrl(datasourceModel.getString(URL));
            properties.setUser(datasourceModel.getString(USER));
            properties.setPassword(datasourceModel.getString(PASSWORD));
            var props = datasourceModel.getJsonObject(PROPERTIES);
            props.keySet().forEach(propName -> {
                var value = props.getString(propName);
                if (PropertyUtils.isWriteable(properties, propName)) {
                    try {
                        PropertyUtils.setProperty(properties, propName, value);
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ex) {
                        log.warn(ex.getMessage());
                    }
                }
            });


            serverModel.setDataSource(new OpenLibertyDataSourceModel(
                    jndiName,
                    new OpenLibertyJdbcDriverModel("jdbcLib"),
                    properties)
            );

            log.debug("Modifing pom.xml");
            Model model = ProjectModelUtil.getModel(mavenProject);
            Profile profile = ProjectModelUtil.getProfile(model, "openliberty");
            BuildBase build = ProjectModelUtil.getBuild(profile);
            Optional<Plugin> pluginOpt = ProjectModelUtil.addPlugin(build, "io.openliberty.tools", "liberty-maven-plugin", "3.3.4");
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

        } catch (IOException | XmlPullParserException | JAXBException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public static ServerModel getServerModel(Log log, MavenProject mavenProject) throws JAXBException, IOException {
        log.debug("Creating server.xml file");
        Path baseDirPath = mavenProject.getBasedir().toPath();
        log.debug("baseDirPath:" + baseDirPath);
        Path serverXmlPath = baseDirPath.resolve(SERVER_XML_PATH);
        log.debug("serverXmlPath:" + serverXmlPath);
        Files.createDirectories(serverXmlPath.getParent());
        if (Files.exists(serverXmlPath)) {
            JAXBContext ctx = JAXBContext.newInstance(ServerModel.class);
            Unmarshaller unmarshaller = ctx.createUnmarshaller();
            return (ServerModel) unmarshaller.unmarshal(serverXmlPath.toFile());
        }
        String projectName = mavenProject.getName();
        ServerModel serverModel = new ServerModel();
        serverModel.setDescription(projectName);
        serverModel.getWebApplication().setContextRoot("/" + projectName);
        serverModel.getWebApplication().setLocation(projectName + ".war");
        return serverModel;
    }

    public static void saveServerModel(MavenProject mavenProject, ServerModel serverModel) throws JAXBException {
        Path baseDirPath = mavenProject.getBasedir().toPath();
        Path serverXmlPath = baseDirPath.resolve(SERVER_XML_PATH);
        JAXBContext ctx = JAXBContext.newInstance(ServerModel.class);
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(serverModel, serverXmlPath.toFile());

    }

    private OpenLibertyUtil() {

    }

}
