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

import com.apuntesdejava.lemon.plugin.util.ProjectModelUtil;
import com.apuntesdejava.lemon.plugin.util.ViewModelUtil;
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

import static com.apuntesdejava.lemon.plugin.util.Constants.VIEW_STYLE;
import static com.apuntesdejava.lemon.plugin.util.Constants.VIEW_STYLE_JSF;

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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("Creating view layer...");
            var viewModelUtil = ViewModelUtil.getInstance(getLog(), mavenProject);
            viewModelUtil.getViewModel(viewProjectFile).ifPresent(model -> {
                this.viewModel = model;
            });
            String viewStyle = this.viewModel.getString(VIEW_STYLE, VIEW_STYLE_JSF);
            if (VIEW_STYLE_JSF.equals(viewStyle)) {
                addJsfDependencies();
                viewModelUtil.createServletJsf();
                viewModelUtil.createViews(this.viewModel, primeflexDependency.getVersion());

            }

        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }


    private void addJsfDependencies() {
        try {
            getLog().debug("Modifing pom.xml");
            Model model = ProjectModelUtil.getModel(mavenProject);
            ProjectModelUtil.addDependency(getLog(), model, "org.primefaces", "primefaces",
                                           Map.of("classifier", "jakarta"));
            this.primeflexDependency = ProjectModelUtil.addDependency(getLog(), model, "org.webjars.npm", "primeflex");
            ProjectModelUtil.saveModel(mavenProject, model);
        } catch (IOException | XmlPullParserException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

}
