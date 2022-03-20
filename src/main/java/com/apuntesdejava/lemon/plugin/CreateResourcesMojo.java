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

import com.apuntesdejava.lemon.jakarta.openapi.model.OpenApiModel;
import com.apuntesdejava.lemon.plugin.util.OpenApiModelUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Mojo(name = "create-rest")
public class CreateResourcesMojo extends AbstractMojo {

    @Parameter(
            property = "openapi",
            defaultValue = "openapi.json"
    )
    private String modelProjectFile;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;
    private OpenApiModel openApiModel;
    private final Map<String, String> componentsMap = new LinkedHashMap<>();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Path path = mavenProject
                    .getBasedir()
                    .toPath()
                    .resolve(modelProjectFile);
            getLog().debug("modelProjectFile:" + path);
            Optional<OpenApiModel> openApiModel = OpenApiModelUtil.getInstance().getModel(path);
            getLog().debug("openApiModel:" + openApiModel);
            if (openApiModel.isPresent()) {
                this.openApiModel = openApiModel.get();
                createComponents();
                createResources();
            }
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createComponents() {
        getLog().debug("Creating components");
        Map<String, Object> components = openApiModel.getComponents();
        ((Map) components.get("schemas")).entrySet().forEach(schema -> {
            Map.Entry<String, Map<String, Object>> item = (Map.Entry<String, Map<String, Object>>) schema;
            getLog().info("schema:" + item);
            String schemaName = item.getKey();
            String type = (String) item.getValue().get("type");
            switch (type) {
                case "object":
                    String className = OpenApiModelUtil.getInstance().createClass(getLog(), mavenProject,
                            schemaName, (Map<String, Map<String, String>>) item.getValue().get("properties"));
                    componentsMap.put(schemaName, className);
                    break;
            }

        });
        getLog().debug("components:" + components);
    }

    private void createResources() {
        getLog().debug("Creating paths");
        Set<String> paths = openApiModel.getPaths().keySet();
        String pathRoot = paths.stream().findFirst().get();
        paths.forEach(path->{
           // StringUtils.
        });
    }

}
