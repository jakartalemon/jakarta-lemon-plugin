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

import com.apuntesdejava.lemon.jakarta.jpa.model.ProjectModel;
import com.apuntesdejava.lemon.jakarta.server.liberty.model.ServerModel;
import com.apuntesdejava.lemon.plugin.util.OpenLibertyUtil;
import com.apuntesdejava.lemon.plugin.util.ProjectModelUtil;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.util.Optional;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.Profile;
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
            Model model = ProjectModelUtil.getModel(mavenProject);
            Profile profile = ProjectModelUtil.getProfile(model, "openliberty");
            BuildBase build = ProjectModelUtil.getBuild(profile);
            PluginManagement pm = ProjectModelUtil.getPluginManagement(build);
            ProjectModelUtil.addPlugin(pm, "org.apache.maven.plugins", "maven-war-plugin", "3.3.2");
            ProjectModelUtil.addPlugin(pm, "io.openliberty.tools", "liberty-maven-plugin", "3.5.1");
            ProjectModelUtil.addPlugin(build, "io.openliberty.tools", "liberty-maven-plugin");

            ProjectModelUtil.saveModel(mavenProject, model);

        } catch (IOException | XmlPullParserException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createServerXml() {
        try {
            ServerModel serverModel = OpenLibertyUtil.getServerModel(getLog(), mavenProject);
            OpenLibertyUtil.saveServerModel(mavenProject, serverModel);
        } catch (IOException | JAXBException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }
}
