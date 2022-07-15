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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class ViewModelUtil {

    private static ViewModelUtil INSTANCE;

    private synchronized static void newInstance() {
        INSTANCE = new ViewModelUtil();
    }

    private ViewModelUtil() {

    }

    public static ViewModelUtil getInstance() {
        if (INSTANCE == null) {
            newInstance();
        }
        return INSTANCE;
    }

    public Optional<JsonObject> getViewModel(Log log, String viewProjectFile) throws FileNotFoundException, IOException {
        log.debug("Reading view configuration:" + viewProjectFile);
        try ( InputStream in = new FileInputStream(viewProjectFile)) {
            return Optional.ofNullable(Json.createReader(in).readObject());
        }
    }

}
