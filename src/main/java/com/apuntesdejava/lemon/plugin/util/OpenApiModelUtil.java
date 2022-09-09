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
package com.apuntesdejava.lemon.plugin.util;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class OpenApiModelUtil {

    private OpenApiModelUtil() {
    }

    public static OpenApiModelUtil getInstance() {
        return OpenApiModelUtilHolder.INSTANCE;
    }

    public String createClass(Log log, String packageName, MavenProject mavenProject, String schemaName, JsonObject properties) {
        try {
            Path basedir = mavenProject.getBasedir().toPath();
            log.debug("basedir:" + basedir);
            log.debug("schemaName:" + schemaName);
            if (schemaName.endsWith("Request")) {
                packageName += ".request";
            } else if (schemaName.endsWith("Response")) {
                packageName += ".response";
            }
            String[] paths = packageName.split("\\.");
            Path packageFile = Paths.get(mavenProject.getBasedir().toPath().resolve("src").resolve("main").resolve("java").toString(), paths);
            Files.createDirectories(packageFile);

            Path classFile = packageFile.resolve(schemaName + ".java");
            List<String> content = new ArrayList<>();
            content.add("package " + packageName + ";\n");
            content.add("@lombok.Data");
            content.add("public class " + schemaName + " {\n");
            properties.entrySet().forEach((entry0) -> {
                String fieldName = entry0.getKey();
                String type = getJavaType(entry0.getValue().asJsonObject().getString("type"));
                content.add(StringUtils.repeat(StringUtils.SPACE, 4) + "private " + type + " " + fieldName + ";");
            });
            content.add("}");

            Files.write(classFile, content);
            return packageName + "." + schemaName;

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;
    }

    public static String getJavaType(String schemaType) {
        switch (schemaType) {
            case "string":
                return "String";
            case "integer":
                return "Integer";
        }
        return schemaType;
    }

    private static class OpenApiModelUtilHolder {

        private static final OpenApiModelUtil INSTANCE = new OpenApiModelUtil();
    }

    public JsonObject getModel(Path modelProjectFile) throws IOException {
        try ( InputStream in = new FileInputStream(modelProjectFile.toFile());  JsonReader reader = Json.createReader(in)) {
            return reader.readObject();
        }
    }
}
