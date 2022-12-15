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

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class WebXmlUtil {

    public static Document openWebXml(File basedir) throws IOException {
        Path webXmlPath = Paths.get(basedir.toString(), "src", "main", "webapp", "WEB-INF", "web.xml").normalize();
        Files.createDirectories(webXmlPath.getParent());
        return DocumentXmlUtil.openDocument(webXmlPath).orElseGet(() -> {
            try {
                var document = DocumentXmlUtil.newDocument("web-app");
                DocumentXmlUtil.findElementsByFilter(document, "/web-app").stream().findFirst()
                        .ifPresent(webappElement -> {
                            webappElement.setAttribute("xmlns", "https://jakarta.ee/xml/ns/jakartaee");
                            webappElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                            webappElement.setAttribute("xsi:schemaLocation",
                                                       "https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd");
                            webappElement.setAttribute("version", "5.0");
                            DocumentXmlUtil.createElement(document, webappElement, "session-config")
                                    .ifPresent(sessionConfigElem -> DocumentXmlUtil.createElement(document, sessionConfigElem, "session-timeout",
                                                                                              "30"));
                        });


                return document;
            } catch (ParserConfigurationException | XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static void saveWebXml(File basedir,
                                  Document document) {
        Path webXmlPath = Paths.get(basedir.toString(), "src", "main", "webapp", "WEB-INF", "web.xml").normalize();
        DocumentXmlUtil.saveDocument(webXmlPath, document);

    }
}
