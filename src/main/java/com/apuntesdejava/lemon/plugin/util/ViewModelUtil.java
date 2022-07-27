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

import static com.apuntesdejava.lemon.plugin.util.Constants.TAB;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class ViewModelUtil {

    private static ViewModelUtil INSTANCE;

    private synchronized static void newInstance(MavenProject mavenProject) {
        INSTANCE = new ViewModelUtil(mavenProject);
    }
    private final String packageName;
    private Path packageBasePath;

    private ViewModelUtil(MavenProject mavenProject) {
        this.packageName = StringUtils.replaceChars(mavenProject.getGroupId() + '.' + mavenProject.getArtifactId(), '-', '.');
        Path baseDirPath = mavenProject.getBasedir().toPath();
        Path javaMainSrc = baseDirPath.resolve("src").resolve("main").resolve("java");
        String groupId = packageName;
        String[] packagePaths = groupId.split("\\.");
        this.packageBasePath = javaMainSrc;
        for (String packagePath : packagePaths) {
            packageBasePath = packageBasePath.resolve(packagePath);

        }

    }

    public static ViewModelUtil getInstance(MavenProject mavenProject) {
        if (INSTANCE == null) {
            newInstance(mavenProject);
        }
        return INSTANCE;
    }

    public Optional<JsonObject> getViewModel(Log log, String viewProjectFile) throws FileNotFoundException, IOException {
        log.debug("Reading view configuration:" + viewProjectFile);
        try ( InputStream in = new FileInputStream(viewProjectFile)) {
            return Optional.ofNullable(Json.createReader(in).readObject());
        }
    }

    public void createPaths(Log log, Set<Map.Entry<String, JsonValue>> entrySet) {
        try {

            final Path packageViewPath = packageBasePath.resolve("view");
            Files.createDirectories(packageViewPath);
            entrySet.stream().forEach(item -> createPath(log, packageViewPath, item));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void createPath(Log log, Path packageViewPath, Map.Entry<String, JsonValue> entry) {

        try {
            var pathName = entry.getKey();
            log.info("Creating view:" + pathName);
            var pathJson = entry.getValue().asJsonObject();
            var isList = pathJson.getString("type").equals("list");
            String scoped = isList ? "SessionScoped" : "RequestScoped";
            var $temp = pathName.replaceAll("[^a-zA-Z]", "") + "View";

            String className = name2ClassName($temp);
            String classFileName = className + ".java";
            Path classPath = packageViewPath.resolve(classFileName);

            var lines = new ArrayList<String>();
            var fieldName = pathJson.getString("formBean");
            var fieldClassType = name2ClassName(fieldName);
            lines.add("package " + packageName + ".view;");
            lines.add(EMPTY);
            lines.add("import jakarta.enterprise.context." + scoped + ";");
            lines.add(String.format("import %s.formbean.%s;", packageName, fieldClassType));
            lines.add("import jakarta.inject.Named;");
            lines.add(EMPTY);
            lines.add("@Named");
            lines.add("@" + scoped);
            lines.add(String.format("public class %s %s{", className, (isList ? "implements java.io.Serializable " : EMPTY)));
            lines.add(EMPTY);
            var fieldType = fieldClassType;

            if (isList) {
                lines.add(2, "import java.util.List;");
                lines.add(2, "import java.util.ArrayList;");
                fieldType = "List<" + fieldClassType + ">";
                fieldName += "sList";
            }
            lines.add(String.format("%sprivate %s %s;", StringUtils.repeat(SPACE, TAB), fieldType, fieldName));

            lines.add("}");
            Files.write(classPath, lines);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }

    }

    public void createFormBeans(Log log, Set<Map.Entry<String, JsonValue>> entrySet) {
        try {
            final Path packageFormBean = packageBasePath.resolve("formbean");
            Files.createDirectories(packageFormBean);
            entrySet.stream().forEach(item -> createFormbean(log, packageFormBean, item));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static String name2ClassName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private void createFormbean(Log log, Path packageFormBean, Map.Entry<String, JsonValue> entry) {
        try {
            var pathName = entry.getKey();
            log.info("Creating Form bean:" + pathName);
            var bodyBean = entry.getValue().asJsonObject();

            String className = name2ClassName(pathName);
            String classFileName = className + ".java";
            Path classPath = packageFormBean.resolve(classFileName);

            var lines = new ArrayList<String>();
            lines.add("package " + packageName + ".formbean;");
            lines.add(EMPTY);
            lines.add("import lombok.Data;");
            lines.add(EMPTY);
            lines.add("@Data");
            lines.add("public class " + className + " {");
            lines.add(EMPTY);
            bodyBean.entrySet().forEach(field -> {
                var fieldName = field.getKey();
                var fieldType = "String";
                switch (field.getValue().getValueType()) {
                    case OBJECT:
                        var bodyStruct = field.getValue().asJsonObject();
                        fieldType = bodyStruct.getString("type", "String");
                        insertValidation(lines, bodyStruct);

                        break;
                    case STRING:
                        JsonString jsonString = (JsonString) field.getValue();
                        fieldType = new StringBuilder(jsonString.getChars()).toString();

                }
                insertImportType(lines, fieldType);
                lines.add(String.format("%sprivate %s %s;", StringUtils.repeat(SPACE, TAB), fieldType, fieldName));
                lines.add(EMPTY);
            });
            lines.add("}");
            Files.write(classPath, lines);
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static void insertImportType(ArrayList<String> lines, String fieldType) {
        switch (fieldType) {
            case "LocalDate":
                lines.add(2, "import java.time.LocalDate;");
                break;
            case "LocalDateTime":
                lines.add(2, "import java.time.LocalDateTime;");
                break;
            case "Date":
                lines.add(2, "import java.util.Date;");
        }
    }

    private static void insertValidation(ArrayList<String> lines, JsonObject bodyStruct) {
        if (bodyStruct.keySet().contains("futureOrPresent") && bodyStruct.getBoolean("futureOrPresent")) {
            lines.add(2, "import jakarta.validation.constraints.FutureOrPresent;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@FutureOrPresent");
        }
        if (bodyStruct.keySet().contains("future") && bodyStruct.getBoolean("future")) {
            lines.add(2, "import jakarta.validation.constraints.Future;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@Future");
        }
        if (bodyStruct.keySet().contains("past") && bodyStruct.getBoolean("past")) {
            lines.add(2, "import jakarta.validation.constraints.Past;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@Past");
        }
        if (bodyStruct.keySet().contains("pastOrPresent") && bodyStruct.getBoolean("pastOrPresent")) {
            lines.add(2, "import jakarta.validation.constraints.PastOrPresent;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@PastOrPresent");
        }
        if (bodyStruct.keySet().contains("email") && bodyStruct.getBoolean("email")) {
            lines.add(2, "import jakarta.validation.constraints.Email;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@Email");
        }
        if (bodyStruct.keySet().contains("notNull") && bodyStruct.getBoolean("notNull")) {
            lines.add(2, "import jakarta.validation.constraints.NotNull;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@NotNull");
        }
        if (bodyStruct.keySet().contains("notEmpty") && bodyStruct.getBoolean("notEmpty")) {
            lines.add(2, "import jakarta.validation.constraints.NotEmpty;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@NotEmpty");
        }
        if (bodyStruct.keySet().contains("decimalMax")) {
            lines.add(2, "import jakarta.validation.constraints.DecimalMax;");
            var value = bodyStruct.getInt("decimalMax");
            lines.add(String.format("%s@DecimalMax(value = \"%d\")", StringUtils.repeat(SPACE, TAB), value));
        }
        if (bodyStruct.keySet().contains("decimalMin")) {
            lines.add(2, "import jakarta.validation.constraints.DecimalMin;");
            var value = bodyStruct.getInt("decimalMin");
            lines.add(String.format("%s@DecimalMin(value = \"%d\")", StringUtils.repeat(SPACE, TAB), value));
        }
        if (bodyStruct.keySet().contains("digits")) {
            lines.add(2, "import jakarta.validation.constraints.Digits;");
            var digits = bodyStruct.getJsonObject("digits");
            lines.add(String.format("%s@Digits(integer = %d, fraction = %d)", StringUtils.repeat(SPACE, TAB), digits.getInt("integer"), digits.getInt("fraction")));
        }
    }

}
