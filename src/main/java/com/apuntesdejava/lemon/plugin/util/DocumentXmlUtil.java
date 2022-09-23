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

import java.util.LinkedHashSet;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author Diego Silva <diego.silva at apuntesdejava.com>
 */
public class DocumentXmlUtil {

    public static class ElementBuilder {

        private final String tagName;
        private final Set<String[]> attributes = new LinkedHashSet<>();
        private final Set<ElementBuilder> children = new LinkedHashSet<>();

        private ElementBuilder(String tagName) {
            this.tagName = tagName;
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
