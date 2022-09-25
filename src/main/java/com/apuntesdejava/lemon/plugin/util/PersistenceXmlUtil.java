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

import com.apuntesdejava.lemon.jakarta.persistence.model.PersistenceModel;
import com.apuntesdejava.lemon.jakarta.persistence.model.PersistenceUnitModel;
import com.apuntesdejava.lemon.jakarta.persistence.model.PropertiesModel;
import com.apuntesdejava.lemon.jakarta.persistence.model.PropertyModel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class PersistenceXmlUtil extends AbstractXmlUtil<PersistenceModel> {


    public PersistenceXmlUtil(String basedir) {
        super(PersistenceModel.class, basedir);
    }

    @Override
    protected PersistenceModel newModelInstance() {
        var model = new PersistenceModel();
        var persistenceUnit = new PersistenceUnitModel();
        persistenceUnit.setProperties(
                new PropertiesModel(
                        List.of(new PropertyModel("jakarta.persistence.schema-generation.database.action", "create"))
                )
        );
        model.setPersistenceUnit(persistenceUnit);
        return model;
    }

    @Override
    protected Path getXmlFullPath(String baseDir) {
        return Paths.get(baseDir, "src", "main", "resources", "META-INF", "persistence.xml");
    }
}
