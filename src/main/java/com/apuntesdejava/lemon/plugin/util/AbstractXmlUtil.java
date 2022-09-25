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

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 * @param <T> Type of XML model
 */
public abstract class AbstractXmlUtil<T> {

    private final Class<T> clazz;
    private final Path xmlPath;

    public AbstractXmlUtil(Class<T> clazz, String basedir) {
        this.clazz = clazz;
        xmlPath = getXmlFullPath(basedir);
    }

    public T getModel() throws JAXBException, IOException {
        if (Files.exists(xmlPath)) {
            var jaxbContext = JAXBContext.newInstance(clazz);
            var jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (T) jaxbUnmarshaller.unmarshal(xmlPath.toFile());
        } else Files.createDirectories(xmlPath.getParent());
        return newModelInstance();
    }

    public void saveModel(T model, Consumer<Marshaller> consumer) throws JAXBException {
        var context = JAXBContext.newInstance(clazz);
        var marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(model, xmlPath.toFile());
        if (consumer != null) consumer.accept(marshaller);
    }

    public void saveModel(T model) throws JAXBException {
        saveModel(model, null);
    }

    protected abstract T newModelInstance();


    public Path getXmlPath() {
        return xmlPath;
    }

    protected abstract Path getXmlFullPath(String baseDir);
}
