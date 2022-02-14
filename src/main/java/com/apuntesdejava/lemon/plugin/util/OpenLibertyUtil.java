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

import com.apuntesdejava.lemon.jakarta.model.ProjectModel;
import com.apuntesdejava.lemon.jakarta.server.liberty.model.FilesetModel;
import com.apuntesdejava.lemon.jakarta.server.liberty.model.LibraryModel;
import com.apuntesdejava.lemon.jakarta.server.liberty.model.ServerModel;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.Profile;
import org.apache.maven.shared.utils.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class OpenLibertyUtil {

    private static final Path SERVER_XML_PATH = Paths.get("src", "main", "liberty", "config", "server.xml");

    private OpenLibertyUtil() {

    }

    public static void createDataSource(Log log, ProjectModel projectModel, MavenProject mavenProject) {
        try {
            ServerModel serverModel = getServerModel(log, mavenProject);
            serverModel.getFeatureManager().getFeature().add("jdbc-4.3");
            serverModel.setLibrary(
                    new LibraryModel("jdbcLib", new FilesetModel("jdbc", "*.jar"))
            );
            Model model = ProjectModelUtil.getModel(mavenProject);
            Profile profile = ProjectModelUtil.getProfile(model, "openliberty");
            BuildBase build = ProjectModelUtil.getBuild(profile);
            Optional<Plugin> pluginOpt = ProjectModelUtil.addPlugin(build, "io.openliberty.tools", "liberty-maven-plugin", "3.3.4");
//            if (pluginOpt.isPresent()) {
//                Plugin plugin = pluginOpt.get();
//                Xpp3Dom xconf = (Xpp3Dom) plugin.getConfiguration();
//                Xpp3Dom conf = new Xpp3Dom("configuration");
//                Xpp3Dom copyDependencies = new Xpp3Dom("copyDependencies");
//                Xpp3Dom dependencyGroup = new Xpp3Dom("dependencyGroup");
//                Xpp3Dom location = new Xpp3Dom("location");
//                location.setValue("jdbc");
//                Xpp3Dom dependency = new Xpp3Dom("dependency");
//                dependencyGroup.addChild(location);
//                dependencyGroup.addChild(dependency);
//                Xpp3Dom groupId = new Xpp3Dom("groupId");
//                Xpp3Dom artifactId = new Xpp3Dom("artifactId");
//                Xpp3Dom version = new Xpp3Dom("version");
//                dependency.addChild(groupId);
//                dependency.addChild(artifactId);
//                dependency.addChild(version);
//                copyDependencies.addChild(dependencyGroup);
//                conf.addChild(copyDependencies);
//                plugin.setConfiguration(conf);
////                Object conf = plugin.getConfiguration();
////                log.debug("conf:" + conf);
////                plugin.setConfiguration(plugin);
//            }

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

}
