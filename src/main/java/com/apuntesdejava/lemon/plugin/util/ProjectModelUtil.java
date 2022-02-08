/*
 * Copyright 2021 Apuntes de Java.
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

import com.apuntesdejava.lemon.jakarta.jpa.model.ProjectModel;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import org.apache.maven.plugin.logging.Log;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class ProjectModelUtil {

    private ProjectModelUtil() {

    }

    public static Optional<ProjectModel> getProjectModel(Log log, String modelProjectFile) {
        log.debug("Reading model configuration:" + modelProjectFile);
        try ( InputStream in = new FileInputStream(modelProjectFile)) {
            Jsonb jsonb = JsonbBuilder.create();
            return Optional.ofNullable(jsonb.fromJson(in, ProjectModel.class));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
        log.error("Model configuration file :" + modelProjectFile + " not found");
        return Optional.empty();
    }

}
