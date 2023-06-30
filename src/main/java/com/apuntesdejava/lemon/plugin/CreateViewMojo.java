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

import com.apuntesdejava.lemon.plugin.util.DependenciesUtil;
import com.apuntesdejava.lemon.plugin.util.ProjectModelUtil;
import com.apuntesdejava.lemon.plugin.util.ViewModelUtil;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.IOException;
import java.util.Map;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Mojo(name = "create-view")
public class CreateViewMojo extends AbstractMojo {

    @Parameter(
        defaultValue = "${project}",
        readonly = true
    )
    private MavenProject mavenProject;

    @Parameter(
        property = "view",
        defaultValue = "view.json"
    )
    private String viewProjectFile;
    private JsonObject viewModel;
    private Dependency primeflexDependency;

    /**
     * Main method that runs the Plugin
     *
     * @throws MojoExecutionException if Mojo Execution Exception
     * @throws MojoFailureException   if Mojo Failure Exception
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("Creating view layer...");
            var viewModelUtil = ViewModelUtil.getInstance(getLog(), mavenProject);
            viewModelUtil.getViewModel(viewProjectFile).ifPresent(model -> this.viewModel = model);
            String viewStyle = this.viewModel.getString(VIEW_STYLE, VIEW_STYLE_JSF);
            if (VIEW_STYLE_JSF.equals(viewStyle)) {
                addJsfDependencies();
                viewModelUtil.createServletJsf();
                viewModelUtil.createViews(this.viewModel, primeflexDependency.getVersion());
                var pathsObject = this.viewModel.getJsonObject(PATHS);

                var viewsForIndex = pathsObject.entrySet().stream()
                    .filter(item -> item.getValue().asJsonObject().getString(TYPE).equalsIgnoreCase(LIST))
                    .collect(Json::createArrayBuilder, (arrayBuilder, valueEntry) ->
                            arrayBuilder.add(Json.createObjectBuilder().add(valueEntry.getKey(), valueEntry.getValue())),
                        JsonArrayBuilder::addAll)
                    .build();

                viewModelUtil.createIndexPage(viewsForIndex, primeflexDependency.getVersion());
            }
            DependenciesUtil.addProjectLombokDependency(getLog(), mavenProject);

        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void addJsfDependencies() {
        try {
            getLog().debug("Modifing pom.xml");
            Model model = ProjectModelUtil.getModel(mavenProject);
            ProjectModelUtil.addDependency(
                getLog(),
                model.getDependencies(),
                PRIMEFACES_GROUP_ID,
                PRIMEFACES_ARTIFACT_ID,
                Map.of(CLASSIFIER, "jakarta")
            );
            this.primeflexDependency = ProjectModelUtil.addDependency(
                getLog(), model.getDependencies(), PRIMEFLEX_GROUP_ID, PRIMEFLEX_ARTIFACT_ID
            );
            ProjectModelUtil.saveModel(mavenProject, model);
        } catch (IOException | XmlPullParserException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

}
