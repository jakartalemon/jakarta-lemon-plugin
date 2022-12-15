package com.apuntesdejava.lemon.plugin;

import com.apuntesdejava.lemon.jakarta.model.types.DatasourceDefinitionStyleType;
import com.apuntesdejava.lemon.plugin.util.*;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.persistence.GenerationType;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;
import static com.apuntesdejava.lemon.plugin.util.JsonValuesUtil.*;

@Mojo(name = "create-model")
public class CreateModelMojo extends AbstractMojo {

    private static void removeLastComma(List<String> list) {
        list.set(list.size() - 1, StringUtils.removeEnd(list.get(list.size() - 1), ","));
    }

    @Parameter(property = "model", defaultValue = "model.json")
    private String modelProjectFile;
    private JsonObject projectModel;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    private DatasourceDefinitionStyleType style;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        ProjectModelUtil.getProjectModel(getLog(), modelProjectFile).ifPresent(pm -> {

            String groupId = mavenProject.getGroupId();
            String packageName = StringUtils.replaceChars(groupId + '.' + mavenProject.getArtifactId(), '-', '.');
            this.projectModel = Json.createObjectBuilder(pm).add(PACKAGE_NAME, Json.createValue(packageName))
                    .add(PROJECT_NAME, Json.createValue(mavenProject.getId())).build();
            getLog().debug("groupId:" + groupId);
            getLog().debug("packageName:" + packageName);
            buildModel(packageName);
            addDatasource();
            addDependencies();
            addPersistenceXML();
        });
    }

    private void buildModel(String packageName) {
        try {
            getLog().debug("Building model");
            Path baseDirPath = mavenProject.getBasedir().toPath();
            getLog().debug("baseDir:" + baseDirPath);

            Path javaMainSrc = baseDirPath.resolve("src").resolve("main").resolve("java");
            Path resourcesMainSrc = baseDirPath.resolve("src").resolve("main").resolve("resources");
            Path javaTestSrc = baseDirPath.resolve("src").resolve("test").resolve("java");
            Path packageBasePath = javaMainSrc;
            String[] packagePaths = packageName.split("\\.");
            for (String packagePath : packagePaths) {
                packageBasePath = packageBasePath.resolve(packagePath);
            }
            Files.createDirectories(javaMainSrc);
            Files.createDirectories(resourcesMainSrc);
            Files.createDirectories(javaTestSrc);
            Files.createDirectories(packageBasePath);
            createAbstractsClasses(packageBasePath);

            final Path packageBaseModel = packageBasePath.resolve("model");
            final Path packageBaseRepository = packageBasePath.resolve("repositories");
            final Path packageBaseService = packageBasePath.resolve("services");
            Files.createDirectories(packageBaseModel);

            projectModel.getJsonArray(ENTITIES).stream().map(JsonValue::asJsonObject).forEach(entity -> {
                createEntity(packageBaseModel.resolve(entity.getString(NAME) + ".java"), entity);
                createRepository(packageBaseRepository, entity);
                createService(packageBaseService, entity);
            });

        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }

    }

    private void createRepository(Path packageBaseRepository,
                                  JsonObject entity) {
        try {
            var entityName = entity.getString(NAME);
            var packageName = projectModel.getString(PACKAGE_NAME);
            getLog().debug("Creando repositorio de " + entityName);
            String className = entityName + "Repository";
            Path target = packageBaseRepository.resolve(className + ".java");
            List<String> lines = new ArrayList<>();
            lines.add("package " + packageName + ".repositories;\n");
            lines.add("import " + packageName + ".model." + entityName + ";");
            lines.add("import jakarta.enterprise.context.ApplicationScoped;");
            lines.add("import jakarta.inject.Inject;");
            lines.add("import jakarta.persistence.EntityManager;\n");
            lines.add("@ApplicationScoped");
            Optional<Map.Entry<String, JsonValue>> pk = entity.getJsonObject(FIELDS).entrySet().stream()
                    .filter(item -> item.getValue().asJsonObject().getBoolean(PK)).findFirst();
            AtomicReference<String> idClass = new AtomicReference<>("Object");
            pk.ifPresent(pkValue -> idClass.set(pkValue.getValue().asJsonObject().getString(TYPE)));
            lines.add(String.format("public class %s extends AbstractRepository<%s, %s> {\n", className, idClass.get(),
                                    entityName));

            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "@Inject");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "private EntityManager em;\n");

            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "public " + className + "() {");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + "super(" + entityName + ".class);");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "}\n");

            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "@Override");
            lines.add(StringUtils.repeat(StringUtils.SPACE,
                                         Constants.TAB) + "protected EntityManager getEntityManager() {");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + "return em;");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "}\n");

            if (!isFieldsEmpty(entity, FINDERS)) {
                getLog().debug("creando métodos de búsqueda");
                entity.getJsonObject(FINDERS).forEach((name, aValue) -> {
                    var value = aValue.asJsonObject();
                    String params = "()";
                    if (!isFieldsEmpty(value, PARAMETERS)) {
                        StringBuilder param = new StringBuilder();
                        param.append("(\n");
                        var parameters = value.getJsonObject(PARAMETERS);
                        parameters.keySet().forEach(paramName -> {
                            var type = parameters.getString(paramName);
                            param.append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2));
                            param.append(type).append(' ');
                            param.append(paramName).append(',');
                        });
                        param.setLength(param.length() - 1);
                        param.append("\n").append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)).append(")");
                        params = param.toString();
                    }
                    lines.add(String.format("%spublic %s findBy%s%s {",
                                            StringUtils.repeat(StringUtils.SPACE, Constants.TAB),
                                            value.getString(RETURN_VALUE_TYPE), name, params));
                    lines.add(
                            StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + "return em." + (value.getBoolean(
                                    NATIVE_QUERY,
                                    false) ? "createNativeQuery" : "createNamedQuery") + "(\"" + entityName + ".findBy" + name + "\"," + entityName + ".class)");
                    if (!isFieldsEmpty(value, PARAMETERS)) {
                        value.getJsonObject(PARAMETERS).keySet().forEach(paramName -> lines.add(
                                StringUtils.repeat(StringUtils.SPACE,
                                                   Constants.TAB * 3) + ".setParameter(\"" + paramName + "\"," + paramName + ")"));
                    }
                    if (value.getBoolean(UNIQUE, false)) {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 3) + ".getSingleResult();");
                    } else {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 3) + ".getResultList();");
                    }
                    lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "}\n");
                });
            }

            lines.add("}");
            Files.write(target, lines);
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createService(Path packageBaseService,
                               JsonObject entity) {
        try {
            var entityName = entity.getString(NAME);
            var packageName = projectModel.getString(PACKAGE_NAME);
            getLog().debug("Creando servicio de " + entityName);
            String className = entityName + "Service";
            Path target = packageBaseService.resolve(className + ".java");
            StringBuilder lines = new StringBuilder();
            lines.append("package ").append(packageName).append(".services;\n\n");
            StringBuilder repositoryClass = new StringBuilder().append(entityName).append("Repository");
            lines.append("import ").append(packageName).append(".repositories.").append(repositoryClass).append(";\n");
            lines.append("import ").append(packageName).append(".model.").append(entityName).append(';').append('\n');
            lines.append("import jakarta.enterprise.context.ApplicationScoped;\n");
            lines.append("import jakarta.inject.Inject;\n\n");
            lines.append("@ApplicationScoped\n");
            var fields = entity.getJsonObject(FIELDS);
            var idClass = fields.values().stream().filter(jsonValue -> jsonValue.asJsonObject().getBoolean(PK))
                    .map(jsonValue -> jsonValue.asJsonObject().getString(TYPE)).findFirst().orElse("Object");
            lines.append("public class ").append(className).append(" extends AbstractService<").append(idClass)
                    .append(',').append(entityName).append(',').append(repositoryClass).append('>').append('{')
                    .append('\n');

            lines.append('\n').append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)).append("@Inject\n")
                    .append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)).append("private ")
                    .append(repositoryClass).append(" repository;\n");

            lines.append("\n\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)).append("@Override\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)).append("public ").append(repositoryClass)
                    .append(" getRepository(){\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2)).append("return repository;\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)).append("}\n");

            lines.append('}');
            Files.writeString(target, lines.toString());
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createAbstractsClasses(Path packageBasePath) {
        var packageName = projectModel.getString(PACKAGE_NAME);
        var projectName = projectModel.getString(PROJECT_NAME);
        createFile(packageBasePath.resolve("repositories").resolve("JpaProvider.java"),
                   "/classes/JpaProvider.javatemplate",
                   Map.of("{package}", packageName, "{unitNamePU}", projectName + "PU"));
        createFile(packageBasePath.resolve("services").resolve("AbstractService.java"),
                   "/classes/AbstractService.javatemplate", Map.of("{package}", packageName));
        createFile(packageBasePath.resolve("repositories").resolve("AbstractRepository.java"),
                   "/classes/AbstractRepository.javatemplate", Map.of("{package}", packageName));
    }

    private void createFile(Path target,
                            String source,
                            Map<String, String> maps) {
        try {
            getLog().debug("==createFile:\n\tsource=" + source + "\n\ttarget:" + target);
            Files.createDirectories(target.getParent());
            try (InputStream is = getClass().getResourceAsStream(source)) {
                if (is != null) {
                    List<String> code = IOUtils.readLines(is, Charset.defaultCharset());
                    List<String> newCode = code.stream()
                            .map(line -> StringUtils.replaceEach(line, maps.keySet().toArray(String[]::new),
                                                                 maps.values().toArray(String[]::new)))
                            .collect(Collectors.toList());
                    Files.write(target, newCode);
                }
            }
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createEntity(Path target,
                              JsonObject entity) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("package " + projectModel.getString(PACKAGE_NAME) + "." + "model" + ";\n");
            lines.add("@lombok.Data");
            if (!isStringEmpty(entity, TABLE_NAME)) {
                lines.add(String.format("@jakarta.persistence.Table(name = \"%s\" )", entity.getString(TABLE_NAME)));
            }
            lines.add("@jakarta.persistence.Entity");
            if (entity.containsKey(FINDERS)) {
                entity.getJsonObject(FINDERS).entrySet().stream()
                        .filter(entry -> entry.getValue().asJsonObject().containsKey(NATIVE_QUERY) && entry.getValue()
                                .asJsonObject().getBoolean(NATIVE_QUERY)).forEach(entry -> {
                            var value = entry.getValue().asJsonObject();
                            lines.add("@jakarta.persistence.NamedNativeQuery(");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + String.format(
                                    "name = \"%s.findBy%s\",", entity.getString(NAME), entry.getKey()));
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "query = \"" + value.getString(
                                    QUERY) + ",\n");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "resultClass = " + value.getString(
                                    RETURN_VALUE_TYPE));
                            lines.add(")");
                        });
                entity.getJsonObject(FINDERS).entrySet().stream()
                        .filter(entry -> !entry.getValue().asJsonObject().containsKey(NATIVE_QUERY) || !entry.getValue()
                                .asJsonObject().getBoolean(NATIVE_QUERY)).forEach(entry -> {
                            var value = entry.getValue().asJsonObject();
                            lines.add("@jakarta.persistence.NamedQuery(");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "name = \"" + entity.getString(
                                    NAME) + ".findBy" + entry.getKey() + "\",");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "query = \"" + value.getString(
                                    QUERY) + "\"");
                            lines.add(")");
                        });
            }
            lines.add("public class " + entity.getString(NAME) + "{\n");
            if (entity.containsKey(FIELDS)) {
                entity.getJsonObject(FIELDS).forEach((key, item) -> {
                    var value = item.asJsonObject();
                    if (value.containsKey(PK) && value.getBoolean(PK)) {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "@jakarta.persistence.Id");
                    }
                    var isJoinPresent = !isStringEmpty(value, JOIN);
                    if (isJoinPresent) {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + String.format(
                                "@jakarta.persistence.%s", value.getString(JOIN)));
                    }
                    if (value.containsKey(COLUMN_NAME)) {
                        if (!isJoinPresent) {
                            lines.add(StringUtils.repeat(StringUtils.SPACE,
                                                         Constants.TAB) + "@jakarta.persistence.Column(");
                            List<String> attrsList = new ArrayList<>();
                            attrsList.add(StringUtils.repeat(StringUtils.SPACE,
                                                             Constants.TAB * 2) + "name = \"" + value.getString(
                                    COLUMN_NAME) + "\",");
                            if (!isNumberEmpty(value, LENGTH)) {
                                attrsList.add(StringUtils.repeat(StringUtils.SPACE,
                                                                 Constants.TAB * 2) + "length = " + value.getJsonNumber(
                                        LENGTH) + ",");
                            }
                            removeLastComma(attrsList);
                            lines.addAll(attrsList);
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + ")");
                        } else {
                            lines.add(StringUtils.repeat(StringUtils.SPACE,
                                                         Constants.TAB) + "@jakarta.persistence.JoinColumn(");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + String.format(
                                    "name = \"%s\"", value.getString(COLUMN_NAME)));
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + ")");

                        }
                    }
                    if (!isStringEmpty(value, GENERATED_VALUE)) {

                        GenerationType generatedValueType = ObjectUtils.defaultIfNull(
                                EnumUtils.getEnum(GenerationType.class,
                                                  StringUtils.upperCase(value.getString(GENERATED_VALUE))),
                                GenerationType.AUTO);
                        lines.add(StringUtils.repeat(StringUtils.SPACE,
                                                     Constants.TAB) + "@jakarta.persistence.GeneratedValue(");
                        lines.add(StringUtils.repeat(StringUtils.SPACE,
                                                     Constants.TAB * 2) + "strategy = jakarta.persistence.GenerationType." + generatedValueType.name());
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + ")");
                    }
                    lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "private " + value.getString(
                            TYPE) + " " + key + ";\n");
                });
            }
            lines.add("}");

            Files.write(target, lines);
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void addDependencies() {
        if (this.style == DatasourceDefinitionStyleType.WEB) { //se agrega dependencia solo si está incorporado dentro del .war
            addDBDependencies();
        }
        addProjectLombokDependency();
    }

    private void addDBDependencies() {
        try {
            getLog().debug("Add DB Dependencies");
            String database = projectModel.getJsonObject(DATASOURCE).getString(DB);

            Model model = ProjectModelUtil.getModel(mavenProject);
            ProjectModelUtil.addDependenciesDatabase(getLog(), model, database);

            ProjectModelUtil.saveModel(mavenProject, model);
        } catch (IOException | XmlPullParserException ex) {
            getLog().error(ex.getMessage(), ex);
        }

    }

    private void addDatasource() {
        getLog().debug("Creating datasource");
        if (projectModel.containsKey(DATASOURCE)) {
            var datasource = projectModel.getJsonObject(DATASOURCE);
            try {
                String driverDataSource = ProjectModelUtil.getDriver(getLog(), datasource.getString(DB));
                getLog().debug("Driver: " + driverDataSource);
                String styleSrc = datasource.getString(STYLE);
                this.style = DatasourceDefinitionStyleType.findByValue(styleSrc);
                if (style != null) {
                    switch (style) {
                        case PAYARA_RESOURCES:
                            PayaraUtil.createPayaraDataSourceResources(getLog(), projectModel, mavenProject);
                            break;
                        case WEB:
                            createWebXML();
                            break;
                        case OPENLIBERTY:
                            OpenLibertyUtil.createDataSource(getLog(), projectModel, mavenProject);
                            break;
                        default:
                            getLog().error("DataSource Style is invalid:" + styleSrc);
                    }
                }
            } catch (IOException | InterruptedException | URISyntaxException ex) {
                getLog().error(ex.getMessage(), ex);
            }
        }
    }

    private void addPersistenceXML() {
        try {
            getLog().debug("Create persistence.xml");
            var baseDir = mavenProject.getBasedir();
            getLog().debug("baseDir:" + baseDir);
            var persistenceUtil = new PersistenceXmlUtil(baseDir.toString());
            var persistenceXml = persistenceUtil.getModel();
            persistenceXml.getPersistenceUnit().setName(projectModel.getString(PROJECT_NAME) + "PU");
            String dataSourceName = (style == DatasourceDefinitionStyleType.WEB ? "java:app/" : "") + "jdbc/" + mavenProject.getArtifactId();
            persistenceXml.getPersistenceUnit().setJtaDataSource(dataSourceName);
            persistenceUtil.saveModel(persistenceXml);

        } catch (IOException | JAXBException ex) {
            getLog().error(ex.getMessage(), ex);
        }

    }

    private void createWebXML() {
        try {

            var webXmlDocument = WebXmlUtil.openWebXml(mavenProject.getBasedir());

            String dataSourceName = "java:app/jdbc/" + mavenProject.getArtifactId();

            boolean createDataSource = DocumentXmlUtil.findElementsByFilter(webXmlDocument, "/web-app/data-source")
                    .isEmpty();
            if (createDataSource) {
                var datasource = projectModel.getJsonObject(DATASOURCE);
                String driverDataSource = ProjectModelUtil.getDriver(getLog(), datasource.getString(DB));

                DocumentXmlUtil.createElement(webXmlDocument, "/web-app", "data-source")
                        .ifPresent(datasourceElement -> {
                            DocumentXmlUtil.createElement(webXmlDocument, datasourceElement, "name", dataSourceName);
                            DocumentXmlUtil.createElement(webXmlDocument, datasourceElement, "class-name",
                                                          driverDataSource);
                            DocumentXmlUtil.createElement(webXmlDocument, datasourceElement, URL,
                                                          datasource.getString(URL));
                            DocumentXmlUtil.createElement(webXmlDocument, datasourceElement, USER,
                                                          datasource.getString(USER));
                            DocumentXmlUtil.createElement(webXmlDocument, datasourceElement, PASSWORD,
                                                          datasource.getString(PASSWORD));
                            if (!isFieldsEmpty(datasource, PROPERTIES)) {
                                var properties = datasource.getJsonObject(PROPERTIES);
                                properties.keySet().forEach(
                                        key -> DocumentXmlUtil.createElement(webXmlDocument, datasourceElement,
                                                                             "property").ifPresent(property -> {
                                            DocumentXmlUtil.createElement(webXmlDocument, property, NAME, key);
                                            DocumentXmlUtil.createElement(webXmlDocument, property, "value",
                                                                          properties.getString(key));
                                        }));

                            }
                        });
                WebXmlUtil.saveWebXml(mavenProject.getBasedir(), webXmlDocument);

            }
        } catch (IOException | InterruptedException | URISyntaxException | XPathExpressionException ex) {
            getLog().error(ex.getMessage(), ex);
        }

    }

    private void addProjectLombokDependency() {
        try {
            getLog().debug("Add Project Lombok Dependencies");
            Model model = ProjectModelUtil.getModel(mavenProject);
            ProjectModelUtil.addDependency(getLog(), model, "org.projectlombok", "lombok");

            ProjectModelUtil.saveModel(mavenProject, model);
        } catch (IOException | XmlPullParserException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

}
