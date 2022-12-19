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

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class DependenciesUtil {

    private DependenciesUtil() {

    }

    public static Optional<JsonObject> getByDatabase(Log log,
            String database) {
        try {
            var dependenciesDefinitions = HttpClientUtil.getJson(log, DEPENDENCIES_URL, JsonReader::readObject);
            return Optional.ofNullable(dependenciesDefinitions.getJsonObject(database))
                    .map(dependency -> {

                        var query = String.format("g:%s+AND+a:%s", dependency.getString("g"),
                                dependency.getString("a"));
                        return getLastVersionDependency(log, query).get();
                    });
        } catch (IOException | InterruptedException | URISyntaxException ex) {
            log.error(ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public static Optional<JsonObject> getLastVersionDependency(Log log,
            String query) {
        try {
            String uri = QUERY_MAVEN_URL + query;
            var jsonResp = HttpClientUtil.getJson(log, uri, JsonReader::readObject);
            var responseJson = jsonResp.getJsonObject("response");
            var docsJson = responseJson.getJsonArray("docs");
            var docJson = docsJson.get(0).asJsonObject();
            return Optional.of(Json.createObjectBuilder().add(DEPENDENCY_GROUP_ID, docJson.getString("g"))
                    .add(DEPENDENCY_ARTIFACT_ID, docJson.getString("a"))
                    .add(DEPENDENCY_VERSION, docJson.getString("latestVersion")).build()
            );

        } catch (URISyntaxException | IOException | InterruptedException ex) {
            log.error(ex.getMessage(), ex);
        }
        return Optional.empty();
    }

}
