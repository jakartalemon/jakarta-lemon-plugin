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
package com.apuntesdejava.lemon.plugin;

import com.apuntesdejava.lemon.jakarta.jpa.model.ProjectModel;
import com.apuntesdejava.lemon.jakarta.model.types.DatasourceDefinitionStyleType;
import com.apuntesdejava.lemon.plugin.util.PayaraUtil;
import com.apuntesdejava.lemon.plugin.util.ProjectModelUtil;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.apache.maven.model.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Mojo(name = "add-payara-micro")
public class AddPayaraMicroMojo extends AbstractMojo {

    private static final List<List<String>> options = List.of(
            List.of("--autoBindHttp"),
            List.of("--deploy", "${project.build.directory}/${project.build.finalName}"),
            List.of("--postbootcommandfile", "post-boot-commands.txt"),
            List.of("--contextroot", "/"),
            List.of("--addlibs", "target/lib")
    );

    @Parameter(
            property = "model",
            defaultValue = "model.json"
    )
    private String _modelProjectFile;
    private ProjectModel projectModel;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Override
    public void execute() {
        Optional<ProjectModel> opt = ProjectModelUtil.getProjectModel(getLog(), _modelProjectFile);
        if (opt.isPresent()) {
            this.projectModel = opt.get();
            addPlugin();
        }
    }

    private void addPlugin() {
        try {
            getLog().debug("Add Payara Micro Plugin");
            Model model = ProjectModelUtil.getModel(mavenProject);
            Profile profile = ProjectModelUtil.getProfile(model, "payara-micro");
            Properties props = ProjectModelUtil.getProperties(profile);
            props.setProperty("version.payara", "5.2022.1");
            BuildBase build = ProjectModelUtil.getBuild(profile);
            Optional<Plugin> payaraPlugin = ProjectModelUtil.addPlugin(build, "fish.payara.maven.plugins", "payara-micro-maven-plugin", "1.4.0");
            if (payaraPlugin.isPresent()) {
                Plugin plugin = payaraPlugin.get();
                Xpp3Dom conf = ProjectModelUtil.getConfiguration(plugin);
                ProjectModelUtil.addChildren(conf, "payaraVersion").setValue("${version.payara}");
                ProjectModelUtil.addChildren(conf, "deployWar").setValue("false");
                Xpp3Dom commandLineOptions = ProjectModelUtil.addChildren(conf, "commandLineOptions");

                options.forEach(option -> {
                    Xpp3Dom opt = ProjectModelUtil.addChildren(commandLineOptions, "option", true);
                    ProjectModelUtil.addChildren(opt, "key").setValue(option.get(0));
                    if (option.size() > 1) {
                        ProjectModelUtil.addChildren(opt, "value").setValue(option.get(1));
                    }
                });

                DatasourceDefinitionStyleType style = DatasourceDefinitionStyleType.findByValue(projectModel.getDatasource().getStyle());
                if (style == DatasourceDefinitionStyleType.PAYARA_RESOURCES) {
                    addPayaraMicroResources(commandLineOptions);
                }
            }
            Optional<Plugin> mavenDependencyPlugin = ProjectModelUtil.addPlugin(build, "org.apache.maven.plugins", "maven-dependency-plugin");
            if (mavenDependencyPlugin.isPresent()) {
                Plugin plugin = mavenDependencyPlugin.get();
                PluginExecution execution = plugin.getExecutions()
                        .stream()
                        .filter(exec -> exec.getId().equals("copy-jdbc"))
                        .findFirst()
                        .orElseGet(() -> {
                            PluginExecution pe = new PluginExecution();
                            plugin.addExecution(pe);
                            pe.setId("copy-jdbc");
                            return pe;
                        });
                execution.getGoals()
                        .stream()
                        .filter(goal -> goal.equals("copy"))
                        .findFirst()
                        .orElseGet(() -> {
                            execution.addGoal("copy");
                            return "copy";
                        });
                Xpp3Dom conf = ProjectModelUtil.getConfiguration(execution);
                ProjectModelUtil.addChildren(conf, "outputDirectory").setValue("target/lib");
                ProjectModelUtil.addChildren(conf, "stripVersion").setValue("true");
                Xpp3Dom artifactItems = ProjectModelUtil.addChildren(conf, "artifactItems");
                Xpp3Dom artifactItem = ProjectModelUtil.addChildren(artifactItems, "artifactItem");

                ProjectModelUtil.addDependenciesDatabase(artifactItem, projectModel.getDatasource().getDb());

            }

            ProjectModelUtil.saveModel(mavenProject, model);
        } catch (XmlPullParserException | IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void addPayaraMicroResources(Xpp3Dom commandLineOptions) {
        Xpp3Dom opt1 = ProjectModelUtil.addChildren(commandLineOptions, "option");
        ProjectModelUtil.addChildren(opt1, "key").setValue("--postbootcommandfile");
        ProjectModelUtil.addChildren(opt1, "value").setValue("post-boot-commands.txt");

        Xpp3Dom opt2 = ProjectModelUtil.addChildren(commandLineOptions, "option");
        ProjectModelUtil.addChildren(opt2, "key").setValue("--addLibs");
        ProjectModelUtil.addChildren(opt2, "value").setValue("target/lib");

        PayaraUtil.createPayaraMicroDataSourcePostBootFile(getLog(), "post-boot-commands.txt", projectModel, mavenProject);

    }

}
