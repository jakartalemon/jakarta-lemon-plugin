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

import com.apuntesdejava.lemon.jakarta.model.DependencyModel;
import com.apuntesdejava.lemon.jakarta.model.MavenDocResponse;
import com.apuntesdejava.lemon.jakarta.model.MavenResponse;
import static com.apuntesdejava.lemon.plugin.util.Constants.DB_DEFINITIONS;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class DependenciesUtil {

    private static final Logger LOGGER = Logger.getLogger(DependenciesUtil.class.getName());

    private static final String HOST_MAVEN_SEARCH = "https://search.maven.org";

    private DependenciesUtil() {

    }

    public static DependencyModel getByDatabase(String database) {
        Map<String, Object> aDef = (Map<String, Object>) DB_DEFINITIONS.get(database);
        return getLastVersionDependency((String) aDef.get("search"));
    }

    public static DependencyModel getLastVersionDependency(String query) {
        try ( CloseableHttpClient httpclient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(HOST_MAVEN_SEARCH + "/solrsearch/select?q=" + query);
            try ( CloseableHttpResponse response = httpclient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                String resp = EntityUtils.toString(entity);
                LOGGER.log(Level.FINE, "resp:{0}", resp);
                Jsonb jsonb = JsonbBuilder.create();
                StatusLine statusLine = response.getStatusLine();
                LOGGER.log(Level.FINE, "code:{0}", statusLine.getStatusCode());
                LOGGER.log(Level.FINE, "phrase:{0}", statusLine.getReasonPhrase());
                EntityUtils.consumeQuietly(entity);
                MavenResponse mavenResponse = jsonb.fromJson(resp, MavenResponse.class);
                MavenDocResponse model = mavenResponse.getResponse().getDocs().get(0);
                return new DependencyModel(model.getG(), model.getA(), model.getLatestVersion());
            }
        } catch (IOException ex) {
            LOGGER.severe(ex.getMessage());
        }
        return null;

    }

}
