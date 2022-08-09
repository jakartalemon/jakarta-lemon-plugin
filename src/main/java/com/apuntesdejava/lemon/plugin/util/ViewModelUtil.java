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
import com.apuntesdejava.lemon.plugin.util.DocumentXmlUtil.ElementBuilder;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
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
    private final Path baseDirPath;
    private final Path webAppPath;
    private final Path resourcePath;

    private ViewModelUtil(MavenProject mavenProject) {
        this.packageName = StringUtils.replaceChars(mavenProject.getGroupId() + '.' + mavenProject.getArtifactId(), '-', '.');
        this.baseDirPath = mavenProject.getBasedir().toPath();
        Path javaMainSrc = baseDirPath.resolve("src").resolve("main").resolve("java");
        this.webAppPath = baseDirPath.resolve("src").resolve("main").resolve("webapp");
        this.resourcePath = baseDirPath.resolve("src").resolve("main").resolve("resources");
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

    public void createPaths(Log log, Set<Map.Entry<String, JsonValue>> entrySet, Set<Map.Entry<String, JsonValue>> formBeans, String primeflexVersion) {
        try {

            final Path packageViewPath = packageBasePath.resolve("view");
            Files.createDirectories(packageViewPath);
            entrySet.stream().forEach(item -> {
                try {
                    var currentEntry = item.getValue().asJsonObject();
                    var managedBeanClassName = createManagedBean(log, packageViewPath, item);
                    String formBeanName = currentEntry.getString("formBean");
                    var formBean = formBeans.stream().filter(entry -> entry.getKey().equals(formBeanName)).findFirst();
                    if (formBean.isPresent()) {
                        createView(log, managedBeanClassName, item, formBeanName, formBean.get().getValue().asJsonObject(), primeflexVersion);
                    }
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            });
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private String createManagedBean(Log log, Path packageViewPath, Map.Entry<String, JsonValue> entry) throws IOException {
        Set<String> imports = new TreeSet<>();

        var pathName = entry.getKey();
        log.info("Creating Managed Bean:" + pathName);
        var pathJson = entry.getValue().asJsonObject();
        var isList = pathJson.getString("type").equals("list");
        String scoped = isList ? "SessionScoped" : "RequestScoped";
        var $temp = pathName.replaceAll("[^a-zA-Z]", "") + "View";

        String className = name2ClassName($temp);
        String classFileName = className + ".java";
        Path classPath = packageViewPath.resolve(classFileName);

        List<String> lines = new ArrayList<>();
        var fieldName = pathJson.getString("formBean");
        var fieldClassType = name2ClassName(fieldName);
        lines.add("package " + packageName + ".view;");
        lines.add(EMPTY);
        imports.add("import jakarta.enterprise.context." + scoped + ";");
        imports.add(String.format("import %s.formbean.%s;", packageName, fieldClassType));
        imports.add("import jakarta.inject.Named;");
        lines.add(EMPTY);
        lines.add("@Named");
        lines.add("@" + scoped);
        lines.add(String.format("public class %s %s{", className, (isList ? "implements java.io.Serializable " : EMPTY)));
        lines.add(EMPTY);
        var fieldType = fieldClassType;
        var newInstance = fieldClassType;

        if (isList) {
            imports.add("import java.util.List;");
            imports.add("import java.util.ArrayList;");
            fieldType = "List<" + fieldClassType + ">";
            fieldName += "sList";
            newInstance = "ArrayList<>";
        }
        lines.add(String.format("%sprivate %s %s = new %s();", StringUtils.repeat(SPACE, TAB), fieldType, fieldName, newInstance));

        //methods setter & getter
        var setterName = "set" + name2ClassName(fieldName);
        var getterName = "get" + name2ClassName(fieldName);
        lines.add(EMPTY);
        lines.add(String.format("%spublic void %s(%s %s) {", StringUtils.repeat(SPACE, TAB), setterName, fieldType, fieldName));
        lines.add(String.format("%sthis.%2$s = %2$s;", StringUtils.repeat(SPACE, TAB * 2), fieldName));
        lines.add(String.format("%s}", StringUtils.repeat(SPACE, TAB)));

        lines.add(EMPTY);
        lines.add(String.format("%spublic %s %s() {", StringUtils.repeat(SPACE, TAB), fieldType, getterName));
        lines.add(String.format("%sreturn this.%s;", StringUtils.repeat(SPACE, TAB * 2), fieldName));
        lines.add(String.format("%s}", StringUtils.repeat(SPACE, TAB)));

        lines.add("}");
        lines.addAll(2, imports);
        Files.write(classPath, lines);
        return className;

    }

    public void createFormBeans(Log log, Set<Map.Entry<String, JsonValue>> entrySet) {
        try {
            final Path packageFormBean = packageBasePath.resolve("formbean");
            Files.createDirectories(packageFormBean);
            entrySet.stream().forEach(item -> createFormBean(log, packageFormBean, item));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static String name2ClassName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private void createFormBean(Log log, Path packageFormBean, Map.Entry<String, JsonValue> entry) {
        try {
            var pathName = entry.getKey();
            log.info("Creating Form bean:" + pathName);
            var bodyBean = entry.getValue().asJsonObject();

            String className = name2ClassName(pathName);
            String classFileName = className + ".java";
            Path classPath = packageFormBean.resolve(classFileName);
            Map<String, String> labels = new LinkedHashMap<>();

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
                        insertLabels(log, labels, fieldName, bodyStruct);

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

            Path messagePropertiesPath = resourcePath.resolve("messages.properties");
            List<String> messages = Files.exists(messagePropertiesPath)
                    ? Files.readAllLines(messagePropertiesPath)
                    : new ArrayList<>();
            messages.add(EMPTY);
            messages.add("## FORM BEAN " + className);
            labels.entrySet().forEach(label -> messages.add(String.format("%s_%s=%s", className, label.getKey(), label.getValue())));

            Files.write(messagePropertiesPath, messages);

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

    private void createView(Log log, String managedBeanClassName, Map.Entry<String, JsonValue> entry, String formBeanName, JsonObject formBean, String primeflexVersion) {
        var pathJson = entry.getValue().asJsonObject();
        var isList = pathJson.getString("type").equals("list");
        try {
            var pathName = entry.getKey().replaceAll("[^a-zA-Z]", "");
            log.info("Creating View page:" + pathName);
            var viewJsf = webAppPath.resolve(pathName + ".xhtml");

            var docFactory = DocumentBuilderFactory.newInstance();
            var docBuilder = docFactory.newDocumentBuilder();

            var doc = docBuilder.newDocument();

            ElementBuilder hForm;

            var htmlElem = DocumentXmlUtil.ElementBuilder.newInstance("html")
                    .addAttribute("xmlns", "http://www.w3.org/1999/xhtml")
                    .addAttribute("xmlns:h", "http://xmlns.jcp.org/jsf/html")
                    .addAttribute("xmlns:ui", "http://xmlns.jcp.org/jsf/facelets")
                    .addAttribute("xmlns:p", "http://primefaces.org/ui")
                    .addAttribute("xmlns:f", "http://xmlns.jcp.org/jsf/core")
                    .addChild(DocumentXmlUtil.ElementBuilder.newInstance("h:head")
                            .addChild(DocumentXmlUtil.ElementBuilder.newInstance("h:outputStylesheet")
                                    .addAttribute("library", "webjars")
                                    .addAttribute("name", String.format("primeflex/%s/primeflex.min.css", primeflexVersion))
                            ).addChild(
                                    DocumentXmlUtil.ElementBuilder.newInstance("f:loadBundle")
                                            .addAttribute("var", "messages")
                                            .addAttribute("basename", "messages")
                            )
                    )
                    .addChild(
                            DocumentXmlUtil.ElementBuilder.newInstance("h:body")
                                    .addChild(
                                            DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup")
                                                    .addAttribute("styleClass", "card")
                                                    .addAttribute("layout", "block")
                                                    .addChild(
                                                            DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup")
                                                                    .addAttribute("styleClass", "card-container")
                                                                    .addAttribute("layout", "block")
                                                                    .addChild(
                                                                            DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup")
                                                                                    .addAttribute("styleClass", "block p-4 mb-3")
                                                                                    .addAttribute("layout", "block")
                                                                                    .addChild(
                                                                                            hForm = DocumentXmlUtil.ElementBuilder.newInstance("h:form")
                                                                                    )
                                                                    )
                                                    )
                                    )
                    );

            if (isList) {
                hForm.addChild(createList(pathName, formBeanName, formBean));
            }

            doc.appendChild(htmlElem.build(doc));
            try ( var fos = new FileOutputStream(viewJsf.toFile())) {

                var tf = TransformerFactory.newInstance();
                var t = tf.newTransformer();
                t.setOutputProperty(OutputKeys.INDENT, "yes");
                t.setOutputProperty(OutputKeys.STANDALONE, "yes");
                var source = new DOMSource(doc);
                var result = new StreamResult(fos);
                t.transform(source, result);
            } catch (TransformerException ex) {
                log.error(ex.getMessage(), ex);
            }
        } catch (ParserConfigurationException | IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private ElementBuilder createList(String pathName, String formBeanName, JsonObject formBean) {
        var variableName = pathName;
        var pDataTable = DocumentXmlUtil.ElementBuilder.newInstance("p:dataTable")
                .addAttribute("value", String.format("#{%1$sView.%1$sList}", variableName))
                .addAttribute("var", "item");
        var $formBeanName = name2ClassName(formBeanName);
        formBean.forEach((fieldName, type) -> {
            pDataTable.addChild(
                    DocumentXmlUtil.ElementBuilder.newInstance("p:column")
                            .addAttribute("headerText", String.format("#{messages.%s_%s}", $formBeanName, fieldName))
                            .addChild(
                                    DocumentXmlUtil.ElementBuilder.newInstance("h:outputText")
                                            .addAttribute("value", String.format("#{item.%s}", fieldName))
                            )
            );
        });
        return pDataTable;

    }

    /**
     * Create lables for properties
     *
     * @param labels Map of messages
     * @param bodyStruct JSON Structure from view.json
     */
    private void insertLabels(Log log, Map<String, String> labels, String fieldName, JsonObject bodyStruct) {
        if (bodyStruct.containsKey("label")) {
            var value = bodyStruct.get("label");
            if (value.getValueType() == JsonValue.ValueType.STRING) {
                labels.put(fieldName, bodyStruct.getString("label"));
            } else if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                var sValue = value.asJsonObject();
                try {
                    labels.put(fieldName, sValue.getString("en"));
                } catch (Exception ex) {
                    log.warn("field " + fieldName + ex.getMessage());
                    try {
                        labels.put(fieldName, sValue.getString("default"));
                    } catch (Exception ex0) {
                        log.warn("field " + fieldName + ex.getMessage());

                    }
                }
            }
        }
    }

}
