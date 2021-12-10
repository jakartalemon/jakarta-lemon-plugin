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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import static javax.xml.transform.OutputKeys.DOCTYPE_PUBLIC;
import static javax.xml.transform.OutputKeys.DOCTYPE_SYSTEM;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class XmlUtil {

    public static Element createElement(Document doc, String name, String textContent) {
        Element elem = doc.createElement(name);
        elem.setTextContent(textContent);
        return elem;
    }

    public static Element createElement(Document doc, String name, Node... children) {
        return createElement(doc, name, null, children);
    }

    public static Element createElement(Document doc, String name, Map<String, String> attrs, Node... children) {
        Element elem = doc.createElement(name);
        for (Node child : children) {
            elem.appendChild(child);

        }
        if (attrs != null) {
            attrs.entrySet().forEach(entry -> {
                Element propElem = doc.createElement("property");
                propElem.setAttribute("name",entry.getKey());
                propElem.setAttribute("value", entry.getValue());
                elem.appendChild(propElem);
            });
        }
        return elem;
    }

    public static Element createElement(Document doc, String name) {
        return doc.createElement(name);
    }

    public static void writeXml(Document doc, Path path) throws IOException, TransformerException {
        writeXml(doc, path.toFile());
    }

    public static void writeXml(Document doc, String publicId, String systemId, Path path) throws IOException, TransformerException {
        writeXml(doc, publicId, systemId, path.toFile());
    }

    public static void writeXml(Document doc, File file) throws IOException, TransformerException {
        writeXml(doc, null, null, file);
    }

    public static void writeXml(Document doc, String publicId, String systemId, File file) throws IOException, TransformerException {
        try ( FileOutputStream output
                = new FileOutputStream(file)) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            if (StringUtils.isNoneBlank(publicId, systemId)) {
                transformer.setOutputProperty(DOCTYPE_PUBLIC, publicId);
                transformer.setOutputProperty(DOCTYPE_SYSTEM, systemId);
            }

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(output);

            transformer.transform(source, result);
        }
    }
}
