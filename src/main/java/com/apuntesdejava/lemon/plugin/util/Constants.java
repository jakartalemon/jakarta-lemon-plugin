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
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class Constants {

    public static final int TAB = 4;

    public final static String DEPENDENCIES_URL = "https://jakartalemon.dev/dependencies.json";
    public final static String LEMON_CONFIG_URL = "https://jakartalemon.dev/lemon-plugin-config.json";

    public final static String DEPENDENCY_GROUP_ID = "groupId";
    public final static String DEPENDENCY_ARTIFACT_ID = "artifactId";
    public final static String DEPENDENCY_VERSION = "version";
    public final static String DEPENDENCY_TYPE = "type";
    public final static String RUNTIME_ARTIFACT = "runtimeArtifact";
    public final static String OPENLIBERTY = "openliberty";
    public final static String PERSISTENCE = "persistence";
    public final static String CONFIGURATION = "configuration";
    public final static String ENABLED = "enabled";

    public final static String XMLNS = "xmlns";
    public final static String XMLNS_XSI = "xmlns:xsi";
    public final static String XMLNS_XSI_INSTANCE = "http://www.w3.org/2001/XMLSchema-instance";

    public final static String DATASOURCE = "datasource";
    public final static String URL = "url";
    public final static String PASSWORD = "password";
    public final static String USER = "user";
    public final static String PROPERTIES = "properties";
    public final static String VERSION = "version";
    public final static String STYLE = "style";

    public static final String PROPERTY = "property";
    public static final String GET = "get";
    public static final String POST = "post";
    public static final String PUT = "put";
    public static final String DELETE = "delete";

    public static final String QUERY_MAVEN_URL = "https://search.maven.org/solrsearch/select?q=";

    public static final String VIEW_STYLE_JSF = "jsf";
    public static final String VIEW_STYLE = "style";

    public static final String PACKAGE_NAME = "packageName";
    public static final String PROJECT_NAME = "projectName";

    public static final String ENTITIES = "entities";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String TABLE_NAME = "tableName";
    public static final String FINDERS = "finders";
    public static final String NATIVE_QUERY = "native";
    public static final String QUERY = "query";
    public static final String RETURN_VALUE_TYPE = "return";
    public static final String FIELDS = "fields";
    public static final String PK = "pk";
    public static final String JOIN = "join";
    public static final String COLUMN_NAME = "columnName";
    public static final String LENGTH = "length";
    public static final String GENERATED_VALUE = "generatedValue";
    public static final String TYPE = "type";
    public static final String DB = "db";
    public static final String PARAMETERS = "parameters";
    public static final String UNIQUE = "unique";

    public static final String FACES_SERVLET = "jakarta.faces.webapp.FacesServlet";
    public static final String FACES_SERVLET_NAME = "Server Faces Servlet";

    public static final String SERVLET_NAME = "servlet-name";
    public static final String SERVLET_CLASS = "servlet-class";
    public static final String SERVLET = "servlet";
    public static final String URL_PATTERN = "url-pattern";

    public static final String LIBERTY_VAR_SYSTEM_HTTP_PORT = "liberty.var.system.http.port";
    public static final String LIBERTY_VAR_DEFAULT_HTTP_PORT = "liberty.var.default.http.port";
    public static final String LIBERTY_VAR_DEFAULT_HTTPS_PORT = "liberty.var.default.https.port";
    public static final String LIBERTY_VAR_APP_CONTEXT_ROOT = "liberty.var.app.context.root";
    public static final String PLUGIN = "plugin";
    public static final String SERVER_NAME = "serverName";
    public static final String SERVER = "server";
    public static final String FEATURE = "feature";
    public static final String FEATURE_MANAGER = "featureManager";
    public static final String COMPONENTS = "components";
    public static final String SCHEMAS = "schemas";
    public static final String SCHEMA = "schema";
    public static final String OBJECT = "object";
    public static final String CONTENT = "content";
    public static final String PATH = "path";
    public static final String PATHS = "paths";
    public static final String RESOURCES = "resources";
    public static final String DEFAULT = "default";
    public static final String IN = "in";
    public static final String ITEMS = "items";
    public static final String ARRAY = "array";
    public static final String REF = "$ref";
    public static final String RESPONSES = "responses";
    public static final String RESPONSE = "response";
    public static final String DOCS = "docs";
    public static final String G_KEY = "g";
    public static final String A_KEY = "a";
    public static final String LATEST_VERSION = "latestVersion";
    public static final String REQUEST_BODY = "requestBody";

    public static final String PRIMEFACES_GROUP_ID = "org.primefaces";
    public static final String PRIMEFACES_ARTIFACT_ID = "primefaces";
    public static final String PRIMEFLEX_GROUP_ID = "org.webjars.npm";
    public static final String PRIMEFLEX_ARTIFACT_ID = "primeflex";
    public static final String SRC_PATH = "src";
    public static final String MAIN_PATH = "main";
    public static final String JAVA_PATH = "java";

    public static final String SYSTEM_PROPERTY_VARIABLES = "systemPropertyVariables";

    public static final String WEB_APP = "web-app";

    public static final String META_INF = "META-INF";

    public static final String DEPENDENCY = "dependency";

    public static final String JDBC_LIB = "jdbcLib";
    public static final String JDBC = "jdbc";
    public static final String ID = "id";

    public static final String JNDI_NAME = "jndiName";

    public static final String KEY = "key";

    public static final String TARGET_LIB = "target/lib";
    public static final String COPY = "copy";
    public static final String COPY_JDBC = "copy-jdbc";
    public static final String WEBAPP = "webapp";
    public static final String WEBXML = "web.xml";
    public static final String WEB_INF_PATH = "WEB-INF";
    public static final String OPTION = "option";
    public static final String SCOPE = "scope";
    public static final String CLASSIFIER = "classifier";

    public static final String MAVEN_PLUGIN_GROUP_ID="org.apache.maven.plugins";

    private Constants() {
    }

}
