/*
 * Copyright 2021 Diego Silva <diego.silva at apuntesdejava.com>.
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
package com.apuntesdejava.lemon.jakarta.model;

import java.util.List;
import java.util.Map;
import lombok.Data;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
@Data
public class ProjectModel {

    private String rest;

    private List<EntityModel> entities;

    private String packageName;
    private String projectName;

    private DataSourceModel datasource;

    public Map<String, Object> getDbDefinitions() {
        return (Map<String, Object>) DB_DEFINITIONS.get(datasource.getDb());
    }

    public String getDriver() {
        return DB_DEFINITIONS.containsKey(datasource.getDb())
                ? (String) ((Map<String, Object>) DB_DEFINITIONS.get(datasource.getDb())).get("datasource")
                : datasource.getDb();
    }
    private static final Map<String, Object> DB_DEFINITIONS
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
}
