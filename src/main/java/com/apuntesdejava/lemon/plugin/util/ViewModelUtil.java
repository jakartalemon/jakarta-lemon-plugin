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

import com.apuntesdejava.lemon.jakarta.webxml.model.ServletMappingModel;
import com.apuntesdejava.lemon.jakarta.webxml.model.ServletModel;
import com.apuntesdejava.lemon.plugin.util.DocumentXmlUtil.ElementBuilder;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static com.apuntesdejava.lemon.plugin.util.Constants.TAB;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.SPACE;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class ViewModelUtil {

    private static ViewModelUtil INSTANCE;
    private final MavenProject mavenProject;
    private final Log log;
    private final String packageName;
    private final Path webAppPath;
    private final Path resourcePath;
    private Path packageBasePath;

    private ViewModelUtil(Log log, MavenProject mavenProject) {
        this.mavenProject = mavenProject;
        this.log = log;
        this.packageName = StringUtils.replaceChars(mavenProject.getGroupId() + '.' + mavenProject.getArtifactId(), '-', '.');
        var baseDirPath = mavenProject.getBasedir().toPath();
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

    private synchronized static void newInstance(Log log, MavenProject mavenProject) {
        INSTANCE = new ViewModelUtil(log, mavenProject);
    }

    public static ViewModelUtil getInstance(Log log, MavenProject mavenProject) {
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
        if (bodyStruct.containsKey("futureOrPresent") && bodyStruct.getBoolean("futureOrPresent")) {
            lines.add(2, "import jakarta.validation.constraints.FutureOrPresent;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@FutureOrPresent");
        }
        if (bodyStruct.containsKey("future") && bodyStruct.getBoolean("future")) {
            lines.add(2, "import jakarta.validation.constraints.Future;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@Future");
        }
        if (bodyStruct.containsKey("past") && bodyStruct.getBoolean("past")) {
            lines.add(2, "import jakarta.validation.constraints.Past;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@Past");
        }
        if (bodyStruct.containsKey("pastOrPresent") && bodyStruct.getBoolean("pastOrPresent")) {
            lines.add(2, "import jakarta.validation.constraints.PastOrPresent;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@PastOrPresent");
        }
        if (bodyStruct.containsKey("email") && bodyStruct.getBoolean("email")) {
            lines.add(2, "import jakarta.validation.constraints.Email;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@Email");
        }
        if (bodyStruct.containsKey("notNull") && bodyStruct.getBoolean("notNull")) {
            lines.add(2, "import jakarta.validation.constraints.NotNull;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@NotNull");
        }
        if (bodyStruct.containsKey("notEmpty") && bodyStruct.getBoolean("notEmpty")) {
            lines.add(2, "import jakarta.validation.constraints.NotEmpty;");
            lines.add(StringUtils.repeat(SPACE, TAB) + "@NotEmpty");
        }
        if (bodyStruct.containsKey("decimalMax")) {
            lines.add(2, "import jakarta.validation.constraints.DecimalMax;");
            var value = bodyStruct.getInt("decimalMax");
            lines.add(String.format("%s@DecimalMax(value = \"%d\")", StringUtils.repeat(SPACE, TAB), value));
        }
        if (bodyStruct.containsKey("decimalMin")) {
            lines.add(2, "import jakarta.validation.constraints.DecimalMin;");
            var value = bodyStruct.getInt("decimalMin");
            lines.add(String.format("%s@DecimalMin(value = \"%d\")", StringUtils.repeat(SPACE, TAB), value));
        }
        if (bodyStruct.containsKey("digits")) {
            lines.add(2, "import jakarta.validation.constraints.Digits;");
            var digits = bodyStruct.getJsonObject("digits");
            lines.add(String.format("%s@Digits(integer = %d, fraction = %d)", StringUtils.repeat(SPACE, TAB), digits.getInt("integer"), digits.getInt("fraction")));
        }
    }

    public void createServletJsf() throws IOException {
        try {
            log.info("Creating Jakarta Server Faces views");
            var baseDir = mavenProject.getBasedir();
            log.debug("baseDir:" + baseDir);
            var webXmlUtil = new WebXmlUtil(baseDir.toString());
            var webxml = webXmlUtil.getModel();
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

                webXmlUtil.saveModel(webxml);
            }

        } catch (JAXBException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public Optional<JsonObject> getViewModel(String viewProjectFile) throws IOException {
        log.debug("Reading view configuration:" + viewProjectFile);
        try ( InputStream in = new FileInputStream(viewProjectFile)) {
            return Optional.ofNullable(Json.createReader(in).readObject());
        }
    }

    public void createPaths(Set<Map.Entry<String, JsonValue>> entrySet, Set<Map.Entry<String, JsonValue>> formBeans, String primeflexVersion) {
        try {

            final Path packageViewPath = packageBasePath.resolve("view");
            Files.createDirectories(packageViewPath);
            entrySet.forEach(item -> {
                try {
                    var currentEntry = item.getValue().asJsonObject();
                    var managedBeanClassName = createManagedBean(packageViewPath, item);
                    String formBeanName = currentEntry.getString("formBean");
                    var formBean = formBeans.stream().filter(entry -> entry.getKey().equals(formBeanName)).findFirst();
                    formBean.ifPresent(stringJsonValueEntry -> createView(managedBeanClassName, item, formBeanName, stringJsonValueEntry.getValue().asJsonObject(), primeflexVersion));
                } catch (IOException ex) {
                    log.error(ex.getMessage(), ex);
                }
            });
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private static String getNameFromPath(String pathName) {
        return pathName.replaceAll("[^a-zA-Z]", "");
    }

    private String createManagedBean(Path packageViewPath, Map.Entry<String, JsonValue> entry) throws IOException {
        Set<String> imports = new TreeSet<>();

        var pathName = entry.getKey();
        log.info("Creating Managed Bean:" + pathName);
        var pathJson = entry.getValue().asJsonObject();
        var isList = pathJson.getString("type").equals("list");
        String scoped = isList ? "SessionScoped" : "RequestScoped";
        var $temp = getNameFromPath(pathName) + "View";
        var hasListView = entry.getValue().asJsonObject().containsKey("listView");

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
        lines.add(String.format("public class %s %s{", className, (isList ? "implements java.io.Serializable " : EMPTY)));
        if (hasListView) {
            lines.add(EMPTY);
            lines.add(String.format("%s@Inject", StringUtils.repeat(SPACE, TAB)));
            var variableName = getNameFromPath(entry.getValue().asJsonObject().getString("listView"));
            var variableNameView = variableName + "View";
            var classNameListView = name2ClassName(variableNameView);
            var variableListView = name2Variable(variableNameView);
            variablesMap.put("variableName", variableName);
            lines.add(String.format("%sprivate %s %s;", StringUtils.repeat(SPACE, TAB), classNameListView, variableListView));

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

        if (!isList) {
            lines.add(EMPTY);
            lines.add(String.format("%spublic String save() {", StringUtils.repeat(SPACE, TAB)));
            lines.add(String.format("%s%sView.get%sList().add(%s);", StringUtils.repeat(SPACE, TAB * 2), variablesMap.get("variableName"), name2ClassName(variablesMap.get("variableName")), fieldName));
            lines.add(String.format("%sreturn \"%s?faces-redirect=true\";", StringUtils.repeat(SPACE, TAB * 2), entry.getValue().asJsonObject().getString("listView", "/index")));
            lines.add(String.format("%s}", StringUtils.repeat(SPACE, TAB)));
            lines.add(EMPTY);
            lines.add(String.format("%spublic void onload() {", StringUtils.repeat(SPACE, TAB)));
            lines.add(String.format("%s//TODO: Code here what you need", StringUtils.repeat(SPACE, TAB * 2)));
            lines.add(String.format("%s}", StringUtils.repeat(SPACE, TAB)));
        }

        lines.add("}");
        lines.addAll(2, imports);
        Files.write(classPath, lines);
        return className;

    }

    public void createFormBeans(Set<Map.Entry<String, JsonValue>> entrySet) {
        try {
            final Path packageFormBean = packageBasePath.resolve("formbean");
            Files.createDirectories(packageFormBean);
            entrySet.forEach(item -> createFormBean(packageFormBean, item));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void createFormBean(Path packageFormBean, Map.Entry<String, JsonValue> entry) {
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
                        insertLabels(labels, fieldName, bodyStruct);

                        break;
                    case STRING:
                        JsonString jsonString = (JsonString) field.getValue();
                        fieldType = String.valueOf(jsonString.getChars());

                }
                insertImportType(lines, fieldType);
                lines.add(String.format("%sprivate %s %s;", StringUtils.repeat(SPACE, TAB), fieldType, fieldName));
                lines.add(EMPTY);
            });
            lines.add("}");
            Files.write(classPath, lines);

            Path messagePropertiesPath = resourcePath.resolve("messages.properties");

            Set<String> messages = Files.exists(messagePropertiesPath)
                    ? new LinkedHashSet<>(Files.readAllLines(messagePropertiesPath))
                    : new LinkedHashSet<>(Arrays.asList("form.save=Guardar", "form.cancel=Cancelar", "list.new_record=Nuevo registro"));
            Set<String> newMessages = new LinkedHashSet<>();
            newMessages.add("## FORM BEAN " + className);
            labels.entrySet().forEach(label -> newMessages.add(String.format("%s_%s=%s", className, label.getKey(), label.getValue())));
            messages.addAll(newMessages);

            Files.write(messagePropertiesPath, messages);

        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void createView(String managedBeanClassName, Map.Entry<String, JsonValue> entry, String formBeanName, JsonObject formBean, String primeflexVersion) {
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
                    );
            if (!isList) {
                getPrimaryKey(formBean).ifPresent(id -> {
                    htmlElem.addChild(DocumentXmlUtil.ElementBuilder.newInstance("f:metadata")
                            .addChild(
                                    DocumentXmlUtil.ElementBuilder.newInstance("f:viewParam")
                                            .addAttribute("name", "id")
                                            .addAttribute("value", String.format("#{%sView.%s.%s}", pathName, formBeanName, id))
                            )
                            .addChild(
                                    DocumentXmlUtil.ElementBuilder.newInstance("f:viewAction")
                                            .addAttribute("action", String.format("#{%sView.onload}", pathName))
                            )
                    );
                });
            }
            htmlElem.addChild(
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
                hForm.addChild(
                        DocumentXmlUtil.ElementBuilder.newInstance("p:linkButton")
                                .addAttribute("outcome", "/customerForm")
                                .addAttribute("icon", "pi pi-plus-circle")
                                .addAttribute("value", "#{messages['list.new_record']}")
                ).addChild(createList(pathName, formBeanName, formBean, entry.getValue().asJsonObject().getString("editForm")));
            } else {
                hForm.addChild(createForm(pathName, formBeanName, formBean))
                        .addChild(createButtons(entry.getValue().asJsonObject().getString("listView"), formBeanName));

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

    private ElementBuilder createForm(String pathName, String formBeanName, JsonObject formBean) {
        var panel = DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup")
                .addAttribute("layout", "block")
                .addAttribute("id", "formPanel")
                .addAttribute("styleClass", "card");
        var $formBeanName = name2ClassName(formBeanName);
        formBean.forEach((fieldName, type) -> {
            ElementBuilder fieldPanelGroup;
            panel.addChild(
                    fieldPanelGroup = DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup")
                            .addAttribute("layout", "block")
                            .addAttribute("styleClass", "field col-12 md:col-6")
                            .addChild(
                                    DocumentXmlUtil.ElementBuilder.newInstance("p:outputLabel")
                                            .addAttribute("for", fieldName)
                                            .addAttribute("value", String.format("#{messages.%s_%s}", $formBeanName, fieldName))
                            )
            );
            log.debug("----" + fieldName + ":" + type);
            var fieldType = getFileType(type);
            ElementBuilder child = null;
            switch (fieldType) {
                case "String":
                    fieldPanelGroup.addChild(
                            child = DocumentXmlUtil.ElementBuilder.newInstance("p:inputText")
                    );
                    break;
                case "LocalDate":
                    fieldPanelGroup.addChild(
                            child = DocumentXmlUtil.ElementBuilder.newInstance("p:datePicker")
                    );
                    break;
                case "float":
                    fieldPanelGroup.addChild(
                            child = DocumentXmlUtil.ElementBuilder.newInstance("p:inputNumber")
                    );
                    break;
            }
            if (child != null) {
                child.addAttribute("id", fieldName)
                        .addAttribute("styleClass", "w-full")
                        .addAttribute("value", String.format("#{%1$sFormView.%1$s.%2$s}", formBeanName, fieldName));
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
                    DocumentXmlUtil.ElementBuilder.newInstance("p:message")
                            .addAttribute("for", fieldName)
            );

        });
        return panel;
    }

    private static String getFileType(JsonValue type) {
        if (type.getValueType() == JsonValue.ValueType.OBJECT) {
            return type.asJsonObject().getString("type", "String");
        }
        return "String";//String.valueOf(((JsonString) type.getValue()).getChars());
    }

    private ElementBuilder createList(String pathName, String formBeanName, JsonObject formBean, String editForm) {
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
        getPrimaryKey(formBean).ifPresent(field -> {
            pDataTable.addChild(
                    DocumentXmlUtil.ElementBuilder.newInstance("p:column")
                            .addChild(DocumentXmlUtil.ElementBuilder.newInstance("p:linkButton")
                                    .addAttribute("outcome", editForm)
                                    .addAttribute("icon", "pi pi-pencil")
                                    .addChild(
                                            DocumentXmlUtil.ElementBuilder.newInstance("f:param")
                                                    .addAttribute("name", "id")
                                                    .addAttribute("value", String.format("#{item.%s}", field))
                                    )
                            )
            );
        });
        return pDataTable;

    }

    private static Optional<String> getPrimaryKey(JsonObject formBean) {
        return formBean.entrySet().stream().filter(
                entry -> entry.getValue().asJsonObject().containsKey("primary")
                && entry.getValue().asJsonObject().getBoolean("primary")
        ).map(entry -> entry.getKey()).findFirst();
    }

    /**
     * Create lables for properties
     *
     * @param labels Map of messages
     * @param bodyStruct JSON Structure from view.json
     */
    private void insertLabels(Map<String, String> labels, String fieldName, JsonObject bodyStruct) {
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

    public void createViews(JsonObject viewModel, String version) {
        Set<Map.Entry<String, JsonValue>> formBeans = viewModel.getJsonObject("formBeans").entrySet();
        createPaths(
                viewModel.getJsonObject("paths").entrySet(),
                formBeans,
                version
        );
        createFormBeans(formBeans);
    }

    private ElementBuilder createButtons(String listLink, String formBeanName) {
        return DocumentXmlUtil.ElementBuilder.newInstance("h:panelGroup")
                .addAttribute("layout", "block")
                .addAttribute("styleClass", "card")
                .addChild(
                        DocumentXmlUtil.ElementBuilder.newInstance("p:commandButton")
                                .addAttribute("value", "#{messages['form.save']}")
                                .addAttribute("styleClass", "mr-3")
                                .addAttribute("update", "formPanel")
                                .addAttribute("action", String.format("#{%sFormView.save()}", formBeanName))
                )
                .addChild(
                        DocumentXmlUtil.ElementBuilder.newInstance("p:linkButton")
                                .addAttribute("value", "#{messages['form.cancel']}")
                                .addAttribute("styleClass", "ui-button-secondary mr-3")
                                .addAttribute("outcome", listLink)
                );
    }
}
