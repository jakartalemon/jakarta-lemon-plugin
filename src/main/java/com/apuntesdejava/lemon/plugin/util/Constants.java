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
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class Constants {

    private Constants() {
    }

    public static final String QUERY_MAVEN_URL = "https://search.maven.org/solrsearch/select";

    public static final Map<String, Object> DB_DEFINITIONS = Map.of(
            "mysql", Map.of(
                    "driver", "com.mysql.cj.jdbc.Driver",
                    "datasource", "com.mysql.cj.jdbc.MysqlDataSource",
                    "pool", "com.mysql.cj.jdbc.MysqlConnectionPoolDataSource",
                    "search", "g:mysql+AND+a:mysql-connector-java" 
            ),
            "postgresql", Map.of(
                    "driver", "org.postgresql.Driver",
                    "datasource", "org.postgresql.jdbc3.Jdbc3ConnectionPool",
                    "search", "g:org.postgresql+AND+a:postgresql" 
            ),
            "mariadb", Map.of(
                    "driver", "org.mariadb.jdbc.Driver",
                    "datasource", "org.mariadb.jdbc.MariaDbDataSource",
                    "pool", "org.mariadb.jdbc.MariaDbPoolDataSource",
                    "search", "g:org.mariadb.jdbc+AND+a:mariadb-java-client" 
            )
    );

}
