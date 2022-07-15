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

import com.apuntesdejava.lemon.jakarta.webxml.model.ServletMappingModel;
import com.apuntesdejava.lemon.jakarta.webxml.model.ServletModel;
import com.apuntesdejava.lemon.plugin.util.ViewModelUtil;
import com.apuntesdejava.lemon.plugin.util.WebXmlUtil;
import jakarta.json.JsonObject;
import jakarta.xml.bind.JAXBException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
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

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            getLog().info("Creating view layer...");
            ViewModelUtil.getInstance().getViewModel(getLog(), viewProjectFile).ifPresent(model -> {
                this.viewModel = model;
            });
            String viewStyle = this.viewModel.getString("style", "jsf");
            switch (viewStyle) {
                case "jsf":
                    createJsfViews();
                    break;
            }

        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createJsfViews() throws IOException {
        try {
            getLog().info("Creating Jakarta Server Faces views");
            var baseDir = mavenProject.getBasedir();
            getLog().debug("baseDir:" + baseDir);
            var webXmlUtil = WebXmlUtil.getInstance(baseDir.toString());
            var webxml =  webXmlUtil.getWebxml();
            boolean createServlet = webxml.getServlet() == null
                    || webxml.getServlet()
                            .stream()
                            .filter(item -> item.getServletClass().equals("jakarta.faces.webapp.FacesServlet"))
                            .findFirst().isEmpty();
            if (createServlet) {
                var servletList = Optional.ofNullable(webxml.getServlet()).orElse(new ArrayList<>());
                var servletMappingList = Optional.ofNullable(webxml.getServletMapping()).orElse(new ArrayList<>());
                servletList.add(new ServletModel("Server Faces Servlet", "jakarta.faces.webapp.FacesServlet"));
                servletMappingList.add(new ServletMappingModel("Server Faces Servlet", "*.jsf"));
                webxml.setServlet(servletList);
                webxml.setServletMapping(servletMappingList);

                webXmlUtil.saveWebXml(webxml);
            }

        } catch (JAXBException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

}
