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
public class WebXmlUtil {
    /**
     * Open or create the web.xml file for the project being worked on. It receives as a parameter the path of the
     * current project.
     *
     * @param basedir Project folder being worked on.
     * @return XML object {@link Document} from the <code>web.xml</code> file to be manipulated.
     * @throws IOException if IO Exception
     */
    public static Document openWebXml(File basedir) throws IOException {
        Path webXmlPath = Paths.get(basedir.toString(), SRC_PATH, MAIN_PATH, WEBAPP, WEB_INF_PATH, WEBXML).normalize();
        Files.createDirectories(webXmlPath.getParent());
        return DocumentXmlUtil.openDocument(webXmlPath).orElseGet(() -> {
            try {
                var document = DocumentXmlUtil.newDocument(WEB_APP);
                DocumentXmlUtil.listElementsByFilter(document, SLASH + WEB_APP)
                    .stream()
                    .findFirst()
                    .ifPresent(webappElement -> {
                        webappElement.setAttribute(XMLNS, "https://jakarta.ee/xml/ns/jakartaee");
                        webappElement.setAttribute(XMLNS_XSI, XMLNS_XSI_INSTANCE);
                        webappElement.setAttribute("xsi:schemaLocation",
                            "https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd");
                        webappElement.setAttribute(VERSION, "5.0");
                        DocumentXmlUtil.createElement(document, webappElement, "session-config")
                            .ifPresent(sessionConfigElem -> DocumentXmlUtil.createElement(document, sessionConfigElem,
                                "session-timeout", "30"));
                    });


                return document;
            } catch (ParserConfigurationException | XPathExpressionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Saves the <code>web.xml</code> XML object to the appropriate location, based on the location of the project
     *
     * @param basedir  Project folder being worked on.
     * @param document XML object {@link Document} from the <code>web.xml</code> file to save.
     */

    public static void saveWebXml(File basedir, Document document) {
        Path webXmlPath = Paths.get(basedir.toString(), SRC_PATH, MAIN_PATH, WEBAPP, WEB_INF_PATH, WEBXML).normalize();
        DocumentXmlUtil.saveDocument(webXmlPath, document);

    }
}
