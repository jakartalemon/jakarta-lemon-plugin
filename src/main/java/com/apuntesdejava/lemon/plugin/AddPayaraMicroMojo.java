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

import com.apuntesdejava.lemon.jakarta.model.types.DatasourceDefinitionStyleType;
import static com.apuntesdejava.lemon.plugin.util.Constants.*;
import com.apuntesdejava.lemon.plugin.util.DependenciesUtil;
import com.apuntesdejava.lemon.plugin.util.PayaraUtil;
import com.apuntesdejava.lemon.plugin.util.ProjectModelUtil;
import jakarta.json.JsonObject;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
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
    private JsonObject projectModel;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Override
    public void execute() {
        ProjectModelUtil.getProjectModel(getLog(), _modelProjectFile).ifPresent(pm -> {
            this.projectModel = pm;
            addPlugin();
        });
    }

    private void addPlugin() {
        try {
            getLog().debug("Add Payara Micro Plugin");
            Model model = ProjectModelUtil.getModel(mavenProject);
            Profile profile = ProjectModelUtil.getProfile(model, "payara-micro");
            Properties props = ProjectModelUtil.getProperties(profile);
            DependenciesUtil.getLastVersionDependency(getLog(),
                    "g:fish.payara.extras+AND+a:payara-micro").ifPresent(dependencyModel -> {
                props.setProperty("version.payara", dependencyModel.getVersion());
            });
            var datasource = projectModel.getJsonObject(DATASOURCE);
            BuildBase build = ProjectModelUtil.getBuild(profile);
            ProjectModelUtil.addPlugin(build, "fish.payara.maven.plugins",
                    "payara-micro-maven-plugin", "1.4.0").ifPresent(plugin -> {
                Xpp3Dom conf = ProjectModelUtil.getConfiguration(plugin);
                ProjectModelUtil.addChildren(conf, "payaraVersion").setValue("${version.payara}");
                ProjectModelUtil.addChildren(conf, "deployWar").setValue("false");
                Xpp3Dom commandLineOptions
                        = ProjectModelUtil.addChildren(conf, "commandLineOptions");

                options.forEach(option -> {
                    Xpp3Dom opt = ProjectModelUtil.addChildren(commandLineOptions, "option", true);
                    ProjectModelUtil.addChildren(opt, "key").setValue(option.get(0));
                    if (option.size() > 1) {
                        ProjectModelUtil.addChildren(opt, "value").setValue(option.get(1));
                    }
                });

                DatasourceDefinitionStyleType style = DatasourceDefinitionStyleType.findByValue(
                        datasource.getString(STYLE));
                if (style == DatasourceDefinitionStyleType.PAYARA_RESOURCES) {
                    addPayaraMicroResources(commandLineOptions);
                }
            });
            ProjectModelUtil.addPlugin(build, "org.apache.maven.plugins",
                    "maven-dependency-plugin").ifPresent(plugin -> {
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

                ProjectModelUtil.addDependenciesDatabase(getLog(), artifactItem, datasource.getString(DB));

            });

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

        PayaraUtil.createPayaraMicroDataSourcePostBootFile(getLog(), "post-boot-commands.txt",
                projectModel, mavenProject);

    }

}
