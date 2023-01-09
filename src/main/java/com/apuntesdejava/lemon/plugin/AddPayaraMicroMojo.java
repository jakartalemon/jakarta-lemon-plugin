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
import com.apuntesdejava.lemon.plugin.util.DependenciesUtil;
import com.apuntesdejava.lemon.plugin.util.PayaraUtil;
import com.apuntesdejava.lemon.plugin.util.ProjectModelUtil;
import jakarta.json.JsonObject;
import org.apache.maven.model.BuildBase;
import org.apache.maven.model.Model;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Mojo(name = "add-payara-micro")
public class AddPayaraMicroMojo extends AbstractMojo {

    private static final List<Map<String, String>> OPTIONS_LIST
        = List.of(
            Map.of(KEY, "--autoBindHttp"),
            Map.of(KEY, "--deploy", VALUE, "${project.build.directory}/${project.build.finalName}"),
            Map.of(KEY, "--postbootcommandfile", VALUE, "post-boot-commands.txt"),
            Map.of(KEY, "--contextroot", VALUE, String.valueOf(SLASH)),
            Map.of(KEY, "--addlibs", VALUE, TARGET_LIB)
        );

    @Parameter(
        property = "model",
        defaultValue = "model.json"
    )
    private String _modelProjectFile;
    private JsonObject projectModel;

    @Parameter(
        defaultValue = "${project}",
        readonly = true
    )
    private MavenProject mavenProject;

    /**
     * Main method that runs the Plugin
     */
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
            DependenciesUtil.getLastVersionDependency(getLog(), "g:fish.payara.extras+AND+a:payara-micro")
                .ifPresent(dependencyModel -> props.setProperty("version.payara", dependencyModel.getString(DEPENDENCY_VERSION)));
            var datasource = projectModel.getJsonObject(DATASOURCE);
            BuildBase build = ProjectModelUtil.getBuild(profile);
            List<Map<String, String>> commandLineOptionsList = new ArrayList<>(OPTIONS_LIST);
            DatasourceDefinitionStyleType style = DatasourceDefinitionStyleType.findByValue(datasource.getString(STYLE));
            if (style == DatasourceDefinitionStyleType.PAYARA_RESOURCES) {
                commandLineOptionsList.addAll(
                    List.of(
                        Map.of(
                            KEY, "--postbootcommandfile",
                            VALUE, "post-boot-commands.txt"
                        ),
                        Map.of(
                            KEY, "--addLibs",
                            VALUE, TARGET_LIB
                        )
                    )
                );
                PayaraUtil.createPayaraMicroDataSourcePostBootFile(getLog(), "post-boot-commands.txt", projectModel, mavenProject);
            }
            ProjectModelUtil.addPlugin(build, "fish.payara.maven.plugins", "payara-micro-maven-plugin", "1.4.0",
                Map.of("payaraVersion", "${version.payara}",
                    "deployWar", "false",
                    "commandLineOptions", commandLineOptionsList));

            ProjectModelUtil.addPlugin(build, MAVEN_PLUGIN_GROUP_ID, "maven-dependency-plugin")
                .ifPresent(plugin -> {
                    PluginExecution execution = plugin.getExecutions()
                        .stream()
                        .filter(exec -> exec.getId().equals(COPY_JDBC))
                        .findFirst()
                        .orElseGet(() -> {
                            PluginExecution pe = new PluginExecution();
                            plugin.addExecution(pe);
                            pe.setId(COPY_JDBC);
                            return pe;
                        });
                    execution.getGoals().stream().filter(goal -> goal.equals(COPY)).findFirst().orElseGet(() -> {
                        execution.addGoal(COPY);
                        return COPY;
                    });
                    DependenciesUtil.getByDatabase(getLog(), datasource.getString(DB)).ifPresent(dependen -> ProjectModelUtil.setConfigurationOptions(execution, Map.of(
                        "outputDirectory", TARGET_LIB,
                        "stripVersion", "true",
                        "artifactItems", Map.of(
                            "artifactItem", Map.of(
                                DEPENDENCY_GROUP_ID, dependen.getString(DEPENDENCY_GROUP_ID),
                                DEPENDENCY_ARTIFACT_ID, dependen.getString(DEPENDENCY_ARTIFACT_ID),
                                DEPENDENCY_VERSION, dependen.getString(DEPENDENCY_VERSION)
                            )
                        )
                    )));

                });

            ProjectModelUtil.saveModel(mavenProject, model);
        } catch (XmlPullParserException | IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

}
