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
import jakarta.json.JsonReader;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

/**
 * Utility class for HTTP calls
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class HttpClientUtil {

    /**
     * It makes a GET HTTP call and the response processes it as JSON. The way it returns it is done through the read  parameter.
     *
     * @param <T>  Data type to be returned after processing the response.
     * @param log  Maven log
     * @param uri  Request URI
     * @param read Function that processes the request and returns a value based on the indicated type. This function must have a parameter of type {@link JsonReader}, and can return any data type.
     * @return Processed object of the request
     * @throws IOException IOException
     * @throws InterruptedException InterruptedException
     * @throws URISyntaxException URISyntaxException
     */
    public static <T> T getJson(Log log, String uri, Function<JsonReader, T> read) throws IOException, InterruptedException, URISyntaxException {
        log.debug("getting uri:" + uri);
        var httpRequest = HttpRequest.newBuilder(new URI(uri)).GET().build();
        var httpResponse = HttpClient.newBuilder().build().send(httpRequest, HttpResponse.BodyHandlers.ofString());
        log.debug("code:" + httpResponse.statusCode());
        var json = httpResponse.body();

        log.debug("resp:" + json);
        try (var stringReader = new StringReader(json); var jsonReader = Json.createReader(stringReader)) {
            return read.apply(jsonReader);
        }
    }
}
