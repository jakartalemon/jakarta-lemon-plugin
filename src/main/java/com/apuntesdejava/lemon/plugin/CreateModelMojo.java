package com.apuntesdejava.lemon.plugin;

import com.apuntesdejava.lemon.jakarta.jpa.model.EntityModel;
import com.apuntesdejava.lemon.jakarta.jpa.model.FieldModel;
import com.apuntesdejava.lemon.jakarta.jpa.model.ProjectModel;
import com.apuntesdejava.lemon.jakarta.model.types.DatasourceDefinitionStyleType;
import com.apuntesdejava.lemon.jakarta.webxml.model.DataSourceModel;
import com.apuntesdejava.lemon.plugin.util.*;
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Mojo(name = "create-model")
public class CreateModelMojo extends AbstractMojo {

    private static void removeLastComma(List<String> list) {
        list.set(list.size() - 1, StringUtils.removeEnd(list.get(list.size() - 1), ","));
    }

    @Parameter(
            property = "model",
            defaultValue = "model.json"
    )
    private String modelProjectFile;
    private ProjectModel projectModel;

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    private DatasourceDefinitionStyleType style;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        ProjectModelUtil.getProjectModel(getLog(), modelProjectFile)
                .ifPresent(pm -> {
                    this.projectModel = pm;
                    buildModel();
                    addDatasource();
                    addDependencies();
                    addPersistenceXML();
                });
    }

    private void buildModel() {
        try {
            getLog().debug("Building model");
            String groupId = mavenProject.getGroupId();
            String packageName
                    = StringUtils.replaceChars(groupId + '.' + mavenProject.getArtifactId(), '-',
                            '.');
            projectModel.setPackageName(packageName);
            projectModel.setProjectName(mavenProject.getId());
            Path baseDirPath = mavenProject.getBasedir().toPath();
            getLog().debug("groupId:" + groupId);
            getLog().debug("packageName:" + packageName);
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
            lines.add(
                    "import " + projectModel.getPackageName() + ".model." + entity.getName() + ";");
            lines.add("import jakarta.enterprise.context.ApplicationScoped;");
            lines.add("import jakarta.inject.Inject;");
            lines.add("import jakarta.persistence.EntityManager;\n");
            lines.add("@ApplicationScoped");
            Optional<Map.Entry<String, FieldModel>> pk
                    = entity.getFields().entrySet().stream().filter(item -> item.getValue().isPk())
                            .findFirst();
            String idClass = "Object";
            if (pk.isPresent()) {
                idClass = pk.get().getValue().getType();
            }
            lines.add(
                    "public class " + className + " extends AbstractRepository<" + idClass + ", "
                    + entity.getName() + "> {\n");

            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "@Inject");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                    + "private EntityManager em;\n");

            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "public " + className
                    + "() {");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + "super("
                    + entity.getName() + ".class);");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "}\n");

            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "@Override");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                    + "protected EntityManager getEntityManager() {");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2) + "return em;");
            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "}\n");

            if (entity.getFinders() != null && !entity.getFinders().isEmpty()) {
                getLog().debug("creando métodos de búsqueda");
                entity.getFinders().forEach((name, value) -> {
                    String params = "()";
                    if (value.getParameters() != null) {
                        StringBuilder param = new StringBuilder();
                        param.append("(\n");
                        value.getParameters().forEach((paramName, type) -> {
                            param.append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2));
                            param.append(type).append(' ');
                            param.append(paramName).append(',');
                        });
                        param.setLength(param.length() - 1);
                        param.append("\n")
                                .append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB))
                                .append(")");
                        params = param.toString();
                    }
                    lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "public "
                            + value.getReturnValueType() + " findBy" + name + params + " {");
                    lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2)
                            + "return em."
                            + (value.isNativeQuery() ? "createNativeQuery" : "createNamedQuery")
                            + "(\"" + entity.getName() + ".findBy" + name + "\","
                            + entity.getName() + ".class)");
                    if (value.getParameters() != null) {
                        value.getParameters().forEach((paramName, param) -> {
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 3)
                                    + ".setParameter(\"" + paramName + "\"," + paramName + ")");
                        });
                    }
                    if (value.isUnique()) {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 3)
                                + ".getSingleResult();");
                    } else {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 3)
                                + ".getResultList();");
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

    private void createService(Path packageBaseService, EntityModel entity) {
        try {
            getLog().debug("Creando servicio de " + entity.getName());
            String className = entity.getName() + "Service";
            Path target = packageBaseService.resolve(className + ".java");
            StringBuilder lines = new StringBuilder();
            lines.append("package ").append(projectModel.getPackageName()).append(".services;\n\n");
            StringBuilder repositoryClass
                    = new StringBuilder().append(entity.getName()).append("Repository");
            lines.append("import ").append(projectModel.getPackageName()).append(".repositories.")
                    .append(repositoryClass).append(";\n");
            lines.append("import ").append(projectModel.getPackageName()).append(".model.")
                    .append(entity.getName()).append(';').append('\n');
            lines.append("import jakarta.enterprise.context.ApplicationScoped;\n");
            lines.append("import jakarta.inject.Inject;\n\n");
            lines.append("@ApplicationScoped\n");
            Optional<Map.Entry<String, FieldModel>> pk
                    = entity.getFields().entrySet().stream().filter(item -> item.getValue().isPk())
                            .findFirst();
            String idClass = pk.isPresent() ? pk.get().getValue().getType() : "Object";
            lines.append("public class ").append(className).append(" extends AbstractService<")
                    .append(idClass).append(',').append(entity.getName()).append(',')
                    .append(repositoryClass).append('>').append('{').append('\n');

            lines.append('\n').append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB))
                    .append("@Inject\n")
                    .append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)).append("private ")
                    .append(repositoryClass).append(" repository;\n");

            lines.append("\n\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB))
                    .append("@Override\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)).append("public ")
                    .append(repositoryClass).append(" getRepository(){\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2))
                    .append("return repository;\n");
            lines.append(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)).append("}\n");

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
            try (InputStream is = getClass().getResourceAsStream(source)) {
                if (is != null) {
                    List<String> code = IOUtils.readLines(is, Charset.defaultCharset());
                    List<String> newCode = code
                            .stream()
                            .map(
                                    line -> StringUtils.replaceEach(line,
                                            maps.keySet().toArray(String[]::new),
                                            maps.values().toArray(String[]::new)
                                    )
                            )
                            .collect(Collectors.toList());
                    Files.write(target, newCode);
                }
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
            if (StringUtils.isNotBlank(entity.getTableName())) {
                lines.add(String.format("@jakarta.persistence.Table(name = \"%s\" )",
                        entity.getTableName()));
            }
            lines.add("@jakarta.persistence.Entity");
            if (entity.getFinders() != null) {
                entity.getFinders().entrySet().stream()
                        .filter(entry -> entry.getValue().isNativeQuery())
                        .forEach(entry -> {
                            lines.add("@jakarta.persistence.NamedNativeQuery(");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                                    + String.format("name = \"%s.findBy%s\",", entity.getName(),
                                            entry.getKey()));
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                                    + "query = \"" + entry.getValue().getQuery() + ",\n");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                                    + "resultClass = " + entry.getValue().getReturnValueType());
                            lines.add(")");
                        });
                entity.getFinders().entrySet().stream()
                        .filter(entry -> !entry.getValue().isNativeQuery())
                        .forEach(entry -> {
                            lines.add("@jakarta.persistence.NamedQuery(");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                                    + "name = \"" + entity.getName() + ".findBy" + entry.getKey()
                                    + "\",");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                                    + "query = \"" + entry.getValue().getQuery() + "\"");
                            lines.add(")");
                        });
            }
            lines.add("public class " + entity.getName() + "{\n");
            if (entity.getFields() != null) {
                entity.getFields().forEach((key, value) -> {
                    if (value.isPk()) {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                                + "@jakarta.persistence.Id");
                    }
                    if (StringUtils.isNotBlank(value.getJoin())) {
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                                + String.format("@jakarta.persistence.%s", value.getJoin()));
                    }
                    if (StringUtils.isNotBlank(value.getColumnName())) {
                        if (StringUtils.isBlank(value.getJoin())) {
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                                    + "@jakarta.persistence.Column(");
                            List<String> attrsList = new ArrayList<>();
                            attrsList.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2)
                                    + "name = \"" + value.getColumnName() + "\",");
                            if (value.getLength() != null && value.getLength() > 0) {
                                attrsList.add(
                                        StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2)
                                        + "length = " + value.getLength() + ",");
                            }
                            removeLastComma(attrsList);
                            lines.addAll(attrsList);
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + ")");
                        } else {
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                                    + "@jakarta.persistence.JoinColumn(");
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2)
                                    + String.format("name = \"%s\"", value.getColumnName()));
                            lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + ")");

                        }
                    }
                    if (StringUtils.isNotBlank(value.getGeneratedValue())) {

                        GenerationType generatedValueType
                                = ObjectUtils.defaultIfNull(
                                        EnumUtils.getEnum(GenerationType.class,
                                                value.getGeneratedValue().toUpperCase()),
                                        GenerationType.AUTO
                                );
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB)
                                + "@jakarta.persistence.GeneratedValue(");
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB * 2)
                                + "strategy = jakarta.persistence.GenerationType."
                                + generatedValueType.name());
                        lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + ")");
                    }
                    lines.add(StringUtils.repeat(StringUtils.SPACE, Constants.TAB) + "private "
                            + value.getType() + " " + key + ";\n");
                });
            }
            lines.add("}");

            Files.write(target, lines);
        } catch (IOException ex) {
            getLog().error(ex.getMessage(), ex);
        }
    }

    private void addDependencies() {
        if (this.style
                == DatasourceDefinitionStyleType.WEB) { //se agrega dependencia solo si está incorporado dentro del .war
            addDBDependencies();
        }
        addProjectLombokDependency();
    }

    private void addDBDependencies() {
        try {
            getLog().debug("Add DB Dependencies");
            String database = projectModel.getDatasource().getDb();

            Model model = ProjectModelUtil.getModel(mavenProject);
            ProjectModelUtil.addDependenciesDatabase(getLog(), model, database);

            ProjectModelUtil.saveModel(mavenProject, model);
        } catch (IOException | XmlPullParserException ex) {
            getLog().error(ex.getMessage(), ex);
        }

    }

    private void addDatasource() {

        getLog().debug("Creating datasource");
        if (projectModel.getDatasource() != null) {
            try {
                String driverDataSource = ProjectModelUtil.getDriver(getLog(), projectModel.getDatasource().getDb());
                getLog().debug("Driver: " + driverDataSource);
                String styleSrc = projectModel.getDatasource().getStyle();
                this.style = DatasourceDefinitionStyleType.findByValue(styleSrc);
                if (style != null) {
                    switch (style) {
                        case PAYARA_RESOURCES:
                            PayaraUtil.createPayaraDataSourceResources(getLog(), projectModel,
                                    mavenProject);
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
            persistenceXml.getPersistenceUnit().setName(projectModel.getProjectName() + "PU");
            String dataSourceName = (style == DatasourceDefinitionStyleType.WEB ? "java:app/" : "")
                    + "jdbc/" + mavenProject.getArtifactId();
            persistenceXml.getPersistenceUnit().setJtaDataSource(dataSourceName);
            persistenceUtil.saveModel(persistenceXml);

        } catch (IOException | JAXBException ex) {
            getLog().error(ex.getMessage(), ex);
        }

    }

    private void createWebXML() {
        try {
            Path webXmlPath
                    = Paths.get(mavenProject.getBasedir().toString(), "src", "main", "webapp",
                            "WEB-INF", "web.xml").normalize();
            getLog().debug("Creating DataSource at " + webXmlPath);
            Files.createDirectories(webXmlPath.getParent());
            String dataSourceName = "java:app/jdbc/" + mavenProject.getArtifactId();

            var webXmlUtil = new WebXmlUtil(mavenProject.getBasedir().toString());
            var webXml = webXmlUtil.getModel();

            boolean createDataSource = webXml.getDataSource() == null;
            if (createDataSource) {
                String driverDataSource = ProjectModelUtil.getDriver(getLog(), projectModel.getDatasource().getDb());
                var dataSourceModelBuilder = new DataSourceModel.DataSourceModelBuilder()
                        .setName(dataSourceName)
                        .setClassName(driverDataSource)
                        .setUrl(projectModel.getDatasource().getUrl())
                        .setUser(projectModel.getDatasource().getUser())
                        .setPassword(projectModel.getDatasource().getPassword());

                if (projectModel.getDatasource().getProperties() != null) {
                    for (Map.Entry<String, String> entry : projectModel.getDatasource()
                            .getProperties().entrySet()) {
                        dataSourceModelBuilder.addProperty(entry.getKey(), entry.getValue());
                    }
                }
                webXml.setDataSource(dataSourceModelBuilder.build());
                webXmlUtil.saveModel(webXml);

            }
        } catch (IOException | JAXBException | InterruptedException | URISyntaxException ex) {
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
