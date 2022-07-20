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

import com.apuntesdejava.lemon.jakarta.payararesources.model.PayaraResourcesModel;
import com.apuntesdejava.lemon.jakarta.webxml.model.WebAppModel;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class PayaraResourcesXmlUtil {

    private static PayaraResourcesXmlUtil INSTANCE;
    private final Path payaraResourcesXmlPath;

    private PayaraResourcesXmlUtil(Path payaraResourcesXmlPath) {
        this.payaraResourcesXmlPath = payaraResourcesXmlPath;
    }

    private static void newInstance(Path payaraResourcesXmlPath) {
        INSTANCE = new PayaraResourcesXmlUtil(payaraResourcesXmlPath);
    }

    public static PayaraResourcesXmlUtil getInstance(String baseDir) {
        Path resourceXml = Paths.get(baseDir, "src", "main", "setup", "payara-resources.xml").normalize();
        if (INSTANCE == null) newInstance(resourceXml);
        return INSTANCE;
    }
    public PayaraResourcesModel getPayaraResourcesXml() throws JAXBException, IOException {
        if (Files.exists(payaraResourcesXmlPath)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(WebAppModel.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (PayaraResourcesModel) jaxbUnmarshaller.unmarshal(payaraResourcesXmlPath.toFile());
        }
        Files.createDirectories(payaraResourcesXmlPath.getParent());
        var model = new PayaraResourcesModel();
        return model;
    }

    public void savePayaraResourcesXml(PayaraResourcesModel payaraResourcesModel) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(PayaraResourcesModel.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        //marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd");
        marshaller.marshal(payaraResourcesModel, payaraResourcesXmlPath.toFile());
    }
}
