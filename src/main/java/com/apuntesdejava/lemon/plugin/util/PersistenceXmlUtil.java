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

import org.w3c.dom.Document;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class PersistenceXmlUtil {

    private static final String PERSISTENCE_FILE_NAME = "persistence.xml";

    public static Document openPersistenceXml(File basedir) throws IOException {
        Path xmlPath = Paths.get(basedir.toString(), SRC_PATH, MAIN_PATH, RESOURCES, META_INF, PERSISTENCE_FILE_NAME)
            .normalize();
        Files.createDirectories(xmlPath.getParent());
        return DocumentXmlUtil.openDocument(xmlPath).orElseGet(() -> {
            try {
                var document = DocumentXmlUtil.newDocument(PERSISTENCE);
                DocumentXmlUtil.findElementsByFilter(document, "/" + PERSISTENCE)
                    .stream()
                    .findFirst()
                    .ifPresent(persistenceElement -> {
                        persistenceElement.setAttribute(XMLNS, "https://jakarta.ee/xml/ns/persistence");
                        persistenceElement.setAttribute(XMLNS_XSI, XMLNS_XSI_INSTANCE);
                        persistenceElement.setAttribute(VERSION, "3.0");
                    });
                return document;
            } catch (ParserConfigurationException | XPathExpressionException ex) {
                throw new RuntimeException(ex);
            }
        });

    }

    public static void saveWebXml(File basedir, Document document) {
        Path webXmlPath = Paths.get(basedir.toString(), SRC_PATH, MAIN_PATH, RESOURCES, META_INF, PERSISTENCE_FILE_NAME)
            .normalize();
        DocumentXmlUtil.saveDocument(webXmlPath, document);

    }

}
