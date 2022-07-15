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
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class PersistenceXmlUtil {

    private static PersistenceXmlUtil INSTANCE;
    private final Path persistenceXmlPath;

    private static synchronized void newInstance(Path persistenceXmlPath) {
        INSTANCE = new PersistenceXmlUtil(persistenceXmlPath);
    }

    public static PersistenceXmlUtil getInstance(String baseDir) {
        Path persistenceXmlPath = Paths.get(baseDir, "src", "main", "resources", "META-INF", "persistence.xml");
        if (INSTANCE == null) {
            newInstance(persistenceXmlPath);
        }
        return INSTANCE;
    }

    private PersistenceXmlUtil(Path persistenceXmlPath) {
        this.persistenceXmlPath = persistenceXmlPath;
    }

    public PersistenceModel getPersistenceXml() throws IOException, JAXBException {
        if (Files.exists(persistenceXmlPath)) {
            var jaxbContext = JAXBContext.newInstance(PersistenceModel.class);
            var jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (PersistenceModel) jaxbUnmarshaller.unmarshal(persistenceXmlPath.toFile());
        }
        Files.createDirectories(persistenceXmlPath.getParent());
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

    public void savePersistenceXml(PersistenceModel persistenceModel) throws JAXBException {
        var context = JAXBContext.newInstance(PersistenceModel.class);
        var marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://xmlns.jcp.org/xml/ns/persistence http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd");
        marshaller.marshal(persistenceModel, persistenceXmlPath.toFile());
    }

}
