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

import com.apuntesdejava.lemon.plugin.util.Constants;
import com.apuntesdejava.lemon.plugin.util.OpenApiModelUtil;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;
import static com.apuntesdejava.lemon.plugin.util.OpenApiModelUtil.getJavaType;
import static java.util.stream.Collectors.joining;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
@Mojo(name = "create-rest")
public class CreateResourcesMojo extends AbstractMojo {

    private final Map<String, String> componentsMap = new LinkedHashMap<>();
    @Parameter(
        property = "openapi",
        defaultValue = "openapi.json"
    )
    private String modelProjectFile;
    @Parameter(
        defaultValue = "${project}",
        readonly = true
    )
    private MavenProject mavenProject;
    private String packageName;
    private JsonObject openApiModel;
/**
 * Main method that runs the Plugin
 * @throws MojoExecutionException if Mojo Execution Exception
 * @throws MojoFailureException if Mojo Failure Exception 
 */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Path path = mavenProject.getBasedir().toPath().resolve(modelProjectFile);
            getLog().debug("modelProjectFile:" + path);
            this.openApiModel = OpenApiModelUtil.getInstance().getModel(path);
            getLog().debug("openApiModel:" + openApiModel);
            this.packageName = StringUtils.replaceChars(mavenProject.getGroupId() + '.' + mavenProject.getArtifactId(), '-', '.');
            createComponents();
            createResources();
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createComponents() {
        getLog().debug("Creating components");
        JsonObject components = openApiModel.getJsonObject(COMPONENTS);
        components.getJsonObject(SCHEMAS).forEach((schemaName, item) -> {
            getLog().info("schema:" + item);
            String type = item.asJsonObject().getString(TYPE);

            if (OBJECT.equals(type)) {
                String className = OpenApiModelUtil.getInstance()
                    .createClass(getLog(), packageName, mavenProject, schemaName, item.asJsonObject()
                        .getJsonObject(PROPERTIES));
                componentsMap.put(schemaName, className);
            }

        });
        getLog().debug("components:" + components);
    }

