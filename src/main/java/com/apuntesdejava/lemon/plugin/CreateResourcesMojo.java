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
import com.apuntesdejava.lemon.jakarta.openapi.model.OperationModel;
import com.apuntesdejava.lemon.jakarta.openapi.model.PathModel;
import com.apuntesdejava.lemon.plugin.util.Constants;
import com.apuntesdejava.lemon.plugin.util.OpenApiModelUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
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
        try {
            getLog().debug("Creating paths");
            List<String> paths = new ArrayList<>(openApiModel.getPaths().keySet());
            int pos = 0;

            String aPath = paths.get(0);
            //halland root path para ignorarlo en las rutas

            chars:
            for (int i = 0; i < aPath.length(); i++) {
                char rec = aPath.charAt(i);
                for (String path : paths) {
                    if (i > path.length() || path.charAt(i) != rec) {
                        break chars;
                    }
                }
                pos++;
            }
            String rootPath = aPath.substring(0, pos);
            getLog().debug("Root path:" + rootPath);
            Path baseDirPath = mavenProject.getBasedir().toPath();
            Path javaMainSrc = baseDirPath.resolve("src").resolve("main").resolve("java");
            String groupId = mavenProject.getGroupId();
            String[] packagePaths = groupId.split("\\.");
            Path packageBasePath = javaMainSrc;
            for (String packagePath : packagePaths) {
                packageBasePath = packageBasePath.resolve(packagePath);

            }
            final Path packageBaseResources = packageBasePath.resolve("resources");
            Files.createDirectories(packageBaseResources);

            openApiModel.getPaths().entrySet().forEach(entry -> createResource(StringUtils.substringAfter(entry.getKey(), rootPath), entry.getValue(), packageBaseResources));
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createResource(String pathName, PathModel pathModel, Path packageBaseResources) {
        try {
            getLog().debug("path:" + pathName + "\tpathModel:" + pathModel + "\tpackageBaseResources:" + packageBaseResources);
            getLog().info("Creating " + pathName);
            String resourceName = StringUtils.substringBefore(pathName, "/");
            String resourceClassName = StringUtils.capitalize(resourceName) + "Resource";
            Path classPath = packageBaseResources.resolve(resourceClassName + ".java");
            List<String> lines;
            if (Files.exists(classPath)) {
                lines = Files.readAllLines(classPath);
                lines.removeIf(line -> StringUtils.equals(line, "}"));
            } else {
                lines = new ArrayList<>();
                lines.add("package " + mavenProject.getGroupId() + ".resources;");
                lines.add("\nimport jakarta.ws.rs.*;");
                lines.add("import jakarta.ws.rs.core.*;");
                lines.add("\n@Path(\"" + resourceName + "\")");
                lines.add("public class " + resourceClassName + " {");

            }
            if (pathModel.getGet() != null) {
                createOperation(lines, "@GET", pathModel.getGet());
            } else if (pathModel.getPost() != null) {
                createOperation(lines, "@POST", pathModel.getPost());
            } else if (pathModel.getPut() != null) {
                createOperation(lines, "@PUT", pathModel.getPut());
            } else if (pathModel.getDelete() != null) {
                createOperation(lines, "@DELETE", pathModel.getDelete());
            }
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + "return Response.ok().build();");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "}");

            lines.add("}");

            Files.write(classPath, lines);
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createOperation(List<String> lines, String method, OperationModel operationModel) {
        lines.add('\n' + StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + method);
        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "public Response " + operationModel.getOperationId() + "() {");
    }

}
