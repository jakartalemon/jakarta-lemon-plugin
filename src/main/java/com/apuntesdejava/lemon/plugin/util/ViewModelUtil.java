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

import com.apuntesdejava.lemon.plugin.util.DocumentXmlUtil.ElementBuilder;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class ViewModelUtil {

    private static final String KEY_PRIMARY = "primary";
    private static final String KEY_PARAMETERS = "parameters";
    private static final String JAKARTA_VALIDATION_CONSTRAINTS = "jakarta.validation.constraints";
    private static final String IMPORT_JAKARTA_CONSTRAINTS_PACKAGE = "import " + JAKARTA_VALIDATION_CONSTRAINTS + ".%s;";
    private static ViewModelUtil INSTANCE;
    private final MavenProject mavenProject;
    private final Log log;
    private final String packageName;
    private final Path webAppPath;
    private final Path resourcePath;
    private final JsonObject validationJson;
    private Path packageBasePath;

    private ViewModelUtil(Log log,
                          MavenProject mavenProject) {
        this.mavenProject = mavenProject;
        this.log = log;
        this.packageName = StringUtils.replaceChars(mavenProject.getGroupId() + '.' + mavenProject.getArtifactId(), '-',
            '.');
        var baseDirPath = mavenProject.getBasedir().toPath();
        var mainPath = baseDirPath.resolve(SRC_PATH).resolve(MAIN_PATH);
        Path javaMainSrc = mainPath.resolve(JAVA_PATH);
        this.webAppPath = mainPath.resolve(WEBAPP);
        this.resourcePath = mainPath.resolve(RESOURCES);
        String[] packagePaths = packageName.split("\\.");
        this.packageBasePath = javaMainSrc;
        for (String packagePath : packagePaths) {
            packageBasePath = packageBasePath.resolve(packagePath);

        }

        try (var validationsReader = Json.createReader(ViewModelUtil.class.getResourceAsStream("/validations.json"))) {
            this.validationJson = validationsReader.readObject();
        }

    }

    private synchronized static void newInstance(Log log,
                                                 MavenProject mavenProject) {
        INSTANCE = new ViewModelUtil(log, mavenProject);
    }

    /**
     * Gets an instance of ViewModelUtil to be able to manipulate
     *
     * @param log          The maven log
     * @param mavenProject Maven Project
     * @return ViewModelUtil Instance
     */

    public static ViewModelUtil getInstance(Log log,
                                            MavenProject mavenProject) {
        if (INSTANCE == null) {
            newInstance(log, mavenProject);
        }
        return INSTANCE;
    }

    private static String name2ClassName(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private static String name2Variable(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }

    private static void insertImportType(Set<String> lines,
                                         String fieldType) {
        switch (fieldType) {
            case "LocalDate":
                lines.add("import java.time.LocalDate;");
                break;
            case "LocalDateTime":
                lines.add("import java.time.LocalDateTime;");
                break;
            case "Date":
                lines.add("import java.util.Date;");
        }
    }

    private static String getNameFromPath(String pathName) {
        return pathName.replaceAll("[^a-zA-Z]", "");
    }

    private static String getFileType(JsonValue type) {
        if (type.getValueType() == JsonValue.ValueType.OBJECT) {
            return type.asJsonObject().getString("type", "String");
        }
        return "String";//String.valueOf(((JsonString) type.getValue()).getChars());
    }

    private static Optional<String> getPrimaryKey(JsonObject formBean) {
        return formBean.entrySet().stream()
            .filter(entry -> entry.getValue().asJsonObject().containsKey(KEY_PRIMARY) && entry.getValue()
                .asJsonObject().getBoolean(KEY_PRIMARY)).map(Map.Entry::getKey).findFirst();
    }

    private void insertValidation(Set<String> imports,
                                  List<String> lines,
                                  JsonObject bodyStruct) {
        validationJson.entrySet().stream().filter(entry -> bodyStruct.containsKey(entry.getKey())).forEach(($entry) -> {
            var validationName = $entry.getKey();
            var validationBodyJson = $entry.getValue().asJsonObject();
            var className = validationBodyJson.getString("class");
            log.debug(String.format("---validation:%s", className));

            imports.add(String.format(IMPORT_JAKARTA_CONSTRAINTS_PACKAGE, className));
            var parametersBuilder = Json.createObjectBuilder().add("message", "\"%s\"");
            if (validationBodyJson.containsKey(KEY_PARAMETERS)) {
                validationBodyJson.getJsonObject(KEY_PARAMETERS).forEach(parametersBuilder::add);
            }
            var parameters = parametersBuilder.build();

            JsonObject arguments = null;
            if (bodyStruct.containsKey(validationName)) {
                var value = bodyStruct.get(validationName);
                if (value.getValueType() == JsonValue.ValueType.OBJECT) {
                    arguments = value.asJsonObject();
                } else if (value.getValueType() == JsonValue.ValueType.TRUE) {
                    arguments = JsonValue.EMPTY_JSON_OBJECT;
                }

            }
            String classDeclaring;
            Object[] declaringArgumentsClass;
            if (arguments == null) {
                classDeclaring = String.format("%s@%s", StringUtils.repeat(SPACE, TAB), className);
                declaringArgumentsClass = new Object[0];
            } else {

                var params = parameters.keySet().stream().sorted().filter(arguments::containsKey).map(jsonValue -> {
                    var entryValue = parameters.getString(jsonValue);
                    return String.format("%s = %s", jsonValue, entryValue);
                }).collect(Collectors.joining(", "));
                classDeclaring = String.format("%s@%s(%s)", StringUtils.repeat(SPACE, TAB), className, params);
                JsonObject finalArguments = arguments;
                declaringArgumentsClass = arguments.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .map(jsonValueEntry -> {
                        var valueType = jsonValueEntry.getValue().getValueType();
                        log.debug("- valueType:" + valueType);
                        if (null == valueType) {
                            return finalArguments.getString(jsonValueEntry.getKey());
                        } else {
                            switch (valueType) {
                                case NUMBER:
                                    return finalArguments.getInt(jsonValueEntry.getKey());
                                case TRUE:
                                    return "true";
                                case FALSE:
                                    return "false";
                                default:
                                    return finalArguments.getString(jsonValueEntry.getKey());
                            }
                        }
                    }).toArray();
            }
            var line = String.format(classDeclaring, declaringArgumentsClass);
            lines.add(line);
        });

    }

    /**
     * Create the Servlet for Jakarta Faces
     *
     * @throws IOException If IO Exception
     */

    public void createServletJsf() throws IOException {
        log.info("Creating Jakarta Server Faces views");
        var baseDir = mavenProject.getBasedir();
        log.debug("baseDir:" + baseDir);
        Optional.of(WebXmlUtil.openWebXml(baseDir)).ifPresent(webXmlDocument -> {
            try {
                boolean saveXml = false;
                if (DocumentXmlUtil.listElementsByFilter(webXmlDocument, String.format(
                    "/web-app/servlet/servlet-class[text()='%s']", FACES_SERVLET)).isEmpty()) {
                    DocumentXmlUtil.createElement(webXmlDocument, "/web-app", SERVLET).ifPresent(servletElem -> {
                        DocumentXmlUtil.createElement(webXmlDocument, servletElem, SERVLET_NAME,
                            FACES_SERVLET_NAME);
                        DocumentXmlUtil.createElement(webXmlDocument, servletElem, SERVLET_CLASS, FACES_SERVLET);
                    });
                    saveXml = true;

                }
                if (DocumentXmlUtil.listElementsByFilter(webXmlDocument, String.format(
                    "/web-app/servlet-mapping/servlet-name[text()='%s']", FACES_SERVLET_NAME)).isEmpty()) {
                    DocumentXmlUtil.createElement(webXmlDocument, "/web-app", "servlet-mapping")
                        .ifPresent(servletElem -> {
                            DocumentXmlUtil.createElement(webXmlDocument, servletElem, SERVLET_NAME,
                                FACES_SERVLET_NAME);
                            DocumentXmlUtil.createElement(webXmlDocument, servletElem, URL_PATTERN, "*.jsf");
                        });
                    saveXml = true;
                }
                if (saveXml) {
                    WebXmlUtil.saveWebXml(baseDir, webXmlDocument);
                }
            } catch (XPathExpressionException e) {
                log.error(e.getMessage(), e);
            }
        });

    }

    /**
     * Opens the views settings for the project.
     *
     * @param viewProjectFile The name of the views configuration file.
     * @return JSON object with the configuration of the views
     * @throws IOException if IO exception
     */

    public Optional<JsonObject> getViewModel(String viewProjectFile) throws IOException {
        log.debug("Reading view configuration:" + viewProjectFile);
        try (InputStream in = new FileInputStream(viewProjectFile)) {
            return Optional.ofNullable(Json.createReader(in).readObject());
        }
    }

    private void createPaths(Set<Map.Entry<String, JsonValue>> pathEntries,
                             Set<Map.Entry<String, JsonValue>> formBeans,
                             String primeflexVersion) {
        try {

            final Path packageViewPath = packageBasePath.resolve("view");
            Files.createDirectories(packageViewPath);
            pathEntries.forEach(pathEntry -> {
                try {
                    var currentEntry = pathEntry.getValue().asJsonObject();
                    createManagedBean(packageViewPath, pathEntry);
                    String formBeanName = currentEntry.getString("formBean");
                    var formBean = formBeans.stream().filter(entry -> entry.getKey().equals(formBeanName)).findFirst();
                    formBean.ifPresent(stringJsonValueEntry -> createView(pathEntry, formBeanName,
                        stringJsonValueEntry.getValue()
                            .asJsonObject(), primeflexVersion));
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            });
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void createManagedBean(Path packageViewPath,
                                   Map.Entry<String, JsonValue> pathEntry) throws IOException {
        Set<String> imports = new TreeSet<>();

        var pathName = pathEntry.getKey();
        log.info("Creating Managed Bean:" + pathName);
        var pathJson = pathEntry.getValue().asJsonObject();
        var isList = pathJson.getString("type").equals("list");
        String scoped = isList ? "SessionScoped" : "RequestScoped";
        var $temp = getNameFromPath(pathName) + "View";
        var hasListView = pathEntry.getValue().asJsonObject().containsKey("listView");

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
        Map<String, String> variablesMap = new LinkedHashMap<>();
        if (hasListView) {
            imports.add("import jakarta.inject.Inject;");
        }
        imports.add("import jakarta.inject.Named;");
        lines.add(EMPTY);
        lines.add("@Named");
        lines.add("@" + scoped);
        lines.add(
            String.format("public class %s %s{", className, (isList ? "implements java.io.Serializable " : EMPTY)));
        if (hasListView) {
            lines.add(EMPTY);
            lines.add(String.format("%s@Inject", StringUtils.repeat(SPACE, TAB)));
            var variableName = getNameFromPath(pathEntry.getValue().asJsonObject().getString("listView"));
            var variableNameView = variableName + "View";
            var classNameListView = name2ClassName(variableNameView);
            var variableListView = name2Variable(variableNameView);
            variablesMap.put("variableName", variableName);
            lines.add(String.format("%sprivate %s %s;", StringUtils.repeat(SPACE, TAB), classNameListView,
                variableListView));

        }
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
        lines.add(String.format("%sprivate %s %s = new %s();", StringUtils.repeat(SPACE, TAB), fieldType, fieldName,
            newInstance));

        //methods setter & getter
        var setterName = "set" + name2ClassName(fieldName);
        var getterName = "get" + name2ClassName(fieldName);
        lines.add(EMPTY);
        lines.add(String.format("%spublic void %s(%s %s) {", StringUtils.repeat(SPACE, TAB), setterName, fieldType,
            fieldName));
        lines.add(String.format("%sthis.%2$s = %2$s;", StringUtils.repeat(SPACE, TAB * 2), fieldName));
        lines.add(String.format("%s}", StringUtils.repeat(SPACE, TAB)));

        lines.add(EMPTY);
        lines.add(String.format("%spublic %s %s() {", StringUtils.repeat(SPACE, TAB), fieldType, getterName));
        lines.add(String.format("%sreturn this.%s;", StringUtils.repeat(SPACE, TAB * 2), fieldName));
        lines.add(String.format("%s}", StringUtils.repeat(SPACE, TAB)));

        if (!isList) {
            lines.add(EMPTY);
            lines.add(String.format("%spublic String save() {", StringUtils.repeat(SPACE, TAB)));
            lines.add(String.format("%s%sView.get%sList().add(%s);", StringUtils.repeat(SPACE, TAB * 2),
                variablesMap.get("variableName"), name2ClassName(variablesMap.get("variableName")),
                fieldName));
            lines.add(String.format("%sreturn \"%s?faces-redirect=true\";", StringUtils.repeat(SPACE, TAB * 2),
                pathEntry.getValue().asJsonObject().getString("listView", "/index")));
            lines.add(String.format("%s}", StringUtils.repeat(SPACE, TAB)));
            lines.add(EMPTY);
            lines.add(String.format("%spublic void onload() {", StringUtils.repeat(SPACE, TAB)));
            lines.add(String.format("%s//TODO: Code here what you need", StringUtils.repeat(SPACE, TAB * 2)));
            lines.add(String.format("%s}", StringUtils.repeat(SPACE, TAB)));
        }

        lines.add("}");
        lines.addAll(2, imports);
        Files.write(classPath, lines);

    }

    private void createFormBeans(Set<Map.Entry<String, JsonValue>> entrySet) {
        try {
            final Path packageFormBean = packageBasePath.resolve("formbean");
            Files.createDirectories(packageFormBean);
            entrySet.forEach(item -> createFormBean(packageFormBean, item));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void createFormBean(Path packageFormBean,
                                Map.Entry<String, JsonValue> entry) {
        try {
            var pathName = entry.getKey();
            log.info("Creating Form bean:" + pathName);
            var bodyBean = entry.getValue().asJsonObject();

            String className = name2ClassName(pathName);
            String classFileName = className + ".java";
            Path classPath = packageFormBean.resolve(classFileName);
            Map<String, String> labels = new LinkedHashMap<>();

            List<String> lines = new ArrayList<>();
            lines.add("package " + packageName + ".formbean;");
            lines.add(EMPTY);
            lines.add("import lombok.Data;");
            lines.add(EMPTY);
            lines.add("@Data");
            lines.add("public class " + className + " {");
            lines.add(EMPTY);
            Set<String> imports = new TreeSet<>();
            bodyBean.forEach((fieldName, value) -> {
                log.debug(String.format("--field:%s", fieldName));
                var fieldType = "String";
                switch (value.getValueType()) {
                    case OBJECT:
                        var bodyStruct = value.asJsonObject();
                        fieldType = bodyStruct.getString("type", "String");

                        insertValidation(imports, lines, bodyStruct);
                        insertLabels(labels, fieldName, bodyStruct);

                        break;
                    case STRING:
                        JsonString jsonString = (JsonString) value;
                        fieldType = String.valueOf(jsonString.getChars());

                }
                insertImportType(imports, fieldType);
                lines.add(String.format("%sprivate %s %s;", StringUtils.repeat(SPACE, TAB), fieldType, fieldName));
                lines.add(EMPTY);
            });
            lines.addAll(2, imports);
            lines.add("}");
            Files.write(classPath, lines);

            Path messagePropertiesPath = resourcePath.resolve("messages.properties");

            Set<String> messages = Files.exists(messagePropertiesPath) ? new LinkedHashSet<>(
                Files.readAllLines(messagePropertiesPath)) : new LinkedHashSet<>(
                Arrays.asList("form.save=Guardar", "form.cancel=Cancelar", "list.new_record=Nuevo registro"));
            Set<String> newMessages = new LinkedHashSet<>();
            newMessages.add("## FORM BEAN " + className);
            labels.forEach((key, value) -> newMessages.add(String.format("%s_%s=%s", className, key, value)));
            messages.addAll(newMessages);

            Files.write(messagePropertiesPath, messages);

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void createView(Map.Entry<String, JsonValue> entry,
                            String formBeanName,
                            JsonObject formBean,
                            String primeflexVersion) {
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
                .addAttribute(XMLNS, "http://www.w3.org/1999/xhtml")
                .addAttribute("xmlns:h", "http://xmlns.jcp.org/jsf/html")
                .addAttribute("xmlns:ui", "http://xmlns.jcp.org/jsf/facelets")
                .addAttribute("xmlns:p", "http://primefaces.org/ui")
                .addAttribute("xmlns:f", "http://xmlns.jcp.org/jsf/core").addChild(
                    DocumentXmlUtil.ElementBuilder.newInstance("h:head").addChild(
                        DocumentXmlUtil.ElementBuilder.newInstance("h:outputStylesheet")
                            .addAttribute("library", "webjars").addAttribute("name", String.format(
                                "primeflex/%s/primeflex.min.css", primeflexVersion))).addChild(
                        DocumentXmlUtil.ElementBuilder.newInstance("f:loadBundle")
                            .addAttribute("var", "messages").addAttribute("basename", "messages")));
            if (!isList) {
                getPrimaryKey(formBean).ifPresent(id -> htmlElem.addChild(ElementBuilder.newInstance("f:metadata")
                    .addChild(ElementBuilder.newInstance(
                        "f:viewParam").addAttribute(
                        "name", ID).addAttribute(
                        VALUE, String.format(
                            "#{%sView.%s.%s}",
                            pathName,
                            formBeanName, id)))
                    .addChild(ElementBuilder.newInstance(
                        "f:viewAction").addAttribute(
                        "action", String.format(
                            "#{%sView.onload}",
                            pathName)))));
            }
            htmlElem.addChild(DocumentXmlUtil.ElementBuilder.newInstance("h:body").addChild(
                DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup").addAttribute("styleClass", "card")
                    .addAttribute("layout", "block").addChild(
                        DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup")
                            .addAttribute("styleClass", "card-container").addAttribute("layout", "block")
                            .addChild(DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup")
                                .addAttribute("styleClass", "block p-4 mb-3")
                                .addAttribute("layout", "block").addChild(
                                    hForm = DocumentXmlUtil.ElementBuilder.newInstance("h:form"))))));

            if (isList) {
                hForm.addChild(DocumentXmlUtil.ElementBuilder.newInstance("p:linkButton")
                    .addAttribute("outcome", "/customerForm")
                    .addAttribute("icon", "pi pi-plus-circle")
                    .addAttribute(VALUE, "#{messages['list.new_record']}")).addChild(
                    createList(pathName, formBeanName, formBean,
                        entry.getValue().asJsonObject().getString("editForm")));
            } else {
                hForm.addChild(createForm(formBeanName, formBean))
                    .addChild(createButtons(entry.getValue().asJsonObject().getString("listView"), formBeanName));

            }

            doc.appendChild(htmlElem.build(doc));
            DocumentXmlUtil.saveDocument(viewJsf, doc);

        } catch (ParserConfigurationException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private ElementBuilder createForm(String formBeanName,
                                      JsonObject formBean) {
        var panel = DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup").addAttribute("layout", "block")
            .addAttribute(ID, "formPanel").addAttribute("styleClass", "card");
        var $formBeanName = name2ClassName(formBeanName);
        formBean.forEach((fieldName, type) -> {
            ElementBuilder fieldPanelGroup;
            panel.addChild(fieldPanelGroup = DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup")
                .addAttribute("layout", "block").addAttribute("styleClass", "field col-12 md:col-6").addChild(
                    DocumentXmlUtil.ElementBuilder.newInstance("p:outputLabel").addAttribute("for", fieldName)
                        .addAttribute(VALUE,
                            String.format("#{messages.%s_%s}", $formBeanName, fieldName))));
            log.debug("----" + fieldName + ":" + type);
            var fieldType = getFileType(type);
            ElementBuilder child = null;
            switch (fieldType) {
                case "String":
                    fieldPanelGroup.addChild(child = DocumentXmlUtil.ElementBuilder.newInstance("p:inputText"));
                    break;
                case "LocalDate":
                    fieldPanelGroup.addChild(child = DocumentXmlUtil.ElementBuilder.newInstance("p:datePicker"));
                    break;
                case "float":
                    fieldPanelGroup.addChild(child = DocumentXmlUtil.ElementBuilder.newInstance("p:inputNumber"));
                    break;
            }
            if (child != null) {
                child.addAttribute(ID, fieldName).addAttribute("styleClass", "w-full")
                    .addAttribute(VALUE, String.format("#{%1$sFormView.%1$s.%2$s}", formBeanName, fieldName));
                if (type.getValueType() == JsonValue.ValueType.OBJECT) {
                    var typeObject = type.asJsonObject();
                    if (typeObject.containsKey("size")) {
                        var size = typeObject.getJsonObject("size");
                        if (size.containsKey("max")) {
                            var max = size.getInt("max");
                            child.addAttribute("maxlength", String.valueOf(max));
                        }
                    }
                }
            }
            fieldPanelGroup.addChild(
                DocumentXmlUtil.ElementBuilder.newInstance("p:message").addAttribute("for", fieldName));

        });
        return panel;
    }

    private ElementBuilder createList(String variableName,
                                      String formBeanName,
                                      JsonObject formBean,
                                      String editForm) {
        var pDataTable = DocumentXmlUtil.ElementBuilder.newInstance("p:dataTable")
            .addAttribute(VALUE, String.format("#{%1$sView.%1$sList}", variableName)).addAttribute("var", "item");
        var $formBeanName = name2ClassName(formBeanName);
        formBean.forEach((fieldName, type) -> pDataTable.addChild(ElementBuilder.newInstance("p:column")
            .addAttribute("headerText", String.format(
                "#{messages.%s_%s}", $formBeanName,
                fieldName)).addChild(
                ElementBuilder.newInstance("h:outputText")
                    .addAttribute(VALUE, String.format("#{item.%s}", fieldName)))));
        getPrimaryKey(formBean).ifPresent(field -> pDataTable.addChild(ElementBuilder.newInstance("p:column").addChild(
            ElementBuilder.newInstance("p:linkButton").addAttribute("outcome", editForm)
                .addAttribute("icon", "pi pi-pencil").addChild(
                    ElementBuilder.newInstance("f:param").addAttribute("name", ID)
                        .addAttribute(VALUE, String.format("#{item.%s}", field))))));
        return pDataTable;

    }

    /**
     * Create lables for properties
     *
     * @param labels     Map of messages
     * @param bodyStruct JSON Structure from view.json
     */
    private void insertLabels(Map<String, String> labels,
                              String fieldName,
                              JsonObject bodyStruct) {
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

    /**
     * Create the views for the project, based on the configuration file
     *
     * @param viewModel        Configuration of the views, taken from the configuration file.
     * @param primeflexVersion PrimeFlex version
     */
    public void createViews(JsonObject viewModel,
                            String primeflexVersion) {
        Set<Map.Entry<String, JsonValue>> formBeans = viewModel.getJsonObject("formBeans").entrySet();
        createPaths(viewModel.getJsonObject("paths").entrySet(), formBeans, primeflexVersion);
        createFormBeans(formBeans);
    }

    private ElementBuilder createButtons(String listLink,
                                         String formBeanName) {
        return DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup").addAttribute("layout", "block")
            .addAttribute("styleClass", "card").addChild(
                DocumentXmlUtil.ElementBuilder.newInstance("p:commandButton")
                    .addAttribute(VALUE, "#{messages['form.save']}").addAttribute("styleClass", "mr-3")
                    .addAttribute("update", "formPanel")
                    .addAttribute("action", String.format("#{%sFormView.save()}", formBeanName))).addChild(
                DocumentXmlUtil.ElementBuilder.newInstance("p:linkButton")
                    .addAttribute(VALUE, "#{messages['form.cancel']}")
                    .addAttribute("styleClass", "ui-button-secondary mr-3")
                    .addAttribute("outcome", listLink));
    }
}
