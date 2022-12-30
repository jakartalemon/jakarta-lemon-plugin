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

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

import static java.util.Collections.emptyMap;
import static javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING;
import static org.apache.commons.lang3.BooleanUtils.NO;
import static org.apache.commons.lang3.BooleanUtils.YES;

/**
 * @author Diego Silva mailto:diego.silva@apuntesdejava.com
 */
public class DocumentXmlUtil {

    private static final Logger LOGGER = Logger.getLogger(DocumentXmlUtil.class.getName());
    private static final String STRIP_XSL_FILE_NAME = "/xml/strip.xsl";

    public static Document newDocument(String rootElementName) throws ParserConfigurationException {
        var documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setFeature(FEATURE_SECURE_PROCESSING, true);
        var documentBuilder = documentBuilderFactory.newDocumentBuilder();
        var document = documentBuilder.newDocument();
        var rootElement = document.createElement(rootElementName);
        document.appendChild(rootElement);
        return document;

    }

    public static Optional<Document> openDocument(Path path) {

        var documentBuilderFactory = DocumentBuilderFactory.newInstance();
        try {
            documentBuilderFactory.setFeature(FEATURE_SECURE_PROCESSING, true);
            var documentBuilder = documentBuilderFactory.newDocumentBuilder();
            var document = documentBuilder.parse(path.toFile());
            document.getDocumentElement().normalize();
            return Optional.of(document);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.severe(e.getMessage());
        }
        return Optional.empty();
    }

    public static List<Element> findElementsByFilter(Document document, String expression) throws XPathExpressionException {
        var xPath = XPathFactory.newInstance().newXPath();
        var nodeList = (NodeList) xPath.compile(expression).evaluate(document, XPathConstants.NODESET);
        List<Element> elementList = new ArrayList<>();
        for (int i = 0; i < nodeList.getLength(); i++) {
            elementList.add((Element) nodeList.item(i));
        }
        return elementList;
    }

    public static Optional<Element> createElement(Document document, String inPath, String elementName) throws XPathExpressionException {
        var elements = findElementsByFilter(document, inPath);
        if (!elements.isEmpty()) {
            Element element = document.createElement(elementName);
            elements.stream().findFirst().ifPresent(elem -> elem.appendChild(element));
            return Optional.of(element);
        }
        return Optional.empty();
    }

    public static Optional<Element> createElement(Document document, Element parentElement, String elementName, String textContent) {
        var element = document.createElement(elementName);
        if (StringUtils.isNotBlank(textContent)) {
            element.setTextContent(textContent);
        }
        parentElement.appendChild(element);
        return Optional.of(element);
    }

    public static Optional<Element> createElement(Document document, Element parentElement, String elementName) {
        return createElement(document, parentElement, elementName, null);
    }

    public static void saveDocument(Path path, Document document) {
        saveDocument(path, document, emptyMap());
    }

    public static void saveDocument(Path path, Document document, Map<String, String> outputProperties) {
        try ( var fos = new FileOutputStream(path.toFile());  var xlsIs = DocumentXmlUtil.class.getResourceAsStream(STRIP_XSL_FILE_NAME)) {
            Source xslt = new StreamSource(xlsIs);
            var transformerFactory = TransformerFactory.newInstance();
            var transformer = transformerFactory.newTransformer(xslt);
            transformer.setOutputProperty(OutputKeys.INDENT, YES);
            transformer.setOutputProperty(OutputKeys.STANDALONE, NO);
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, NO);
            outputProperties.forEach(transformer::setOutputProperty);
            document.setXmlStandalone(true);
            var source = new DOMSource(document);
            var result = new StreamResult(fos);
            transformer.transform(source, result);
        } catch (IOException | TransformerException e) {
            LOGGER.severe(e.getMessage());
        }
    }

    public static class ElementBuilder {

        private final String tagName;
        private final Set<String[]> attributes;
        private final Set<ElementBuilder> children;

        private ElementBuilder(String tagName) {
            this.tagName = tagName;
            attributes = new LinkedHashSet<>();
            children = new LinkedHashSet<>();
        }

        public static ElementBuilder newInstance(String tagName) {
            return new ElementBuilder(tagName);
        }

        public ElementBuilder addAttribute(String name, String value) {
            attributes.add(new String[]{name, value});
            return this;
        }

        public ElementBuilder addChild(ElementBuilder elementBuilder) {
            children.add(elementBuilder);
            return this;
        }

        public Element build(Document document) {
            Element element = document.createElement(tagName);
            attributes.forEach(attr -> element.setAttribute(attr[0], attr[1]));
            children.forEach(child -> element.appendChild(child.build(document)));

            return element;
        }

    }
}
