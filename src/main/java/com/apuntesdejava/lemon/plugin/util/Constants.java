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

import java.util.Map;

/**
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class Constants {

    public static final int TAB = 4;

    private Constants() {
    }

    public final static String DATASOURCE = "datasource";
    public final static String DRIVER = "driver";
    public final static String POOL = "pool";
    public final static String SEARCH = "search";

    public static final String QUERY_MAVEN_URL = "https://search.maven.org/solrsearch/select";

    public static final String VIEW_STYLE_JSF ="jsf";
    public static final String VIEW_STYLE ="style";

    public static final Map<String, Object> DB_DEFINITIONS = Map.of(
            "mysql", Map.of(
                    DRIVER, "com.mysql.cj.jdbc.Driver",
                    DATASOURCE, "com.mysql.cj.jdbc.MysqlDataSource",
                    POOL, "com.mysql.cj.jdbc.MysqlConnectionPoolDataSource",
                    SEARCH, "g:mysql+AND+a:mysql-connector-java"
            ),
            "postgresql", Map.of(
                    DRIVER, "org.postgresql.Driver",
                    DATASOURCE, "org.postgresql.jdbc3.Jdbc3ConnectionPool",
                    SEARCH, "g:org.postgresql+AND+a:postgresql"
            ),
            "mariadb", Map.of(
                    DRIVER, "org.mariadb.jdbc.Driver",
                    DATASOURCE, "org.mariadb.jdbc.MariaDbDataSource",
                    POOL, "org.mariadb.jdbc.MariaDbPoolDataSource",
                    SEARCH, "g:org.mariadb.jdbc+AND+a:mariadb-java-client"
            )
    );

}
