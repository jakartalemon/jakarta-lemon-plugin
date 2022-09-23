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
import static com.apuntesdejava.lemon.plugin.util.Constants.DB_DEFINITIONS;
import static com.apuntesdejava.lemon.plugin.util.Constants.SEARCH;
import jakarta.json.Json;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class DependenciesUtil {

    private static final String HOST_MAVEN_SEARCH = "https://search.maven.org";

    private DependenciesUtil() {

    }

    public static DependencyModel getByDatabase(Log log, String database) {
        Map<String, Object> aDef = (Map<String, Object>) DB_DEFINITIONS.get(database);
        return getLastVersionDependency(log, (String) aDef.get(SEARCH));
    }

    public static DependencyModel getLastVersionDependency(Log log, String query) {
        try ( CloseableHttpClient httpclient = HttpClients.createDefault()) {
            String uri = HOST_MAVEN_SEARCH + "/solrsearch/select?q=" + query;
            log.debug("getting uri:" + uri);
            HttpGet httpGet = new HttpGet(uri);
            try ( CloseableHttpResponse response = httpclient.execute(httpGet)) {
                HttpEntity entity = response.getEntity();
                StatusLine statusLine = response.getStatusLine();
                log.debug("code:" + statusLine.getStatusCode());
                String json = EntityUtils.toString(entity);
                EntityUtils.consumeQuietly(entity);
                log.debug("resp:" + json);
                try ( StringReader stringReader = new StringReader(json);  var jsonReader = Json.createReader(stringReader)) {

                    var jsonResp = jsonReader.readObject();
                    var responseJson = jsonResp.getJsonObject("response");
                    var docsJson = responseJson.getJsonArray("docs");
                    var docJson = docsJson.get(0).asJsonObject();
                    return new DependencyModel(docJson.getString("g"), docJson.getString("a"), docJson.getString("latestVersion"));
                }
            }
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        return null;

    }

}