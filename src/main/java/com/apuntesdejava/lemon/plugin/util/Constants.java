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

/**
 * Class with constants of the expressions used for the plugin
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class Constants {

    /**
     * Value 4
     */
    public static final int TAB = 4;
    /**
     * URL with dependencies for Jakarta Lemon
     */
    public final static String DEPENDENCIES_URL = "https://jakartalemon.dev/dependencies.json";

    /**
     * URL with plugins for Jakarta Lemon
     */
    public final static String LEMON_CONFIG_URL = "https://jakartalemon.dev/lemon-plugin-config.json";
    /**
     * Value {@code groupId }
     */
    public final static String DEPENDENCY_GROUP_ID = "groupId";

    /**
     * Value {@code artifactId}
     */
    public final static String DEPENDENCY_ARTIFACT_ID = "artifactId";

    /**
     * Value {@code version}
     */
    public final static String DEPENDENCY_VERSION = "version";

    /**
     * Value {@code type}
     */
    public final static String DEPENDENCY_TYPE = "type";

    /**
     * Value {@code runtimeArtifact}
     */
    public final static String RUNTIME_ARTIFACT = "runtimeArtifact";
    public final static String OUTCOME = "outcome";

    /**
     * Value {@code openliberty}
     */
    public final static String OPENLIBERTY = "openliberty";
    public final static String LIST = "list";

    /**
     * Value {@code payara-resources}
     */
    public final static String PAYARA_RESOURCES = "payara-resources";
    /**
     * Value {@code persistence}
     */
    public final static String PERSISTENCE = "persistence";

    /**
     * Value {@code configuration}
     */
    public final static String CONFIGURATION = "configuration";

    /**
     * Value {@code enabled}
     */
    public final static String ENABLED = "enabled";

    /**
     * Value {@code xmlns}
     */
    public final static String XMLNS = "xmlns";
    /**
     * Value {@code xmlns:xsi}
     */
    public final static String XMLNS_XSI = "xmlns:xsi";
    /**
     * Value {@code http://www.w3.org/2001/XMLSchema-instance}
     */
    public final static String XMLNS_XSI_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";
    /**
     * Value {@code datasource}
     */
    public final static String DATASOURCE = "datasource";
    /**
     * Value {@code url}
     */
    public final static String URL = "url";
    /**
     * Value {@code password}
     */
    public final static String PASSWORD = "password";
    /**
     * Value {@code properties}
     */
    public final static String PROPERTIES = "properties";
    /**
     * Value {@code user}
     */
    public final static String USER = "user";
    /**
     * Value {@code version}
     */
    public final static String VERSION = "version";
    /**
     * Value {@code style}
     */
    public final static String STYLE = "style";
    /**
     * Value {@code property}
     */
    public static final String PROPERTY = "property";
    /**
     * Value {@code get}
     */
    public static final String GET = "get";
    /**
     * Value {@code post}
     */
    public static final String POST = "post";
    /**
     * Value {@code put}
     */
    public static final String PUT = "put";
    /**
     * Value {@code delete}
     */
    public static final String DELETE = "delete";
    /**
     * It is the query string to look up the dependencies in the Maven repository.
     * <a href="https://central.sonatype.org/search/rest-api-guide/">REST API Maven Query</a>
     * Value {@code https://search.maven.org/solrsearch/select?q=}
     */

    public static final String QUERY_MAVEN_URL = "https://search.maven.org/solrsearch/select?q=";
    /**
     * Value {@code jsf}
     */
    public static final String VIEW_STYLE_JSF = "jsf";
    /**
     * Value {@code style}
     */
    public static final String VIEW_STYLE = "style";
    /**
     * Value {@code packageName}
     */

    public static final String PACKAGE_NAME = "packageName";
    /**
     * Value {@code projectName}
     */
    public static final String PROJECT_NAME = "projectName";
    /**
     * Value {@code entities}
     */

    public static final String ENTITIES = "entities";
    /**
     * Value {@code name}
     */
    public static final String NAME = "name";
    /**
     * Value {@code value}
     */
    public static final String VALUE = "value";
    public static final String MAX = "max";
    public static final String SIZE = "size";
    public static final String OPTIONS = "options";
    /**
     * Value {@code tableName}
     */
    public static final String TABLE_NAME = "tableName";
    /**
     * Value {@code finders}
     */
    public static final String FINDERS = "finders";
    /**
     * Value {@code native}
     */
    public static final String NATIVE_QUERY = "native";
    /**
     * Value {@code query}
     */
    public static final String QUERY = "query";
    /**
     * Value {@code return}
     */
    public static final String RETURN_VALUE_TYPE = "return";
    /**
     * Value {@code fields}
     */
    public static final String FIELDS = "fields";
    /**
     * Value {@code pk}
     */
    public static final String PK = "pk";
    /**
     * Value {@code join}
     */
    public static final String JOIN = "join";
    /**
     * Value {@code columnName}
     */
    public static final String COLUMN_NAME = "columnName";
    /**
     * Value {@code length}
     */
    public static final String LENGTH = "length";
    /**
     * Value {@code generatedValue}
     */
    public static final String GENERATED_VALUE = "generatedValue";
    /**
     * Value {@code type}
     */
    public static final String TYPE = "type";
    /**
     * Value {@code db}
     */
    public static final String DB = "db";
    /**
     * Value {@code parameters}
     */
    public static final String PARAMETERS = "parameters";
    /**
     * Value {@code unique}
     */
    public static final String UNIQUE = "unique";
    /**
     * Value {@code jakarta.faces.webapp.FacesServlet}
     */

    public static final String FACES_SERVLET = "jakarta.faces.webapp.FacesServlet";
    /**
     * Value {@code Server Faces Servlet}
     */
    public static final String FACES_SERVLET_NAME = "Server Faces Servlet";

    /**
     * Value {@code servlet-name}
     */
    public static final String SERVLET_NAME = "servlet-name";
    /**
     * Value {@code servlet-class}
     */
    public static final String SERVLET_CLASS = "servlet-class";
    /**
     * Value {@code servlet}
     */
    public static final String SERVLET = "servlet";
    /**
     * Value {@code url-pattern}
     */
    public static final String URL_PATTERN = "url-pattern";
    /**
     * Value {@code liberty.var.system.http.port}
     */

    public static final String LIBERTY_VAR_SYSTEM_HTTP_PORT = "liberty.var.system.http.port";
    /**
     * Value {@code liberty.var.default.http.port}
     */
    public static final String LIBERTY_VAR_DEFAULT_HTTP_PORT = "liberty.var.default.http.port";
    /**
     * Value {@code liberty.var.default.https.port}
     */
    public static final String LIBERTY_VAR_DEFAULT_HTTPS_PORT = "liberty.var.default.https.port";
    /**
     * Value {@code liberty.var.app.context.root}
     */
    public static final String LIBERTY_VAR_APP_CONTEXT_ROOT = "liberty.var.app.context.root";
    /**
     * Value {@code plugin}
     */
    public static final String PLUGIN = "plugin";
    /**
     * Value {@code serverName}
     */
    public static final String SERVER_NAME = "serverName";
    /**
     * Value {@code server}
     */
    public static final String SERVER = "server";
    /**
     * Value {@code feature}
     */
    public static final String FEATURE = "feature";
    /**
     * Value {@code featureManager}
     */
    public static final String FEATURE_MANAGER = "featureManager";
    /**
     * Value {@code components}
     */
    public static final String COMPONENTS = "components";
    /**
     * Value {@code schemas}
     */
    public static final String SCHEMAS = "schemas";
    /**
     * Value {@code schema}
     */
    public static final String SCHEMA = "schema";
    /**
     * Value {@code object}
     */
    public static final String OBJECT = "object";
    /**
     * Value {@code content}
     */
    public static final String CONTENT = "content";
    /**
     * Value {@code path}
     */
    public static final String PATH = "path";
    /**
     * Value {@code paths}
     */
    public static final String PATHS = "paths";
    /**
     * Value {@code resources}
     */
    public static final String RESOURCES = "resources";
    /**
     * Value {@code default}
     */
    public static final String DEFAULT = "default";
    /**
     * Value {@code in}
     */
    public static final String IN = "in";
    /**
     * Value {@code items}
     */
    public static final String ITEMS = "items";
    /**
     * Value {@code array}
     */
    public static final String ARRAY = "array";
    /**
     * Value {@code $ref}
     */
    public static final String REF = "$ref";
    /**
     * Value {@code responses}
     */
    public static final String RESPONSES = "responses";
    /**
     * Value {@code response}
     */
    public static final String RESPONSE = "response";
    /**
     * Value {@code docs}
     */
    public static final String DOCS = "docs";
    /**
     * Value {@code g}
     */
    public static final String G_KEY = "g";
    /**
     * Value {@code a}
     */
    public static final String A_KEY = "a";
    /**
     * Value {@code latestVersion}
     */
    public static final String LATEST_VERSION = "latestVersion";
    /**
     * Value {@code requestBody}
     */
    public static final String REQUEST_BODY = "requestBody";
    /**
     * Value {@code org.primefaces}
     */

    public static final String PRIMEFACES_GROUP_ID = "org.primefaces";

    /**
     * Value {@code primefaces}
     */
    public static final String PRIMEFACES_ARTIFACT_ID = "primefaces";

    /**
     * Value {@code org.webjars.npm}
     */
    public static final String PRIMEFLEX_GROUP_ID = "org.webjars.npm";

    /**
     * Value {@code primeflex}
     */
    public static final String PRIMEFLEX_ARTIFACT_ID = "primeflex";

    /**
     * Value {@code src}
     */
    public static final String SRC_PATH = "src";

    /**
     * Value {@code main}
     */
    public static final String MAIN_PATH = "main";

    public static final String STRING_TYPE = "String";
    public static final String LOCALDATE_TYPE = "LocalDate";
    public static final String MULTI = "multi";

    /**
     * Value {@code  java}
     */
    public static final String JAVA_PATH = "java";

    /**
     * Value {@code systemPropertyVariables}
     */
    public static final String SYSTEM_PROPERTY_VARIABLES = "systemPropertyVariables";

    /**
     * Value {@code web-app}
     */
    public static final String WEB_APP = "web-app";

    /**
     * Value {@code META-INF}
     */
    public static final String META_INF = "META-INF";

    /**
     * Value {@code dependency}
     */
    public static final String DEPENDENCY = "dependency";

    /**
     * Value {@code  jdbcLib}
     */
    public static final String JDBC_LIB = "jdbcLib";

    /**
     * Value {@code jdbc}
     */
    public static final String JDBC = "jdbc";

    /**
     * Value {@code jndiName}
     */
    public static final String ID = "id";

    /**
     * Value {@code jndiName}
     */
    public static final String JNDI_NAME = "jndiName";

    /**
     * Value {@code key}
     */
    public static final String KEY = "key";

    /**
     * Value {@code target/lib}
     */
    public static final String TARGET_LIB = "target/lib";

    /**
     * Value {@code copy}
     */
    public static final String COPY = "copy";

    /**
     * Value {@code copy-jdbc}
     */
    public static final String COPY_JDBC = "copy-jdbc";

    /**
     * Value {@code webapp}
     */
    public static final String WEBAPP = "webapp";

    /**
     * Value {@code web.xml}
     */
    public static final String WEBXML = "web.xml";
    /**
     * Value {@code WEB-INF}
     */
    public static final String WEB_INF_PATH = "WEB-INF";
    /**
     * value {@code option}
     */
    public static final String OPTION = "option";
    /**
     * Value {@code scope}
     */
    public static final String SCOPE = "scope";
    /**
     * Value {@code classifier }
     */
    public static final String CLASSIFIER = "classifier";
    /**
     * Value {@code '/'}
     */
    public static final char SLASH = '/';

    /**
     * Value {@code  org.apache.maven.plugins }
     */
    public static final String MAVEN_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
    public static final String FOR = "for";

    public static final String PRIMEFLEX_CSS = "primeflex/%s/primeflex.min.css";
    public static final String BLOCK = "block";
    public static final String LAYOUT = "layout";
    public static final String STYLECLASS = "styleClass";
    public static final String H_PANEL_GROUP = "h:panelGroup";
    public static final String LABEL = "label";
    public static final String P_LINK_BUTTON = "p:linkButton";
    public static final String P_LINK = "p:link";
    public static final String ITEM_LABEL = "itemLabel";
    public static final String OPTIONS_TYPE = "optionsType";
    public static final String P_SELECT_ONE_RADIO = "p:selectOneRadio";

    public static final String PAYARA_MICRO_PROFILE = "payara-micro";

    private Constants() {
    }

}
