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
import javax.xml.transform.OutputKeys;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static com.apuntesdejava.lemon.plugin.util.Constants.*;

/**
 * Utility class for handling the XML file of Payara resources
 *
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class PayaraResourcesXmlUtil {

    private static final String PAYARA_RESOURCE_DOCTYPE_PUBLIC = "-//Payara.fish//DTD Payara Server 4 Resource Definitions//EN";
    private static final String PAYARA_RESOURCE_DOCTYPE_SYSTEM = "https://raw.githubusercontent.com/payara/Payara-Community-Documentation/master/docs/modules/ROOT/pages/schemas/payara-resources_1_6.dtd";

    /**
     * Gets the Payara resource XML file
     *
     * @param basedir Project Base dir
     * @return Document XML Payara Resources
     * @throws IOException IOException
     */
    public static Document openPayaraXml(File basedir) throws IOException {
        Path xmlPath = getPayaraResourcesPath(basedir);
        Files.createDirectories(xmlPath.getParent());
        return DocumentXmlUtil.openDocument(xmlPath).orElseGet(() -> {
            try {
                var document = DocumentXmlUtil.newDocument(RESOURCES);
                DocumentXmlUtil.listElementsByFilter(document, SLASH + RESOURCES)
                    .stream()
                    .findFirst()
                    .ifPresent(webappElement -> {
                    });
                return document;
            } catch (ParserConfigurationException | XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Save the Payara resource XML file
     *
     * @param basedir  Project Base dir
     * @param document Document XML Payara Resources
     * @return path of the Payara resource XML file
     */
    public static Path savePayaraXml(File basedir, Document document) {
        var path = getPayaraResourcesPath(basedir);
        DocumentXmlUtil.saveDocument(path, document,
            Map.of(
                OutputKeys.DOCTYPE_PUBLIC, PAYARA_RESOURCE_DOCTYPE_PUBLIC,
                OutputKeys.DOCTYPE_SYSTEM, PAYARA_RESOURCE_DOCTYPE_SYSTEM
            ));
        return path;

    }

    /**
     * Gets the path of the Payara resource XML file
     *
     * @param basedir Project Base dir
     * @return the path of the Payara resource XML file
     */
    private static Path getPayaraResourcesPath(File basedir) {
        return Paths.get(basedir.toString(), SRC_PATH, MAIN_PATH, "setup", "payara-resources.xml").normalize();
    }

}
