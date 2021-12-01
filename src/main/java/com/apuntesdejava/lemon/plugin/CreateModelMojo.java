package com.apuntesdejava.lemon.plugin;

import com.apuntesdejava.lemon.jakarta.model.EntityModel;
import com.apuntesdejava.lemon.jakarta.model.FieldModel;
import com.apuntesdejava.lemon.jakarta.model.ProjectModel;
import com.apuntesdejava.lemon.jakarta.model.types.DatasourceDefinitionStyleType;
import com.apuntesdejava.lemon.jakarta.model.types.GenerationType;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Mojo(name = "create-model")
public class CreateModelMojo extends AbstractMojo {

    private static final int TAB = 4;

    private static void removeLastComma(List<String> list) {
        list.set(list.size() - 1, StringUtils.removeEnd(list.get(list.size() - 1), ","));
    }

    @Parameter(
            property = "model",
            defaultValue = "model.json"
    )
    private String _modelProjectFile;
    private ProjectModel projectModel;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;
    private final Map<String, Object> dbDefinitions
            = Map.of("mysql",
                    Map.of(
                            "driver", "com.mysql.cj.jdbc.Driver",
                            "datasource", "com.mysql.cj.jdbc.MysqlDataSource",
                            "pool", "com.mysql.cj.jdbc.MysqlConnectionPoolDataSource",
                            "dependency", Map.of(
                                    "version", "8.0.27",
                                    "groupId", "mysql",
                                    "artifactId", "mysql-connector-java"
                            )
                    ),
                    "postgresql",
                    Map.of(
                            "driver", "org.postgresql.Driver",
                            "datasource", "org.postgresql.jdbc3.Jdbc3ConnectionPool",
                            "dependency", Map.of(
                                    "version", "42.3.1",
                                    "groupId", "org.postgresql",
                                    "artifactId", "postgresql"
                            )
                    ),
                    "mariadb",
                    Map.of(
                            "driver", "org.mariadb.jdbc.Driver",
                            "datasource", "org.mariadb.jdbc.MariaDbDataSource",
                            "pool", "org.mariadb.jdbc.MariaDbPoolDataSource",
                            "dependency", Map.of(
                                    "version", "2.7.4",
                                    "groupId", "org.mariadb.jdbc",
                                    "artifactId", "mariadb-java-client"
                            )
                    )
            );

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Jsonb jsonb = JsonbBuilder.create();
        getLog().debug("Reading model configuration:" + _modelProjectFile);
        try ( InputStream in = new FileInputStream(_modelProjectFile)) {
            this.projectModel = jsonb.fromJson(in, ProjectModel.class);
            buildModel();
            addDependencies();
            addDatasource();
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }

    }

    private void buildModel() {
        try {
            getLog().debug("Building model");
            String groupId = mavenProject.getGroupId();
            projectModel.setPackageName(groupId);
            projectModel.setProjectName(mavenProject.getId());
            Path baseDirPath = mavenProject.getBasedir().toPath();
            getLog().debug("groupId:" + groupId);
            getLog().debug("baseDir:" + baseDirPath);

            Path javaMainSrc = baseDirPath.resolve("src").resolve("main").resolve("java");
            Path resourcesMainSrc = baseDirPath.resolve("src").resolve("main").resolve("resources");
            Path javaTestSrc = baseDirPath.resolve("src").resolve("test").resolve("java");
            Path packageBasePath = javaMainSrc;
            String[] packagePaths = groupId.split("\\.");
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
            projectModel.getEntities().forEach(entity -> {
                createEntity(packageBaseModel.resolve(entity.getName() + ".java"), "model", entity);
                createRepository(packageBaseRepository, entity);
                createService(packageBaseService, entity);
            });
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }

    }

    private void createRepository(Path packageBaseRepository, EntityModel entity) {
        try {
            getLog().debug("Creando repositorio de " + entity.getName());
            String className = entity.getName() + "Repository";
            Path target = packageBaseRepository.resolve(className + ".java");
            List<String> lines = new ArrayList<>();
            lines.add("package " + projectModel.getPackageName() + ".repositories;\n");
            lines.add("import " + projectModel.getPackageName() + ".model." + entity.getName() + ";");
            lines.add("import javax.enterprise.context.ApplicationScoped;");
            lines.add("import javax.inject.Inject;");
            lines.add("import javax.persistence.EntityManager;\n");
            lines.add("@ApplicationScoped");
            Optional<Map.Entry<String, FieldModel>> pk = entity.getFields().entrySet().stream().filter(item -> item.getValue().isPk()).findFirst();
            String idClass = "Object";
            if (pk.isPresent()) {
                idClass = pk.get().getValue().getType();
            }
            lines.add("public class " + className + " extends AbstractRepository<" + idClass + ", " + entity.getName() + "> {\n");

            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "@Inject");
            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "private EntityManager em;\n");

            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "public " + className + "() {");
            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB * 2) + "super(" + entity.getName() + ".class);");
            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "}\n");

            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "@Override");
            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "protected EntityManager getEntityManager() {");
            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB * 2) + "return em;");
            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "}\n");

            if (entity.getFinders() != null && !entity.getFinders().isEmpty()) {
                getLog().debug("creando métodos de búsqueda");
                entity.getFinders().forEach((name, value) -> {
                    String params = "()";
                    if (value.getParameters() != null) {
                        StringBuilder param = new StringBuilder();
                        param.append("(\n");
                        value.getParameters().forEach((paramName, type) -> {
                            param.append(StringUtils.repeat(StringUtils.SPACE, TAB * 2));
                            param.append(type).append(' ');
                            param.append(paramName).append(',');
                        });
                        param.setLength(param.length() - 1);
                        param.append("\n").append(StringUtils.repeat(StringUtils.SPACE, TAB)).append(")");
                        params = param.toString();
                    }
                    lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "public " + value.getReturnValueType() + " findBy" + name + params + " {");
                    lines.add(StringUtils.repeat(StringUtils.SPACE, TAB * 2) + "return em." + (value.isNativeQuery() ? "createNativeQuery" : "createNamedQuery")
                            + "(\"" + entity.getName() + ".findBy" + name + "\"," + entity.getName() + ".class)");
                    if (value.getParameters() != null) {
                        value.getParameters().forEach((paramName, param) -> {
                            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB * 3) + ".setParameter(\"" + paramName + "\"," + paramName + ")");
                        });
                    }
                    if (value.isUnique()) {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, TAB * 3) + ".getSingleResult();");
                    } else {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, TAB * 3) + ".getResultList();");
                    }
                    lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "}\n");
                });
            }

            lines.add("}");
            Files.write(target, lines);
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createService(Path packageBaseService, EntityModel entity) {
        try {
            getLog().debug("Creando servicio de " + entity.getName());
            String className = entity.getName() + "Service";
            Path target = packageBaseService.resolve(className + ".java");
            StringBuilder lines = new StringBuilder();
            lines.append("package ").append(projectModel.getPackageName()).append(".services;\n\n");
            StringBuilder repositoryClass = new StringBuilder().append(entity.getName()).append("Repository");
            lines.append("import ").append(projectModel.getPackageName()).append(".repositories.").append(repositoryClass).append(";\n");
            lines.append("import ").append(projectModel.getPackageName()).append(".model.").append(entity.getName()).append(';').append('\n');
            lines.append("import javax.enterprise.context.ApplicationScoped;\n");
            lines.append("import javax.inject.Inject;\n\n");
            lines.append("@ApplicationScoped\n");
            Optional<Map.Entry<String, FieldModel>> pk = entity.getFields().entrySet().stream().filter(item -> item.getValue().isPk()).findFirst();
            String idClass = pk.isPresent() ? pk.get().getValue().getType() : "Object";
            lines.append("public class ").append(className).append(" extends AbstractService<").append(idClass).append(',').append(entity.getName()).append(',').append(repositoryClass).append('>').append('{').append('\n');

            lines.append('\n').append(StringUtils.repeat(StringUtils.SPACE, TAB)).append("@Inject\n")
                    .append(StringUtils.repeat(StringUtils.SPACE, TAB)).append("private ").append(repositoryClass).append(" repository;\n");

            lines.append("\n\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, TAB)).append("@Override\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, TAB)).append("public ").append(repositoryClass).append(" getRepository(){\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, TAB * 2)).append("return repository;\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, TAB)).append("}\n");

            lines.append('}');
            Files.writeString(target, lines.toString());
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createAbstractsClasses(Path packageBasePath) {
        createFile(
                packageBasePath.resolve("repositories").resolve("JpaProvider.java"),
                "/classes/JpaProvider.javatemplate",
                Map.of(
                        "{package}", projectModel.getPackageName(),
                        "{unitNamePU}", projectModel.getProjectName() + "PU"
                )
        );
        createFile(
                packageBasePath.resolve("services").resolve("AbstractService.java"),
                "/classes/AbstractService.javatemplate",
                Map.of(
                        "{package}", projectModel.getPackageName()
                )
        );
        createFile(
                packageBasePath.resolve("repositories").resolve("AbstractRepository.java"),
                "/classes/AbstractRepository.javatemplate",
                Map.of(
                        "{package}", projectModel.getPackageName()
                )
        );
    }

    private void createFile(Path target, String source, Map<String, String> maps) {
        try {
            getLog().debug("==createFile:\n\tsource=" + source + "\n\ttarget:" + target);
            Files.createDirectories(target.getParent());
            try ( InputStream is = getClass().getResourceAsStream(source)) {
                List<String> code = IOUtils.readLines(is, Charset.defaultCharset());
                List<String> newCode = code
                        .stream()
                        .map(
                                line -> StringUtils.replaceEach(line,
                                        maps.keySet().toArray(new String[0]),
                                        maps.values().toArray(new String[0])
                                )
                        )
                        .collect(Collectors.toList());
                Files.write(target, newCode);
            }
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void createEntity(Path target, String subPackageName, EntityModel entity) {
        try {
            List<String> lines = new ArrayList<>();
            lines.add("package " + projectModel.getPackageName() + "." + subPackageName + ";\n");
            lines.add("@lombok.Data");
            lines.add("@javax.persistence.Entity");
            if (entity.getFinders() != null) {
                entity.getFinders().entrySet().stream().filter(entry -> entry.getValue().isNativeQuery())
                        .forEach(entry -> {
                            lines.add("@javax.persistence.NamedNativeQuery(");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "name = \"" + entity.getName() + ".findBy" + entry.getKey() + "\",");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "query = \"" + entry.getValue().getQuery() + ",\n");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "resultClass = " + entry.getValue().getReturnValueType());
                            lines.add(")");
                        });
                entity.getFinders().entrySet().stream().filter(entry -> !entry.getValue().isNativeQuery())
                        .forEach(entry -> {
                            lines.add("@javax.persistence.NamedQuery(");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "name = \"" + entity.getName() + ".findBy" + entry.getKey() + "\",");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "query = \"" + entry.getValue().getQuery() + "\"");
                            lines.add(")");
                        });
            }
            lines.add("public class " + entity.getName() + "{\n");
            if (entity.getFields() != null) {
                entity.getFields().forEach((key, value) -> {
                    if (value.isPk()) {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "@javax.persistence.Id");
                    }
                    if (StringUtils.isNotBlank(value.getColumnName())) {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "@javax.persistence.Column(");
                        List<String> attrsList = new ArrayList<>();
                        attrsList.add(StringUtils.repeat(StringUtils.SPACE, TAB * 2) + "name = \"" + value.getColumnName() + "\",");
                        if (value.getLength() != null && value.getLength() > 0) {
                            attrsList.add(StringUtils.repeat(StringUtils.SPACE, TAB * 2) + "length = " + value.getLength() + ",");
                        }
                        removeLastComma(attrsList);
                        lines.addAll(attrsList);
                        lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + ")");
                    }
                    if (StringUtils.isNotBlank(value.getGeneratedValue())) {

                        GenerationType generatedValueType
                                = ObjectUtils.defaultIfNull(
                                        EnumUtils.getEnum(GenerationType.class, value.getGeneratedValue().toUpperCase()),
                                        GenerationType.AUTO
                                );
                        lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "@javax.persistence.GeneratedValue(");
                        lines.add(StringUtils.repeat(StringUtils.SPACE, TAB * 2) + "strategy = javax.persistence.GenerationType." + generatedValueType.name());
                        lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + ")");
                    }
                    lines.add(StringUtils.repeat(StringUtils.SPACE, TAB) + "private " + value.getType() + " " + key + ";\n");
                });
            }
            lines.add("}");

            Files.write(target, lines);
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void addDependencies() {
        addDBDependencies();
    }

    private void addDBDependencies() {
        getLog().debug("Add DB Dependencies");
        String dbType = projectModel.getDatasource().getDb();
        Map<String, Object> definitions = (Map<String, Object>) dbDefinitions.get(dbType);
        if (definitions != null) {
            Map<String, String> dependencyMap = (Map<String, String>) definitions.get("dependency");
            String version = dependencyMap.get("version");
            String groupId = dependencyMap.get("groupId");
            String artifactId = dependencyMap.get("artifactId");
            boolean found = mavenProject.getDependencies()
                    .stream()
                    .filter(item
                            -> StringUtils.equals(item.getGroupId(), groupId)
                    && StringUtils.equals(item.getArtifactId(), artifactId)
                    ).count() > 0;
            if (!found) {
                try {
                    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                    dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
                    DocumentBuilder db = dbf.newDocumentBuilder();
                    Document doc = db.parse(mavenProject.getFile());
                    doc.getDocumentElement().normalize();
                    XPath xPath = XPathFactory.newInstance().newXPath();

                    NodeList dependenciesNodeList = (NodeList) xPath.compile("/project/dependencies").evaluate(doc, XPathConstants.NODESET);
                    Element dependenciesElem = (Element) dependenciesNodeList.item(0);
                    Element dependecyDriver = doc.createElement("dependency");

                    Element groupIdElem = doc.createElement("groupId");
                    groupIdElem.setTextContent(groupId);
                    Element artifactIdElem = doc.createElement("artifactId");
                    artifactIdElem.setTextContent(artifactId);
                    Element versionElem = doc.createElement("version");
                    versionElem.setTextContent(version);

                    dependecyDriver.appendChild(groupIdElem);
                    dependecyDriver.appendChild(artifactIdElem);
                    dependecyDriver.appendChild(versionElem);

                    dependenciesElem.appendChild(dependecyDriver);

                    try ( FileOutputStream output
                            = new FileOutputStream(mavenProject.getFile())) {
                        writeXml(doc, output);
                    } catch (TransformerException ex) {
                        getLog().error(ex.getMessage(), ex);
                    }

                } catch (ParserConfigurationException | SAXException | XPathExpressionException | IOException ex) {
                    getLog().error(ex.getMessage(), ex);
                }
            }
        }

    }

    private static void writeXml(Document doc,
            OutputStream output)
            throws TransformerException {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(output);

        transformer.transform(source, result);

    }

    private void addDatasource() {
        getLog().debug("Creating datasource");
        if (projectModel.getDatasource() != null) {
            String dbType = projectModel.getDatasource().getDb();
            Map<String, Object> definitions = (Map<String, Object>) dbDefinitions.get(dbType);
            String driverDataSource = (definitions == null) ? dbType : (String) definitions.get("datasource");
            getLog().debug("Driver: " + driverDataSource);
            var style = DatasourceDefinitionStyleType.findByValue(projectModel.getDatasource().getStyle());
            switch (style) {
                case PAYARA_RESOURCE:

                    break;
                case WEB:
                    createWebXML();
                    break;
                default:
                    getLog().error("DataSource Style is invalid");
            }
        }
    }

    private void createWebXML() {
        try {
            Path webXmlPath = Paths.get(mavenProject.getBasedir().toString(), "src", "main", "webapp", "WEB-INF", "web.xml").normalize();
            getLog().debug("Creating DataSource at " + webXmlPath);
            Files.createDirectories(webXmlPath.getParent());
            String dataSourceName = "java:app/jdbc/" + mavenProject.getArtifactId();
            boolean createDataSource;
            Document doc;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Element rootElement;
            if (Files.exists(webXmlPath)) {
                doc = db.parse(webXmlPath.toFile());
                doc.getDocumentElement().normalize();
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList dataSourceNodeList = (NodeList) xPath.compile("/web-app/data-source/name[text()='" + dataSourceName + "']").evaluate(doc, XPathConstants.NODESET);
                createDataSource = dataSourceNodeList.getLength() == 0;
                rootElement = (Element) doc.getElementsByTagName("web-app").item(0);
            } else {
                createDataSource = true;
                doc = db.newDocument();
                rootElement = doc.createElementNS("http://xmlns.jcp.org/xml/ns/javaee", "web-app");
                rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                rootElement.setAttribute("xsi:schemaLocation", "http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd");
                rootElement.setAttribute("version", "4.0");
                doc.appendChild(rootElement);
                rootElement.appendChild(createElement(doc, "session-config", createElement(doc, "session-timeout", "30")));

            }
            if (createDataSource) {
                String dbType = projectModel.getDatasource().getDb();
                Map<String, Object> definitions = (Map<String, Object>) dbDefinitions.get(dbType);
                String driverDataSource = (definitions == null) ? dbType : (String) definitions.get("datasource");

                Element dataSourceElem = createElement(doc, "data-source");
                dataSourceElem.appendChild(createElement(doc, "name", dataSourceName));
                dataSourceElem.appendChild(createElement(doc, "class-name", driverDataSource));
                dataSourceElem.appendChild(createElement(doc, "url", projectModel.getDatasource().getUrl()));
                dataSourceElem.appendChild(createElement(doc, "user", projectModel.getDatasource().getUsername()));
                dataSourceElem.appendChild(createElement(doc, "password", projectModel.getDatasource().getPassword()));
                if (projectModel.getDatasource().getProperties() != null) {
                    for (Map.Entry<String, String> entry : projectModel.getDatasource().getProperties().entrySet()) {
                        dataSourceElem.appendChild(
                                createElement(
                                        doc, "property",
                                        createElement(doc, "name", entry.getKey()),
                                        createElement(doc, "value", entry.getValue()
                                        )
                                )
                        );
                    }
                }
                rootElement.appendChild(dataSourceElem);

                try ( FileOutputStream output
                        = new FileOutputStream(webXmlPath.toFile())) {
                    writeXml(doc, output);
                } catch (TransformerException ex) {
                    getLog().error(ex.getMessage(), ex);
                }
            }
        } catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException ex) {
            getLog().error(ex.getMessage(), ex);
        }

    }

    private static Element createElement(Document doc, String name, String textContent) {
        Element elem = doc.createElement(name);
        elem.setTextContent(textContent);
        return elem;
    }

    private static Element createElement(Document doc, String name, Node... children) {
        Element elem = doc.createElement(name);
        for (Node child : children) {
            elem.appendChild(child);

        }
        return elem;
    }

    private static Element createElement(Document doc, String name) {
        Element elem = doc.createElement(name);
        return elem;
    }

}