    private void createResources() {
        try {
            getLog().debug("Creating paths");
            List<String> paths = new ArrayList<>(openApiModel.getJsonObject(PATHS).keySet());
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
            Path javaMainSrc = baseDirPath.resolve(SRC_PATH).resolve(MAIN_PATH).resolve(JAVA_PATH);
            String groupId = packageName; // mavenProject.getGroupId();
            String[] packagePaths = groupId.split("\\.");
            Path packageBasePath = javaMainSrc;
            for (String packagePath : packagePaths) {
                packageBasePath = packageBasePath.resolve(packagePath);

            }
            final Path packageBaseResources = packageBasePath.resolve(RESOURCES);
            Files.createDirectories(packageBaseResources);

            openApiModel.getJsonObject(PATHS)
                .forEach((key, value) -> createResource(StringUtils.substringAfter(key, rootPath), value
                    .asJsonObject(), packageBaseResources));
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createResource(String pathName, JsonObject pathModel, Path packageBaseResources) {
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
                lines.add("package " + packageName + ".resources;");
                lines.add("\nimport jakarta.ws.rs.*;");
                lines.add("import jakarta.ws.rs.core.*;");
                lines.add("\n@Path(\"" + resourceName + "\")");
                lines.add("public class " + resourceClassName + " {");

            }
            JsonObject operation = null;
            if (pathModel.containsKey(GET)) {
                createOperation(lines, "@GET", operation = pathModel.getJsonObject(GET), pathName, resourceName);
            } else if (pathModel.containsKey(POST)) {
                createOperation(lines, "@POST", operation = pathModel.getJsonObject(POST), pathName, resourceName);
            } else if (pathModel.containsKey(PUT)) {
                createOperation(lines, "@PUT", operation = pathModel.getJsonObject(PUT), pathName, resourceName);
            } else if (pathModel.containsKey(DELETE)) {
                createOperation(lines, "@DELETE", operation = pathModel.getJsonObject(DELETE), pathName, resourceName);
            }
//preparando response

            JsonObject response = (operation == null) ? null : operation.getJsonObject(DEFAULT);
            if (response != null && response.containsKey(CONTENT)) {
                JsonObject content = response.getJsonObject(CONTENT);
                var schema = (Map<String, Object>) content.get(SCHEMA);
                var type = (String) schema.get(TYPE);
                var items = (Map<String, String>) schema.get(ITEMS);
                var $ref = items != null ? items.get(REF) : (String) schema.get(REF);
                String onlyClassName = "";
                if (StringUtils.isNotBlank($ref)) {
                    String modelResponse = componentsMap.get(StringUtils.substringAfterLast($ref, "/"));
                    String line = "import " + modelResponse + ";";
                    if (!lines.contains(line)) {
                        lines.add(2, line);
                    }
                    onlyClassName = StringUtils.substringAfterLast(modelResponse, ".");
                }
                if (StringUtils.equalsAnyIgnoreCase(type, ARRAY)) {
                    lines.add(2, "import java.util.Collections;");
                    lines.add(2, "import java.util.List;");
                    lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + "List<" + onlyClassName + "> response = Collections.emptyList();");

                } else {
                    lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + onlyClassName + " response = new " + onlyClassName + "();");
                }
                lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + "return Response.ok(response).build();");
            } else {
                lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + "return Response.ok().build();");
            }
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "}");

            lines.add("}");

            Files.write(classPath, lines);
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createOperation(List<String> lines, String method, JsonObject operationModel, String pathName, String resourceName) {
        lines.add(StringUtils.EMPTY);
        boolean paramsIn = operationModel.containsKey(PARAMETERS) && operationModel.getJsonArray(PARAMETERS)
            .stream()
            .map(JsonValue::asJsonObject)
            .anyMatch(item -> item.containsKey(IN) && StringUtils.equals(item.getString(IN), PATH));
        if (paramsIn) {
            String operationPath = StringUtils.substringBetween(StringUtils.substringAfter(pathName, resourceName), "{", "}");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "@Path(\"{" + operationPath + "}\")");
        }
        JsonObject response = operationModel.getJsonObject(RESPONSES).getJsonObject(DEFAULT);
        if (response != null && response.containsKey(CONTENT)) {
            String mimeType = String.join("\",\"", response.getJsonObject(CONTENT).keySet());
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "@Produces(\"" + mimeType + "\")");
        }
        StringBuilder bodyParams = new StringBuilder();
        if (operationModel.containsKey(REQUEST_BODY)) {
            var requestBody = operationModel.getJsonObject(REQUEST_BODY);
            getLog().debug("requestBody:" + requestBody);
            if (requestBody != null) {
                var content = requestBody.getJsonObject(CONTENT);
                String mimeType = String.join("\",\"", content.keySet());
                lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "@Consumes(\"" + mimeType + "\")");
                var schemaOpt = content.values().stream().findFirst();
                if (schemaOpt.isPresent()) {
                    var schema = schemaOpt.get().asJsonObject().getJsonObject(SCHEMA);

                    String modelRequest = componentsMap.get(StringUtils.substringAfterLast(schema.getString(REF), "/"));
                    String line = "import " + modelRequest + ";";
                    if (!lines.contains(line)) {
                        lines.add(2, line);
                    }
                    var className = StringUtils.substringAfterLast(modelRequest, ".");
                    bodyParams.append(className).append(" request");
                }
            }
        }
        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + method);
        String parameters = !operationModel.containsKey(PARAMETERS) ? StringUtils.EMPTY : operationModel.getJsonArray(PARAMETERS)
            .stream()
            .map(JsonValue::asJsonObject)
            .map(param -> {
                StringBuilder result = new StringBuilder();
                if (PATH.equals(param.getString(IN))) {
                    result.append("@PathParam(\"").append(param.getString(NAME)).append("\") ");
                }
                result.append(getJavaType(param.getJsonObject(SCHEMA).getString(TYPE)))
                    .append(' ')
                    .append(param.getString(NAME));
                return result.toString();
            })
            .collect(joining(","));
        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + StringUtils.replaceEach("public Response {operationId}({parameters}) {", new String[]{"{operationId}", "{parameters}"}, new String[]{operationModel.getString("operationId"), bodyParams.length() == 0 ? parameters : bodyParams.toString()}));
    }

}
