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
package com.apuntesdejava.lemon.plugin;

import com.apuntesdejava.lemon.jakarta.model.ProjectModel;
import com.apuntesdejava.lemon.jakarta.server.liberty.model.ServerModel;
import com.apuntesdejava.lemon.plugin.util.ProjectModelUtil;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Mojo(name = "add-openliberty")
public class AddOpenLibertyMojo extends AbstractMojo {

    @Parameter(
            property = "model",
            defaultValue = "model.json"
    )
    private String _modelProjectFile;
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;
    private ProjectModel projectModel;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Optional<ProjectModel> opt = ProjectModelUtil.getProjectModel(getLog(), _modelProjectFile);
        if (opt.isPresent()) {
            this.projectModel = opt.get();
            addPlugin();
            createServerXml();
        }
    }

    private void addPlugin() {
        try {
            getLog().debug("Add OpenLiberty Plugin");
            File projectFile = mavenProject.getFile();
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(projectFile));
            Profile profile = model.getProfiles()
                    .stream()
                    .filter(p -> p.getId().equals("openliberty"))
                    .findFirst()
                    .orElseGet(() -> {
                        Profile p = new Profile();
                        p.setId("openliberty");
                        model.addProfile(p);
                        return p;
                    });
            BuildBase build = Optional.ofNullable(profile.getBuild())
                    .orElseGet(() -> {
                        BuildBase b = new BuildBase();
                        profile.setBuild(b);
                        return b;
                    });
            PluginManagement pm = Optional.ofNullable(build.getPluginManagement())
                    .orElseGet(() -> {
                        PluginManagement $pm = new PluginManagement();
                        build.setPluginManagement($pm);
                        return $pm;
                    });
            List<Plugin> plugins = pm.getPlugins();
            if (plugins.stream().filter(
                    item -> item.getGroupId().equals("org.apache.maven.plugins")
                    && item.getArtifactId().equals("maven-war-plugin")).count() == 0) {
                Plugin p = new Plugin();
                p.setGroupId("org.apache.maven.plugins");
                p.setArtifactId("maven-war-plugin");
                p.setVersion("3.3.2");
                pm.addPlugin(p);
            }
            if (plugins.stream().filter(
                    item -> item.getGroupId().equals("io.openliberty.tools")
                    && item.getArtifactId().equals("liberty-maven-plugin")).count() == 0) {
                Plugin p = new Plugin();
                p.setGroupId("io.openliberty.tools");
                p.setArtifactId("liberty-maven-plugin");
                p.setVersion("3.5.1");
                pm.addPlugin(p);
            }

            if (build.getPlugins().stream()
                    .filter(
                            item -> item.getGroupId().equals("io.openliberty.tools")
                            && item.getArtifactId().equals("liberty-maven-plugin")
                    ).count() == 0) {
                Plugin p = new Plugin();
                p.setGroupId("io.openliberty.tools");
                p.setArtifactId("liberty-maven-plugin");
                build.addPlugin(p);
            }

            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(new FileWriter(projectFile), model);

        } catch (IOException | XmlPullParserException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createServerXml() {
        try {
            getLog().debug("Creating server.xml file");
            Path baseDirPath = mavenProject.getBasedir().toPath();
            getLog().info("baseDirPath:" + baseDirPath);
            Path serverXmlPath = baseDirPath.resolve(Paths.get("src", "main", "liberty", "config", "server.xml"));
            getLog().info("serverXmlPath:" + serverXmlPath);
            Files.createDirectories(serverXmlPath.getParent());

            ServerModel serverModel = new ServerModel();
            String projectName = mavenProject.getName();
            serverModel.setDescription(projectName);
            serverModel.getWebApplication().setContextRoot("/" + projectName);
            serverModel.getWebApplication().setLocation(projectName + ".war");

            JAXBContext ctx = JAXBContext.newInstance(ServerModel.class);
            Marshaller marshaller = ctx.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(serverModel, serverXmlPath.toFile());
        } catch (IOException | JAXBException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }
}
